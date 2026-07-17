// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import org.junit.Assert.assertEquals
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
        assertTrue(quality.needsCalibration)
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
        val quality = compassQuality(primaryStatus = 3, magnetometerStatus = 3, headingErrorDegrees = 15f)
        assertTrue(quality.lowConfidence)
        assertFalse(quality.needsCalibration)
        val boundary = compassQuality(
            primaryStatus = 3,
            magnetometerStatus = 3,
            headingErrorDegrees = LowConfidenceHeadingErrorDegrees,
        )
        assertFalse(boundary.lowConfidence)
        val unknownError = compassQuality(primaryStatus = 3, magnetometerStatus = 3, headingErrorDegrees = Float.NaN)
        assertFalse(unknownError.lowConfidence)
    }

    @Test
    fun rawFallbackRequiresBothVectorsToBeFresh() {
        val now = 2_000_000_000L
        assertTrue(sensorPairIsFresh(now, 1_500_000_000L, 1_900_000_000L, 1_000_000_000L))
        assertFalse(sensorPairIsFresh(now, 0L, 1_900_000_000L, 1_000_000_000L))
        assertFalse(sensorPairIsFresh(now, 500_000_000L, 1_900_000_000L, 1_000_000_000L))
        assertFalse(sensorPairIsFresh(now, 2_100_000_000L, 1_900_000_000L, 1_000_000_000L))
        assertFalse(
            sensorPairIsFresh(
                nowNanos = now,
                firstSampleNanos = 1_500_000_000L,
                secondSampleNanos = 1_900_000_000L,
                maxAgeNanos = 1_000_000_000L,
                maxSkewNanos = 200_000_000L,
            ),
        )
    }

    @Test
    fun sensorEventsMustBelongToTheCurrentGenerationAndArriveFresh() {
        val now = 2_000_000_000L
        assertTrue(sensorEventIsCurrent(now, 1_900_000_000L, 1_000_000_000L, 1_800_000_000L, 500_000_000L))
        assertFalse(sensorEventIsCurrent(now, 900_000_000L, 1_000_000_000L, 0L, 2_000_000_000L))
        assertFalse(sensorEventIsCurrent(now, 1_800_000_000L, 1_000_000_000L, 1_800_000_000L, 500_000_000L))
        assertFalse(sensorEventIsCurrent(now, 1_000_000_000L, 500_000_000L, 0L, 500_000_000L))
        assertFalse(sensorEventIsCurrent(now, 2_100_000_000L, 1_000_000_000L, 0L, 500_000_000L))
    }

    @Test
    fun sensorStatusLabelsCoverEveryPlatformValue() {
        assertEquals("—", sensorStatusLabel(null))
        assertEquals("No contact (-1)", sensorStatusLabel(SensorStatusNoContact))
        assertEquals("Unreliable (0)", sensorStatusLabel(SensorStatusUnreliable))
        assertEquals("Low (1)", sensorStatusLabel(SensorStatusAccuracyLow))
        assertEquals("Medium (2)", sensorStatusLabel(SensorStatusAccuracyMedium))
        assertEquals("High (3)", sensorStatusLabel(SensorStatusAccuracyHigh))
        assertEquals("Unknown (7)", sensorStatusLabel(7))
    }
}
