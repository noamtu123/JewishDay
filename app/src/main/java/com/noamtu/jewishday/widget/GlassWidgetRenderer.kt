// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.noamtu.jewishday.R
import com.noamtu.jewishday.data.hasLocationPermission
import com.noamtu.jewishday.ui.components.iconRes
import com.noamtu.jewishday.ui.theme.GlassDayColors
import com.noamtu.jewishday.ui.theme.GlassNightColors
import com.noamtu.jewishday.ui.theme.Moonlight
import com.noamtu.jewishday.ui.theme.SkyFrame
import com.noamtu.jewishday.ui.theme.celestialLight

/**
 * The skies the glass widgets are painted with: the one outside now, and the Shabbat widget's — the
 * sky of its candle lighting until Shabbat or the chag is in, then the live one.
 */
internal data class WidgetSkies(val now: SkyFrame, val shabbat: SkyFrame)

/**
 * The Glass theme's two glasses, as the app wears them over the same sky: dark glass and light ink by
 * night and dusk, milky glass and dark ink by day; accents in the light of whichever of the sun or
 * moon is up (see GlassTheme.kt).
 */
private class GlassInk(sky: SkyFrame) {
    private val scheme = if (sky.dark) GlassNightColors else GlassDayColors
    private val light = celestialLight(sky)
    val primary = scheme.onBackground.toArgb()
    val secondary = scheme.onSurfaceVariant.toArgb()
    val past = scheme.onSurfaceVariant.copy(alpha = 0.5f).toArgb()
    // Big times glow in the sky's own light at night; by day the milky glass needs ink for them.
    val time = (if (sky.dark) light.seen else scheme.primary).toArgb()
    val pane = if (sky.dark) R.drawable.widget_glass_pane_dark else R.drawable.widget_glass_pane_light
    val accent = when {
        light == Moonlight -> R.drawable.widget_glass_accent_moon
        sky.dark -> R.drawable.widget_glass_accent_sun_dark
        else -> R.drawable.widget_glass_accent_sun_light
    }
    val onAccent = when {
        sky.dark -> light.core
        light == Moonlight -> Color(0xFF1A2150)
        else -> Color(0xFF4A2600)
    }.toArgb()
}

internal fun glassDateViews(context: Context, content: WidgetContent, sky: SkyFrame, options: Bundle) =
    glassViews(context, R.layout.widget_glass_date, sky, DateSky, options, content.rtl, 250, 110) { ink ->
        setText(R.id.glass_hebrew_date, content.hebrewDate, ink.primary)
        setText(R.id.glass_gregorian_date, content.gregorianDate, ink.secondary)
        setChip(R.id.glass_special_day, content.specialDay, ink)
        setPane(R.id.glass_pane, ink)
        setText(R.id.glass_next_label, content.nextLabel, ink.secondary)
        setText(R.id.glass_next_title, content.next?.title ?: "—", ink.primary)
        setText(R.id.glass_next_time, content.next?.time.orEmpty(), ink.time)
    }

internal fun glassNextZmanViews(context: Context, content: WidgetContent, sky: SkyFrame, options: Bundle) =
    glassViews(context, R.layout.widget_glass_next, sky, NextSky, options, content.rtl, 110, 110) { ink ->
        setPane(R.id.glass_pane, ink)
        setText(R.id.glass_next_label, content.nextLabel, ink.secondary)
        setText(R.id.glass_next_title, content.next?.title ?: "—", ink.primary)
        setText(R.id.glass_next_time, content.next?.time.orEmpty(), ink.time)
        setOptionalText(R.id.glass_following, content.following?.let { "${content.followingLabel}: ${it.title} ${it.time}" })
        setTextColor(R.id.glass_following, ink.secondary)
    }

internal fun glassZmanimViews(context: Context, content: WidgetContent, sky: SkyFrame, options: Bundle) =
    glassViews(context, R.layout.widget_glass_zmanim, sky, ZmanimSky, options, content.rtl, 250, 180) { ink ->
        setText(R.id.glass_hebrew_date, content.hebrewDate, ink.primary)
        setChip(R.id.glass_special_day, content.specialDay, ink)
        setPane(R.id.glass_pane, ink)
        removeAllViews(R.id.glass_rows)
        visibleRows(content, rowsThatFit(options, GlassZmanimChromeDp)).forEach { (index, zman) ->
            val isNext = index == content.nextIndex
            val color = when {
                isNext -> ink.onAccent
                zman.isPast -> ink.past
                else -> ink.primary
            }
            val row = RemoteViews(context.packageName, R.layout.widget_glass_row).apply {
                setText(R.id.glass_row_title, zman.title, color)
                setText(R.id.glass_row_time, zman.time, color)
                if (isNext) setInt(R.id.glass_row, "setBackgroundResource", ink.accent)
            }
            addView(R.id.glass_rows, row)
        }
    }

internal fun glassShabbatViews(context: Context, content: WidgetContent, sky: SkyFrame, options: Bundle) =
    glassViews(context, R.layout.widget_glass_shabbat, sky, ShabbatSky, options, content.rtl, 250, 110) { ink ->
        val shabbat = content.shabbat
        setText(R.id.glass_shabbat_title, shabbat?.title.orEmpty(), ink.primary)
        setChip(R.id.glass_shabbat_subtitle, shabbat?.subtitle, ink)
        setPane(R.id.glass_entry_pane, ink)
        setPane(R.id.glass_exit_pane, ink)
        setText(R.id.glass_entry_label, shabbat?.entryLabel.orEmpty(), ink.secondary)
        setText(R.id.glass_entry_time, shabbat?.entryTime ?: "—", ink.time)
        setText(R.id.glass_exit_label, shabbat?.exitLabel.orEmpty(), ink.secondary)
        setText(R.id.glass_exit_time, shabbat?.exitTime ?: "—", ink.time)
    }

/**
 * The whole day on the sky, fitted to whatever frame the launcher gives it: [dayWidgetLayoutFor]
 * decides what fits and at what size, and each piece it leaves out is hidden whole.
 */
internal fun glassDayViews(context: Context, content: WidgetContent, sky: SkyFrame, options: Bundle) =
    glassViews(context, R.layout.widget_glass_day, sky, DaySky, options, content.rtl, DefaultDayWidthDp, DefaultDayHeightDp) { ink ->
        val state = dayWidgetState(content)
        val (widthDp, heightDp) = widgetSizeDp(options, DefaultDayWidthDp, DefaultDayHeightDp)
        val layout = dayWidgetLayoutFor(state, DpSize(widthDp.dp, heightDp.dp), context.resources.configuration.fontScale)
        val shown = layout.slots.toSet()
        fun show(viewId: Int, slot: DayWidgetSlot) =
            setViewVisibility(viewId, if (slot in shown) View.VISIBLE else View.GONE)

        // A strip with a line or two floats them in the middle of its height; a fuller widget reads
        // from the top down.
        setInt(R.id.day_column, "setGravity", if (layout.slots.size <= 2) Gravity.CENTER_VERTICAL else Gravity.TOP)

        setText(R.id.day_hebrew_date, state.hebrewDate, ink.primary)
        setTextViewTextSize(R.id.day_hebrew_date, TypedValue.COMPLEX_UNIT_SP, layout.hebrewDateSp.toFloat())
        val festival = state.festival
        if (festival != null && layout.festivalIconDp > 0) {
            setViewVisibility(R.id.day_festival, View.VISIBLE)
            setImageViewResource(R.id.day_festival, festival.iconRes)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setViewLayoutWidth(R.id.day_festival, layout.festivalIconDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
                setViewLayoutHeight(R.id.day_festival, layout.festivalIconDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
            }
        } else {
            setViewVisibility(R.id.day_festival, View.GONE)
        }

        show(R.id.day_gregorian_date, DayWidgetSlot.GregorianDate)
        setText(R.id.day_gregorian_date, state.gregorianDate, ink.secondary)
        setTextViewTextSize(R.id.day_gregorian_date, TypedValue.COMPLEX_UNIT_SP, GregorianDateSp.toFloat())

        show(R.id.day_chip, DayWidgetSlot.Chip)
        setText(R.id.day_chip, state.chip.orEmpty(), ink.time)
        setTextViewTextSize(R.id.day_chip, TypedValue.COMPLEX_UNIT_SP, ChipSp.toFloat())

        show(R.id.day_times, DayWidgetSlot.Times)
        setPane(R.id.day_times, ink)
        removeAllViews(R.id.day_times)
        layout.times.forEach { time ->
            val column = RemoteViews(context.packageName, R.layout.widget_glass_day_time).apply {
                setText(R.id.day_time_label, time.label, ink.secondary)
                setTextViewTextSize(R.id.day_time_label, TypedValue.COMPLEX_UNIT_SP, layout.timeLabelSp.toFloat())
                setText(R.id.day_time_value, time.time, ink.time)
                setTextViewTextSize(R.id.day_time_value, TypedValue.COMPLEX_UNIT_SP, TimeSp.toFloat())
            }
            addView(R.id.day_times, column)
        }

        show(R.id.day_learning, DayWidgetSlot.Learning)
        setText(R.id.day_learning, state.learning.orEmpty(), ink.primary)
        setTextViewTextSize(R.id.day_learning, TypedValue.COMPLEX_UNIT_SP, LineSp.toFloat())

        show(R.id.day_location, DayWidgetSlot.Location)
        setText(R.id.day_location, state.locationNote.orEmpty(), ink.secondary)
        setTextViewTextSize(R.id.day_location, TypedValue.COMPLEX_UNIT_SP, LocationSp.toFloat())
        // Tapping the warning takes a fix in place. Without permission there is none to take here, so
        // the tap falls through to the widget's own: the app opens and asks for it.
        if (content.locationMissing && context.hasLocationPermission()) {
            setOnClickPendingIntent(R.id.day_location, WidgetLocationService.pendingIntent(context))
        }
    }

/** A glass widget: its own sky painted to its size, and the content over it in the sky's glass. */
private fun glassViews(
    context: Context,
    layout: Int,
    sky: SkyFrame,
    skyLayout: SkyLayout,
    options: Bundle,
    rtl: Boolean,
    defaultWidthDp: Int,
    defaultHeightDp: Int,
    fill: RemoteViews.(GlassInk) -> Unit,
): RemoteViews = RemoteViews(context.packageName, layout).apply {
    val (widthDp, heightDp) = widgetSizeDp(options, defaultWidthDp, defaultHeightDp)
    setImageViewBitmap(R.id.glass_sky, paintGlassSky(context, sky, skyLayout, widthDp, heightDp, rtl))
    fill(GlassInk(sky))
}

private fun RemoteViews.setText(viewId: Int, text: String, color: Int) {
    setTextViewText(viewId, text)
    setTextColor(viewId, color)
}

private fun RemoteViews.setChip(viewId: Int, text: String?, ink: GlassInk) {
    setOptionalText(viewId, text)
    setTextColor(viewId, ink.onAccent)
    setInt(viewId, "setBackgroundResource", ink.accent)
}

private fun RemoteViews.setPane(viewId: Int, ink: GlassInk) {
    setInt(viewId, "setBackgroundResource", ink.pane)
}

// Where each widget's sun and moon travel: the open sky beside or above its content.
// The date widget's text runs down the start side, so the lights keep to the end half, above the pane.
private val DateSky = SkyLayout(fromX = 0.56f, toX = 0.94f, horizonY = 0.52f, peakY = 0.14f, lightSize = 0.085f)
// The next-zman pane sits low, leaving the whole top of the square to the sky.
private val NextSky = SkyLayout(fromX = 0.12f, toX = 0.88f, horizonY = 0.36f, peakY = 0.10f, lightSize = 0.075f)
// Only the header strip is open above the list, so the arc there is shallow.
private val ZmanimSky = SkyLayout(fromX = 0.60f, toX = 0.94f, horizonY = 0.13f, peakY = 0.06f, lightSize = 0.05f)
private val ShabbatSky = SkyLayout(fromX = 0.58f, toX = 0.94f, horizonY = 0.40f, peakY = 0.12f, lightSize = 0.085f)
// The day widget's text runs from the top down on the start side, so the lights keep high on the end side.
private val DaySky = SkyLayout(fromX = 0.60f, toX = 0.94f, horizonY = 0.34f, peakY = 0.09f, lightSize = 0.07f)

// widget_glass_zmanim.xml's padding, header, pane margin and pane padding, in dp.
private const val GlassZmanimChromeDp = 80

// The day widget before the launcher has reported a size: its picker size, four cells by two.
private const val DefaultDayWidthDp = 250
private const val DefaultDayHeightDp = 110
