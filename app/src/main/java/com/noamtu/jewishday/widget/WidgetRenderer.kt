// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
        WidgetKind.HebrewDate -> hebrewDateViews(context, content)
        WidgetKind.NextZman -> nextZmanViews(context, content)
        WidgetKind.Zmanim -> zmanimViews(context, content, options)
        WidgetKind.Shabbat -> shabbatViews(context, content)
        WidgetKind.GlassDate -> glassDateViews(context, content, skies.now, options)
        WidgetKind.GlassNextZman -> glassNextZmanViews(context, content, skies.now, options)
        WidgetKind.GlassZmanim -> glassZmanimViews(context, content, skies.now, options)
        WidgetKind.GlassShabbat -> glassShabbatViews(context, content, skies.shabbat, options)
        WidgetKind.GlassDay -> glassDayViews(context, content, skies.now, options)
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

private fun hebrewDateViews(context: Context, content: WidgetContent) =
    RemoteViews(context.packageName, R.layout.widget_hebrew_date).apply {
        setTextViewText(R.id.widget_hebrew_date, content.hebrewDate)
        setOptionalText(R.id.widget_special_day, content.specialDay)
        setTextViewText(R.id.widget_gregorian_date, content.gregorianDate)
    }

private fun nextZmanViews(context: Context, content: WidgetContent) =
    RemoteViews(context.packageName, R.layout.widget_next_zman).apply {
        setTextViewText(R.id.widget_next_label, content.nextLabel)
        setTextViewText(R.id.widget_next_title, content.next?.title ?: "—")
        setTextViewText(R.id.widget_next_time, content.next?.time.orEmpty())
        setOptionalText(
            R.id.widget_following,
            content.following?.let { "${content.followingLabel}: ${it.title} ${it.time}" },
        )
    }

private fun zmanimViews(context: Context, content: WidgetContent, options: Bundle) =
    RemoteViews(context.packageName, R.layout.widget_zmanim).apply {
        setTextViewText(R.id.widget_hebrew_date, content.hebrewDate)
        setOptionalText(R.id.widget_special_day, content.specialDay)
        removeAllViews(R.id.widget_rows)
        visibleRows(content, rowsThatFit(options, ZmanimChromeDp)).forEach { (index, zman) ->
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
    }

private fun shabbatViews(context: Context, content: WidgetContent) =
    RemoteViews(context.packageName, R.layout.widget_shabbat).apply {
        val shabbat = content.shabbat
        setTextViewText(R.id.widget_shabbat_title, shabbat?.title.orEmpty())
        setOptionalText(R.id.widget_shabbat_subtitle, shabbat?.subtitle)
        setTextViewText(R.id.widget_entry_label, shabbat?.entryLabel.orEmpty())
        setTextViewText(R.id.widget_entry_time, shabbat?.entryTime ?: "—")
        setTextViewText(R.id.widget_exit_label, shabbat?.exitLabel.orEmpty())
        setTextViewText(R.id.widget_exit_time, shabbat?.exitTime ?: "—")
    }

/**
 * How many zman rows the widget's current height holds. Portrait is the height a phone is mostly
 * held at, and it is the larger of the two the launcher reports.
 */
internal fun rowsThatFit(options: Bundle, chromeDp: Int): Int =
    ((widgetSizeDp(options, DefaultZmanimWidthDp, DefaultZmanimHeightDp).second - chromeDp) / ZmanimRowDp)
        .coerceAtLeast(1)

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

// The zmanim widget's padding and date header, and one row, in dp — matching widget_zmanim.xml.
private const val ZmanimChromeDp = 72
internal const val ZmanimRowDp = 24
private const val DefaultZmanimWidthDp = 180
private const val DefaultZmanimHeightDp = 180
