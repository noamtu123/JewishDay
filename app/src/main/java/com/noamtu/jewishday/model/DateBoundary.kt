package com.noamtu.jewishday.model

import java.time.Clock
import java.time.Duration
import java.time.Instant
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

/** The next instant at which either the Gregorian or the Jewish date changes. */
fun nextDateBoundary(
    location: JewishLocation,
    settings: ZmanimCalculationSettings,
    now: Instant,
): Instant = minOf(
    nextGregorianMidnight(location, now),
    nextTzeit(location, settings, now),
)

/**
 * Emits immediately and then again whenever the displayed date changes (tzeit or
 * midnight in the location's zone), so date-bound UI state recomputes while visible.
 */
fun dateBoundaryTicker(
    clock: Clock,
    location: JewishLocation,
    settings: ZmanimCalculationSettings,
): Flow<Instant> = flow {
    while (true) {
        val now = clock.instant()
        emit(now)
        val next = nextDateBoundary(location, settings, now)
        delay(Duration.between(now, next).toMillis().coerceAtLeast(MinimumTickMillis))
    }
}

private const val MinimumTickMillis = 1_000L
