// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noamtu.jewishday.data.AppSettingsRepository
import com.noamtu.jewishday.data.AppThemeOption
import com.noamtu.jewishday.data.CurrentLocationRepository
import com.noamtu.jewishday.data.RestoredSettingsReconciler
import com.noamtu.jewishday.data.StartupSettingsCache
import com.noamtu.jewishday.data.hasLocationPermission
import com.noamtu.jewishday.i18n.AppLanguageController
import com.noamtu.jewishday.i18n.AppLocales
import com.noamtu.jewishday.ui.JewishDayApp
import com.noamtu.jewishday.ui.ShowDayRequest
import com.noamtu.jewishday.ui.theme.JewishDayTheme
import com.noamtu.jewishday.ui.theme.appThemeBackgroundColor
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
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

    @Inject
    lateinit var appLanguageController: AppLanguageController

    private var startupWindowBackgroundColor: Int? = null

    /**
     * Whether the next time this Activity is shown counts as opening the app. The very first
     * showing does not: the composition below refreshes then, after the first frame, so the pull
     * does not sit in front of the launch. Every later one does — see [onStart] / [onStop].
     */
    private var reopened = false

    // The latest widget tap's day to open on. Taken from the intent that started the activity (not
    // from a recreation, which must not step the screen back to a day the user has moved on from)
    // and from any later one delivered to it; the zmanim screen acts on each request once.
    private var showDayRequest by mutableStateOf<ShowDayRequest?>(null)

    /** Android 12 and older have no per-app language, so the activity takes the app's by hand. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocales.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Three things have to happen before a single setting is read, and all of them block:
        //
        //  - anything a backup restored is judged, so the first launch after a reinstall clears it
        //    while settings carried from another phone are kept (the alternative is the UI reading
        //    settings that are about to be wiped);
        //  - an existing install is moved to the Glass theme, once, before anything below writes a
        //    first setting and makes a new install look like an old one;
        //  - the app's language is settled: on an install's first launch it is set from the settings
        //    (an upgrade, a restore) or the phone's language, so a later system-language change
        //    cannot silently flip it; afterwards the settings just record it (AppLanguageController).
        //
        // They used to be three separate runBlocking calls on the main thread during onCreate —
        // three dispatcher round trips, and each one re-entering DataStore on its own. One block
        // shares a single DataStore read across all of them and leaves one stall instead of three.
        val displayedLanguage = AppLocales.displayTag(resources.configuration)
        var languageChanged = false
        val initialRootSettings = runBlocking(Dispatchers.IO) {
            restoredSettingsReconciler.reconcile()
            appSettingsRepository.introduceGlassTheme()
            languageChanged = appLanguageController.syncOnLaunch(displayed = displayedLanguage)
            // Only pay for the DataStore read when the synchronous cache had nothing. The cache is
            // SharedPreferences, which is excluded from backup, so it is empty in exactly the cases
            // the reconciler above has just acted on. Read after the move to Glass, which updates it.
            startupSettingsCache.read() ?: appSettingsRepository.rootUiSettings.first()
        }
        // Android 13+ recreates the activity for a new per-app language by itself; older versions
        // only read it as the activity is created (see attachBaseContext).
        if (languageChanged && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            recreate()
            return
        }
        setStartupWindowBackground(initialRootSettings.themeOption)
        enableEdgeToEdge()
        // Nor from Recents: reopening a closed task there hands back the intent that first started
        // it, and an old Shabbat-widget tap would step the screen to that day again, days later.
        val fromRecents = (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0
        if (savedInstanceState == null && !fromRecents) showDayRequest = intent.showDayRequest()

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
                JewishDayApp(showDayRequest = showDayRequest)
            }
        }
    }

    /**
     * Every opening of the app pulls the location again, so the zmanim are for where the phone is
     * now rather than where it was when the app was last launched. Forced, because the point is to
     * bypass the repository's own five-minute throttle on fresh fixes: coming back to the app after
     * travelling is exactly when the cached fix is the wrong one.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.showDayRequest()?.let { showDayRequest = it }
    }

    override fun onStart() {
        super.onStart()
        if (!reopened) return
        reopened = false
        if (hasLocationPermission()) {
            currentLocationRepository.refreshCurrentLocation(force = true)
        }
    }

    override fun onStop() {
        super.onStop()
        // A rotation stops and restarts the Activity without the app ever leaving the foreground,
        // and the recreated composition does its own refresh, so it is not a reopening.
        if (!isChangingConfigurations) reopened = true
    }

    private fun setStartupWindowBackground(themeOption: AppThemeOption) {
        val backgroundColor = appThemeBackgroundColor(themeOption)
        if (startupWindowBackgroundColor == backgroundColor) return
        startupWindowBackgroundColor = backgroundColor
        window.setBackgroundDrawable(backgroundColor.toDrawable())
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

    companion object {
        private const val ExtraShowDate = "com.noamtu.jewishday.extra.SHOW_DATE"

        /** Opens the app afresh on the zmanim of [date] (see [ShowDayRequest]). */
        fun showDayIntent(context: Context, date: LocalDate): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ExtraShowDate, date.toString())

        private fun Intent.showDayRequest(): ShowDayRequest? = getStringExtra(ExtraShowDate)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?.let(::ShowDayRequest)
    }
}
