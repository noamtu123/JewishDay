// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.noamtu.jewishday.R
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

/**
 * The date over the sky, and the next time on a pane. One row high the two sit side by side; from
 * two rows the date heads the widget and the pane sits at the bottom, and as the widget grows the
 * date and time grow with it and the pane lists the times after the next one, keeping the band
 * between them open for the sun or moon.
 */
internal fun glassDateViews(context: Context, content: WidgetContent, sky: SkyFrame, options: Bundle): RemoteViews {
    val (widthDp, heightDp) = widgetSizeDp(options, DefaultWideWidthDp, DefaultRegularHeightDp)
    val shape = widgetShape(heightDp)
    if (shape == WidgetShape.Strip) return glassDateStrip(context, content, sky, widthDp, heightDp)

    val titleDp = fittedTextSize(content.hebrewDate, widthDp - 28f, 22f, minOf(if (shape == WidgetShape.Tall) 46f else 32f, heightDp * 0.16f))
    val timeDp = if (shape == WidgetShape.Tall) (heightDp * 0.085f).coerceIn(24f, 34f) else 22f
    val headerDp = 14 + titleDp * LineHeight + 18 + (if (content.specialDay != null) 28 else 0)
    val paneDp = 16 + maxOf(34f, timeDp * LineHeight)
    // Whatever height the sun needs is kept back; the rest of the open band goes to more times.
    val spareDp = heightDp - headerDp - paneDp - 14 - heightDp * OpenSkyShare
    val ahead = content.ahead.drop(1).take((spareDp / AheadRowDp).toInt().coerceIn(0, MaxAheadRows))
    val paneTopDp = heightDp - 14 - paneDp - ahead.size * AheadRowDp - (if (ahead.isEmpty()) 0 else 4)
    val skyLayout = if (shape == WidgetShape.Tall) openBandSky(headerDp / heightDp, paneTopDp / heightDp) else DateSky

    return glassViews(context, R.layout.widget_glass_date, sky, skyLayout, widthDp, heightDp, content.rtl) { ink ->
        setText(R.id.glass_hebrew_date, content.hebrewDate, ink.primary)
        setTextSizeDp(R.id.glass_hebrew_date, titleDp)
        setText(R.id.glass_gregorian_date, content.gregorianDate, ink.secondary)
        setChip(R.id.glass_special_day, content.specialDay, ink)
        setPane(R.id.glass_pane, ink)
        setText(R.id.glass_next_label, content.nextLabel, ink.secondary)
        setText(R.id.glass_next_title, content.next?.title ?: "—", ink.primary)
        setText(R.id.glass_next_time, content.next?.time.orEmpty(), ink.time)
        setTextSizeDp(R.id.glass_next_time, timeDp)
        removeAllViews(R.id.glass_ahead)
        setViewVisibility(R.id.glass_ahead, if (ahead.isEmpty()) View.GONE else View.VISIBLE)
        ahead.forEach { zman ->
            addView(R.id.glass_ahead, glassRow(context, R.layout.widget_glass_row, zman, ink.primary).apply {
                // Under the next time in the pane, flush with it rather than inset for a highlight.
                setViewPadding(R.id.glass_row, 0, 0, 0, 0)
            })
        }
    }
}

/** One row high: the date at the start and the next time in a small pane at the end, if both fit. */
private fun glassDateStrip(context: Context, content: WidgetContent, sky: SkyFrame, widthDp: Int, heightDp: Int): RemoteViews {
    val next = content.next
    // The pane is as wide as its longer line; it gives way if the date would be squeezed below legible.
    val paneDp = next?.let { 24 + maxOf(minOf(it.title.length * CharWidthEm * 12, 130f), it.time.length * CharWidthEm * 20) } ?: 0f
    val withPane = next != null && (widthDp - 24 - 10 - paneDp) / (content.hebrewDate.length * CharWidthEm) >= 20f
    val dateWidthDp = widthDp - 24f - (if (withPane) 10 + paneDp else 0f)
    val titleDp = fittedTextSize(content.hebrewDate, dateWidthDp, 18f, minOf(30f, heightDp * 0.3f))
    // A row this short holds two lines under the date only if it is tall enough; the day's name wins.
    val showGregorian = content.specialDay == null || heightDp >= TwoLinesUnderStripDp
    return glassViews(context, R.layout.widget_glass_date_strip, sky, StripSky, widthDp, heightDp, content.rtl) { ink ->
        setText(R.id.glass_hebrew_date, content.hebrewDate, ink.primary)
        setTextSizeDp(R.id.glass_hebrew_date, titleDp)
        setOptionalText(R.id.glass_gregorian_date, content.gregorianDate.takeIf { showGregorian })
        setTextColor(R.id.glass_gregorian_date, ink.secondary)
        setChip(R.id.glass_special_day, content.specialDay, ink)
        setViewVisibility(R.id.glass_pane, if (withPane) View.VISIBLE else View.GONE)
        setPane(R.id.glass_pane, ink)
        setText(R.id.glass_next_title, next?.title.orEmpty(), ink.primary)
        setText(R.id.glass_next_time, next?.time.orEmpty(), ink.time)
    }
}

/**
 * The next time. From two rows (its 3x2 default) it is a pane low in the widget under the sky; one
 * row high it sits on the sky itself, with the one after it beneath only when that line fits whole.
 */
internal fun glassNextZmanViews(context: Context, content: WidgetContent, sky: SkyFrame, options: Bundle): RemoteViews {
    val (widthDp, heightDp) = widgetSizeDp(options, DefaultListWidthDp, DefaultRegularHeightDp)
    if (widgetShape(heightDp) == WidgetShape.Strip) {
        val time = content.next?.time.orEmpty()
        val following = content.following?.let { "${content.followingLabel}: ${it.title} ${it.time}" }
            ?.takeIf { it.length * CharWidthEm * 11 <= widthDp - 28 }
        return glassViews(context, R.layout.widget_glass_next_strip, sky, NextStripSky, widthDp, heightDp, content.rtl) { ink ->
            setText(R.id.glass_next_title, content.next?.title ?: "—", ink.primary)
            setText(R.id.glass_next_time, time, ink.time)
            setTextSizeDp(R.id.glass_next_time, fittedTextSize(time, widthDp - 28f, 20f, minOf(34f, heightDp * 0.32f)))
            setOptionalText(R.id.glass_following, following)
            setTextColor(R.id.glass_following, ink.secondary)
        }
    }
    return glassViews(context, R.layout.widget_glass_next, sky, NextSky, widthDp, heightDp, content.rtl) { ink ->
        setPane(R.id.glass_pane, ink)
        setText(R.id.glass_next_label, content.nextLabel, ink.secondary)
        setText(R.id.glass_next_title, content.next?.title ?: "—", ink.primary)
        setText(R.id.glass_next_time, content.next?.time.orEmpty(), ink.time)
        setOptionalText(R.id.glass_following, content.following?.let { "${content.followingLabel}: ${it.title} ${it.time}" })
        setTextColor(R.id.glass_following, ink.secondary)
    }
}

internal fun glassZmanimViews(context: Context, content: WidgetContent, sky: SkyFrame, options: Bundle): RemoteViews {
    val (widthDp, heightDp) = widgetSizeDp(options, DefaultListWidthDp, DefaultListHeightDp)
    return glassViews(context, R.layout.widget_glass_zmanim, sky, ZmanimSky, widthDp, heightDp, content.rtl) { ink ->
        setText(R.id.glass_hebrew_date, content.hebrewDate, ink.primary)
        setChip(R.id.glass_special_day, content.specialDay, ink)
        setPane(R.id.glass_pane, ink)
        removeAllViews(R.id.glass_rows)
        val capacity = rowsThatFit(options, GlassZmanimChromeDp)
        val rows = visibleRows(content, capacity)
        rows.forEach { (index, zman) ->
            val isNext = index == content.nextIndex
            val color = when {
                isNext -> ink.onAccent
                zman.isPast -> ink.past
                else -> ink.primary
            }
            val row = glassRow(context, R.layout.widget_glass_zman_slot, zman, color)
            if (isNext) row.setInt(R.id.glass_row, "setBackgroundResource", ink.accent)
            addView(R.id.glass_rows, row)
        }
        addRowSpacers(context, R.id.glass_rows, rowSlots(options, GlassZmanimChromeDp) - rows.size)
    }
}

/**
 * What is coming and when it comes in and goes out. One row high the times sit beside the name; from
 * two rows they are panes along the bottom, and on a taller widget the name and times grow with it
 * while the band between keeps the sky of the candle lighting.
 */
internal fun glassShabbatViews(context: Context, content: WidgetContent, sky: SkyFrame, options: Bundle): RemoteViews {
    val (widthDp, heightDp) = widgetSizeDp(options, DefaultWideWidthDp, DefaultRegularHeightDp)
    val shape = widgetShape(heightDp)
    val shabbat = content.shabbat
    val title = shabbat?.title.orEmpty()
    if (shape == WidgetShape.Strip) {
        return glassViews(context, R.layout.widget_glass_shabbat_strip, sky, StripSky, widthDp, heightDp, content.rtl) { ink ->
            setText(R.id.glass_shabbat_title, title, ink.primary)
            setOptionalText(R.id.glass_shabbat_subtitle, shabbat?.subtitle)
            setTextColor(R.id.glass_shabbat_subtitle, ink.secondary)
            fillShabbatTimes(shabbat, ink)
        }
    }

    val titleDp = fittedTextSize(title, widthDp - 28f, 20f, minOf(if (shape == WidgetShape.Tall) 40f else 28f, heightDp * 0.15f))
    val timeDp = if (shape == WidgetShape.Tall) (heightDp * 0.09f).coerceIn(24f, 36f) else 22f
    val headerDp = 14 + titleDp * LineHeight + (if (shabbat?.subtitle != null) 28 else 0)
    val paneTopDp = heightDp - 14 - 16 - 15 - timeDp * LineHeight
    val skyLayout = if (shape == WidgetShape.Tall) openBandSky(headerDp / heightDp, paneTopDp / heightDp) else ShabbatSky
    return glassViews(context, R.layout.widget_glass_shabbat, sky, skyLayout, widthDp, heightDp, content.rtl) { ink ->
        setText(R.id.glass_shabbat_title, title, ink.primary)
        setTextSizeDp(R.id.glass_shabbat_title, titleDp)
        setChip(R.id.glass_shabbat_subtitle, shabbat?.subtitle, ink)
        fillShabbatTimes(shabbat, ink)
        setTextSizeDp(R.id.glass_entry_time, timeDp)
        setTextSizeDp(R.id.glass_exit_time, timeDp)
    }
}

private fun RemoteViews.fillShabbatTimes(shabbat: WidgetShabbat?, ink: GlassInk) {
    setPane(R.id.glass_entry_pane, ink)
    setPane(R.id.glass_exit_pane, ink)
    setText(R.id.glass_entry_label, shabbat?.entryHeading.orEmpty(), ink.secondary)
    setText(R.id.glass_entry_time, shabbat?.entryTime ?: "—", ink.time)
    setText(R.id.glass_exit_label, shabbat?.exitHeading.orEmpty(), ink.secondary)
    setText(R.id.glass_exit_time, shabbat?.exitTime ?: "—", ink.time)
}

/** One time on a line, in [layout]: a fixed-height row, or a share of the zmanim list. */
private fun glassRow(context: Context, layout: Int, zman: WidgetZman, color: Int) =
    RemoteViews(context.packageName, layout).apply {
        setText(R.id.glass_row_title, zman.title, color)
        setText(R.id.glass_row_time, zman.time, color)
    }

/**
 * On a tall widget the sun and moon travel the open band between the header (ending at [headerBottom])
 * and the panes (starting at [paneTop]), both fractions of the height, across most of the width.
 */
private fun openBandSky(headerBottom: Float, paneTop: Float): SkyLayout {
    val band = (paneTop - headerBottom).coerceAtLeast(0.12f)
    return SkyLayout(
        fromX = 0.14f,
        toX = 0.86f,
        horizonY = paneTop - band * 0.12f,
        peakY = headerBottom + band * 0.32f,
        lightSize = (band * 0.22f).coerceIn(0.05f, 0.09f),
    )
}

/** A glass widget: its own sky painted to its size, and the content over it in the sky's glass. */
private fun glassViews(
    context: Context,
    layout: Int,
    sky: SkyFrame,
    skyLayout: SkyLayout,
    widthDp: Int,
    heightDp: Int,
    rtl: Boolean,
    fill: RemoteViews.(GlassInk) -> Unit,
): RemoteViews = RemoteViews(context.packageName, layout).apply {
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
// One row high there is no open band, so the lights take the middle, between the text and the panes.
// The 2x1 next zman's text keeps to the start, so its lights keep to the end.
private val NextStripSky = SkyLayout(fromX = 0.66f, toX = 0.92f, horizonY = 0.88f, peakY = 0.26f, lightSize = 0.14f)
private val StripSky = SkyLayout(fromX = 0.40f, toX = 0.64f, horizonY = 0.92f, peakY = 0.24f, lightSize = 0.13f)
private val ShabbatSky = SkyLayout(fromX = 0.58f, toX = 0.94f, horizonY = 0.40f, peakY = 0.12f, lightSize = 0.085f)

// widget_glass_zmanim.xml's padding, header, pane margin and pane padding, in dp.
private const val GlassZmanimChromeDp = 72

// Height estimates for laying out by size: a line is about 1.3 of its text size, a listed time 24dp.
private const val LineHeight = 1.3f
private const val AheadRowDp = 24
private const val MaxAheadRows = 4
// The share of a widget's height kept open for the sky before more times are listed in it.
private const val OpenSkyShare = 0.3f
private const val TwoLinesUnderStripDp = 120

// Sizes to lay out for before the launcher has reported one: the providers' target cells.
internal const val DefaultWideWidthDp = 300
internal const val DefaultRegularHeightDp = 190
// The zmanim list's 3x3.
internal const val DefaultListWidthDp = 225
internal const val DefaultListHeightDp = 330
