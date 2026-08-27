// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import java.time.Duration
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Shabbat gets the same header treatment as a fast: announced one Jewish day before it enters —
 * i.e. from Thursday sunset, when the displayed date rolls to Friday — and cleared at motzei
 * Shabbat, not at the sunset before it.
 *
 * Fixed dates (Jerusalem): 2026-09-03 Thursday … 2026-09-06 Sunday.
 */
class ShabbatHeaderTest {
    private val thursday = LocalDate.of(2026, 9, 3)
    private val friday = LocalDate.of(2026, 9, 4)
    private val saturday = LocalDate.of(2026, 9, 5)
    private val sunday = LocalDate.of(2026, 9, 6)

    @Test
    fun hiddenEarlierInTheWeek() {
        val thursdaySunset = requireNotNull(sunsetForDate(date = thursday))
        assertNull(zmanimForDate(date = thursday, now = thursdaySunset.minus(Duration.ofHours(3))).holyDayInfo)
        val wednesday = thursday.minusDays(1)
        val wednesdaySunset = requireNotNull(sunsetForDate(date = wednesday))
        assertNull(zmanimForDate(date = wednesday, now = wednesdaySunset.plus(Duration.ofMinutes(30))).holyDayInfo)
    }

    @Test
    fun appearsAtThursdaySunsetWithEntryAndExitTimes() {
        val thursdaySunset = requireNotNull(sunsetForDate(date = thursday))
        val info = requireNotNull(
            zmanimForDate(date = thursday, now = thursdaySunset.plus(Duration.ofMinutes(30))).holyDayInfo,
        )

        // Entry is Friday's candle lighting; exit is Saturday's motzei Shabbat.
        val entry = requireNotNull(info.startTime)
        val exit = requireNotNull(info.endTime)
        assertEquals(friday, entry.atZone(defaultJerusalemLocation.zoneId).toLocalDate())
        assertEquals(saturday, exit.atZone(defaultJerusalemLocation.zoneId).toLocalDate())
        assertEquals(holyDayExitForDate(date = saturday), exit)
    }

    @Test
    fun staysUpThroughFridayAndShabbatItself() {
        assertNotNull(zmanimForDate(date = friday, now = requireNotNull(sunsetForDate(date = friday)).minus(Duration.ofHours(4))).holyDayInfo)
        assertNotNull(zmanimForDate(date = saturday, now = requireNotNull(sunsetForDate(date = saturday)).minus(Duration.ofHours(4))).holyDayInfo)
    }

    @Test
    fun survivesSaturdaySunsetAndClearsOnlyAtMotzei() {
        val motzei = requireNotNull(holyDayExitForDate(date = saturday))
        val saturdaySunset = requireNotNull(sunsetForDate(date = saturday))

        // Between sunset and motzei the displayed date has already rolled to Sunday, but Shabbat
        // is still out, so the header must stay.
        assertNotNull(zmanimForDate(date = saturday, now = saturdaySunset.plus(Duration.ofMinutes(5))).holyDayInfo)
        assertNotNull(zmanimForDate(date = saturday, now = motzei.minus(Duration.ofMinutes(2))).holyDayInfo)

        assertNull(zmanimForDate(date = saturday, now = motzei.plus(Duration.ofMinutes(5))).holyDayInfo)
        assertNull(zmanimForDate(date = sunday, now = motzei.plus(Duration.ofHours(12))).holyDayInfo)
    }
}
