// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import android.location.Location
import com.noamtu.jewishday.core.model.JewishLocation
import com.noamtu.jewishday.core.model.defaultJerusalemLocation
import java.time.ZoneId

/**
 * Accuracy-aware one-shot fix comparison adapted from Android's location guidance. A fix more than
 * two minutes newer wins; one more than two minutes older loses. Within that window, prefer better
 * accuracy, or a newer fix that is not dramatically less accurate.
 */
fun isBetterLocationFix(
    candidateTimeMillis: Long,
    candidateAccuracyMeters: Float?,
    currentTimeMillis: Long,
    currentAccuracyMeters: Float?,
): Boolean {
    val timeDelta = candidateTimeMillis - currentTimeMillis
    if (timeDelta > SignificantLocationTimeDeltaMillis) return true
    if (timeDelta < -SignificantLocationTimeDeltaMillis) return false

    val candidateAccuracy = candidateAccuracyMeters
        ?.takeIf { it.isFinite() && it >= 0f }
        ?: Float.POSITIVE_INFINITY
    val currentAccuracy = currentAccuracyMeters
        ?.takeIf { it.isFinite() && it >= 0f }
        ?: Float.POSITIVE_INFINITY
    if (candidateAccuracy < currentAccuracy) return true
    return timeDelta > 0L && candidateAccuracy <= currentAccuracy + MaximumAccuracyRegressionMeters
}

private const val SignificantLocationTimeDeltaMillis = 2 * 60 * 1_000L
private const val MaximumAccuracyRegressionMeters = 200f

/**
 * Sentinel display name for a device-derived fix. The UI recognizes it to show a *localized*
 * "current location" label instead of this raw English string.
 */
const val CurrentLocationName = "Current location"

/** Where the location the app is using came from, so the UI can label it honestly. */
enum class LocationSource { CurrentFix, Jerusalem, Named }

/** Classifies a [JewishLocation] name into its [LocationSource] using the sentinel above. */
fun locationSourceForName(name: String): LocationSource = when (name) {
    CurrentLocationName -> LocationSource.CurrentFix
    defaultJerusalemLocation.name -> LocationSource.Jerusalem
    else -> LocationSource.Named
}

fun Location.toJewishLocation(name: String = CurrentLocationName): JewishLocation = JewishLocation(
    name = name,
    latitude = latitude,
    longitude = longitude,
    elevationMeters = if (hasAltitude()) altitude else 0.0,
    zoneId = ZoneId.systemDefault(),
)
