// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import com.noamtu.jewishday.model.JewishLocation
import com.noamtu.jewishday.model.ZmanimCalculationSettings
import com.noamtu.jewishday.model.nextGregorianMidnight
import com.noamtu.jewishday.model.nextTzeit
import com.noamtu.jewishday.model.nextWeeklyParshaBoundary
import com.noamtu.jewishday.model.skyDayFor
import com.noamtu.jewishday.model.zmanimForDate
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * When the widget is next to be re-rendered, and whether that moment has to be hit on the dot.
 *
 * [exact] is true when something the widget *says* changes at [at] — the Hebrew date, the calendar
 * day, a holy day or fast coming in or going out — and false when only the sky moves on. The alarm
 * scheduler gives the first an exact alarm and lets the system batch the second.
 */
data class DayWidgetRefresh(val at: Instant, val exact: Boolean)

/**
 * The next moment the widget needs re-rendering, and how precisely: the earlier of the next content
 * boundary and the next sky tick.
 *
 * Two cadences, because the two things that change are not equally worth a wake-up. The content is
 * what the widget is for: showing yesterday's Hebrew date for a quarter of an hour past tzeit, or
 * "Shabbat starts 18:10" once Shabbat has started, because the system batched the alarm, is a wrong
 * widget — so [nextDayWidgetContentBoundary] gets an exact alarm. The sun's place on its arc is
 * cosmetic: nobody can tell whether it moved at 10:15 or 10:23, so the sky ticks are inexact and let
 * the system fold them in with whatever else it wakes for.
 *
 * The sky ticks every quarter hour while the sun is on the move — from forty minutes before alot,
 * when the Glass sky begins to lighten, to an hour after tzeit, when it has settled into night. The
 * night itself is one still frame (the moon and stars stand still and the sun is gone), so it has no
 * ticks of its own: the tick after dusk is the next first light, and midnight's and tzeit's content
 * boundaries are the only night-time wake-ups. Where the sun never reaches one of those angles, the
 * same fallback hours GlassSky draws by stand in, so the widget ticks through the very day the app
 * paints.
 *
 * A content boundary that falls within one tick of the sky tick wins outright rather than losing on
 * time alone. Tzeit's boundary sits a minute after tzeit, so a strict "earlier wins" would hand
 * exactly the boundary that matters most to an inexact alarm; skipping the one sky tick in between
 * moves nothing anyone can see, and the exact alarm re-renders the sky anyway.
 *
 * The result is never sooner than a minute from [now]: an alarm that fires on the heels of the
 * refresh that armed it would only loop, and nothing on the widget changes within the minute.
 */
fun nextDayWidgetRefresh(
    location: JewishLocation,
    settings: ZmanimCalculationSettings,
    now: Instant,
): DayWidgetRefresh {
    val earliest = now.plus(MinimumLead)
    val content = maxOf(nextDayWidgetContentBoundary(location, settings, now), earliest)
    val tick = maxOf(nextSkyTick(location, settings, earliest), earliest)
    return if (!content.isAfter(tick.plus(DayStep))) {
        DayWidgetRefresh(content, exact = true)
    } else {
        DayWidgetRefresh(tick, exact = false)
    }
}

/**
 * The next instant something the widget *says* changes — the widget's own moments, not the zmanim
 * screen's, whose nextZmanimRefreshBoundary adds alot, candle lighting, chatzot halaila and every
 * day's holy-day exit, none of which the widget shows on an ordinary day: midnight, when the civil
 * date, the times, the events and the learning turn over; tzeit, when the Hebrew date and weekday
 * roll and the header announces what is coming; the parasha's roll once Shabbat is out; and the
 * actual entry and exit of whatever holy day or fast the day's computation announces, the moments
 * the badge and the entry/exit lines appear and disappear on. A second past each, so the
 * recomputation sees the new state.
 */
fun nextDayWidgetContentBoundary(
    location: JewishLocation,
    settings: ZmanimCalculationSettings,
    now: Instant,
): Instant {
    val today = now.atZone(location.zoneId).toLocalDate()
    val observances = listOf(today, today.plusDays(1))
        .map { date -> zmanimForDate(location, date, settings, now) }
        .flatMap { day ->
            listOfNotNull(
                day.holyDayInfo?.startTime,
                day.holyDayInfo?.endTime,
                day.fastDayInfo?.startTime,
                day.fastDayInfo?.endTime,
            )
        }
        .map { it.plusSeconds(1) }
    val rolls = listOf(
        nextGregorianMidnight(location, now),
        nextTzeit(location, settings, now),
        nextWeeklyParshaBoundary(location, settings, now),
    )
    return (rolls + observances).filter { it.isAfter(now) }.min()
}

/**
 * The next sky tick at or after [earliest]: quarter-hourly while the sun moves, otherwise the next
 * first light — today's while the night before dawn is still on, tomorrow's once dusk has settled.
 */
private fun nextSkyTick(
    location: JewishLocation,
    settings: ZmanimCalculationSettings,
    earliest: Instant,
): Instant {
    val zone = location.zoneId
    val today = earliest.atZone(zone).toLocalDate()
    val (firstLight, settledNight) = skyMovesBetween(location, settings, today)
    return when {
        earliest.isBefore(firstLight) -> firstLight
        !earliest.isAfter(settledNight) -> nextMultiple(earliest, zone, DayStep)
        else -> skyMovesBetween(location, settings, today.plusDays(1)).first
    }
}

/**
 * The stretch of [date] the sky moves in: from forty minutes before alot to an hour after tzeit.
 * GlassSky's stand-ins for a sun that never reaches the angle (polar day and night) are kept in step
 * with skyKeys there, so the widget ticks through the same day the app draws.
 */
private fun skyMovesBetween(
    location: JewishLocation,
    settings: ZmanimCalculationSettings,
    date: LocalDate,
): Pair<Instant, Instant> {
    val midnight = date.atStartOfDay(location.zoneId).toInstant()
    val day = skyDayFor(location, date, settings)
    val sunrise = day.sunrise ?: midnight.plus(FallbackSunrise)
    val sunset = day.sunset ?: midnight.plus(FallbackSunset)
    val alot = day.alot ?: sunrise.minus(FallbackAlotBeforeSunrise)
    val tzeit = day.tzeit ?: sunset.plus(FallbackTzeitAfterSunset)
    return alot.minus(SkyLightensBeforeAlot) to tzeit.plus(SkySettlesAfterTzeit)
}

/** The first local wall-clock multiple of [step] (a divisor of an hour) at or after [from]. */
private fun nextMultiple(from: Instant, zone: ZoneId, step: Duration): Instant {
    val local = from.atZone(zone)
    val stepMinutes = step.toMinutes()
    val floor = local.truncatedTo(ChronoUnit.HOURS).plusMinutes(local.minute / stepMinutes * stepMinutes)
    val floorInstant = floor.toInstant()
    return if (floorInstant.isBefore(from)) floor.plusMinutes(stepMinutes).toInstant() else floorInstant
}

private val MinimumLead: Duration = Duration.ofSeconds(60)
private val DayStep: Duration = Duration.ofMinutes(15)

// The sky's Night look holds until forty minutes before alot and returns an hour after tzeit.
private val SkyLightensBeforeAlot: Duration = Duration.ofMinutes(40)
private val SkySettlesAfterTzeit: Duration = Duration.ofMinutes(60)

private val FallbackSunrise: Duration = Duration.ofMinutes(6 * 60 + 30)
private val FallbackSunset: Duration = Duration.ofMinutes(18 * 60 + 30)
private val FallbackAlotBeforeSunrise: Duration = Duration.ofMinutes(72)
private val FallbackTzeitAfterSunset: Duration = Duration.ofMinutes(20)
