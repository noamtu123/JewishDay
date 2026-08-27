// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether this install has already shown the one-time notification-permission request.
 *
 * Deliberately kept out of the settings DataStore. The DataStore lives under `files/` and is
 * included in Android's cloud backup, so it is restored on reinstall — but runtime permissions are
 * never restored. A restored "we already asked" would leave the app certain it had prompted on an
 * install where the OS had never been asked, permanently suppressing the request. SharedPreferences
 * is excluded from backup (see `backup_rules.xml` / `data_extraction_rules.xml`), which is exactly
 * the right lifetime for this: it belongs to the install, not to the user's settings.
 */
interface NotificationPromptState {
    val requested: StateFlow<Boolean>
    fun markRequested()
}

@Singleton
class SharedPreferencesNotificationPromptState @Inject constructor(
    @ApplicationContext context: Context,
) : NotificationPromptState {
    private val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(preferences.getBoolean(RequestedKey, false))

    override val requested: StateFlow<Boolean> = state.asStateFlow()

    override fun markRequested() {
        preferences.edit { putBoolean(RequestedKey, true) }
        state.value = true
    }

    private companion object {
        const val PreferencesName = "notification_prompt"
        const val RequestedKey = "notification_permission_requested"
    }
}
