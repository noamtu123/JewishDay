package com.turel.jewishdaynext.feature.zmanim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turel.jewishdaynext.data.AppSettingsRepository
import com.turel.jewishdaynext.data.CurrentLocationRepository
import com.turel.jewishdaynext.data.JewishDayRepository
import com.turel.jewishdaynext.model.ZmanimDay
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

data class ZmanimUiState(
    val zmanimDay: ZmanimDay,
    val use24HourTime: Boolean = true,
)

@HiltViewModel
class ZmanimViewModel @Inject constructor(
    jewishDayRepository: JewishDayRepository,
    appSettingsRepository: AppSettingsRepository,
    currentLocationRepository: CurrentLocationRepository,
) : ViewModel() {
    private val initialZmanim = jewishDayRepository.getZmanim()

    val uiState: StateFlow<ZmanimUiState> = combine(
        appSettingsRepository.settings,
        currentLocationRepository.currentLocation,
    ) { settings, currentLocation ->
            val calculationLocation = currentLocation ?: currentLocationRepository.currentLocationOrDefault()
            ZmanimUiState(
                zmanimDay = jewishDayRepository.getZmanim(
                    location = calculationLocation,
                    settings = settings.zmanimSettings,
                ),
                use24HourTime = settings.use24HourTime,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ZmanimUiState(zmanimDay = initialZmanim),
        )
}
