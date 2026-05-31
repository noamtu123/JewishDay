package com.turel.jewishdaynext.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turel.jewishdaynext.data.AppSettingsRepository
import com.turel.jewishdaynext.data.JewishDayRepository
import com.turel.jewishdaynext.model.JewishDayInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class TodayUiState(
    val dayInfo: JewishDayInfo,
    val preferHebrewDates: Boolean = false,
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    jewishDayRepository: JewishDayRepository,
    appSettingsRepository: AppSettingsRepository,
) : ViewModel() {
    private val dayInfo = jewishDayRepository.getToday()

    val uiState: StateFlow<TodayUiState> = appSettingsRepository.settings
        .map { settings ->
            TodayUiState(
                dayInfo = jewishDayRepository.getToday(
                    location = settings.selectedPlace.toJewishLocation(),
                    settings = settings.zmanimSettings,
                ),
                preferHebrewDates = settings.preferHebrewDates || settings.useHebrewInterface,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TodayUiState(dayInfo = dayInfo),
        )
}
