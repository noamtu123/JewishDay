// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noamtu.jewishday.data.AppSettingsRepository
import com.noamtu.jewishday.notification.DateStatusIconScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives the one-time notification-permission request on first launch. The Hebrew date icon is on by
 * default, so as soon as the user grants notifications we start the foreground service that posts it.
 */
@HiltViewModel
class NotificationSetupViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val dateStatusIconScheduler: DateStatusIconScheduler,
) : ViewModel() {

    /** True until the first-launch notification prompt has been shown once. */
    val needsFirstLaunchPrompt: StateFlow<Boolean> =
        appSettingsRepository.settings
            .map { !it.notificationPermissionRequested }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = false,
            )

    fun onPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.setNotificationPermissionRequested(true)
            // Grant → keep the Hebrew date icon on and start it now; reject → turn it off so we don't
            // sit in a state that can never post. Either way the switch reflects reality.
            appSettingsRepository.setHebrewDateStatusIconEnabled(granted)
            dateStatusIconScheduler.sync(enabled = granted)
        }
    }
}
