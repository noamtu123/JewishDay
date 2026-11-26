// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.noamtu.jewishday.data.DataStoreAppSettingsRepository
import com.noamtu.jewishday.data.DataStoreDeveloperOverridesRepository
import java.io.File
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * The app's real settings and developer-override repositories over one DataStore file — as in the
 * app, where both share `app_settings` — so a view model test exercises what is actually stored.
 */
class TestStores(folder: TemporaryFolder) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope) {
        File(folder.newFolder(), "app_settings.preferences_pb")
    }
    val settings = DataStoreAppSettingsRepository(dataStore, NoStartupCache)
    val developerOverrides = DataStoreDeveloperOverridesRepository(dataStore, scope)

    override fun close() = scope.cancel()
}

/** Runs `viewModelScope` (which uses the main dispatcher) on the test's own thread, at once. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(UnconfinedTestDispatcher())

    override fun finished(description: Description) = Dispatchers.resetMain()
}

/** The first value that satisfies [predicate], failing rather than hanging if none comes. */
suspend fun <T> Flow<T>.awaitFirst(predicate: (T) -> Boolean): T = withTimeout(10.seconds) { first(predicate) }

/** Waits until [condition] holds, for work a view model launched that offers nothing to observe. */
suspend fun eventually(condition: () -> Boolean) = withTimeout(10.seconds) {
    while (!condition()) delay(10)
}
