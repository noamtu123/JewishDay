// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

import androidx.compose.runtime.Immutable
import java.time.Instant
import java.time.LocalDate

/**
 * The four moments the Glass theme's sky is keyed to on one civil day. Any of them can be missing
 * far enough north, where the sun does not reach the angle in question; the sky falls back to
 * clock hours for those.
 */
@Immutable
public data class SkyDay(
    val alot: Instant?,
    val sunrise: Instant?,
    val sunset: Instant?,
    val tzeit: Instant?,
)

/** The sky's moments for [date], by the user's own opinions for each, so it agrees with the list. */
public fun skyDayFor(
    location: JewishLocation,
    date: LocalDate,
    settings: ZmanimCalculationSettings,
): SkyDay {
    val calendar = comprehensiveZmanimCalendar(location, date, settings)
    return SkyDay(
        // The same for dawn: the darkness ends at last night's solar midnight.
        alot = calendar.ownAlot(settings)
            ?: calendar.lastChatzotHaLaila().takeIf { calendar.sunrise(settings.sunriseMethod) != null },
        sunrise = calendar.sunrise(settings.sunriseMethod),
        sunset = calendar.sunset(settings.sunsetMethod),
        // The sky keys to the dark itself, never to a fixed-minute stand-in: on a night the sun never
        // sinks to the user's angle that is solar midnight, the darkest it gets.
        tzeit = calendar.ownTzeit(settings)
            ?: calendar.chatzotHaLaila(ChatzotMethod.Solar).takeIf { calendar.sunset(settings.sunsetMethod) != null },
    )
}
