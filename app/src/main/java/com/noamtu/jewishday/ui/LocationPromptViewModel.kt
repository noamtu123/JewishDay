// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noamtu.jewishday.data.AppSettingsRepository
import com.noamtu.jewishday.data.CurrentLocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the app-wide location prompt: pull a fresh location when we can, or fall back to Jerusalem
 * (the app never remembers a past location) when we can't — either just for now, or for good.
 */
@HiltViewModel
class LocationPromptViewModel @Inject constructor(
    private val currentLocationRepository: CurrentLocationRepository,
    private val appSettingsRepository: AppSettingsRepository,
) : ViewModel() {

    /**
     * Whether the user has settled this permanently. Null until the stored value has actually been
     * read — the prompt waits for that rather than guessing, so someone who already chose "always"
     * never sees the dialog flash past on the way to being dismissed.
     */
    val alwaysUseJerusalem: StateFlow<Boolean?> = appSettingsRepository.settings
        .map { settings -> settings.alwaysUseJerusalem }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Location is available: use it, and treat that as revoking any standing "always Jerusalem". */
    fun useCurrentLocation() {
        currentLocationRepository.refreshCurrentLocation()
        if (alwaysUseJerusalem.value == true) {
            viewModelScope.launch { appSettingsRepository.setAlwaysUseJerusalem(false) }
        }
    }

    /** Jerusalem for now, and ask again next time. */
    fun useJerusalemOnce() = currentLocationRepository.useJerusalemFallback()

    /** Jerusalem from now on. Granting location later undoes it — see [useCurrentLocation]. */
    fun useJerusalemAlways() {
        currentLocationRepository.useJerusalemFallback()
        viewModelScope.launch { appSettingsRepository.setAlwaysUseJerusalem(true) }
    }
}
