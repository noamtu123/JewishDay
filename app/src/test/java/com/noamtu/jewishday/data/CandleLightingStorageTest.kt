// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.noamtu.jewishday.core.model.CandleLightingMethod
import com.noamtu.jewishday.core.model.ZmanimCalculationSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** "At plag hamincha" is stored under a value of its own, and the values older builds wrote still read the same. */
class CandleLightingStorageTest {
    private val key = stringPreferencesKey("zmanim_candle_method")

    @Test
    fun everyOptionSurvivesTheRoundTrip() {
        for (method in CandleLightingMethod.entries) {
            val written = mutablePreferencesOf().apply {
                putZmanimSettings(ZmanimCalculationSettings(candleLightingMethod = method, candleLightingCustomMinutes = 25))
            }
            assertEquals(method, written.zmanimSettings().candleLightingMethod)
            assertEquals(25, written.zmanimSettings().candleLightingCustomMinutes)
        }
    }

    @Test
    fun atPlagHasANewStorageValue() {
        assertEquals("plag_hamincha", CandleLightingMethod.AtPlag.storageValue)
        assertEquals(CandleLightingMethod.entries.size, CandleLightingMethod.entries.map { it.storageValue }.distinct().size)
        assertNull(ZmanimCalculationSettings(candleLightingMethod = CandleLightingMethod.AtPlag).candleLightingOffsetMinutes)
    }

    @Test
    fun valuesOlderBuildsWroteReadAsBefore() {
        val older = mapOf(
            "minutes_20" to CandleLightingMethod.Minutes20,
            "minutes_30" to CandleLightingMethod.Minutes30,
            "minutes_40" to CandleLightingMethod.Minutes40,
            "custom_minutes" to CandleLightingMethod.Custom,
        )
        for ((stored, method) in older) {
            val prefs = mutablePreferencesOf().apply {
                putZmanimSettings(ZmanimCalculationSettings())
                this[key] = stored
            }
            assertEquals(stored, method, prefs.zmanimSettings().candleLightingMethod)
        }
    }

    @Test
    fun thePromptStillOffersOnlyTheThreeMinhagim() {
        assertEquals(
            listOf(CandleLightingMethod.Minutes20, CandleLightingMethod.Minutes30, CandleLightingMethod.Minutes40),
            CandleLightingMethod.PromptOptions,
        )
    }
}
