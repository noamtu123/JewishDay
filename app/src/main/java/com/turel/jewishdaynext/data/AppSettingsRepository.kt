package com.turel.jewishdaynext.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

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
    val advancedZmanimModeEnabled: Boolean = false,
    val rambamThreeChaptersEnabled: Boolean = false,
    val themeOption: AppThemeOption = AppThemeOption.Classic,
    val zmanimSettings: ZmanimCalculationSettings = ZmanimCalculationSettings(),
)

data class RootUiSettings(
    val themeOption: AppThemeOption = AppThemeOption.Classic,
    val useHebrewInterface: Boolean = false,
)

interface AppSettingsRepository {
    val settings: Flow<AppSettings>
    val rootUiSettings: Flow<RootUiSettings>

    suspend fun setDailyDateNotificationEnabled(enabled: Boolean)
    suspend fun setHebrewDateStatusIconEnabled(enabled: Boolean)
    suspend fun setEnglishDateStatusIconEnabled(enabled: Boolean)
    suspend fun setPreferHebrewDates(enabled: Boolean)
    suspend fun setUseHebrewInterface(enabled: Boolean)
    suspend fun setUse24HourTime(enabled: Boolean)
    suspend fun setAdvancedZmanimModeEnabled(enabled: Boolean)
    suspend fun setRambamThreeChaptersEnabled(enabled: Boolean)
    suspend fun setThemeOption(themeOption: AppThemeOption)
    suspend fun setZmanimSettings(settings: ZmanimCalculationSettings)
}

class DataStoreAppSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val startupSettingsCache: StartupSettingsCache,
) : AppSettingsRepository {
    override val rootUiSettings: Flow<RootUiSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map(::decodeRootUiSettings)
        .distinctUntilChanged()
        .onEach(startupSettingsCache::write)

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val rootUiSettings = decodeRootUiSettings(preferences)
            AppSettings(
                dailyDateNotificationEnabled = preferences[DailyDateNotificationEnabled] ?: false,
                hebrewDateStatusIconEnabled = preferences[HebrewDateStatusIconEnabled] ?: false,
                englishDateStatusIconEnabled = preferences[EnglishDateStatusIconEnabled] ?: false,
                preferHebrewDates = preferences[PreferHebrewDates] ?: false,
                useHebrewInterface = rootUiSettings.useHebrewInterface,
                use24HourTime = preferences[Use24HourTime] ?: true,
                advancedZmanimModeEnabled = preferences[AdvancedZmanimModeEnabled] ?: false,
                rambamThreeChaptersEnabled = preferences[RambamThreeChaptersEnabled] ?: false,
                themeOption = rootUiSettings.themeOption,
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
        val updatedPreferences = dataStore.edit { preferences ->
            preferences[UseHebrewInterface] = enabled
        }
        startupSettingsCache.write(decodeRootUiSettings(updatedPreferences))
    }

    override suspend fun setUse24HourTime(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Use24HourTime] = enabled
        }
    }

    override suspend fun setAdvancedZmanimModeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AdvancedZmanimModeEnabled] = enabled
        }
    }

    override suspend fun setRambamThreeChaptersEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[RambamThreeChaptersEnabled] = enabled
        }
    }

    override suspend fun setThemeOption(themeOption: AppThemeOption) {
        val updatedPreferences = dataStore.edit { preferences ->
            preferences[ThemeOption] = themeOption.storageValue
            preferences.remove(BlueWhiteTheme)
            preferences.remove(AmoledBlackTheme)
        }
        startupSettingsCache.write(decodeRootUiSettings(updatedPreferences))
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

    private fun decodeRootUiSettings(preferences: Preferences): RootUiSettings = RootUiSettings(
        themeOption = decodeThemeOption(preferences),
        useHebrewInterface = preferences[UseHebrewInterface] ?: false,
    )

    private fun decodeThemeOption(preferences: Preferences): AppThemeOption =
        AppThemeOption.fromStorageValue(preferences[ThemeOption])
            ?: when {
                preferences[AmoledBlackTheme] == true -> AppThemeOption.AmoledBlack
                preferences[BlueWhiteTheme] == true -> AppThemeOption.BlueWhite
                else -> AppThemeOption.Classic
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
        val DailyDateNotificationEnabled = booleanPreferencesKey("daily_date_notification_enabled")
        val HebrewDateStatusIconEnabled = booleanPreferencesKey("hebrew_date_status_icon_enabled")
        val EnglishDateStatusIconEnabled = booleanPreferencesKey("english_date_status_icon_enabled")
        val PreferHebrewDates = booleanPreferencesKey("prefer_hebrew_dates")
        val UseHebrewInterface = booleanPreferencesKey("use_hebrew_interface")
        val Use24HourTime = booleanPreferencesKey("use_24_hour_time")
        val AdvancedZmanimModeEnabled = booleanPreferencesKey("advanced_zmanim_mode_enabled")
        val RambamThreeChaptersEnabled = booleanPreferencesKey("rambam_three_chapters_enabled")
        val ThemeOption = stringPreferencesKey("theme_option")
        val BlueWhiteTheme = booleanPreferencesKey("blue_white_theme")
        val AmoledBlackTheme = booleanPreferencesKey("amoled_black_theme")
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
    }
}
