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
    fun nextDateBoundaryIsTzeitDuringTheDayAndChatzotHaLailaAtNight() {
        val settings = ZmanimCalculationSettings()
        val tzeit = requireNotNull(tzeitForDate(date = seventeenTammuz))
        val chatzotHaLaila = requireNotNull(chatzotHaLailaForDate(date = seventeenTammuz))

        val atNoon = seventeenTammuz.atTime(12, 0).atZone(zone).toInstant()
        assertEquals(
            tzeit.plus(Duration.ofMinutes(1)),
            nextDateBoundary(defaultJerusalemLocation, settings, atNoon),
        )

        // Late at night the next boundary is chatzot halaila, not civil midnight.
        val lateNight = seventeenTammuz.atTime(23, 30).atZone(zone).toInstant()
        assertEquals(
            chatzotHaLaila.plus(Duration.ofMinutes(1)),
            nextDateBoundary(defaultJerusalemLocation, settings, lateNight),
        )
    }

    @Test
    fun theDisplayedDayRollsAtChatzotHaLailaRatherThanCivilMidnight() {
        val settings = ZmanimCalculationSettings()
        val chatzotHaLaila = requireNotNull(chatzotHaLailaForDate(date = seventeenTammuz))
        // Solar midnight in Jerusalem falls after civil midnight, so there is a window on the next
        // civil day that still belongs to 2 July.
        assertTrue("$chatzotHaLaila", chatzotHaLaila.atZone(zone).toLocalDate() == seventeenTammuz.plusDays(1))

        val justBefore = chatzotHaLaila.minus(Duration.ofMinutes(5))
        val justAfter = chatzotHaLaila.plus(Duration.ofMinutes(5))

        assertEquals(seventeenTammuz, zmanimDateFor(defaultJerusalemLocation, settings, justBefore))
        assertEquals(seventeenTammuz.plusDays(1), zmanimDateFor(defaultJerusalemLocation, settings, justAfter))

        // And in the evening, before civil midnight, the day has not moved on yet.
        val evening = seventeenTammuz.atTime(21, 0).atZone(zone).toInstant()
        assertEquals(seventeenTammuz, zmanimDateFor(defaultJerusalemLocation, settings, evening))
    }
}