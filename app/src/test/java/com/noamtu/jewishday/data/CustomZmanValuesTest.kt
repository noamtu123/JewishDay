// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.noamtu.jewishday.core.model.CustomZmanUnit
import com.noamtu.jewishday.core.model.CustomZmanValue
import com.noamtu.jewishday.core.model.ZmanimCalculationSettings
import com.noamtu.jewishday.feature.settings.wholeMinutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** A custom zman takes whatever number the user gives it; only what is no number at all is refused. */
class CustomZmanValuesTest {
    private val values = CustomZmanValue(degrees = 16.1, minutes = 72, zmaniyotMinutes = 72)

    @Test
    fun anyNumberIsTakenAsGiven() {
        assertEquals(40.0, values.withValue(CustomZmanUnit.Degrees, 40.0).degrees, 0.0)
        assertEquals(200.0, values.withValue(CustomZmanUnit.Degrees, 200.0).degrees, 0.0)
        assertEquals(19.848, values.withValue(CustomZmanUnit.Degrees, 19.848).degrees, 0.0)
        assertEquals(9_999_999, values.withValue(CustomZmanUnit.Minutes, 9_999_999.0).minutes)
        assertEquals(9_999_999, values.withValue(CustomZmanUnit.ZmaniyotMinutes, 9_999_999.0).zmaniyotMinutes)
    }

    @Test
    fun whatCannotBeANumberIsRefusedAndAnOverlongCountStopsAtAnInt() {
        assertEquals(values, values.withValue(CustomZmanUnit.Degrees, Double.NaN))
        assertEquals(values, values.withValue(CustomZmanUnit.Degrees, Double.POSITIVE_INFINITY))
        assertEquals(values, values.withValue(CustomZmanUnit.Minutes, Double.NaN))
        assertEquals(Int.MAX_VALUE, values.withValue(CustomZmanUnit.Minutes, 1e30).minutes)
        assertEquals(Int.MAX_VALUE, wholeMinutes("99999999999999999999"))
        assertEquals(18, wholeMinutes("18"))
        assertNull(wholeMinutes(""))
        assertNull(wholeMinutes("1a"))
    }

    @Test
    fun storedValuesAreReadAsStored() {
        val stored = mutablePreferencesOf().apply {
            putZmanimSettings(ZmanimCalculationSettings())
            this[doublePreferencesKey("zmanim_alot_custom_degrees")] = 90.0
            this[intPreferencesKey("zmanim_alot_custom_minutes")] = 300
            this[intPreferencesKey("zmanim_alot_custom_zmaniyot")] = 9_999_999
            // Never written by the app, but a corrupt file must not reach the calculations.
            this[doublePreferencesKey("zmanim_tzeit_custom_degrees")] = Double.NaN
        }
        val settings = stored.zmanimSettings()
        assertEquals(CustomZmanValue(degrees = 90.0, minutes = 300, zmaniyotMinutes = 9_999_999), settings.alotHashacharCustom)
        assertEquals(ZmanimCalculationSettings().tzeitHakochavimCustom, settings.tzeitHakochavimCustom)
    }
}
