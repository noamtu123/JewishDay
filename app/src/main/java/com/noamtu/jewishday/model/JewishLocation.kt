package com.noamtu.jewishday.model

import java.time.ZoneId

data class JewishLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double,
    val zoneId: ZoneId,
)

val defaultJerusalemLocation = JewishLocation(
    name = "Jerusalem",
    latitude = 31.778,
    longitude = 35.2354,
    elevationMeters = 754.0,
    zoneId = ZoneId.of("Asia/Jerusalem"),
)
