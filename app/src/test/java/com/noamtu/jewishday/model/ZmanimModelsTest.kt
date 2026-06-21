package com.noamtu.jewishday.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ZmanimModelsTest {
    @Test
    fun standardDefaultsMatch2netIsraelConventions() {
        val settings = ZmanimCalculationSettings()

        // GRA is always shown as its own row; the configurable basis defaults to MGA 72.
        assertEquals(SofZmanShemaMethod.Mga72, settings.sofZmanShemaMethod)
        assertEquals(SofZmanTefillahMethod.Mga72, settings.sofZmanTefillahMethod)
        assertEquals(MinchaGedolaMethod.Standard, settings.minchaGedolaMethod)
        // Defaults aligned to 2net (Israel): degree-based dawn, tzeit = sunset + 20 min.
        assertEquals(AlotHashacharMethod.Degrees16Point1, settings.alotHashacharMethod)
        assertEquals(TzeitHakochavimMethod.Minutes20, settings.tzeitHakochavimMethod)
        assertEquals(MotzeiShabbatMethod.Geonim8Point5, settings.motzeiShabbatMethod)
        assertEquals(RabbeinuTamMethod.Minutes72, settings.rabbeinuTamMethod)
    }

    @Test
    fun magenAvrahamPresetUsesMagenAvrahamMethodsAcrossDaytimeZmanim() {
        val settings = ZmanimPreset.MagenAvraham72.defaultSettings()

        assertEquals(AlotHashacharMethod.Minutes72, settings.alotHashacharMethod)
        assertEquals(SofZmanShemaMethod.Mga72, settings.sofZmanShemaMethod)
        assertEquals(SofZmanTefillahMethod.Mga72, settings.sofZmanTefillahMethod)
        assertEquals(MinchaGedolaMethod.Mga72, settings.minchaGedolaMethod)
        assertEquals(MinchaKetanaMethod.Mga72, settings.minchaKetanaMethod)
        assertEquals(PlagHaminchaMethod.Mga72, settings.plagHaminchaMethod)
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

        assertTrue(shabbatItems.first { it.title == "Candle Lighting & Shabbat Entry" }.description.contains("2026-06-12"))
        assertTrue(shabbatItems.first { it.title == "Motzei Shabbat" }.description.contains("2026-06-13"))
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
            date = secondDayShavuot,
            settings = ZmanimCalculationSettings(inIsrael = true),
        ).eventValues()
        val diasporaEvents = zmanimForDate(
            date = secondDayShavuot,
            settings = ZmanimCalculationSettings(inIsrael = false),
        ).eventValues()

        assertTrue(israelEvents.contains("Isru Chag"))
        assertTrue(diasporaEvents.contains("Shavuos"))
        assertFalse(israelEvents == diasporaEvents)
        assertTrue(diasporaEvents.any { it.isNotBlank() })
    }

    @Test
    fun outsideIsraelCanHaveDifferentWeeklyParsha() {
        val splitParshaShabbat = LocalDate.of(2026, 5, 30)

        val israelParsha = zmanimForDate(
            date = splitParshaShabbat,
            settings = ZmanimCalculationSettings(inIsrael = true),
        ).weeklyParsha()
        val diasporaParsha = zmanimForDate(
            date = splitParshaShabbat,
            settings = ZmanimCalculationSettings(inIsrael = false),
        ).weeklyParsha()

        assertFalse(israelParsha == diasporaParsha)
    }

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

    private fun ZmanimDay.weeklyParsha(): String = groups
        .first { it.title == "Shabbat" }
        .items
        .first { it.title == "Weekly Parsha" }
        .value.orEmpty()

    private fun ZmanimDay.eventValues(): List<String> = groups
        .firstOrNull { it.title.isBlank() }
        ?.items
        ?.mapNotNull { it.value }
        .orEmpty()
}
