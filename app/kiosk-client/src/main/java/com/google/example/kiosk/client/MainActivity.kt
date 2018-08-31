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

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.graphics.BitmapFactory
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.example.kiosk.client.databinding.ActivityMainBinding
import com.google.type.LatLng
import io.grpc.okhttp.OkHttpChannelBuilder
import kiosk.DisplayClient
import kiosk.ScreenSize
import kiosk.Sign
import kotlinx.android.synthetic.main.activity_main.*

private const val TAG = "Main"

/**
 * Main activity for our application.
 *
 * The app for the phone will use this directly.
 * The Android Things version will subclass this to take advantage of the hardware.
 */
open class MainActivity : AppCompatActivity() {

    private lateinit var displayClient: DisplayClient
    private lateinit var locationClient: FusedLocationProviderClient
    protected lateinit var model: KioskViewModel

    private lateinit var preferences: SharedPreferences

    private var kioskId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // get preferences
        preferences = getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

        // get location services
        locationClient = LocationServices.getFusedLocationProviderClient(this)

        // create client to access the kiosk API
        val host = preferences.getString(PREF_HOST, null) ?: PREF_HOST_DEFAULT
        val port = preferences.getInt(PREF_PORT, PREF_PORT_DEFAULT)
        val channel = OkHttpChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .keepAliveWithoutCalls(true)
                .build()
        displayClient = DisplayClient.fromCredentials(channel = channel)

        // wire up the UI
        val modelFactory = KioskViewModelFactory(application, displayClient)
        model = ViewModelProviders.of(this, modelFactory).get(KioskViewModel::class.java)
        val binding: ActivityMainBinding = DataBindingUtil.setContentView(
                this, R.layout.activity_main)
        binding.setLifecycleOwner(this)
        binding.model = model
        setSupportActionBar(toolbar)

        // update background image on changes
        model.sign.observe(this, Observer<Sign> { sign ->
            val bytes = sign?.image?.toByteArray()
            if (bytes != null && bytes.isNotEmpty()) {
                val photo = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                signImage.setImageBitmap(photo)
            } else {
                signImage.setImageBitmap(null)
            }
        })

        // reuse the name if this device has been previously registered
        registerKiosk(preferences.getInt(PREF_KIOSK_ID, -1))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.tool_bar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        // open settings activity on click
        if (item?.itemId == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Register this device as a kiosk with the given [id].
     *
     * If the [id] is less than 0 (default) then attempt to register as a new kiosk.
     */
    protected fun registerKiosk(id: Int = -1) {
        // register this kiosk after getting the device location
        if (ContextCompat.checkSelfPermission(applicationContext,
                        ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationClient.lastLocation.addOnSuccessListener { registerKiosk(id, it) }
        } else {
            registerKiosk(id, null)
        }
    }

    private fun registerKiosk(id: Int, location: Location?) {
        if (id < 0) {
            // ensure no old id is left
            preferences.edit()
                    .remove(PREF_KIOSK_ID)
                    .apply()

            // get more info about the device
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            val screenSize = ScreenSize {
                width = displayMetrics.widthPixels
                height = displayMetrics.heightPixels
            }
            val latLng = LatLng {
                latitude = location?.latitude ?: 0.0
                longitude = location?.longitude ?: 0.0
            }

            val name = NameGenerator.next()
            model.registerKiosk(name, latLng, screenSize) {
                Log.i(TAG, "Registered kiosk as '${it.body.name}'")

                // save id
                preferences.edit()
                        .putInt(PREF_KIOSK_ID, it.body.id)
                        .apply()

                // active the kiosk to show the active sign
                kioskId = it.body.id
                model.switchToKiosk(it.body.id)
            }
        } else {
            kioskId = id
            model.switchToKiosk(id)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // shutdown channel
        ShutdownTask(displayClient).execute()
    }

    private class ShutdownTask(private val client: DisplayClient) : AsyncTask<Unit, Unit, Unit>() {
        override fun doInBackground(vararg args: Unit?) = client.shutdownChannel()
    }
}
