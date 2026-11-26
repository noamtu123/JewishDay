// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import com.noamtu.jewishday.core.model.CandleLightingMethod
import com.noamtu.jewishday.core.model.DailyLearningType
import com.noamtu.jewishday.core.model.ZmanimCalculationSettings
import com.noamtu.jewishday.core.model.ZmanimTimeOption
import kotlinx.coroutines.flow.Flow

/**
 * Every theme is a fixed palette. None of them follows the system light/dark setting — a theme is a
 * look the user picked, not a mode, so the phone's setting must not quietly repaint it. That used
 * to be true of all but one: "Classic calm" swapped between two palettes with the system, and its
 * light half now stands alone as Olive grove.
 *
 * Ordered light first, then dark, which is the order the picker shows.
 */
enum class AppThemeOption(val storageValue: String) {
    BlueWhite("blue_white"),
    OliveGrove("olive_grove"),
    JerusalemStone("jerusalem_stone"),
    Sand("sand"),
    Midnight("midnight"),
    Slate("slate"),
    AmoledBlack("amoled_black"),
    Glass("glass"),
    ;

    companion object {
        /** The app-wide default applied on first launch and as a fallback. */
        val Default = Glass

        // What an install that never picked a theme was on before Glass became the default.
        internal val DefaultBeforeGlass = BlueWhite

        fun fromStorageValue(value: String?): AppThemeOption? =
            entries.firstOrNull { it.storageValue == value }
    }
}

data class AppSettings(
    val hebrewDateStatusIconEnabled: Boolean = true,
    // The settings' record of the app's language (a BCP 47 tag), or null before the first launch has
    // set one. The language itself is Android's per-app language — see `AppLanguageController`.
    val language: String? = null,
    val use24HourTime: Boolean = true,
    val enabledDailyLearning: Set<DailyLearningType> = DailyLearningType.Default,
    val enabledZmanimTimes: Set<ZmanimTimeOption> = ZmanimTimeOption.Default,
    val themeOption: AppThemeOption = AppThemeOption.Default,
    val zmanimSettings: ZmanimCalculationSettings = ZmanimCalculationSettings(),
    val candleLightingPromptHandled: Boolean = false,
    // The candle-lighting offset the user picked at first launch; used as the "default" marker
    // in the picker and as the value the Reset button restores to. Null until first launch answered.
    val candleLightingDefault: CandleLightingMethod? = null,
    /**
     * The user answered the location prompt with "always use Jerusalem", so it is never shown
     * again. Granting location clears it: that is a clearer statement of intent than the setting.
     */
    val alwaysUseJerusalem: Boolean = false,
    /**
     * Offer pre-releases as updates too. Off by default: a stable install must never be pulled onto
     * a test build by accident, and a pre-release cannot be uninstalled back down from in place.
     */
    val includePreReleases: Boolean = false,
    /**
     * The theme an existing install was on when it was moved to Glass, while the message saying so
     * has not been dismissed yet; null otherwise. See [AppSettingsRepository.introduceGlassTheme].
     */
    val themeBeforeGlass: AppThemeOption? = null,
)

data class RootUiSettings(
    val themeOption: AppThemeOption = AppThemeOption.Default,
)

interface AppSettingsRepository {
    val settings: Flow<AppSettings>
    val rootUiSettings: Flow<RootUiSettings>

    /**
     * Once per install, before anything else is written: an existing install on any theme but Glass
     * is moved to Glass, remembering what it was on, so the app can say so and offer the way back.
     * A new install has nothing stored yet and simply starts on Glass, the default.
     */
    suspend fun introduceGlassTheme()

    /** The user has read the message about the move to Glass, and kept it or gone back. */
    suspend fun dismissGlassThemeNotice()

    suspend fun setHebrewDateStatusIconEnabled(enabled: Boolean)
    /** Records the app's language ([tag]), or that there is none to record (null). */
    suspend fun setAppLanguage(tag: String?)
    suspend fun setUse24HourTime(enabled: Boolean)
    suspend fun setEnabledDailyLearning(types: Set<DailyLearningType>)
    suspend fun setEnabledZmanimTimes(options: Set<ZmanimTimeOption>)
    suspend fun setThemeOption(themeOption: AppThemeOption)
    suspend fun setZmanimSettings(settings: ZmanimCalculationSettings)
    suspend fun setCandleLightingPromptHandled(handled: Boolean)
    suspend fun setCandleLightingDefault(method: CandleLightingMethod)
    suspend fun setAlwaysUseJerusalem(enabled: Boolean)
    suspend fun setIncludePreReleases(enabled: Boolean)
}
