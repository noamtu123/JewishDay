// SPDX-License-Identifier: GPL-3.0-or-later

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
import com.noamtu.jewishday.model.CustomZmanUnit
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
import com.noamtu.jewishday.model.ZmanimTimeOption
import com.noamtu.jewishday.notification.DateStatusIconScheduler
import com.noamtu.jewishday.BuildConfig
import com.noamtu.jewishday.update.AppUpdateRepository
import com.noamtu.jewishday.update.AppVersion
import com.noamtu.jewishday.update.PendingUpdateStore
import com.noamtu.jewishday.update.UpdateCheckReport
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
    val includePreReleases: Boolean = false,
    /** This build is itself a pre-release, which changes what turning the switch off can do. */
    val installedPreReleaseName: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val dateStatusIconScheduler: DateStatusIconScheduler,
    private val appUpdateRepository: AppUpdateRepository,
    private val pendingUpdates: PendingUpdateStore,
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
                includePreReleases = settings.includePreReleases,
                installedPreReleaseName = InstalledPreReleaseName,
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

    private companion object {
        /**
         * The running build's version name when it is a pre-release, else null. Read from
         * BuildConfig rather than from GitHub: the answer is about this APK, so it needs no network
         * and is right even offline.
         */
        val InstalledPreReleaseName: String? =
            AppVersion.parse(BuildConfig.VERSION_NAME)?.takeIf { it.isPreRelease }?.displayName
    }

    fun setIncludePreReleases(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.setIncludePreReleases(enabled)
            // The channel just changed, so what is worth offering did too: turning it on may find a
            // test version, turning it off the stable one to go back to. Check now rather than on
            // the next open, and let the result replace whatever the old channel had offered.
            when (val report = appUpdateRepository.check()) {
                is UpdateCheckReport.Available -> pendingUpdates.offer(report.release, report.isDowngrade)
                is UpdateCheckReport.UpToDate,
                is UpdateCheckReport.NoReleases,
                -> pendingUpdates.clear()
                // Never reached GitHub: nothing learned, so leave any existing offer alone.
                is UpdateCheckReport.Failed -> Unit
            }
        }
    }

    /**
     * Resets all per-zman calculation methods to defaults, keeping the candle-lighting offset the
     * user chose at first launch.
     */
    fun resetZmanimMethods() {
        viewModelScope.launch {
            val current = appSettingsRepository.settings.first()
            val candle = current.candleLightingDefault ?: current.zmanimSettings.candleLightingMethod
            appSettingsRepository.setZmanimSettings(
                ZmanimCalculationSettings(
                    candleLightingMethod = candle,
                    // Kept with it: if their choice is a typed-in offset, the number is the choice.
                    candleLightingCustomMinutes = current.zmanimSettings.candleLightingCustomMinutes,
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

    fun setAlotHashacharMethod(method: AlotHashacharMethod) {
        updateZmanimSettings { it.copy(alotHashacharMethod = method) }
    }

    fun setMisheyakirMethod(method: MisheyakirMethod) {
        updateZmanimSettings { it.copy(misheyakirMethod = method) }
    }

    fun setSunriseMethod(method: SunriseMethod) {
        updateZmanimSettings { it.copy(sunriseMethod = method) }
    }

    fun setSofZmanShemaGraMethod(method: SofZmanShemaMethod) {
        updateZmanimSettings { it.copy(sofZmanShemaGraMethod = method) }
    }

    fun setSofZmanShemaMethod(method: SofZmanShemaMethod) {
        updateZmanimSettings { it.copy(sofZmanShemaMethod = method) }
    }

    fun setSofZmanTefillahGraMethod(method: SofZmanTefillahMethod) {
        updateZmanimSettings { it.copy(sofZmanTefillahGraMethod = method) }
    }

    fun setSofZmanTefillahMethod(method: SofZmanTefillahMethod) {
        updateZmanimSettings { it.copy(sofZmanTefillahMethod = method) }
    }

    fun setChatzotMethod(method: ChatzotMethod) {
        updateZmanimSettings { it.copy(chatzotMethod = method) }
    }

    fun setChatzotHaLailaMethod(method: ChatzotMethod) {
        updateZmanimSettings { it.copy(chatzotHaLailaMethod = method) }
    }

    fun setMinchaGedolaMethod(method: MinchaGedolaMethod) {
        updateZmanimSettings { it.copy(minchaGedolaMethod = method) }
    }

    fun setMinchaKetanaMethod(method: MinchaKetanaMethod) {
        updateZmanimSettings { it.copy(minchaKetanaMethod = method) }
    }

    fun setPlagHaminchaMethod(method: PlagHaminchaMethod) {
        updateZmanimSettings { it.copy(plagHaminchaMethod = method) }
    }

    fun setSunsetMethod(method: SunsetMethod) {
        updateZmanimSettings { it.copy(sunsetMethod = method) }
    }

    fun setTzeitHakochavimMethod(method: TzeitHakochavimMethod) {
        updateZmanimSettings { it.copy(tzeitHakochavimMethod = method) }
    }

    fun setCandleLightingMethod(method: CandleLightingMethod) {
        updateZmanimSettings { it.copy(candleLightingMethod = method) }
    }

    fun setMotzeiShabbatMethod(method: MotzeiShabbatMethod) {
        updateZmanimSettings { it.copy(motzeiShabbatMethod = method) }
    }

    /**
     * The end of the Ateret Torah day: minutes after sunset. Every one of his opinions is measured to
     * it, so it is part of the method rather than a display preference.
     */
    fun setAteretTorahSunsetOffsetMinutes(minutes: Int) {
        updateZmanimSettings { it.copy(ateretTorahSunsetOffsetMinutes = minutes.coerceIn(1, 120)) }
    }

    /** Tosefet added when leaving Shabbat or a Yom Tov, in minutes. Clamped to something sane. */
    fun setHolyDayTosefetMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(0, 120)
        updateZmanimSettings { it.copy(holyDayTosefetMinutes = clamped) }
    }

    fun setRabbeinuTamMethod(method: RabbeinuTamMethod) {
        updateZmanimSettings { it.copy(rabbeinuTamMethod = method) }
    }

    fun setChametzMethod(method: ChametzMethod) {
        updateZmanimSettings { it.copy(chametzMethod = method) }
    }

    // The custom options: each one stores the number that was typed in and selects the option that
    // uses it in a single write, so a method can never be showing a value that was not saved with it.

    fun setAlotHashacharCustomValue(method: AlotHashacharMethod, unit: CustomZmanUnit, value: Double) {
        updateZmanimSettings {
            it.copy(
                alotHashacharMethod = method,
                alotHashacharCustom = it.alotHashacharCustom.withValue(unit, value),
            )
        }
    }

    fun setMisheyakirCustomValue(method: MisheyakirMethod, unit: CustomZmanUnit, value: Double) {
        updateZmanimSettings {
            it.copy(
                misheyakirMethod = method,
                misheyakirCustom = it.misheyakirCustom.withValue(unit, value),
            )
        }
    }

    fun setSofZmanShemaCustomValue(method: SofZmanShemaMethod, unit: CustomZmanUnit, value: Double) {
        updateZmanimSettings {
            it.copy(
                sofZmanShemaMethod = method,
                sofZmanShemaCustom = it.sofZmanShemaCustom.withValue(unit, value),
            )
        }
    }

    fun setSofZmanTefillahCustomValue(method: SofZmanTefillahMethod, unit: CustomZmanUnit, value: Double) {
        updateZmanimSettings {
            it.copy(
                sofZmanTefillahMethod = method,
                sofZmanTefillahCustom = it.sofZmanTefillahCustom.withValue(unit, value),
            )
        }
    }

    fun setMinchaGedolaCustomValue(method: MinchaGedolaMethod, unit: CustomZmanUnit, value: Double) {
        updateZmanimSettings {
            it.copy(
                minchaGedolaMethod = method,
                minchaGedolaCustom = it.minchaGedolaCustom.withValue(unit, value),
            )
        }
    }

    fun setMinchaKetanaCustomValue(method: MinchaKetanaMethod, unit: CustomZmanUnit, value: Double) {
        updateZmanimSettings {
            it.copy(
                minchaKetanaMethod = method,
                minchaKetanaCustom = it.minchaKetanaCustom.withValue(unit, value),
            )
        }
    }

    fun setPlagHaminchaCustomValue(method: PlagHaminchaMethod, unit: CustomZmanUnit, value: Double) {
        updateZmanimSettings {
            it.copy(
                plagHaminchaMethod = method,
                plagHaminchaCustom = it.plagHaminchaCustom.withValue(unit, value),
            )
        }
    }

    fun setTzeitHakochavimCustomValue(method: TzeitHakochavimMethod, unit: CustomZmanUnit, value: Double) {
        updateZmanimSettings {
            it.copy(
                tzeitHakochavimMethod = method,
                tzeitHakochavimCustom = it.tzeitHakochavimCustom.withValue(unit, value),
            )
        }
    }

    fun setMotzeiShabbatCustomValue(method: MotzeiShabbatMethod, unit: CustomZmanUnit, value: Double) {
        updateZmanimSettings {
            it.copy(
                motzeiShabbatMethod = method,
                motzeiShabbatCustom = it.motzeiShabbatCustom.withValue(unit, value),
            )
        }
    }

    fun setRabbeinuTamCustomValue(method: RabbeinuTamMethod, unit: CustomZmanUnit, value: Double) {
        updateZmanimSettings {
            it.copy(
                rabbeinuTamMethod = method,
                rabbeinuTamCustom = it.rabbeinuTamCustom.withValue(unit, value),
            )
        }
    }

    fun setChametzCustomValue(method: ChametzMethod, unit: CustomZmanUnit, value: Double) {
        updateZmanimSettings {
            it.copy(
                chametzMethod = method,
                chametzCustom = it.chametzCustom.withValue(unit, value),
            )
        }
    }

    /** A typed-in candle-lighting offset, which also selects the custom option that uses it. */
    fun setCandleLightingCustomMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(0, 120)
        updateZmanimSettings {
            it.copy(
                candleLightingMethod = CandleLightingMethod.Custom,
                candleLightingCustomMinutes = clamped,
            )
        }
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