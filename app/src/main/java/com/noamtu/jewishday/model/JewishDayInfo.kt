// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import androidx.annotation.StringRes
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.noamtu.jewishday.R
import com.noamtu.jewishday.i18n.CalendarText
import com.noamtu.jewishday.i18n.UiText
import com.noamtu.jewishday.i18n.asUiText
import com.noamtu.jewishday.i18n.uiText
import java.time.DayOfWeek
import java.time.LocalDate

/** What the status-bar date icon shows: the day, and the Hebrew date it is. */
data class JewishDayInfo(
    val gregorianDate: LocalDate,
    // The civil date the Hebrew date belongs to — [gregorianDate], or the next day from tzeit on.
    val jewishDate: LocalDate,
    val hebrewDayOfMonth: Int,
    // The icon's glyph: the day of the month in Hebrew numerals ("ט״ו") in every language, since the
    // icon is the Hebrew date itself.
    val dayOfMonthGlyph: String,
) {
    // Weekday of [gregorianDate] — the civil day, so it rolls at midnight. Deliberately not the
    // Jewish weekday: in the status bar the day name is read on its own, and "Friday" appearing on
    // Thursday evening reads as a mistake there even though the Hebrew date beside it has rolled.
    // The app's own header does use the Jewish weekday, where the Hebrew date gives it context.
    val weekday: UiText get() = uiText(weekdayName(gregorianDate.dayOfWeek))

    val hebrewDate: UiText get() = CalendarText.HebrewDate(jewishDate).asUiText()
}

fun jewishDayInfo(date: LocalDate): JewishDayInfo = jewishDayInfo(gregorianDate = date, jewishDate = date)

fun jewishDayInfo(
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

/** The icon's own weekday names: Hebrew's Saturday is plain "שבת" there, not java.time's "יום שבת". */
@StringRes
private fun weekdayName(day: DayOfWeek): Int = when (day) {
    DayOfWeek.SUNDAY -> R.string.weekday_sunday
    DayOfWeek.MONDAY -> R.string.weekday_monday
    DayOfWeek.TUESDAY -> R.string.weekday_tuesday
    DayOfWeek.WEDNESDAY -> R.string.weekday_wednesday
    DayOfWeek.THURSDAY -> R.string.weekday_thursday
    DayOfWeek.FRIDAY -> R.string.weekday_friday
    DayOfWeek.SATURDAY -> R.string.weekday_saturday
}

fun hebrewDayOfMonthText(dayOfMonth: Int): String {
    require(dayOfMonth in 1..30) { "Hebrew day of month must be 1..30" }
    return when (dayOfMonth) {
        15 -> "ט״ו"
        16 -> "ט״ז"
        in 1..9 -> "${hebrewUnits[dayOfMonth]}׳"
        10, 20, 30 -> "${hebrewTens[dayOfMonth]}׳"
        else -> {
            val tens = dayOfMonth / 10 * 10
            val units = dayOfMonth % 10
            "${hebrewTens[tens]}״${hebrewUnits[units]}"
        }
    }
}

private val hebrewUnits = mapOf(
    1 to "א",
    2 to "ב",
    3 to "ג",
    4 to "ד",
    5 to "ה",
    6 to "ו",
    7 to "ז",
    8 to "ח",
    9 to "ט",
)

private val hebrewTens = mapOf(
    10 to "י",
    20 to "כ",
    30 to "ל",
)