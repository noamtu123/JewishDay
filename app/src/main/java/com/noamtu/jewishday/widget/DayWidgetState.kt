// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import com.noamtu.jewishday.model.Festival

/**
 * One column of the day widget's times row: what the moment is and its clock time. A [pinned] time —
 * a Shabbat's, chag's or fast's entry or exit — is kept however few columns fit.
 */
data class DayWidgetTime(val label: String, val time: String, val pinned: Boolean = false)

/**
 * What the day widget shows, taken from the same [WidgetContent] as every other widget, so it can
 * never word the day differently from them or from the app.
 */
data class DayWidgetState(
    val useHebrew: Boolean,
    val hebrewDate: String,
    val gregorianDate: String,
    val festival: Festival?,
    val chip: String?,
    val times: List<DayWidgetTime>,
    val learning: String?,
    val locationNote: String?,
)

internal fun dayWidgetState(content: WidgetContent) = DayWidgetState(
    useHebrew = content.rtl,
    hebrewDate = content.hebrewDate,
    gregorianDate = content.gregorianDate,
    festival = content.festival,
    // What the day is; else, from the evening before, the Shabbat or chag the header has announced.
    chip = content.specialDay ?: content.shabbat?.takeIf { it.announced && !it.isUnderWay }?.title,
    times = content.upcoming.map { DayWidgetTime(it.shortTitle, it.time, pinned = it.observance) },
    learning = content.learning,
    locationNote = content.locationNote,
)
