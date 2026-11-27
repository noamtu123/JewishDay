// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.notification

import com.noamtu.jewishday.core.model.JewishLocation
import com.noamtu.jewishday.core.model.ZmanimCalculationSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map

/**
 * One value for each later change to what the date icon is worked out from: where the phone is
 * (a first fix after the icon rendered Jerusalem, a journey, a time-zone change, which re-stamps the
 * fix's zone) and the zmanim opinions (the tzeit it rolls at). The current values are skipped: the
 * service has just rendered from them.
 *
 * Without it the icon heard of neither until its next alarm, hours away, and that alarm was itself
 * set from the old place and opinion: the Hebrew date rolled at the wrong tzeit, and after a flight
 * the weekday turned at the old zone's midnight.
 */
internal fun statusIconInputChanges(
    zmanimSettings: Flow<ZmanimCalculationSettings>,
    location: Flow<JewishLocation?>,
): Flow<Unit> = combine(zmanimSettings, location) { settings, place -> settings to place }
    .distinctUntilChanged()
    .drop(1)
    .map { }
