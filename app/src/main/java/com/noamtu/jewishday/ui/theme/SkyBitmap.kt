// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui.theme

import android.graphics.Bitmap
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * The Glass sky as a picture, for the home-screen widget.
 *
 * A widget is a RemoteViews snapshot: the launcher shows whatever the app last handed it and cannot
 * run Compose drawing of its own. So the sky the app paints live on screen (see [drawSky]) is drawn
 * here, by that same code, into a bitmap the widget can carry. The widget is re-rendered on a
 * schedule, and each fresh bitmap is what walks the sun along its arc through the day.
 *
 * Drawn at a density of 1 so [widthPx] and [heightPx] are taken as plain pixels; the sky lays itself
 * out as fractions of its size, so nothing in it depends on density. A [cornerRadiusPx] above zero
 * rounds the corners (transparent outside them) so the sky can sit inside a widget's rounded frame.
 */
fun renderSkyBitmap(frame: SkyFrame, widthPx: Int, heightPx: Int, cornerRadiusPx: Float = 0f): Bitmap {
    val width = widthPx.coerceAtLeast(1)
    val height = heightPx.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap.asImageBitmap())
    val bounds = Size(width.toFloat(), height.toFloat())
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, bounds) {
        if (cornerRadiusPx > 0f) {
            val rounded = Path().apply {
                addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(cornerRadiusPx)))
            }
            clipPath(rounded) { drawSky(frame) }
        } else {
            drawSky(frame)
        }
    }
    return bitmap
}
