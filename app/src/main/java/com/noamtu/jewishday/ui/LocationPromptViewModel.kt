// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui

import androidx.lifecycle.ViewModel
import com.noamtu.jewishday.data.CurrentLocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Backs the app-wide location prompt: pull a fresh location when we can, or fall back to Jerusalem
 * (the app never remembers a past location) when we can't.
 */
@HiltViewModel
class LocationPromptViewModel @Inject constructor(
    private val currentLocationRepository: CurrentLocationRepository,
) : ViewModel() {

    fun refreshCurrentLocation() = currentLocationRepository.refreshCurrentLocation()

    fun useJerusalemFallback() = currentLocationRepository.useJerusalemFallback()
}
