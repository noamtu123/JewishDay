// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import com.noamtu.jewishday.model.defaultJerusalemLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrentLocationQualityTest {
    @Test
    fun fixAgeUsesElapsedTimeAndFallsBackToWallTimeAcrossClockDomains() {
        val elapsedFix = CurrentLocationFix(
            location = defaultJerusalemLocation,
            horizontalAccuracyMeters = 20f,
            elapsedRealtimeNanos = 1_000_000_000L,
            wallTimeMillis = 10_000L,
            hasPrecisePermission = true,
        )
        assertEquals(500L, elapsedFix.ageMillis(1_500_000_000L, 99_000L))
        assertEquals(2_000L, elapsedFix.ageMillis(500_000_000L, 12_000L))
    }

    @Test
    fun significantlyNewerFixWinsEvenWhenLessAccurate() {
        assertTrue(isBetterLocationFix(200_001L, 1_000f, 0L, 10f))
    }

    @Test
    fun significantlyOlderFixLosesEvenWhenMoreAccurate() {
        assertFalse(isBetterLocationFix(0L, 5f, 200_001L, 1_000f))
    }

    @Test
    fun nearbyTimesPreferAccuracy() {
        assertTrue(isBetterLocationFix(10_000L, 20f, 0L, 200f))
        assertFalse(isBetterLocationFix(10_000L, 500f, 0L, 20f))
    }

    @Test
    fun newerFixMayRegressOnlySlightly() {
        assertTrue(isBetterLocationFix(10_000L, 250f, 0L, 100f))
        assertFalse(isBetterLocationFix(10_000L, 350f, 0L, 100f))
    }

    @Test
    fun knownAccuracyBeatsUnknownWithinTheTimeWindow() {
        assertTrue(isBetterLocationFix(0L, 50f, 0L, null))
        assertFalse(isBetterLocationFix(0L, null, 0L, 50f))
    }

    @Test
    fun invalidAccuracyIsTreatedAsUnknown() {
        assertTrue(isBetterLocationFix(0L, 50f, 0L, Float.NaN))
        assertFalse(isBetterLocationFix(0L, Float.NaN, 0L, 50f))
        assertFalse(isBetterLocationFix(0L, -1f, 0L, 50f))
    }
}
