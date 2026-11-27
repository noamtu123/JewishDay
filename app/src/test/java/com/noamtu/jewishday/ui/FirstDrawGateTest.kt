// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The app's first frame waits for the sky and the zmanim, never longer than the safety net. */
class FirstDrawGateTest {
    @Test
    fun waitsUntilEveryHoldIsReleased() {
        val gate = FirstDrawGate(startedAt = 0, timeoutMillis = 1_000)
        gate.hold() // the sky
        gate.hold() // the zmanim
        assertFalse(gate.mayDraw(now = 10))
        gate.release()
        assertFalse(gate.mayDraw(now = 20))
        gate.release()
        assertTrue(gate.mayDraw(now = 30))
    }

    @Test
    fun drawsAtOnceWhenNothingIsHeld() {
        assertTrue(FirstDrawGate(startedAt = 0).mayDraw(now = 0))
    }

    @Test
    fun drawsAnywayAfterTheTimeout() {
        val gate = FirstDrawGate(startedAt = 0, timeoutMillis = 1_000)
        gate.hold()
        assertFalse(gate.mayDraw(now = 999))
        assertTrue(gate.mayDraw(now = 1_000))
    }

    @Test
    fun onceOpenStaysOpen() {
        val gate = FirstDrawGate(startedAt = 0)
        assertTrue(gate.mayDraw(now = 0))
        // A screen that later waits again (a day being stepped to) does not blank the app.
        gate.hold()
        assertTrue(gate.mayDraw(now = 5))
    }
}
