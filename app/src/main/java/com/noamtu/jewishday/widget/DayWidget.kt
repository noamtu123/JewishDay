// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.noamtu.jewishday.MainActivity
import com.noamtu.jewishday.R
import com.noamtu.jewishday.ui.theme.renderSkyBitmap
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException

/**
 * The home-screen widget: today — its Hebrew date, badge, key times, events and learning — laid
 * over the sky the Glass theme paints at this hour, sun by day and moon and stars by night.
 *
 * A widget is a RemoteViews snapshot the launcher shows as-is, so the sky cannot be drawn live: it
 * is rendered to a bitmap here, and the widget is re-rendered on a schedule (wired up on the
 * receiver's side) so the sun visibly moves along its arc through the day. The widget is composed
 * once per size the launcher reports for it — its portrait and landscape sizes, usually — so the sky
 * is drawn at the widget's own pixels and its baked corners meet the frame, rather than at one of a
 * few declared buckets and then cropped or stretched to fit, which took the sun and moon off the
 * top. Three tiers trade detail for room — a two-cell strip keeps just the dates, a short wide widget
 * adds the badge and the times, and a tall one has the whole day — chosen by the height given.
 */
class DayWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Everything slow — settings, a location fix, zmanim — happens before the composition that
        // provideContent starts. A failure degrades to a plain tile with the app's name rather than
        // Glance's error view or a blank frame, and tapping it still opens the app.
        val state = try {
            dayWidgetStateLoader(context).load()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(Tag, "Could not load today for the widget; showing the fallback tile", e)
            null
        }
        provideContent { if (state == null) DayWidgetUnavailable() else DayWidgetContent(state) }
    }

    companion object {
        /** The strip, the smallest the widget goes (its minimum size in day_widget_info): the dates alone. */
        val Small = DpSize(100.dp, 40.dp)

        /** From this height there is room for the dates, the badge and the key times. */
        val Medium = DpSize(200.dp, 100.dp)

        /** From this height there is room for the whole day. */
        val Large = DpSize(200.dp, 180.dp)

        private const val Tag = "DayWidget"
    }
}

/** Which of the three layouts a composition is for. */
internal enum class DayWidgetTier { Small, Medium, Large }

/**
 * The tier for the size the launcher has given the widget. Only the height decides, since the two
 * wide tiers share a width and the strip is the only short one; a widget too narrow for the wide
 * tiers gets the strip however tall it is.
 */
internal fun dayWidgetTierFor(size: DpSize): DayWidgetTier = when {
    size.height < DayWidget.Medium.height -> DayWidgetTier.Small
    size.height < DayWidget.Large.height -> DayWidgetTier.Medium
    else -> DayWidgetTier.Large
}

/**
 * The pixel size the sky is rendered at for a widget of [size] on a screen of [density]: the widget's
 * own pixels, capped in width with the height scaled along so the sky keeps its shape.
 *
 * The cap exists because a widget's bitmaps travel inside its RemoteViews, and the system rejects an
 * update whose bitmaps together exceed a few times the screen's pixel count. Every size the launcher
 * reports (portrait and landscape) gets its own sky in the same update, so each must stay modest; a
 * widget is a small fraction of the screen, so at its own pixels they do, by a wide margin.
 */
internal fun skyBitmapSize(size: DpSize, density: Float): IntSize {
    val fullWidth = (size.width.value * density).roundToInt().coerceAtLeast(1)
    val scale = minOf(1f, MaxSkyWidthPx.toFloat() / fullWidth)
    return IntSize(
        width = (fullWidth * scale).roundToInt().coerceAtLeast(1),
        height = (size.height.value * density * scale).roundToInt().coerceAtLeast(1),
    )
}

@Composable
private fun DayWidgetContent(state: DayWidgetState) {
    val size = LocalSize.current
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val bitmapSize = skyBitmapSize(size, density)
    // The corners are scaled along with the bitmap so a capped sky still meets the frame's curve.
    val bitmapScale = bitmapSize.width / (size.width.value * density)
    val cornerRadiusPx = widgetCornerRadiusPx(context) * bitmapScale
    val sky = remember(state.sky, bitmapSize) {
        renderSkyBitmap(state.sky, bitmapSize.width, bitmapSize.height, cornerRadiusPx)
    }
    // Glance resolves Start/End from the device's locale, while the texts are in the app's own
    // language. When the two disagree, Start would put Hebrew on the left, so the reading side is
    // chosen by hand and rows that read in order (the times) are mirrored to match.
    val rtlLayout = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
    val mirrored = state.useHebrew != rtlLayout
    val ink = SkyInk(dark = state.sky.dark, align = if (mirrored) TextAlign.End else TextAlign.Start)
    val tier = dayWidgetTierFor(size)
    Box(modifier = GlanceModifier.fillMaxSize().appWidgetBackground().clickable(actionStartActivity<MainActivity>())) {
        // The bitmap is the frame's own size (or a capped scale of it), so it maps onto the frame
        // edge to edge and its baked corners land exactly on the launcher's; cropping would cut the
        // sun and moon off the top whenever the two differed in shape.
        Image(
            provider = ImageProvider(sky),
            contentDescription = null,
            modifier = GlanceModifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
            // The strip floats its two lines in the middle of whatever height it is given; the
            // others read from the top down with the times settling at the bottom.
            verticalAlignment = if (tier == DayWidgetTier.Small) Alignment.CenterVertically else Alignment.Top,
            horizontalAlignment = if (mirrored) Alignment.End else Alignment.Start,
        ) {
            when (tier) {
                DayWidgetTier.Small -> SmallTier(state, ink)
                DayWidgetTier.Medium -> MediumTier(state, ink, mirrored)
                DayWidgetTier.Large -> LargeTier(state, ink, mirrored)
            }
        }
    }
}

/** The strip: the two dates and nothing else. Both may wrap once, since two cells are narrow. */
@Composable
private fun SmallTier(state: DayWidgetState, ink: SkyInk) {
    Line(state.hebrewDate, ink.primary(17.sp, FontWeight.Bold), maxLines = 2)
    Line(state.weekdayAndDate, ink.primary(12.sp), maxLines = 2)
}

/**
 * Short and wide: the header and the times. The observance lines wait for the tall tier — at this
 * tier's minimum height they would push the times off the bottom, and a clipped row is worse than a
 * missing one.
 */
@Composable
private fun ColumnScope.MediumTier(state: DayWidgetState, ink: SkyInk, mirrored: Boolean) {
    Header(state, ink)
    Spacer(GlanceModifier.defaultWeight())
    TimesRow(state.times, ink, mirrored)
}

/** Tall: the whole day, least important last so that is what a crowded day loses. */
@Composable
private fun ColumnScope.LargeTier(state: DayWidgetState, ink: SkyInk, mirrored: Boolean) {
    Header(state, ink)
    // Grouped so the outer Column stays within Glance's ten-children limit on the fullest of days.
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        state.eventLine?.let { Line(it, ink.primary(12.sp)) }
        state.observanceLines.forEach { Line(it, ink.primary(12.sp)) }
    }
    Spacer(GlanceModifier.defaultWeight())
    TimesRow(state.times, ink, mirrored)
    state.learning?.let { Line(it, ink.primary(12.sp), maxLines = 2) }
    state.locationName?.let { Line(it, ink.secondary(10.sp)) }
}

/** The Hebrew date, the weekday and civil date, and the day's badge when it has one. */
@Composable
private fun Header(state: DayWidgetState, ink: SkyInk) {
    Line(state.hebrewDate, ink.primary(17.sp, FontWeight.Bold))
    Line(state.weekdayAndDate, ink.primary(12.sp))
    state.chip?.let { Line(it, ink.primary(13.sp, FontWeight.Medium)) }
}

/** The key times spread across the width, each a small label over its clock time. */
@Composable
private fun TimesRow(times: List<DayWidgetTime>, ink: SkyInk, mirrored: Boolean) {
    if (times.isEmpty()) return
    // The launcher lays a row out in the device's direction; when the texts read the other way,
    // the columns are reversed by hand so sunrise still comes first in reading order.
    val ordered = if (mirrored) times.asReversed() else times
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        ordered.forEach { time ->
            Column(modifier = GlanceModifier.defaultWeight()) {
                Line(time.label, ink.secondary(10.sp))
                Line(time.time, ink.primary(14.sp, FontWeight.Medium))
            }
        }
    }
}

/** One full-width line of text; the width is what lets its alignment mean anything. */
@Composable
private fun Line(text: String, style: TextStyle, maxLines: Int = 1) {
    Text(text = text, modifier = GlanceModifier.fillMaxWidth(), style = style, maxLines = maxLines)
}

/** What the widget shows when today could not be loaded: the app's name on night blue, still tappable. */
@Composable
private fun DayWidgetUnavailable() {
    Box(
        modifier = GlanceModifier.fillMaxSize().background(NightNavy).clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = LocalContext.current.getString(R.string.app_name),
            style = TextStyle(color = ColorProvider(Color.White), fontSize = 14.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
    }
}

/**
 * How text sits on the sky: white over a dark sky, deep navy over a light one, always on the reading
 * side. Secondary text is the same ink a little lighter, for labels and the location.
 */
private class SkyInk(dark: Boolean, private val align: TextAlign) {
    private val color = if (dark) Color.White else NavyInk
    private val primary = ColorProvider(color)
    private val secondary = ColorProvider(color.copy(alpha = 0.82f))

    fun primary(size: TextUnit, weight: FontWeight = FontWeight.Normal): TextStyle =
        TextStyle(color = primary, fontSize = size, fontWeight = weight, textAlign = align)

    fun secondary(size: TextUnit): TextStyle = TextStyle(color = secondary, fontSize = size, textAlign = align)
}

/**
 * The radius the launcher clips widgets to on Android 12+, so the sky's baked corners meet the frame
 * exactly; older launchers do not clip at all, so a fixed radius rounds the sky itself.
 */
private fun widgetCornerRadiusPx(context: Context): Float =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.resources.getDimension(android.R.dimen.system_app_widget_background_radius)
    } else {
        FallbackCornerRadius.value * context.resources.displayMetrics.density
    }

private val NavyInk = Color(0xFF1B2440)

/** The night sky's base colour, so the fallback tile still looks like the app. */
private val NightNavy = Color(0xFF0A0F24)
private val FallbackCornerRadius = 16.dp
// 720 px wide keeps even four composed sizes of a full-width widget well inside the RemoteViews budget.
private const val MaxSkyWidthPx = 720
