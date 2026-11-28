// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import com.noamtu.jewishday.core.i18n.CalendarText
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Omer and Chanukah follow the evening: once tzeit has passed, today's screen shows tonight's
 * count and candle. A stepped-to day is read whole. And Erev Pesach on Shabbat burns its chametz on
 * Friday.
 */
class EveningEventsTest {
    private val location = defaultJerusalemLocation
    private val settings = ZmanimCalculationSettings()

    private fun firstDayIn(year: Int, test: (JewishCalendar) -> Boolean): LocalDate =
        generateSequence(LocalDate.of(year, 1, 1)) { it.plusDays(1) }
            .first { test(jewishCalendar(it, inIsrael = true)) }

    private fun afterTzeit(date: LocalDate): Instant = requireNotNull(tzeitForDate(location, date, settings)).plusSeconds(60)

    private fun beforeTzeit(date: LocalDate): Instant = requireNotNull(tzeitForDate(location, date, settings)).minusSeconds(60)

    private fun facts(date: LocalDate, now: Instant?, rolled: Boolean? = null) = zmanimDayFacts(location, date, settings, now, rolled)

    private fun omer(facts: ZmanimDayFacts) = facts.events.filterIsInstance<DayEvent.Omer>().singleOrNull()

    private fun chanukah(facts: ZmanimDayFacts) = facts.events.filterIsInstance<DayEvent.Chanukah>().singleOrNull()

    @Test
    fun theFirstNightOfTheOmerShowsItsCountFromTzeit() {
        val pesach = firstDayIn(2027) { it.jewishMonth == JewishDate.NISSAN && it.jewishDayOfMonth == 15 }
        assertNull(omer(facts(pesach, beforeTzeit(pesach))))
        assertEquals(DayEvent.Omer(CalendarText.Omer(pesach.plusDays(1), true)), omer(facts(pesach, afterTzeit(pesach))))
    }

    @Test
    fun afterTzeitTheOmerIsTonightsCount() {
        val day = firstDayIn(2027) { it.dayOfOmer == 10 }
        assertEquals(DayEvent.Omer(CalendarText.Omer(day, true)), omer(facts(day, beforeTzeit(day))))
        assertEquals(DayEvent.Omer(CalendarText.Omer(day.plusDays(1), true)), omer(facts(day, afterTzeit(day))))
    }

    @Test
    fun afterTheFortyNinthDayTheOmerRowGoesAtTzeit() {
        val last = firstDayIn(2027) { it.dayOfOmer == 49 }
        assertNotNull(omer(facts(last, beforeTzeit(last))))
        assertNull(omer(facts(last, afterTzeit(last))))
    }

    @Test
    fun chanukahsFirstCandleShowsFromTzeitOn24Kislev() {
        val firstDay = firstDayIn(2026) { it.isChanukah }
        val erev = firstDay.minusDays(1)
        assertNull(chanukah(facts(erev, beforeTzeit(erev))))
        assertEquals(DayEvent.Chanukah(1), chanukah(facts(erev, afterTzeit(erev))))
        assertEquals(DayEvent.Chanukah(3), chanukah(facts(firstDay.plusDays(1), afterTzeit(firstDay.plusDays(1)))))
        // The eighth day's evening lights nothing.
        val eighth = firstDay.plusDays(7)
        assertEquals(DayEvent.Chanukah(8), chanukah(facts(eighth, beforeTzeit(eighth))))
        assertNull(chanukah(facts(eighth, afterTzeit(eighth))))
    }

    @Test
    fun aSteppedToDayIsReadWhole() {
        // The repository reads a stepped-to day with no clock and the roll switched off.
        val erev = firstDayIn(2026) { it.isChanukah }.minusDays(1)
        assertNull(chanukah(facts(erev, null, rolled = false)))
        val pesach = firstDayIn(2027) { it.jewishMonth == JewishDate.NISSAN && it.jewishDayOfMonth == 15 }
        assertNull(omer(facts(pesach, null, rolled = false)))
        assertEquals(DayEvent.Omer(CalendarText.Omer(pesach.plusDays(1), true)), omer(facts(pesach.plusDays(1), null, rolled = false)))
    }

    @Test
    fun erevPesachOnShabbatBurnsOnFridayAndOnlyEatsOnShabbat() {
        // 14 Nisan on Shabbat: checked every year to 2045, so each one is covered.
        val shabbatYears = (2026..2045).mapNotNull { year ->
            val erevPesach = firstDayIn(year) { it.yomTovIndex == JewishCalendar.EREV_PESACH }
            erevPesach.takeIf { it.dayOfWeek == DayOfWeek.SATURDAY }
        }
        assertEquals(listOf(2045), shabbatYears.map { it.year })
        for (shabbat in shabbatYears) {
            val friday = shabbat.minusDays(1)
            val onFriday = facts(friday, null).events.filterIsInstance<DayEvent.Chametz>().single()
            assertFalse(onFriday.eats)
            assertTrue(onFriday.burns)
            val fridayBurn = requireNotNull(onFriday.burnUntil)
            assertEquals(friday, fridayBurn.atZone(location.zoneId).toLocalDate())

            val onShabbat = facts(shabbat, null).events.filterIsInstance<DayEvent.Chametz>().single()
            assertTrue(onShabbat.eats)
            assertFalse(onShabbat.burns)
            assertEquals(shabbat, requireNotNull(onShabbat.eatUntil).atZone(location.zoneId).toLocalDate())
        }
    }

    @Test
    fun anOrdinaryErevPesachHasBothRowsAndTheDayBeforeNone() {
        val erevPesach = firstDayIn(2027) { it.yomTovIndex == JewishCalendar.EREV_PESACH }
        val chametz = facts(erevPesach, null).events.filterIsInstance<DayEvent.Chametz>().single()
        assertTrue(chametz.eats && chametz.burns)
        assertTrue(facts(erevPesach.minusDays(1), null).events.none { it is DayEvent.Chametz })
    }
}
