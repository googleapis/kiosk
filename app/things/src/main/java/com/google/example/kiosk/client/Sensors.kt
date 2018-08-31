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

import android.graphics.Color
import android.os.CountDownTimer
import android.view.KeyEvent
import com.google.android.things.contrib.driver.apa102.Apa102
import com.google.android.things.contrib.driver.button.ButtonInputDriver
import com.google.android.things.contrib.driver.ht16k33.Ht16k33
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat

/** Utility functions for interacting with the Rainbow HAT board. */
internal object Sensors {

    private var rainbowFlashing = false
    private var buzzing = false

    /** toggles the LED above the switch */
    fun toggleRedLed(on: Boolean) = RainbowHat.openLedRed().use { it.value = on }

    /** toggles the LED above the switch */
    fun toggleBlueLed(on: Boolean) = RainbowHat.openLedBlue().use { it.value = on }

    /** toggles the LED above the switch */
    fun togleGreenLed(on: Boolean) = RainbowHat.openLedGreen().use { it.value = on }

    /** Set all the LEDs above the switches */
    fun toggleLeds(red: Boolean, green: Boolean, blue: Boolean) {
        toggleRedLed(red)
        togleGreenLed(green)
        toggleBlueLed(blue)
    }

    /** Get drivers for all the buttons */
    fun getButtonDrivers() = ButtonDrivers(
            RainbowHat.createButtonAInputDriver(KeyEvent.KEYCODE_A),
            RainbowHat.createButtonBInputDriver(KeyEvent.KEYCODE_B),
            RainbowHat.createButtonCInputDriver(KeyEvent.KEYCODE_C)
    )

    /** set the text on the rainbow hat display */
    fun setRainbowHatDisplay(text: String?) {
        RainbowHat.openDisplay().use { display ->
            if (text != null) {
                // right align text
                val t = when (text.length) {
                    1 -> "   $text"
                    2 -> "  $text"
                    3 -> " $text"
                    else -> text
                }

                // update display
                display.setBrightness(Ht16k33.HT16K33_BRIGHTNESS_MAX)
                display.display(t)
                display.setEnabled(true)
            } else {
                // disable display
                display.display("")
                display.setEnabled(false)
            }
        }
    }

    /** does a short rainbow animation on the LED strip */
    @Synchronized
    fun flashRainbow(durationInMillis: Long = 2_000) {
        if (rainbowFlashing) return
        rainbowFlashing = true

        // do a quick flash of the rainbow LED strip
        val ledStrip = RainbowHat.openLedStrip()
        ledStrip.brightness = Apa102.MAX_BRIGHTNESS
        object : CountDownTimer(durationInMillis,
                durationInMillis / (RainbowHat.LEDSTRIP_LENGTH * 2)) {
            var ledNum = RainbowHat.LEDSTRIP_LENGTH - 1

            override fun onTick(millisRemaining: Long) {
                val rainbow = Array(RainbowHat.LEDSTRIP_LENGTH) { i ->
                    val idx = Math.abs(i)
                    if (idx == Math.abs(ledNum)) {
                        Color.HSVToColor(255,
                                floatArrayOf(idx * 360.0f / RainbowHat.LEDSTRIP_LENGTH, 1.0f, 1.0f))
                    } else {
                        Color.BLACK
                    }
                }
                ledStrip.write(rainbow.toIntArray())
                ledNum--
            }

            override fun onFinish() {
                rainbowFlashing = false
                ledStrip.write(Array(RainbowHat.LEDSTRIP_LENGTH) { Color.BLACK }.toIntArray())
                ledStrip.close()
            }
        }.start()
    }

    /** make some noise! */
    @Synchronized
    fun buzz(
            durationInMillis: Long = 500,
            frequencyStart: Double = 300.0,
            frequencyEnd: Double = 700.0,
            steps: Int = 4,
            interpolator: BuzzInterpolator = LinearBuzzInterpolator
    ) {
        if (buzzing) return
        buzzing = true

        // play a short tone
        val buzzer = RainbowHat.openPiezo()
        buzzer.play(frequencyStart)
        object : CountDownTimer(durationInMillis, durationInMillis / (steps - 1)) {
            var interval = 1

            override fun onTick(timeRemaining: Long) {
                buzzer.stop()
                buzzer.play(interpolator.value(frequencyStart, frequencyEnd, interval, steps))
                interval++
            }

            override fun onFinish() {
                buzzer.stop()
                buzzer.close()
                buzzing = false
            }
        }.start()
    }
}

internal data class ButtonDrivers(
        val a: ButtonInputDriver,
        val b: ButtonInputDriver,
        val c: ButtonInputDriver
)

internal interface BuzzInterpolator {
    fun value(start: Double, end: Double, interval: Int, total: Int): Double
}

internal object LinearBuzzInterpolator : BuzzInterpolator {
    override fun value(start: Double, end: Double, interval: Int, total: Int) =
            start + ((Math.abs(end - start) / total) * interval)
}
