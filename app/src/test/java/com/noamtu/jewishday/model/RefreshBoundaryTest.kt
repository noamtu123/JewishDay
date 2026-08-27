// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import java.time.Duration
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The header's name appears and disappears on an observance's own entry and exit, so the screen has
 * to recompute at those instants. None of them lands on sunset or midnight, which is all the ticker
 * used to know about — a screen left open would have shown the previous state for hours.
 *
 * Fixed dates (Jerusalem): 2026-09-04 Friday, 2026-09-05 Shabbat.
 */
class RefreshBoundaryTest {
    private val location = defaultJerusalemLocation
    private val settings = ZmanimCalculationSettings()
    private val friday = LocalDate.of(2026, 9, 4)
    private val saturday = LocalDate.of(2026, 9, 5)

    private fun refreshAfter(from: java.time.Instant) =
        nextZmanimRefreshBoundary(location, settings, from)

    @Test
    fun theScreenRefreshesWhenShabbatComesIn() {
        val entry = requireNotNull(
            zmanimForDate(
                date = saturday,
                now = requireNotNull(sunsetForDate(date = saturday)).minus(Duration.ofHours(5)),
            ).holyDayInfo?.startTime,
        )

        val refresh = refreshAfter(entry.minus(Duration.ofMinutes(30)))
        assertTrue("$refresh", !refresh.isBefore(entry))
        // Just past the entry, not the following sunset twenty minutes later.
        assertTrue("$refresh", refresh.isBefore(entry.plus(Duration.ofMinutes(1))))
    }

    @Test
    fun theScreenRefreshesWhenShabbatGoesOut() {
        val exit = requireNotNull(holyDayExitForDate(date = saturday))

        val refresh = refreshAfter(exit.minus(Duration.ofMinutes(2)))
        assertTrue("$refresh", !refresh.isBefore(exit))
        assertTrue("$refresh", refresh.isBefore(exit.plus(Duration.ofMinutes(1))))

        // And that tick is where the name clears.
        assertEquals(null, zmanimForDate(date = saturday, now = refresh).holyDayInfo)
    }

    @Test
    fun theScreenRefreshesWhenADawnFastBeginsAndEnds() {
        val fastDay = LocalDate.of(2026, 7, 2)
        val fast = requireNotNull(
            zmanimForDate(
                date = fastDay,
                now = requireNotNull(tzeitForDate(date = fastDay)).minus(Duration.ofHours(5)),
            ).fastDayInfo,
        )
        val alot = requireNotNull(fast.startTime)
        val tzeit = requireNotNull(fast.endTime)

        val onEntry = refreshAfter(alot.minus(Duration.ofHours(1)))
        assertTrue("$onEntry", !onEntry.isBefore(alot) && onEntry.isBefore(alot.plus(Duration.ofMinutes(1))))

        val onExit = refreshAfter(tzeit.minus(Duration.ofMinutes(2)))
        assertTrue("$onExit", !onExit.isBefore(tzeit) && onExit.isBefore(tzeit.plus(Duration.ofMinutes(1))))
    }

    @Test
    fun theParshaRollsWhenShabbatIsActuallyOutTosefetIncluded() {
        val exit = requireNotNull(holyDayExitForDate(date = saturday))
        val boundary = nextWeeklyParshaBoundary(location, settings, friday.atStartOfDay(location.zoneId).toInstant())

        // Not at bare motzei, which is the tosefet earlier.
        assertTrue("$boundary", !boundary.isBefore(exit))
    }
}
