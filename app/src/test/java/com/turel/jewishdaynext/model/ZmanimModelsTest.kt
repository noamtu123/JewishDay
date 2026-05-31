package com.turel.jewishdaynext.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ZmanimModelsTest {
    @Test
    fun todayZmanimContainsCoreGroupsForDefaultLocation() {
        val date = LocalDate.of(2026, 5, 29)
        val zmanimDay = zmanimForDate(date = date)

        assertEquals("Jerusalem", zmanimDay.locationName)
        assertEquals(date, zmanimDay.date)
        assertEquals(defaultJerusalemLocation.zoneId, zmanimDay.zoneId)
        assertEquals(listOf("Daily", "Morning", "Afternoon & Evening", "Shabbat", "Learning & Place"), zmanimDay.groups.map { it.title })
        assertTrue(zmanimDay.groups.all { it.items.isNotEmpty() })
        assertFalse(zmanimDay.groups.flatMap { it.items }.any { it.time == null && it.value == null })
    }
}
