// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.i18n

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.noamtu.jewishday.core.i18n.CalendarText
import com.noamtu.jewishday.core.i18n.LanguageFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Text that is not worded yet. The model and view-model layers describe what is to be said — a
 * string resource and its arguments, a date, a time, a calendar name — and only the edges (a
 * composable, a widget, the notification) word it, through a [TextResolver] for the app's current
 * language. Nothing below the edges ever holds a finished sentence, so a language change is just a
 * new resolver, and a new language is a new translation folder.
 *
 * Arguments are [UiText] too: they are worded first, in the same language, and a [Number] fills a
 * `%d` as the number it is.
 */
@Immutable
sealed interface UiText {
    /** A string resource, with its format arguments. */
    data class Res(@StringRes val id: Int, val args: List<UiText> = emptyList()) : UiText

    /** A plurals resource for [count]; [args] defaults to the count itself. */
    data class Plural(@PluralsRes val id: Int, val count: Int, val args: List<UiText> = listOf(Number(count))) : UiText

    /** A whole number, for a `%d` placeholder; on its own it reads as its digits. */
    data class Number(val value: Int) : UiText

    /** Data that is not the app's own wording and is never translated: a place name, a version. */
    data class Verbatim(val text: String) : UiText

    /** A name only a language's own calendar formatter can give — see [LanguageFormatter]. */
    data class Calendar(val text: CalendarText) : UiText

    /** A time of day, as the user set the clock (24 hours or AM/PM), in the language's own words. */
    data class Time(
        val instant: Instant,
        val zoneId: ZoneId,
        val use24Hour: Boolean,
        // Prefix the short weekday — for a time that falls on another day than the one shown.
        val withWeekday: Boolean = false,
    ) : UiText

    /** A civil date in one of the app's fixed styles. */
    data class Date(val date: LocalDate, val style: DateStyle) : UiText
}

/** The ways the app writes a civil date. */
enum class DateStyle {
    /** "Sunday", "יום ראשון". */
    Weekday,

    /** "Sun", "יום א׳". */
    ShortWeekday,

    /** "October 11", "11 באוקטובר" — the pattern is a translatable resource (`date_pattern_day_month`). */
    DayMonth,
}

fun uiText(@StringRes id: Int, vararg args: UiText): UiText = UiText.Res(id, args.toList())

fun pluralText(@PluralsRes id: Int, count: Int, vararg args: UiText): UiText =
    UiText.Plural(id, count, if (args.isEmpty()) listOf(UiText.Number(count)) else args.toList())

fun verbatim(text: String): UiText = UiText.Verbatim(text)

fun CalendarText.asUiText(): UiText = UiText.Calendar(this)
