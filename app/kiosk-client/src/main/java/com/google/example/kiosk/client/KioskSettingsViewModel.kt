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

import android.annotation.SuppressLint
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.content.SharedPreferences
import android.databinding.InverseMethod

internal const val PREF_HOST = "kiosk_host"
internal const val PREF_PORT = "kiosk_port"
internal const val PREF_KIOSK_ID = "kiosk_id"

// default to use the host running the Android emulator
internal const val PREFS_FILE_NAME = "kiosk"
internal const val PREF_HOST_DEFAULT = "10.0.2.2"
internal const val PREF_PORT_DEFAULT = 8080

/** ViewModel for the [SettingsActivity] to change the [host] and [port].*/
class KioskSettingsViewModel(
    private val preferences: SharedPreferences
) : ViewModel() {

    val host = MutableLiveData<String>()
    val port = MutableLiveData<Int>()

    init {
        // set initial values
        host.postValue(preferences.getString(PREF_HOST, PREF_HOST_DEFAULT))
        port.postValue(preferences.getInt(PREF_PORT, PREF_PORT_DEFAULT))
    }

    /** Save the current values */
    @SuppressLint("ApplySharedPref")
    fun save() {
        val h = host.value ?: ""
        val p = port.value ?: 8080

        preferences.edit()
                .putString(PREF_HOST, h)
                .putInt(PREF_PORT, p)
                .commit()
    }
}

/** A factory to create a [KioskViewModel] with it's dependencies. */
class KioskSettingsViewModelFactory(
    val preferences: SharedPreferences
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KioskSettingsViewModel::class.java)) {
            return KioskSettingsViewModel(preferences) as T
        }
        throw IllegalArgumentException("Unexpected ViewModel of type: $modelClass")
    }
}

/** Data binding converters */
class Converter {

    companion object {
        @JvmStatic
        fun intToString(value: Int): String = Integer.toString(value)

        @JvmStatic
        @InverseMethod("intToString")
        fun stringToInt(value: String): Int = try {
            Integer.parseInt(value)
        } catch (e: NumberFormatException) {
            0
        }
    }
}
