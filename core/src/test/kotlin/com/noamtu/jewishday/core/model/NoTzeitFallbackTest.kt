// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Far north in summer some nights have no tzeit by the user's method — the sun sets but never sinks
 * far enough — and at the midnight sun it does not set at all. On those nights [NoTzeitFallback]
 * stands in for tzeit everywhere nightfall is waited for; anywhere else it changes nothing.
 */
class NoTzeitFallbackTest {
    private val helsinki = JewishLocation("Helsinki", 60.1699, 24.9384, 17.0, ZoneId.of("Europe/Helsinki"))
    private val reykjavik = JewishLocation("Reykjavik", 64.1466, -21.9426, 20.0, ZoneId.of("Atlantic/Reykjavik"))
    private val tromso = JewishLocation("Tromsø", 69.6492, 18.9553, 10.0, ZoneId.of("Europe/Oslo"))
    private val newYork = JewishLocation("New York", 40.7128, -74.006, 10.0, ZoneId.of("America/New_York"))

    private val minutes72 = ZmanimCalculationSettings()
    private val chatzot = ZmanimCalculationSettings(noTzeitFallback = NoTzeitFallback.ChatzotHaLaila)

    /** 8.5° — never reached in Helsinki around midsummer, though 6.2° is. */
    private fun deeper(settings: ZmanimCalculationSettings) = settings.copy(
        tzeitHakochavimMethod = TzeitHakochavimMethod.CustomDegrees,
        tzeitHakochavimCustom = settings.tzeitHakochavimCustom.copy(degrees = 8.5),
    )

    private val midsummer = LocalDate.of(2026, 6, 19)

    private fun solarMidnight(location: JewishLocation, date: LocalDate): Instant =
        requireNotNull(comprehensiveZmanimCalendar(location, date, chatzot).chatzotHaLaila(ChatzotMethod.Solar))

    private fun ownTzeit(location: JewishLocation, date: LocalDate, settings: ZmanimCalculationSettings): Instant? =
        comprehensiveZmanimCalendar(location, date, settings).ownTzeit(settings)

    @Test
    fun theDefaultIs72MinutesAfterSunset() {
        assertEquals(NoTzeitFallback.Minutes72AfterSunset, minutes72.noTzeitFallback)
    }

    @Test
    fun aSummerNightWithoutTzeitTakes72MinutesAfterTheChosenSunset() {
        for ((city, settings) in listOf(helsinki to deeper(minutes72), reykjavik to minutes72)) {
            for (sunsetMethod in SunsetMethod.entries) {
                val chosen = settings.copy(sunsetMethod = sunsetMethod)
                assertNull("${city.name} has no tzeit of its own", ownTzeit(city, midsummer, chosen))
                val sunset = requireNotNull(sunsetForDate(city, midsummer, chosen))
                assertEquals("${city.name} $sunsetMethod", sunset.plus(Duration.ofMinutes(72)), tzeitForDate(city, midsummer, chosen))
                val row = zmanimDayFacts(city, midsummer, chosen).zmanim.single { it.option == ZmanimTimeOption.Tzeit }
                assertEquals(NoTzeitFallback.Minutes72AfterSunset, row.fallback)
            }
        }
    }

    @Test
    fun aSummerNightWithoutTzeitTakesChatzotHaLaila() {
        for ((city, settings) in listOf(helsinki to deeper(chatzot), reykjavik to chatzot)) {
            assertNull("${city.name} has no tzeit of its own", ownTzeit(city, midsummer, settings))
            val tzeit = tzeitForDate(city, midsummer, settings)
            assertEquals(city.name, solarMidnight(city, midsummer), tzeit)
            val row = zmanimDayFacts(city, midsummer, settings).zmanim.single { it.option == ZmanimTimeOption.Tzeit }
            assertEquals(tzeit, row.time)
            assertEquals(NoTzeitFallback.ChatzotHaLaila, row.fallback)
        }
    }

    @Test
    fun theHebrewDateChangesAtTheFallback() {
        for (settings in listOf(deeper(minutes72), deeper(chatzot))) {
            val fallback = requireNotNull(tzeitForDate(helsinki, midsummer, settings))
            val before = fallback.minusSeconds(60)
            val after = fallback.plusSeconds(60)
            // The civil date of the Jewish day: midsummer's own until the fallback, the next one after.
            assertEquals(midsummer, jewishDayCivilDate(helsinki, settings, before))
            assertEquals(midsummer.plusDays(1), jewishDayCivilDate(helsinki, settings, after))
            val beforeDay = before.atZone(helsinki.zoneId).toLocalDate()
            assertEquals(midsummer, zmanimDayFacts(helsinki, beforeDay, settings, now = before).displayedDate)
            val afterDay = after.atZone(helsinki.zoneId).toLocalDate()
            assertEquals(midsummer.plusDays(1), zmanimDayFacts(helsinki, afterDay, settings, now = after).displayedDate)
            // And the icon's alarm is set for it.
            assertEquals(fallback.plus(1, ChronoUnit.MINUTES), nextTzeit(helsinki, settings, before))
        }
    }

    @Test
    fun aFastWithoutTzeitEndsAtTheFallback() {
        // 17 Tammuz 5786.
        val fastDate = LocalDate.of(2026, 7, 2)
        for (settings in listOf(deeper(minutes72), deeper(chatzot))) {
            assertNull(ownTzeit(helsinki, fastDate, settings))
            val noon = fastDate.atTime(12, 0).atZone(helsinki.zoneId).toInstant()
            val fast = requireNotNull(zmanimDayFacts(helsinki, fastDate, settings, now = noon).fastDayInfo)
            assertEquals(Fast.SeventeenthOfTammuz, fast.fast)
            assertEquals(tzeitForDate(helsinki, fastDate, settings), fast.endTime)
            assertNotNull(fast.endTime)
        }
    }

    @Test
    fun theMidnightSunHasShabbatComeInAndGoOutAtChatzotHaLaila() {
        val friday = midsummer
        assertEquals(DayOfWeek.FRIDAY, friday.dayOfWeek)
        val wednesday = friday.minusDays(2)
        // With no sunset to count 72 minutes from, the default gives way to chatzot halaila too.
        for (settings in listOf(minutes72, chatzot)) {
            val shabbat = requireNotNull(zmanimDayFacts(tromso, wednesday, settings).shabbat)
            val entry = requireNotNull(shabbat.candleLighting)
            assertEquals(solarMidnight(tromso, friday), entry.time)
            assertEquals(NoTzeitFallback.ChatzotHaLaila, entry.fallback)
            assertFalse(entry.isPlag)
            val tosefet = Duration.ofMinutes(settings.holyDayTosefetMinutes.toLong())
            assertEquals(solarMidnight(tromso, friday.plusDays(1)).plus(tosefet), shabbat.exit)
            assertEquals(NoTzeitFallback.ChatzotHaLaila, shabbat.exitFallback)
            val row = zmanimDayFacts(tromso, friday, settings).zmanim.single { it.option == ZmanimTimeOption.Tzeit }
            assertEquals(NoTzeitFallback.ChatzotHaLaila, row.fallback)
        }
    }

    @Test
    fun placesWithTzeitAreUntouched() {
        val days = generateSequence(LocalDate.of(2026, 1, 1)) { it.plusDays(1) }
            .takeWhile { it.year == 2026 }
            .toList()
        for (city in listOf(defaultJerusalemLocation, newYork)) {
            for (day in days) {
                val own = requireNotNull(ownTzeit(city, day, minutes72))
                for (settings in listOf(minutes72, chatzot)) {
                    assertEquals("${city.name} $day", own, tzeitForDate(city, day, settings))
                    assertEquals(holyDayExitForDate(city, day, minutes72), holyDayExitForDate(city, day, settings))
                }
            }
            for (day in days.filterIndexed { index, _ -> index % 5 == 0 }) {
                val noon = day.atTime(12, 0).atZone(city.zoneId).toInstant()
                val facts = zmanimDayFacts(city, day, minutes72, now = noon)
                assertEquals("${city.name} $day", facts, zmanimDayFacts(city, day, chatzot, now = noon))
                assertTrue(facts.zmanim.all { it.fallback == null })
                assertNull(facts.shabbat?.exitFallback)
                assertNull(facts.shabbat?.candleLighting?.fallback)
            }
        }
    }

    @Test
    fun theChoiceIsOfferedOnlyWhereSomeNightHasNoTzeit() {
        val from = LocalDate.of(2026, 11, 27)
        assertFalse(hasNightThatStaysLight(defaultJerusalemLocation, minutes72, from))
        assertFalse(hasNightThatStaysLight(newYork, minutes72, from))
        // Helsinki's midsummer sun reaches 6.2° for tzeit, but never 16.1° for alot.
        assertTrue(hasNightThatStaysLight(helsinki, minutes72, from))
        assertTrue(hasNightThatStaysLight(reykjavik, minutes72, from))
        assertTrue(hasNightThatStaysLight(tromso, minutes72, from))
        // The fallback itself is not part of the question.
        assertTrue(hasNightThatStaysLight(tromso, chatzot, from))
    }
}
