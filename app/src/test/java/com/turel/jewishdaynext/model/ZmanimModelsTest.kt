package com.turel.jewishdaynext.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ZmanimModelsTest {
    @Test
    fun standardPresetUsesGraGeonimAndShowsRabbeinuTam() {
        val settings = ZmanimPreset.Standard.defaultSettings()

        assertEquals(SofZmanShemaMethod.Gra, settings.sofZmanShemaMethod)
        assertEquals(SofZmanTefillahMethod.Gra, settings.sofZmanTefillahMethod)
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
        assertEquals(SamuchLeMinchaKetanaMethod.Mga72, settings.samuchLeMinchaKetanaMethod)
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
        assertEquals(listOf("Daily", "Morning", "Afternoon & Evening", "Shabbat", "Additional Opinions", "Daily Learning", "Location"), zmanimDay.groups.map { it.title })
        assertTrue(zmanimDay.groups.all { it.items.isNotEmpty() })
        assertFalse(zmanimDay.groups.flatMap { it.items }.any { it.time == null && it.value == null })
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
