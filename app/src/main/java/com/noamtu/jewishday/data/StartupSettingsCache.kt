// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface StartupSettingsCache {
    fun read(): RootUiSettings?
    fun write(settings: RootUiSettings)
}

@Singleton
class SharedPreferencesStartupSettingsCache @Inject constructor(
    @ApplicationContext context: Context,
) : StartupSettingsCache {
    private val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)

    override fun read(): RootUiSettings? {
        val themeOption = preferences.getString(ThemeOptionKey, null) ?: return null
        return RootUiSettings(
            themeOption = AppThemeOption.fromStorageValue(themeOption)
                ?: AppThemeOption.Default,
            language = decodeLanguage(),
        )
    }

    private fun decodeLanguage(): AppLanguage =
        AppLanguage.fromStorageValue(preferences.getString(LanguageKey, null))
        // Migrate caches written before the language picker existed.
            ?: if (preferences.contains(LegacyHebrewKey)) {
                if (preferences.getBoolean(LegacyHebrewKey, false)) AppLanguage.Hebrew else AppLanguage.English
            } else {
                AppLanguage.systemDefault()
            }

    override fun write(settings: RootUiSettings) {
        preferences.edit()
            .putString(ThemeOptionKey, settings.themeOption.storageValue)
            .putString(LanguageKey, settings.language.storageValue)
            .remove(LegacyHebrewKey)
            .apply()
    }

    private companion object {
        const val PreferencesName = "startup_settings"
        const val ThemeOptionKey = "theme_option"
        const val LanguageKey = "app_language"
        const val LegacyHebrewKey = "use_hebrew_interface"
    }
}