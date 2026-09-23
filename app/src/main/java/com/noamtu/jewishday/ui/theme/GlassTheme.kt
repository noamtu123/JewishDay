// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.PI
import kotlin.math.sin

/** True while the Glass theme is in use: surfaces go see-through over the sky backdrop. */
val LocalGlassTheme = staticCompositionLocalOf { false }

/** How the glass is drawn right now — the dark kind over a night sky, the milky kind by day. */
data class GlassStyle(
    /** A frosted pane: enough colour to read as glass, little enough to let the sky through. */
    val fill: Color,
    /** The lit edge along a pane — most of what makes a translucent box read as glass. */
    val edge: Color,
    /** The floating navigation pill. */
    val bar: Color,
    /** The selected tab inside the pill, and what is written on it. */
    val selected: Color,
    val onSelected: Color,
)

private val DarkGlass = GlassStyle(
    fill = Color.White.copy(alpha = 0.12f),
    edge = Color.White.copy(alpha = 0.28f),
    bar = Color.White.copy(alpha = 0.14f),
    selected = Color.White.copy(alpha = 0.28f),
    onSelected = Color.White,
)

private val LightGlass = GlassStyle(
    fill = Color.White.copy(alpha = 0.55f),
    edge = Color.White.copy(alpha = 0.95f),
    bar = Color.White.copy(alpha = 0.60f),
    selected = Color(0xFF18181F),
    onSelected = Color.White,
)

val LocalGlassStyle = staticCompositionLocalOf { DarkGlass }

// Glass over the night and dusk skies: light text, solid navy for dialogs and menus.
private val GlassNightColors = darkColorScheme(
    primary = Color(0xFFB9D6FF),
    onPrimary = Color(0xFF00315B),
    primaryContainer = Color(0xFF2B4F7A),
    onPrimaryContainer = Color(0xFFD6E9FF),
    secondary = Color(0xFFCFC8F5),
    secondaryContainer = Color(0xFF433C6B),
    onSecondaryContainer = Color(0xFFE6E0FF),
    tertiary = Color(0xFFFFC9A8),
    background = Color(0xFF0E1530),
    onBackground = Color.White,
    surface = Color(0xFF18203F),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF28304F),
    onSurfaceVariant = Color(0xFFD4DAF0),
    surfaceContainerHigh = Color(0xFF222A4A),
    surfaceContainerHighest = Color(0xFF2A3354),
    outline = Color(0xFFA3ABC8),
    outlineVariant = Color.White.copy(alpha = 0.14f),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF8C2F2A),
    onErrorContainer = Color(0xFFFFDAD6),
)

// Glass over the day skies: near-black text, soft white for dialogs and menus.
private val GlassDayColors = lightColorScheme(
    primary = Color(0xFF2F4FA8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE4FF),
    onPrimaryContainer = Color(0xFF0B1A4A),
    secondary = Color(0xFF5B5580),
    secondaryContainer = Color(0xFFE6E1FA),
    onSecondaryContainer = Color(0xFF1D1838),
    tertiary = Color(0xFF9A4F24),
    background = Color(0xFFF1F2F8),
    onBackground = Color(0xFF18181F),
    surface = Color(0xFFFAFAFD),
    onSurface = Color(0xFF18181F),
    surfaceVariant = Color(0xFFEDEEF6),
    onSurfaceVariant = Color(0xFF55566A),
    surfaceContainerHigh = Color(0xFFF4F4FA),
    surfaceContainerHighest = Color(0xFFEDEEF6),
    outline = Color(0xFF7C7D92),
    outlineVariant = Color(0xFF14142A).copy(alpha = 0.08f),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

/**
 * The sky behind everything in the Glass theme. Under any other theme it draws nothing and simply
 * hosts [content].
 *
 * The sky follows the day's real alot, sunrise, sunset and tzeit (see [skyAt]) and every change is
 * eased, so it drifts rather than steps. It also decides which glass the app is wearing: dark glass
 * and light text at night and dusk, milky glass and dark text by day. That swaps the whole colour
 * scheme under it, and the status-bar icons with it.
 */
@Composable
fun GlassBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (!LocalGlassTheme.current) {
        Box(modifier) { content() }
        return
    }
    val viewModel: GlassSkyViewModel = hiltViewModel()
    val frame by viewModel.frame.collectAsStateWithLifecycle()
    val sky = frame
    if (sky == null) {
        // The first frame is a moment away; the static Glass scheme covers it.
        Box(modifier) { content() }
        return
    }

    val ease = tween<Color>(durationMillis = 3_000)
    val easeF = tween<Float>(durationMillis = 3_000)
    val base by animateColorAsState(sky.base, ease, label = "skyBase")
    val bloom0 by animateColorAsState(sky.blooms[0], ease, label = "bloom0")
    val bloom1 by animateColorAsState(sky.blooms[1], ease, label = "bloom1")
    val bloom2 by animateColorAsState(sky.blooms[2], ease, label = "bloom2")
    val bloomAlpha by animateFloatAsState(sky.bloomAlpha, easeF, label = "bloomAlpha")
    val stars by animateFloatAsState(sky.stars, easeF, label = "stars")
    val sun by animateFloatAsState(sky.sun, easeF, label = "sun")
    val sunArc by animateFloatAsState(sky.sunArc, easeF, label = "sunArc")

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !sky.dark
            controller.isAppearanceLightNavigationBars = !sky.dark
        }
    }

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(base)
            drawStars(stars)
            drawSun(sun, sunArc, sky.dark)
            // Blooms placed as in the mockup: top corner, left middle, bottom corner.
            drawBloom(bloom0, bloomAlpha, Offset(size.width * 0.95f, size.height * 0.08f), size.width * 0.75f)
            drawBloom(bloom1, bloomAlpha, Offset(size.width * 0.05f, size.height * 0.52f), size.width * 0.70f)
            drawBloom(bloom2, bloomAlpha, Offset(size.width * 0.85f, size.height * 0.98f), size.width * 0.65f)
        }
        MaterialTheme(colorScheme = if (sky.dark) GlassNightColors else GlassDayColors) {
            CompositionLocalProvider(LocalGlassStyle provides if (sky.dark) DarkGlass else LightGlass) {
                content()
            }
        }
    }
}

/** A soft pool of light: the colour at the centre fading to nothing at [radius]. */
private fun DrawScope.drawBloom(color: Color, alpha: Float, center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), color.copy(alpha = alpha * 0.45f), Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

/** The sun climbs from one side at sunrise, peaks at midday and sets on the other side. */
private fun DrawScope.drawSun(alpha: Float, arc: Float, dusk: Boolean) {
    if (alpha <= 0.01f) return
    val center = Offset(
        x = size.width * (0.12f + arc * 0.76f),
        y = size.height * (0.48f - sin(arc * PI).toFloat() * 0.36f),
    )
    val radius = size.width * 0.16f
    val core = if (dusk) Color(0xFFFFD08A) else Color(0xFFFFF6D8)
    val halo = if (dusk) Color(0xFFFF8A5C) else Color(0xFFFFD98A)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(core.copy(alpha = alpha), halo.copy(alpha = alpha * 0.6f), Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

// Fixed spots, as fractions of the screen, so the stars do not jump between frames.
private val StarField = listOf(
    0.20f to 0.12f, 0.70f to 0.08f, 0.85f to 0.22f, 0.40f to 0.30f, 0.12f to 0.40f, 0.58f to 0.18f,
    0.30f to 0.05f, 0.92f to 0.36f, 0.50f to 0.44f, 0.08f to 0.20f, 0.76f to 0.50f, 0.64f to 0.33f,
    0.25f to 0.58f, 0.88f to 0.62f, 0.45f to 0.10f, 0.15f to 0.70f,
)

private fun DrawScope.drawStars(alpha: Float) {
    if (alpha <= 0.01f) return
    StarField.forEachIndexed { index, (x, y) ->
        drawCircle(
            color = Color.White.copy(alpha = alpha * if (index % 3 == 0) 0.95f else 0.6f),
            radius = if (index % 3 == 0) 2.2f else 1.4f,
            center = Offset(size.width * x, size.height * y),
        )
    }
}
