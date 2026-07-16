// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MizrachTest {
    @Test
    fun kodeshHakodashimToKodeshHakodashimIsZeroDistance() {
        val info = mizrachInfo(kodeshHakodashimLocation)

        assertEquals("Kodesh HaKodashim", info.fromLocationName)
        assertEquals(0, info.distanceKm)
        assertEquals(0, info.bearingDegrees)
    }

    @Test
    fun newYorkToJerusalemHasExpectedDirectionAndDistance() {
        val newYork = JewishLocation(
            name = "New York",
            latitude = 40.7128,
            longitude = -74.0060,
            elevationMeters = 10.0,
            zoneId = ZoneId.of("America/New_York"),
        )

        val info = mizrachInfo(newYork)

        assertTrue(info.bearingDegrees in 50..60)
        assertTrue(info.distanceKm in 9100..9250)
    }

    @Test
    fun dueSouthOfTargetBearsNorth() {
        val south = jewishLocationAt(latitude = 20.0, longitude = kodeshHakodashimLocation.longitude)

        assertEquals(0, mizrachInfo(south).bearingDegrees)
    }

    @Test
    fun dueNorthOfTargetBearsSouth() {
        val north = jewishLocationAt(latitude = 40.0, longitude = kodeshHakodashimLocation.longitude)

        assertEquals(180, mizrachInfo(north).bearingDegrees)
    }

    @Test
    fun bearingIsCorrectAcrossTheAntimeridian() {
        // Samoa sits past the antimeridian from Jerusalem; the great circle heads northwest.
        val samoa = jewishLocationAt(latitude = -13.76, longitude = -171.8)

        assertTrue(mizrachInfo(samoa).bearingDegrees in 300..320)
    }

    @Test
    fun southernHemisphereBearingIsCorrect() {
        val auckland = jewishLocationAt(latitude = -36.85, longitude = 174.76)

        assertTrue(mizrachInfo(auckland).bearingDegrees in 265..285)
    }

    @Test
    fun antipodalPointStaysFiniteAtHalfTheCircumference() {
        // The exact antipode of the target: rounding can push the haversine intermediate past 1,
        // which would turn the distance into NaN without clamping.
        val antipode = jewishLocationAt(
            latitude = -kodeshHakodashimLocation.latitude,
            longitude = kodeshHakodashimLocation.longitude - 180.0,
        )

        val info = mizrachInfo(antipode)

        assertTrue(info.distanceKm in 19900..20100)
        assertTrue(info.bearingDegrees in 0..359)
    }

    private fun jewishLocationAt(latitude: Double, longitude: Double): JewishLocation = JewishLocation(
        name = "Test",
        latitude = latitude,
        longitude = longitude,
        elevationMeters = 0.0,
        zoneId = ZoneId.of("UTC"),
    )
}