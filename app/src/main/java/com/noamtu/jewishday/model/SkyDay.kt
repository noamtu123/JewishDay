// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import java.time.Instant
import java.time.LocalDate

/**
 * The four moments the Glass theme's sky is keyed to on one civil day. Any of them can be missing
 * far enough north, where the sun does not reach the angle in question; the sky falls back to
 * clock hours for those.
 */
data class SkyDay(
    val alot: Instant?,
    val sunrise: Instant?,
    val sunset: Instant?,
    val tzeit: Instant?,
)

/** The sky's moments for [date], by the user's own opinions for each, so it agrees with the list. */
fun skyDayFor(
    location: JewishLocation,
    date: LocalDate,
    settings: ZmanimCalculationSettings,
): SkyDay {
    val calendar = complexZmanimCalendar(location, date, settings)
    return SkyDay(
        alot = calendar.alotHashachar(settings)?.toInstant(),
        sunrise = calendar.sunrise(settings.sunriseMethod)?.toInstant(),
        sunset = calendar.sunset(settings.sunsetMethod)?.toInstant(),
        tzeit = calendar.tzeit(settings)?.toInstant(),
    )
}
