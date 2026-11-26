// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import com.noamtu.jewishday.i18n.TestText
import com.noamtu.jewishday.i18n.en
import com.noamtu.jewishday.i18n.he
import com.noamtu.jewishday.model.ZmanimGroupKind
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Candle lighting is never earlier than plag hamincha: far north in December the chosen minutes
 * before sunset would fall before it, and the row then shows the user's own plag and says why.
 */
class CandleLightingAtPlagTest {
    private val stockholm = JewishLocation("Stockholm", 59.3293, 18.0686, 28.0, ZoneId.of("Europe/Stockholm"))
    private val newYork = JewishLocation("New York", 40.7128, -74.0060, 10.0, ZoneId.of("America/New_York"))
    private val decemberFriday = LocalDate.of(2026, 12, 18)
    private val summerFriday = LocalDate.of(2026, 7, 3)

    private fun settingsFor(minutes: Int) = when (minutes) {
        20 -> ZmanimCalculationSettings(candleLightingMethod = CandleLightingMethod.Minutes20)
        30 -> ZmanimCalculationSettings(candleLightingMethod = CandleLightingMethod.Minutes30)
        40 -> ZmanimCalculationSettings(candleLightingMethod = CandleLightingMethod.Minutes40)
        else -> ZmanimCalculationSettings(candleLightingMethod = CandleLightingMethod.Custom, candleLightingCustomMinutes = minutes)
    }

    private fun day(location: JewishLocation, friday: LocalDate, settings: ZmanimCalculationSettings) =
        zmanimForDate(location, friday, settings, now = friday.atTime(9, 0).atZone(location.zoneId).toInstant())

    private fun ZmanimDay.candleRow() = groups.first { it.kind == ZmanimGroupKind.Shabbat }
        .items.first { it.id == ZmanimTimeOption.ShabbatCandleLighting.storageValue }

    @Test
    fun fortyMinutesInAStockholmDecemberIsHeldAtTheChosenPlag() {
        // Both the default GRA plag and a custom one: the fallback is the plag the user picked.
        listOf(
            settingsFor(40),
            settingsFor(40).copy(plagHaminchaMethod = PlagHaminchaMethod.CustomMinutes),
        ).forEach { settings ->
            val calendar = comprehensiveZmanimCalendar(stockholm, decemberFriday, settings)
            val plag = requireNotNull(calendar.plagHamincha(settings))
            val sunset = requireNotNull(calendar.sunset(settings.sunsetMethod))
            assertTrue("40 minutes must fall before plag here", sunset.minus(Duration.ofMinutes(40)).isBefore(plag))

            val day = day(stockholm, decemberFriday, settings)
            val row = day.candleRow()
            assertEquals(plag, row.time)
            assertEquals("Friday; Plag hamincha — the earliest time", row.description.en())
            assertEquals("יום שישי; פלג המנחה — הזמן המוקדם ביותר", row.description.he())
            assertEquals(plag, requireNotNull(day.holyDayInfo).startTime)
        }
    }

    @Test
    fun ordinaryLatitudesAreUnchanged() {
        listOf(defaultJerusalemLocation, newYork).forEach { location ->
            listOf(18, 20, 30, 40).forEach { minutes -> assertMinutesBeforeSunset(location, decemberFriday, minutes) }
        }
    }

    @Test
    fun aSummerFridayIsUnchanged() {
        listOf(defaultJerusalemLocation, newYork, stockholm).forEach { location ->
            listOf(18, 20, 30, 40).forEach { minutes -> assertMinutesBeforeSunset(location, summerFriday, minutes) }
        }
    }

    private fun assertMinutesBeforeSunset(location: JewishLocation, friday: LocalDate, minutes: Int) {
        val settings = settingsFor(minutes)
        val calendar = comprehensiveZmanimCalendar(location, friday, settings)
        val expected = requireNotNull(calendar.sunset(settings.sunsetMethod)).minus(Duration.ofMinutes(minutes.toLong()))
        val lighting = requireNotNull(calendar.candleLighting(settings))
        assertFalse("${location.name} $friday $minutes min was held at plag", lighting.isPlag)
        assertEquals(expected, lighting.time)

        val row = day(location, friday, settings).candleRow()
        assertEquals(expected, row.time)
        assertEquals("Friday; $minutes minutes", row.description.en())
        assertEquals("יום שישי; $minutes דקות", row.description.he())
    }
}
