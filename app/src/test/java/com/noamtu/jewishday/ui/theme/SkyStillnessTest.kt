// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui.theme

import com.noamtu.jewishday.model.ZmanimCalculationSettings
import com.noamtu.jewishday.model.defaultJerusalemLocation
import com.noamtu.jewishday.model.skyDayFor
import java.time.Duration
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The glass widgets skip their sky ticks while [skyStillUntil] says the sky stands still, so it must
 * say so only when every frame in between really is the same picture.
 *
 * Fixed date (Jerusalem): 2026-11-04, an ordinary Wednesday.
 */
class SkyStillnessTest {
    private val location = defaultJerusalemLocation
    private val zone = location.zoneId
    private val date = LocalDate.of(2026, 11, 4)
    private val day = skyDayFor(location, date, ZmanimCalculationSettings())

    private fun at(hour: Int, minute: Int = 0) = date.atTime(hour, minute).atZone(zone).toInstant()

    @Test
    fun lateInTheEveningTheSkyStandsStillUntilMidnight() {
        val now = requireNotNull(day.tzeit).plus(Duration.ofMinutes(90))

        assertEquals(date.plusDays(1).atStartOfDay(zone).toInstant(), skyStillUntil(now, day, zone))
    }

    @Test
    fun afterMidnightItStandsStillUntilFortyMinutesBeforeAlot() {
        assertEquals(requireNotNull(day.alot).minus(Duration.ofMinutes(40)), skyStillUntil(at(1), day, zone))
    }

    @Test
    fun whileTheSunMovesItIsNeverStill() {
        (5..17).forEach { hour -> assertNull("$hour:00", skyStillUntil(at(hour), day, zone)) }
    }

    @Test
    fun theSlowDaySkyIsRedrawnRarely() {
        assertEquals(at(12).plus(Duration.ofMinutes(20)), nextSkyRedraw(at(12), day, zone))
    }

    @Test
    fun theFastDuskIsRedrawnEveryFewMinutes() {
        val justAfterSunset = requireNotNull(day.sunset).plus(Duration.ofMinutes(1))

        assertEquals(justAfterSunset.plus(Duration.ofMinutes(5)), nextSkyRedraw(justAfterSunset, day, zone))
    }

    @Test
    fun theStillNightIsNotRedrawnUntilItEnds() {
        assertEquals(skyStillUntil(at(1), day, zone), nextSkyRedraw(at(1), day, zone))
    }

    @Test
    fun theStillNightReallyIsOneFrame() {
        val lastStill = requireNotNull(day.alot).minus(Duration.ofMinutes(41))

        assertEquals(skyAt(at(0, 1), day, zone), skyAt(lastStill, day, zone))
    }
}
