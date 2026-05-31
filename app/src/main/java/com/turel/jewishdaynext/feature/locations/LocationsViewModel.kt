package com.turel.jewishdaynext.feature.locations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turel.jewishdaynext.data.AppSettingsRepository
import com.turel.jewishdaynext.data.SavedPlace
import com.turel.jewishdaynext.notification.DateStatusIconScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LocationsUiState(
    val savedPlaces: List<SavedPlace> = emptyList(),
    val selectedPlaceId: String = "",
)

@HiltViewModel
class LocationsViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val dateStatusIconScheduler: DateStatusIconScheduler,
) : ViewModel() {
    val uiState: StateFlow<LocationsUiState> = appSettingsRepository.settings
        .map { settings ->
            LocationsUiState(
                savedPlaces = settings.savedPlaces,
                selectedPlaceId = settings.selectedPlaceId,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LocationsUiState(),
        )

    fun selectPlace(placeId: String) {
        viewModelScope.launch {
            appSettingsRepository.selectPlace(placeId)
            syncDateStatusIcons()
        }
    }

    fun deletePlace(placeId: String) {
        viewModelScope.launch {
            appSettingsRepository.deletePlace(placeId)
            syncDateStatusIcons()
        }
    }

    fun savePlace(
        name: String,
        latitude: Double,
        longitude: Double,
        elevationMeters: Double,
        zoneId: ZoneId,
    ) {
        viewModelScope.launch {
            appSettingsRepository.savePlace(
                SavedPlace(
                    id = "place_${System.currentTimeMillis()}",
                    name = name.trim(),
                    latitude = latitude,
                    longitude = longitude,
                    elevationMeters = elevationMeters,
                    zoneId = zoneId,
                ),
            )
            syncDateStatusIcons()
        }
    }

    fun savePlace(place: SavedPlace) {
        viewModelScope.launch {
            appSettingsRepository.savePlace(place)
            syncDateStatusIcons()
        }
    }

    private suspend fun syncDateStatusIcons() {
        val settings = appSettingsRepository.settings.first()
        dateStatusIconScheduler.sync(
            hebrewEnabled = settings.hebrewDateStatusIconEnabled,
            englishEnabled = settings.englishDateStatusIconEnabled,
        )
    }
}
