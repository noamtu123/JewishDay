// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import com.noamtu.jewishday.model.ZmanimCalculationSettings
import com.noamtu.jewishday.model.chatzotHaLailaForDate
import com.noamtu.jewishday.model.defaultJerusalemLocation
import com.noamtu.jewishday.model.nextGregorianMidnight
import com.noamtu.jewishday.model.nextStatusIconBoundary
import com.noamtu.jewishday.model.sunsetForDate
import com.noamtu.jewishday.model.tzeitForDate
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultJewishDayRepositoryTest {
    @Test
    fun getTodayUsesInjectedClockDate() {
        val repository = DefaultJewishDayRepository(
            Clock.fixed(Instant.parse("2024-04-23T10:00:00Z"), ZoneId.of("UTC")),
        )

        assertEquals(LocalDate.of(2024, 4, 23), repository.getToday().gregorianDate)
    }

    @Test
    fun getZmanimUsesInjectedClockDateInLocationZone() {
        val repository = DefaultJewishDayRepository(
            Clock.fixed(Instant.parse("2024-04-22T22:30:00Z"), ZoneId.of("UTC")),
        )

        assertEquals(LocalDate.of(2024, 4, 23), repository.getZmanim(defaultJerusalemLocation).date)
    }

    @Test
    fun hebrewDateForStatusIconRollsOverAtTzeit() {
        // 2026-07-02 is 17 Tammuz 5786 in Jerusalem; the Hebrew date flips at tzeit, not sunset.
        val zone = defaultJerusalemLocation.zoneId
        val sunset = requireNotNull(sunsetForDate(date = LocalDate.of(2026, 7, 2)))
        val tzeit = requireNotNull(tzeitForDate(date = LocalDate.of(2026, 7, 2)))

        val beforeSunset = DefaultJewishDayRepository(Clock.fixed(sunset.minusSeconds(3_600), zone))
            .getToday(defaultJerusalemLocation)
        val beinHashmashot = DefaultJewishDayRepository(Clock.fixed(sunset.plusSeconds(60), zone))
            .getToday(defaultJerusalemLocation)
        val afterTzeit = DefaultJewishDayRepository(Clock.fixed(tzeit.plusSeconds(60), zone))
            .getToday(defaultJerusalemLocation)

        assertTrue(beforeSunset.hebrewDateEnglish, beforeSunset.hebrewDateEnglish.contains("17 Tammuz"))
        assertTrue(beinHashmashot.hebrewDateEnglish, beinHashmashot.hebrewDateEnglish.contains("17 Tammuz"))
        assertTrue(afterTzeit.hebrewDateEnglish, afterTzeit.hebrewDateEnglish.contains("18 Tammuz"))
    }

    @Test
    fun theIconWeekdayStaysOnTheCivilDayWhileItsHebrewDateRolls() {
        // 2026-07-02 is a Thursday. After tzeit the icon's Hebrew date moves on to 18 Tammuz, but
        // its weekday does not: read on its own in the status bar, a day name that jumps at
        // nightfall reads as a mistake. The app's header is the one that says "Friday" here.
        val zone = defaultJerusalemLocation.zoneId
        val tzeit = requireNotNull(tzeitForDate(date = LocalDate.of(2026, 7, 2)))
        val repository = DefaultJewishDayRepository(Clock.fixed(tzeit.plusSeconds(3_600), zone))

        val dayInfo = repository.getToday(defaultJerusalemLocation)
        val zmanim = repository.getZmanim(defaultJerusalemLocation)

        assertEquals("Thursday", dayInfo.dayOfWeekEnglish)
        assertEquals(LocalDate.of(2026, 7, 2), dayInfo.gregorianDate)
        // The app's header weekday is formatted from this, and has rolled.
        assertEquals(DayOfWeek.FRIDAY, zmanim.displayedDate.dayOfWeek)
        // Both sides still agree on the Hebrew date itself, which is what the icon prints below it.
        assertEquals(zmanim.hebrewDateEnglish, dayInfo.hebrewDateEnglish)
    }

    @Test
    fun theIconWeekdayTurnsOverAtMidnight() {
        val zone = defaultJerusalemLocation.zoneId
        val july2 = LocalDate.of(2026, 7, 2)

        fun weekdayAt(time: LocalTime): String = DefaultJewishDayRepository(
            Clock.fixed(july2.atTime(time).atZone(zone).toInstant(), zone),
        ).getToday(defaultJerusalemLocation).dayOfWeekEnglish

        assertEquals("Thursday", weekdayAt(LocalTime.of(23, 59)))
        // One minute later — still before chatzot halaila, so the zmanim have not moved on yet.
        val justAfterMidnight = july2.plusDays(1).atTime(0, 1).atZone(zone).toInstant()
        val repository = DefaultJewishDayRepository(Clock.fixed(justAfterMidnight, zone))
        assertEquals("Friday", repository.getToday(defaultJerusalemLocation).dayOfWeekEnglish)
        assertEquals(july2, repository.getZmanim(defaultJerusalemLocation).date)
    }

    @Test
    fun theIconWakesAtWhicheverOfMidnightOrTzeitComesFirst() {
        val zone = defaultJerusalemLocation.zoneId
        val july2 = LocalDate.of(2026, 7, 2)
        val settings = ZmanimCalculationSettings()
        val tzeit = requireNotNull(tzeitForDate(date = july2))

        val atNoon = july2.atTime(12, 0).atZone(zone).toInstant()
        assertEquals(
            tzeit.plus(Duration.ofMinutes(1)),
            nextStatusIconBoundary(defaultJerusalemLocation, settings, atNoon),
        )

        // Later that night the next thing to change is the weekday, at midnight — not chatzot.
        val evening = july2.atTime(21, 0).atZone(zone).toInstant()
        assertEquals(
            nextGregorianMidnight(defaultJerusalemLocation, evening),
            nextStatusIconBoundary(defaultJerusalemLocation, settings, evening),
        )
    }

    @Test
    fun theZmanimDayOnlyMovesOnAtChatzotHaLaila() {
        val zone = defaultJerusalemLocation.zoneId
        val july2 = LocalDate.of(2026, 7, 2)
        val chatzotHaLaila = requireNotNull(chatzotHaLailaForDate(date = july2))

        val beforeChatzot = DefaultJewishDayRepository(
            Clock.fixed(chatzotHaLaila.minusSeconds(300), zone),
        ).getZmanim(defaultJerusalemLocation)
        val afterChatzot = DefaultJewishDayRepository(
            Clock.fixed(chatzotHaLaila.plusSeconds(300), zone),
        ).getZmanim(defaultJerusalemLocation)

        // Both instants fall on 3 July by the civil clock; only the second is a new day here.
        assertEquals(july2, beforeChatzot.date)
        assertEquals(july2.plusDays(1), afterChatzot.date)
    }
}