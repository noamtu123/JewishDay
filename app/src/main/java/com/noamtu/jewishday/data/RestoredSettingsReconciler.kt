// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** What the first launch of an install found waiting for it. */
enum class RestoreOutcome {
    /** Nothing was restored, or this is not the first launch of the install. */
    Nothing,

    /** The backup came from this same phone, so it was a reinstall: the settings were cleared. */
    ClearedAfterReinstall,

    /** The backup came from another phone: the settings were kept. */
    KeptFromAnotherDevice,
}

/**
 * Decides what to do with settings that arrived from a backup.
 *
 * They are backed up so a new phone keeps them — restored from Google's backup or migrated
 * directly, both work. Android offers no way to have that without also restoring them on a plain
 * reinstall, which should start fresh, and it cannot tell the two apart: to the system they are the
 * same restore.
 *
 * The app can, because the settings carry a marker naming the device that wrote them:
 *
 * - marker names this phone → the backup came from here, so this is a reinstall → clear it
 * - marker names another phone → the settings were carried here → keep them, adopt the marker
 * - no marker → nothing was restored → write one
 *
 * All of which is only asked on the first launch of an install; on every later launch the marker
 * naturally matches and nothing happens. [InstallMarker] is what says which launch that is, and it
 * lives outside the backup so it is always missing right after an install.
 *
 * A factory reset changes the identifier, so a reset phone restored from its own backup keeps its
 * settings. From the user's side that is the same as a new phone, and treating it that way is the
 * friendlier reading.
 */
@Singleton
class RestoredSettingsReconciler @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val deviceIdentity: DeviceIdentity,
    private val installMarker: InstallMarker,
) {

    /** Run once at startup, before anything reads the settings. */
    suspend fun reconcile(): RestoreOutcome {
        if (!installMarker.isFirstLaunchOfInstall) return RestoreOutcome.Nothing
        installMarker.markLaunched()

        // Without an identifier there is no way to judge, and wrongly wiping someone's settings is
        // far worse than wrongly keeping them.
        val thisDevice = deviceIdentity.id ?: return RestoreOutcome.Nothing
        val stored = dataStore.data.first()[DeviceMarker]

        return when (stored) {
            null -> {
                // A clean install, or a restore from a build that predates the marker. Either way
                // there is nothing trustworthy to act on, so just claim it for this device.
                dataStore.edit { it[DeviceMarker] = thisDevice }
                RestoreOutcome.Nothing
            }

            thisDevice -> {
                dataStore.edit { preferences ->
                    preferences.asMap().keys.toList().forEach { key -> preferences.remove(key) }
                    preferences[DeviceMarker] = thisDevice
                }
                RestoreOutcome.ClearedAfterReinstall
            }

            else -> {
                dataStore.edit { it[DeviceMarker] = thisDevice }
                RestoreOutcome.KeptFromAnotherDevice
            }
        }
    }

    private companion object {
        val DeviceMarker = stringPreferencesKey("settings_device_marker")
    }
}
