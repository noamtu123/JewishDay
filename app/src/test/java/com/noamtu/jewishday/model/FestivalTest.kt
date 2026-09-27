// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Which days carry a festival's picture: the festival's own days and its erev, and nothing else. */
class FestivalTest {
    private fun on(month: Int, day: Int, year: Int = 5787, inIsrael: Boolean = true): Festival? =
        festivalOf(
            JewishCalendar(year, month, day, inIsrael).apply {
                isUseModernHolidays = true
            },
        )

    @Test
    fun theErevAlreadyBelongsToItsFestival() {
        assertEquals(Festival.Sukkot, on(JewishDate.TISHREI, 14))
        assertEquals(Festival.Pesach, on(JewishDate.NISSAN, 14))
        assertEquals(Festival.YomKippur, on(JewishDate.TISHREI, 9))
    }

    @Test
    fun theIntermediateDaysKeepTheFestivalsPicture() {
        assertEquals(Festival.Sukkot, on(JewishDate.TISHREI, 18))
        assertEquals(Festival.Sukkot, on(JewishDate.TISHREI, 21))
        assertEquals(Festival.Pesach, on(JewishDate.NISSAN, 18))
    }

    @Test
    fun simchatTorahFollowsTheCalendarOfWhereYouAre() {
        assertEquals(Festival.SimchatTorah, on(JewishDate.TISHREI, 22))
        assertNull(on(JewishDate.TISHREI, 23))
        assertEquals(Festival.SimchatTorah, on(JewishDate.TISHREI, 23, inIsrael = false))
    }

    @Test
    fun fastsRoshChodeshAndOrdinaryDaysHaveNone() {
        assertNull(on(JewishDate.TAMMUZ, 17))
        assertNull(on(JewishDate.CHESHVAN, 1))
        assertNull(on(JewishDate.CHESHVAN, 10))
    }

    @Test
    fun everyFestivalComesRoundInAYear() {
        val calendar = JewishCalendar(5786, JewishDate.TISHREI, 1, true).apply { isUseModernHolidays = true }
        val seen = mutableSetOf<Festival>()
        while (calendar.jewishYear == 5786) {
            festivalOf(calendar)?.let(seen::add)
            calendar.forward(Calendar.DATE, 1)
        }

        assertEquals(Festival.entries.toSet(), seen)
    }
}
