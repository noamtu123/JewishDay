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
    val moon: Float,
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
    /**
     * Only full night has the moon. It fades out on the way to dawn before the sun starts, and back
     * in over the hour after tzeit once the sun has gone, so the two never share the sky and
     * neither appears or vanishes at once.
     */
    val moon: Float = 0f,
)

// Blooms are listed high, middle, horizon. Only dawn and dusk warm the horizon; by day it is a
// pale haze, so nothing warm sits in a corner of an otherwise cool sky.
private val Night = Look(0xFF0A0F24, listOf(0xFF3A3290, 0xFF1C3F8F, 0xFF16305E), 0.45f, 1f, 0f, dark = true, moon = 1f)
private val Dawn = Look(0xFF1E2150, listOf(0xFF7A5CE0, 0xFFB0609A, 0xFFF2A07E), 0.55f, 0.35f, 0f, dark = true)
private val Sunrise = Look(0xFFE9E4F5, listOf(0xFFA9C1F5, 0xFFF1C9DD, 0xFFFFCFAE), 0.80f, 0f, 0.8f, dark = false)
private val Day = Look(0xFFD6E4FA, listOf(0xFF8DB3F2, 0xFFBFE0F2, 0xFFEEF4FF), 0.75f, 0f, 1f, dark = false)
private val Afternoon = Look(0xFFDDE3F6, listOf(0xFF9FBDF2, 0xFFE3D3F2, 0xFFFBDCC2), 0.75f, 0f, 0.9f, dark = false)
private val Sunset = Look(0xFF2A1F4E, listOf(0xFF6E47B0, 0xFFD0578A, 0xFFFF7E6B), 0.75f, 0f, 0.7f, dark = true)
private val Twilight = Look(0xFF151A40, listOf(0xFF5B4BC8, 0xFF8A4C82, 0xFF6A4A8E), 0.55f, 0.55f, 0f, dark = true)

/** One of the sky's pinned moments, for the developer tools' "jump to" buttons. */
data class SkyPhase(val name: String, val nameHebrew: String, val at: Instant)

private data class SkyKey(val at: Instant, val look: Look, val name: String?, val nameHebrew: String?)

/**
 * The day's pinned moments: each look sits at one of the day's real times — dawn halfway from alot
 * to sunrise, full day at midday, the sunset colours at sunset, twilight at tzeit. Moments the sun
 * never reaches (polar days and nights) fall back to fixed clock hours.
 */
private fun skyKeys(day: SkyDay, midnight: Instant): List<SkyKey> {
    fun hour(h: Double) = midnight.plusSeconds((h * 3600).toLong())
    val sunrise = day.sunrise ?: hour(6.5)
    val sunset = day.sunset ?: hour(18.5)
    val alot = day.alot ?: sunrise.minus(Duration.ofMinutes(72))
    val tzeit = day.tzeit ?: sunset.plus(Duration.ofMinutes(20))
    val midday = sunrise.plus(Duration.between(sunrise, sunset).dividedBy(2))
    return listOf(
        SkyKey(midnight, Night, null, null),
        SkyKey(alot.minus(Duration.ofMinutes(40)), Night, "Night", "לילה"),
        SkyKey(alot.plus(Duration.between(alot, sunrise).dividedBy(2)), Dawn, "Dawn", "שחר"),
        SkyKey(sunrise.plus(Duration.ofMinutes(30)), Sunrise, "Sunrise", "זריחה"),
        SkyKey(midday, Day, "Midday", "צהריים"),
        SkyKey(sunset.minus(Duration.ofMinutes(60)), Afternoon, "Afternoon", "אחר הצהריים"),
        SkyKey(sunset, Sunset, "Sunset", "שקיעה"),
        SkyKey(tzeit, Twilight, "Bein hashmashot", "בין השמשות"),
        SkyKey(tzeit.plus(Duration.ofMinutes(60)), Night, "Night", "לילה"),
        SkyKey(midnight.plus(Duration.ofDays(1)), Night, null, null),
    ).sortedBy { it.at }
}

/** The named moments of the day, in order — where each look is at its fullest. */
fun skyPhases(day: SkyDay, date: java.time.LocalDate, zone: ZoneId): List<SkyPhase> =
    skyKeys(day, date.atStartOfDay(zone).toInstant())
        .filter { it.name != null }
        .map { SkyPhase(it.name!!, it.nameHebrew!!, it.at) }

/**
 * The sky at [now]: blended between the two pinned moments (see [skyKeys]) either side of it, so it
 * moves the whole time rather than switching on the hour.
 */
fun skyAt(now: Instant, day: SkyDay, zone: ZoneId): SkyFrame {
    val midnight = now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
    val keys = skyKeys(day, midnight)
    val sunrise = day.sunrise ?: midnight.plusSeconds(6 * 3600 + 1800)
    val sunset = day.sunset ?: midnight.plusSeconds(18 * 3600 + 1800)

    val index = keys.indexOfLast { !it.at.isAfter(now) }.coerceIn(0, keys.size - 2)
    val fromAt = keys[index].at
    val from = keys[index].look
    val toAt = keys[index + 1].at
    val to = keys[index + 1].look
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
        moon = from.moon + (to.moon - from.moon) * f,
        sunArc = arc.coerceIn(0f, 1f),
        dark = if (f < 0.5f) from.dark else to.dark,
    )
}
