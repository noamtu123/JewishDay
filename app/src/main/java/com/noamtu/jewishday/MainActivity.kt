// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.noamtu.jewishday.data.RestoredSettingsReconciler
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

    @Inject
    lateinit var restoredSettingsReconciler: RestoredSettingsReconciler

    private var startupWindowBackgroundColor: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Two things have to happen before a single setting is read, and both block:
        //
        //  - anything a backup restored is judged, so the first launch after a reinstall clears it
        //    while settings carried from another phone are kept (the alternative is the UI reading
        //    settings that are about to be wiped);
        //  - the system-language default is persisted, so a later system-language change cannot
        //    silently flip the in-app language on the next launch.
        //
        // They used to be three separate runBlocking calls on the main thread during onCreate —
        // three dispatcher round trips, and each one re-entering DataStore on its own. One block
        // shares a single DataStore read across all of them and leaves one stall instead of three.
        val cachedRootSettings = startupSettingsCache.read()
        val initialRootSettings = runBlocking(Dispatchers.IO) {
            restoredSettingsReconciler.reconcile()
            appSettingsRepository.seedLanguageDefault()
            // Only pay for the DataStore read when the synchronous cache had nothing. The cache is
            // SharedPreferences, which is excluded from backup, so it is empty in exactly the cases
            // the reconciler above has just acted on.
            cachedRootSettings ?: appSettingsRepository.rootUiSettings.first()
        }
        setStartupWindowBackground(initialRootSettings.themeOption)
        enableEdgeToEdge()

        setContent {
            val rootSettings by appSettingsRepository.rootUiSettings.collectAsStateWithLifecycle(
                initialValue = initialRootSettings,
            )
            SideEffect {
                setStartupWindowBackground(rootSettings.themeOption)
                applySystemBarIconAppearance(rootSettings.themeOption)
            }

            LaunchedEffect(Unit) {
                withFrameNanos { }
                if (hasLocationPermission()) {
                    currentLocationRepository.refreshCurrentLocation()
                }
            }

            JewishDayTheme(themeOption = rootSettings.themeOption) {
                JewishDayApp(useHebrewInterface = rootSettings.useHebrewInterface)
            }
        }
    }

    private fun setStartupWindowBackground(themeOption: AppThemeOption) {
        val backgroundColor = appThemeBackgroundColor(themeOption)
        if (startupWindowBackgroundColor == backgroundColor) return
        startupWindowBackgroundColor = backgroundColor
        window.setBackgroundDrawable(
            ColorDrawable(backgroundColor),
        )
    }

    // Status/navigation bar icons contrast with the app's own theme background. No theme follows
    // the system dark-mode flag, so the flag is not consulted here either: a light theme on a
    // dark-mode phone needs dark icons, or the white system icons vanish on the white background.
    private fun applySystemBarIconAppearance(themeOption: AppThemeOption) {
        val backgroundColor = appThemeBackgroundColor(themeOption)
        val lightBackground = ColorUtils.calculateLuminance(backgroundColor) > 0.5
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = lightBackground
        controller.isAppearanceLightNavigationBars = lightBackground
    }
}
