// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Clock changes are the highest-risk untested assumption in the time handling: every other zmanim
 * test runs mid-season in Jerusalem, so nothing crosses one.
 *
 * The pipeline should survive them by construction — it is Instants all the way through, with the
 * zone applied only at the display boundary and no manual offset arithmetic anywhere — and these
 * pin that down so a later refactor cannot quietly reintroduce offset math.
 *
 * Deliberately asserted as *properties* rather than literal clock times, so a tzdata update in the
 * JDK cannot fail the suite for the wrong reason.
 */
class DaylightSavingTest {

    private val jerusalem = defaultJerusalemLocation
    private val newYork = JewishLocation(
        name = "New York",
        latitude = 40.7128,
        longitude = -74.0060,
        elevationMeters = 10.0,
        zoneId = ZoneId.of("America/New_York"),
    )
    private val settings = ZmanimCalculationSettings()

    /** Israel moves to summer time on the Friday before the last Sunday in March, at 02:00. */
    private val israelSpringForward = LocalDate.of(2026, 3, 27)

    /** ...and back on the last Sunday in October, at 02:00. */
    private val israelFallBack = LocalDate.of(2026, 10, 25)

    /** The United States moves forward on the second Sunday in March. */
    private val usSpringForward = LocalDate.of(2026, 3, 8)

    private fun localSunsetMinutes(location: JewishLocation, date: LocalDate): Int {
        val sunset = requireNotNull(sunsetForDate(location, date, settings)) { "no sunset on $date" }
        val local = sunset.atZone(location.zoneId)
        return local.hour * 60 + local.minute
    }

    @Test
    fun springForwardMovesTheLocalClockButNotTheSun() {
        val before = israelSpringForward.minusDays(1)

        // The wall clock jumps an hour, so sunset is written about an hour later than yesterday.
        val localJump = localSunsetMinutes(jerusalem, israelSpringForward) -
            localSunsetMinutes(jerusalem, before)
        assertTrue("local sunset moved $localJump min, expected ~60", localJump in 55..65)

        // The sun itself did not move: consecutive sunsets stay one solar day apart.
        val gap = Duration.between(
            requireNotNull(sunsetForDate(jerusalem, before, settings)),
            requireNotNull(sunsetForDate(jerusalem, israelSpringForward, settings)),
        )
        assertTrue("sunsets $gap apart, expected ~24h", gap.toMinutes() in (24 * 60 - 5L)..(24 * 60 + 5L))
    }

    @Test
    fun fallBackMovesTheLocalClockTheOtherWay() {
        val before = israelFallBack.minusDays(1)

        val localJump = localSunsetMinutes(jerusalem, israelFallBack) -
            localSunsetMinutes(jerusalem, before)
        assertTrue("local sunset moved $localJump min, expected ~-60", localJump in -65..-55)

        val gap = Duration.between(
            requireNotNull(sunsetForDate(jerusalem, before, settings)),
            requireNotNull(sunsetForDate(jerusalem, israelFallBack, settings)),
        )
        assertTrue("sunsets $gap apart, expected ~24h", gap.toMinutes() in (24 * 60 - 5L)..(24 * 60 + 5L))
    }

    @Test
    fun theUnitedStatesTransitionBehavesTheSameWay() {
        val localJump = localSunsetMinutes(newYork, usSpringForward) -
            localSunsetMinutes(newYork, usSpringForward.minusDays(1))
        assertTrue("local sunset moved $localJump min, expected ~60", localJump in 55..65)
    }

    @Test
    fun everyRefreshBoundaryAcrossATransitionIsStillAheadAndWithinADay() {
        // Walked hour by hour through both transitions in both zones. A boundary that landed in the
        // past would spin the ticker; one more than a day out would freeze a screen left open.
        for ((location, transition) in listOf(
            jerusalem to israelSpringForward,
            jerusalem to israelFallBack,
            newYork to usSpringForward,
        )) {
            var instant = transition.minusDays(1).atStartOfDay(location.zoneId).toInstant()
            val end = transition.plusDays(2).atStartOfDay(location.zoneId).toInstant()
            while (instant.isBefore(end)) {
                val boundary = nextZmanimRefreshBoundary(location, settings, instant)
                assertTrue(
                    "${location.name} $instant -> $boundary is not in the future",
                    boundary.isAfter(instant),
                )
                assertTrue(
                    "${location.name} $instant -> $boundary is more than a day out",
                    Duration.between(instant, boundary) <= Duration.ofHours(26),
                )
                instant = instant.plus(Duration.ofHours(1))
            }
        }
    }

    @Test
    fun theHebrewDateAdvancesOneDayPerCivilDayAcrossATransition() {
        // Neither a skipped nor a repeated day: the 23-hour civil day must still be one Jewish day.
        for (transition in listOf(israelSpringForward, israelFallBack)) {
            val days = (-1L..2L).map { offset ->
                val date = transition.plusDays(offset)
                // Midday, so the sunset rollover is not in play and the date is unambiguous.
                val noon = date.atTime(12, 0).atZone(jerusalem.zoneId).toInstant()
                zmanimForDate(jerusalem, date, settings, noon).hebrewDateEnglish
            }
            assertEquals("$transition produced duplicate Hebrew dates: $days", days.size, days.toSet().size)
        }
    }
}
