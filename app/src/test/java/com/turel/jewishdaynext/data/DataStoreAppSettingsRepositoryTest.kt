package com.turel.jewishdaynext.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.turel.jewishdaynext.model.AlotHashacharMethod
import com.turel.jewishdaynext.model.BainHashmashotMethod
import com.turel.jewishdaynext.model.CandleLightingMethod
import com.turel.jewishdaynext.model.ChametzMethod
import com.turel.jewishdaynext.model.ChatzotMethod
import com.turel.jewishdaynext.model.FastDayMethod
import com.turel.jewishdaynext.model.HighLatitudeHandling
import com.turel.jewishdaynext.model.MinchaGedolaMethod
import com.turel.jewishdaynext.model.MinchaKetanaMethod
import com.turel.jewishdaynext.model.MisheyakirMethod
import com.turel.jewishdaynext.model.MotzeiShabbatMethod
import com.turel.jewishdaynext.model.PlagHaminchaMethod
import com.turel.jewishdaynext.model.RabbeinuTamMethod
import com.turel.jewishdaynext.model.SamuchLeMinchaKetanaMethod
import com.turel.jewishdaynext.model.SofZmanShemaMethod
import com.turel.jewishdaynext.model.SofZmanTefillahMethod
import com.turel.jewishdaynext.model.SunriseMethod
import com.turel.jewishdaynext.model.SunsetMethod
import com.turel.jewishdaynext.model.TzeitHakochavimMethod
import com.turel.jewishdaynext.model.ZmanimCalculationSettings
import com.turel.jewishdaynext.model.ZmanimPreset
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
            assertEquals(AppSettings(), repository.settings.first())
            assertEquals(RootUiSettings(), repository.rootUiSettings.first())

            repository.setDailyDateNotificationEnabled(true)
            repository.setHebrewDateStatusIconEnabled(true)
            repository.setEnglishDateStatusIconEnabled(true)
            repository.setPreferHebrewDates(true)
            repository.setUseHebrewInterface(true)
            repository.setUse24HourTime(false)
            repository.setAdvancedZmanimModeEnabled(true)
            repository.setRambamThreeChaptersEnabled(true)
            repository.setThemeOption(AppThemeOption.IsraelSky)

            assertEquals(
                AppSettings(
                    dailyDateNotificationEnabled = true,
                    hebrewDateStatusIconEnabled = true,
                    englishDateStatusIconEnabled = true,
                    preferHebrewDates = true,
                    useHebrewInterface = true,
                    use24HourTime = false,
                    advancedZmanimModeEnabled = true,
                    rambamThreeChaptersEnabled = true,
                    themeOption = AppThemeOption.IsraelSky,
                ),
                repository.settings.first(),
            )
            assertEquals(
                RootUiSettings(
                    themeOption = AppThemeOption.IsraelSky,
                    useHebrewInterface = true,
                ),
                repository.rootUiSettings.first(),
            )
            assertEquals(
                RootUiSettings(
                    themeOption = AppThemeOption.IsraelSky,
                    useHebrewInterface = true,
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
            samuchLeMinchaKetanaMethod = SamuchLeMinchaKetanaMethod.Degrees16Point1,
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
