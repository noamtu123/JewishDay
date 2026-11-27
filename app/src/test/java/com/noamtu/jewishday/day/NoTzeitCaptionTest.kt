// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.day

import com.noamtu.jewishday.core.model.JewishLocation
import com.noamtu.jewishday.core.model.NoTzeitFallback
import com.noamtu.jewishday.core.model.ZmanimCalculationSettings
import com.noamtu.jewishday.core.model.ZmanimTimeOption
import com.noamtu.jewishday.i18n.en
import com.noamtu.jewishday.i18n.he
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

/** A time the no-tzeit stand-in supplied says so underneath, instead of naming a method that gave nothing. */
class NoTzeitCaptionTest {
    private val reykjavik = JewishLocation("Reykjavik", 64.1466, -21.9426, 20.0, ZoneId.of("Atlantic/Reykjavik"))
    private val tromso = JewishLocation("Tromsø", 69.6492, 18.9553, 10.0, ZoneId.of("Europe/Oslo"))
    private val midsummer = LocalDate.of(2026, 6, 19)

    private fun ZmanimDay.row(option: ZmanimTimeOption): ZmanItem =
        groups.flatMap { it.items }.first { it.id == option.storageValue }

    @Test
    fun theTzeitRowNamesTheStandIn() {
        val minutes = zmanimForDate(reykjavik, midsummer, ZmanimCalculationSettings()).row(ZmanimTimeOption.Tzeit)
        assertEquals("72 min after sunset (no tzeit tonight)", minutes.description.en())
        assertEquals("72 דקות אחרי השקיעה (אין צאת הכוכבים הלילה)", minutes.description.he())

        val chatzotSettings = ZmanimCalculationSettings(noTzeitFallback = NoTzeitFallback.ChatzotHaLaila)
        val chatzot = zmanimForDate(reykjavik, midsummer, chatzotSettings).row(ZmanimTimeOption.Tzeit)
        assertEquals("Chatzot HaLaila (no tzeit tonight)", chatzot.description.en())
        assertEquals("חצות הלילה (אין צאת הכוכבים הלילה)", chatzot.description.he())

        val settings = ZmanimCalculationSettings(noTzeitFallback = NoTzeitFallback.EretzYisrael)
        val jerusalem = zmanimForDate(reykjavik, midsummer, settings).row(ZmanimTimeOption.Tzeit)
        assertEquals("As in Jerusalem (no tzeit tonight)", jerusalem.description.en())
    }

    @Test
    fun theMidnightSunsShabbatTimesNameTheStandIn() {
        // The default's 72 minutes has no sunset to count from here, so the caption says chatzot.
        val day = zmanimForDate(tromso, midsummer.minusDays(2), ZmanimCalculationSettings())
        assertEquals("Friday; Chatzot HaLaila (no sunset)", day.row(ZmanimTimeOption.ShabbatCandleLighting).description.en())
        assertEquals("Saturday; Chatzot HaLaila (no tzeit tonight) + 5m", day.row(ZmanimTimeOption.MotzeiShabbat).description.en())
    }
}
