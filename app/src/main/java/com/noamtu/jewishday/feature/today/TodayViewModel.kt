package com.noamtu.jewishday.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noamtu.jewishday.data.AppSettingsRepository
import com.noamtu.jewishday.data.CurrentLocationRepository
import com.noamtu.jewishday.data.JewishDayRepository
import com.noamtu.jewishday.model.dateBoundaryTicker
import com.noamtu.jewishday.model.defaultJerusalemLocation
import com.noamtu.jewishday.model.JewishDayInfo
import com.noamtu.jewishday.model.JewishLocation
import com.noamtu.jewishday.model.ZmanimCalculationSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

data class TodayUiState(
    val dayInfo: JewishDayInfo? = null,
    val preferHebrewDates: Boolean = false,
)

private data class TodaySettings(
    val preferHebrewDates: Boolean,
    val zmanimSettings: ZmanimCalculationSettings,
)

private data class TodayInput(
    val settings: TodaySettings,
    val location: JewishLocation,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel @Inject constructor(
    jewishDayRepository: JewishDayRepository,
    appSettingsRepository: AppSettingsRepository,
    currentLocationRepository: CurrentLocationRepository,
    clock: Clock,
) : ViewModel() {
    val uiState: StateFlow<TodayUiState> = combine(
        appSettingsRepository.settings
            .map { settings ->
                TodaySettings(
                    preferHebrewDates = settings.preferHebrewDates,
                    zmanimSettings = settings.zmanimSettings,
                )
            }
            .distinctUntilChanged(),
        currentLocationRepository.currentLocation
            .map { currentLocation -> currentLocation ?: defaultJerusalemLocation }
            .distinctUntilChanged(),
        ::TodayInput,
    )
        .distinctUntilChanged()
        // Re-emit at each tzeit/midnight boundary so the date stays correct while visible.
        .flatMapLatest { input ->
            dateBoundaryTicker(clock, input.location, input.settings.zmanimSettings)
                .map { input }
        }
        .map { input ->
            TodayUiState(
                dayInfo = jewishDayRepository.getToday(
                    location = input.location,
                    settings = input.settings.zmanimSettings,
                ),
                preferHebrewDates = input.settings.preferHebrewDates,
            )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TodayUiState(),
        )
}
