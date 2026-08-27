// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The update check runs at launch, before the background collector that fills [
 * DeveloperOverridesRepository.snapshot] has had a chance to read DataStore. Reading the snapshot
 * there saw the defaults every time, so a spoofed version was silently ignored and no update was
 * ever offered. `current()` waits for the stored value instead.
 */
class DeveloperOverridesReadTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun currentReadsTheStoredValueWithoutWaitingOnTheCachedSnapshot() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(temporaryFolder.newFolder(), "developer.preferences_pb") },
        )

        DeveloperOverridesRepository(dataStore, scope).setSpoofedVersionName("0.5.0")

        // A repository built fresh, exactly as a cold launch builds one: its cached snapshot is
        // still at the defaults, while the stored override is there to be read.
        val reader = DeveloperOverridesRepository(dataStore, scope)
        assertEquals("", reader.snapshot().spoofedVersionName)
        assertEquals("0.5.0", reader.current().spoofedVersionName)

        scope.cancel()
    }

    @Test
    fun anEmptySpoofMeansUseTheRealVersion() = runBlocking {
        val file = File(temporaryFolder.newFolder(), "empty.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val repository = DeveloperOverridesRepository(
            PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }),
            scope,
        )

        assertEquals("", repository.current().spoofedVersionName)
        scope.cancel()
    }
}
