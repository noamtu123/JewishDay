package com.turel.jewishdaynext.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.turel.jewishdaynext.model.JewishLocation
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
import com.turel.jewishdaynext.model.defaultJerusalemLocation
import java.io.IOException
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

data class SavedPlace(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double,
    val zoneId: ZoneId,
) {
    fun toJewishLocation(): JewishLocation = JewishLocation(
        name = name,
        latitude = latitude,
        longitude = longitude,
        elevationMeters = elevationMeters,
        zoneId = zoneId,
    )
}

val defaultSavedPlace = SavedPlace(
    id = "jerusalem",
    name = defaultJerusalemLocation.name,
    latitude = defaultJerusalemLocation.latitude,
    longitude = defaultJerusalemLocation.longitude,
    elevationMeters = defaultJerusalemLocation.elevationMeters,
    zoneId = defaultJerusalemLocation.zoneId,
)

enum class AppThemeOption(val storageValue: String) {
    Classic("classic"),
    BlueWhite("blue_white"),
    IsraelSky("israel_sky"),
    JerusalemStone("jerusalem_stone"),
    AmoledBlack("amoled_black"),
    ;

    companion object {
        fun fromStorageValue(value: String?): AppThemeOption? =
            entries.firstOrNull { it.storageValue == value }
    }
}

data class AppSettings(
    val dailyDateNotificationEnabled: Boolean = false,
    val hebrewDateStatusIconEnabled: Boolean = false,
    val englishDateStatusIconEnabled: Boolean = false,
    val preferHebrewDates: Boolean = false,
    val useHebrewInterface: Boolean = false,
    val use24HourTime: Boolean = true,
    val themeOption: AppThemeOption = AppThemeOption.Classic,
    val savedPlaces: List<SavedPlace> = listOf(defaultSavedPlace),
    val selectedPlaceId: String = defaultSavedPlace.id,
    val zmanimSettings: ZmanimCalculationSettings = ZmanimCalculationSettings(),
) {
    val selectedPlace: SavedPlace = savedPlaces.firstOrNull { it.id == selectedPlaceId } ?: defaultSavedPlace
}

interface AppSettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setDailyDateNotificationEnabled(enabled: Boolean)
    suspend fun setHebrewDateStatusIconEnabled(enabled: Boolean)
    suspend fun setEnglishDateStatusIconEnabled(enabled: Boolean)
    suspend fun setPreferHebrewDates(enabled: Boolean)
    suspend fun setUseHebrewInterface(enabled: Boolean)
    suspend fun setUse24HourTime(enabled: Boolean)
    suspend fun setThemeOption(themeOption: AppThemeOption)
    suspend fun savePlace(place: SavedPlace)
    suspend fun selectPlace(placeId: String)
    suspend fun deletePlace(placeId: String)
    suspend fun setZmanimSettings(settings: ZmanimCalculationSettings)
}

class DataStoreAppSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : AppSettingsRepository {
    override val settings: Flow<AppSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val savedPlaces = preferences[SavedPlaces]
                .orEmpty()
                .mapNotNull(::decodeSavedPlace)
                .let { places -> (listOf(defaultSavedPlace) + places).distinctBy(SavedPlace::id) }
            val themeOption = AppThemeOption.fromStorageValue(preferences[ThemeOption])
                ?: when {
                    preferences[AmoledBlackTheme] == true -> AppThemeOption.AmoledBlack
                    preferences[BlueWhiteTheme] == true -> AppThemeOption.BlueWhite
                    else -> AppThemeOption.Classic
                }
            AppSettings(
                dailyDateNotificationEnabled = preferences[DailyDateNotificationEnabled] ?: false,
                hebrewDateStatusIconEnabled = preferences[HebrewDateStatusIconEnabled] ?: false,
                englishDateStatusIconEnabled = preferences[EnglishDateStatusIconEnabled] ?: false,
                preferHebrewDates = preferences[PreferHebrewDates] ?: false,
                useHebrewInterface = preferences[UseHebrewInterface] ?: false,
                use24HourTime = preferences[Use24HourTime] ?: true,
                themeOption = themeOption,
                savedPlaces = savedPlaces,
                selectedPlaceId = preferences[SelectedPlaceId] ?: defaultSavedPlace.id,
                zmanimSettings = decodeZmanimSettings(preferences),
            )
        }

    override suspend fun setDailyDateNotificationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DailyDateNotificationEnabled] = enabled
        }
    }

    override suspend fun setHebrewDateStatusIconEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[HebrewDateStatusIconEnabled] = enabled
        }
    }

    override suspend fun setEnglishDateStatusIconEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[EnglishDateStatusIconEnabled] = enabled
        }
    }

    override suspend fun setPreferHebrewDates(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferHebrewDates] = enabled
        }
    }

    override suspend fun setUseHebrewInterface(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[UseHebrewInterface] = enabled
        }
    }

    override suspend fun setUse24HourTime(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Use24HourTime] = enabled
        }
    }

    override suspend fun setThemeOption(themeOption: AppThemeOption) {
        dataStore.edit { preferences ->
            preferences[ThemeOption] = themeOption.storageValue
            preferences.remove(BlueWhiteTheme)
            preferences.remove(AmoledBlackTheme)
        }
    }

    override suspend fun savePlace(place: SavedPlace) {
        dataStore.edit { preferences ->
            val existing = preferences[SavedPlaces]
                .orEmpty()
                .mapNotNull(::decodeSavedPlace)
                .filterNot { it.id == place.id || it.isSameStoredPlace(place) }
            preferences[SavedPlaces] = (existing + place)
                .filterNot { it.id == defaultSavedPlace.id }
                .map(::encodeSavedPlace)
                .toSet()
            preferences[SelectedPlaceId] = place.id
        }
    }

    override suspend fun selectPlace(placeId: String) {
        dataStore.edit { preferences ->
            preferences[SelectedPlaceId] = placeId
        }
    }

    override suspend fun deletePlace(placeId: String) {
        if (placeId == defaultSavedPlace.id) return

        dataStore.edit { preferences ->
            preferences[SavedPlaces] = preferences[SavedPlaces]
                .orEmpty()
                .mapNotNull(::decodeSavedPlace)
                .filterNot { it.id == placeId }
                .map(::encodeSavedPlace)
                .toSet()
            if (preferences[SelectedPlaceId] == placeId) {
                preferences[SelectedPlaceId] = defaultSavedPlace.id
            }
        }
    }

    override suspend fun setZmanimSettings(settings: ZmanimCalculationSettings) {
        dataStore.edit { preferences ->
            preferences[ZmanimPresetKey] = settings.preset.storageValue
            preferences[InIsrael] = settings.inIsrael
            preferences[HighLatitudeHandlingKey] = settings.highLatitudeHandling.storageValue
            preferences[AlotHashacharMethodKey] = settings.alotHashacharMethod.storageValue
            preferences[MisheyakirMethodKey] = settings.misheyakirMethod.storageValue
            preferences[SunriseMethodKey] = settings.sunriseMethod.storageValue
            preferences[SofZmanShemaMethodKey] = settings.sofZmanShemaMethod.storageValue
            preferences[SofZmanTefillahMethodKey] = settings.sofZmanTefillahMethod.storageValue
            preferences[ChatzotMethodKey] = settings.chatzotMethod.storageValue
            preferences[MinchaGedolaMethodKey] = settings.minchaGedolaMethod.storageValue
            preferences[SamuchLeMinchaKetanaMethodKey] = settings.samuchLeMinchaKetanaMethod.storageValue
            preferences[MinchaKetanaMethodKey] = settings.minchaKetanaMethod.storageValue
            preferences[PlagHaminchaMethodKey] = settings.plagHaminchaMethod.storageValue
            preferences[SunsetMethodKey] = settings.sunsetMethod.storageValue
            preferences[TzeitHakochavimMethodKey] = settings.tzeitHakochavimMethod.storageValue
            preferences[CandleLightingMethodKey] = settings.candleLightingMethod.storageValue
            preferences[MotzeiShabbatMethodKey] = settings.motzeiShabbatMethod.storageValue
            preferences[RabbeinuTamMethodKey] = settings.rabbeinuTamMethod.storageValue
            preferences[BainHashmashotMethodKey] = settings.bainHashmashotMethod.storageValue
            preferences[FastDayMethodKey] = settings.fastDayMethod.storageValue
            preferences[ChametzMethodKey] = settings.chametzMethod.storageValue
            preferences[AteretTorahOffsetMinutes] = settings.ateretTorahSunsetOffsetMinutes
        }
    }

    private fun decodeZmanimSettings(preferences: Preferences): ZmanimCalculationSettings {
        val legacyUseMga = preferences[UseMgaForShemaAndTefila] ?: false
        return ZmanimCalculationSettings(
            preset = ZmanimPreset.fromStorageValue(preferences[ZmanimPresetKey]) ?: ZmanimPreset.Standard,
            inIsrael = preferences[InIsrael] ?: true,
            highLatitudeHandling = HighLatitudeHandling.fromStorageValue(preferences[HighLatitudeHandlingKey]) ?: HighLatitudeHandling.FixedMinutesFallback,
            alotHashacharMethod = AlotHashacharMethod.fromStorageValue(preferences[AlotHashacharMethodKey])
                ?: legacyAlotMethod(preferences[AlotHashacharOffsetMinutes] ?: 72),
            misheyakirMethod = MisheyakirMethod.fromStorageValue(preferences[MisheyakirMethodKey]) ?: MisheyakirMethod.Degrees11Point5,
            sunriseMethod = SunriseMethod.fromStorageValue(preferences[SunriseMethodKey])
                ?: if (preferences[UseSeaLevelSunrise] == false) SunriseMethod.ElevationAdjusted else SunriseMethod.SeaLevel,
            sofZmanShemaMethod = SofZmanShemaMethod.fromStorageValue(preferences[SofZmanShemaMethodKey])
                ?: if (legacyUseMga) SofZmanShemaMethod.Mga72 else SofZmanShemaMethod.Gra,
            sofZmanTefillahMethod = SofZmanTefillahMethod.fromStorageValue(preferences[SofZmanTefillahMethodKey])
                ?: if (legacyUseMga) SofZmanTefillahMethod.Mga72 else SofZmanTefillahMethod.Gra,
            chatzotMethod = ChatzotMethod.fromStorageValue(preferences[ChatzotMethodKey]) ?: ChatzotMethod.Solar,
            minchaGedolaMethod = MinchaGedolaMethod.fromStorageValue(preferences[MinchaGedolaMethodKey]) ?: MinchaGedolaMethod.Standard,
            samuchLeMinchaKetanaMethod = SamuchLeMinchaKetanaMethod.fromStorageValue(preferences[SamuchLeMinchaKetanaMethodKey]) ?: SamuchLeMinchaKetanaMethod.Gra,
            minchaKetanaMethod = MinchaKetanaMethod.fromStorageValue(preferences[MinchaKetanaMethodKey]) ?: MinchaKetanaMethod.Standard,
            plagHaminchaMethod = PlagHaminchaMethod.fromStorageValue(preferences[PlagHaminchaMethodKey])
                ?: legacyPlagMethod(preferences[PlagHaminchaOffsetMinutes] ?: 0),
            sunsetMethod = SunsetMethod.fromStorageValue(preferences[SunsetMethodKey])
                ?: if (preferences[UseSeaLevelSunset] == false) SunsetMethod.ElevationAdjusted else SunsetMethod.SeaLevel,
            tzeitHakochavimMethod = TzeitHakochavimMethod.fromStorageValue(preferences[TzeitHakochavimMethodKey]) ?: TzeitHakochavimMethod.Geonim8Point5,
            candleLightingMethod = CandleLightingMethod.fromStorageValue(preferences[CandleLightingMethodKey])
                ?: legacyCandleMethod(preferences[CandleLightingOffsetMinutes] ?: 18),
            motzeiShabbatMethod = MotzeiShabbatMethod.fromStorageValue(preferences[MotzeiShabbatMethodKey]) ?: MotzeiShabbatMethod.Geonim8Point5,
            rabbeinuTamMethod = RabbeinuTamMethod.fromStorageValue(preferences[RabbeinuTamMethodKey]) ?: RabbeinuTamMethod.Minutes72,
            bainHashmashotMethod = BainHashmashotMethod.fromStorageValue(preferences[BainHashmashotMethodKey]) ?: BainHashmashotMethod.RabbeinuTam13Point24,
            fastDayMethod = FastDayMethod.fromStorageValue(preferences[FastDayMethodKey]) ?: FastDayMethod.Alot72ToTzeit8Point5,
            chametzMethod = ChametzMethod.fromStorageValue(preferences[ChametzMethodKey]) ?: ChametzMethod.Gra,
            ateretTorahSunsetOffsetMinutes = preferences[AteretTorahOffsetMinutes] ?: 40,
        )
    }

    private fun legacyAlotMethod(minutes: Int): AlotHashacharMethod = when (minutes) {
        90 -> AlotHashacharMethod.Minutes90
        120 -> AlotHashacharMethod.Minutes120
        else -> AlotHashacharMethod.Minutes72
    }

    private fun legacyPlagMethod(minutes: Int): PlagHaminchaMethod = when (minutes) {
        60 -> PlagHaminchaMethod.Mga60
        72 -> PlagHaminchaMethod.Mga72
        90 -> PlagHaminchaMethod.Mga90
        96 -> PlagHaminchaMethod.Mga96
        120 -> PlagHaminchaMethod.Mga120
        else -> PlagHaminchaMethod.Gra
    }

    private fun legacyCandleMethod(minutes: Int): CandleLightingMethod = when (minutes) {
        20 -> CandleLightingMethod.Minutes20
        30 -> CandleLightingMethod.Minutes30
        40 -> CandleLightingMethod.Minutes40
        else -> CandleLightingMethod.Minutes18
    }

    private companion object {
        const val PlaceDelimiter = "\u001F"
        val DailyDateNotificationEnabled = booleanPreferencesKey("daily_date_notification_enabled")
        val HebrewDateStatusIconEnabled = booleanPreferencesKey("hebrew_date_status_icon_enabled")
        val EnglishDateStatusIconEnabled = booleanPreferencesKey("english_date_status_icon_enabled")
        val PreferHebrewDates = booleanPreferencesKey("prefer_hebrew_dates")
        val UseHebrewInterface = booleanPreferencesKey("use_hebrew_interface")
        val Use24HourTime = booleanPreferencesKey("use_24_hour_time")
        val ThemeOption = stringPreferencesKey("theme_option")
        val BlueWhiteTheme = booleanPreferencesKey("blue_white_theme")
        val AmoledBlackTheme = booleanPreferencesKey("amoled_black_theme")
        val SavedPlaces = stringSetPreferencesKey("saved_places")
        val SelectedPlaceId = stringPreferencesKey("selected_place_id")
        val ZmanimPresetKey = stringPreferencesKey("zmanim_preset")
        val HighLatitudeHandlingKey = stringPreferencesKey("zmanim_high_latitude_handling")
        val AlotHashacharMethodKey = stringPreferencesKey("zmanim_alot_method")
        val MisheyakirMethodKey = stringPreferencesKey("zmanim_misheyakir_method")
        val SunriseMethodKey = stringPreferencesKey("zmanim_sunrise_method")
        val SofZmanShemaMethodKey = stringPreferencesKey("zmanim_shema_method")
        val SofZmanTefillahMethodKey = stringPreferencesKey("zmanim_tefillah_method")
        val ChatzotMethodKey = stringPreferencesKey("zmanim_chatzot_method")
        val MinchaGedolaMethodKey = stringPreferencesKey("zmanim_mincha_gedola_method")
        val SamuchLeMinchaKetanaMethodKey = stringPreferencesKey("zmanim_samuch_le_mincha_ketana_method")
        val MinchaKetanaMethodKey = stringPreferencesKey("zmanim_mincha_ketana_method")
        val PlagHaminchaMethodKey = stringPreferencesKey("zmanim_plag_method")
        val SunsetMethodKey = stringPreferencesKey("zmanim_sunset_method")
        val TzeitHakochavimMethodKey = stringPreferencesKey("zmanim_tzeit_method")
        val CandleLightingMethodKey = stringPreferencesKey("zmanim_candle_method")
        val MotzeiShabbatMethodKey = stringPreferencesKey("zmanim_motzei_method")
        val RabbeinuTamMethodKey = stringPreferencesKey("zmanim_rabbeinu_tam_method")
        val BainHashmashotMethodKey = stringPreferencesKey("zmanim_bain_hashmashot_method")
        val FastDayMethodKey = stringPreferencesKey("zmanim_fast_day_method")
        val ChametzMethodKey = stringPreferencesKey("zmanim_chametz_method")
        val AteretTorahOffsetMinutes = intPreferencesKey("zmanim_ateret_torah_offset_minutes")
        val InIsrael = booleanPreferencesKey("zmanim_in_israel")
        val UseMgaForShemaAndTefila = booleanPreferencesKey("zmanim_use_mga")
        val AlotHashacharOffsetMinutes = intPreferencesKey("zmanim_alot_offset_minutes")
        val PlagHaminchaOffsetMinutes = intPreferencesKey("zmanim_plag_offset_minutes")
        val UseSeaLevelSunrise = booleanPreferencesKey("zmanim_use_sea_level_sunrise")
        val UseSeaLevelSunset = booleanPreferencesKey("zmanim_use_sea_level_sunset")
        val CandleLightingOffsetMinutes = intPreferencesKey("zmanim_candle_lighting_offset_minutes")

        fun encodeSavedPlace(place: SavedPlace): String = listOf(
            place.id,
            place.name,
            place.latitude.toString(),
            place.longitude.toString(),
            place.elevationMeters.toString(),
            place.zoneId.id,
        ).joinToString(PlaceDelimiter)

        fun decodeSavedPlace(value: String): SavedPlace? {
            val parts = value.split(PlaceDelimiter)
            if (parts.size != 6) return null

            return runCatching {
                SavedPlace(
                    id = parts[0],
                    name = parts[1],
                    latitude = parts[2].toDouble(),
                    longitude = parts[3].toDouble(),
                    elevationMeters = parts[4].toDouble(),
                    zoneId = ZoneId.of(parts[5]),
                )
            }.getOrNull()
        }

        fun SavedPlace.isSameStoredPlace(other: SavedPlace): Boolean =
            name.equals(other.name, ignoreCase = true) &&
                abs(latitude - other.latitude) < 0.0001 &&
                abs(longitude - other.longitude) < 0.0001
    }
}
