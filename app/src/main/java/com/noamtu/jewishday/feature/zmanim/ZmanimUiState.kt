// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.zmanim

import androidx.compose.runtime.Immutable
import com.noamtu.jewishday.R
import com.noamtu.jewishday.core.model.DailyLearningType
import com.noamtu.jewishday.core.model.ZmanimTimeOption
import com.noamtu.jewishday.day.ZmanimDay
import com.noamtu.jewishday.day.ZmanimGroupKind
import com.noamtu.jewishday.i18n.DateStyle
import com.noamtu.jewishday.i18n.UiText
import com.noamtu.jewishday.i18n.ends
import com.noamtu.jewishday.i18n.nameText
import com.noamtu.jewishday.i18n.parshaText
import com.noamtu.jewishday.i18n.pluralText
import com.noamtu.jewishday.i18n.sequelText
import com.noamtu.jewishday.i18n.starts
import com.noamtu.jewishday.i18n.uiText
import com.noamtu.jewishday.i18n.verbatim
import java.time.Instant
import kotlinx.coroutines.flow.map

@Immutable
data class ZmanimUiState(
    val header: ZmanimHeaderUi? = null,
    val groups: List<ZmanimGroupUi> = emptyList(),
    val showCandleLightingPrompt: Boolean = false,
    /** How many days from today the screen is showing; 0 is today. */
    val dayOffset: Int = 0,
    /**
     * The hidden developer clock override is pinning "now", so every time on this screen — and the
     * status-bar icon — is simulated. It survives process death and travels in the settings backup,
     * so it must be visible on the screen it falsifies, not only in the developer tools.
     */
    val developerTimeOverrideActive: Boolean = false,
)

/**
 * Date header pinned at the top of the tab: the Jewish date with the Gregorian date beneath. Like the
 * rest of the state it holds [UiText], worded by the screen in the app's current language — so a
 * language change needs no new state, and the view model never holds a sentence.
 */
@Immutable
data class ZmanimHeaderUi(
    val jewishDate: UiText,
    val gregorianDate: UiText,
    // The name of the location the times were computed for; the UI turns this into a small caption
    // when it isn't a fresh current-location fix (last-known, Jerusalem, or a named place).
    val locationName: String = "",
    val fastName: UiText? = null,
    // Fully written lines: "כניסת הצום 04:10", "יציאת הצום מחר 20:17".
    val fastStart: UiText? = null,
    val fastEnd: UiText? = null,
    val holyDayName: UiText? = null,
    // The day's own name ("ערב פסח", "פורים") and, ahead of Shabbat, the coming week's parasha.
    // Both are fallbacks for the chip, behind whatever is actually under way.
    val dayName: UiText? = null,
    val parshaName: UiText? = null,
    val holyDayStart: UiText? = null,
    val holyDayEnd: UiText? = null,
    // Set when another holy day begins the moment this one ends.
    val holyDaySequel: UiText? = null,
    val fastLeadsHeader: Boolean = false,
)

@Immutable
data class ZmanimGroupUi(
    val key: String,
    // Null for the untitled group of the day's events.
    val title: UiText?,
    val rows: List<ZmanimRowUi>,
    // The one line shown in place of rows when the user has hidden every zman.
    val emptyNote: UiText? = null,
)

@Immutable
data class ZmanimRowUi(
    val key: String,
    val title: UiText,
    val description: UiText,
    val value: UiText,
    // A Rambam Yomi row, whose value the screen trims to fit (see [rambamRowText]).
    val isRambam: Boolean = false,
)

/**
 * Drops the Zmanim rows and Daily-Learning rows the user has hidden, then removes any group
 * left empty. Rows without an id (always-on) and other groups are untouched. The Zmanim group is the
 * exception: with every one of its rows hidden it stays, empty, so the screen can say so rather
 * than lose the section without a word.
 */
internal fun ZmanimDay.filterForDisplay(
    enabledZmanimTimes: Set<ZmanimTimeOption>,
    enabledDailyLearning: Set<DailyLearningType>,
): ZmanimDay {
    val zmanimIds = enabledZmanimTimes.mapTo(mutableSetOf()) { it.storageValue }
    val learningIds = enabledDailyLearning.mapTo(mutableSetOf()) { it.storageValue }
    val filtered = groups.mapNotNull { group ->
        val items = when (group.kind) {
            ZmanimGroupKind.Zmanim, ZmanimGroupKind.Shabbat -> group.items.filter { it.id == null || it.id in zmanimIds }
            ZmanimGroupKind.DailyLearning -> group.items.filter { it.id == null || it.id in learningIds }
            ZmanimGroupKind.Events -> group.items
        }
        if (items.isEmpty() && group.kind != ZmanimGroupKind.Zmanim) null else group.copy(items = items)
    }
    return copy(groups = filtered)
}

/**
 * When both Rambam Yomi tracks are shown, collapse them into one "Rambam Yomi" entry whose value
 * lists the 1-chapter and 3-chapter references on separate lines, so they read as one section
 * rather than two look-alike rows.
 */
internal fun ZmanimDay.mergeRambamRows(): ZmanimDay {
    val merged = groups.map { group ->
        if (group.kind != ZmanimGroupKind.DailyLearning) return@map group
        val one = group.items.firstOrNull { it.id == DailyLearningType.RambamYomi.storageValue }
        val three = group.items.firstOrNull { it.id == DailyLearningType.RambamYomiThreeChapters.storageValue }
        if (one == null || three == null) return@map group
        val mergedRow = one.copy(
            description = uiText(R.string.learning_rambam_merged_description),
            value = uiText(
                R.string.learning_rambam_merged,
                pluralText(R.plurals.learning_rambam_chapters, 1),
                one.value ?: verbatim(""),
                pluralText(R.plurals.learning_rambam_chapters, 3),
                three.value ?: verbatim(""),
            ),
        )
        val items = group.items.mapNotNull { item ->
            when (item.id) {
                one.id -> mergedRow
                three.id -> null
                else -> item
            }
        }
        group.copy(items = items)
    }
    return copy(groups = merged)
}

internal fun ZmanimDay.toUiState(
    use24HourTime: Boolean,
    showCandleLightingPrompt: Boolean,
    developerTimeOverrideActive: Boolean,
    dayOffset: Int,
): ZmanimUiState {
    fun time(instant: Instant): UiText = UiText.Time(instant, zoneId, use24HourTime)

    val uiGroups = groups.mapIndexed { groupIndex, group ->
        ZmanimGroupUi(
            key = "group:$groupIndex:${group.kind}",
            title = group.kind.title?.let { uiText(it) },
            rows = group.items.mapIndexed { itemIndex, item ->
                ZmanimRowUi(
                    key = "row:$groupIndex:$itemIndex:${item.id ?: item.kind ?: ""}",
                    title = item.title,
                    description = item.description,
                    value = item.value ?: item.time?.let(::time) ?: verbatim("--"),
                    isRambam = item.id.isRambamId(),
                )
            },
            emptyNote = uiText(R.string.zmanim_none_selected).takeIf { group.kind == ZmanimGroupKind.Zmanim && group.items.isEmpty() },
        )
    }

    return ZmanimUiState(
        header = ZmanimHeaderUi(
            jewishDate = hebrewDate,
            // The weekday and the day-of-month come from different days: the weekday belongs to the
            // Jewish day and rolls at tzeit — Thursday evening is already "Friday", along with the
            // Hebrew date above it — while the day-of-month is the calendar date the times below
            // belong to, which turns over at midnight.
            gregorianDate = uiText(
                R.string.header_date,
                UiText.Date(displayedDate, DateStyle.Weekday),
                UiText.Date(date, DateStyle.DayMonth),
            ),
            locationName = locationName,
            // The fast's name belongs to it only while it is on; the times show a day ahead.
            fastName = fastDayInfo?.takeIf { it.isUnderWay }?.nameText,
            fastStart = fastDayInfo?.startTime?.let { observanceLine(R.string.fast_starts, time(it)) },
            fastEnd = fastDayInfo?.endTime?.let { observanceLine(R.string.fast_ends, time(it)) },
            // The name belongs to the holy day only while it is in; the times show a day ahead.
            holyDayName = holyDayInfo?.takeIf { it.isUnderWay }?.nameText,
            dayName = dayName,
            // While Shabbat is only announced — from the moment the entry/exit card appears — the
            // parasha gives the card a heading without claiming Shabbat has begun.
            parshaName = holyDayInfo?.takeUnless { it.isUnderWay }?.parshaText,
            // The two ends of the span are named separately: a Yom Tov entering on Friday goes out
            // on Shabbat, so "כניסת החג" is paired with "צאת שבת".
            holyDayStart = holyDayInfo?.startTime?.let { observanceLine(holyDayInfo.entryTerm.starts, time(it)) },
            holyDayEnd = holyDayInfo?.endTime?.let { observanceLine(holyDayInfo.exitTerm.ends, time(it)) },
            holyDaySequel = holyDayInfo?.sequelText,
            fastLeadsHeader = fastLeadsHeader,
        ),
        groups = uiGroups,
        showCandleLightingPrompt = showCandleLightingPrompt,
        developerTimeOverrideActive = developerTimeOverrideActive,
        dayOffset = dayOffset,
    )
}

private fun String?.isRambamId(): Boolean =
    this == DailyLearningType.RambamYomi.storageValue || this == DailyLearningType.RambamYomiThreeChapters.storageValue

/** One line of an observance card: what the time is, then the time — "צאת חג שני 19:20". */
private fun observanceLine(label: Int, time: UiText): UiText = uiText(R.string.observance_line, uiText(label), time)
