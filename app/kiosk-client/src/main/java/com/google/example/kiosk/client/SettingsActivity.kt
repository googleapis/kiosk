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

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import com.google.example.kiosk.client.databinding.ActivitySettingsBinding
import kotlinx.android.synthetic.main.activity_settings.*

private const val TAG = "Setting"

/**
 * Settings for changing the host/port of the Kiosk API server.
 */
class SettingsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RETURN_TO_ACTIVITY = "return-activity"
    }

    private lateinit var model: KioskSettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // wire up the UI
        val modelFactory = KioskSettingsViewModelFactory(
                getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE))
        model = ViewModelProviders.of(this, modelFactory)
                .get(KioskSettingsViewModel::class.java)
        val binding: ActivitySettingsBinding = DataBindingUtil.setContentView(this,
                R.layout.activity_settings)
        binding.setLifecycleOwner(this)
        binding.model = model
        setSupportActionBar(toolbar)

        // add options for known hosts
        val hostAdapter = HostArrayAdapter(this, resources.getStringArray(R.array.known_servers))
        if (hostAdapter.isEmpty) {
            serverAddressSpinner.visibility = View.GONE
        } else {
            serverAddressSpinner.adapter = hostAdapter
            serverAddressSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    pos: Int,
                    id: Long
                ) {
                    val newValue = serverAddressSpinner.selectedItem.toString().split(":")
                    if (newValue.isNotEmpty()) {
                        try {
                            // format is "label:host:port"
                            val port = Integer.parseInt(newValue[2])
                            model.host.postValue(newValue[1])
                            model.port.postValue(port)
                        } catch (ex: Exception) {
                            Log.e(TAG, "Unable to set known host")
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        // go back to the main activity
        saveButton.setOnClickListener {
            model.save()
            goToMainActivity()
        }
    }

    // treat back button as a "cancel" operation
    override fun onBackPressed() {
        goToMainActivity()
        super.onBackPressed()
    }

    private fun goToMainActivity() {
        val clazz = intent?.extras?.getSerializable(EXTRA_RETURN_TO_ACTIVITY) as? Class<*>
        startActivity(Intent(this, clazz ?: MainActivity::class.java))
        finish()
    }
}

// array adapter for selecting the server address
private class HostArrayAdapter(
    context: Context,
    list: Array<String>
) : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, list) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = (convertView ?: LayoutInflater.from(context).inflate(
                android.R.layout.simple_spinner_item, parent, false)) as TextView
        view.text = getItem(position)!!.split(":")[0]
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = (convertView ?: LayoutInflater.from(context).inflate(
                android.R.layout.simple_spinner_dropdown_item, parent, false)) as TextView
        view.text = getItem(position)!!.replaceFirst(":", " ")
        return view
    }
}