// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.i18n

import android.content.Context
import android.os.Build
import com.noamtu.jewishday.data.AppSettingsRepository
import com.noamtu.jewishday.notification.DateStatusIconScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** The per-app language as [AppLanguageController] works with it: Android's on a phone, a fake in tests. */
interface PerAppLanguage {
    /** The languages the app has, the default first. */
    val supported: List<String>

    /** The app's own language, or null when it follows the phone's. */
    fun current(): String?

    fun set(tag: String)

    /** Whether this install's first launch has settled the language. */
    fun isInitialized(): Boolean

    fun markInitialized()

    /** The language the phone itself would show the app in. */
    fun systemLanguage(): String
}

/** [PerAppLanguage] as [AppLocales] keeps it. */
class AndroidPerAppLanguage @Inject constructor(
    @ApplicationContext private val context: Context,
) : PerAppLanguage {
    override val supported: List<String> get() = AppLocales.supported

    override fun current(): String? = AppLocales.applicationLocale(context)

    override fun set(tag: String) {
        AppLocales.setApplicationLocale(context, tag)
        // Android 13+ tells the app with a locale broadcast, which redraws the status-bar icon (see
        // DateStatusIconBootReceiver); older versions have no such broadcast for an app's own
        // language. The widgets follow the settings' record.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) DateStatusIconScheduler.refresh(context)
    }

    override fun isInitialized(): Boolean = AppLocales.isInitialized(context)

    override fun markInitialized() = AppLocales.markInitialized(context)

    override fun systemLanguage(): String = AppLocales.systemLanguage()
}

/**
 * Keeps the app's language — Android's per-app language, see [AppLocales] — and the settings' record
 * of it ([AppSettingsRepository.setAppLanguage]) in step.
 *
 * The per-app language is the truth: it is what the picker in Settings sets, and on Android 13+ it
 * is just as much what the system's own language setting sets. The record exists so the choice
 * travels with the rest of the settings to a new phone, so an install upgraded from a version that
 * kept the language only in its settings starts out in that language, and so the widgets, which
 * follow the settings, are redrawn when it changes.
 */
@Singleton
class AppLanguageController @Inject constructor(
    private val language: PerAppLanguage,
    private val settings: AppSettingsRepository,
) {
    /**
     * The user picked [tag] in the app's own Settings: it is the app's language from now on. Takes
     * effect at once — Android 13+ updates the running app itself, and on older versions the caller
     * recreates the activity, which then reads it.
     */
    fun select(tag: String) {
        language.set(tag)
    }

    /** Records [tag] in the settings, after [select]. */
    suspend fun record(tag: String) {
        settings.setAppLanguage(tag)
    }

    /**
     * Run at every launch, once restored settings have been judged (see `RestoredSettingsReconciler`).
     *
     * The first launch of an install settles the language: an upgraded install, or one restored onto
     * a new phone, takes the language its settings record; a new one takes the phone's language —
     * and keeps it, so a later change of the phone's language does not silently change the app's,
     * exactly as before the app had a per-app language. After that the record only follows the
     * per-app language, including any change made in the system settings.
     *
     * Returns whether the language it settled on differs from [displayed], the one the calling
     * activity was created in. Android 13+ then recreates the activity itself; on older versions the
     * caller does. Either happens once at most, on that first launch.
     */
    suspend fun syncOnLaunch(displayed: String): Boolean {
        val applied = language.current()
        val recorded = settings.settings.first().language
        if (!language.isInitialized()) {
            val settled = applied ?: recorded?.takeIf { it in language.supported } ?: language.systemLanguage()
            if (applied == null) language.set(settled)
            settings.setAppLanguage(settled)
            language.markInitialized()
            return settled != displayed
        }
        if (applied != recorded) {
            // Null when "System default" was chosen in the system settings: nothing to carry over.
            settings.setAppLanguage(applied)
        }
        return false
    }
}
