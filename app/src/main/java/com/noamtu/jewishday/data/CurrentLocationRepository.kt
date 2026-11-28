// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import com.noamtu.jewishday.core.model.JewishLocation
import kotlinx.coroutines.flow.StateFlow

data class CurrentLocationFix(
    val location: JewishLocation,
    val horizontalAccuracyMeters: Float?,
    val elapsedRealtimeNanos: Long,
    val wallTimeMillis: Long,
    val hasPrecisePermission: Boolean,
) {
    fun ageMillis(nowElapsedRealtimeNanos: Long, nowWallTimeMillis: Long): Long =
        if (elapsedRealtimeNanos > 0L && nowElapsedRealtimeNanos >= elapsedRealtimeNanos) {
            (nowElapsedRealtimeNanos - elapsedRealtimeNanos) / 1_000_000L
        } else {
            (nowWallTimeMillis - wallTimeMillis).coerceAtLeast(0L)
        }
}

data class CurrentLocationState(
    val location: JewishLocation? = null,
    /** Null for fallbacks and developer overrides, which cannot authorize live alignment. */
    val compassFix: CurrentLocationFix? = null,
    val isDeveloperOverride: Boolean = false,
)

/**
 * Where the phone is, as the rest of the app sees it: the developer-pinned location when one is set,
 * otherwise the device's own fix, falling back to the last remembered fix and then Jerusalem.
 */
interface CurrentLocationRepository {
    /** The location in use and where it came from; the compass needs a real, fresh fix. */
    val currentLocationState: StateFlow<CurrentLocationState>

    /** The location in use, or null before any fix (or after [useJerusalemFallback]). */
    val currentLocation: StateFlow<JewishLocation?>

    /** Publishes the OS's cached fix now and asks for a fresh one; [force] skips the throttle. */
    fun refreshCurrentLocation(force: Boolean = false)

    /** Drops and forgets the device fix, so the app uses Jerusalem. */
    fun useJerusalemFallback()

    /** The best location there is right now, for background work: never null, Jerusalem at worst. */
    fun currentLocationOrDefault(): JewishLocation
}
