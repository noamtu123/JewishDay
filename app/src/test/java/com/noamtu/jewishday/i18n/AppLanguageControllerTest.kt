// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.i18n

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.noamtu.jewishday.data.DataStoreAppSettingsRepository
import com.noamtu.jewishday.data.RootUiSettings
import com.noamtu.jewishday.data.StartupSettingsCache
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The move from a language kept in the app's own settings to Android's per-app language: every
 * existing install must come out in the language it was in, and after that the per-app language —
 * which the system settings can change too — leads.
 */
class AppLanguageControllerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private class FakeLanguage(
        var applied: String? = null,
        var initialized: Boolean = false,
        private val system: String = "en",
    ) : PerAppLanguage {
        override val supported = listOf("en", "he")
        override fun current() = applied
        override fun set(tag: String) {
            applied = tag
        }
        override fun isInitialized() = initialized
        override fun markInitialized() {
            initialized = true
        }
        override fun systemLanguage() = system
    }

    private fun scenario(
        language: FakeLanguage,
        stored: suspend (DataStore<Preferences>) -> Unit = {},
        block: suspend (AppLanguageController, DataStoreAppSettingsRepository, DataStore<Preferences>) -> Unit,
    ) = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            File(temporaryFolder.newFolder(), "settings.preferences_pb")
        }
        stored(dataStore)
        val repository = DataStoreAppSettingsRepository(dataStore, NoStartupCache)
        try {
            block(AppLanguageController(language, repository), repository, dataStore)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun anUpgradedHebrewInstallOnAnEnglishPhoneStaysHebrew() {
        val language = FakeLanguage(system = "en")
        scenario(language, stored = { it.edit { preferences -> preferences[LanguageKey] = "hebrew" } }) { controller, repository, _ ->
            // The activity came up in the phone's English; it has to be recreated in Hebrew.
            assertTrue(controller.syncOnLaunch(displayed = "en"))
            assertEquals("he", language.applied)
            assertEquals("he", repository.settings.first().language)
            assertTrue(language.initialized)
        }
    }

    @Test
    fun anUpgradedEnglishInstallOnAHebrewPhoneStaysEnglish() {
        val language = FakeLanguage(system = "he")
        scenario(language, stored = { it.edit { preferences -> preferences[LanguageKey] = "english" } }) { controller, repository, _ ->
            assertTrue(controller.syncOnLaunch(displayed = "he"))
            assertEquals("en", language.applied)
            assertEquals("en", repository.settings.first().language)
        }
    }

    @Test
    fun theOldestInstallsKeptOnlyAHebrewSwitch() {
        val language = FakeLanguage(system = "en")
        scenario(language, stored = { it.edit { preferences -> preferences[LegacyHebrewKey] = true } }) { controller, repository, _ ->
            controller.syncOnLaunch(displayed = "en")
            assertEquals("he", language.applied)
            assertEquals("he", repository.settings.first().language)
        }
    }

    @Test
    fun aNewInstallTakesThePhonesLanguageAndKeepsIt() {
        val language = FakeLanguage(system = "he")
        scenario(language) { controller, repository, _ ->
            // Already showing Hebrew, so nothing needs recreating.
            assertFalse(controller.syncOnLaunch(displayed = "he"))
            // Set explicitly, so a later change of the phone's language does not change the app's.
            assertEquals("he", language.applied)
            assertEquals("he", repository.settings.first().language)
        }
    }

    @Test
    fun aLanguageTheSystemRestoredWinsOverTheSettingsRecord() {
        // Android 13+ backs the per-app language up itself; on a new phone it is back before launch.
        val language = FakeLanguage(applied = "en", system = "he")
        scenario(language, stored = { it.edit { preferences -> preferences[LanguageKey] = "hebrew" } }) { controller, repository, _ ->
            assertFalse(controller.syncOnLaunch(displayed = "en"))
            assertEquals("en", language.applied)
            assertEquals("en", repository.settings.first().language)
        }
    }

    @Test
    fun aRecordedLanguageThisVersionDoesNotHaveFallsBackToThePhones() {
        val language = FakeLanguage(system = "he")
        scenario(language, stored = { it.edit { preferences -> preferences[LanguageKey] = "fr" } }) { controller, _, _ ->
            controller.syncOnLaunch(displayed = "he")
            assertEquals("he", language.applied)
        }
    }

    @Test
    fun afterTheFirstLaunchTheRecordFollowsTheSystemSettings() {
        val language = FakeLanguage(applied = "he", initialized = true)
        scenario(language, stored = { it.edit { preferences -> preferences[LanguageKey] = "hebrew" } }) { controller, repository, dataStore ->
            // Changed under Settings → Apps → JewishDay → Language.
            language.applied = "en"
            assertFalse(controller.syncOnLaunch(displayed = "en"))
            assertEquals("en", repository.settings.first().language)
            // Stored under the name older versions read, so going back to one still works.
            assertEquals("english", dataStore.data.first()[LanguageKey])

            // "System default": the app follows the phone, and there is no choice to carry over.
            language.applied = null
            controller.syncOnLaunch(displayed = "en")
            assertNull(repository.settings.first().language)
            assertNull(language.applied)
        }
    }

    @Test
    fun choosingInTheAppSetsTheLanguageAndRecordsIt() {
        val language = FakeLanguage(applied = "en", initialized = true)
        scenario(language) { controller, repository, _ ->
            controller.select("he")
            assertEquals("he", language.applied)
            controller.record("he")
            assertEquals("he", repository.settings.first().language)
        }
    }

    private object NoStartupCache : StartupSettingsCache {
        override fun read(): RootUiSettings? = null
        override fun write(settings: RootUiSettings) = Unit
    }

    private companion object {
        val LanguageKey = stringPreferencesKey("app_language")
        val LegacyHebrewKey = booleanPreferencesKey("use_hebrew_interface")
    }
}
