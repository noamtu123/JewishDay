// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

import java.time.Duration
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/** The step a widget tap takes from today's screen to the day it shows (Jerusalem). */
class DayOffsetTest {
    private val settings = ZmanimCalculationSettings()
    private val location = defaultJerusalemLocation
    private val friday = LocalDate.of(2026, 9, 4)
    private val saturday = friday.plusDays(1)
    private val fridayTzeit = requireNotNull(tzeitForDate(location, friday, settings))

    private fun offset(date: LocalDate, now: java.time.Instant) = dayOffsetTo(date, location, settings, now)

    @Test
    fun shabbatIsOneStepAwayOnFridayAfternoon() {
        assertEquals(1, offset(saturday, fridayTzeit.minus(Duration.ofHours(2))))
    }

    @Test
    fun afterTzeitShabbatIsAlreadyToday() {
        assertEquals(0, offset(saturday, fridayTzeit.plus(Duration.ofMinutes(5))))
    }

    @Test
    fun aDayThatHasBegunOrPassedOpensOnToday() {
        val noon = friday.atTime(12, 0).atZone(location.zoneId).toInstant()
        assertEquals(0, offset(friday, noon))
        assertEquals(0, offset(friday.minusDays(3), noon))
        // After tzeit the screen already shows tomorrow's Hebrew date, so today's civil day is behind it.
        assertEquals(0, offset(friday, fridayTzeit.plus(Duration.ofMinutes(5))))
    }

    @Test
    fun aDayFurtherOffCountsWholeDays() {
        val noon = friday.atTime(12, 0).atZone(location.zoneId).toInstant()
        assertEquals(8, offset(saturday.plusDays(7), noon))
    }
}
