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
import com.google.example.kiosk.client.databinding.ActivitySettingsBinding
import kotlinx.android.synthetic.main.activity_settings.*

/**
 * Settings for changing the host/port of the Kiosk API server.
 */
class SettingsActivity : AppCompatActivity() {

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
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
