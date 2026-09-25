// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ZmanItem(
    val title: String,
    val titleHebrew: String,
    val time: Instant?,
    val description: String,
    val descriptionHebrew: String,
    val value: String? = null,
    val valueHebrew: String? = value,
    // Stable id for show/hide filtering (ZmanimTimeOption / DailyLearningType storageValue).
    // Null means the row is always shown and never user-toggleable.
    val id: String? = null,
)

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
    // The Jewish (Hebrew) calendar date, formatted for the date header at the top of the tab.
    val hebrewDateEnglish: String,
    val hebrewDateHebrew: String,
    // The day's own name — "ערב פסח", "פורים", "חול המועד סוכות" — for the header chip, on the days
    // no fast or holy day already claims it. Null on an ordinary day.
    val dayName: String? = null,
    val dayNameHebrew: String? = null,
    val groups: List<ZmanimGroup>,
    // Populated only on one of the six fasts, for the date header.
    val fastDayInfo: FastDayInfo? = null,
    // Populated from one Jewish day before a holy day enters until it ends, for the date header.
    val holyDayInfo: HolyDayInfo? = null,
    // Which of the two names the header chip, when a fast and a holy day are both showing: the one
    // happening now rather than the one merely announced.
    val fastLeadsHeader: Boolean = false,
    // The festival the displayed Hebrew date belongs to, erev included, for the widget's picture of
    // it. Null on any other day.
    val festival: Festival? = null,
)

/**
 * A melacha-forbidden stretch — Shabbat, a Yom Tov, or several of them running back to back — as
 * one span: it enters when its first day enters and goes out when its last day does, so nothing has
 * to be pieced together from a card that changes at every boundary in between. [sequel] says what
 * the stretch is made of, and [name] still follows the day currently in.
 */
data class HolyDayInfo(
    val name: String,
    val nameHebrew: String,
    // What the header chip says while the day is only announced — "פרשת נצבים" — so the entry/exit
    // card is labelled without the chip claiming Shabbat is in. Null for a Yom Tov (no parsha).
    val parsha: String? = null,
    val parshaHebrew: String? = null,
    // The whole stretch: its first day's entry and its last day's exit.
    val startTime: Instant?,
    val endTime: Instant?,
    // What the two times are named, each by the day it falls on, so the pair says how far the stretch
    // reaches: "כניסת חג ראשון" … "צאת חג שני" for a two-day Yom Tov, and "כניסת החג" … "צאת שבת" for
    // one running into Shabbat.
    val entryTerm: String,
    val entryTermHebrew: String,
    val exitTerm: String,
    val exitTermHebrew: String,
    // True while the day is actually in — from its entry until its exit. The times show a day
    // earlier than that; the name does not.
    val isUnderWay: Boolean = true,
    // What is still ahead, when more than one observance runs together: "חג כפול + צום".
    val sequel: String? = null,
    val sequelHebrew: String? = null,
)

data class FastDayInfo(
    val name: String,
    val nameHebrew: String,
    val startTime: Instant?,
    val endTime: Instant?,
    // True while the fast is actually running. The times show a day ahead, but the name waits:
    // the Hebrew date rolls at sunset, so a dawn fast owns its date all night before it begins.
    val isUnderWay: Boolean = true,
)

data class ZmanimGroup(
    val title: String,
    val titleHebrew: String,
    val items: List<ZmanItem>,
)

fun ZmanimDay.withDailyLearningItems(items: List<ZmanItem>): ZmanimDay = copy(
    groups = groups.map { group ->
        if (group.title == DailyLearningGroupTitle) {
            group.copy(items = items.ifEmpty { group.items })
        } else {
            group
        }
    },
)

internal const val ZmanimGroupTitle = "Zmanim"
internal const val ShabbatGroupTitle = "Shabbat"
internal const val DailyLearningGroupTitle = "Daily Learning"