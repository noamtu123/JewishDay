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
 * app follows where you actually are rather than a manual toggle.
 *
 * This used to be a latitude/longitude rectangle, which is cheap but cannot express a border: the
 * box that reached Metula in the north and the Golan in the east also swallowed Tyre in southern
 * Lebanon and Irbid in northwestern Jordan, and silently gave them one day of Yom Tov. It is now a
 * point-in-polygon test against [IsraelOutline].
 */
val JewishLocation.isInIsrael: Boolean
    get() = isInsideIsraelOutline(latitude = latitude, longitude = longitude)

/**
 * A coarse outline of the area the Israel calendar is used in — Israel proper together with
 * Judea/Samaria and the Golan, which is the behaviour this replaces. Vertices are (longitude,
 * latitude) in order around the perimeter, accurate to roughly a few kilometres: enough to place a
 * city on the right side of the border, and deliberately not a statement about where any border
 * runs. Nothing halachic is decided here — only which calendar the coordinates imply.
 */
private val IsraelOutline: List<Pair<Double, Double>> = listOf(
    // Mediterranean coast, north to south.
    35.10 to 33.09, // Rosh Hanikra
    34.95 to 32.83, // Haifa
    34.85 to 32.33, // Netanya
    34.75 to 32.08, // Tel Aviv
    34.63 to 31.80, // Ashdod
    34.55 to 31.66, // Ashkelon
    34.48 to 31.59,
    // Egyptian border, running south-south-east to the Gulf of Aqaba.
    34.27 to 31.22, // Kerem Shalom / Rafah
    34.42 to 30.87, // Nitzana
    34.47 to 30.72, // Ezuz
    34.60 to 30.40,
    34.78 to 30.03,
    34.89 to 29.49, // Taba
    // Southern tip and the Jordanian border running north up the Arava.
    34.98 to 29.52, // Eilat / Aqaba corner
    35.08 to 30.00,
    35.15 to 30.50,
    35.30 to 30.90,
    35.40 to 31.10, // southern Dead Sea
    35.50 to 31.50,
    35.55 to 31.80, // northern Dead Sea / Jordan river
    35.55 to 32.10,
    35.57 to 32.40,
    35.57 to 32.50, // Beit She'an
    35.60 to 32.70,
    35.68 to 32.72, // Yarmouk
    // Golan, east side.
    35.78 to 32.85,
    35.90 to 33.10,
    35.90 to 33.28,
    35.86 to 33.40, // Mount Hermon
    // Lebanese border, running back west to the sea.
    35.70 to 33.33,
    35.57 to 33.28, // Metula
    35.50 to 33.11,
    35.30 to 33.09,
)

// Cheap rejection box around the outline, so the common "nowhere near Israel" case costs two
// comparisons instead of a full ray cast.
private val IsraelLatitudes = IsraelOutline.minOf { it.second }..IsraelOutline.maxOf { it.second }
private val IsraelLongitudes = IsraelOutline.minOf { it.first }..IsraelOutline.maxOf { it.first }

/** Standard even-odd ray cast against [IsraelOutline]. Exposed for tests. */
internal fun isInsideIsraelOutline(latitude: Double, longitude: Double): Boolean {
    if (!latitude.isFinite() || !longitude.isFinite()) return false
    if (latitude !in IsraelLatitudes || longitude !in IsraelLongitudes) return false

    var inside = false
    var previous = IsraelOutline.size - 1
    for (current in IsraelOutline.indices) {
        val (currentLongitude, currentLatitude) = IsraelOutline[current]
        val (previousLongitude, previousLatitude) = IsraelOutline[previous]
        if ((currentLatitude > latitude) != (previousLatitude > latitude)) {
            val crossingLongitude = (previousLongitude - currentLongitude) *
                (latitude - currentLatitude) / (previousLatitude - currentLatitude) + currentLongitude
            if (longitude < crossingLongitude) inside = !inside
        }
        previous = current
    }
    return inside
}

val defaultJerusalemLocation = JewishLocation(
    name = "Jerusalem",
    latitude = 31.778,
    longitude = 35.2354,
    elevationMeters = 754.0,
    zoneId = ZoneId.of("Asia/Jerusalem"),
)
