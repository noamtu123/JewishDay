// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * A GPS fix below sea level used to crash the app: KosherJava's GeoLocation rejects a negative
 * elevation outright, and Israel has a very populated place that is 430m below it.
 */
class BelowSeaLevelTest {

    private val deadSea = JewishLocation(
        name = "Ein Gedi",
        latitude = 31.4617,
        longitude = 35.3889,
        elevationMeters = -430.0,
        zoneId = ZoneId.of("Asia/Jerusalem"),
    )

    @Test
    fun aLocationBelowSeaLevelStillProducesZmanim() {
        val calendar = complexZmanimCalendar(deadSea, LocalDate.of(2026, 8, 27), ZmanimCalculationSettings())

        assertEquals(0.0, calendar.geoLocation.elevation, 0.0)
        assertNotNull(calendar.sunrise)
        assertNotNull(calendar.sunset)
    }

    @Test
    fun aNoisyFixJustBelowZeroIsTreatedAsSeaLevel() {
        val justBelow = deadSea.copy(name = "Coast", latitude = 32.0853, longitude = 34.7818, elevationMeters = -0.4)

        val calendar = complexZmanimCalendar(justBelow, LocalDate.of(2026, 8, 27), ZmanimCalculationSettings())

        assertEquals(0.0, calendar.geoLocation.elevation, 0.0)
    }

    @Test
    fun aRealElevationIsStillUsed() {
        val jerusalem = deadSea.copy(name = "Jerusalem", latitude = 31.778, longitude = 35.2354, elevationMeters = 754.0)

        val calendar = complexZmanimCalendar(jerusalem, LocalDate.of(2026, 8, 27), ZmanimCalculationSettings())

        assertEquals(754.0, calendar.geoLocation.elevation, 0.0)
    }
}
