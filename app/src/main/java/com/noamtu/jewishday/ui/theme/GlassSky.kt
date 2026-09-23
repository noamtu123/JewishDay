// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.noamtu.jewishday.model.SkyDay
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * One moment of the Glass sky: the base colour, three soft blooms of light, how much of the stars
 * and the sun show, where the sun is along its arc, and whether the glass over it is the dark kind
 * (light text) or the light kind (dark text).
 */
data class SkyFrame(
    val base: Color,
    val blooms: List<Color>,
    val bloomAlpha: Float,
    val stars: Float,
    val sun: Float,
    /** 0 at sunrise, 1 at sunset — where the sun sits on its arc. */
    val sunArc: Float,
    val dark: Boolean,
)

private data class Look(
    val base: Long,
    val blooms: List<Long>,
    val bloomAlpha: Float,
    val stars: Float,
    val sun: Float,
    val dark: Boolean,
)

// The palettes from the approved mockup: Aurora by night, Frost by day, the sunset sky between.
private val Night = Look(0xFF070B1E, listOf(0xFF3B2F8F, 0xFF1B3B8A, 0xFF0F5C73), 0.55f, 1f, 0f, dark = true)
private val Dawn = Look(0xFF2A2560, listOf(0xFFFF9A7A, 0xFF8A5CFF, 0xFFF5B6C8), 0.70f, 0.3f, 0.3f, dark = true)
private val Sunrise = Look(0xFFF6E7EF, listOf(0xFFFFC2A0, 0xFFC7B8FF, 0xFFFFE1B0), 0.90f, 0f, 0.8f, dark = false)
private val Day = Look(0xFFEEF2FB, listOf(0xFFBCD4FF, 0xFFC9F0E4, 0xFFFFE3C4), 0.90f, 0f, 1f, dark = false)
private val Afternoon = Look(0xFFF5EFE6, listOf(0xFFFFD3A6, 0xFFBCD0FF, 0xFFFFE9C7), 0.90f, 0f, 0.9f, dark = false)
private val Sunset = Look(0xFF3A2A5C, listOf(0xFFFF8A5C, 0xFFB04A8A, 0xFFFFB36B), 0.80f, 0f, 0.7f, dark = true)
private val Twilight = Look(0xFF1A1C48, listOf(0xFF7B5CFF, 0xFFD0607A, 0xFF2F6FB0), 0.65f, 0.5f, 0.15f, dark = true)

/**
 * The sky at [now]. Each look is pinned to one of the day's real moments — dawn halfway from alot
 * to sunrise, full day at midday, the sunset colours at sunset, twilight at tzeit — and the sky
 * blends between neighbours, so it moves the whole time rather than switching on the hour. Moments
 * the sun never reaches (polar days and nights) fall back to fixed clock hours.
 */
fun skyAt(now: Instant, day: SkyDay, zone: ZoneId): SkyFrame {
    val midnight = now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
    fun hour(h: Double) = midnight.plusSeconds((h * 3600).toLong())
    val sunrise = day.sunrise ?: hour(6.5)
    val sunset = day.sunset ?: hour(18.5)
    val alot = day.alot ?: sunrise.minus(Duration.ofMinutes(72))
    val tzeit = day.tzeit ?: sunset.plus(Duration.ofMinutes(20))
    val midday = sunrise.plus(Duration.between(sunrise, sunset).dividedBy(2))

    val keys = listOf(
        midnight to Night,
        alot.minus(Duration.ofMinutes(40)) to Night,
        alot.plus(Duration.between(alot, sunrise).dividedBy(2)) to Dawn,
        sunrise.plus(Duration.ofMinutes(30)) to Sunrise,
        midday to Day,
        sunset.minus(Duration.ofMinutes(60)) to Afternoon,
        sunset to Sunset,
        tzeit to Twilight,
        tzeit.plus(Duration.ofMinutes(60)) to Night,
        midnight.plus(Duration.ofDays(1)) to Night,
    ).sortedBy { it.first }

    val index = keys.indexOfLast { !it.first.isAfter(now) }.coerceIn(0, keys.size - 2)
    val (fromAt, from) = keys[index]
    val (toAt, to) = keys[index + 1]
    val span = Duration.between(fromAt, toAt).toMillis().coerceAtLeast(1)
    val f = (Duration.between(fromAt, now).toMillis().toFloat() / span).coerceIn(0f, 1f)

    val arc = Duration.between(sunrise, now).toMillis().toFloat() /
        Duration.between(sunrise, sunset).toMillis().coerceAtLeast(1)
    return SkyFrame(
        base = lerp(Color(from.base), Color(to.base), f),
        blooms = from.blooms.zip(to.blooms) { a, b -> lerp(Color(a), Color(b), f) },
        bloomAlpha = from.bloomAlpha + (to.bloomAlpha - from.bloomAlpha) * f,
        stars = from.stars + (to.stars - from.stars) * f,
        sun = from.sun + (to.sun - from.sun) * f,
        sunArc = arc.coerceIn(0f, 1f),
        dark = if (f < 0.5f) from.dark else to.dark,
    )
}
