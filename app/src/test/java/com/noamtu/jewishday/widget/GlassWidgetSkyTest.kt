// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import kotlin.math.min
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Android 17 refuses a widget update whose bitmaps add up to more than 1.5 full-screen ARGB buffers,
 * so the glass widgets' sky must stay under that on every screen, at every size a widget can take.
 */
class GlassWidgetSkyTest {

    /** A screen: its size in dp as held in portrait, and its density. */
    private data class Screen(val name: String, val widthDp: Int, val heightDp: Int, val density: Float) {
        val pixels: Long get() = (widthDp * density).toLong() * (heightDp * density).toLong()
    }

    private val screens = listOf(
        Screen("ldpi small phone", 320, 427, 0.75f),
        Screen("mdpi phone", 320, 480, 1f),
        Screen("hdpi phone", 320, 533, 1.5f),
        Screen("xhdpi phone", 360, 640, 2f),
        Screen("Pixel-sized phone", 412, 915, 2.625f),
        Screen("QHD phone", 412, 915, 3.5f),
        Screen("foldable, outer", 360, 780, 3f),
        Screen("foldable, inner", 673, 841, 2.625f),
        Screen("tablet", 800, 1280, 2f),
        Screen("large tablet", 1024, 1366, 2f),
        Screen("tablet in landscape", 1280, 800, 2f),
    )

    /** Every size a glass widget is drawn at on [screen]: its defaults, its preview, and the whole screen. */
    private fun sizesOn(screen: Screen): List<Pair<Int, Int>> = WidgetKind.entries.filter { it.glass }
        .map { it.previewWidthDp to it.previewHeightDp } +
        listOf(
            DefaultWideWidthDp to DefaultRegularHeightDp,
            DefaultListWidthDp to DefaultRegularHeightDp,
            DefaultListWidthDp to DefaultListHeightDp,
            screen.widthDp to screen.heightDp,
            screen.heightDp to screen.widthDp,
            screen.widthDp to 80,
        )

    @Test
    fun `no glass sky is ever more than half of what a widget update may carry`() {
        for (screen in screens) {
            for ((widthDp, heightDp) in sizesOn(screen)) {
                val sky = skyBitmapSize(widthDp, heightDp, screen.density, screen.pixels)
                val limit = widgetBitmapLimitBytes(screen.pixels)
                assertTrue("${widthDp}x${heightDp}dp on the ${screen.name}: ${sky.bytes} bytes, limit $limit", sky.bytes * 2 <= limit)
            }
        }
    }

    @Test
    fun `a sky laid out on a foldable's inner screen still fits once it is closed`() {
        val inner = screens.single { it.name == "foldable, inner" }
        val outer = screens.single { it.name == "foldable, outer" }
        for ((widthDp, heightDp) in sizesOn(inner)) {
            val sky = skyBitmapSize(widthDp, heightDp, inner.density, inner.pixels)
            val limit = widgetBitmapLimitBytes(outer.pixels)
            assertTrue("${widthDp}x${heightDp}dp: ${sky.bytes} bytes, limit $limit", sky.bytes <= limit)
        }
    }

    @Test
    fun `on a phone or tablet the sky is drawn exactly as before the limit`() {
        for (screen in screens.filter { it.density >= 2f }) {
            for ((widthDp, heightDp) in sizesOn(screen)) {
                val before = min(screen.density, sqrt(100_000f / (widthDp.toFloat() * heightDp))).coerceAtLeast(1f)
                assertEquals(
                    "${widthDp}x${heightDp}dp on the ${screen.name}",
                    before,
                    skyBitmapSize(widthDp, heightDp, screen.density, screen.pixels).scale,
                )
            }
        }
    }

    @Test
    fun `the previews stay small`() {
        for (kind in WidgetKind.entries.filter { it.glass }) {
            val sky = skyBitmapSize(kind.previewWidthDp, kind.previewHeightDp, 3.5f, 1440L * 3200)
            assertTrue("$kind preview: ${sky.bytes} bytes", sky.bytes <= 400_000)
        }
    }
}
