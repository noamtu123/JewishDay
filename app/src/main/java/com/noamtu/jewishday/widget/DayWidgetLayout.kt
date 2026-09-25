// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import androidx.compose.ui.unit.DpSize
import kotlin.math.ceil

/** The pieces of the widget, in the order they stack from the top. */
internal enum class DayWidgetSlot { HebrewDate, Weekday, Chip, Times, Observance, Event, Learning, Location }

/**
 * What one widget frame shows: which slots, how the Hebrew date is set (beside the festival's
 * picture, on a festival), which times and how many entry/exit lines.
 */
internal data class DayWidgetLayout(
    val slots: List<DayWidgetSlot>,
    val hebrewDateSp: Int,
    /** One line, or two on a frame too narrow to hold the date on one at a readable size. */
    val hebrewDateLines: Int,
    /** How large the festival's picture beside the date is, or 0 for none. */
    val festivalIconDp: Int,
    /** The times the row shows: the soonest its width has columns for, observance boundaries always kept. */
    val times: List<DayWidgetTime>,
    /** The times' labels' size: as large as sets them all on one line, else every word of them whole. */
    val timeLabelSp: Int,
    val observanceLinesShown: Int,
) {
    val timesShown: Int get() = times.size
}

/**
 * Fits the day into a frame of [size], its text scaled by the system's [fontScale]. The text is set
 * large enough to read at arm's length, so not everything fits everywhere; the pieces are taken in
 * order of importance while the height allows — the Hebrew date always, then the times still to
 * come (the reason to look), then the weekday, the badge, the entry and exit lines, the events, the
 * learning and the location caption — and stacked in reading order. A piece the frame has no room
 * for is left out whole rather than clipped, and a line too long for the width wraps to a second.
 *
 * The times row has as many columns as the width holds at the times' size, up to four. The Hebrew
 * date shrinks to fit the width, and on a frame too narrow for it at a readable size it breaks onto
 * two lines instead. On a festival its picture sits beside the date, sized to the frame, as long as
 * the date keeps most of its size beside it; a frame too narrow for both keeps just the date.
 *
 * Sizes are estimated from the text, since a RemoteViews layout cannot be measured before it is
 * shown; the estimate errs a little generous, so a tight frame loses a line rather than cuts one.
 */
internal fun dayWidgetLayoutFor(state: DayWidgetState, size: DpSize, fontScale: Float = 1f): DayWidgetLayout {
    val text = TextMetrics(
        lineWidthDp = (size.width.value - 2 * HorizontalPaddingDp).coerceAtLeast(1f),
        charWidthEm = if (state.useHebrew) HebrewCharWidthEm else LatinCharWidthEm,
        boldCharWidthEm = if (state.useHebrew) HebrewBoldCharWidthEm else LatinCharWidthEm,
        fontScale = fontScale,
    )
    var budget = size.height.value - 2 * VerticalPaddingDp
    val chosen = mutableSetOf(DayWidgetSlot.HebrewDate)
    fun take(slot: DayWidgetSlot, height: Float): Boolean {
        if (height > budget) return false
        chosen += slot
        budget -= height
        return true
    }

    val plainDate = hebrewDateSettingFor(state.hebrewDate, text, text.lineWidthDp, budget)
    val iconDp = festivalIconDpFor(text.lineWidthDp).takeIf { state.festival != null && it <= budget }
    val besideIcon = iconDp?.let { hebrewDateSettingFor(state.hebrewDate, text, text.lineWidthDp - it - FestivalIconGapDp, budget) }
    val showIcon = besideIcon != null && besideIcon.fits && besideIcon.sp >= plainDate.sp * MinDateShareBesideIcon
    val date = if (showIcon) requireNotNull(besideIcon) else plainDate
    val festivalIconDp = if (showIcon) requireNotNull(iconDp) else 0
    budget -= maxOf(date.lines * text.lineDp(date.sp), festivalIconDp.toFloat())
    val row = timesRowFor(state.times, text)
    if (row.times.isNotEmpty()) take(DayWidgetSlot.Times, row.heightDp)
    take(DayWidgetSlot.Weekday, text.blockDp(state.weekday, WeekdaySp))
    state.chip?.let { take(DayWidgetSlot.Chip, text.blockDp(it, ChipSp)) }
    val observanceLines = state.observanceLines.takeWhile { take(DayWidgetSlot.Observance, text.blockDp(it, LineSp)) }
    state.eventLine?.let { take(DayWidgetSlot.Event, text.blockDp(it, LineSp)) }
    state.learning?.let { take(DayWidgetSlot.Learning, text.blockDp(it, LineSp)) }
    state.locationName?.let { take(DayWidgetSlot.Location, text.blockDp(it, LocationSp)) }
    return DayWidgetLayout(
        slots = DayWidgetSlot.entries.filter { it in chosen },
        hebrewDateSp = date.sp,
        hebrewDateLines = date.lines,
        festivalIconDp = festivalIconDp,
        times = if (DayWidgetSlot.Times in chosen) row.times else emptyList(),
        timeLabelSp = row.labelSp,
        observanceLinesShown = observanceLines.size,
    )
}

/**
 * How many lines [text] takes when wrapped at word boundaries into lines [lineWidthDp] wide, each
 * character [charDp] wide — the way a TextView breaks it. A word wider than a whole line is broken
 * across as many as it needs.
 */
internal fun wrappedLines(text: String, charDp: Float, lineWidthDp: Float): Int {
    var lines = 1
    var used = 0f
    text.split(' ').filter(String::isNotEmpty).forEach { word ->
        val wordDp = word.length * charDp
        if (used > 0f && used + charDp + wordDp <= lineWidthDp) {
            used += charDp + wordDp
            return@forEach
        }
        if (used > 0f) lines++
        val spill = ceil(wordDp / lineWidthDp).toInt().coerceAtLeast(1)
        lines += spill - 1
        used = wordDp - (spill - 1) * lineWidthDp
    }
    return lines
}

/** The text's size, how many lines it is set on, and whether it fits there at all rather than at the least size. */
private data class TextSetting(val sp: Int, val lines: Int, val fits: Boolean = true)

/** The festival's picture, a fifth of the line across and no smaller than a glance can make out. */
private fun festivalIconDpFor(lineWidthDp: Float): Int =
    (lineWidthDp * FestivalIconShare).toInt().coerceIn(MinFestivalIconDp, FestivalIconDp)

/**
 * The Hebrew date at the largest size its line fits [widthDp] and [heightDp] at. Below a readable
 * size, two lines win if they let it be set markedly larger and the height has room for both.
 */
private fun hebrewDateSettingFor(date: String, text: TextMetrics, widthDp: Float, heightDp: Float): TextSetting {
    fun largest(fits: (Int) -> Boolean): Int? = (HebrewDateSp downTo MinHebrewDateSp).firstOrNull(fits)
    val oneLine = largest { text.boldLines(date, it, widthDp) == 1 && text.lineDp(it) <= heightDp }
    if (oneLine != null && oneLine >= ReadableHebrewDateSp) return TextSetting(oneLine, 1)
    val twoLines = largest {
        text.boldFitsWhole(date, it, widthDp) && text.boldLines(date, it, widthDp) <= 2 && 2 * text.lineDp(it) <= heightDp
    }
    return when {
        twoLines != null && twoLines >= (oneLine ?: 0) + TwoLineGainSp -> TextSetting(twoLines, 2)
        else -> TextSetting(oneLine ?: MinHebrewDateSp, 1, fits = oneLine != null)
    }
}

/** The times row: which times it shows, the size of their labels and how tall it stands. */
private class TimesRow(val times: List<DayWidgetTime>, val labelSp: Int, val heightDp: Float)

/**
 * As many columns as the width holds at the times' own size, up to four, filled with the soonest
 * times but never without an observance boundary. The labels shrink a little to sit on one line, as
 * a row of labels at one height reads tidier than one broken over two. Where no size lets them, a
 * label wraps to a second line and the row grows with it, but a word is never broken: if even that
 * leaves a word wider than its column, the row gives up a column.
 */
private fun timesRowFor(times: List<DayWidgetTime>, text: TextMetrics): TimesRow {
    if (times.isEmpty()) return TimesRow(emptyList(), TimeLabelSp, 0f)
    val columnDp = times.maxOf { text.textDp(it.time, TimeSp, DigitWidthEm) } + 2 * TimeColumnPaddingDp
    val mostColumns = (text.lineWidthDp / columnDp).toInt().coerceIn(1, MaxTimesShown)
    fun labelWidthDp(columns: Int): Float = text.lineWidthDp / columns - 2 * TimeColumnPaddingDp
    fun row(shown: List<DayWidgetTime>, labelSp: Int): TimesRow {
        val labelLines = shown.maxOf { text.lines(it.label, labelSp, labelWidthDp(shown.size)) }
            .coerceAtMost(MaxTextLines)
        return TimesRow(shown, labelSp, TimesGapDp + labelLines * text.lineDp(labelSp) + text.lineDp(TimeSp))
    }
    val sizes = TimeLabelSp downTo MinTimeLabelSp
    for (columns in mostColumns downTo 1) {
        val shown = times.keepingPinned(columns)
        val widthDp = labelWidthDp(shown.size)
        val labelSp = sizes.firstOrNull { sp -> shown.all { text.fitsOneLine(it.label, sp, widthDp) } }
            ?: sizes.firstOrNull { sp -> shown.all { text.fitsWhole(it.label, sp, widthDp) } }
        if (labelSp != null) return row(shown, labelSp)
    }
    return row(times.keepingPinned(1), MinTimeLabelSp)
}

/**
 * The widget's text geometry, estimated: the frame's line width, the script's average glyph width
 * (bold, for the Hebrew date, runs wider) and the system's font scale, which enlarges every sp alike.
 */
private class TextMetrics(
    val lineWidthDp: Float,
    private val charWidthEm: Float,
    private val boldCharWidthEm: Float,
    private val fontScale: Float,
) {
    /** The height a line of [sp] text takes: the font's line height plus the view's own padding. */
    fun lineDp(sp: Int): Float = sp * fontScale * LineHeightFactor + LinePaddingDp

    fun textDp(text: String, sp: Int, em: Float = charWidthEm): Float = text.length * sp * fontScale * em

    fun lines(text: String, sp: Int, widthDp: Float = lineWidthDp): Int =
        wrappedLines(text, sp * fontScale * charWidthEm, widthDp.coerceAtLeast(1f))

    fun boldLines(text: String, sp: Int, widthDp: Float = lineWidthDp): Int =
        wrappedLines(text, sp * fontScale * boldCharWidthEm, widthDp)

    /** Whether no word of bold [text] at [sp] is wider than [widthDp], with a character's slack as [fitsWhole] gives. */
    fun boldFitsWhole(text: String, sp: Int, widthDp: Float): Boolean =
        text.split(' ').all { textDp("$it ", sp, boldCharWidthEm) <= widthDp }

    /** Whether [text] at [sp] sets in [widthDp] on one line, with a character's slack as [fitsWhole] gives a word. */
    fun fitsOneLine(text: String, sp: Int, widthDp: Float): Boolean = textDp("$text ", sp) <= widthDp

    /**
     * Whether [text] at [sp] sets in [widthDp] on at most [MaxTextLines] with no word broken across
     * two. A word is given a character's slack, as a long word's letters can run wider than the average.
     */
    fun fitsWhole(text: String, sp: Int, widthDp: Float): Boolean =
        text.split(' ').all { textDp("$it ", sp) <= widthDp } && lines(text, sp, widthDp) <= MaxTextLines

    /**
     * The height [text] takes at [sp] across the frame; a text that would need more than
     * [MaxTextLines] has no height that fits, so it is left out rather than cut short.
     */
    fun blockDp(text: String, sp: Int): Float =
        lines(text, sp).let { if (it > MaxTextLines) Float.POSITIVE_INFINITY else it * lineDp(sp) }
}

internal const val HebrewDateSp = 38
internal const val MinHebrewDateSp = 14
internal const val WeekdaySp = 22
internal const val ChipSp = 24
internal const val LineSp = 21
internal const val TimeLabelSp = 19
internal const val MinTimeLabelSp = 15
internal const val TimeSp = 30
internal const val LocationSp = 17
internal const val MaxTextLines = 2
internal const val HorizontalPaddingDp = 12
internal const val VerticalPaddingDp = 6
internal const val TimeColumnPaddingDp = 4

// A little air between the lines above and the times row, so the row stands apart as the widget's table.
internal const val TimesGapDp = 6
internal const val FestivalIconDp = 40
internal const val FestivalIconGapDp = 6
private const val MinFestivalIconDp = 26
private const val FestivalIconShare = 0.2f

// The picture is a garnish: a frame that could only fit it by shrinking the date by more than this
// share of its size leaves it out.
private const val MinDateShareBesideIcon = 0.75f

private const val MaxTimesShown = 4

// Below this the date reads small enough that two larger lines serve better, if they are this much larger.
private const val ReadableHebrewDateSp = 26
private const val TwoLineGainSp = 5

// Average glyph widths of the launcher's sans-serif as a fraction of its size, measured off Roboto
// and Noto Sans Hebrew and rounded up to the widest of the widget's texts: bold Hebrew runs wide,
// and a clock time is mostly digits.
private const val HebrewCharWidthEm = 0.53f
private const val HebrewBoldCharWidthEm = 0.57f
private const val LatinCharWidthEm = 0.55f
private const val DigitWidthEm = 0.53f

// A line of text stands 1.34 times its size with the font's padding, and Hebrew's fallback font 1.36.
private const val LineHeightFactor = 1.36f
private const val LinePaddingDp = 1f
