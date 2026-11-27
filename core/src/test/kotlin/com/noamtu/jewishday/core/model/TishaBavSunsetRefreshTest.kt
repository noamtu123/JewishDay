// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

import java.time.Duration
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tisha B'Av begins at sunset, and the header names the fast from that moment. A screen left open
 * on Erev Tisha B'Av must be refreshed then, not only at tzeit half an hour later.
 */
class TishaBavSunsetRefreshTest {
    private val settings = ZmanimCalculationSettings()

    // 9 Av 5786 is Thursday 23 July 2026; 10 Av 5789 is Sunday 22 July 2029, pushed from Shabbat.
    private val eves = listOf(LocalDate.of(2026, 7, 22), LocalDate.of(2029, 7, 21))

    @Test
    fun theScreenIsRefreshedAtTheSunsetTheFastBeginsAt() {
        for (eve in eves) {
            val sunset = requireNotNull(sunsetForDate(defaultJerusalemLocation, eve, settings))
            val before = sunset.minus(Duration.ofMinutes(5))
            val fastBefore = zmanimDayFacts(defaultJerusalemLocation, eve, settings, now = before).fastDayInfo
            assertFalse("$eve: not yet under way", fastBefore?.isUnderWay ?: false)
            val fastAfter = zmanimDayFacts(defaultJerusalemLocation, eve, settings, now = sunset.plusSeconds(2)).fastDayInfo
            assertTrue("$eve: under way from sunset", fastAfter?.isUnderWay == true)

            val next = nextZmanimRefreshBoundary(defaultJerusalemLocation, settings, before)
            assertTrue("$eve: next refresh $next should be by sunset $sunset", !next.isAfter(sunset.plusSeconds(1)))
        }
    }
}
