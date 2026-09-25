// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import androidx.compose.ui.unit.DpSize

/** The pieces of the widget, in the order they stack from the top. */
internal enum class DayWidgetSlot { HebrewDate, WeekdayAndDate, Chip, Times, Observance, Event, Learning, Location }

/** What one widget frame shows: which slots, how large the date, which times and how many entry/exit lines. */
internal data class DayWidgetLayout(
    val slots: List<DayWidgetSlot>,
    val hebrewDateSp: Int,
    /** The times the row shows: the soonest its width has columns for, observance boundaries always kept. */
    val times: List<DayWidgetTime>,
    val observanceLinesShown: Int,
) {
    val timesShown: Int get() = times.size
}

/**
 * Fits the day into a frame of [size]. The text is set large enough to read at arm's length, so not
 * everything fits everywhere; the pieces are taken in order of importance while the height allows —
 * the Hebrew date always, then the times still to come (the reason to look), then the weekday, the
 * badge, the entry and exit lines, the events, the learning and the location caption — and stacked
 * in reading order. A piece the frame has no room for is left out whole rather than clipped. The
 * times row spreads across the width: two on a narrow widget, up to four on a full-width one.
 *
 * Heights are estimated from the text sizes, since a RemoteViews layout cannot be measured before
 * it is shown; the estimate errs a little generous, so a tight frame loses a line rather than cuts one.
 */
internal fun dayWidgetLayoutFor(state: DayWidgetState, size: DpSize): DayWidgetLayout {
    val narrow = size.width.value < NarrowWidthDp
    val hebrewDateSp = if (narrow) NarrowHebrewDateSp else HebrewDateSp
    val columns = when {
        narrow -> 2
        size.width.value < WideWidthDp -> 3
        else -> MaxTimesShown
    }
    val times = state.times.keepingPinned(columns)
    // A label longer than its column wraps to a second line, and the whole row grows with it.
    val columnDp = (size.width.value - 2 * HorizontalPaddingDp) / times.size.coerceAtLeast(1)
    val labelCharsPerLine = (columnDp / (TimeLabelSp * AverageCharWidthEm)).toInt().coerceAtLeast(1)
    val longLabels = times.any { it.label.length > labelCharsPerLine }
    val timesHeight = lineDp(TimeLabelSp) * (if (longLabels) 2 else 1) + lineDp(TimeSp)

    var budget = size.height.value - 2 * VerticalPaddingDp
    val chosen = mutableSetOf<DayWidgetSlot>()
    fun take(slot: DayWidgetSlot, height: Float, wanted: Boolean = true, force: Boolean = false) {
        if (!wanted || (!force && height > budget)) return
        chosen += slot
        budget -= height
    }
    take(DayWidgetSlot.HebrewDate, lineDp(hebrewDateSp), force = true)
    take(DayWidgetSlot.Times, timesHeight, wanted = times.isNotEmpty())
    take(DayWidgetSlot.WeekdayAndDate, lineDp(DateSp))
    take(DayWidgetSlot.Chip, lineDp(ChipSp), wanted = state.chip != null)
    var observanceLines = 0
    while (observanceLines < state.observanceLines.size && lineDp(LineSp) <= budget) {
        observanceLines++
        budget -= lineDp(LineSp)
    }
    if (observanceLines > 0) chosen += DayWidgetSlot.Observance
    take(DayWidgetSlot.Event, lineDp(LineSp), wanted = state.eventLine != null)
    take(DayWidgetSlot.Learning, lineDp(LineSp), wanted = state.learning != null)
    take(DayWidgetSlot.Location, lineDp(LocationSp), wanted = state.locationName != null)
    return DayWidgetLayout(
        slots = DayWidgetSlot.entries.filter { it in chosen },
        hebrewDateSp = hebrewDateSp,
        times = if (DayWidgetSlot.Times in chosen) times else emptyList(),
        observanceLinesShown = observanceLines,
    )
}

/** The height a line of [sp] text takes, roughly: the font's line height plus the view's own padding. */
internal fun lineDp(sp: Int): Float = sp * LineHeightFactor + LinePaddingDp

internal const val HebrewDateSp = 22
internal const val NarrowHebrewDateSp = 18
internal const val DateSp = 14
internal const val ChipSp = 15
internal const val LineSp = 13
internal const val TimeLabelSp = 12
internal const val TimeSp = 18
internal const val LocationSp = 11
internal const val HorizontalPaddingDp = 12
internal const val VerticalPaddingDp = 6

private const val MaxTimesShown = 4
private const val NarrowWidthDp = 180f
private const val WideWidthDp = 300f
// The average glyph width of the launcher's sans-serif, as a fraction of its size.
private const val AverageCharWidthEm = 0.55f
private const val LineHeightFactor = 1.35f
private const val LinePaddingDp = 2f
