// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noamtu.jewishday.data.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the one-time note telling an existing user the app moved them to the Glass theme (see
 * [AppSettingsRepository.introduceGlassTheme]).
 */
@HiltViewModel
class GlassThemeNoticeViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
) : ViewModel() {

    /** Whether the note is still to be read. */
    val pending: StateFlow<Boolean> = appSettingsRepository.settings
        .map { settings -> settings.themeBeforeGlass != null }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun dismiss() {
        viewModelScope.launch { appSettingsRepository.dismissGlassThemeNotice() }
    }
}
