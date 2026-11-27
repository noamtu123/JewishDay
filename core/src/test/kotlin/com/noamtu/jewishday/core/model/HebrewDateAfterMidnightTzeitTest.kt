// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Far enough north in summer the stars are out only after midnight — 00:33 in Helsinki on 2 July
 * 2026. The Hebrew date rolls at that tzeit, the one belonging to the civil day the Jewish day began
 * on, so between midnight and then it is still yesterday's: the header, the status-bar icon and the
 * moments they are refreshed at all wait for it.
 */
class HebrewDateAfterMidnightTzeitTest {
    private val settings = ZmanimCalculationSettings()
    private val cities = listOf(
        JewishLocation("Helsinki", 60.1699, 24.9384, 17.0, ZoneId.of("Europe/Helsinki")),
        JewishLocation("Reykjavik", 64.1466, -21.9426, 20.0, ZoneId.of("Atlantic/Reykjavik")),
    )
    private val summer = generateSequence(LocalDate.of(2026, 6, 1)) { it.plusDays(1) }
        .takeWhile { it.isBefore(LocalDate.of(2026, 8, 1)) }
        .toList()

    /** The summer days of [city] whose tzeit falls after midnight, each with that tzeit. */
    private fun lateTzeitDays(city: JewishLocation): List<Pair<LocalDate, Instant>> = summer.mapNotNull { day ->
        val tzeit = tzeitForDate(city, day, settings) ?: return@mapNotNull null
        (day to tzeit).takeIf { tzeit.atZone(city.zoneId).toLocalDate() == day.plusDays(1) }
    }

    /** A moment after midnight but before [tzeit]. */
    private fun betweenMidnightAndTzeit(city: JewishLocation, day: LocalDate, tzeit: Instant): Instant {
        val midnight = day.plusDays(1).atStartOfDay(city.zoneId).toInstant()
        return midnight.plus(Duration.between(midnight, tzeit).dividedBy(2))
    }

    @Test
    fun theHebrewDateStaysOnYesterdayUntilYesterdaysTzeit() {
        for (city in cities) {
            val days = lateTzeitDays(city)
            assertTrue("${city.name}: some summer tzeit falls after midnight", days.size >= 7)
            for ((day, tzeit) in days) {
                val now = betweenMidnightAndTzeit(city, day, tzeit)
                assertEquals("${city.name} $now", day, jewishDayCivilDate(city, settings, now))
                assertEquals("${city.name} $now", day, zmanimDayFacts(city, day.plusDays(1), settings, now).displayedDate)

                val afterTzeit = tzeit.plusSeconds(120)
                assertEquals("${city.name} $afterTzeit", day.plusDays(1), jewishDayCivilDate(city, settings, afterTzeit))
                assertEquals("${city.name} $afterTzeit", day.plusDays(1), zmanimDayFacts(city, day.plusDays(1), settings, afterTzeit).displayedDate)
            }
        }
    }

    @Test
    fun theHeaderAndTheIconAreRefreshedAtYesterdaysTzeit() {
        for (city in cities) {
            for ((day, tzeit) in lateTzeitDays(city)) {
                val now = betweenMidnightAndTzeit(city, day, tzeit)
                val rolls = tzeit.plusSeconds(60)
                for ((name, next) in listOf(
                    "icon" to nextStatusIconBoundary(city, settings, now),
                    "date" to nextDateBoundary(city, settings, now),
                    "screen" to nextZmanimRefreshBoundary(city, settings, now),
                )) {
                    assertTrue("${city.name} $now $name: $next should not be after $rolls", !next.isAfter(rolls))
                }
            }
        }
    }

    @Test
    fun onFridayNightShabbatIsInButItsDateWaitsForTheStars() {
        for (city in cities) {
            val fridays = lateTzeitDays(city).filter { it.first.dayOfWeek == DayOfWeek.FRIDAY }
            assertTrue("${city.name}: a Friday with tzeit after midnight", fridays.isNotEmpty())
            for ((friday, tzeit) in fridays) {
                val now = betweenMidnightAndTzeit(city, friday, tzeit)
                val facts = zmanimDayFacts(city, friday.plusDays(1), settings, now)
                assertEquals("${city.name} $now: still Friday's Hebrew date", friday, facts.displayedDate)
                val shabbat = requireNotNull(facts.holyDayInfo) { "${city.name} $now" }
                assertTrue("${city.name} $now: Shabbat is in", shabbat.isUnderWay)
                assertEquals(HolyDayName.Shabbat(dayName = null), shabbat.day)
            }
        }
    }

    @Test
    fun aDayStillReadAtNoonIsUnaffected() {
        for (city in cities) {
            for ((day, _) in lateTzeitDays(city)) {
                val noon = day.plusDays(1).atTime(12, 0).atZone(city.zoneId).toInstant()
                assertEquals(day.plusDays(1), jewishDayCivilDate(city, settings, noon))
                assertEquals(day.plusDays(1), zmanimDayFacts(city, day.plusDays(1), settings, noon).displayedDate)
            }
        }
    }
}
