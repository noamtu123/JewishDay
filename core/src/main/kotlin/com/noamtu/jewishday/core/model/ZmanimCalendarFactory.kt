// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

import com.kosherjava.zmanim.ComprehensiveZmanimCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.util.GeoLocation
import java.time.LocalDate

internal fun comprehensiveZmanimCalendar(
    location: JewishLocation,
    date: LocalDate,
    settings: ZmanimCalculationSettings,
): ComprehensiveZmanimCalendar {
    val geoLocation = GeoLocation(
        location.name,
        location.latitude,
        location.longitude,
        // KosherJava rejects a negative elevation outright, and GPS reports plenty of them: the
        // Dead Sea sits 430m below sea level, and a fix near the coast dips below zero on noise
        // alone. Treated as sea level, which is what the elevation-free zmanim use anyway.
        location.elevationMeters.coerceAtLeast(0.0),
        location.zoneId,
    )
    return ComprehensiveZmanimCalendar(geoLocation).apply {
        localDate = date
        ateretTorahSunsetOffset = settings.ateretTorahSunsetOffsetMinutes.toDouble()
    }
}

/**
 * The Jewish calendar for [date] as the app reads it everywhere: modern holidays on, and Israel's
 * or the diaspora's Yom Tov days by where the user is ([inIsrael]).
 */
public fun jewishCalendar(date: LocalDate, inIsrael: Boolean): JewishCalendar = JewishCalendar(date).apply {
    isUseModernHolidays = true
    setInIsrael(inIsrael)
}
