/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.example.kiosk.client

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import com.google.common.base.Preconditions
import com.google.kgax.grpc.CallResult
import com.google.kgax.grpc.ServerStreamingCall
import com.google.protobuf.Timestamp
import com.google.type.LatLng
import kiosk.DisplayClient
import kiosk.GetKioskRequest
import kiosk.GetSignIdForKioskIdRequest
import kiosk.GetSignIdResponse
import kiosk.GetSignRequest
import kiosk.Kiosk
import kiosk.ScreenSize
import kiosk.Sign

private const val TAG = "KioskVM"

private const val RETRY_INTERVAL = 2_000L

/** A ViewModel for showing the current [sign] on a [kiosk]. */
class KioskViewModel(
    private val app: Application,
    private val client: DisplayClient
) : AndroidViewModel(app) {

    enum class TextPosition {
        CENTER, BOTTOM
    }

    /** If a connection to the backend is alive */
    val connected = MutableLiveData<Boolean>()
    /** An error message to show in place of the sign content */
    val errorMessage = MutableLiveData<String>()
    /** A optional detailed error message (may not be localized) */
    val errorStacktrace = MutableLiveData<String>()
    /** where to show the text */
    val textPosition = MutableLiveData<TextPosition>()

    /** The kiosk after a successful [switchToKiosk] */
    val kiosk = MutableLiveData<Kiosk>()
    /** The current sign to display */
    val sign = MutableLiveData<Sign>()

    private var kioskId: Int? = null
    private var signSubscription: ServerStreamingCall<GetSignIdResponse>? = null
    private var reconnectPending = false

    init {
        connected.postValue(false)
        textPosition.postValue(TextPosition.CENTER)
    }

    /**
     * Register the device as a new kiosk with the given parameters.
     *
     * [onComplete] is only called if the registration was successful
     * and an error is shown if the operation fails.
     */
    fun registerKiosk(
        kioskName: String,
        kioskLocation: LatLng,
        screenSize: ScreenSize,
        onComplete: (CallResult<Kiosk>) -> Unit
    ) {
        val now = System.currentTimeMillis()

        // register the kiosk
        Log.i(TAG, "Registering kiosk with name: $kioskName")
        client.createKiosk(Kiosk {
            name = kioskName
            createTime = Timestamp {
                seconds = now / 1_000
                nanos = ((now % 1_000) * 1_000_000).toInt()
            }
            size = screenSize
            location = kioskLocation
        }).onUI {
            success = onComplete
            error = {
                Log.e(TAG, "failed to register kiosk", it)

                errorMessage.postValue(app.getString(R.string.error_kiosk_registration))
                errorStacktrace.postValue(it.localizedMessage ?: it.toString())
            }
        }
    }

    /**
     * Switch to the kiosk with [newId] and display the active sign.
     */
    @Synchronized
    fun switchToKiosk(newId: Int) {
        kioskId = newId

        // wait if a reconnect is pending
        if (reconnectPending) {
            return
        }

        Log.i(TAG, "Switching to kiosk: $newId")

        // reset state
        kiosk.postValue(null)
        sign.postValue(null)
        connected.postValue(false)

        // TODO: shutdown signSubscription (no way to do it yet with the Kotlin clients)
        //       so we'll just ignore the events when this happens

        // switch
        client.getKiosk(GetKioskRequest {
            id = newId
        }).onUI {
            success = {
                kiosk.postValue(it.body)
                subscribeToSigns(it.body)
            }
            error = {
                Log.e(TAG, "Unable to fetch kiosk: $newId", it)

                errorMessage.postValue(app.getString(R.string.error_kiosk_update))
                errorStacktrace.postValue(it.localizedMessage ?: it.toString())

                reconnect(RETRY_INTERVAL)
            }
            ignoreIf = { newId != kioskId }
        }
    }

    private fun subscribeToSigns(k: Kiosk) {
        Log.i(TAG, "Subscribing to kiosk sign updates for kiosk: ${k.id}")

        // start the subscription
        val stream = client.getSignIdsForKioskId(GetSignIdForKioskIdRequest {
            kioskId = k.id
        })
        signSubscription = stream

        // process the responses
        stream.start {
            executor = MainThreadExecutor
            onNext = {
                if (kiosk.value == k) {
                    Log.i(TAG, "Received updated sign: ${it.signId} from kiosk: ${k.id}")

                    // reset state
                    connected.postValue(true)
                    errorMessage.postValue(null)
                    errorStacktrace.postValue(null)

                    fetchSign(it.signId, stream)
                }
            }
            onCompleted = {
                if (kiosk.value == k) {
                    Log.i(TAG, "Subscription terminated for kiosk: ${k.id}")

                    reconnect()
                }
            }
            onError = {
                if (kiosk.value == k) {
                    Log.i(TAG, "Error watching for sign updates on kiosk: ${k.id}", it)

                    errorMessage.postValue(app.getString(R.string.error_kiosk_update))
                    errorStacktrace.postValue(it.localizedMessage ?: it.toString())

                    reconnect(RETRY_INTERVAL)
                }
            }
        }
    }

    private fun fetchSign(signId: Int, stream: ServerStreamingCall<GetSignIdResponse>) {
        if (signId <= 0) {
            Log.i(TAG, "No sign has been assigned... skipping fetch.")
            return
        }

        client.getSign(GetSignRequest {
            id = signId
        }).onUI {
            success = {
                Log.i(TAG, "Fetched sign with id: $signId")

                // update sign body
                if (it.body != sign.value) {
                    sign.postValue(it.body)
                    val position = if (it.body.image != null && !it.body.image.isEmpty) {
                        TextPosition.BOTTOM
                    } else {
                        TextPosition.CENTER
                    }
                    textPosition.postValue(position)
                }
            }
            error = {
                Log.e(TAG, "Unable to getch sign: $signId")

                errorMessage.postValue(app.getString(R.string.error_kiosk_sign_update))
                errorStacktrace.postValue(it.localizedMessage ?: it.toString())

                reconnect(RETRY_INTERVAL)
            }
            ignoreIf = { stream != signSubscription }
        }
    }

    // retry helper for when the connection is terminated
    @Synchronized
    private fun reconnect(delay: Long = 0) {
        Preconditions.checkState(!reconnectPending,
                "Reconnection is pending. Illegal call to reconnect!")

        Log.d(TAG, "Retrying (due to server disconnection or error)...")

        // ensure UI state show disconnected status
        connected.postValue(false)

        // attempt to reconnect
        reconnectPending = true
        fun doConnect() {
            reconnectPending = false
            kioskId?.let { switchToKiosk(it) }
        }

        if (delay > 0) {
            object : CountDownTimer(delay, delay) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() {
                    doConnect()
                }
            }.start()
        } else {
            doConnect()
        }
    }

    /** Visibility helpers */
    object Vis {

        @JvmStatic
        fun showProgress(connected: Boolean?, errorMessage: String?) =
                if (errorMessage == null &&
                        connected != null && !connected) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

        @JvmStatic
        fun showIfError(errorMessage: String?) =
                if (errorMessage != null) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

        @JvmStatic
        @JvmOverloads
        fun showSign(
            connected: Boolean?,
            errorMessage: String?,
            sign: Sign?,
            condition: Boolean? = true
        ) =
                if (connected != null && connected &&
                        errorMessage == null &&
                        sign != null &&
                        condition != null && condition) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

        @JvmStatic
        fun showNoSign(connected: Boolean?, errorMessage: String?, sign: Sign?) =
                if (connected != null && connected &&
                        errorMessage == null &&
                        sign == null) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
    }
}

/** A factory to create a [KioskViewModel] with it's dependencies. */
class KioskViewModelFactory(
    private val app: Application,
    private val client: DisplayClient
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KioskViewModel::class.java)) {
            return KioskViewModel(app, client) as T
        }
        throw IllegalArgumentException("Unexpected ViewModel of type: $modelClass")
    }
}
