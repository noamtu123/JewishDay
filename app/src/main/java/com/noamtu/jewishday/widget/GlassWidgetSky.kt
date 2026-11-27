// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.createBitmap
import com.noamtu.jewishday.R
import com.noamtu.jewishday.ui.theme.SkyFrame
import com.noamtu.jewishday.ui.theme.drawBloom
import com.noamtu.jewishday.ui.theme.drawCrescent
import com.noamtu.jewishday.ui.theme.drawHorizon
import com.noamtu.jewishday.ui.theme.drawSunDisc
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Where the sun and moon travel inside one glass widget, as fractions of its width and height.
 *
 * Each widget lays its own sky out around its content rather than showing a crop of the app's
 * full-screen one: the lights keep to the open sky beside or above the text, sized to the widget,
 * so a small widget still gets a whole sun rather than the edge of one.
 */
internal data class SkyLayout(
    // The sun rises at [fromX] and sets at [toX]; in a right-to-left widget the two swap sides.
    val fromX: Float,
    val toX: Float,
    val horizonY: Float,
    val peakY: Float,
    // The disc's radius, as a fraction of the widget's shorter side.
    val lightSize: Float,
)

/** Paints the Glass sky of [sky] into a bitmap the size of the widget, with its corners rounded. */
internal fun paintGlassSky(
    context: Context,
    sky: SkyFrame,
    layout: SkyLayout,
    widthDp: Int,
    heightDp: Int,
    rtl: Boolean,
): Bitmap {
    val metrics = context.resources.displayMetrics
    val (width, height, scale) = skyBitmapSize(widthDp, heightDp, metrics.density, metrics.widthPixels.toLong() * metrics.heightPixels)

    val image = ImageBitmap(width, height)
    CanvasDrawScope().draw(Density(scale), LayoutDirection.Ltr, Canvas(image), Size(width.toFloat(), height.toFloat())) {
        drawSky(sky, layout, rtl)
    }
    return roundCorners(image.asAndroidBitmap(), context.resources.getDimension(R.dimen.widget_corner_radius) / context.resources.displayMetrics.density * scale)
}

/**
 * The sky bitmap's size in pixels for a widget [widthDp] x [heightDp] on a screen of [density] with
 * [screenPixels] pixels in all.
 *
 * The sky is soft gradients, so it is drawn at a lower resolution than the screen and stretched: a
 * bitmap goes to the launcher in every update, and a full-resolution one for a large widget would be
 * megabytes each time. Usually that keeps it to [MaxSkyPixels]; a very large widget keeps at least a
 * pixel per dp, so its stars and the sun's rim stay sharp.
 *
 * Whatever that comes to, it never passes half of what Android 17 accepts in one widget update
 * ([widgetBitmapLimitBytes]): past that the launcher refuses the update and the widget is left as it
 * was. Half, because the screen can change under a widget — a foldable closed onto its smaller outer
 * screen — between its size being reported and its picture being sent. On a phone the ceiling never
 * comes into play; it only catches a huge widget on a low-density screen.
 */
internal fun skyBitmapSize(widthDp: Int, heightDp: Int, density: Float, screenPixels: Long): SkyBitmapSize {
    val areaDp = (widthDp.toFloat() * heightDp).coerceAtLeast(1f)
    val fitsBudget = sqrt(MaxSkyPixels / areaDp)
    val ceiling = sqrt(widgetBitmapLimitBytes(screenPixels) / 2f / SkyBytesPerPixel / areaDp)
    val scale = minOf(min(density, fitsBudget).coerceAtLeast(1f), ceiling)
    return SkyBitmapSize((widthDp * scale).toInt().coerceAtLeast(1), (heightDp * scale).toInt().coerceAtLeast(1), scale)
}

/** A sky bitmap's [width] and [height] in pixels, and the pixels per dp it is drawn at. */
internal data class SkyBitmapSize(val width: Int, val height: Int, val scale: Float) {
    val bytes: Long get() = width.toLong() * height * SkyBytesPerPixel
}

/**
 * The most bitmap memory, in bytes, Android 17 accepts in one widget update from an app targeting
 * it: 1.5 times a full-screen ARGB_8888 buffer of the screen it is shown on.
 */
internal fun widgetBitmapLimitBytes(screenPixels: Long): Long = 6L * screenPixels

/** The sky keeps its alpha (its corners are rounded), so it is ARGB_8888: four bytes a pixel. */
internal const val SkyBytesPerPixel = 4

private fun DrawScope.drawSky(sky: SkyFrame, layout: SkyLayout, rtl: Boolean) {
    val w = size.width
    val h = size.height
    val shortSide = min(w, h)
    fun x(fraction: Float) = w * if (rtl) 1f - fraction else fraction

    // The same sky as the app's backdrop (GlassBackdrop): deeper at the zenith, three blooms and the
    // horizon glow — but placed for a widget's shape, which is usually wider than it is tall.
    val zenith = if (sky.dark) lerp(sky.base, Color.Black, 0.18f) else lerp(sky.base, sky.blooms[0], 0.55f)
    drawRect(Brush.verticalGradient(listOf(zenith, sky.base, lerp(sky.base, Color.White, 0.04f))))
    val reach = maxOf(w, h)
    drawBloom(sky.blooms[0], sky.bloomAlpha, Offset(x(0.92f - sky.sunArc * 0.25f), h * 0.02f), reach * 0.75f)
    drawBloom(sky.blooms[1], sky.bloomAlpha, Offset(x(0.04f + sky.sunArc * 0.12f), h * 0.70f), reach * 0.70f)
    drawHorizon(sky.blooms[2], sky.bloomAlpha)

    if (sky.stars > 0.01f) {
        WidgetStarField.forEachIndexed { index, (sx, sy) ->
            val bright = index % 3 == 0
            drawCircle(
                color = Color.White.copy(alpha = sky.stars * if (bright) 0.95f else 0.6f),
                radius = (if (bright) 1.3f else 0.85f) * density,
                center = Offset(x(sx), h * sy),
            )
        }
    }

    val lightRadius = shortSide * layout.lightSize
    // The moon keeps to one spot high in the open sky, as it does in the app.
    drawCrescent(
        alpha = sky.moon,
        center = Offset(x(layout.fromX + (layout.toX - layout.fromX) * 0.72f), h * layout.peakY + lightRadius * 0.4f),
        radius = lightRadius * 0.85f,
    )
    // The sun climbs from one side at sunrise, peaks at midday and sets on the other.
    val arc = sky.sunArc
    drawSunDisc(
        alpha = sky.sun,
        center = Offset(
            x = x(layout.fromX + (layout.toX - layout.fromX) * arc),
            y = h * (layout.horizonY - sin(arc * PI).toFloat() * (layout.horizonY - layout.peakY)),
        ),
        disc = lightRadius,
        dusk = sky.dark,
    )
}

/** Clips [source] to a rounded rectangle, anti-aliased (a clip path on a bitmap canvas is not). */
private fun roundCorners(source: Bitmap, radius: Float): Bitmap {
    val output = createBitmap(source.width, source.height)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }
    android.graphics.Canvas(output).drawRoundRect(
        RectF(0f, 0f, source.width.toFloat(), source.height.toFloat()),
        radius,
        radius,
        paint,
    )
    source.recycle()
    return output
}

// Fixed spots, as fractions of the widget, so the stars stay put from one update to the next.
private val WidgetStarField = listOf(
    0.08f to 0.14f, 0.22f to 0.06f, 0.35f to 0.20f, 0.48f to 0.09f, 0.61f to 0.24f, 0.73f to 0.07f,
    0.86f to 0.18f, 0.95f to 0.30f, 0.15f to 0.34f, 0.55f to 0.38f, 0.28f to 0.46f, 0.80f to 0.42f,
    0.42f to 0.56f, 0.90f to 0.58f, 0.05f to 0.62f, 0.66f to 0.66f,
)

// About 400 KB of ARGB per sky: well inside what a widget update can carry.
private const val MaxSkyPixels = 100_000f
