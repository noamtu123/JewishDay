package com.turel.jewishdaynext.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turel.jewishdaynext.data.AppSettingsRepository
import com.turel.jewishdaynext.data.SavedPlace
import com.turel.jewishdaynext.model.ZmanimCalculationSettings
import com.turel.jewishdaynext.notification.DailyNotificationScheduler
import com.turel.jewishdaynext.notification.DateStatusIconScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val dailyDateNotificationEnabled: Boolean = false,
    val hebrewDateStatusIconEnabled: Boolean = false,
    val englishDateStatusIconEnabled: Boolean = false,
    val preferHebrewDates: Boolean = false,
    val useHebrewInterface: Boolean = false,
    val use24HourTime: Boolean = true,
    val blueWhiteTheme: Boolean = false,
    val amoledBlackTheme: Boolean = false,
    val savedPlaces: List<SavedPlace> = emptyList(),
    val selectedPlaceId: String = "",
    val zmanimSettings: ZmanimCalculationSettings = ZmanimCalculationSettings(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val dailyNotificationScheduler: DailyNotificationScheduler,
    private val dateStatusIconScheduler: DateStatusIconScheduler,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = appSettingsRepository.settings
        .map { settings ->
            SettingsUiState(
                dailyDateNotificationEnabled = settings.dailyDateNotificationEnabled,
                hebrewDateStatusIconEnabled = settings.hebrewDateStatusIconEnabled,
                englishDateStatusIconEnabled = settings.englishDateStatusIconEnabled,
                preferHebrewDates = settings.preferHebrewDates,
                useHebrewInterface = settings.useHebrewInterface,
                use24HourTime = settings.use24HourTime,
                blueWhiteTheme = settings.blueWhiteTheme,
                amoledBlackTheme = settings.amoledBlackTheme,
                savedPlaces = settings.savedPlaces,
                selectedPlaceId = settings.selectedPlaceId,
                zmanimSettings = settings.zmanimSettings,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun setDailyDateNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.setDailyDateNotificationEnabled(enabled)
            if (enabled) {
                dailyNotificationScheduler.schedule()
            } else {
                dailyNotificationScheduler.cancel()
            }
        }
    }

    fun setHebrewDateStatusIconEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.setHebrewDateStatusIconEnabled(enabled)
            syncDateStatusIcons()
        }
    }

    fun setEnglishDateStatusIconEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.setEnglishDateStatusIconEnabled(enabled)
            syncDateStatusIcons()
        }
    }

    fun setPreferHebrewDates(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.setPreferHebrewDates(enabled)
        }
    }

    fun setUseHebrewInterface(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.setUseHebrewInterface(enabled)
        }
    }

    fun setUse24HourTime(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.setUse24HourTime(enabled)
        }
    }

    fun setBlueWhiteTheme(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.setBlueWhiteTheme(enabled)
        }
    }

    fun setAmoledBlackTheme(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.setAmoledBlackTheme(enabled)
        }
    }

    fun selectPlace(placeId: String) {
        viewModelScope.launch {
            appSettingsRepository.selectPlace(placeId)
            syncDateStatusIcons()
        }
    }

    fun setInIsrael(enabled: Boolean) {
        viewModelScope.launch { appSettingsRepository.setInIsrael(enabled) }
    }

    fun setUseMgaForShemaAndTefila(enabled: Boolean) {
        viewModelScope.launch { appSettingsRepository.setUseMgaForShemaAndTefila(enabled) }
    }

    fun setUseSeaLevelSunrise(enabled: Boolean) {
        viewModelScope.launch { appSettingsRepository.setUseSeaLevelSunrise(enabled) }
    }

    fun setUseSeaLevelSunset(enabled: Boolean) {
        viewModelScope.launch { appSettingsRepository.setUseSeaLevelSunset(enabled) }
    }

    fun setAlotHashacharOffsetMinutes(minutes: Int) {
        viewModelScope.launch { appSettingsRepository.setAlotHashacharOffsetMinutes(minutes) }
    }

    fun setPlagHaminchaOffsetMinutes(minutes: Int) {
        viewModelScope.launch { appSettingsRepository.setPlagHaminchaOffsetMinutes(minutes) }
    }

    fun setCandleLightingOffsetMinutes(minutes: Int) {
        viewModelScope.launch { appSettingsRepository.setCandleLightingOffsetMinutes(minutes) }
    }

    private suspend fun syncDateStatusIcons() {
        val settings = appSettingsRepository.settings.first()
        dateStatusIconScheduler.sync(
            hebrewEnabled = settings.hebrewDateStatusIconEnabled,
            englishEnabled = settings.englishDateStatusIconEnabled,
        )
    }
}
