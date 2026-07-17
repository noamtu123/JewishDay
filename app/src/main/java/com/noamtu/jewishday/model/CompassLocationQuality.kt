// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import kotlin.math.min
import kotlin.math.sin

/**
 * Whether a location fix is strong enough for alignment and haptics, not merely for zmanim.
 * The distance-relative limit keeps location uncertainty within a 3-degree bearing-error budget;
 * the absolute cap prevents a very coarse fix from becoming authoritative at long distances.
 */
fun isCompassLocationTrusted(
    hasPreciseLocationPermission: Boolean,
    horizontalAccuracyMeters: Float?,
    ageMillis: Long,
    distanceToTargetMeters: Double,
): Boolean {
    if (!hasPreciseLocationPermission) return false
    val accuracy = horizontalAccuracyMeters
        ?.takeIf { it.isFinite() && it >= 0f }
        ?: return false
    if (ageMillis !in 0L..CompassLocationMaxAgeMillis ||
        !distanceToTargetMeters.isFinite() || distanceToTargetMeters <= 0.0
    ) return false

    val directionalLimit = distanceToTargetMeters * sin(Math.toRadians(LocationBearingErrorBudgetDegrees))
    return accuracy.toDouble() <= min(CompassLocationAbsoluteAccuracyMeters, directionalLimit)
}

const val CompassLocationMaxAgeMillis = 2 * 60 * 1_000L
private const val CompassLocationAbsoluteAccuracyMeters = 5_000.0
private const val LocationBearingErrorBudgetDegrees = 3.0
