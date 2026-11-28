// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.i18n

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.YerushalmiYomiCalculator
import com.noamtu.jewishday.core.model.jewishCalendar
import com.noamtu.jewishday.core.model.kitzurShulchanAruch
import com.noamtu.jewishday.core.model.mishnahYomi
import com.noamtu.jewishday.core.model.rambamYomi
import com.noamtu.jewishday.core.model.shemiratHaLashon
import com.noamtu.jewishday.core.model.tanakhYomi
import com.noamtu.jewishday.core.model.tehillimRangeForDay

/**
 * Hebrew: KosherJava's own Hebrew names and Hebrew numerals ("ה׳ תשרי תשפ״ז"), and the daily-learning
 * references punctuated with geresh and gershayim so every track writes its numbers the same way.
 */
public class HebrewLanguageFormatter : LanguageFormatter {
    private val formatter = HebrewDateFormatter().apply { isHebrewFormat = true }

    override val rambamChapterWords: List<String> = listOf("פרקים", "פרק")

    override fun rambamRowReference(reference: String): String = reference.removePrefix("הלכות ").trimStart()

    override fun format(text: CalendarText): String = when (text) {
        is CalendarText.HebrewDate -> formatter.format(JewishCalendar(text.date))
        is CalendarText.Parsha -> formatter.formatParshah(jewishCalendar(text.date, text.inIsrael))
        is CalendarText.YomTov -> formatter.formatYomTov(jewishCalendar(text.date, text.inIsrael))
        is CalendarText.RoshChodesh -> formatter.formatRoshChodesh(jewishCalendar(text.date, text.inIsrael))
        is CalendarText.RoshChodeshMonth -> formatter.formatMonth(roshChodeshMonthCalendar(text.date))
        is CalendarText.Omer -> formatter.formatOmer(jewishCalendar(text.date, text.inIsrael))
        is CalendarText.DafYomiBavli -> formatter.formatDafYomiBavli(jewishCalendar(text.date, text.inIsrael).dafYomiBavli)
        is CalendarText.DafYomiYerushalmi -> formatter.formatDafYomiYerushalmi(
            YerushalmiYomiCalculator.getDafYomiYerushalmi(jewishCalendar(text.date, text.inIsrael)),
        )
        is CalendarText.TehillimYomi -> {
            val calendar = jewishCalendar(text.date, text.inIsrael)
            val range = tehillimRangeForDay(calendar.jewishDayOfMonth, calendar.daysInJewishMonth)
            fun number(value: Int) = formatter.formatHebrewNumber(value)
            if (range.verseStart == null) {
                "תהילים ${number(range.chapterStart)}-${number(range.chapterEnd)}"
            } else {
                "תהילים ${number(range.chapterStart)}:${number(range.verseStart)}-${number(range.verseEnd ?: range.verseStart)}"
            }
        }
        is CalendarText.MishnahYomi -> mishnahYomi(text.date)?.hebrew().orEmpty()
        is CalendarText.RambamYomi -> rambamYomi(text.date, text.chaptersPerDay)?.hebrew().orEmpty()
        is CalendarText.TanakhYomi -> tanakhYomi(text.date)?.hebrew().orEmpty()
        is CalendarText.ShemiratHaLashon -> shemiratHaLashon(text.date)?.hebrew().orEmpty()
        is CalendarText.KitzurShulchanAruch -> kitzurShulchanAruch(text.date)?.hebrew().orEmpty()
    }
}
