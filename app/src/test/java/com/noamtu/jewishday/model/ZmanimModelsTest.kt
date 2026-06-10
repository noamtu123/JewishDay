package com.noamtu.jewishday.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ZmanimModelsTest {
    @Test
    fun standardDefaultsUseMagenAvrahamBasisGeonimAndShowRabbeinuTam() {
        val settings = ZmanimCalculationSettings()

        // GRA is always shown as its own row; the configurable basis defaults to MGA 72.
        assertEquals(SofZmanShemaMethod.Mga72, settings.sofZmanShemaMethod)
        assertEquals(SofZmanTefillahMethod.Mga72, settings.sofZmanTefillahMethod)
        assertEquals(MinchaGedolaMethod.Standard, settings.minchaGedolaMethod)
        assertEquals(TzeitHakochavimMethod.Geonim8Point5, settings.tzeitHakochavimMethod)
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
        assertEquals(listOf("Daily", "Zmanim", "Shabbat", "Daily Learning", "Location"), zmanimDay.groups.map { it.title })
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
    }

    @Test
    fun shabbatGroupUsesUpcomingFridayAndSaturday() {
        val date = LocalDate.of(2026, 6, 7)
        val shabbatItems = zmanimForDate(date = date)
            .groups
            .first { it.title == "Shabbat" }
            .items

        assertTrue(shabbatItems.first { it.title == "Candle Lighting" }.description.contains("2026-06-12"))
        assertTrue(shabbatItems.first { it.title == "Motzei Shabbat" }.description.contains("2026-06-13"))
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
}
