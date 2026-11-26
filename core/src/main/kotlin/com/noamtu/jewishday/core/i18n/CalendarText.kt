// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.i18n

import androidx.compose.runtime.Immutable
import com.noamtu.jewishday.core.model.HebcalLearningEntry
import java.time.LocalDate

/**
 * The calendar's own names — a Hebrew date, a parsha, a holiday, a daf — as facts rather than
 * words. They cannot be plain string resources: each comes out of a calendar computation, and each
 * language writes it its own way (Hebrew numerals and KosherJava's Hebrew lists for Hebrew, the
 * modern transliteration for English). A [LanguageFormatter] turns one into text.
 *
 * Every fact names the civil date (and, where the calendar differs, Israel or not) it is read off,
 * so the formatter rebuilds exactly the calendar the model used.
 */
@Immutable
public sealed interface CalendarText {
    /** "ה׳ תשרי תשפ״ז", "5 Tishrei, 5787". */
    public data class HebrewDate(val date: LocalDate) : CalendarText

    /** The weekly parsha read on [date] (a Shabbat). */
    public data class Parsha(val date: LocalDate, val inIsrael: Boolean) : CalendarText

    /** The holiday or named day falling on [date]. */
    public data class YomTov(val date: LocalDate, val inIsrael: Boolean) : CalendarText

    /** "ראש חודש חשון", "Rosh Chodesh Cheshvan". */
    public data class RoshChodesh(val date: LocalDate, val inIsrael: Boolean) : CalendarText

    /** That day's count of the Omer. */
    public data class Omer(val date: LocalDate, val inIsrael: Boolean) : CalendarText

    /** KosherJava's Daf Yomi Bavli for [date]. */
    public data class DafYomiBavli(val date: LocalDate, val inIsrael: Boolean) : CalendarText

    /** KosherJava's Daf Yomi Yerushalmi for [date]; only asked for on days the cycle has one. */
    public data class DafYomiYerushalmi(val date: LocalDate, val inIsrael: Boolean) : CalendarText

    /** The day's portion of the monthly Tehillim division. */
    public data class TehillimYomi(val date: LocalDate, val inIsrael: Boolean) : CalendarText

    /** One of Hebcal's daily-learning readings, as Hebcal sent it. */
    public data class Hebcal(val entry: HebcalLearningEntry) : CalendarText
}
