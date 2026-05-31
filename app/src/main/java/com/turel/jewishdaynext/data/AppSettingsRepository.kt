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
import com.turel.jewishdaynext.model.ZmanimCalculationSettings
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

data class AppSettings(
    val dailyDateNotificationEnabled: Boolean = false,
    val hebrewDateStatusIconEnabled: Boolean = false,
    val englishDateStatusIconEnabled: Boolean = false,
    val preferHebrewDates: Boolean = false,
    val useHebrewInterface: Boolean = false,
    val use24HourTime: Boolean = true,
    val blueWhiteTheme: Boolean = false,
    val amoledBlackTheme: Boolean = false,
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
    suspend fun setBlueWhiteTheme(enabled: Boolean)
    suspend fun setAmoledBlackTheme(enabled: Boolean)
    suspend fun savePlace(place: SavedPlace)
    suspend fun selectPlace(placeId: String)
    suspend fun deletePlace(placeId: String)
    suspend fun setInIsrael(enabled: Boolean)
    suspend fun setUseMgaForShemaAndTefila(enabled: Boolean)
    suspend fun setAlotHashacharOffsetMinutes(minutes: Int)
    suspend fun setPlagHaminchaOffsetMinutes(minutes: Int)
    suspend fun setUseSeaLevelSunrise(enabled: Boolean)
    suspend fun setUseSeaLevelSunset(enabled: Boolean)
    suspend fun setCandleLightingOffsetMinutes(minutes: Int)
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
            AppSettings(
                dailyDateNotificationEnabled = preferences[DailyDateNotificationEnabled] ?: false,
                hebrewDateStatusIconEnabled = preferences[HebrewDateStatusIconEnabled] ?: false,
                englishDateStatusIconEnabled = preferences[EnglishDateStatusIconEnabled] ?: false,
                preferHebrewDates = preferences[PreferHebrewDates] ?: false,
                useHebrewInterface = preferences[UseHebrewInterface] ?: false,
                use24HourTime = preferences[Use24HourTime] ?: true,
                blueWhiteTheme = preferences[BlueWhiteTheme] ?: false,
                amoledBlackTheme = preferences[AmoledBlackTheme] ?: false,
                savedPlaces = savedPlaces,
                selectedPlaceId = preferences[SelectedPlaceId] ?: defaultSavedPlace.id,
                zmanimSettings = ZmanimCalculationSettings(
                    inIsrael = preferences[InIsrael] ?: true,
                    useMgaForShemaAndTefila = preferences[UseMgaForShemaAndTefila] ?: false,
                    alotHashacharOffsetMinutes = preferences[AlotHashacharOffsetMinutes] ?: 72,
                    plagHaminchaOffsetMinutes = preferences[PlagHaminchaOffsetMinutes] ?: 0,
                    useSeaLevelSunrise = preferences[UseSeaLevelSunrise] ?: true,
                    useSeaLevelSunset = preferences[UseSeaLevelSunset] ?: true,
                    candleLightingOffsetMinutes = preferences[CandleLightingOffsetMinutes] ?: 18,
                ),
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

    override suspend fun setBlueWhiteTheme(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[BlueWhiteTheme] = enabled
        }
    }

    override suspend fun setAmoledBlackTheme(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AmoledBlackTheme] = enabled
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

    override suspend fun setInIsrael(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[InIsrael] = enabled }
    }

    override suspend fun setUseMgaForShemaAndTefila(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[UseMgaForShemaAndTefila] = enabled }
    }

    override suspend fun setAlotHashacharOffsetMinutes(minutes: Int) {
        dataStore.edit { preferences -> preferences[AlotHashacharOffsetMinutes] = minutes }
    }

    override suspend fun setPlagHaminchaOffsetMinutes(minutes: Int) {
        dataStore.edit { preferences -> preferences[PlagHaminchaOffsetMinutes] = minutes }
    }

    override suspend fun setUseSeaLevelSunrise(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[UseSeaLevelSunrise] = enabled }
    }

    override suspend fun setUseSeaLevelSunset(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[UseSeaLevelSunset] = enabled }
    }

    override suspend fun setCandleLightingOffsetMinutes(minutes: Int) {
        dataStore.edit { preferences -> preferences[CandleLightingOffsetMinutes] = minutes }
    }

    private companion object {
        const val PlaceDelimiter = "\u001F"
        val DailyDateNotificationEnabled = booleanPreferencesKey("daily_date_notification_enabled")
        val HebrewDateStatusIconEnabled = booleanPreferencesKey("hebrew_date_status_icon_enabled")
        val EnglishDateStatusIconEnabled = booleanPreferencesKey("english_date_status_icon_enabled")
        val PreferHebrewDates = booleanPreferencesKey("prefer_hebrew_dates")
        val UseHebrewInterface = booleanPreferencesKey("use_hebrew_interface")
        val Use24HourTime = booleanPreferencesKey("use_24_hour_time")
        val BlueWhiteTheme = booleanPreferencesKey("blue_white_theme")
        val AmoledBlackTheme = booleanPreferencesKey("amoled_black_theme")
        val SavedPlaces = stringSetPreferencesKey("saved_places")
        val SelectedPlaceId = stringPreferencesKey("selected_place_id")
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
