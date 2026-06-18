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
internal const val DailyLearningGroupTitle = "Daily Learning"
