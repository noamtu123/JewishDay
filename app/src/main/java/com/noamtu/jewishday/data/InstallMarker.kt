// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether this install has ever been launched.
 *
 * Deliberately in SharedPreferences, which is excluded from backup on every channel, so it is
 * always absent right after an install however that install came about. That is what makes it
 * possible to spot the one launch where restored settings need judging.
 */
interface InstallMarker {
    val isFirstLaunchOfInstall: Boolean
    fun markLaunched()
}

@Singleton
class SharedPreferencesInstallMarker @Inject constructor(
    @ApplicationContext context: Context,
) : InstallMarker {
    private val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)

    override val isFirstLaunchOfInstall: Boolean get() = !preferences.getBoolean(LaunchedKey, false)

    override fun markLaunched() = preferences.edit { putBoolean(LaunchedKey, true) }

    private companion object {
        const val PreferencesName = "install_marker"
        const val LaunchedKey = "install_launched"
    }
}
