// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

public fun nextGregorianMidnight(location: JewishLocation, now: Instant): Instant =
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
internal fun nextTzeit(
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
 * civil midnight.
 */
public fun chatzotHaLailaForDate(
    location: JewishLocation = defaultJerusalemLocation,
    date: LocalDate,
    settings: ZmanimCalculationSettings = ZmanimCalculationSettings(),
): Instant? = comprehensiveZmanimCalendar(location, date, settings)
    .chatzotHaLaila(settings.chatzotHaLailaMethod)

/**
 * The civil date whose daytime belongs to the Jewish day now in progress — the civil date of the
 * Hebrew date to show. It rolls at tzeit: from nightfall on Thursday the Hebrew date is Friday's.
 * The zmanim list does not follow it; the times stay on the calendar day and turn over at midnight.
 */
public fun jewishDayCivilDate(
    location: JewishLocation,
    settings: ZmanimCalculationSettings,
    now: Instant,
): LocalDate {
    val civilDate = now.atZone(location.zoneId).toLocalDate()
    val tzeit = tzeitForDate(location, civilDate, settings)
    return if (tzeit != null && !now.isBefore(tzeit)) civilDate.plusDays(1) else civilDate
}

/**
 * How many days the zmanim screen steps from today to show [date]. Steps count from the Hebrew day
 * today's screen shows ([jewishDayCivilDate]), so after tzeit Shabbat is already 0; a day that has
 * begun or passed is 0 too, today itself, shown live.
 */
public fun dayOffsetTo(
    date: LocalDate,
    location: JewishLocation,
    settings: ZmanimCalculationSettings,
    now: Instant,
): Int = ChronoUnit.DAYS.between(jewishDayCivilDate(location, settings, now), date).coerceAtLeast(0).toInt()

/**
 * The next chatzot halaila after [now] — the midnight of the night in progress, or the coming one
 * once it has passed. Solar midnight can land either side of civil midnight depending on longitude
 * and DST, so both neighbouring nights are considered.
 */
public fun upcomingChatzotHaLaila(
    location: JewishLocation,
    settings: ZmanimCalculationSettings,
    now: Instant,
): Instant? {
    val civilDate = now.atZone(location.zoneId).toLocalDate()
    return listOf(civilDate.minusDays(1), civilDate, civilDate.plusDays(1))
        .mapNotNull { date -> chatzotHaLailaForDate(location, date, settings) }
        .firstOrNull { it.isAfter(now) }
}

/** The next chatzot halaila (plus a minute), the instant the "chatzot halaila" row flips nights. */
internal fun nextChatzotHaLaila(
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
public fun nextStatusIconBoundary(
    location: JewishLocation,
    settings: ZmanimCalculationSettings,
    now: Instant,
): Instant = minOf(
    nextGregorianMidnight(location, now),
    nextTzeit(location, settings, now),
)

/**
 * The next instant the zmanim screen needs recomputing for the date alone: midnight, when the
 * calendar day and every time on the list turn over; tzeit, when the Hebrew date and weekday roll;
 * and chatzot halaila, when the "next chatzot" row flips to the following night.
 */
public fun nextDateBoundary(
    location: JewishLocation,
    settings: ZmanimCalculationSettings,
    now: Instant,
): Instant = minOf(
    nextGregorianMidnight(location, now),
    minOf(nextChatzotHaLaila(location, settings, now), nextTzeit(location, settings, now)),
)

public fun nextWeeklyParshaBoundary(
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
 * The header's name appears and disappears on exactly these instants, and only Tisha B'Av's lands on
 * sunset: candle lighting is minutes before sunset, alot is hours before it, and both exits fall
 * after sunset. Without them a screen left open would keep showing the
 * previous state for hours: no name at candle lighting on a Friday evening, or a finished fast's
 * name lingering until midnight.
 *
 * A second's cushion puts the tick just past the boundary, so the recomputation sees the new state.
 */
internal fun nextObservanceBoundary(
    location: JewishLocation,
    settings: ZmanimCalculationSettings,
    now: Instant,
): Instant? {
    val localToday = now.atZone(location.zoneId).toLocalDate()
    val inIsrael = location.isInIsrael
    // From yesterday: far enough north a summer fast ends (and Shabbat goes out) after midnight.
    return (-1..2)
        .map { localToday.plusDays(it.toLong()) }
        .flatMap { date ->
            val calendar = comprehensiveZmanimCalendar(location, date, settings)
            listOfNotNull(
                calendar.candleLighting(settings)?.time, // a holy day comes in
                calendar.alotHashachar(settings), // a dawn fast begins
                // Tisha B'Av begins at the sunset before its day.
                calendar.sunset(settings.sunsetMethod)
                    ?.takeIf { jewishCalendar(date.plusDays(1), inIsrael).yomTovIndex == JewishCalendar.TISHA_BEAV },
                calendar.tzeit(settings), // a fast ends
                calendar.holyDayExit(settings), // a holy day goes out
            )
        }
        .map { it.plusSeconds(1) }
        .filter { it.isAfter(now) }
        .minOrNull()
}

public fun nextZmanimRefreshBoundary(
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
