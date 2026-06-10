package com.noamtu.jewishday.model

import java.lang.Math.toDegrees
import java.lang.Math.toRadians
import java.time.ZoneId
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_KM = 6371.0088

// Approximate Holy of Holies / Foundation Stone location. The precise historical point has
// small uncertainty, which matters only when the user is already on or very near Har HaBayit.
val kodeshHakodashimLocation = JewishLocation(
    name = "Kodesh HaKodashim",
    latitude = 31.7781,
    longitude = 35.2354,
    elevationMeters = 740.0,
    zoneId = ZoneId.of("Asia/Jerusalem"),
)

data class MizrachInfo(
    val fromLocationName: String,
    val fromLatitude: Double,
    val fromLongitude: Double,
    val fromElevationMeters: Double,
    val bearingDegreesExact: Float,
    val bearingDegrees: Int,
    val distanceKm: Int,
    val distanceMeters: Double,
)

fun mizrachInfo(from: JewishLocation = defaultJerusalemLocation): MizrachInfo {
    val fromLatitude = toRadians(from.latitude)
    val toLatitude = toRadians(kodeshHakodashimLocation.latitude)
    val longitudeDelta = toRadians(kodeshHakodashimLocation.longitude - from.longitude)

    val y = sin(longitudeDelta) * cos(toLatitude)
    val x = cos(fromLatitude) * sin(toLatitude) -
        sin(fromLatitude) * cos(toLatitude) * cos(longitudeDelta)
    val bearing = normalizeDegrees(toDegrees(atan2(y, x)))

    val distanceKm = haversineDistanceKm(
        from.latitude,
        from.longitude,
        kodeshHakodashimLocation.latitude,
        kodeshHakodashimLocation.longitude,
    )

    return MizrachInfo(
        fromLocationName = from.name,
        fromLatitude = from.latitude,
        fromLongitude = from.longitude,
        fromElevationMeters = from.elevationMeters,
        bearingDegreesExact = bearing.toFloat(),
        bearingDegrees = normalizeDegrees(bearing.roundToInt()),
        distanceKm = distanceKm.roundToInt(),
        distanceMeters = distanceKm * 1_000.0,
    )
}

private fun normalizeDegrees(degrees: Double): Double = ((degrees % 360.0) + 360.0) % 360.0

private fun normalizeDegrees(degrees: Int): Int = ((degrees % 360) + 360) % 360

private fun haversineDistanceKm(
    fromLatitude: Double,
    fromLongitude: Double,
    toLatitude: Double,
    toLongitude: Double,
): Double {
    val latitudeDelta = toRadians(toLatitude - fromLatitude)
    val longitudeDelta = toRadians(toLongitude - fromLongitude)
    val fromLatitudeRadians = toRadians(fromLatitude)
    val toLatitudeRadians = toRadians(toLatitude)

    val a = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
        cos(fromLatitudeRadians) * cos(toLatitudeRadians) *
        sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return EARTH_RADIUS_KM * c
}
