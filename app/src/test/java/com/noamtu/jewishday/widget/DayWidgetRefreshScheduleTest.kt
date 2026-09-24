// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import com.noamtu.jewishday.model.JewishLocation
import com.noamtu.jewishday.model.ZmanimCalculationSettings
import com.noamtu.jewishday.model.defaultJerusalemLocation
import com.noamtu.jewishday.model.skyDayFor
import com.noamtu.jewishday.model.tzeitForDate
import com.noamtu.jewishday.model.zmanimForDate
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The widget's refresh schedule is pure arithmetic over the day's sky and its content boundaries, so
 * it is pinned here: quarter-hour ticks while the sun moves, none through the still night, a content
 * boundary winning with an exact alarm, only the widget's own moments counting as boundaries, and a
 * floor of a minute so an alarm never re-arms itself into a loop.
 *
 * Fixed dates (Jerusalem): 2026-11-04, an ordinary Wednesday with nothing coming in or going out, and
 * the Friday after it.
 */
class DayWidgetRefreshScheduleTest {
    private val location = defaultJerusalemLocation
    private val zone = location.zoneId
    private val settings = ZmanimCalculationSettings()
    private val ordinaryWednesday = LocalDate.of(2026, 11, 4)
    private val friday = LocalDate.of(2026, 11, 6)

    private fun at(hour: Int, minute: Int, date: LocalDate = ordinaryWednesday): Instant =
        date.atTime(hour, minute).atZone(zone).toInstant()

    private fun refreshAfter(now: Instant, where: JewishLocation = location): DayWidgetRefresh =
        nextDayWidgetRefresh(where, settings, now)

    private fun contentBoundaryAfter(now: Instant, where: JewishLocation = location): Instant =
        nextDayWidgetContentBoundary(where, settings, now)

    /** Forty minutes before alot, where the Glass sky starts to lighten. */
    private fun firstLightOf(date: LocalDate): Instant =
        requireNotNull(skyDayFor(location, date, settings).alot).minus(Duration.ofMinutes(40))

    private fun tzeitOf(date: LocalDate): Instant = requireNotNull(tzeitForDate(location, date, settings))

    private fun midnightAfter(date: LocalDate): Instant = date.plusDays(1).atStartOfDay(zone).toInstant()

    @Test
    fun byDayTheSkyTicksOnTheNextQuarterHourInexactly() {
        val now = at(10, 7)

        val refresh = refreshAfter(now)

        assertEquals(at(10, 15), refresh.at)
        assertFalse(refresh.exact)
        assertTrue(Duration.between(now, refresh.at) <= Duration.ofMinutes(15))
    }

    @Test
    fun theStillNightHasNoTicksOfItsOwnAndWaitsForFirstLight() {
        val now = at(2, 20)
        // Midnight has passed and nothing else changes before dawn, so nothing forces an earlier wake-up.
        assertTrue(contentBoundaryAfter(now).isAfter(firstLightOf(ordinaryWednesday)))

        val refresh = refreshAfter(now)

        assertEquals(firstLightOf(ordinaryWednesday), refresh.at)
        assertFalse(refresh.exact)
    }

    @Test
    fun afterDuskTheNextWakeUpIsMidnightsDateBoundaryThenTomorrowsFirstLight() {
        val settled = tzeitOf(ordinaryWednesday).plus(Duration.ofMinutes(61))

        val overnight = refreshAfter(settled)
        assertEquals(midnightAfter(ordinaryWednesday).plus(Duration.ofMinutes(1)), overnight.at)
        assertTrue(overnight.exact)

        val smallHours = refreshAfter(midnightAfter(ordinaryWednesday).plus(Duration.ofMinutes(2)))
        assertEquals(firstLightOf(ordinaryWednesday.plusDays(1)), smallHours.at)
        assertFalse(smallHours.exact)
    }

    @Test
    fun theNightBeforeDawnEndsWhereTheSkyBeginsToLightenNotOnTheHour() {
        // A winter day whose first light falls well inside its hour.
        val firstLight = generateSequence(LocalDate.of(2026, 12, 1)) { it.plusDays(1) }
            .map(::firstLightOf)
            .first { it.atZone(zone).minute in 20..40 }
        val now = firstLight.atZone(zone).truncatedTo(ChronoUnit.HOURS).plusMinutes(1).toInstant()

        val refresh = refreshAfter(now)

        assertEquals(firstLight, refresh.at)
        assertFalse(refresh.exact)
    }

    @Test
    fun aContentBoundaryWinsWithAnExactAlarm() {
        val now = tzeitOf(ordinaryWednesday).minus(Duration.ofMinutes(2))

        val refresh = refreshAfter(now)

        assertEquals(contentBoundaryAfter(now), refresh.at)
        assertEquals(tzeitOf(ordinaryWednesday).plus(Duration.ofMinutes(1)), refresh.at)
        assertTrue(refresh.exact)
    }

    @Test
    fun onAnOrdinaryDayOnlyTzeitAndMidnightAreContentBoundaries() {
        // Not alot, not candle lighting, not the daily holy-day exit, not chatzot halaila: the zmanim
        // screen refreshes for those, the widget shows none of them on a weekday.
        assertEquals(tzeitOf(ordinaryWednesday).plus(Duration.ofMinutes(1)), contentBoundaryAfter(at(12, 0)))
        assertEquals(
            midnightAfter(ordinaryWednesday).plus(Duration.ofMinutes(1)),
            contentBoundaryAfter(tzeitOf(ordinaryWednesday).plus(Duration.ofMinutes(2))),
        )
    }

    @Test
    fun aComingShabbatsEntryIsAContentBoundary() {
        val now = at(12, 0, friday)
        val announced = requireNotNull(zmanimForDate(location, friday, settings, now).holyDayInfo)
        val entry = requireNotNull(announced.startTime)
        assertTrue(entry.isBefore(tzeitOf(friday)))

        assertEquals(entry.plusSeconds(1), contentBoundaryAfter(now))
        assertTrue(refreshAfter(entry.minus(Duration.ofMinutes(3))).exact)
    }

    @Test
    fun theRefreshIsNeverLaterThanTheContentBoundaryNorSoonerThanAMinute() {
        val tzeit = tzeitOf(ordinaryWednesday)
        val midnight = midnightAfter(ordinaryWednesday)
        val sevenPastEveryQuarter = (0 until 24 * 4).map { at(it / 4, it % 4 * 15 + 7) }
        val edges = listOf(
            tzeit.minusSeconds(90),
            // The boundary is under a minute away, so the floor pushes the refresh just past it.
            tzeit.minusSeconds(30),
            tzeit.plusSeconds(30),
            midnight.minusSeconds(30),
            midnight.plusSeconds(30),
        )

        (sevenPastEveryQuarter + edges).forEach { now ->
            val refresh = refreshAfter(now)
            val latest = maxOf(contentBoundaryAfter(now), now.plusSeconds(60))
            assertTrue("$now -> $refresh", refresh.at.isAfter(now.plusSeconds(59)))
            assertTrue("$now -> $refresh, latest $latest", !refresh.at.isAfter(latest))
            // Whichever wins is what the exactness says.
            assertEquals("$now -> $refresh, latest $latest", refresh.at == latest, refresh.exact)
        }
    }

    @Test
    fun theMidnightSunDoesNotBreakTheSchedule() {
        val tromso = JewishLocation(
            name = "Tromsø",
            latitude = 69.65,
            longitude = 18.96,
            elevationMeters = 0.0,
            zoneId = ZoneId.of("Europe/Oslo"),
        )
        val midsummer = LocalDate.of(2026, 6, 21)

        (0 until 24 step 3).forEach { hour ->
            val now = midsummer.atTime(hour, 5).atZone(tromso.zoneId).toInstant()
            val refresh = refreshAfter(now, tromso)
            val latest = maxOf(contentBoundaryAfter(now, tromso), now.plusSeconds(60))
            assertTrue("$now -> $refresh", refresh.at.isAfter(now.plusSeconds(59)))
            assertTrue("$now -> $refresh, latest $latest", !refresh.at.isAfter(latest))
        }
    }
}
