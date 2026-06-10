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
}
