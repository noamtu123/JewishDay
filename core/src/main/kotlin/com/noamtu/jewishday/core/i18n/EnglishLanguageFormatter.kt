// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.i18n

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.YerushalmiYomiCalculator
import com.noamtu.jewishday.core.model.englishHebrewDateFormatter
import com.noamtu.jewishday.core.model.jewishCalendar
import com.noamtu.jewishday.core.model.tehillimRangeForDay

/**
 * English, and the fallback for every language without a formatter of its own: KosherJava's
 * transliterated names, with the modern spelling the app uses ("Sukkot", "Tevet", "Bereshit") rather
 * than KosherJava's Ashkenazi defaults — see [englishHebrewDateFormatter].
 */
public class EnglishLanguageFormatter : LanguageFormatter {
    private val formatter = englishHebrewDateFormatter()

    override val rambamChapterWords: List<String> = listOf("chapters", "chapter")

    override fun format(text: CalendarText): String = when (text) {
        is CalendarText.HebrewDate -> formatter.format(JewishCalendar(text.date))
        is CalendarText.Parsha -> formatter.formatParshah(jewishCalendar(text.date, text.inIsrael))
        is CalendarText.YomTov -> formatter.formatYomTov(jewishCalendar(text.date, text.inIsrael))
        is CalendarText.RoshChodesh -> formatter.formatRoshChodesh(jewishCalendar(text.date, text.inIsrael))
        is CalendarText.Omer -> formatter.formatOmer(jewishCalendar(text.date, text.inIsrael))
        is CalendarText.DafYomiBavli -> formatter.formatDafYomiBavli(jewishCalendar(text.date, text.inIsrael).dafYomiBavli)
        is CalendarText.DafYomiYerushalmi -> formatter.formatDafYomiYerushalmi(
            YerushalmiYomiCalculator.getDafYomiYerushalmi(jewishCalendar(text.date, text.inIsrael)),
        )
        is CalendarText.TehillimYomi -> {
            val calendar = jewishCalendar(text.date, text.inIsrael)
            val range = tehillimRangeForDay(calendar.jewishDayOfMonth, calendar.daysInJewishMonth)
            if (range.verseStart == null) {
                "Psalms ${range.chapterStart}-${range.chapterEnd}"
            } else {
                "Psalm ${range.chapterStart}:${range.verseStart}-${range.verseEnd}"
            }
        }
        is CalendarText.Hebcal -> text.entry.englishText()
    }
}
