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
import java.time.ZoneId
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
        val repository = DataStoreAppSettingsRepository(
            PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { File(temporaryFolder.newFolder(), "settings.preferences_pb") },
            ),
        )

        try {
            assertEquals(AppSettings(), repository.settings.first())

            repository.setDailyDateNotificationEnabled(true)
            repository.setHebrewDateStatusIconEnabled(true)
            repository.setEnglishDateStatusIconEnabled(true)
            repository.setPreferHebrewDates(true)
            repository.setUseHebrewInterface(true)
            repository.setUse24HourTime(false)
            repository.setThemeOption(AppThemeOption.IsraelSky)

            assertEquals(
                AppSettings(
                    dailyDateNotificationEnabled = true,
                    hebrewDateStatusIconEnabled = true,
                    englishDateStatusIconEnabled = true,
                    preferHebrewDates = true,
                    useHebrewInterface = true,
                    use24HourTime = false,
                    themeOption = AppThemeOption.IsraelSky,
                ),
                repository.settings.first(),
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

    @Test
    fun savedPlacesPersistAndSelectedPlaceFallsBackAfterDelete() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val repository = DataStoreAppSettingsRepository(
            PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { File(temporaryFolder.newFolder(), "places.preferences_pb") },
            ),
        )
        val home = SavedPlace(
            id = "home",
            name = "Home",
            latitude = 40.7128,
            longitude = -74.0060,
            elevationMeters = 10.0,
            zoneId = ZoneId.of("America/New_York"),
        )

        try {
            repository.savePlace(home)

            val settingsWithHome = repository.settings.first()
            assertEquals(home, settingsWithHome.selectedPlace)
            assertEquals(listOf(defaultSavedPlace, home), settingsWithHome.savedPlaces)

            repository.deletePlace(home.id)

            val settingsAfterDelete = repository.settings.first()
            assertEquals(defaultSavedPlace, settingsAfterDelete.selectedPlace)
            assertEquals(listOf(defaultSavedPlace), settingsAfterDelete.savedPlaces)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun savingSamePlaceReplacesExistingEntry() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val repository = DataStoreAppSettingsRepository(
            PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { File(temporaryFolder.newFolder(), "dedupe.preferences_pb") },
            ),
        )
        val home = SavedPlace(
            id = "home",
            name = "Home",
            latitude = 40.7128,
            longitude = -74.0060,
            elevationMeters = 10.0,
            zoneId = ZoneId.of("America/New_York"),
        )
        val updatedHome = home.copy(id = "updated_home", elevationMeters = 12.0)

        try {
            repository.savePlace(home)
            repository.savePlace(updatedHome)

            val settings = repository.settings.first()
            assertEquals(updatedHome, settings.selectedPlace)
            assertEquals(listOf(defaultSavedPlace, updatedHome), settings.savedPlaces)
        } finally {
            scope.cancel()
        }
    }
}
