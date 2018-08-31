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

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import kiosk.Kiosk
import kiosk.Sign
import kotlin.properties.Delegates

private const val TAG = "Main"

/** Main activity for the Android Things version of the app */
class MainActivityThings : MainActivity() {

    private val buttons = ButtonState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // show the available buttons and active them
        Sensors.toggleLeds(red = true, green = false, blue = false)
        Sensors.getButtonDrivers().let { drivers ->
            drivers.a.register()
            drivers.b.register()
            drivers.c.register()
        }

        // display the kiosk id on the display
        model.kiosk.observe(this, Observer<Kiosk> { kiosk ->
            Sensors.setRainbowHatDisplay(kiosk?.id?.toString())
        })

        // flash the LED strip to draw attention to a sign change
        model.sign.observe(this, Observer<Sign> { sign ->
            if (sign != null) {
                Sensors.flashRainbow()
                Sensors.buzz()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()

        // turn off display
        Sensors.setRainbowHatDisplay(null)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        updateButtonState(keyCode, true)
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        updateButtonState(keyCode, false)
        return super.onKeyUp(keyCode, event)
    }

    private fun updateButtonState(keyCode: Int, down: Boolean) {
        when (keyCode) {
            KeyEvent.KEYCODE_A -> buttons.a = down
            KeyEvent.KEYCODE_B -> buttons.b = down
            KeyEvent.KEYCODE_C -> buttons.c = down
        }

        // check for the "reset" sequence of button presses
        if (!down && buttons.isResetSequence()) {
            Log.i(TAG, "Resetting kiosk (reset button sequence)..." + System.currentTimeMillis())

            buttons.clearSequence()
            registerKiosk()
        }
    }

}

/** Helper class for maintain the state of the buttons */
private class ButtonState {
    private val sequence = mutableListOf<ButtonEvent>()

    /** A button state */
    var a: Boolean by Delegates.observable(false) { _, _, new ->
        updateSequence("a", new)
    }

    /** B button state */
    var b: Boolean by Delegates.observable(false) { _, _, new ->
        updateSequence("b", new)
    }

    /** C button state */
    var c: Boolean by Delegates.observable(false) { _, _, new ->
        updateSequence("c", new)
    }

    /**
     * Tests if the last sequence of key events is the "reset" signal
     * (3 A's in a row within a short time period)
     */
    fun isResetSequence(): Boolean {
        val now = System.currentTimeMillis()
        return sequence
                .filter { now - it.time < 4_000 }
                .joinToString("") { it.button } == "aaa"
    }

    /** Forgets all previous button presses */
    fun clearSequence() = sequence.clear()

    private fun updateSequence(button: String, down: Boolean) {
        if (down) {
            sequence.add(ButtonEvent(button, System.currentTimeMillis()))
            while (sequence.size > 3) {
                sequence.removeAt(0)
            }
        }
    }

    private data class ButtonEvent(val button: String, val time: Long)

}
