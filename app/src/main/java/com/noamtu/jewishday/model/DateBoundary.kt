// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun nextGregorianMidnight(location: JewishLocation, now: Instant): Instant =
    now.atZone(location.zoneId)
        .toLocalDate()
        .plusDays(1)
        .atStartOfDay(location.zoneId)
        .plusMinutes(1)
        .toInstant()

/**
 * The next tzeit hakochavim (plus a minute), the instant at which the Hebrew date rolls over.
 *
 * Not sunset: sunset only opens bein hashmashot, the doubtful stretch between the days. The date is
 * the next one once the stars are out.
 */
fun nextTzeit(
    location: JewishLocation,
    settings: ZmanimCalculationSettings,
    now: Instant,
): Instant {
    val localToday = now.atZone(location.zoneId).toLocalDate()
    return listOf(localToday, localToday.plusDays(1), localToday.plusDays(2))
        .mapNotNull { date -> tzeitForDate(location, date, settings)?.plus(1, ChronoUnit.MINUTES) }
        .firstOrNull { it.isAfter(now) }
        ?: nextGregorianMidnight(location, now)
}

/**
 * Chatzot halaila of the night that begins on [date] — solar midnight, a little either side of
 * civil midnight. This, not midnight, is where the displayed *day* turns over: the zmanim list,
 * the weekday and the civil date all stay on [date] until it passes.
 */
fun chatzotHaLailaForDate(
    location: JewishLocation = defaultJerusalemLocation,
    date: LocalDate,
    settings: ZmanimCalculationSettings = ZmanimCalculationSettings(),
): Instant? = complexZmanimCalendar(location, date, settings)
    .chatzotHaLaila(settings.chatzotHaLailaMethod)
    ?.toInstant()

/**
 * The civil date the displayed zmanim belong to. The night belongs to the day it started on, so
 * the list only rolls forward at chatzot halaila: between civil midnight and chatzot you are still
 * shown the previous day's times. (The Hebrew *date* is separate — it rolls at sunset.)
 *
 * Solar midnight can land either side of civil midnight depending on longitude and DST, so both
 * neighbouring nights are checked rather than assuming it falls after midnight.
 */
fun zmanimDateFor(
    location: JewishLocation,
    settings: ZmanimCalculationSettings,
    now: Instant,
): LocalDate {
    val civilDate = now.atZone(location.zoneId).toLocalDate()
    val tonightsChatzot = chatzotHaLailaForDate(location, civilDate, settings)
    if (tonightsChatzot != null && !now.isBefore(tonightsChatzot)) return civilDate.plusDays(1)
    val lastNightsChatzot = chatzotHaLailaForDate(location, civilDate.minusDays(1), settings)
    if (lastNightsChatzot != null && now.isBefore(lastNightsChatzot)) return civilDate.minusDays(1)
    return civilDate
}

/** The next chatzot halaila (plus a minute), the instant at which the displayed day rolls over. */
fun nextChatzotHaLaila(
    location: JewishLocation,
    settings: ZmanimCalculationSettings,
    now: Instant,
): Instant {
    val civilDate = now.atZone(location.zoneId).toLocalDate()
    return listOf(civilDate.minusDays(1), civilDate, civilDate.plusDays(1))
        .mapNotNull { date -> chatzotHaLailaForDate(location, date, settings)?.plus(1, ChronoUnit.MINUTES) }
        .firstOrNull { it.isAfter(now) }
        ?: nextGregorianMidnight(location, now)
}

/**
 * The next instant the status-bar icon's text changes: midnight for the weekday it shows, tzeit
 * for the Hebrew date. Deliberately not [nextDateBoundary] — the icon does not show the zmanim day,
 * so waking it at chatzot halaila would be a nightly wake-up that changes nothing on screen.
 */
fun nextStatusIconBoundary(
    location: JewishLocation,
    settings: ZmanimCalculationSettings,
    now: Instant,
): Instant = minOf(
    nextGregorianMidnight(location, now),
    nextTzeit(location, settings, now),
)

/** The next instant at which either the displayed day or the Hebrew date changes. */
fun nextDateBoundary(
    location: JewishLocation,
    settings: ZmanimCalculationSettings,
    now: Instant,
): Instant = minOf(
    nextChatzotHaLaila(location, settings, now),
    nextTzeit(location, settings, now),
)

fun nextWeeklyParshaBoundary(
    location: JewishLocation,
    settings: ZmanimCalculationSettings,
    now: Instant,
): Instant {
    val localToday = now.atZone(location.zoneId).toLocalDate()
    return (0..7)
        .map { localToday.plusDays(it.toLong()) }
        .filter { it.dayOfWeek == DayOfWeek.SATURDAY }
        // The parsha rolls when Shabbat is actually out, tosefet included — not at bare motzei.
        .mapNotNull { date -> holyDayExitForDate(location, date, settings)?.plus(1, ChronoUnit.MINUTES) }
        .firstOrNull { it.isAfter(now) }
        ?: nextDateBoundary(location, settings, now)
}

/**
 * The next moment the date header changes state: a fast or a holy day coming in or going out.
 *
 * The header's name appears and disappears on exactly these instants, and not one of them lands on
 * sunset or midnight — candle lighting is minutes before sunset, alot is hours before it, and both
 * exits fall between sunset and midnight. Without them a screen left open would keep showing the
 * previous state for hours: no name at candle lighting on a Friday evening, or a finished fast's
 * name lingering until midnight.
 *
 * A second's cushion puts the tick just past the boundary, so the recomputation sees the new state.
 */
fun nextObservanceBoundary(
    location: JewishLocation,
    settings: ZmanimCalculationSettings,
    now: Instant,
): Instant? {
    val localToday = now.atZone(location.zoneId).toLocalDate()
    return (0..2)
        .map { localToday.plusDays(it.toLong()) }
        .flatMap { date ->
            val calendar = complexZmanimCalendar(location, date, settings)
            listOfNotNull(
                calendar.candleLighting?.toInstant(), // a holy day comes in
                calendar.alotHashachar(settings)?.toInstant(), // a dawn fast begins
                calendar.tzeit(settings)?.toInstant(), // a fast ends
                calendar.holyDayExit(settings)?.toInstant(), // a holy day goes out
            )
        }
        .map { it.plusSeconds(1) }
        .filter { it.isAfter(now) }
        .minOrNull()
}

fun nextZmanimRefreshBoundary(
    location: JewishLocation,
    settings: ZmanimCalculationSettings,
    now: Instant,
): Instant {
    val dateAndParsha = minOf(
        nextDateBoundary(location, settings, now),
        nextWeeklyParshaBoundary(location, settings, now),
    )
    val observance = nextObservanceBoundary(location, settings, now) ?: return dateAndParsha
    return minOf(dateAndParsha, observance)
}

/**
 * Emits immediately and then again whenever the displayed date changes (tzeit for the Hebrew
 * date, chatzot halaila for the day itself), so date-bound UI state recomputes while visible.
 */
fun dateBoundaryTicker(
    clock: Clock,
    location: JewishLocation,
    settings: ZmanimCalculationSettings,
): Flow<Instant> = flow {
    while (true) {
        val now = clock.instant()
        emit(now)
        val next = nextZmanimRefreshBoundary(location, settings, now)
        delay(Duration.between(now, next).toMillis().coerceAtLeast(MinimumTickMillis))
    }
}

private const val MinimumTickMillis = 1_000L