// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import java.time.LocalDate

data class JewishDayInfo(
    val gregorianDate: LocalDate,
    val hebrewDayOfMonth: Int,
    val hebrewDayOfMonthHebrew: String,
    val hebrewDateEnglish: String,
    val hebrewDateHebrew: String,
)

fun jewishDayInfo(date: LocalDate): JewishDayInfo {
    val jewishCalendar = JewishCalendar(date)
    return jewishDayInfo(
        gregorianDate = date,
        jewishCalendar = jewishCalendar,
    )
}

fun jewishDayInfo(
    gregorianDate: LocalDate,
    jewishDate: LocalDate,
): JewishDayInfo = jewishDayInfo(
    gregorianDate = gregorianDate,
    jewishCalendar = JewishCalendar(jewishDate),
)

private fun jewishDayInfo(
    gregorianDate: LocalDate,
    jewishCalendar: JewishCalendar,
): JewishDayInfo {
    val englishFormatter = HebrewDateFormatter()
    val hebrewFormatter = HebrewDateFormatter().apply {
        isHebrewFormat = true
    }

    return JewishDayInfo(
        gregorianDate = gregorianDate,
        hebrewDayOfMonth = jewishCalendar.jewishDayOfMonth,
        hebrewDayOfMonthHebrew = hebrewDayOfMonthText(jewishCalendar.jewishDayOfMonth),
        hebrewDateEnglish = englishFormatter.format(jewishCalendar),
        hebrewDateHebrew = hebrewFormatter.format(jewishCalendar),
    )
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