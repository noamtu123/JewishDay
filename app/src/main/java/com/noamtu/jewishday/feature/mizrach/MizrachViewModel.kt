// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.mizrach

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noamtu.jewishday.data.CurrentLocationRepository
import com.noamtu.jewishday.data.CurrentLocationState
import com.noamtu.jewishday.data.DeveloperOverridesRepository
import com.noamtu.jewishday.data.JewishDayRepository
import com.noamtu.jewishday.model.MizrachInfo
import com.noamtu.jewishday.model.defaultJerusalemLocation
import com.noamtu.jewishday.model.isCompassLocationTrusted
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

enum class CompassLocationTrust {
    Trusted,
    NeedsPrecisePermission,
    NeedsBetterFix,
    DeveloperOverride,
}

data class MizrachUiState(
    val mizrachInfo: MizrachInfo,
    val hasCurrentLocation: Boolean = false,
    val compassLocationTrust: CompassLocationTrust = CompassLocationTrust.NeedsBetterFix,
    /** Hidden developer switch: overlay live sensor/quality diagnostics on the compass. */
    val compassMonitoringEnabled: Boolean = false,
) {
    val compassLocationTrusted: Boolean get() = compassLocationTrust == CompassLocationTrust.Trusted
}

@HiltViewModel
class MizrachViewModel @Inject constructor(
    private val jewishDayRepository: JewishDayRepository,
    private val currentLocationRepository: CurrentLocationRepository,
    private val developerOverridesRepository: DeveloperOverridesRepository,
) : ViewModel() {
    private val trustTicks = flow {
        while (true) {
            emit(Unit)
            delay(LocationTrustRefreshMillis)
        }
    }

    val uiState: StateFlow<MizrachUiState> = combine(
        currentLocationRepository.currentLocationState,
        developerOverridesRepository.state,
        trustTicks,
    ) { currentState: CurrentLocationState, overrides, _ ->
        val location = currentState.location ?: defaultJerusalemLocation
        val mizrachInfo = jewishDayRepository.getMizrach(location)
        val fix = currentState.compassFix
        val fixAgeMillis = fix?.ageMillis(
            nowElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos(),
            nowWallTimeMillis = System.currentTimeMillis(),
        )
        val locationTrust = when {
            currentState.isDeveloperOverride -> CompassLocationTrust.DeveloperOverride
            fix == null -> CompassLocationTrust.NeedsBetterFix
            !fix.hasPrecisePermission -> CompassLocationTrust.NeedsPrecisePermission
            isCompassLocationTrusted(
                hasPreciseLocationPermission = true,
                horizontalAccuracyMeters = fix.horizontalAccuracyMeters,
                ageMillis = fixAgeMillis ?: Long.MAX_VALUE,
                distanceToTargetMeters = mizrachInfo.distanceMeters,
            ) -> CompassLocationTrust.Trusted
            else -> CompassLocationTrust.NeedsBetterFix
        }
        MizrachUiState(
            mizrachInfo = mizrachInfo,
            hasCurrentLocation = currentState.location != null,
            compassLocationTrust = locationTrust,
            compassMonitoringEnabled = overrides.compassMonitoringEnabled,
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MizrachUiState(mizrachInfo = jewishDayRepository.getMizrach()),
        )

    fun refreshCurrentLocation() {
        currentLocationRepository.refreshCurrentLocation(force = true)
    }

    /** Drops the device fix (app policy: a location we can no longer obtain is not remembered). */
    fun useJerusalemFallback() {
        currentLocationRepository.useJerusalemFallback()
    }

    private companion object {
        const val LocationTrustRefreshMillis = 1_000L
    }
}
