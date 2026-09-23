// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noamtu.jewishday.BuildConfig
import com.noamtu.jewishday.update.AppVersion
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

    /**
     * The version this page shows. Normally the real one — but while the developer tools are
     * spoofing a version it shows that instead, marked, so the spoof is visibly in effect rather
     * than something you have to take on faith after no update dialog appears.
     */
    val displayedVersion: StateFlow<String> = developerOverridesRepository.state
        .map { overrides ->
            overrides.spoofedVersionName
                .takeIf { it.isNotBlank() }
                ?.let { spoofed -> "$spoofed (spoofed)" }
                ?: RealVersionLabel
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RealVersionLabel)

    fun unlockDeveloperMode() {
        viewModelScope.launch { developerOverridesRepository.setUnlocked(true) }
    }

}

/** This build's version as people see it — `pre-1.1.1` rather than `1.1.1-pre.1`. */
private val RealVersionLabel: String =
    AppVersion.parse(BuildConfig.VERSION_NAME)?.displayName ?: BuildConfig.VERSION_NAME
