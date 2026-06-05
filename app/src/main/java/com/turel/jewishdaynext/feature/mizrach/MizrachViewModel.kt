package com.turel.jewishdaynext.feature.mizrach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turel.jewishdaynext.data.CurrentLocationRepository
import com.turel.jewishdaynext.data.JewishDayRepository
import com.turel.jewishdaynext.model.MizrachInfo
import com.turel.jewishdaynext.model.defaultJerusalemLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MizrachUiState(
    val mizrachInfo: MizrachInfo,
    val hasCurrentLocation: Boolean = false,
)

@HiltViewModel
class MizrachViewModel @Inject constructor(
    private val jewishDayRepository: JewishDayRepository,
    private val currentLocationRepository: CurrentLocationRepository,
) : ViewModel() {
    val uiState: StateFlow<MizrachUiState> = currentLocationRepository.currentLocation
        .map { currentLocation ->
            val location = currentLocation ?: defaultJerusalemLocation
            MizrachUiState(
                mizrachInfo = jewishDayRepository.getMizrach(location),
                hasCurrentLocation = currentLocation != null,
            )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MizrachUiState(mizrachInfo = jewishDayRepository.getMizrach()),
        )

    fun refreshCurrentLocation() {
        currentLocationRepository.refreshCurrentLocation()
    }
}
