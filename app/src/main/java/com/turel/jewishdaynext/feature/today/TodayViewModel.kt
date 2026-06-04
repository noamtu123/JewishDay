package com.turel.jewishdaynext.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turel.jewishdaynext.data.AppSettingsRepository
import com.turel.jewishdaynext.data.CurrentLocationRepository
import com.turel.jewishdaynext.data.JewishDayRepository
import com.turel.jewishdaynext.model.JewishDayInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

data class TodayUiState(
    val dayInfo: JewishDayInfo,
    val preferHebrewDates: Boolean = false,
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    jewishDayRepository: JewishDayRepository,
    appSettingsRepository: AppSettingsRepository,
    currentLocationRepository: CurrentLocationRepository,
) : ViewModel() {
    private val dayInfo = jewishDayRepository.getToday()

    val uiState: StateFlow<TodayUiState> = combine(
        appSettingsRepository.settings,
        currentLocationRepository.currentLocation,
    ) { settings, currentLocation ->
            val calculationLocation = currentLocation ?: currentLocationRepository.currentLocationOrDefault()
            TodayUiState(
                dayInfo = jewishDayRepository.getToday(
                    location = calculationLocation,
                    settings = settings.zmanimSettings,
                ),
                preferHebrewDates = settings.preferHebrewDates,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TodayUiState(dayInfo = dayInfo),
        )
}
