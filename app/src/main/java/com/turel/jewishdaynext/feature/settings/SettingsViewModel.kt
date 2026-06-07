package com.turel.jewishdaynext.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turel.jewishdaynext.data.AppThemeOption
import com.turel.jewishdaynext.data.AppSettingsRepository
import com.turel.jewishdaynext.model.AlotHashacharMethod
import com.turel.jewishdaynext.model.BainHashmashotMethod
import com.turel.jewishdaynext.model.CandleLightingMethod
import com.turel.jewishdaynext.model.ChametzMethod
import com.turel.jewishdaynext.model.ChatzotMethod
import com.turel.jewishdaynext.model.FastDayMethod
import com.turel.jewishdaynext.model.HighLatitudeHandling
import com.turel.jewishdaynext.model.MinchaGedolaMethod
import com.turel.jewishdaynext.model.MinchaKetanaMethod
import com.turel.jewishdaynext.model.MisheyakirMethod
import com.turel.jewishdaynext.model.MotzeiShabbatMethod
import com.turel.jewishdaynext.model.PlagHaminchaMethod
import com.turel.jewishdaynext.model.RabbeinuTamMethod
import com.turel.jewishdaynext.model.SamuchLeMinchaKetanaMethod
import com.turel.jewishdaynext.model.SofZmanShemaMethod
import com.turel.jewishdaynext.model.SofZmanTefillahMethod
import com.turel.jewishdaynext.model.SunriseMethod
import com.turel.jewishdaynext.model.SunsetMethod
import com.turel.jewishdaynext.model.TzeitHakochavimMethod
import com.turel.jewishdaynext.model.ZmanimCalculationSettings
import com.turel.jewishdaynext.model.ZmanimPreset
import com.turel.jewishdaynext.model.defaultSettings
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
    val advancedZmanimModeEnabled: Boolean = false,
    val rambamThreeChaptersEnabled: Boolean = false,
    val themeOption: AppThemeOption = AppThemeOption.Classic,
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
                advancedZmanimModeEnabled = settings.advancedZmanimModeEnabled,
                rambamThreeChaptersEnabled = settings.rambamThreeChaptersEnabled,
                themeOption = settings.themeOption,
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

    fun setAdvancedZmanimModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.setAdvancedZmanimModeEnabled(enabled)
        }
    }

    fun setRambamThreeChaptersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.setRambamThreeChaptersEnabled(enabled)
        }
    }

    fun setThemeOption(themeOption: AppThemeOption) {
        viewModelScope.launch {
            appSettingsRepository.setThemeOption(themeOption)
        }
    }

    fun setZmanimPreset(preset: ZmanimPreset) {
        viewModelScope.launch {
            val current = uiState.value.zmanimSettings
            appSettingsRepository.setZmanimSettings(preset.defaultSettings(current.inIsrael))
        }
    }

    fun setInIsrael(enabled: Boolean) {
        updateZmanimSettings { it.copy(preset = ZmanimPreset.Custom, inIsrael = enabled) }
    }

    fun setHighLatitudeHandling(method: HighLatitudeHandling) {
        updateZmanimSettings { it.copy(preset = ZmanimPreset.Custom, highLatitudeHandling = method) }
    }

    fun setAlotHashacharMethod(method: AlotHashacharMethod) {
        updateZmanimSettings { it.copy(preset = ZmanimPreset.Custom, alotHashacharMethod = method) }
    }

    fun setMisheyakirMethod(method: MisheyakirMethod) {
        updateZmanimSettings { it.copy(preset = ZmanimPreset.Custom, misheyakirMethod = method) }
    }

    fun setSunriseMethod(method: SunriseMethod) {
        updateZmanimSettings { it.copy(preset = ZmanimPreset.Custom, sunriseMethod = method) }
    }

    fun setSofZmanShemaMethod(method: SofZmanShemaMethod) {
        updateZmanimSettings { it.copy(preset = ZmanimPreset.Custom, sofZmanShemaMethod = method) }
    }

    fun setSofZmanTefillahMethod(method: SofZmanTefillahMethod) {
        updateZmanimSettings { it.copy(preset = ZmanimPreset.Custom, sofZmanTefillahMethod = method) }
    }

    fun setChatzotMethod(method: ChatzotMethod) {
        updateZmanimSettings { it.copy(preset = ZmanimPreset.Custom, chatzotMethod = method) }
    }

    fun setMinchaGedolaMethod(method: MinchaGedolaMethod) {
        updateZmanimSettings { it.copy(preset = ZmanimPreset.Custom, minchaGedolaMethod = method) }
    }

    fun setSamuchLeMinchaKetanaMethod(method: SamuchLeMinchaKetanaMethod) {
        updateZmanimSettings { it.copy(preset = ZmanimPreset.Custom, samuchLeMinchaKetanaMethod = method) }
    }

    fun setMinchaKetanaMethod(method: MinchaKetanaMethod) {
        updateZmanimSettings { it.copy(preset = ZmanimPreset.Custom, minchaKetanaMethod = method) }
    }

    fun setPlagHaminchaMethod(method: PlagHaminchaMethod) {
        updateZmanimSettings { it.copy(preset = ZmanimPreset.Custom, plagHaminchaMethod = method) }
    }

    fun setSunsetMethod(method: SunsetMethod) {
        updateZmanimSettings { it.copy(preset = ZmanimPreset.Custom, sunsetMethod = method) }
    }

    fun setTzeitHakochavimMethod(method: TzeitHakochavimMethod) {
        updateZmanimSettings { it.copy(preset = ZmanimPreset.Custom, tzeitHakochavimMethod = method) }
    }

    fun setCandleLightingMethod(method: CandleLightingMethod) {
        updateZmanimSettings { it.copy(preset = ZmanimPreset.Custom, candleLightingMethod = method) }
    }

    fun setMotzeiShabbatMethod(method: MotzeiShabbatMethod) {
        updateZmanimSettings { it.copy(preset = ZmanimPreset.Custom, motzeiShabbatMethod = method) }
    }

    fun setRabbeinuTamMethod(method: RabbeinuTamMethod) {
        updateZmanimSettings { it.copy(preset = ZmanimPreset.Custom, rabbeinuTamMethod = method) }
    }

    fun setBainHashmashotMethod(method: BainHashmashotMethod) {
        updateZmanimSettings { it.copy(preset = ZmanimPreset.Custom, bainHashmashotMethod = method) }
    }

    fun setFastDayMethod(method: FastDayMethod) {
        updateZmanimSettings { it.copy(preset = ZmanimPreset.Custom, fastDayMethod = method) }
    }

    fun setChametzMethod(method: ChametzMethod) {
        updateZmanimSettings { it.copy(preset = ZmanimPreset.Custom, chametzMethod = method) }
    }

    private fun updateZmanimSettings(transform: (ZmanimCalculationSettings) -> ZmanimCalculationSettings) {
        viewModelScope.launch {
            appSettingsRepository.setZmanimSettings(transform(uiState.value.zmanimSettings))
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
