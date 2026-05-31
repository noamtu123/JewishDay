package com.turel.jewishdaynext.feature.zmanim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turel.jewishdaynext.data.AppSettingsRepository
import com.turel.jewishdaynext.data.JewishDayRepository
import com.turel.jewishdaynext.model.ZmanimDay
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ZmanimUiState(
    val zmanimDay: ZmanimDay,
    val use24HourTime: Boolean = true,
)

@HiltViewModel
class ZmanimViewModel @Inject constructor(
    jewishDayRepository: JewishDayRepository,
    appSettingsRepository: AppSettingsRepository,
) : ViewModel() {
    private val initialZmanim = jewishDayRepository.getZmanim()

    val uiState: StateFlow<ZmanimUiState> = appSettingsRepository.settings
        .map { settings ->
            ZmanimUiState(
                zmanimDay = jewishDayRepository.getZmanim(
                    location = settings.selectedPlace.toJewishLocation(),
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
