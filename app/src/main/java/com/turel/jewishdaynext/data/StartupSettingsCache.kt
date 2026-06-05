package com.turel.jewishdaynext.data

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
                ?: AppThemeOption.Classic,
            useHebrewInterface = preferences.getBoolean(UseHebrewInterfaceKey, false),
        )
    }

    override fun write(settings: RootUiSettings) {
        preferences.edit()
            .putString(ThemeOptionKey, settings.themeOption.storageValue)
            .putBoolean(UseHebrewInterfaceKey, settings.useHebrewInterface)
            .apply()
    }

    private companion object {
        const val PreferencesName = "startup_settings"
        const val ThemeOptionKey = "theme_option"
        const val UseHebrewInterfaceKey = "use_hebrew_interface"
    }
}
