// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import com.noamtu.jewishday.model.defaultJerusalemLocation
import com.noamtu.jewishday.model.sunsetForDate
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
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
    fun hebrewDateForStatusIconRollsOverAtSunset() {
        // 2026-07-02 is 17 Tammuz 5786 in Jerusalem; the Hebrew date must flip at sunset.
        val zone = defaultJerusalemLocation.zoneId
        val sunset = requireNotNull(sunsetForDate(date = LocalDate.of(2026, 7, 2)))

        val beforeSunset = DefaultJewishDayRepository(Clock.fixed(sunset.minusSeconds(3_600), zone))
            .getToday(defaultJerusalemLocation)
        val afterSunset = DefaultJewishDayRepository(Clock.fixed(sunset.plusSeconds(60), zone))
            .getToday(defaultJerusalemLocation)

        assertTrue(beforeSunset.hebrewDateEnglish, beforeSunset.hebrewDateEnglish.contains("17 Tammuz"))
        assertTrue(afterSunset.hebrewDateEnglish, afterSunset.hebrewDateEnglish.contains("18 Tammuz"))
    }
}