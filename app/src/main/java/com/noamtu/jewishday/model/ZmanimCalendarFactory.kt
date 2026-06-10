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
        location.elevationMeters,
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
