package com.turel.jewishdaynext

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turel.jewishdaynext.data.AppSettingsRepository
import com.turel.jewishdaynext.data.AppThemeOption
import com.turel.jewishdaynext.data.CurrentLocationRepository
import com.turel.jewishdaynext.data.StartupSettingsCache
import com.turel.jewishdaynext.data.hasLocationPermission
import com.turel.jewishdaynext.ui.JewishDayApp
import com.turel.jewishdaynext.ui.theme.JewishDayTheme
import com.turel.jewishdaynext.ui.theme.appThemeBackgroundColor
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
}

private fun android.content.res.Resources.isSystemDarkTheme(): Boolean =
    configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
