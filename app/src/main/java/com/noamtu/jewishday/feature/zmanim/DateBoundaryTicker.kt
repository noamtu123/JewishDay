// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.zmanim

import com.noamtu.jewishday.core.model.JewishLocation
import com.noamtu.jewishday.core.model.ZmanimCalculationSettings
import com.noamtu.jewishday.core.model.nextZmanimRefreshBoundary
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Emits immediately and then again whenever the displayed date changes (tzeit for the Hebrew
 * date, midnight for the day itself), so date-bound UI state recomputes while visible.
 */
internal fun dateBoundaryTicker(
    clock: Clock,
    location: JewishLocation,
    settings: ZmanimCalculationSettings,
): Flow<Instant> = flow {
    while (true) {
        val now = clock.instant()
        emit(now)
        val next = nextZmanimRefreshBoundary(location, settings, now)
        delay(Duration.between(now, next).toMillis().coerceAtLeast(MinimumTickMillis))
    }
}

private const val MinimumTickMillis = 1_000L
