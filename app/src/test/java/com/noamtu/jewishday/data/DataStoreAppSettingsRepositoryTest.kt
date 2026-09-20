// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.noamtu.jewishday.model.AlotHashacharMethod
import com.noamtu.jewishday.model.CandleLightingMethod
import com.noamtu.jewishday.model.CustomZmanValue
import com.noamtu.jewishday.model.ChametzMethod
import com.noamtu.jewishday.model.ChatzotMethod
import com.noamtu.jewishday.model.MinchaGedolaMethod
import com.noamtu.jewishday.model.MinchaKetanaMethod
import com.noamtu.jewishday.model.MisheyakirMethod
import com.noamtu.jewishday.model.MotzeiShabbatMethod
import com.noamtu.jewishday.model.PlagHaminchaMethod
import com.noamtu.jewishday.model.RabbeinuTamMethod
import com.noamtu.jewishday.model.SofZmanShemaMethod
import com.noamtu.jewishday.model.SofZmanTefillahMethod
import com.noamtu.jewishday.model.SunriseMethod
import com.noamtu.jewishday.model.SunsetMethod
import com.noamtu.jewishday.model.TzeitHakochavimMethod
import com.noamtu.jewishday.model.ZmanimCalculationSettings
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreAppSettingsRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun settingsExposeDefaultsAndPersistUpdates() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val startupSettingsCache = FakeStartupSettingsCache()
        val repository = DataStoreAppSettingsRepository(
            PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { File(temporaryFolder.newFolder(), "settings.preferences_pb") },
            ),
            startupSettingsCache,
        )

        try {
            // First launch follows the device language, so pin the expected default to
            // the same resolution the repository uses rather than a hard-coded value.
            val defaultLanguage = AppLanguage.systemDefault()
            assertEquals(AppSettings(language = defaultLanguage), repository.settings.first())
            assertEquals(RootUiSettings(language = defaultLanguage), repository.rootUiSettings.first())

            repository.setHebrewDateStatusIconEnabled(true)
            repository.setAppLanguage(AppLanguage.Hebrew)
            repository.setUse24HourTime(false)
            repository.setThemeOption(AppThemeOption.Midnight)
            repository.setCandleLightingPromptHandled(true)

            assertEquals(
                AppSettings(
                    hebrewDateStatusIconEnabled = true,
                    language = AppLanguage.Hebrew,
                    use24HourTime = false,
                    themeOption = AppThemeOption.Midnight,
                    candleLightingPromptHandled = true,
                ),
                repository.settings.first(),
            )
            assertEquals(
                RootUiSettings(
                    themeOption = AppThemeOption.Midnight,
                    language = AppLanguage.Hebrew,
                ),
                repository.rootUiSettings.first(),
            )
            assertEquals(
                RootUiSettings(
                    themeOption = AppThemeOption.Midnight,
                    language = AppLanguage.Hebrew,
                ),
                startupSettingsCache.read(),
            )
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun zmanimCalculationSettingsPersistAllAdvancedChoices() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val repository = DataStoreAppSettingsRepository(
            PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { File(temporaryFolder.newFolder(), "zmanim.preferences_pb") },
            ),
            FakeStartupSettingsCache(),
        )
        // Every method a different value from its default, and every custom option carrying a
        // number of its own, so a field left out of the encode or the decode shows up here.
        val zmanimSettings = ZmanimCalculationSettings(
            alotHashacharMethod = AlotHashacharMethod.CustomDegrees,
            alotHashacharCustom = CustomZmanValue(degrees = 18.5, minutes = 71, zmaniyotMinutes = 73),
            misheyakirMethod = MisheyakirMethod.CustomMinutesBeforeSunrise,
            misheyakirCustom = CustomZmanValue(degrees = 7.65, minutes = 44, zmaniyotMinutes = 46),
            sunriseMethod = SunriseMethod.ElevationAdjusted,
            sofZmanShemaMethod = SofZmanShemaMethod.CustomZmaniyotMinutes,
            sofZmanShemaCustom = CustomZmanValue(degrees = 18.0, minutes = 89, zmaniyotMinutes = 91),
            sofZmanTefillahMethod = SofZmanTefillahMethod.CustomDegrees,
            sofZmanTefillahCustom = CustomZmanValue(degrees = 17.25, minutes = 88, zmaniyotMinutes = 92),
            chatzotMethod = ChatzotMethod.FixedLocal,
            minchaGedolaMethod = MinchaGedolaMethod.AhavatShalom,
            minchaGedolaCustom = CustomZmanValue(degrees = 15.5, minutes = 70, zmaniyotMinutes = 74),
            minchaKetanaMethod = MinchaKetanaMethod.BaalHatanya,
            minchaKetanaCustom = CustomZmanValue(degrees = 15.25, minutes = 69, zmaniyotMinutes = 75),
            plagHaminchaMethod = PlagHaminchaMethod.AteretTorah,
            plagHaminchaCustom = CustomZmanValue(degrees = 14.5, minutes = 68, zmaniyotMinutes = 76),
            sunsetMethod = SunsetMethod.ElevationAdjusted,
            tzeitHakochavimMethod = TzeitHakochavimMethod.CustomMinutes,
            tzeitHakochavimCustom = CustomZmanValue(degrees = 5.9, minutes = 51, zmaniyotMinutes = 53),
            candleLightingMethod = CandleLightingMethod.Custom,
            candleLightingCustomMinutes = 22,
            motzeiShabbatMethod = MotzeiShabbatMethod.CustomZmaniyotMinutes,
            motzeiShabbatCustom = CustomZmanValue(degrees = 6.45, minutes = 59, zmaniyotMinutes = 61),
            rabbeinuTamMethod = RabbeinuTamMethod.BainHashmashot2Stars,
            rabbeinuTamCustom = CustomZmanValue(degrees = 25.5, minutes = 119, zmaniyotMinutes = 121),
            chametzMethod = ChametzMethod.BaalHatanya,
            chametzCustom = CustomZmanValue(degrees = 16.05, minutes = 73, zmaniyotMinutes = 77),
            ateretTorahSunsetOffsetMinutes = 37,
        )

        try {
            repository.setZmanimSettings(zmanimSettings)

            assertEquals(zmanimSettings, repository.settings.first().zmanimSettings)
        } finally {
            scope.cancel()
        }
    }

    private class FakeStartupSettingsCache : StartupSettingsCache {
        private var settings: RootUiSettings? = null

        override fun read(): RootUiSettings? = settings

        override fun write(settings: RootUiSettings) {
            this.settings = settings
        }
    }

}