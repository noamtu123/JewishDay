package com.turel.jewishdaynext.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turel.jewishdaynext.data.AppSettingsRepository
import com.turel.jewishdaynext.data.CurrentLocationRepository
import com.turel.jewishdaynext.data.JewishDayRepository
import com.turel.jewishdaynext.model.defaultJerusalemLocation
import com.turel.jewishdaynext.model.JewishDayInfo
import com.turel.jewishdaynext.model.ZmanimCalculationSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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

@HiltViewModel
class TodayViewModel @Inject constructor(
    jewishDayRepository: JewishDayRepository,
    appSettingsRepository: AppSettingsRepository,
    currentLocationRepository: CurrentLocationRepository,
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
        currentLocationRepository.currentLocation,
    ) { settings, currentLocation ->
            val calculationLocation = currentLocation ?: defaultJerusalemLocation
            TodayUiState(
                dayInfo = jewishDayRepository.getToday(
                    location = calculationLocation,
                    settings = settings.zmanimSettings,
                ),
                preferHebrewDates = settings.preferHebrewDates,
            )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TodayUiState(),
        )
}
