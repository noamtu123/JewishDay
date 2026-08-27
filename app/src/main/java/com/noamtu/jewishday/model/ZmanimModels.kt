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
    val date: LocalDate,
    val zoneId: ZoneId,
    // The Jewish (Hebrew) calendar date, formatted for the date header at the top of the tab.
    val hebrewDateEnglish: String,
    val hebrewDateHebrew: String,
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
 * A single melacha-forbidden day — Shabbat or one day of a Yom Tov — with its own entry and exit.
 * Days that run back to back are shown one after the other rather than as one span, each replaced
 * by the next as it goes out, with [followedBy] warning that another begins the moment this ends.
 */
data class HolyDayInfo(
    val name: String,
    val nameHebrew: String,
    val startTime: Instant?,
    val endTime: Instant?,
    // How this day's own times are named — "Shabbat", "Yom Tov", or "first day" / "second day"
    // when a Yom Tov runs more than one day.
    val term: String,
    val termHebrew: String,
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