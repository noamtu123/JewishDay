package com.noamtu.jewishday.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.noamtu.jewishday.model.AlotHashacharMethod
import com.noamtu.jewishday.model.BainHashmashotMethod
import com.noamtu.jewishday.model.CandleLightingMethod
import com.noamtu.jewishday.model.ChametzMethod
import com.noamtu.jewishday.model.ChatzotMethod
import com.noamtu.jewishday.model.FastDayMethod
import com.noamtu.jewishday.model.HighLatitudeHandling
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
import com.noamtu.jewishday.model.ZmanimPreset
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
    fun settingsDefaultToDisabledAndPersistUpdates() = runBlocking {
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
        val zmanimSettings = ZmanimCalculationSettings(
            preset = ZmanimPreset.Custom,
            inIsrael = false,
            highLatitudeHandling = HighLatitudeHandling.Strict,
            alotHashacharMethod = AlotHashacharMethod.Degrees18,
            misheyakirMethod = MisheyakirMethod.Degrees7Point65,
            sunriseMethod = SunriseMethod.ElevationAdjusted,
            sofZmanShemaMethod = SofZmanShemaMethod.KolEliyahu,
            sofZmanTefillahMethod = SofZmanTefillahMethod.TwoHoursBeforeChatzot,
            chatzotMethod = ChatzotMethod.FixedLocal,
            minchaGedolaMethod = MinchaGedolaMethod.AhavatShalom,
            minchaKetanaMethod = MinchaKetanaMethod.BaalHatanya,
            plagHaminchaMethod = PlagHaminchaMethod.AteretTorah,
            sunsetMethod = SunsetMethod.ElevationAdjusted,
            tzeitHakochavimMethod = TzeitHakochavimMethod.Minutes90,
            candleLightingMethod = CandleLightingMethod.Minutes40,
            motzeiShabbatMethod = MotzeiShabbatMethod.Minutes60,
            rabbeinuTamMethod = RabbeinuTamMethod.Degrees26,
            bainHashmashotMethod = BainHashmashotMethod.Yereim18Minutes,
            fastDayMethod = FastDayMethod.BaalHatanya,
            chametzMethod = ChametzMethod.BaalHatanya,
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
