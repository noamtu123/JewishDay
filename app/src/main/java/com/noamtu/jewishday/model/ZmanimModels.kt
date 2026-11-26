// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import androidx.annotation.StringRes
import com.noamtu.jewishday.R
import com.noamtu.jewishday.i18n.UiText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * One row: a time, or a value in place of one. None of it is worded yet — see [UiText] — so the same
 * row reads in whichever language is showing.
 */
data class ZmanItem(
    val title: UiText,
    val time: Instant?,
    val description: UiText,
    val value: UiText? = null,
    // Stable id for show/hide filtering (ZmanimTimeOption / DailyLearningType storageValue).
    // Null means the row is always shown and never user-toggleable.
    val id: String? = null,
    // What the row is, for the few places that look for a particular one (the widgets read Rosh
    // Chodesh and the Omer off the day's events, and the parsha off the Shabbat section).
    val kind: ZmanItemKind? = null,
)

/** The rows something outside the zmanim list looks for by what they are, not by their id. */
enum class ZmanItemKind { RoshChodesh, Omer, WeeklyParsha, TorahReading }

data class ZmanimDay(
    val locationName: String,
    // The civil date these zmanim are for. It turns over at midnight like any calendar day, and is
    // whatever day the user has stepped to.
    val date: LocalDate,
    // The civil date the Hebrew date belongs to: [date] until tzeit, the day after from tzeit on.
    // The header's weekday comes from here, so it rolls with the Hebrew date rather than at
    // midnight — Thursday evening is already "Friday".
    val displayedDate: LocalDate,
    val zoneId: ZoneId,
    // The Jewish (Hebrew) calendar date, for the date header at the top of the tab.
    val hebrewDate: UiText,
    // The day's own name — "ערב פסח", "פורים", "חול המועד סוכות" — for the header chip, on the days
    // no fast or holy day already claims it. Null on an ordinary day.
    val dayName: UiText? = null,
    val groups: List<ZmanimGroup>,
    // Populated only on one of the six fasts, for the date header.
    val fastDayInfo: FastDayInfo? = null,
    // Populated from one Jewish day before a holy day enters until it ends, for the date header.
    val holyDayInfo: HolyDayInfo? = null,
    // Which of the two names the header chip, when a fast and a holy day are both showing: the one
    // happening now rather than the one merely announced.
    val fastLeadsHeader: Boolean = false,
)

/**
 * A melacha-forbidden stretch — Shabbat, a Yom Tov, or several of them running back to back — as
 * one span: it enters when its first day enters and goes out when its last day does, so nothing has
 * to be pieced together from a card that changes at every boundary in between. [sequel] says what
 * the stretch is made of, and [name] still follows the day currently in.
 */
data class HolyDayInfo(
    val name: UiText,
    // What the header chip says while the day is only announced — "פרשת נצבים" — so the entry/exit
    // card is labelled without the chip claiming Shabbat is in. Null for a Yom Tov (no parsha).
    val parsha: UiText? = null,
    // The whole stretch: its first day's entry and its last day's exit.
    val startTime: Instant?,
    val endTime: Instant?,
    // What the two times are named, each by the day it falls on, so the pair says how far the stretch
    // reaches: "כניסת חג ראשון" … "צאת חג שני" for a two-day Yom Tov, and "כניסת החג" … "צאת שבת" for
    // one running into Shabbat.
    val entryTerm: HolyDayTerm,
    val exitTerm: HolyDayTerm,
    // True while the day is actually in — from its entry until its exit. The times show a day
    // earlier than that; the name does not.
    val isUnderWay: Boolean = true,
    // What is still ahead, when more than one observance runs together: "חג כפול + צום".
    val sequel: UiText? = null,
)

/**
 * Which day one end of a holy-day span falls on, so its label can say so: "כניסת חג ראשון" …
 * "צאת חג שני". [YomTov] is a span's only Yom Tov day; the ordinals count Yom Tov days in a row.
 */
enum class HolyDayTerm(@StringRes val starts: Int, @StringRes val ends: Int) {
    Shabbat(R.string.holy_day_starts_shabbat, R.string.holy_day_ends_shabbat),
    YomTov(R.string.holy_day_starts_yom_tov, R.string.holy_day_ends_yom_tov),
    FirstDay(R.string.holy_day_starts_first_day, R.string.holy_day_ends_first_day),
    SecondDay(R.string.holy_day_starts_second_day, R.string.holy_day_ends_second_day),
    ThirdDay(R.string.holy_day_starts_third_day, R.string.holy_day_ends_third_day),
}

data class FastDayInfo(
    val name: UiText,
    val startTime: Instant?,
    val endTime: Instant?,
    // True while the fast is actually running. The times show a day ahead, but the name waits:
    // the Hebrew date rolls at sunset, so a dawn fast owns its date all night before it begins.
    val isUnderWay: Boolean = true,
)

data class ZmanimGroup(
    val kind: ZmanimGroupKind,
    val items: List<ZmanItem>,
)

/** The sections of the zmanim screen, in order. [title] is null for the untitled day events. */
enum class ZmanimGroupKind(@StringRes val title: Int?) {
    // The day's occasional events (Rosh Chodesh, the Omer, Erev Pesach's chametz times…).
    Events(null),
    Zmanim(R.string.group_zmanim),
    Shabbat(R.string.group_shabbat),
    DailyLearning(R.string.group_daily_learning),
}

fun ZmanimDay.withDailyLearningItems(items: List<ZmanItem>): ZmanimDay = copy(
    groups = groups.map { group ->
        if (group.kind == ZmanimGroupKind.DailyLearning) {
            group.copy(items = items.ifEmpty { group.items })
        } else {
            group
        }
    },
)