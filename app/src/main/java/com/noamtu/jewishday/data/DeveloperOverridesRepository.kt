// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import androidx.datastore.core.DataStore
import com.noamtu.jewishday.core.model.JewishLocation
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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
    // Hides the red "times are simulated" banner on the Zmanim screen while the clock is overridden,
    // for screenshots or for looking at the screen as a user would see it.
    val hideTimeOverrideBanner: Boolean = false,
    val locationOverrideEnabled: Boolean = false,
    val locationPresetId: String? = null,
    // Forces the About page to render in English regardless of the app language, so the English
    // copy can be checked without switching the whole interface.
    val aboutInEnglish: Boolean = false,
    // The same, for the "What's new" section of the update dialog: a release body carries both
    // languages, and this shows the English one while the rest of the app stays in Hebrew.
    val updateNotesInEnglish: Boolean = false,
    // Shows a live sensor/quality diagnostics panel on the Prayer Compass screen.
    val compassMonitoringEnabled: Boolean = false,
    // Makes the update check believe this is the installed version, so any release newer than it
    // looks like an update and the whole flow — check, download, install — runs against GitHub for
    // real. Blank means "use the real version".
    val spoofedVersionName: String = "",
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

/**
 * The hidden developer tools' overrides (see [DeveloperOverrides]): read synchronously by the clock
 * and location seams, observed by the screens, and changed only from the developer screen.
 */
interface DeveloperOverridesRepository {
    val state: StateFlow<DeveloperOverrides>

    /** Synchronous snapshot for the clock and location seams (read on hot paths). */
    fun snapshot(): DeveloperOverrides

    /** The pinned location, when the location override is on. */
    val overrideLocation: JewishLocation? get() = snapshot().overrideLocation

    suspend fun setUnlocked(unlocked: Boolean)

    /** Turns the virtual clock on/off. On first enable, it anchors to the real current time. */
    suspend fun setTimeOverrideEnabled(enabled: Boolean)

    suspend fun setTimeFrozen(frozen: Boolean)

    /** Sets the virtual clock to [virtualEpochMs], enabling the override if needed. */
    suspend fun setVirtualTime(virtualEpochMs: Long)

    /** Shifts the virtual clock by [deltaMs] (e.g. +/- a day or hour). */
    suspend fun shiftVirtualTime(deltaMs: Long)

    suspend fun setLocationOverrideEnabled(enabled: Boolean)

    suspend fun setLocationPreset(id: String)

    suspend fun setAboutInEnglish(enabled: Boolean)

    suspend fun setUpdateNotesInEnglish(enabled: Boolean)

    suspend fun setCompassMonitoringEnabled(enabled: Boolean)

    suspend fun setHideTimeOverrideBanner(hidden: Boolean)

    suspend fun setSpoofedVersionName(versionName: String)

    /**
     * The stored overrides, waiting for the first read rather than using [snapshot].
     *
     * [snapshot] starts life at the defaults and is filled in once DataStore emits, so anything
     * running at launch — the update check does — would otherwise read "off" for every override.
     */
    suspend fun current(): DeveloperOverrides

    /** Clears every override but keeps the tools unlocked. */
    suspend fun clearOverrides()

    /**
     * Turns the tools off altogether: every override cleared *and* the feature locked again, so the
     * section disappears and only another 7 taps on the version brings it back. Leaving an override
     * behind would keep a spoofed clock or location running with nothing on screen admitting to it.
     */
    suspend fun disableDeveloperMode()
}
