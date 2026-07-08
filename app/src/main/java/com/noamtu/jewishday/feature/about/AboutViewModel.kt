// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noamtu.jewishday.data.DeveloperOverridesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val developerOverridesRepository: DeveloperOverridesRepository,
) : ViewModel() {
    val developerModeUnlocked: StateFlow<Boolean> = developerOverridesRepository.state
        .map { it.unlocked }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Developer override: force the About page into English regardless of the app language. */
    val aboutInEnglish: StateFlow<Boolean> = developerOverridesRepository.state
        .map { it.aboutInEnglish }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun unlockDeveloperMode() {
        viewModelScope.launch { developerOverridesRepository.setUnlocked(true) }
    }
}