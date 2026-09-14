// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import java.time.Duration
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the day boundaries: the displayed Hebrew date rolls at tzeit while the day whose zmanim
 * are on screen rolls at chatzot halaila, and the fast
 * chip/card follows the fast — announced one Jewish day before it begins and cleared the moment it
 * ends — including the Erev Tisha B'Av / Erev Yom Kippur evenings, when the fast begins at sunset
 * before its civil calendar date.
 *
 * Fixed dates (Jerusalem): 2026-07-02 = 17 Tammuz 5786 (minor fast),
 * 2026-07-23 = 9 Av 5786, so 2026-07-22 is Erev Tisha B'Av.
 */
class FastDayAndRolloverTest {
    private val zone = defaultJerusalemLocation.zoneId
    private val seventeenTammuz = LocalDate.of(2026, 7, 2)
    private val erevTishaBeAv = LocalDate.of(2026, 7, 22)
    private val tishaBeAv = LocalDate.of(2026, 7, 23)

    @Test
    fun minorFastRunsFromDawnToTzeitOnItsCivilDay() {
        val day = zmanimForDate(date = seventeenTammuz)
        val fast = requireNotNull(day.fastDayInfo)

        assertEquals("י״ז בתמוז", fast.nameHebrew)
        val start = requireNotNull(fast.startTime)
        val end = requireNotNull(fast.endTime)
        assertTrue(start.isBefore(end))
        // Dawn start: same civil day, early morning local time.
        assertEquals(seventeenTammuz, start.atZone(zone).toLocalDate())
        assertTrue(start.atZone(zone).hour < 7)
        // Tzeit end: after that day's sunset.
        val sunset = requireNotNull(sunsetForDate(date = seventeenTammuz))
        assertTrue(end.isAfter(sunset))
    }

    @Test
    fun tishaBeAvStartsAtThePreviousEveningSunset() {
        val day = zmanimForDate(date = tishaBeAv)
        val fast = requireNotNull(day.fastDayInfo)

        assertEquals("תשעה באב", fast.nameHebrew)
        val start = requireNotNull(fast.startTime)
        assertEquals(erevTishaBeAv, start.atZone(zone).toLocalDate())
        assertEquals(sunsetForDate(date = erevTishaBeAv), fast.startTime)
    }

    @Test
    fun fastCardIsAlreadyShowingOnErevTishaBeAvBeforeTheFastBegins() {
        val erevSunset = requireNotNull(sunsetForDate(date = erevTishaBeAv))

        // Announced a Jewish day ahead, so it is up throughout the eve, before the fast starts.
        val beforeSunset = zmanimForDate(date = erevTishaBeAv, now = erevSunset.minus(Duration.ofHours(2)))
        assertEquals("תשעה באב", requireNotNull(beforeSunset.fastDayInfo).nameHebrew)

        val afterSunset = zmanimForDate(date = erevTishaBeAv, now = erevSunset.plus(Duration.ofMinutes(30)))
        val fast = requireNotNull(afterSunset.fastDayInfo)
        assertEquals("תשעה באב", fast.nameHebrew)
        assertEquals(erevSunset, fast.startTime)
    }

    @Test
    fun eveningFastIsAnnouncedFromTheSunsetBeforeItsEve() {
        val dayBeforeErev = erevTishaBeAv.minusDays(1)
        val announceSunset = requireNotNull(sunsetForDate(date = dayBeforeErev))

        val tooEarly = zmanimForDate(date = dayBeforeErev, now = announceSunset.minus(Duration.ofHours(2)))
        assertNull(tooEarly.fastDayInfo)

        val announced = zmanimForDate(date = dayBeforeErev, now = announceSunset.plus(Duration.ofMinutes(30)))
        assertEquals("תשעה באב", requireNotNull(announced.fastDayInfo).nameHebrew)
    }

    @Test
    fun dawnFastIsAnnouncedFromAlotOfThePreviousMorning() {
        val dayBefore = seventeenTammuz.minusDays(1)
        val previousAlot = requireNotNull(
            zmanimForDate(date = dayBefore).groups
                .flatMap { it.items }
                .first { it.title == "Alot Hashachar" }
                .time,
        )

        val beforeDawn = zmanimForDate(date = dayBefore, now = previousAlot.minus(Duration.ofHours(1)))
        assertNull(beforeDawn.fastDayInfo)

        val afterDawn = zmanimForDate(date = dayBefore, now = previousAlot.plus(Duration.ofMinutes(30)))
        assertEquals("י״ז בתמוז", requireNotNull(afterDawn.fastDayInfo).nameHebrew)
    }

    @Test
    fun fastCardClearsAfterTheFastEndsInsteadOfLingeringUntilMidnight() {
        val fastEnd = requireNotNull(zmanimForDate(date = seventeenTammuz).fastDayInfo?.endTime)

        // Between sunset and tzeit the fast is still on, so the card must stay.
        val duringBeinHashmashot = zmanimForDate(date = seventeenTammuz, now = fastEnd.minus(Duration.ofMinutes(5)))
        assertNotNull(duringBeinHashmashot.fastDayInfo)

        val afterTzeit = zmanimForDate(date = seventeenTammuz, now = fastEnd.plus(Duration.ofMinutes(30)))
        assertNull(afterTzeit.fastDayInfo)
    }

    @Test
    fun displayedHebrewDateRollsAtTzeitNotSunset() {
        val sunset = requireNotNull(sunsetForDate(date = seventeenTammuz))
        val tzeit = requireNotNull(tzeitForDate(date = seventeenTammuz))

        val beforeSunset = zmanimForDate(date = seventeenTammuz, now = sunset.minus(Duration.ofHours(1)))
        assertTrue(beforeSunset.hebrewDateEnglish, beforeSunset.hebrewDateEnglish.contains("17 Tammuz"))

        // Bein hashmashot — past sunset, before the stars are out — is still the old date.
        val beinHashmashot = zmanimForDate(date = seventeenTammuz, now = sunset.plus(Duration.ofMinutes(1)))
        assertTrue(beinHashmashot.hebrewDateEnglish, beinHashmashot.hebrewDateEnglish.contains("17 Tammuz"))

        val afterTzeit = zmanimForDate(date = seventeenTammuz, now = tzeit.plus(Duration.ofMinutes(1)))
        assertTrue(afterTzeit.hebrewDateEnglish, afterTzeit.hebrewDateEnglish.contains("18 Tammuz"))
    }

    @Test
    fun nextDateBoundaryIsTzeitDuringTheDayThenMidnightThenChatzotHaLaila() {
        val settings = ZmanimCalculationSettings()
        val tzeit = requireNotNull(tzeitForDate(date = seventeenTammuz))
        val chatzotHaLaila = requireNotNull(chatzotHaLailaForDate(date = seventeenTammuz))

        val atNoon = seventeenTammuz.atTime(12, 0).atZone(zone).toInstant()
        assertEquals(
            tzeit.plus(Duration.ofMinutes(1)),
            nextDateBoundary(defaultJerusalemLocation, settings, atNoon),
        )

        // Late at night civil midnight comes first — the times turn over there.
        val lateNight = seventeenTammuz.atTime(23, 30).atZone(zone).toInstant()
        assertEquals(
            nextGregorianMidnight(defaultJerusalemLocation, lateNight),
            nextDateBoundary(defaultJerusalemLocation, settings, lateNight),
        )

        // Just past midnight, chatzot halaila (after 00:00 in Jerusalem) is still ahead.
        val pastMidnight = seventeenTammuz.plusDays(1).atTime(0, 5).atZone(zone).toInstant()
        assertEquals(
            chatzotHaLaila.plus(Duration.ofMinutes(1)),
            nextDateBoundary(defaultJerusalemLocation, settings, pastMidnight),
        )
    }

    @Test
    fun theHebrewDateCivilDateRollsAtTzeit() {
        val settings = ZmanimCalculationSettings()
        val tzeit = requireNotNull(tzeitForDate(date = seventeenTammuz))

        // Before nightfall the Hebrew date is still today's.
        assertEquals(
            seventeenTammuz,
            jewishDayCivilDate(defaultJerusalemLocation, settings, tzeit.minus(Duration.ofMinutes(5))),
        )
        // After it, and for the whole evening, the Hebrew date is tomorrow's.
        assertEquals(
            seventeenTammuz.plusDays(1),
            jewishDayCivilDate(defaultJerusalemLocation, settings, tzeit.plus(Duration.ofMinutes(5))),
        )
        val evening = seventeenTammuz.atTime(21, 0).atZone(zone).toInstant()
        assertEquals(seventeenTammuz.plusDays(1), jewishDayCivilDate(defaultJerusalemLocation, settings, evening))
        // Still the same day after civil midnight: the Jewish day turns at the next tzeit.
        val afterMidnight = seventeenTammuz.plusDays(1).atTime(0, 15).atZone(zone).toInstant()
        assertEquals(seventeenTammuz.plusDays(1), jewishDayCivilDate(defaultJerusalemLocation, settings, afterMidnight))
    }

    @Test
    fun theChatzotHaLailaRowIsAlwaysTheMidnightStillAhead() {
        val settings = ZmanimCalculationSettings()
        val tonight = requireNotNull(chatzotHaLailaForDate(date = seventeenTammuz))
        // Solar midnight in Jerusalem lands after civil midnight, on the following civil day.
        assertEquals(seventeenTammuz.plusDays(1), tonight.atZone(zone).toLocalDate())

        // In the evening the Hebrew date has rolled to tomorrow, but this row must still answer
        // "when is midnight tonight".
        val evening = seventeenTammuz.atTime(21, 0).atZone(zone).toInstant()
        assertEquals(tonight, upcomingChatzotHaLaila(defaultJerusalemLocation, settings, evening))

        // During the day it is the coming night's, as it always was.
        val noon = seventeenTammuz.atTime(12, 0).atZone(zone).toInstant()
        assertEquals(tonight, upcomingChatzotHaLaila(defaultJerusalemLocation, settings, noon))

        // And once tonight's has passed, the next one.
        val past = tonight.plus(Duration.ofMinutes(5))
        assertEquals(
            chatzotHaLailaForDate(date = seventeenTammuz.plusDays(1)),
            upcomingChatzotHaLaila(defaultJerusalemLocation, settings, past),
        )
    }

    @Test
    fun theEveningListStaysOnTodayWhileTheMidnightRowLooksAhead() {
        val location = defaultJerusalemLocation
        val settings = ZmanimCalculationSettings()
        val evening = seventeenTammuz.atTime(21, 0).atZone(zone).toInstant()
        val day = zmanimForDate(location, seventeenTammuz, settings, evening)

        // The times do not move under you after nightfall — the stepper is how you reach tomorrow.
        assertEquals(seventeenTammuz, day.date)
        // Only the Hebrew date has rolled.
        assertEquals(seventeenTammuz.plusDays(1), day.displayedDate)

        val chatzotHaLaila = day.groups.flatMap { it.items }.first { it.title == "Chatzot HaLaila" }.time
        assertEquals(chatzotHaLailaForDate(location, seventeenTammuz, settings), chatzotHaLaila)
    }

    @Test
    fun theMidnightRowIsTheComingOneEvenInTheSmallHours() {
        val location = defaultJerusalemLocation
        val settings = ZmanimCalculationSettings()
        val tonight = requireNotNull(chatzotHaLailaForDate(location, seventeenTammuz, settings))

        // 00:15 on 3 July: the calendar day has turned, but chatzot halaila is still 25 minutes
        // away. Reading the row off the new calendar day alone would put it a whole day out.
        val smallHours = seventeenTammuz.plusDays(1).atTime(0, 15).atZone(zone).toInstant()
        val day = zmanimForDate(location, seventeenTammuz.plusDays(1), settings, smallHours)
        val chatzotHaLaila = day.groups.flatMap { it.items }.first { it.title == "Chatzot HaLaila" }.time

        assertEquals(tonight, chatzotHaLaila)
        assertTrue("$chatzotHaLaila", requireNotNull(chatzotHaLaila).isAfter(smallHours))
    }
}