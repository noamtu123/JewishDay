// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noamtu.jewishday.data.AppSettingsRepository
import com.noamtu.jewishday.data.CurrentLocationRepository
import com.noamtu.jewishday.data.DeveloperOverridesRepository
import com.noamtu.jewishday.model.defaultJerusalemLocation
import com.noamtu.jewishday.model.skyDayFor
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/**
 * Drives the Glass sky from the app's own clock, so it follows the developer clock like every
 * other time on screen, and from the current location and zmanim opinions, so its dawn and dusk
 * are the ones in the list.
 */
@HiltViewModel
class GlassSkyViewModel @Inject constructor(
    clock: Clock,
    currentLocationRepository: CurrentLocationRepository,
    appSettingsRepository: AppSettingsRepository,
    developerOverridesRepository: DeveloperOverridesRepository,
) : ViewModel() {

    private val ticks = flow {
        while (true) {
            emit(Unit)
            delay(TickMillis)
        }
    }

    val frame: StateFlow<SkyFrame?> = combine(
        ticks,
        currentLocationRepository.currentLocation,
        appSettingsRepository.settings,
        // A jump of the developer clock should move the sky at once, not on the next tick.
        developerOverridesRepository.state,
    ) { _, location, settings, _ ->
        val place = location ?: defaultJerusalemLocation
        val now = clock.instant()
        val date = now.atZone(place.zoneId).toLocalDate()
        skyAt(now, skyDayFor(place, date, settings.zmanimSettings), place.zoneId)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private companion object {
        // The sky changes slowly, and each step is eased on screen; a minute is plenty.
        const val TickMillis = 60_000L
    }
}
