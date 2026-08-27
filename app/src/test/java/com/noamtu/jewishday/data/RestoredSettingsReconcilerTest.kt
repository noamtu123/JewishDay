// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The settings are backed up so a new phone keeps them, which means a plain reinstall would get
 * them back too. These pin the rule that tells those apart.
 */
class RestoredSettingsReconcilerTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val theme = stringPreferencesKey("theme_option")
    private val marker = stringPreferencesKey("settings_device_marker")

    private fun store(scope: CoroutineScope) = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { File(temporaryFolder.newFolder(), "settings.preferences_pb") },
    )

    private fun reconciler(
        dataStore: DataStore<Preferences>,
        device: String?,
        firstLaunch: Boolean = true,
    ) = RestoredSettingsReconciler(dataStore, FakeDeviceIdentity(device), FakeInstallMarker(firstLaunch))

    @Test
    fun aReinstallOnTheSamePhoneStartsFresh() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = store(scope)
        // What a restore leaves behind: settings, stamped with the phone that wrote them.
        dataStore.edit {
            it[theme] = "midnight"
            it[marker] = "phone-a"
        }

        val outcome = reconciler(dataStore, device = "phone-a").reconcile()

        assertEquals(RestoreOutcome.ClearedAfterReinstall, outcome)
        assertNull(dataStore.data.first()[theme])
        // The marker is rewritten, so the next reinstall is recognised too.
        assertEquals("phone-a", dataStore.data.first()[marker])
        scope.cancel()
    }

    @Test
    fun settingsCarriedFromAnotherPhoneAreKept() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = store(scope)
        dataStore.edit {
            it[theme] = "midnight"
            it[marker] = "old-phone"
        }

        val outcome = reconciler(dataStore, device = "new-phone").reconcile()

        assertEquals(RestoreOutcome.KeptFromAnotherDevice, outcome)
        assertEquals("midnight", dataStore.data.first()[theme])
        // Adopted, so a later reinstall on the new phone is a reinstall.
        assertEquals("new-phone", dataStore.data.first()[marker])
        scope.cancel()
    }

    @Test
    fun anOrdinaryLaunchNeverTouchesAnything() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = store(scope)
        dataStore.edit {
            it[theme] = "midnight"
            it[marker] = "phone-a"
        }

        // The marker matches this phone on every later launch too, so without the install check
        // this is exactly where settings would be wiped on every single start.
        val outcome = reconciler(dataStore, device = "phone-a", firstLaunch = false).reconcile()

        assertEquals(RestoreOutcome.Nothing, outcome)
        assertEquals("midnight", dataStore.data.first()[theme])
        scope.cancel()
    }

    @Test
    fun aCleanInstallJustClaimsTheDevice() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = store(scope)

        val outcome = reconciler(dataStore, device = "phone-a").reconcile()

        assertEquals(RestoreOutcome.Nothing, outcome)
        assertEquals("phone-a", dataStore.data.first()[marker])
        scope.cancel()
    }

    @Test
    fun withoutADeviceIdNothingIsTouched() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = store(scope)
        dataStore.edit {
            it[theme] = "midnight"
            it[marker] = "phone-a"
        }

        // Wrongly wiping someone's settings is worse than wrongly keeping them.
        val outcome = reconciler(dataStore, device = null).reconcile()

        assertEquals(RestoreOutcome.Nothing, outcome)
        assertEquals("midnight", dataStore.data.first()[theme])
        scope.cancel()
    }

    @Test
    fun everySettingGoesOnAReinstallNotJustTheKnownOnes() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = store(scope)
        val somethingElse = booleanPreferencesKey("some_future_setting")
        dataStore.edit {
            it[somethingElse] = true
            it[marker] = "phone-a"
        }

        reconciler(dataStore, device = "phone-a").reconcile()

        assertNull(dataStore.data.first()[somethingElse])
        scope.cancel()
    }
}

private class FakeDeviceIdentity(override val id: String?) : DeviceIdentity

private class FakeInstallMarker(private var firstLaunch: Boolean) : InstallMarker {
    override val isFirstLaunchOfInstall: Boolean get() = firstLaunch

    override fun markLaunched() {
        firstLaunch = false
    }
}
