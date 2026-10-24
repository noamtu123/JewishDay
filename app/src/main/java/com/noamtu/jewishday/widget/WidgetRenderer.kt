// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.noamtu.jewishday.MainActivity
import com.noamtu.jewishday.R

internal fun renderWidget(
    context: Context,
    kind: WidgetKind,
    content: WidgetContent,
    skies: WidgetSkies,
    options: Bundle,
): RemoteViews {
    val views = when (kind) {
        WidgetKind.HebrewDate -> hebrewDateViews(context, content, options)
        WidgetKind.NextZman -> nextZmanViews(context, content)
        WidgetKind.Zmanim -> zmanimViews(context, content, options)
        WidgetKind.Shabbat -> shabbatViews(context, content, options)
        WidgetKind.GlassDate -> glassDateViews(context, content, skies.now, options)
        WidgetKind.GlassNextZman -> glassNextZmanViews(context, content, skies.now, options)
        WidgetKind.GlassZmanim -> glassZmanimViews(context, content, skies.now, options)
        WidgetKind.GlassShabbat -> glassShabbatViews(context, content, skies.shabbat, options)
    }
    // The app's language, not the phone's, decides which way the widget reads.
    views.setInt(
        android.R.id.background,
        "setLayoutDirection",
        if (content.rtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR,
    )
    views.setOnClickPendingIntent(android.R.id.background, openAppIntent(context))
    return views
}

/**
 * One row high and wide (3x1 and up) the two dates sit side by side, the Hebrew one large at the
 * start and the civil one at the end; otherwise they stack. Either way the Hebrew date is set as
 * large as the widget allows.
 */
private fun hebrewDateViews(context: Context, content: WidgetContent, options: Bundle): RemoteViews {
    val (widthDp, heightDp) = widgetSizeDp(options, DefaultHebrewDateWidthDp, DefaultHebrewDateHeightDp)
    if (widgetShape(heightDp) == WidgetShape.Strip && widthDp >= WideStripFromDp) {
        return RemoteViews(context.packageName, R.layout.widget_hebrew_date_wide).apply {
            setTextViewText(R.id.widget_hebrew_date, content.hebrewDate)
            // The civil date's column takes about a third; the divider and padding the rest.
            val titleWidth = widthDp * 0.62f - 40
            setTextSizeDp(R.id.widget_hebrew_date, fittedTextSize(content.hebrewDate, titleWidth, 18f, minOf(30f, heightDp * 0.34f)))
            setOptionalText(R.id.widget_special_day, content.specialDay)
            setTextViewText(R.id.widget_weekday, content.weekday)
            setTextViewText(R.id.widget_civil_date, content.civilDate)
        }
    }
    return RemoteViews(context.packageName, R.layout.widget_hebrew_date).apply {
        setTextViewText(R.id.widget_hebrew_date, content.hebrewDate)
        val largest = if (widgetShape(heightDp) == WidgetShape.Strip) minOf(26f, heightDp * 0.26f) else minOf(40f, heightDp * 0.2f)
        setTextSizeDp(R.id.widget_hebrew_date, fittedTextSize(content.hebrewDate, widthDp - 24f, 16f, largest))
        // Under a row's height there is room for two lines, so the civil date stays and the day's name goes.
        setOptionalText(R.id.widget_special_day, content.specialDay?.takeIf { heightDp >= ThreeLinesFromDp })
        setTextViewText(R.id.widget_gregorian_date, content.gregorianDate)
    }
}

private fun nextZmanViews(context: Context, content: WidgetContent) =
    RemoteViews(context.packageName, R.layout.widget_next_zman).apply {
        setTextViewText(R.id.widget_next_label, content.nextLabel)
        setTextViewText(R.id.widget_next_title, content.next?.title ?: "—")
        setTextViewText(R.id.widget_next_time, content.next?.time.orEmpty())
        val following = content.following
        setViewVisibility(R.id.widget_following, if (following == null) View.GONE else View.VISIBLE)
        if (following != null) {
            setTextViewText(R.id.widget_following_title, "${content.followingLabel}: ${following.title}")
            setTextViewText(R.id.widget_following_time, following.time)
        }
    }

private fun zmanimViews(context: Context, content: WidgetContent, options: Bundle) =
    RemoteViews(context.packageName, R.layout.widget_zmanim).apply {
        setTextViewText(R.id.widget_hebrew_date, content.hebrewDate)
        setOptionalText(R.id.widget_special_day, content.specialDay)
        removeAllViews(R.id.widget_rows)
        val capacity = rowsThatFit(options, ZmanimChromeDp)
        val rows = visibleRows(content, capacity)
        rows.forEach { (index, zman) ->
            val layout = when {
                index == content.nextIndex -> R.layout.widget_zman_row_next
                zman.isPast -> R.layout.widget_zman_row_past
                else -> R.layout.widget_zman_row
            }
            val row = RemoteViews(context.packageName, layout).apply {
                setTextViewText(R.id.widget_row_title, zman.title)
                setTextViewText(R.id.widget_row_time, zman.time)
            }
            addView(R.id.widget_rows, row)
        }
        addRowSpacers(context, R.id.widget_rows, rowSlots(options, ZmanimChromeDp) - rows.size)
    }

/**
 * One row high (its 3x1 default) the parsha or chag sits at the start and its two times side by side
 * at the end, the name giving way first on a narrow widget; from two rows they stack.
 */
private fun shabbatViews(context: Context, content: WidgetContent, options: Bundle): RemoteViews {
    val (widthDp, heightDp) = widgetSizeDp(options, DefaultShabbatWidthDp, DefaultShabbatHeightDp)
    val strip = widgetShape(heightDp) == WidgetShape.Strip
    return RemoteViews(context.packageName, if (strip) R.layout.widget_shabbat_strip else R.layout.widget_shabbat).apply {
        val shabbat = content.shabbat
        setTextViewText(R.id.widget_shabbat_title, shabbat?.title.orEmpty())
        setOptionalText(R.id.widget_shabbat_subtitle, shabbat?.subtitle)
        setTextViewText(R.id.widget_entry_label, shabbat?.entryHeading.orEmpty())
        setTextViewText(R.id.widget_entry_time, shabbat?.entryTime ?: "—")
        setTextViewText(R.id.widget_exit_label, shabbat?.exitHeading.orEmpty())
        setTextViewText(R.id.widget_exit_time, shabbat?.exitTime ?: "—")
        if (strip) {
            setViewVisibility(R.id.widget_shabbat_heading, if (widthDp >= WideStripFromDp) View.VISIBLE else View.GONE)
        }
    }
}

/**
 * How many zman rows the widget's current height holds. Portrait is the height a phone is mostly
 * held at, and it is the larger of the two the launcher reports.
 */
internal fun rowsThatFit(options: Bundle, chromeDp: Int): Int =
    ((widgetSizeDp(options, DefaultZmanimWidthDp, DefaultZmanimHeightDp).second - chromeDp) / ZmanimRowDp)
        .coerceAtLeast(1)

/**
 * How many equal shares the list's height is cut into: as many rows as fit at a comfortable
 * [RoomyRowDp]. A list with at least that many rows simply shares the height out; a shorter one is
 * padded with empty shares to that count, so three rows stay three rows rather than a third each.
 */
internal fun rowSlots(options: Bundle, chromeDp: Int): Int =
    (widgetSizeDp(options, DefaultZmanimWidthDp, DefaultZmanimHeightDp).second - chromeDp) / RoomyRowDp

/**
 * The widget's size in dp as it stands in portrait — the minimum width and maximum height the
 * launcher reports — or the given defaults before it has reported any.
 */
internal fun widgetSizeDp(options: Bundle, defaultWidthDp: Int, defaultHeightDp: Int): Pair<Int, Int> {
    val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).takeIf { it > 0 } ?: defaultWidthDp
    val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).takeIf { it > 0 }
        ?: options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).takeIf { it > 0 }
        ?: defaultHeightDp
    return width to height
}

/**
 * The rows to show when they don't all fit: the window keeps the next zman in view with the one just
 * past above it, so the widget always answers "what's next" rather than showing the morning at night.
 */
internal fun visibleRows(content: WidgetContent, capacity: Int): List<IndexedValue<WidgetZman>> {
    val all = content.zmanim.withIndex().toList()
    if (all.size <= capacity) return all
    val anchor = (content.nextIndex ?: all.size) - 1
    val start = anchor.coerceIn(0, all.size - capacity)
    return all.subList(start, start + capacity)
}

/** How a widget lays itself out: one row high, a usual two, or taller. */
internal enum class WidgetShape { Strip, Regular, Tall }

internal fun widgetShape(heightDp: Int): WidgetShape = when {
    heightDp < StripBelowDp -> WidgetShape.Strip
    heightDp < TallFromDp -> WidgetShape.Regular
    else -> WidgetShape.Tall
}

/**
 * The largest text size, in dp, at which [text] still fits on one line [widthDp] wide, within
 * [minDp]..[maxDp]. An estimate from the length — a character is about 0.6 of the size wide in the
 * bold system font — which is all a widget can do, since it cannot measure before it is drawn.
 * Sizes are dp rather than sp so that a large system font cannot push a fitted line off the widget.
 */
internal fun fittedTextSize(text: String, widthDp: Float, minDp: Float, maxDp: Float): Float =
    (widthDp / (text.length.coerceAtLeast(1) * CharWidthEm)).coerceIn(minDp, maxOf(minDp, maxDp))

internal fun RemoteViews.setTextSizeDp(viewId: Int, sizeDp: Float) {
    setTextViewTextSize(viewId, TypedValue.COMPLEX_UNIT_DIP, sizeDp)
}

/**
 * The zmanim lists share their height out in equal rows (see [rowSlots]), so the list fills the
 * widget at whatever size the launcher draws it — the picker's preview included.
 */
internal fun RemoteViews.addRowSpacers(context: Context, viewId: Int, count: Int) {
    repeat(count.coerceAtLeast(0)) { addView(viewId, RemoteViews(context.packageName, R.layout.widget_row_spacer)) }
}

internal fun RemoteViews.setOptionalText(viewId: Int, text: String?) {
    if (text.isNullOrBlank()) {
        setViewVisibility(viewId, View.GONE)
    } else {
        setViewVisibility(viewId, View.VISIBLE)
        setTextViewText(viewId, text)
    }
}

private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
    context,
    0,
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    },
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)

// One cell's height is 90-140dp in portrait depending on the phone, two cells 180-280dp.
private const val StripBelowDp = 150
private const val TallFromDp = 280
private const val WideStripFromDp = 200
private const val ThreeLinesFromDp = 84
internal const val CharWidthEm = 0.6f
private const val DefaultHebrewDateWidthDp = 160
private const val DefaultHebrewDateHeightDp = 100
private const val DefaultShabbatWidthDp = 225
private const val DefaultShabbatHeightDp = 100

// The zmanim widget's padding and date header, in dp — matching widget_zmanim.xml — and the least
// height a row needs; rows share out whatever height is left, so they are at least this.
private const val ZmanimChromeDp = 72
internal const val ZmanimRowDp = 18
private const val RoomyRowDp = 28
private const val DefaultZmanimWidthDp = 225
private const val DefaultZmanimHeightDp = 330
