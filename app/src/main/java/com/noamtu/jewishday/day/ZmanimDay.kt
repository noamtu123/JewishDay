// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.day

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.noamtu.jewishday.R
import com.noamtu.jewishday.core.model.FastDayInfo
import com.noamtu.jewishday.core.model.HolyDayInfo
import com.noamtu.jewishday.i18n.UiText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * One civil day as the zmanim screen, the widgets and the repository show it: the header's facts and
 * the rows below them. Built by [zmanimForDate] from the core's `ZmanimDayFacts`; the rows hold
 * [UiText], so the same day reads in whichever language is showing.
 */
@Immutable
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
    // Populated only around one of the six fasts, for the date header (worded by `nameText`).
    val fastDayInfo: FastDayInfo? = null,
    // Populated from one Jewish day before a holy day enters until it ends, for the date header.
    val holyDayInfo: HolyDayInfo? = null,
    // Which of the two names the header chip, when a fast and a holy day are both showing: the one
    // happening now rather than the one merely announced.
    val fastLeadsHeader: Boolean = false,
)

/**
 * One row: a time, or a value in place of one. None of it is worded yet — see [UiText] — so the same
 * row reads in whichever language is showing.
 */
@Immutable
data class ZmanItem(
    val title: UiText,
    val time: Instant?,
    val description: UiText,
    val value: UiText? = null,
    // Stable id for show/hide filtering (ZmanimTimeOption / DailyLearningType storageValue).
    // Null means the row is always shown and never user-toggleable.
    val id: String? = null,
    // What a row without an [id] is, so the screen can key it by what it is: the list keeps each
    // row's place (and its animation) as the rows around it come and go.
    val kind: ZmanItemKind? = null,
)

/** The rows without an [ZmanItem.id] that the screen keys by what they are. */
enum class ZmanItemKind { RoshChodesh, Omer, WeeklyParsha, TorahReading }

@Immutable
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

/** The day with Hebcal's daily learning in place of the offline rows, when Hebcal sent any. */
fun ZmanimDay.withDailyLearningItems(items: List<ZmanItem>): ZmanimDay = copy(
    groups = groups.map { group ->
        if (group.kind == ZmanimGroupKind.DailyLearning) {
            group.copy(items = items.ifEmpty { group.items })
        } else {
            group
        }
    },
)
