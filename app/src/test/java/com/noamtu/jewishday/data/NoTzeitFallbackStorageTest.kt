// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.noamtu.jewishday.core.model.NoTzeitFallback
import com.noamtu.jewishday.core.model.ZmanimCalculationSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class NoTzeitFallbackStorageTest {
    @Test
    fun eachChoiceSurvivesTheRoundTrip() {
        for (fallback in NoTzeitFallback.entries) {
            val written = mutablePreferencesOf().apply {
                putZmanimSettings(ZmanimCalculationSettings(noTzeitFallback = fallback))
            }
            assertEquals(fallback, written.zmanimSettings().noTzeitFallback)
        }
    }

    @Test
    fun settingsFromBeforeTheChoiceReadAs72MinutesAfterSunset() {
        assertEquals(NoTzeitFallback.Minutes72AfterSunset, emptyPreferences().zmanimSettings().noTzeitFallback)
        // An older build's full set of opinions, written without the key at all.
        val older = mutablePreferencesOf().apply { putZmanimSettings(ZmanimCalculationSettings()) }
        older.remove(stringPreferencesKey("zmanim_no_tzeit_fallback"))
        assertEquals(ZmanimCalculationSettings(), older.zmanimSettings())
    }

    @Test
    fun theDroppedEretzYisraelChoiceReadsAs72MinutesAfterSunset() {
        // Offered once, then dropped for want of a source on these nights (see NoTzeitFallback).
        val stored = mutablePreferencesOf(stringPreferencesKey("zmanim_no_tzeit_fallback") to "eretz_yisrael")
        assertEquals(NoTzeitFallback.Minutes72AfterSunset, stored.zmanimSettings().noTzeitFallback)
    }

    @Test
    fun anUnknownStoredValueFallsBackToTheDefault() {
        val stored = mutablePreferencesOf(stringPreferencesKey("zmanim_no_tzeit_fallback") to "civil_midnight")
        assertEquals(NoTzeitFallback.Minutes72AfterSunset, stored.zmanimSettings().noTzeitFallback)
    }
}
