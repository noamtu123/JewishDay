// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noamtu.jewishday.data.AppSettingsRepository
import com.noamtu.jewishday.data.NotificationPromptState
import com.noamtu.jewishday.notification.DateStatusIconScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives the one-time notification-permission request on first launch, and keeps the Hebrew date
 * icon setting honest on every later launch. The icon is on by default, so as soon as the user
 * grants notifications we start the foreground service that posts it.
 */
@HiltViewModel
class NotificationSetupViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val notificationPromptState: NotificationPromptState,
    private val dateStatusIconScheduler: DateStatusIconScheduler,
) : ViewModel() {

    /** True until the first-launch notification prompt has been shown once. */
    val needsFirstLaunchPrompt: StateFlow<Boolean> =
        notificationPromptState.requested
            .map { !it }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = false,
            )

    fun onPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            notificationPromptState.markRequested()
            // Grant → keep the Hebrew date icon on and start it now; reject → turn it off so we don't
            // sit in a state that can never post. Either way the switch reflects reality.
            appSettingsRepository.setHebrewDateStatusIconEnabled(granted)
            dateStatusIconScheduler.sync(enabled = granted)
        }
    }

    /**
     * Run on every foreground. Enforces the invariant "no notification permission → the icon
     * setting is off", and re-starts the service whenever the setting is on and allowed (the
     * service does not survive an app upgrade, a force-stop, or the OS reclaiming it).
     *
     * Deliberately one-directional: granting the permission from system settings never flips a
     * switch the user left off. Only the Settings toggle turns it back on.
     */
    fun reconcile(hasPermission: Boolean) {
        viewModelScope.launch {
            val enabled = appSettingsRepository.settings.first().hebrewDateStatusIconEnabled
            if (!enabled) return@launch
            if (hasPermission) {
                dateStatusIconScheduler.sync(enabled = true)
            } else {
                appSettingsRepository.setHebrewDateStatusIconEnabled(false)
                dateStatusIconScheduler.sync(enabled = false)
            }
        }
    }
}
