// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.i18n

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/** The Rosh Chodesh row's value is the month alone, the same on both days, Adar I and II apart. */
class RoshChodeshMonthTest {
    private val hebrew = HebrewLanguageFormatter()
    private val english = EnglishLanguageFormatter()

    private fun firstOf(month: Int): LocalDate = JewishCalendar(5787, month, 1).localDate

    private fun assertBothDays(firstOfMonth: LocalDate, hebrewName: String, englishName: String) {
        for (day in listOf(firstOfMonth.minusDays(1), firstOfMonth)) {
            assertEquals("$day", hebrewName, hebrew.format(CalendarText.RoshChodeshMonth(day)))
            assertEquals("$day", englishName, english.format(CalendarText.RoshChodeshMonth(day)))
        }
    }

    @Test
    fun cheshvanOnBothDays() = assertBothDays(firstOf(JewishCalendar.CHESHVAN), "חשון", "Cheshvan")

    @Test
    fun adarIInALeapYear() = assertBothDays(firstOf(JewishCalendar.ADAR), "אדר א׳", "Adar I")

    @Test
    fun adarIIInALeapYear() = assertBothDays(firstOf(JewishCalendar.ADAR_II), "אדר ב׳", "Adar II")

    @Test
    fun aOneDayRoshChodesh() {
        // 5787's Kislev has 30 days or not; either way the 1st of Tevet reads "Tevet".
        val first = firstOf(JewishCalendar.TEVES)
        assertEquals("טבת", hebrew.format(CalendarText.RoshChodeshMonth(first)))
        assertEquals("Tevet", english.format(CalendarText.RoshChodeshMonth(first)))
    }
}
