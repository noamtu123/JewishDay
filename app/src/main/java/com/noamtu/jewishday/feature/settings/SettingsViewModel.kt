package com.noamtu.jewishday.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noamtu.jewishday.data.AppLanguage
import com.noamtu.jewishday.data.AppThemeOption
import com.noamtu.jewishday.data.AppSettingsRepository
import com.noamtu.jewishday.model.AlotHashacharMethod
import com.noamtu.jewishday.model.CandleLightingMethod
import com.noamtu.jewishday.model.ChametzMethod
import com.noamtu.jewishday.model.ChatzotMethod
import com.noamtu.jewishday.model.DailyLearningType
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
import com.noamtu.jewishday.model.ZmanimTimeOption
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
    val language: AppLanguage = AppLanguage.English,
    val use24HourTime: Boolean = true,
    val enabledDailyLearning: Set<DailyLearningType> = DailyLearningType.Default,
    val enabledZmanimTimes: Set<ZmanimTimeOption> = ZmanimTimeOption.Default,
    val themeOption: AppThemeOption = AppThemeOption.Default,
    val zmanimSettings: ZmanimCalculationSettings = ZmanimCalculationSettings(),
    val candleLightingDefault: CandleLightingMethod? = null,
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
                language = settings.language,
                use24HourTime = settings.use24HourTime,
                enabledDailyLearning = settings.enabledDailyLearning,
                enabledZmanimTimes = settings.enabledZmanimTimes,
                themeOption = settings.themeOption,
                zmanimSettings = settings.zmanimSettings,
                candleLightingDefault = settings.candleLightingDefault,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun setHebrewDateStatusIconEnabled(enabled: Boolean) {
        dateStatusIconScheduler.sync(enabled = enabled)
        viewModelScope.launch {
            appSettingsRepository.setHebrewDateStatusIconEnabled(enabled)
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

    /**
     * Resets all per-zman calculation methods to defaults, keeping the Outside-Israel choice and
     * the candle-lighting offset the user chose at first launch.
     */
    fun resetZmanimMethods() {
        viewModelScope.launch {
            val current = appSettingsRepository.settings.first()
            val candle = current.candleLightingDefault ?: current.zmanimSettings.candleLightingMethod
            appSettingsRepository.setZmanimSettings(
                ZmanimCalculationSettings(
                    inIsrael = current.zmanimSettings.inIsrael,
                    candleLightingMethod = candle,
                ),
            )
        }
    }

    /** Restores the shown-zmanim list to the default set. */
    fun resetZmanimTimes() {
        viewModelScope.launch {
            appSettingsRepository.setEnabledZmanimTimes(ZmanimTimeOption.Default)
        }
    }

    /** Restores the daily-learning list to the default set. */
    fun resetDailyLearning() {
        viewModelScope.launch {
            appSettingsRepository.setEnabledDailyLearning(DailyLearningType.Default)
        }
    }

    fun setThemeOption(themeOption: AppThemeOption) {
        viewModelScope.launch {
            appSettingsRepository.setThemeOption(themeOption)
        }
    }

    fun setDailyLearningEnabled(type: DailyLearningType, enabled: Boolean) {
        viewModelScope.launch {
            val current = appSettingsRepository.settings.first().enabledDailyLearning
            appSettingsRepository.setEnabledDailyLearning(if (enabled) current + type else current - type)
        }
    }

    fun setZmanimTimeEnabled(option: ZmanimTimeOption, enabled: Boolean) {
        viewModelScope.launch {
            val current = appSettingsRepository.settings.first().enabledZmanimTimes
            appSettingsRepository.setEnabledZmanimTimes(if (enabled) current + option else current - option)
        }
    }

    fun setInIsrael(enabled: Boolean) {
        updateZmanimSettings { it.copy(preset = ZmanimPreset.Custom, inIsrael = enabled) }
    }

    fun setUseElevation(enabled: Boolean) {
        updateZmanimSettings { it.copy(preset = ZmanimPreset.Custom, useElevation = enabled) }
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

    fun setChatzotHaLailaMethod(method: ChatzotMethod) {
        updateZmanimSettings { it.copy(preset = ZmanimPreset.Custom, chatzotHaLailaMethod = method) }
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

}
