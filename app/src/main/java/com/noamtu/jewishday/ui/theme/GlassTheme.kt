// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui.theme

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.PI
import kotlin.math.pow
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

/** In Glass a solid container becomes a pane of glass; under every other theme it is [color]. */
@Composable
fun glassOr(color: Color): Color = if (LocalGlassTheme.current) LocalGlassStyle.current.fill else color

/** In Glass an accent container keeps its hue but lets the sky through; otherwise [color]. */
@Composable
fun glassTint(color: Color): Color = if (LocalGlassTheme.current) color.copy(alpha = 0.45f) else color

/** The lit edge a glass pane carries, or none under the other themes. */
@Composable
fun glassBorder(): BorderStroke? =
    if (LocalGlassTheme.current) BorderStroke(1.dp, LocalGlassStyle.current.edge) else null

// Glass over the night and dusk skies: light text, periwinkle accents, warm gold for "good" (the
// compass once aligned) — the sun's and candlelight's colour, so it belongs to the sky — and soft
// coral for warnings and fasts. Every role is set — a role left out
// falls back to Material's baseline purple, which is what made chips and the compass look foreign.
private val GlassNightColors = darkColorScheme(
    primary = Color(0xFFB9C8FF),
    onPrimary = Color(0xFF1A2466),
    primaryContainer = Color(0xFF36407F),
    onPrimaryContainer = Color(0xFFDEE3FF),
    secondary = Color(0xFFC8C3EE),
    onSecondary = Color(0xFF2E2A55),
    secondaryContainer = Color(0xFF3B3868),
    onSecondaryContainer = Color(0xFFE4E0FF),
    tertiary = Color(0xFFFFD68A),
    onTertiary = Color(0xFF3D2A00),
    tertiaryContainer = Color(0xFF5A4418),
    onTertiaryContainer = Color(0xFFFFE9B8),
    background = Color(0xFF0E1530),
    onBackground = Color(0xFFF4F5FF),
    surface = Color(0xFF181E3E),
    onSurface = Color(0xFFF4F5FF),
    surfaceVariant = Color(0xFF282F52),
    onSurfaceVariant = Color(0xFFCDD2EC),
    surfaceContainerLowest = Color(0xFF0E1330),
    surfaceContainerLow = Color(0xFF161C3A),
    surfaceContainer = Color(0xFF1B2242),
    surfaceContainerHigh = Color(0xFF222A4A),
    surfaceContainerHighest = Color(0xFF2A3354),
    inverseSurface = Color(0xFFE6E8F6),
    inverseOnSurface = Color(0xFF1B1F36),
    inversePrimary = Color(0xFF3B4FB8),
    outline = Color(0xFFA3ABC8),
    outlineVariant = Color.White.copy(alpha = 0.14f),
    error = Color(0xFFFFB3A6),
    onError = Color(0xFF5C1408),
    errorContainer = Color(0xFF7A2E26),
    onErrorContainer = Color(0xFFFFDAD3),
)

// Glass over the day skies: ink text, indigo accents, deep gold for "good", brick for warnings.
private val GlassDayColors = lightColorScheme(
    primary = Color(0xFF3B4FB8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE1FF),
    onPrimaryContainer = Color(0xFF101A55),
    secondary = Color(0xFF5D5A85),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4E1FA),
    onSecondaryContainer = Color(0xFF1C1A3B),
    tertiary = Color(0xFF946300),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE7B3),
    onTertiaryContainer = Color(0xFF3A2800),
    background = Color(0xFFF1F2F8),
    onBackground = Color(0xFF15162A),
    surface = Color(0xFFFAFAFD),
    onSurface = Color(0xFF15162A),
    surfaceVariant = Color(0xFFEDEEF6),
    onSurfaceVariant = Color(0xFF52546B),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F7FC),
    surfaceContainer = Color(0xFFF4F4FA),
    surfaceContainerHigh = Color(0xFFF0F1F8),
    surfaceContainerHighest = Color(0xFFEAEBF4),
    inverseSurface = Color(0xFF2A2C44),
    inverseOnSurface = Color(0xFFF1F2FA),
    inversePrimary = Color(0xFFB9C8FF),
    outline = Color(0xFF7C7E94),
    outlineVariant = Color(0xFF14142A).copy(alpha = 0.08f),
    error = Color(0xFFC0392B),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD3),
    onErrorContainer = Color(0xFF410A04),
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

    // Eased over seconds in real use; near-instant while the developer tools play or scrub the day.
    val previewing by viewModel.previewing.collectAsStateWithLifecycle()
    val easeMillis = if (previewing) 90 else 3_000
    val ease = tween<Color>(durationMillis = easeMillis)
    val easeF = tween<Float>(durationMillis = easeMillis)
    val base by animateColorAsState(sky.base, ease, label = "skyBase")
    val bloom0 by animateColorAsState(sky.blooms[0], ease, label = "bloom0")
    val bloom1 by animateColorAsState(sky.blooms[1], ease, label = "bloom1")
    val bloom2 by animateColorAsState(sky.blooms[2], ease, label = "bloom2")
    val bloomAlpha by animateFloatAsState(sky.bloomAlpha, easeF, label = "bloomAlpha")
    val stars by animateFloatAsState(sky.stars, easeF, label = "stars")
    val moon by animateFloatAsState(sky.moon, easeF, label = "moon")
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
            // Deeper at the top, as a real sky is: darker by night, and by day toward the high
            // bloom's blue, so a day sky has depth instead of fading to a flat white.
            val zenith = if (sky.dark) lerp(base, Color.Black, 0.18f) else lerp(base, bloom0, 0.55f)
            drawRect(Brush.verticalGradient(listOf(zenith, base, lerp(base, Color.White, 0.04f))))
            // Blooms: one high on the far side, one mid-left, and the horizon glow — wide and low
            // across the whole bottom edge, so dawn and dusk warm the horizon rather than a corner.
            // The upper two drift a little with the sun, so the sky never sits quite still.
            drawBloom(bloom0, bloomAlpha, Offset(size.width * (0.92f - sunArc * 0.25f), size.height * 0.06f), size.width * 0.85f)
            drawBloom(bloom1, bloomAlpha, Offset(size.width * (0.02f + sunArc * 0.12f), size.height * 0.50f), size.width * 0.80f)
            drawHorizon(bloom2, bloomAlpha)
            drawStars(stars)
            drawMoon(moon)
            drawSun(sun, sunArc, sky.dark)
        }
        MaterialTheme(colorScheme = celestialAccent(if (sky.dark) GlassNightColors else GlassDayColors, sky)) {
            // The screens sit on transparent surfaces, which pass no content colour down; without
            // this, icons (the day switcher's arrows) keep the default black into the night.
            CompositionLocalProvider(
                LocalGlassStyle provides if (sky.dark) DarkGlass else LightGlass,
                LocalCelestialLight provides celestialLight(sky),
                LocalContentColor provides MaterialTheme.colorScheme.onBackground,
            ) {
                content()
            }
        }
    }
}

/** The two colours a light in the sky is drawn with: its bright centre and the glow at its rim. */
data class CelestialLight(val core: Color, val rim: Color) {
    /**
     * The colour the light *looks*: mostly its bright centre with a touch of its rim. The rim alone
     * is far more saturated than the sun ever reads in the sky.
     */
    val seen: Color get() = lerp(core, rim, 0.4f)

}

private val DaySun = CelestialLight(core = Color(0xFFFFF4D6), rim = Color(0xFFFFE08A))
private val SettingSun = CelestialLight(core = Color(0xFFFFC27A), rim = Color(0xFFFF9A5C))
private val Moonlight = CelestialLight(core = Color(0xFFF8F7F8), rim = Color(0xFFE4E3EC))

/**
 * The light the compass wears once aligned, under Glass; null under every other theme. Two lights,
 * never more: while the sun is in the sky, the low sun's apricot as the sky draws it at first light
 * (the look picked from 6 am — the pale midday sun made the needle unreadable); otherwise the moon's
 * white.
 */
val LocalCelestialLight = staticCompositionLocalOf<CelestialLight?> { null }

private fun celestialLight(sky: SkyFrame): CelestialLight = if (sky.sun > 0.01f) SettingSun else Moonlight

/**
 * The "good" accent (the compass once aligned, its marker and pill, and the chips) is that same
 * light, so every one of them is the same apricot as the needle, with dark text on it.
 */
private fun celestialAccent(scheme: ColorScheme, sky: SkyFrame): ColorScheme {
    val light = celestialLight(sky)
    val ink = if (light == Moonlight) Color(0xFF1A2150) else Color(0xFF4A2600)
    return scheme.copy(
        tertiary = light.seen,
        onTertiary = ink,
        tertiaryContainer = if (sky.dark) light.rim.copy(alpha = 0.30f) else light.core,
        onTertiaryContainer = if (sky.dark) light.core else ink,
    )
}

/** A soft pool of light: the colour at the centre fading to nothing at [radius]. */
private fun DrawScope.drawBloom(color: Color, alpha: Float, center: Offset, radius: Float) {
    // Four stops, not three: a long, even falloff, so a bloom has no visible rim.
    drawCircle(
        brush = Brush.radialGradient(
            0f to color.copy(alpha = alpha),
            0.35f to color.copy(alpha = alpha * 0.6f),
            0.7f to color.copy(alpha = alpha * 0.18f),
            1f to Color.Transparent,
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

/** A glow along the bottom edge, strongest at the edge and gone by a third of the way up. */
private fun DrawScope.drawHorizon(color: Color, alpha: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            0.62f to Color.Transparent,
            0.85f to color.copy(alpha = alpha * 0.45f),
            1f to color.copy(alpha = alpha * 0.8f),
        ),
    )
}

/**
 * The sun climbs from one side at sunrise, peaks at midday and sets on the other side. A crisp disc
 * with a soft corona round it — a blurred blob alone reads as a stain, not a sun. Low in the sky
 * it turns amber.
 */
private fun DrawScope.drawSun(alpha: Float, arc: Float, dusk: Boolean) {
    if (alpha <= 0.01f) return
    val center = Offset(
        x = size.width * (0.14f + arc * 0.72f),
        y = size.height * (0.42f - sin(arc * PI).toFloat() * 0.32f),
    )
    val disc = size.width * 0.055f
    val light = if (dusk) SettingSun else DaySun
    val core = light.core
    val rim = light.rim
    // Corona: wide and faint, then a tighter glow hugging the disc.
    drawCircle(
        brush = Brush.radialGradient(
            listOf(rim.copy(alpha = alpha * 0.28f), Color.Transparent),
            center = center,
            radius = disc * 6f,
        ),
        radius = disc * 6f,
        center = center,
    )
    drawCircle(
        brush = Brush.radialGradient(
            listOf(core.copy(alpha = alpha * 0.55f), rim.copy(alpha = alpha * 0.25f), Color.Transparent),
            center = center,
            radius = disc * 2.2f,
        ),
        radius = disc * 2.2f,
        center = center,
    )
    drawCircle(
        brush = Brush.radialGradient(listOf(Color.White.copy(alpha = alpha), core.copy(alpha = alpha)), center, disc),
        radius = disc,
        center = center,
    )
}

/**
 * A crescent high on the night side, chosen from a gallery of drafts (C3): a medium crescent with a
 * clean outer edge, the inner edge (the terminator) softened just slightly, and a soft light that
 * follows the crescent's own shape — not a round glow, which would outline the dark side and give
 * the full circle away. Its white carries a light touch of warmth. Softness is kept to the
 * terminator and that glow on purpose — a blurred outline reads as low resolution.
 */
private fun DrawScope.drawMoon(alpha: Float) {
    if (alpha <= 0.01f) return
    val center = Offset(size.width * 0.78f, size.height * 0.13f)
    val radius = size.width * 0.04f

    val shadow = center + Offset(-radius * 0.38f, -radius * 0.18f)

    // A faint wide halo, eased over many stops so it does not band, then the crescent's own light:
    // the crescent shape itself, blurred, under the moon. (A blur mask needs Android 9+ with
    // hardware drawing; older phones simply show the moon without that inner glow.)
    drawGlow(Color(0xFFE4E3EC), alpha * 0.10f, center, radius * 0.8f, radius * 6f)
    val crescent = android.graphics.Path().apply {
        addCircle(center.x, center.y, radius, android.graphics.Path.Direction.CW)
        op(
            android.graphics.Path().apply {
                addCircle(shadow.x, shadow.y, radius * 0.92f, android.graphics.Path.Direction.CW)
            },
            android.graphics.Path.Op.DIFFERENCE,
        )
    }
    val crescentGlow = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = MoonFace.copy(alpha = alpha * 0.45f).toArgb()
        maskFilter = android.graphics.BlurMaskFilter(radius * 0.3f, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }
    drawContext.canvas.nativeCanvas.drawPath(crescent, crescentGlow)

    // The disc, brightest toward its lit limb; then the shadow side erased from it with a 4%
    // feather, so whatever sky lies behind shows through the dark side.
    drawContext.canvas.saveLayer(Rect(center, radius * 2f), Paint())
    drawCircle(
        brush = Brush.radialGradient(
            0f to lerp(MoonFace, Color.White, 0.5f).copy(alpha = alpha),
            0.6f to MoonFace.copy(alpha = alpha),
            1f to MoonFace.copy(alpha = alpha),
            center = center + Offset(radius * 0.55f, radius * 0.1f),
            radius = radius * 1.3f,
        ),
        radius = radius,
        center = center,
    )
    val shadowRadius = radius * 0.92f * 1.04f
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color.Black,
            (1f / 1.04f) to Color.Black,
            1f to Color.Transparent,
            center = shadow,
            radius = shadowRadius,
        ),
        radius = shadowRadius,
        center = shadow,
        blendMode = BlendMode.DstOut,
    )
    drawContext.canvas.restore()
}

/** The moon's white: a cool white with a light touch of warmth. */
private val MoonFace = Color(0xFFF8F7F8)

private fun DrawScope.drawGlow(color: Color, alpha: Float, center: Offset, from: Float, to: Float) {
    val start = from / to
    val stops = Array(13) { i ->
        val t = i / 12f
        (start + (1f - start) * t) to color.copy(alpha = alpha * (1f - t).pow(2.2f))
    }
    drawCircle(
        brush = Brush.radialGradient(*stops, center = center, radius = to),
        radius = to,
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
