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
)

data class ZmanimDay(
    val locationName: String,
    val date: LocalDate,
    val zoneId: ZoneId,
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

internal const val DailyLearningGroupTitle = "Daily Learning"
