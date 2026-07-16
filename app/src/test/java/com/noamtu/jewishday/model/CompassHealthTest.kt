// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompassHealthTest {
    @Test
    fun initialUnreliableStatusIsCaught() {
        // The framework's onAccuracyChanged never fires when the first event is already
        // UNRELIABLE (its change-cache defaults to that value), so quality must come from
        // per-event statuses.
        val quality = compassQuality(primaryStatus = 0, magnetometerStatus = null, headingErrorDegrees = Float.NaN)
        assertTrue(quality.unreliableStatus)
        assertTrue(quality.needsCalibration)
    }

    @Test
    fun lowStatusAsksForCalibrationWithoutBeingUnreliable() {
        val quality = compassQuality(primaryStatus = 1, magnetometerStatus = null, headingErrorDegrees = Float.NaN)
        assertTrue(quality.needsCalibration)
        assertFalse(quality.unreliableStatus)
    }

    @Test
    fun mediumAndHighStatusesAreClean() {
        for (status in intArrayOf(2, 3)) {
            val quality = compassQuality(primaryStatus = status, magnetometerStatus = 3, headingErrorDegrees = Float.NaN)
            assertFalse(quality.needsCalibration)
            assertFalse(quality.unreliableStatus)
            assertFalse(quality.lowConfidence)
        }
    }

    @Test
    fun noContactCountsAsUnreliable() {
        val quality = compassQuality(primaryStatus = -1, magnetometerStatus = null, headingErrorDegrees = Float.NaN)
        assertTrue(quality.unreliableStatus)
    }

    @Test
    fun worstKnownStatusWins() {
        // A LOW magnetometer is a genuine calibration signal even while gyro fusion keeps the
        // fused source's own status high.
        val quality = compassQuality(primaryStatus = 3, magnetometerStatus = 1, headingErrorDegrees = Float.NaN)
        assertTrue(quality.needsCalibration)
        assertFalse(quality.unreliableStatus)
    }

    @Test
    fun unknownStatusesMakeNoClaims() {
        val quality = compassQuality(primaryStatus = null, magnetometerStatus = null, headingErrorDegrees = Float.NaN)
        assertFalse(quality.needsCalibration)
        assertFalse(quality.unreliableStatus)
        assertFalse(quality.lowConfidence)
    }

    @Test
    fun highHeadingErrorIsLowConfidenceNotACalibrationClaim() {
        val quality = compassQuality(primaryStatus = 3, magnetometerStatus = 3, headingErrorDegrees = 60f)
        assertTrue(quality.lowConfidence)
        assertFalse(quality.needsCalibration)
        val unknownError = compassQuality(primaryStatus = 3, magnetometerStatus = 3, headingErrorDegrees = Float.NaN)
        assertFalse(unknownError.lowConfidence)
    }
}
