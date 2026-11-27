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
        assertEquals("72 min after sunset (no tzeit)", minutes.description.en())
        assertEquals("72 דקות אחרי השקיעה (אין צאת כוכבים)", minutes.description.he())

        val chatzotSettings = ZmanimCalculationSettings(noTzeitFallback = NoTzeitFallback.ChatzotHaLaila)
        val chatzot = zmanimForDate(reykjavik, midsummer, chatzotSettings).row(ZmanimTimeOption.Tzeit)
        assertEquals("Chatzot HaLaila (no tzeit)", chatzot.description.en())
        assertEquals("חצות הלילה (אין צאת כוכבים)", chatzot.description.he())
    }

    @Test
    fun theMorningRowsNameTheStandIn() {
        val helsinki = JewishLocation("Helsinki", 60.1699, 24.9384, 17.0, ZoneId.of("Europe/Helsinki"))
        val day = zmanimForDate(helsinki, midsummer, ZmanimCalculationSettings())
        assertEquals("72 min before sunrise (no dawn)", day.row(ZmanimTimeOption.AlotHashachar).description.en())
        assertEquals("72 דקות לפני הזריחה (אין עלות השחר)", day.row(ZmanimTimeOption.AlotHashachar).description.he())
        assertEquals("50 min before sunrise (no misheyakir)", day.row(ZmanimTimeOption.TallitTefillin).description.en())
        assertEquals(
            "Magen Avraham, 72 min (no dawn)",
            day.row(ZmanimTimeOption.SofZmanShemaMagenAvraham).description.en(),
        )
        // Helsinki keeps its own 6.2° tzeit, so that row is not a stand-in.
        assertEquals(
            zmanimForDate(helsinki, LocalDate.of(2026, 3, 20), ZmanimCalculationSettings()).row(ZmanimTimeOption.Tzeit).description.en(),
            day.row(ZmanimTimeOption.Tzeit).description.en(),
        )

        val chatzot = zmanimForDate(helsinki, midsummer, ZmanimCalculationSettings(noTzeitFallback = NoTzeitFallback.ChatzotHaLaila))
        assertEquals("Chatzot HaLaila (no dawn)", chatzot.row(ZmanimTimeOption.TallitTefillin).description.en())
        assertEquals("From chatzot HaLaila (no dawn)", chatzot.row(ZmanimTimeOption.SofZmanTefillahMagenAvraham).description.en())
    }

    @Test
    fun theMidnightSunsShabbatTimesNameTheStandIn() {
        // The default's 72 minutes has no sunset to count from here, so the caption says chatzot.
        val day = zmanimForDate(tromso, midsummer.minusDays(2), ZmanimCalculationSettings())
        assertEquals("Friday; Chatzot HaLaila (no sunset)", day.row(ZmanimTimeOption.ShabbatCandleLighting).description.en())
        assertEquals("Saturday; Chatzot HaLaila (no tzeit) + 5m", day.row(ZmanimTimeOption.MotzeiShabbat).description.en())
    }
}
