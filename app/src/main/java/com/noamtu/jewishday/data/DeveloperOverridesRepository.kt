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
import com.noamtu.jewishday.model.JewishLocation
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * A preset location the developer tools can pin the app to, so diaspora Yom Tov, southern
 * hemisphere seasons, and high-latitude edge cases (where some zmanim are null) can be tested
 * without physically being there.
 */
data class DeveloperLocationPreset(
    val id: String,
    val displayName: String,
    val location: JewishLocation,
)

val DeveloperLocationPresets: List<DeveloperLocationPreset> = listOf(
    DeveloperLocationPreset("jerusalem", "Jerusalem, Israel", JewishLocation("Jerusalem", 31.778, 35.2354, 754.0, ZoneId.of("Asia/Jerusalem"))),
    DeveloperLocationPreset("tel_aviv", "Tel Aviv, Israel", JewishLocation("Tel Aviv", 32.0853, 34.7818, 5.0, ZoneId.of("Asia/Jerusalem"))),
    DeveloperLocationPreset("bnei_brak", "Bnei Brak, Israel", JewishLocation("Bnei Brak", 32.0807, 34.8338, 30.0, ZoneId.of("Asia/Jerusalem"))),
    DeveloperLocationPreset("new_york", "New York, USA", JewishLocation("New York", 40.7128, -74.0060, 10.0, ZoneId.of("America/New_York"))),
    DeveloperLocationPreset("lakewood", "Lakewood, NJ, USA", JewishLocation("Lakewood", 40.0978, -74.2176, 12.0, ZoneId.of("America/New_York"))),
    DeveloperLocationPreset("los_angeles", "Los Angeles, USA", JewishLocation("Los Angeles", 34.0522, -118.2437, 71.0, ZoneId.of("America/Los_Angeles"))),
    DeveloperLocationPreset("london", "London, UK", JewishLocation("London", 51.5074, -0.1278, 11.0, ZoneId.of("Europe/London"))),
    DeveloperLocationPreset("buenos_aires", "Buenos Aires, Argentina", JewishLocation("Buenos Aires", -34.6037, -58.3816, 25.0, ZoneId.of("America/Argentina/Buenos_Aires"))),
    DeveloperLocationPreset("melbourne", "Melbourne, Australia", JewishLocation("Melbourne", -37.8136, 144.9631, 31.0, ZoneId.of("Australia/Melbourne"))),
    DeveloperLocationPreset("stockholm", "Stockholm, Sweden (high latitude)", JewishLocation("Stockholm", 59.3293, 18.0686, 28.0, ZoneId.of("Europe/Stockholm"))),
    DeveloperLocationPreset("anchorage", "Anchorage, USA (high latitude)", JewishLocation("Anchorage", 61.2181, -149.9003, 30.0, ZoneId.of("America/Anchorage"))),
)

fun developerLocationPreset(id: String?): DeveloperLocationPreset? =
    DeveloperLocationPresets.firstOrNull { it.id == id }

/**
 * The current state of the hidden developer tools. All fields default to "off", so a normal
 * user (who never unlocks the tools) behaves exactly as before.
 */
data class DeveloperOverrides(
    // Flipped on by tapping the version number 7x in About; gates visibility of the whole feature.
    val unlocked: Boolean = false,
    val timeOverrideEnabled: Boolean = false,
    // The virtual clock is anchored: at [anchorRealEpochMs] real time it reads [anchorVirtualEpochMs].
    val anchorRealEpochMs: Long = 0L,
    val anchorVirtualEpochMs: Long = 0L,
    // Frozen = the clock stays pinned at the anchor; otherwise virtual time flows from the anchor.
    val timeFrozen: Boolean = true,
    val locationOverrideEnabled: Boolean = false,
    val locationPresetId: String? = null,
) {
    /** The instant the app should treat as "now" given the real [realNow]. */
    fun effectiveInstant(realNow: Instant): Instant {
        if (!timeOverrideEnabled) return realNow
        val virtual = if (timeFrozen) {
            anchorVirtualEpochMs
        } else {
            anchorVirtualEpochMs + (realNow.toEpochMilli() - anchorRealEpochMs)
        }
        return Instant.ofEpochMilli(virtual)
    }

    val overrideLocation: JewishLocation?
        get() = if (locationOverrideEnabled) developerLocationPreset(locationPresetId)?.location else null
}

@Singleton
class DeveloperOverridesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(DeveloperOverrides())
    val state: StateFlow<DeveloperOverrides> = _state.asStateFlow()

    init {
        scope.launch {
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .collect { preferences -> _state.value = decode(preferences) }
        }
    }

    /** Synchronous snapshot for the clock and location seams (read on hot paths). */
    fun snapshot(): DeveloperOverrides = _state.value

    val overrideLocation: JewishLocation? get() = snapshot().overrideLocation

    suspend fun setUnlocked(unlocked: Boolean) = update { it.copy(unlocked = unlocked) }

    /** Turns the virtual clock on/off. On first enable, it anchors to the real current time. */
    suspend fun setTimeOverrideEnabled(enabled: Boolean) = update { current ->
        if (enabled && !current.timeOverrideEnabled) {
            val now = System.currentTimeMillis()
            current.copy(timeOverrideEnabled = true, anchorRealEpochMs = now, anchorVirtualEpochMs = now)
        } else {
            current.copy(timeOverrideEnabled = enabled)
        }
    }

    suspend fun setTimeFrozen(frozen: Boolean) = update { current ->
        // Re-anchor at the moment of toggling so the visible time doesn't jump.
        val virtualNow = current.effectiveInstant(Instant.now()).toEpochMilli()
        current.copy(timeFrozen = frozen, anchorRealEpochMs = System.currentTimeMillis(), anchorVirtualEpochMs = virtualNow)
    }

    /** Sets the virtual clock to [virtualEpochMs], enabling the override if needed. */
    suspend fun setVirtualTime(virtualEpochMs: Long) = update { current ->
        current.copy(
            timeOverrideEnabled = true,
            anchorRealEpochMs = System.currentTimeMillis(),
            anchorVirtualEpochMs = virtualEpochMs,
        )
    }

    /** Shifts the virtual clock by [deltaMs] (e.g. +/- a day or hour). */
    suspend fun shiftVirtualTime(deltaMs: Long) = update { current ->
        val virtualNow = current.effectiveInstant(Instant.now()).toEpochMilli()
        current.copy(
            timeOverrideEnabled = true,
            anchorRealEpochMs = System.currentTimeMillis(),
            anchorVirtualEpochMs = virtualNow + deltaMs,
        )
    }

    suspend fun setLocationOverrideEnabled(enabled: Boolean) = update { current ->
        val presetId = current.locationPresetId ?: DeveloperLocationPresets.first().id
        current.copy(locationOverrideEnabled = enabled, locationPresetId = presetId)
    }

    suspend fun setLocationPreset(id: String) = update { it.copy(locationOverrideEnabled = true, locationPresetId = id) }

    /** Clears every override but keeps the tools unlocked. */
    suspend fun clearOverrides() = update {
        DeveloperOverrides(unlocked = it.unlocked)
    }

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
    )

    private fun encode(preferences: androidx.datastore.preferences.core.MutablePreferences, overrides: DeveloperOverrides) {
        preferences[UnlockedKey] = overrides.unlocked
        preferences[TimeEnabledKey] = overrides.timeOverrideEnabled
        preferences[AnchorRealKey] = overrides.anchorRealEpochMs
        preferences[AnchorVirtualKey] = overrides.anchorVirtualEpochMs
        preferences[TimeFrozenKey] = overrides.timeFrozen
        preferences[LocationEnabledKey] = overrides.locationOverrideEnabled
        overrides.locationPresetId?.let { preferences[LocationPresetKey] = it }
    }

    private companion object {
        val UnlockedKey = booleanPreferencesKey("dev_unlocked")
        val TimeEnabledKey = booleanPreferencesKey("dev_time_enabled")
        val AnchorRealKey = longPreferencesKey("dev_anchor_real")
        val AnchorVirtualKey = longPreferencesKey("dev_anchor_virtual")
        val TimeFrozenKey = booleanPreferencesKey("dev_time_frozen")
        val LocationEnabledKey = booleanPreferencesKey("dev_loc_enabled")
        val LocationPresetKey = stringPreferencesKey("dev_loc_preset")
    }
}