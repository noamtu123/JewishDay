// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Far north in summer the sun never sinks to the dawn angle (16.1°) or the misheyakir one (11°) before
 * it rises: Helsinki for weeks, Reykjavik for longer. On those mornings the same [NoTzeitFallback]
 * that stands in for tzeit stands in for alot, misheyakir and the Magen Avraham's day too.
 */
class NoDawnFallbackTest {
    private val helsinki = JewishLocation("Helsinki", 60.1699, 24.9384, 17.0, ZoneId.of("Europe/Helsinki"))
    private val reykjavik = JewishLocation("Reykjavik", 64.1466, -21.9426, 20.0, ZoneId.of("Atlantic/Reykjavik"))

    private val minutes72 = ZmanimCalculationSettings()
    private val chatzot = ZmanimCalculationSettings(noTzeitFallback = NoTzeitFallback.ChatzotHaLaila)

    private val midsummer = LocalDate.of(2026, 6, 19)

    private fun calendar(location: JewishLocation, date: LocalDate, settings: ZmanimCalculationSettings) =
        comprehensiveZmanimCalendar(location, date, settings)

    private fun ZmanimDayFacts.row(option: ZmanimTimeOption): ZmanTime = zmanim.single { it.option == option }

    @Test
    fun theDefaultCountsBackTheFixedMinutesFromSunrise() {
        for (city in listOf(helsinki, reykjavik)) {
            val calendar = calendar(city, midsummer, minutes72)
            assertNull("${city.name} has no dawn of its own", calendar.ownAlot(minutes72))
            val sunrise = requireNotNull(calendar.sunrise(minutes72.sunriseMethod))
            val facts = zmanimDayFacts(city, midsummer, minutes72)

            val alot = facts.row(ZmanimTimeOption.AlotHashachar)
            assertEquals(sunrise.minus(Duration.ofMinutes(72)), alot.time)
            // The very time the old silent fallback gave, so nobody on the default sees it move.
            assertEquals(calendar.alos72Minutes, alot.time)
            assertEquals(NoTzeitFallback.Minutes72AfterSunset, alot.fallback)

            val misheyakir = facts.row(ZmanimTimeOption.TallitTefillin)
            assertEquals(sunrise.minus(Duration.ofMinutes(50)), misheyakir.time)
            assertEquals(NoTzeitFallback.Minutes72AfterSunset, misheyakir.fallback)

            // The Magen Avraham's 72-minute day, again exactly what was shown before.
            val shema = facts.row(ZmanimTimeOption.SofZmanShemaMagenAvraham)
            assertEquals(calendar.sofZmanShmaMGA72Minutes, shema.time)
            assertEquals(NoTzeitFallback.Minutes72AfterSunset, shema.fallback)
            val tefillah = facts.row(ZmanimTimeOption.SofZmanTefillahMagenAvraham)
            assertEquals(calendar.sofZmanTfilaMGA72Minutes, tefillah.time)
            assertEquals(NoTzeitFallback.Minutes72AfterSunset, tefillah.fallback)

            // The GRA's day is sunrise to sunset, which exist: never stood in for.
            assertNull(facts.row(ZmanimTimeOption.SofZmanShemaGra).fallback)
        }
    }

    @Test
    fun chatzotHaLailaBeginsTheMorning() {
        for (city in listOf(helsinki, reykjavik)) {
            val calendar = calendar(city, midsummer, chatzot)
            val lastNight = requireNotNull(calendar(city, midsummer.minusDays(1), chatzot).chatzotHaLaila(ChatzotMethod.Solar))
            val tonight = requireNotNull(calendar.chatzotHaLaila(ChatzotMethod.Solar))
            val facts = zmanimDayFacts(city, midsummer, chatzot)
            assertEquals(city.name, lastNight, facts.row(ZmanimTimeOption.AlotHashachar).time)
            assertEquals(city.name, lastNight, facts.row(ZmanimTimeOption.TallitTefillin).time)
            assertEquals(NoTzeitFallback.ChatzotHaLaila, facts.row(ZmanimTimeOption.TallitTefillin).fallback)
            // Three hours of a day from one chatzot to the next: about six clock hours after the first.
            val shema = requireNotNull(facts.row(ZmanimTimeOption.SofZmanShemaMagenAvraham).time)
            assertEquals(calendar.getSofZmanShma(lastNight, tonight), shema)
            val hours = Duration.between(lastNight, shema).toMinutes() / 60.0
            assertEquals(6.0, hours, 0.02)
        }
    }

    @Test
    fun aDawnFastBeginsAtTheStandIn() {
        // 17 Tammuz 5786.
        val fastDate = LocalDate.of(2026, 7, 2)
        for (settings in listOf(minutes72, chatzot)) {
            assertNull(calendar(helsinki, fastDate, settings).ownAlot(settings))
            val noon = fastDate.atTime(12, 0).atZone(helsinki.zoneId).toInstant()
            val facts = zmanimDayFacts(helsinki, fastDate, settings, now = noon)
            val fast = requireNotNull(facts.fastDayInfo)
            assertEquals(Fast.SeventeenthOfTammuz, fast.fast)
            assertEquals(settings.noTzeitFallback.name, facts.row(ZmanimTimeOption.AlotHashachar).time, fast.startTime)
        }
        val sunrise = requireNotNull(calendar(helsinki, fastDate, minutes72).sunrise(minutes72.sunriseMethod))
        val start = zmanimDayFacts(helsinki, fastDate, minutes72).fastDayInfo?.startTime
        assertEquals(sunrise.minus(Duration.ofMinutes(72)), start)
    }

    @Test
    fun sixMinutesAfterAlotCountsFromTheStandIn() {
        val settings = minutes72.copy(misheyakirMethod = MisheyakirMethod.Minutes6AfterAlos)
        val facts = zmanimDayFacts(helsinki, midsummer, settings)
        val alot = requireNotNull(facts.row(ZmanimTimeOption.AlotHashachar).time)
        val misheyakir = facts.row(ZmanimTimeOption.TallitTefillin)
        assertEquals(alot.plus(Duration.ofMinutes(6)), misheyakir.time)
        // Its own caption still says exactly what it is.
        assertNull(misheyakir.fallback)
    }

    @Test
    fun aMorningWithItsOwnDawnIsUntouched() {
        // Helsinki in March has every angle; only the stand-in setting differs, and it must not matter.
        val march = LocalDate.of(2026, 3, 20)
        val noon: Instant = march.atTime(12, 0).atZone(helsinki.zoneId).toInstant()
        val facts = zmanimDayFacts(helsinki, march, minutes72, now = noon)
        assertEquals(facts, zmanimDayFacts(helsinki, march, chatzot, now = noon))
        assertEquals(calendar(helsinki, march, minutes72).alos16Point1Degrees, facts.row(ZmanimTimeOption.AlotHashachar).time)
        assertNull(facts.row(ZmanimTimeOption.AlotHashachar).fallback)
        assertNull(facts.row(ZmanimTimeOption.TallitTefillin).fallback)
    }
}
