// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Far enough north, a summer fast ends after civil midnight: on 17 Tammuz 5786 (2 July 2026) the
 * stars are out at 00:33 in Helsinki and 00:44 in Reykjavik. Until then the fast is still on, so its
 * card must stay past midnight and clear when it ends, not at midnight.
 */
class FastEndingAfterMidnightTest {
    private val settings = ZmanimCalculationSettings()
    private val fastDate = LocalDate.of(2026, 7, 2)
    private val cities = listOf(
        JewishLocation("Helsinki", 60.1699, 24.9384, 17.0, ZoneId.of("Europe/Helsinki")),
        JewishLocation("Reykjavik", 64.1466, -21.9426, 20.0, ZoneId.of("Atlantic/Reykjavik")),
    )

    @Test
    fun theFastCardStaysUntilTheFastEndsAfterMidnight() {
        for (city in cities) {
            val end = requireNotNull(tzeitForDate(city, fastDate, settings))
            assertEquals("${city.name}: the fast ends after midnight", fastDate.plusDays(1), end.atZone(city.zoneId).toLocalDate())

            val afterMidnight = fastDate.plusDays(1).atTime(0, 5).atZone(city.zoneId).toInstant()
            val fast = zmanimDayFacts(city, fastDate.plusDays(1), settings, now = afterMidnight).fastDayInfo
            assertNotNull("${city.name}: still fasting at 00:05", fast)
            assertEquals(Fast.SeventeenthOfTammuz, fast!!.fast)
            assertEquals(end, fast.endTime)
            assertTrue(fast.isUnderWay)

            val afterEnd = end.plusSeconds(60)
            val after = zmanimDayFacts(city, fastDate.plusDays(1), settings, now = afterEnd).fastDayInfo
            assertEquals("${city.name}: gone once it ends", null, after)
        }
    }

    @Test
    fun theScreenIsRefreshedWhenTheFastEndsAfterMidnight() {
        for (city in cities) {
            val end = requireNotNull(tzeitForDate(city, fastDate, settings))
            val afterMidnight = fastDate.plusDays(1).atTime(0, 5).atZone(city.zoneId).toInstant()
            val next = nextZmanimRefreshBoundary(city, settings, afterMidnight)
            assertTrue("${city.name}: next refresh $next should not be after the fast ends at $end", !next.isAfter(end.plusSeconds(1)))
        }
    }
}
