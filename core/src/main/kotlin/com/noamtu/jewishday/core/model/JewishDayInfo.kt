// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

import androidx.compose.runtime.Immutable
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.noamtu.jewishday.core.i18n.hebrewNumber
import java.time.LocalDate

/**
 * What the status-bar date icon shows: the day, and the Hebrew date it is.
 *
 * Its weekday is that of [gregorianDate] — the civil day, so it rolls at midnight. Deliberately not
 * the Jewish weekday: in the status bar the day name is read on its own, and "Friday" appearing on
 * Thursday evening reads as a mistake there even though the Hebrew date beside it has rolled. The
 * app's own header does use the Jewish weekday, where the Hebrew date gives it context.
 */
@Immutable
public data class JewishDayInfo(
    val gregorianDate: LocalDate,
    // The civil date the Hebrew date belongs to — [gregorianDate], or the next day from tzeit on.
    val jewishDate: LocalDate,
    val hebrewDayOfMonth: Int,
    // The icon's glyph: the day of the month in Hebrew numerals ("ט״ו") in every language, since the
    // icon is the Hebrew date itself.
    val dayOfMonthGlyph: String,
)

public fun jewishDayInfo(date: LocalDate): JewishDayInfo = jewishDayInfo(gregorianDate = date, jewishDate = date)

public fun jewishDayInfo(
    gregorianDate: LocalDate,
    jewishDate: LocalDate,
): JewishDayInfo {
    val jewishCalendar = JewishCalendar(jewishDate)
    return JewishDayInfo(
        gregorianDate = gregorianDate,
        jewishDate = jewishDate,
        hebrewDayOfMonth = jewishCalendar.jewishDayOfMonth,
        dayOfMonthGlyph = hebrewDayOfMonthText(jewishCalendar.jewishDayOfMonth),
    )
}

/** A Hebrew day of the month in punctuated Hebrew numerals: "א׳", "ט״ו", "כ״ט". */
public fun hebrewDayOfMonthText(dayOfMonth: Int): String {
    require(dayOfMonth in 1..30) { "Hebrew day of month must be 1..30" }
    return hebrewNumber(dayOfMonth)
}
