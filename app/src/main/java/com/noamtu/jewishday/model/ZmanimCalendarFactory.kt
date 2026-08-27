// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.util.GeoLocation
import java.time.LocalDate
import java.util.GregorianCalendar
import java.util.TimeZone

internal fun complexZmanimCalendar(
    location: JewishLocation,
    date: LocalDate,
    settings: ZmanimCalculationSettings,
): ComplexZmanimCalendar {
    val timeZone = TimeZone.getTimeZone(location.zoneId)
    val geoLocation = GeoLocation(
        location.name,
        location.latitude,
        location.longitude,
        // KosherJava rejects a negative elevation outright, and GPS reports plenty of them: the
        // Dead Sea sits 430m below sea level, and a fix near the coast dips below zero on noise
        // alone. Treated as sea level, which is what the elevation-free zmanim use anyway.
        location.elevationMeters.coerceAtLeast(0.0),
        timeZone,
    )
    val calculationDate = GregorianCalendar(timeZone).apply {
        clear()
        set(date.year, date.monthValue - 1, date.dayOfMonth)
    }
    return ComplexZmanimCalendar(geoLocation).apply {
        setCalendar(calculationDate)
        candleLightingOffset = settings.candleLightingMethod.offsetMinutes.toDouble()
        ateretTorahSunsetOffset = settings.ateretTorahSunsetOffsetMinutes.toDouble()
    }
}