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
        )
    }

    override fun write(settings: RootUiSettings) {
        preferences.edit()
            .putString(ThemeOptionKey, settings.themeOption.storageValue)
            // The language left this cache for Android's per-app language; drop what earlier
            // versions kept here.
            .remove(LanguageKey)
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