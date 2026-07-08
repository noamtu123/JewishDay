// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import java.time.ZoneId

data class JewishLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double,
    val zoneId: ZoneId,
)

/**
 * Whether this location is within Israel, used to pick the Israel vs. diaspora calendar (one vs.
 * two days of Yom Tov, the parsha schedule, the Hebcal feed). Derived from the coordinates so the
 * app follows where you actually are rather than a manual toggle. The box spans roughly Eilat in
 * the south to the northern border and covers Judea/Samaria and the Golan; the eastern edge stops
 * short of Amman (≈35.93°E) while still including the Golan (≈35.85°E).
 */
val JewishLocation.isInIsrael: Boolean
    get() = latitude in 29.45..33.35 && longitude in 34.25..35.90

val defaultJerusalemLocation = JewishLocation(
    name = "Jerusalem",
    latitude = 31.778,
    longitude = 35.2354,
    elevationMeters = 754.0,
    zoneId = ZoneId.of("Asia/Jerusalem"),
)