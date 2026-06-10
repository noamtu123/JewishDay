package com.noamtu.jewishday.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noamtu.jewishday.data.AppLanguage
import com.noamtu.jewishday.data.AppThemeOption
import com.noamtu.jewishday.data.AppSettingsRepository
import com.noamtu.jewishday.model.AlotHashacharMethod
import com.noamtu.jewishday.model.BainHashmashotMethod
import com.noamtu.jewishday.model.CandleLightingMethod
import com.noamtu.jewishday.model.ChametzMethod
import com.noamtu.jewishday.model.ChatzotMethod
import com.noamtu.jewishday.model.FastDayMethod
import com.noamtu.jewishday.model.HighLatitudeHandling
import com.noamtu.jewishday.model.MinchaGedolaMethod
import com.noamtu.jewishday.model.MinchaKetanaMethod
import com.noamtu.jewishday.model.MisheyakirMethod
import com.noamtu.jewishday.model.MotzeiShabbatMethod
import com.noamtu.jewishday.model.PlagHaminchaMethod
import com.noamtu.jewishday.model.RabbeinuTamMethod
import com.noamtu.jewishday.model.SofZmanShemaMethod
import com.noamtu.jewishday.model.SofZmanTefillahMethod
import com.noamtu.jewishday.model.SunriseMethod
import com.noamtu.jewishday.model.SunsetMethod
import com.noamtu.jewishday.model.TzeitHakochavimMethod
import com.noamtu.jewishday.model.ZmanimCalculationSettings
import com.noamtu.jewishday.model.ZmanimPreset
import com.noamtu.jewishday.notification.DateStatusIconScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val hebrewDateStatusIconEnabled: Boolean = false,
    val englishDateStatusIconEnabled: Boolean = false,
    val preferHebrewDates: Boolean = true,
    val language: AppLanguage = AppLanguage.English,
    val use24HourTime: Boolean = true,
    val advancedZmanimModeEnabled: Boolean = false,
    val rambamThreeChaptersEnabled: Boolean = false,
    val themeOption: AppThemeOption = AppThemeOption.Default,
    val zmanimSettings: ZmanimCalculationSettings = ZmanimCalculationSettings(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val dateStatusIconScheduler: DateStatusIconScheduler,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = appSettingsRepository.settings
        .map { settings ->
            SettingsUiState(
                hebrewDateStatusIconEnabled = settings.hebrewDateStatusIconEnabled,
                englishDateStatusIconEnabled = settings.englishDateStatusIconEnabled,
                preferHebrewDates = settings.preferHebrewDates,
                language = settings.language,
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

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            appSettingsRepository.setAppLanguage(language)
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

    fun setSofZmanShemaGraMethod(method: SofZmanShemaMethod) {
        updateZmanimSettings { it.copy(preset = ZmanimPreset.Custom, sofZmanShemaGraMethod = method) }
    }

    fun setSofZmanShemaMethod(method: SofZmanShemaMethod) {
        updateZmanimSettings { it.copy(preset = ZmanimPreset.Custom, sofZmanShemaMethod = method) }
    }

    fun setSofZmanTefillahGraMethod(method: SofZmanTefillahMethod) {
        updateZmanimSettings { it.copy(preset = ZmanimPreset.Custom, sofZmanTefillahGraMethod = method) }
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
            // Read the latest persisted value rather than uiState.value: the StateFlow
            // updates asynchronously, so rapid consecutive edits could otherwise be
            // applied to a stale snapshot and silently drop the earlier change.
            val current = appSettingsRepository.settings.first().zmanimSettings
            appSettingsRepository.setZmanimSettings(transform(current))
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
