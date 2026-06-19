package com.noamtu.jewishday

import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noamtu.jewishday.data.AppSettingsRepository
import com.noamtu.jewishday.data.AppThemeOption
import com.noamtu.jewishday.data.CurrentLocationRepository
import com.noamtu.jewishday.data.StartupSettingsCache
import com.noamtu.jewishday.data.hasLocationPermission
import com.noamtu.jewishday.ui.JewishDayApp
import com.noamtu.jewishday.ui.theme.JewishDayTheme
import com.noamtu.jewishday.ui.theme.appThemeBackgroundColor
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var appSettingsRepository: AppSettingsRepository

    @Inject
    lateinit var currentLocationRepository: CurrentLocationRepository

    @Inject
    lateinit var startupSettingsCache: StartupSettingsCache

    private var startupWindowBackgroundColor: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Persist the system-language default before any settings are read, so that a later
        // system-language change cannot silently flip the in-app language on next launch.
        runBlocking(Dispatchers.IO) { appSettingsRepository.seedLanguageDefault() }
        val initialRootSettings = startupSettingsCache.read() ?: runBlocking(Dispatchers.IO) {
            appSettingsRepository.rootUiSettings.first()
        }
        setStartupWindowBackground(
            themeOption = initialRootSettings.themeOption,
            darkTheme = resources.isSystemDarkTheme(),
        )
        enableEdgeToEdge()

        setContent {
            val rootSettings by appSettingsRepository.rootUiSettings.collectAsStateWithLifecycle(
                initialValue = initialRootSettings,
            )
            val darkTheme = isSystemInDarkTheme()

            SideEffect {
                setStartupWindowBackground(
                    themeOption = rootSettings.themeOption,
                    darkTheme = darkTheme,
                )
                applySystemBarIconAppearance(
                    themeOption = rootSettings.themeOption,
                    darkTheme = darkTheme,
                )
            }

            LaunchedEffect(Unit) {
                withFrameNanos { }
                if (hasLocationPermission()) {
                    currentLocationRepository.refreshCurrentLocation()
                }
            }

            JewishDayTheme(
                darkTheme = darkTheme,
                themeOption = rootSettings.themeOption,
            ) {
                JewishDayApp(useHebrewInterface = rootSettings.useHebrewInterface)
            }
        }
    }

    private fun setStartupWindowBackground(themeOption: AppThemeOption, darkTheme: Boolean) {
        val backgroundColor = appThemeBackgroundColor(themeOption = themeOption, darkTheme = darkTheme)
        if (startupWindowBackgroundColor == backgroundColor) return
        startupWindowBackgroundColor = backgroundColor
        window.setBackgroundDrawable(
            ColorDrawable(backgroundColor),
        )
    }

    // Status/navigation bar icons must contrast with the app's *theme* background, not
    // the system dark-mode flag: a light theme (e.g. Blue White) on a dark-mode phone
    // needs dark icons, otherwise the white system icons vanish on the white background.
    private fun applySystemBarIconAppearance(themeOption: AppThemeOption, darkTheme: Boolean) {
        val backgroundColor = appThemeBackgroundColor(themeOption = themeOption, darkTheme = darkTheme)
        val lightBackground = ColorUtils.calculateLuminance(backgroundColor) > 0.5
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = lightBackground
        controller.isAppearanceLightNavigationBars = lightBackground
    }
}

private fun android.content.res.Resources.isSystemDarkTheme(): Boolean =
    configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
