// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class ZmanimModelsTest {
    // Israel vs. diaspora is now derived from the location, so these tests pick where you are.
    private val diasporaLocation =
        JewishLocation("New York", 40.7128, -74.0060, 10.0, ZoneId.of("America/New_York"))
    @Test
    fun standardDefaultsMatch2netIsraelConventions() {
        val settings = ZmanimCalculationSettings()

        // GRA is always shown as its own row; the configurable basis defaults to MGA 16.1°.
        assertEquals(SofZmanShemaMethod.Mga16Point1, settings.sofZmanShemaMethod)
        assertEquals(SofZmanTefillahMethod.Mga16Point1, settings.sofZmanTefillahMethod)
        assertEquals(MinchaGedolaMethod.Standard, settings.minchaGedolaMethod)
        // Defaults aligned to 2net (Israel): degree-based dawn, tzeit = 6.2° (Peninei Halacha).
        assertEquals(AlotHashacharMethod.Degrees16Point1, settings.alotHashacharMethod)
        assertEquals(TzeitHakochavimMethod.Degrees6Point2, settings.tzeitHakochavimMethod)
        assertEquals(MotzeiShabbatMethod.Degrees6Point2, settings.motzeiShabbatMethod)
        assertEquals(5, settings.holyDayTosefetMinutes)
        assertEquals(RabbeinuTamMethod.Minutes72, settings.rabbeinuTamMethod)
    }

    @Test
    fun todayZmanimContainsCoreGroupsForDefaultLocation() {
        val date = LocalDate.of(2026, 5, 29)
        val zmanimDay = zmanimForDate(date = date)

        assertEquals("Jerusalem", zmanimDay.locationName)
        assertEquals(date, zmanimDay.date)
        assertEquals(defaultJerusalemLocation.zoneId, zmanimDay.zoneId)
        // No standalone "Daily" section: the parsha sits in Shabbat and day-events only appear
        // (header-less) when present, so a quiet weekday has just these three groups.
        assertEquals(listOf("Zmanim", "Shabbat", "Daily Learning"), zmanimDay.groups.map { it.title })
        assertTrue(zmanimDay.groups.all { it.items.isNotEmpty() })
        assertFalse(zmanimDay.groups.flatMap { it.items }.any { it.time == null && it.value == null })
    }

    @Test
    fun zmanimListShowsGraAndMagenAvrahamRowsChatzotDayAndNightAndNoSamuch() {
        val date = LocalDate.of(2026, 5, 29)
        val zmanimItems = zmanimForDate(date = date)
            .groups
            .first { it.title == "Zmanim" }
            .items
            .map { it.title }

        assertTrue(zmanimItems.contains("Sof Zman Shema (GRA)"))
        assertTrue(zmanimItems.contains("Sof Zman Shema (Magen Avraham)"))
        assertTrue(zmanimItems.contains("Sof Zman Tefillah (GRA)"))
        assertTrue(zmanimItems.contains("Sof Zman Tefillah (Magen Avraham)"))
        assertTrue(zmanimItems.contains("Chatzot HaYom"))
        assertTrue(zmanimItems.contains("Chatzot HaLaila"))
        assertFalse(zmanimItems.contains("Samuch LeMincha Ketana"))

        // Misheyakir row is now labelled Tallit & Tefillin.
        assertTrue(zmanimItems.contains("Tallit & Tefillin"))
        assertFalse(zmanimItems.contains("Misheyakir"))
        // Magen Avraham rows come before their GRA counterparts.
        assertTrue(zmanimItems.indexOf("Sof Zman Shema (Magen Avraham)") < zmanimItems.indexOf("Sof Zman Shema (GRA)"))
        assertTrue(zmanimItems.indexOf("Sof Zman Tefillah (Magen Avraham)") < zmanimItems.indexOf("Sof Zman Tefillah (GRA)"))
    }

    @Test
    fun shabbatGroupUsesUpcomingFridayAndSaturday() {
        val date = LocalDate.of(2026, 6, 7)
        val shabbatItems = zmanimForDate(date = date)
            .groups
            .first { it.title == "Shabbat" }
            .items
        val zone = defaultJerusalemLocation.zoneId
        fun itemDate(title: String): LocalDate =
            shabbatItems.first { it.title == title }.time!!.atZone(zone).toLocalDate()

        // Asserted on the computed times themselves: the descriptions carry only the day name
        // and method, never a raw date.
        assertEquals(LocalDate.of(2026, 6, 12), itemDate("Candle Lighting & Shabbat Entry"))
        assertEquals(LocalDate.of(2026, 6, 13), itemDate("Motzei Shabbat"))
        assertFalse(
            shabbatItems.any { it.description.contains(Regex("\\d{4}-\\d{2}-\\d{2}")) },
        )
    }

    @Test
    fun weeklyParshaRollsForwardAfterMotzeiShabbat() {
        val location = defaultJerusalemLocation
        val saturday = LocalDate.of(2026, 6, 13)
        val sunday = saturday.plusDays(1)
        val beforeMotzei = saturday.atTime(LocalTime.NOON).atZone(location.zoneId).toInstant()
        val afterMotzei = saturday.atTime(23, 30).atZone(location.zoneId).toInstant()

        val before = zmanimForDate(location = location, date = saturday, now = beforeMotzei).weeklyParsha()
        val after = zmanimForDate(location = location, date = saturday, now = afterMotzei).weeklyParsha()
        val sundayParsha = zmanimForDate(location = location, date = sunday).weeklyParsha()

        assertEquals(sundayParsha, after)
        assertFalse(before == after)
    }

    @Test
    fun outsideIsraelShowsSecondDayYomTov() {
        val secondDayShavuot = LocalDate.of(2026, 5, 23)

        val israelEvents = zmanimForDate(
            location = defaultJerusalemLocation,
            date = secondDayShavuot,
        ).eventValues()
        val diasporaEvents = zmanimForDate(
            location = diasporaLocation,
            date = secondDayShavuot,
        ).eventValues()

        // In Israel the day is Isru Chag — nothing is forbidden, so it stays an event row.
        assertTrue(israelEvents.contains("Isru Chag"))
        // In the diaspora it is the second day of Yom Tov, which the header names instead, so it
        // is deliberately absent from the event rows.
        assertFalse(diasporaEvents.contains("Shavuos"))
        val diasporaHolyDay = zmanimForDate(location = diasporaLocation, date = secondDayShavuot).holyDayInfo
        assertTrue(requireNotNull(diasporaHolyDay).name, requireNotNull(diasporaHolyDay).name.contains("Shavuos"))
        assertFalse(israelEvents == diasporaEvents)
    }

    @Test
    fun outsideIsraelCanHaveDifferentWeeklyParsha() {
        val splitParshaShabbat = LocalDate.of(2026, 5, 30)

        val israelParsha = zmanimForDate(
            location = defaultJerusalemLocation,
            date = splitParshaShabbat,
        ).weeklyParsha()
        val diasporaParsha = zmanimForDate(
            location = diasporaLocation,
            date = splitParshaShabbat,
        ).weeklyParsha()

        assertFalse(israelParsha == diasporaParsha)
    }

    @Test
    fun isInIsraelClassifiesByCoordinates() {
        // Inside: Jerusalem, Tel Aviv, Eilat (south), and the Golan.
        assertTrue(defaultJerusalemLocation.isInIsrael)
        assertTrue(location(32.0853, 34.7818).isInIsrael) // Tel Aviv
        assertTrue(location(29.5577, 34.9519).isInIsrael) // Eilat
        assertTrue(location(33.13, 35.85).isInIsrael) // Golan
        // Outside: the diaspora, and neighbouring border cities the box must exclude.
        assertFalse(diasporaLocation.isInIsrael)
        assertFalse(location(31.9539, 35.9284).isInIsrael) // Amman, Jordan
        assertFalse(location(51.5074, -0.1278).isInIsrael) // London
    }

    private fun location(latitude: Double, longitude: Double): JewishLocation =
        JewishLocation("test", latitude, longitude, 0.0, ZoneId.of("UTC"))

    @Test
    fun directTzeitMatchesDisplayedTzeitRow() {
        val date = LocalDate.of(2026, 5, 29)
        val settings = ZmanimCalculationSettings()
        val displayedTzeit = zmanimForDate(date = date, settings = settings)
            .groups
            .flatMap { it.items }
            .first { it.title == "Tzeit" }
            .time

        assertEquals(displayedTzeit, tzeitForDate(date = date, settings = settings))
    }

    // On Shabbat itself the Shabbat section is dropped as a duplicate and the parsha moves up to
    // the event rows, so look for it wherever it is rather than in one group.
    private fun ZmanimDay.weeklyParsha(): String = groups
        .flatMap { it.items }
        .first { it.title == "Weekly Parsha" }
        .value.orEmpty()

    private fun ZmanimDay.eventValues(): List<String> = groups
        .firstOrNull { it.title.isBlank() }
        ?.items
        ?.mapNotNull { it.value }
        .orEmpty()
}