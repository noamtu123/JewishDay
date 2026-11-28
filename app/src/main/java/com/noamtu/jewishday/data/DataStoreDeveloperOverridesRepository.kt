// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.noamtu.jewishday.di.ApplicationScope
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Singleton
class DataStoreDeveloperOverridesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationScope private val scope: CoroutineScope,
) : DeveloperOverridesRepository {
    private val _state = MutableStateFlow(DeveloperOverrides())
    override val state: StateFlow<DeveloperOverrides> = _state.asStateFlow()

    init {
        scope.launch {
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .collect { preferences -> _state.value = decode(preferences) }
        }
    }

    override fun snapshot(): DeveloperOverrides = _state.value

    override suspend fun setUnlocked(unlocked: Boolean) = update { it.copy(unlocked = unlocked) }

    override suspend fun setTimeOverrideEnabled(enabled: Boolean) = update { current ->
        if (enabled && !current.timeOverrideEnabled) {
            val now = System.currentTimeMillis()
            current.copy(timeOverrideEnabled = true, anchorRealEpochMs = now, anchorVirtualEpochMs = now)
        } else {
            current.copy(timeOverrideEnabled = enabled)
        }
    }

    override suspend fun setTimeFrozen(frozen: Boolean) = update { current ->
        // Re-anchor at the moment of toggling so the visible time doesn't jump.
        val virtualNow = current.effectiveInstant(Instant.now()).toEpochMilli()
        current.copy(timeFrozen = frozen, anchorRealEpochMs = System.currentTimeMillis(), anchorVirtualEpochMs = virtualNow)
    }

    override suspend fun setVirtualTime(virtualEpochMs: Long) = update { current ->
        current.copy(
            timeOverrideEnabled = true,
            anchorRealEpochMs = System.currentTimeMillis(),
            anchorVirtualEpochMs = virtualEpochMs,
        )
    }

    override suspend fun shiftVirtualTime(deltaMs: Long) = update { current ->
        val virtualNow = current.effectiveInstant(Instant.now()).toEpochMilli()
        current.copy(
            timeOverrideEnabled = true,
            anchorRealEpochMs = System.currentTimeMillis(),
            anchorVirtualEpochMs = virtualNow + deltaMs,
        )
    }

    override suspend fun setLocationOverrideEnabled(enabled: Boolean) = update { current ->
        val presetId = current.locationPresetId ?: DeveloperLocationPresets.first().id
        current.copy(locationOverrideEnabled = enabled, locationPresetId = presetId)
    }

    override suspend fun setLocationPreset(id: String) = update { it.copy(locationOverrideEnabled = true, locationPresetId = id) }

    override suspend fun setAboutInEnglish(enabled: Boolean) = update { it.copy(aboutInEnglish = enabled) }

    override suspend fun setUpdateNotesInEnglish(enabled: Boolean) = update { it.copy(updateNotesInEnglish = enabled) }

    override suspend fun setCompassMonitoringEnabled(enabled: Boolean) = update { it.copy(compassMonitoringEnabled = enabled) }

    override suspend fun setHideTimeOverrideBanner(hidden: Boolean) = update { it.copy(hideTimeOverrideBanner = hidden) }

    override suspend fun setSpoofedVersionName(versionName: String) =
        update { it.copy(spoofedVersionName = versionName.trim()) }

    override suspend fun current(): DeveloperOverrides = dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map(::decode)
        .first()

    override suspend fun clearOverrides() = update {
        DeveloperOverrides(unlocked = it.unlocked)
    }

    override suspend fun disableDeveloperMode() = update { DeveloperOverrides() }

    private suspend fun update(transform: (DeveloperOverrides) -> DeveloperOverrides) {
        val next = transform(_state.value)
        // Update in-memory first so the clock/location seams see the change immediately,
        // then persist (the DataStore collector will converge to the same value).
        _state.value = next
        dataStore.edit { preferences -> encode(preferences, next) }
    }

    private fun decode(preferences: Preferences): DeveloperOverrides = DeveloperOverrides(
        unlocked = preferences[UnlockedKey] ?: false,
        timeOverrideEnabled = preferences[TimeEnabledKey] ?: false,
        anchorRealEpochMs = preferences[AnchorRealKey] ?: 0L,
        anchorVirtualEpochMs = preferences[AnchorVirtualKey] ?: 0L,
        timeFrozen = preferences[TimeFrozenKey] ?: true,
        locationOverrideEnabled = preferences[LocationEnabledKey] ?: false,
        locationPresetId = preferences[LocationPresetKey],
        aboutInEnglish = preferences[AboutEnglishKey] ?: false,
        updateNotesInEnglish = preferences[UpdateNotesEnglishKey] ?: false,
        compassMonitoringEnabled = preferences[CompassMonitoringKey] ?: false,
        hideTimeOverrideBanner = preferences[HideTimeBannerKey] ?: false,
        spoofedVersionName = preferences[SpoofedVersionKey].orEmpty(),
    )

    private fun encode(preferences: androidx.datastore.preferences.core.MutablePreferences, overrides: DeveloperOverrides) {
        preferences[UnlockedKey] = overrides.unlocked
        preferences[TimeEnabledKey] = overrides.timeOverrideEnabled
        preferences[AnchorRealKey] = overrides.anchorRealEpochMs
        preferences[AnchorVirtualKey] = overrides.anchorVirtualEpochMs
        preferences[TimeFrozenKey] = overrides.timeFrozen
        preferences[LocationEnabledKey] = overrides.locationOverrideEnabled
        // Removed when cleared, or a reset would leave the old preset behind in storage.
        val presetId = overrides.locationPresetId
        if (presetId != null) preferences[LocationPresetKey] = presetId else preferences.remove(LocationPresetKey)
        preferences[AboutEnglishKey] = overrides.aboutInEnglish
        preferences[UpdateNotesEnglishKey] = overrides.updateNotesInEnglish
        preferences[CompassMonitoringKey] = overrides.compassMonitoringEnabled
        preferences[HideTimeBannerKey] = overrides.hideTimeOverrideBanner
        preferences[SpoofedVersionKey] = overrides.spoofedVersionName
    }

    private companion object {
        val UnlockedKey = booleanPreferencesKey("dev_unlocked")
        val TimeEnabledKey = booleanPreferencesKey("dev_time_enabled")
        val AnchorRealKey = longPreferencesKey("dev_anchor_real")
        val AnchorVirtualKey = longPreferencesKey("dev_anchor_virtual")
        val TimeFrozenKey = booleanPreferencesKey("dev_time_frozen")
        val LocationEnabledKey = booleanPreferencesKey("dev_loc_enabled")
        val LocationPresetKey = stringPreferencesKey("dev_loc_preset")
        val AboutEnglishKey = booleanPreferencesKey("dev_about_english")
        val UpdateNotesEnglishKey = booleanPreferencesKey("dev_update_notes_english")
        val CompassMonitoringKey = booleanPreferencesKey("dev_compass_monitoring")
        val HideTimeBannerKey = booleanPreferencesKey("dev_hide_time_banner")
        val SpoofedVersionKey = stringPreferencesKey("dev_spoofed_version")
    }
}
