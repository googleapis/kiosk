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
import android.util.Log
import android.view.View
import android.widget.ImageView
import com.google.api.kgax.grpc.ServerStreamingCall
import com.google.protobuf.timestamp
import com.google.type.LatLng
import io.grpc.StatusRuntimeException
import kiosk.DisplayClient
import kiosk.GetSignIdResponse
import kiosk.Kiosk
import kiosk.ScreenSize
import kiosk.Sign
import kiosk.getKioskRequest
import kiosk.getSignIdForKioskIdRequest
import kiosk.getSignRequest
import kiosk.kiosk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    /** image scale type */
    val scaleType = MutableLiveData<ImageView.ScaleType>()

    private var kioskId: Int? = null
    private var signSubscription: ServerStreamingCall<GetSignIdResponse>? = null
    private var reconnectPending = false

    init {
        connected.postValue(false)
        textPosition.postValue(TextPosition.CENTER)
        scaleType.postValue(ImageView.ScaleType.CENTER_CROP)
    }

    override fun onCleared() {
        super.onCleared()

        // ensure everything stops
        stop(true)
    }

    /**
     * Register the device as a new kiosk with the given parameters.
     *
     * An error is shown if the operation fails and null is returned.
     */
    suspend fun registerKiosk(
        kioskName: String,
        kioskLocation: LatLng,
        screenSize: ScreenSize
    ): Kiosk? {
        Log.i(TAG, "Registering kiosk with name: $kioskName")

        // register the kiosk
        val now = System.currentTimeMillis()
        try {
            return client.createKiosk(kiosk {
                name = kioskName
                createTime = timestamp {
                    seconds = now / 1_000
                    nanos = ((now % 1_000) * 1_000_000).toInt()
                }
                size = screenSize
                location = kioskLocation
            }).body
        } catch (ex: StatusRuntimeException) {
            Log.e(TAG, "failed to register kiosk", ex)

            errorMessage.postValue(app.getString(R.string.error_kiosk_registration))
            errorStacktrace.postValue(ex.localizedMessage ?: ex.toString())
        }

        // registration failed
        return null
    }

    /**
     * Switch to the kiosk with [newId] and display the active sign.
     */
    @Synchronized
    suspend fun switchToKiosk(newId: Int) {
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

        // shutdown sign update stream
        stop()

        // switch
        try {
            val response = client.getKiosk(getKioskRequest {
                id = newId
            })

            kiosk.postValue(response.body)
            subscribeToSigns(response.body)
        } catch (ex: StatusRuntimeException) {
            Log.e(TAG, "Unable to fetch kiosk: $newId", ex)

            errorMessage.postValue(app.getString(R.string.error_kiosk_update))
            errorStacktrace.postValue(ex.localizedMessage ?: ex.toString())

            reconnect(RETRY_INTERVAL)
        }
    }

    @Synchronized
    private fun stop(isTerminal: Boolean = false) {
        if (isTerminal) {
            kioskId = -1
        }

        // shutdown the stream
        val oldStream = signSubscription
        signSubscription = null
        oldStream?.responses?.cancel()

        Log.d(TAG, "Stopped kiosk subscription.")
    }

    /** Toggles the scale type of the image. */
    fun toggleScaleType() {
        val newType = when (scaleType.value) {
            ImageView.ScaleType.CENTER_CROP -> ImageView.ScaleType.CENTER_INSIDE
            ImageView.ScaleType.CENTER_INSIDE -> ImageView.ScaleType.FIT_CENTER
            else -> ImageView.ScaleType.CENTER_CROP
        }
        scaleType.postValue(newType)
    }

    private suspend fun subscribeToSigns(kiosk: Kiosk) = coroutineScope {
        Log.i(TAG, "Subscribing to kiosk sign updates for kiosk: ${kiosk.id}")

        // start the subscription
        val stream = client.getSignIdsForKioskId(getSignIdForKioskIdRequest {
            kioskId = kiosk.id
        })
        signSubscription = stream

        // process the responses
        launch(Dispatchers.IO) {
            try {
                for (response in stream.responses) {
                    Log.i(TAG, "Received updated sign: ${response.signId} from kiosk: ${kiosk.id}")

                    // reset state
                    connected.postValue(true)
                    errorMessage.postValue(null)
                    errorStacktrace.postValue(null)

                    fetchSign(response.signId)
                }

                Log.i(TAG, "Subscription terminated for kiosk: ${kiosk.id}")

                reconnect()
            } catch (ex: StatusRuntimeException) {
                Log.i(TAG, "Error watching for sign updates on kiosk: ${kiosk.id}", ex)

                errorMessage.postValue(app.getString(R.string.error_kiosk_update))
                errorStacktrace.postValue(ex.localizedMessage ?: ex.toString())

                reconnect(RETRY_INTERVAL)
            }
        }
    }

    private suspend fun fetchSign(signId: Int) {
        if (signId <= 0) {
            Log.i(TAG, "No sign has been assigned... skipping fetch.")
            return
        }

        try {
            val response = client.getSign(getSignRequest {
                id = signId
            })
            Log.i(TAG, "Fetched sign with id: $signId")

            // update sign body
            if (response.body != sign.value) {
                sign.postValue(response.body)
                val position = if (response.body.image != null && !response.body.image.isEmpty) {
                    TextPosition.BOTTOM
                } else {
                    TextPosition.CENTER
                }
                textPosition.postValue(position)
            }
        } catch (ex: StatusRuntimeException) {
            Log.e(TAG, "Unable to getch sign: $signId")

            errorMessage.postValue(app.getString(R.string.error_kiosk_sign_update))
            errorStacktrace.postValue(ex.localizedMessage ?: ex.toString())

            reconnect(RETRY_INTERVAL)
        }
    }

    // retry helper for when the connection is terminated
    @Synchronized
    private suspend fun reconnect(delayInMillis: Long = 0) = coroutineScope {
        if (reconnectPending) return@coroutineScope

        Log.d(TAG, "Retrying (due to server disconnection or error)...")

        // ensure UI state show disconnected status
        connected.postValue(false)

        // attempt to reconnect
        reconnectPending = true

        delay(delayInMillis)
        try {
            reconnectPending = false
            kioskId?.let { switchToKiosk(it) }
        } catch (ex: Throwable) {
            Log.d(TAG, "reconnect attempt failed", ex)
        }
    }

    /** Visibility helpers */
    object Vis {

        @JvmStatic
        fun showProgress(connected: Boolean?, errorMessage: String?) =
            if (errorMessage == null &&
                connected != null && !connected
            ) {
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
            condition: Boolean? = true,
            isText: Boolean = false
        ) =
            if (connected != null && connected &&
                errorMessage == null &&
                sign != null &&
                condition != null && condition &&
                ((isText && sign.text?.length ?: 0 > 0) || (!isText))
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

        @JvmStatic
        fun showNoSign(connected: Boolean?, errorMessage: String?, sign: Sign?) =
            if (connected != null && connected &&
                errorMessage == null &&
                sign == null
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

        @JvmStatic
        fun scaleType(type: ImageView.ScaleType?) = type ?: ImageView.ScaleType.CENTER_CROP
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
