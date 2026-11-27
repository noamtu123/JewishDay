// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the core works out for a day, before the app words any of it (Jerusalem). */
class ZmanimDayFactsTest {
    private val location = defaultJerusalemLocation

    private fun firstDayIn(year: Int, test: (JewishCalendar) -> Boolean): LocalDate =
        generateSequence(LocalDate.of(year, 1, 1)) { it.plusDays(1) }
            .first { test(jewishCalendar(it, inIsrael = true)) }

    @Test
    fun theZmanimAreListedInOrderWithRabbeinuTamOnlyOnShabbatItself() {
        val tuesday = LocalDate.of(2026, 9, 1)
        val options = zmanimDayFacts(location, tuesday).zmanim.map { it.option }
        assertEquals(ZmanimTimeOption.AlotHashachar, options.first())
        assertEquals(ZmanimTimeOption.ChatzotHaLaila, options.last())
        assertFalse(ZmanimTimeOption.RabbeinuTam in options)
    }

    @Test
    fun onShabbatTheSectionGivesWayAndRabbeinuTamJoinsTheList() {
        val saturday = LocalDate.of(2026, 9, 5)
        val noon = saturday.atTime(12, 0).atZone(location.zoneId).toInstant()
        val facts = zmanimDayFacts(location, saturday, now = noon)
        assertNull(facts.shabbat)
        assertTrue(facts.zmanim.any { it.option == ZmanimTimeOption.RabbeinuTam })
        assertNotNull(facts.weeklyParsha)
    }

    @Test
    fun beforeShabbatTheSectionHasItsTimes() {
        val wednesday = LocalDate.of(2026, 9, 2)
        val shabbat = requireNotNull(zmanimDayFacts(location, wednesday).shabbat)
        val candles = requireNotNull(shabbat.candleLighting)
        val sunset = requireNotNull(shabbat.sunset)
        assertEquals(DayOfWeek.FRIDAY, candles.time.atZone(location.zoneId).dayOfWeek)
        assertTrue(candles.time.isBefore(sunset))
        assertTrue(requireNotNull(shabbat.exit).isAfter(sunset.plus(Duration.ofHours(20))))
        assertNull("a weekly parsha leaves the day unnamed", shabbat.dayName)
    }

    @Test
    fun erevPesachListsBothChametzTimesInOrder() {
        val erevPesach = firstDayIn(2027) { it.yomTovIndex == JewishCalendar.EREV_PESACH }
        val chametz = zmanimDayFacts(location, erevPesach).events.filterIsInstance<DayEvent.Chametz>().single()
        assertTrue(requireNotNull(chametz.eatUntil).isBefore(requireNotNull(chametz.burnUntil)))
    }

    @Test
    fun chanukahCountsItsDays() {
        val firstCandle = firstDayIn(2026) { it.isChanukah }
        val events = zmanimDayFacts(location, firstCandle.plusDays(2)).events
        assertEquals(DayEvent.Chanukah(3), events.filterIsInstance<DayEvent.Chanukah>().single())
    }

    @Test
    fun theYerushalmiRowGoesOnTheDaysItsCycleSkips() {
        val yomKippur = firstDayIn(2026) { it.yomTovIndex == JewishCalendar.YOM_KIPPUR }
        val tracks = { date: LocalDate -> zmanimDayFacts(location, date).offlineLearning.map { it.track } }
        // Three days before (a Friday) every track has a reading; two before is Shabbat, Tanakh Yomi's day off.
        assertEquals(OfflineLearningTrack.entries, tracks(yomKippur.minusDays(3)))
        assertFalse(OfflineLearningTrack.DafYomiYerushalmi in tracks(yomKippur))
    }

    @Test
    fun aFastDayIsAnnouncedByWhichFastItIs() {
        val gedalyah = firstDayIn(2026) { it.yomTovIndex == JewishCalendar.FAST_OF_GEDALYAH }
        val noon = gedalyah.atTime(12, 0).atZone(location.zoneId).toInstant()
        val fast = requireNotNull(zmanimDayFacts(location, gedalyah, now = noon).fastDayInfo)
        assertEquals(Fast.Gedalyah, fast.fast)
        assertTrue(fast.isUnderWay)
    }
}
