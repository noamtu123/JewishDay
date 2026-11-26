// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.noamtu.jewishday.core.model.CandleLightingMethod
import com.noamtu.jewishday.core.model.DailyLearningType
import com.noamtu.jewishday.core.model.ZmanimCalculationSettings
import com.noamtu.jewishday.core.model.ZmanimTimeOption
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class DataStoreAppSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val startupSettingsCache: StartupSettingsCache,
) : AppSettingsRepository {
    override val rootUiSettings: Flow<RootUiSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map(::decodeRootUiSettings)
        .distinctUntilChanged()
        // Mirrors root UI settings into a synchronous cache for fast startup reads.
        // Note: setThemeOption also writes the cache explicitly — that is
        // intentional, not redundant: this onEach only runs while something is collecting
        // rootUiSettings, which is not guaranteed at write time.
        .onEach(startupSettingsCache::write)

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val rootUiSettings = decodeRootUiSettings(preferences)
            AppSettings(
                hebrewDateStatusIconEnabled = preferences[HebrewDateStatusIconEnabled] ?: true,
                language = decodeLanguage(preferences),
                use24HourTime = preferences[Use24HourTime] ?: true,
                enabledDailyLearning = preferences[EnabledDailyLearningKey]
                    ?.mapNotNull(DailyLearningType::fromStorageValue)?.toSet()
                    ?: DailyLearningType.Default,
                enabledZmanimTimes = preferences[EnabledZmanimTimesKey]
                    ?.mapNotNull(ZmanimTimeOption::fromStorageValue)?.toSet()
                    ?: ZmanimTimeOption.Default,
                themeOption = rootUiSettings.themeOption,
                zmanimSettings = preferences.zmanimSettings(),
                candleLightingPromptHandled = preferences[CandleLightingPromptHandled] ?: false,
                candleLightingDefault = CandleLightingMethod.fromStorageValue(preferences[CandleLightingDefaultKey]),
                alwaysUseJerusalem = preferences[AlwaysUseJerusalem] ?: false,
                includePreReleases = preferences[IncludePreReleases] ?: false,
                themeBeforeGlass = AppThemeOption.fromStorageValue(preferences[ThemeBeforeGlass]),
            )
        }

    override suspend fun introduceGlassTheme() {
        var moved = false
        val updatedPreferences = dataStore.edit { preferences ->
            if (preferences[GlassThemeIntroduced] == true) return@edit
            // Every install that has launched before stored its language at that first launch (see
            // AppLanguageController.syncOnLaunch, which runs after this); a new one has not, whatever
            // else is there.
            val launchedBefore = AppLanguageKey in preferences || UseHebrewInterface in preferences || ThemeOption in preferences
            val previous = if (launchedBefore) {
                storedThemeOption(preferences) ?: AppThemeOption.DefaultBeforeGlass
            } else {
                null
            }
            preferences[GlassThemeIntroduced] = true
            if (previous != null && previous != AppThemeOption.Glass) {
                preferences[ThemeOption] = AppThemeOption.Glass.storageValue
                preferences.remove(BlueWhiteTheme)
                preferences.remove(AmoledBlackTheme)
                preferences[ThemeBeforeGlass] = previous.storageValue
                moved = true
            }
        }
        if (moved) startupSettingsCache.write(decodeRootUiSettings(updatedPreferences))
    }

    override suspend fun dismissGlassThemeNotice() {
        dataStore.edit { preferences -> preferences.remove(ThemeBeforeGlass) }
    }

    override suspend fun setHebrewDateStatusIconEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[HebrewDateStatusIconEnabled] = enabled
        }
    }

    override suspend fun setAppLanguage(tag: String?) {
        dataStore.edit { preferences ->
            if (tag == null) preferences.remove(AppLanguageKey) else preferences[AppLanguageKey] = LanguageRecord.encode(tag)
            // Superseded by the record above; the oldest versions' Hebrew on/off switch.
            preferences.remove(UseHebrewInterface)
        }
    }

    override suspend fun setUse24HourTime(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Use24HourTime] = enabled
        }
    }

    override suspend fun setEnabledDailyLearning(types: Set<DailyLearningType>) {
        dataStore.edit { preferences ->
            preferences[EnabledDailyLearningKey] = types.map(DailyLearningType::storageValue).toSet()
        }
    }

    override suspend fun setEnabledZmanimTimes(options: Set<ZmanimTimeOption>) {
        dataStore.edit { preferences ->
            preferences[EnabledZmanimTimesKey] = options.map(ZmanimTimeOption::storageValue).toSet()
        }
    }

    override suspend fun setThemeOption(themeOption: AppThemeOption) {
        val updatedPreferences = dataStore.edit { preferences ->
            preferences[ThemeOption] = themeOption.storageValue
            preferences.remove(BlueWhiteTheme)
            preferences.remove(AmoledBlackTheme)
        }
        startupSettingsCache.write(decodeRootUiSettings(updatedPreferences))
    }

    override suspend fun setZmanimSettings(settings: ZmanimCalculationSettings) {
        dataStore.edit { preferences ->
            preferences.putZmanimSettings(settings)
        }
    }

    override suspend fun setCandleLightingPromptHandled(handled: Boolean) {
        dataStore.edit { preferences ->
            preferences[CandleLightingPromptHandled] = handled
        }
    }

    override suspend fun setCandleLightingDefault(method: CandleLightingMethod) {
        dataStore.edit { preferences ->
            preferences[CandleLightingDefaultKey] = method.storageValue
        }
    }

    override suspend fun setAlwaysUseJerusalem(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AlwaysUseJerusalem] = enabled
        }
    }

    override suspend fun setIncludePreReleases(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[IncludePreReleases] = enabled
        }
    }

    private fun decodeRootUiSettings(preferences: Preferences): RootUiSettings = RootUiSettings(
        themeOption = decodeThemeOption(preferences),
    )

    private fun decodeLanguage(preferences: Preferences): String? =
        LanguageRecord.decode(preferences[AppLanguageKey])
        // Older installs that stored only the Hebrew on/off boolean.
            ?: preferences[UseHebrewInterface]?.let { if (it) "he" else "en" }

    private fun decodeThemeOption(preferences: Preferences): AppThemeOption =
        storedThemeOption(preferences) ?: AppThemeOption.Default

    /** The theme the user picked, or null if they never picked one. */
    private fun storedThemeOption(preferences: Preferences): AppThemeOption? {
        // "Classic calm" was one theme that followed the system light/dark setting; what survives
        // of it is Olive grove, so that is where a stored "classic" lands.
        if (preferences[ThemeOption] == LegacyClassicTheme) return AppThemeOption.OliveGrove
        return AppThemeOption.fromStorageValue(preferences[ThemeOption])
            ?: when {
                preferences[AmoledBlackTheme] == true -> AppThemeOption.AmoledBlack
                preferences[BlueWhiteTheme] == true -> AppThemeOption.BlueWhite
                else -> null
            }
    }

    private companion object {
        val HebrewDateStatusIconEnabled = booleanPreferencesKey("hebrew_date_status_icon_enabled")
        val AppLanguageKey = stringPreferencesKey("app_language")
        // Retained read-only to migrate installs that predate the language picker.
        val UseHebrewInterface = booleanPreferencesKey("use_hebrew_interface")
        val Use24HourTime = booleanPreferencesKey("use_24_hour_time")
        val EnabledDailyLearningKey = stringSetPreferencesKey("enabled_daily_learning")
        val EnabledZmanimTimesKey = stringSetPreferencesKey("enabled_zmanim_times")
        val CandleLightingPromptHandled = booleanPreferencesKey("candle_lighting_prompt_handled")
        val CandleLightingDefaultKey = stringPreferencesKey("candle_lighting_default")
        val AlwaysUseJerusalem = booleanPreferencesKey("always_use_jerusalem")
        val IncludePreReleases = booleanPreferencesKey("include_pre_releases")
        val ThemeOption = stringPreferencesKey("theme_option")
        val GlassThemeIntroduced = booleanPreferencesKey("glass_theme_introduced")
        val ThemeBeforeGlass = stringPreferencesKey("theme_before_glass")
        val BlueWhiteTheme = booleanPreferencesKey("blue_white_theme")

        /** Retained read-only: the storage value of the theme that used to follow the system. */
        const val LegacyClassicTheme: String = "classic"

        val AmoledBlackTheme = booleanPreferencesKey("amoled_black_theme")
    }
}
