package com.noamtu.jewishday.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.noamtu.jewishday.model.AlotHashacharMethod
import com.noamtu.jewishday.model.CandleLightingMethod
import com.noamtu.jewishday.model.ChametzMethod
import com.noamtu.jewishday.model.ChatzotMethod
import com.noamtu.jewishday.model.DailyLearningType
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
import com.noamtu.jewishday.model.ZmanimTimeOption
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

enum class AppThemeOption(val storageValue: String) {
    BlueWhite("blue_white"),
    Classic("classic"),
    JerusalemStone("jerusalem_stone"),
    Sand("sand"),
    Midnight("midnight"),
    Slate("slate"),
    AmoledBlack("amoled_black"),
    ;

    companion object {
        /** The app-wide default applied on first launch and as a fallback. */
        val Default = BlueWhite

        fun fromStorageValue(value: String?): AppThemeOption? =
            entries.firstOrNull { it.storageValue == value }
    }
}

data class AppSettings(
    val hebrewDateStatusIconEnabled: Boolean = false,
    val language: AppLanguage = AppLanguage.English,
    val use24HourTime: Boolean = true,
    val enabledDailyLearning: Set<DailyLearningType> = DailyLearningType.Default,
    val enabledZmanimTimes: Set<ZmanimTimeOption> = ZmanimTimeOption.Default,
    val themeOption: AppThemeOption = AppThemeOption.Default,
    val zmanimSettings: ZmanimCalculationSettings = ZmanimCalculationSettings(),
    val candleLightingPromptHandled: Boolean = false,
    // The candle-lighting offset the user picked at first launch; used as the "default" marker
    // in the picker and as the value the Reset button restores to. Null until first launch answered.
    val candleLightingDefault: CandleLightingMethod? = null,
) {
    val useHebrewInterface: Boolean get() = language.useHebrewInterface
}

data class RootUiSettings(
    val themeOption: AppThemeOption = AppThemeOption.Default,
    val language: AppLanguage = AppLanguage.English,
) {
    val useHebrewInterface: Boolean get() = language.useHebrewInterface
}

interface AppSettingsRepository {
    val settings: Flow<AppSettings>
    val rootUiSettings: Flow<RootUiSettings>

    /**
     * Called once at startup to lock in the system-language default before any settings are read.
     * If the language has already been stored (first launch happened previously, or the user
     * changed it manually), this is a no-op. This prevents subsequent system-language changes
     * from overriding the stored choice.
     */
    suspend fun seedLanguageDefault()

    suspend fun setHebrewDateStatusIconEnabled(enabled: Boolean)
    suspend fun setAppLanguage(language: AppLanguage)
    suspend fun setUse24HourTime(enabled: Boolean)
    suspend fun setEnabledDailyLearning(types: Set<DailyLearningType>)
    suspend fun setEnabledZmanimTimes(options: Set<ZmanimTimeOption>)
    suspend fun setThemeOption(themeOption: AppThemeOption)
    suspend fun setZmanimSettings(settings: ZmanimCalculationSettings)
    suspend fun setCandleLightingPromptHandled(handled: Boolean)
    suspend fun setCandleLightingDefault(method: CandleLightingMethod)
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
        // Mirrors root UI settings into a synchronous cache for fast startup reads.
        // Note: setAppLanguage/setThemeOption also write the cache explicitly — that is
        // intentional, not redundant: this onEach only runs while something is collecting
        // rootUiSettings, which is not guaranteed at write time.
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
                hebrewDateStatusIconEnabled = preferences[HebrewDateStatusIconEnabled] ?: false,
                language = rootUiSettings.language,
                use24HourTime = preferences[Use24HourTime] ?: true,
                enabledDailyLearning = preferences[EnabledDailyLearningKey]
                    ?.mapNotNull(DailyLearningType::fromStorageValue)?.toSet()
                    ?: DailyLearningType.Default,
                enabledZmanimTimes = preferences[EnabledZmanimTimesKey]
                    ?.mapNotNull(ZmanimTimeOption::fromStorageValue)?.toSet()
                    ?: ZmanimTimeOption.Default,
                themeOption = rootUiSettings.themeOption,
                zmanimSettings = decodeZmanimSettings(preferences),
                candleLightingPromptHandled = preferences[CandleLightingPromptHandled] ?: false,
                candleLightingDefault = CandleLightingMethod.fromStorageValue(preferences[CandleLightingDefaultKey]),
            )
        }

    override suspend fun seedLanguageDefault() {
        dataStore.edit { preferences ->
            // Only write if neither the current key nor the legacy migration key is present.
            // This means the user has never explicitly chosen a language and no prior migration
            // value exists — i.e., genuine first launch.
            if (AppLanguageKey !in preferences && UseHebrewInterface !in preferences) {
                preferences[AppLanguageKey] = AppLanguage.systemDefault().storageValue
            }
        }
    }

    override suspend fun setHebrewDateStatusIconEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[HebrewDateStatusIconEnabled] = enabled
        }
    }

    override suspend fun setAppLanguage(language: AppLanguage) {
        val updatedPreferences = dataStore.edit { preferences ->
            preferences[AppLanguageKey] = language.storageValue
        }
        startupSettingsCache.write(decodeRootUiSettings(updatedPreferences))
    }

    override suspend fun setUse24HourTime(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Use24HourTime] = enabled
        }
    }

    override suspend fun setEnabledDailyLearning(types: Set<DailyLearningType>) {
        dataStore.edit { preferences ->
            preferences[EnabledDailyLearningKey] = types.map(DailyLearningType::storageValue).toSet()
        }
    }

    override suspend fun setEnabledZmanimTimes(options: Set<ZmanimTimeOption>) {
        dataStore.edit { preferences ->
            preferences[EnabledZmanimTimesKey] = options.map(ZmanimTimeOption::storageValue).toSet()
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
            preferences[UseElevation] = settings.useElevation
            preferences[AlotHashacharMethodKey] = settings.alotHashacharMethod.storageValue
            preferences[MisheyakirMethodKey] = settings.misheyakirMethod.storageValue
            preferences[SunriseMethodKey] = settings.sunriseMethod.storageValue
            preferences[SofZmanShemaGraMethodKey] = settings.sofZmanShemaGraMethod.storageValue
            preferences[SofZmanShemaMethodKey] = settings.sofZmanShemaMethod.storageValue
            preferences[SofZmanTefillahGraMethodKey] = settings.sofZmanTefillahGraMethod.storageValue
            preferences[SofZmanTefillahMethodKey] = settings.sofZmanTefillahMethod.storageValue
            preferences[ChatzotMethodKey] = settings.chatzotMethod.storageValue
            preferences[ChatzotHaLailaMethodKey] = settings.chatzotHaLailaMethod.storageValue
            preferences[MinchaGedolaMethodKey] = settings.minchaGedolaMethod.storageValue
            preferences[MinchaKetanaMethodKey] = settings.minchaKetanaMethod.storageValue
            preferences[PlagHaminchaMethodKey] = settings.plagHaminchaMethod.storageValue
            preferences[SunsetMethodKey] = settings.sunsetMethod.storageValue
            preferences[TzeitHakochavimMethodKey] = settings.tzeitHakochavimMethod.storageValue
            preferences[CandleLightingMethodKey] = settings.candleLightingMethod.storageValue
            preferences[MotzeiShabbatMethodKey] = settings.motzeiShabbatMethod.storageValue
            preferences[RabbeinuTamMethodKey] = settings.rabbeinuTamMethod.storageValue
            preferences[ChametzMethodKey] = settings.chametzMethod.storageValue
            preferences[AteretTorahOffsetMinutes] = settings.ateretTorahSunsetOffsetMinutes
        }
    }

    override suspend fun setCandleLightingPromptHandled(handled: Boolean) {
        dataStore.edit { preferences ->
            preferences[CandleLightingPromptHandled] = handled
        }
    }

    override suspend fun setCandleLightingDefault(method: CandleLightingMethod) {
        dataStore.edit { preferences ->
            preferences[CandleLightingDefaultKey] = method.storageValue
        }
    }

    private fun decodeZmanimSettings(preferences: Preferences): ZmanimCalculationSettings {
        // Single source of truth for fallbacks: absent keys resolve to the model's own
        // defaults, so a default change in ZmanimCalculationSettings can never drift from
        // what a fresh install decodes.
        val defaults = ZmanimCalculationSettings()
        return ZmanimCalculationSettings(
            preset = ZmanimPreset.fromStorageValue(preferences[ZmanimPresetKey]) ?: defaults.preset,
            useElevation = preferences[UseElevation] ?: defaults.useElevation,
            alotHashacharMethod = AlotHashacharMethod.fromStorageValue(preferences[AlotHashacharMethodKey])
                ?: preferences[AlotHashacharOffsetMinutes]?.let { legacyAlotMethod(it) }
                ?: defaults.alotHashacharMethod,
            misheyakirMethod = MisheyakirMethod.fromStorageValue(preferences[MisheyakirMethodKey]) ?: defaults.misheyakirMethod,
            sunriseMethod = SunriseMethod.fromStorageValue(preferences[SunriseMethodKey])
                ?: if (preferences[UseSeaLevelSunrise] == false) SunriseMethod.ElevationAdjusted else defaults.sunriseMethod,
            sofZmanShemaGraMethod = SofZmanShemaMethod.fromStorageValue(preferences[SofZmanShemaGraMethodKey])
                ?: defaults.sofZmanShemaGraMethod,
            sofZmanShemaMethod = SofZmanShemaMethod.fromStorageValue(preferences[SofZmanShemaMethodKey])
                ?: defaults.sofZmanShemaMethod,
            sofZmanTefillahGraMethod = SofZmanTefillahMethod.fromStorageValue(preferences[SofZmanTefillahGraMethodKey])
                ?: defaults.sofZmanTefillahGraMethod,
            sofZmanTefillahMethod = SofZmanTefillahMethod.fromStorageValue(preferences[SofZmanTefillahMethodKey])
                ?: defaults.sofZmanTefillahMethod,
            chatzotMethod = ChatzotMethod.fromStorageValue(preferences[ChatzotMethodKey]) ?: defaults.chatzotMethod,
            chatzotHaLailaMethod = ChatzotMethod.fromStorageValue(preferences[ChatzotHaLailaMethodKey]) ?: defaults.chatzotHaLailaMethod,
            minchaGedolaMethod = MinchaGedolaMethod.fromStorageValue(preferences[MinchaGedolaMethodKey]) ?: defaults.minchaGedolaMethod,
            minchaKetanaMethod = MinchaKetanaMethod.fromStorageValue(preferences[MinchaKetanaMethodKey]) ?: defaults.minchaKetanaMethod,
            plagHaminchaMethod = PlagHaminchaMethod.fromStorageValue(preferences[PlagHaminchaMethodKey])
                ?: legacyPlagMethod(preferences[PlagHaminchaOffsetMinutes] ?: 0),
            sunsetMethod = SunsetMethod.fromStorageValue(preferences[SunsetMethodKey])
                ?: if (preferences[UseSeaLevelSunset] == false) SunsetMethod.ElevationAdjusted else defaults.sunsetMethod,
            tzeitHakochavimMethod = TzeitHakochavimMethod.fromStorageValue(preferences[TzeitHakochavimMethodKey]) ?: defaults.tzeitHakochavimMethod,
            candleLightingMethod = CandleLightingMethod.fromStorageValue(preferences[CandleLightingMethodKey])
                ?: legacyCandleMethod(preferences[CandleLightingOffsetMinutes] ?: defaults.candleLightingMethod.offsetMinutes),
            motzeiShabbatMethod = MotzeiShabbatMethod.fromStorageValue(preferences[MotzeiShabbatMethodKey]) ?: defaults.motzeiShabbatMethod,
            rabbeinuTamMethod = RabbeinuTamMethod.fromStorageValue(preferences[RabbeinuTamMethodKey]) ?: defaults.rabbeinuTamMethod,
            chametzMethod = ChametzMethod.fromStorageValue(preferences[ChametzMethodKey]) ?: defaults.chametzMethod,
            ateretTorahSunsetOffsetMinutes = preferences[AteretTorahOffsetMinutes] ?: defaults.ateretTorahSunsetOffsetMinutes,
        )
    }

    private fun decodeRootUiSettings(preferences: Preferences): RootUiSettings = RootUiSettings(
        themeOption = decodeThemeOption(preferences),
        language = decodeLanguage(preferences),
    )

    private fun decodeLanguage(preferences: Preferences): AppLanguage =
        AppLanguage.fromStorageValue(preferences[AppLanguageKey])
        // Migrate older installs that stored only the Hebrew on/off boolean.
            ?: preferences[UseHebrewInterface]?.let { if (it) AppLanguage.Hebrew else AppLanguage.English }
            // First launch: follow the device language.
            ?: AppLanguage.systemDefault()

    private fun decodeThemeOption(preferences: Preferences): AppThemeOption =
        AppThemeOption.fromStorageValue(preferences[ThemeOption])
            ?: when {
                preferences[AmoledBlackTheme] == true -> AppThemeOption.AmoledBlack
                preferences[BlueWhiteTheme] == true -> AppThemeOption.BlueWhite
                else -> AppThemeOption.Default
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
        val HebrewDateStatusIconEnabled = booleanPreferencesKey("hebrew_date_status_icon_enabled")
        val AppLanguageKey = stringPreferencesKey("app_language")
        // Retained read-only to migrate installs that predate the language picker.
        val UseHebrewInterface = booleanPreferencesKey("use_hebrew_interface")
        val Use24HourTime = booleanPreferencesKey("use_24_hour_time")
        val EnabledDailyLearningKey = stringSetPreferencesKey("enabled_daily_learning")
        val EnabledZmanimTimesKey = stringSetPreferencesKey("enabled_zmanim_times")
        val CandleLightingPromptHandled = booleanPreferencesKey("candle_lighting_prompt_handled")
        val CandleLightingDefaultKey = stringPreferencesKey("candle_lighting_default")
        val ThemeOption = stringPreferencesKey("theme_option")
        val BlueWhiteTheme = booleanPreferencesKey("blue_white_theme")
        val AmoledBlackTheme = booleanPreferencesKey("amoled_black_theme")
        val ZmanimPresetKey = stringPreferencesKey("zmanim_preset")
        val AlotHashacharMethodKey = stringPreferencesKey("zmanim_alot_method")
        val MisheyakirMethodKey = stringPreferencesKey("zmanim_misheyakir_method")
        val SunriseMethodKey = stringPreferencesKey("zmanim_sunrise_method")
        val SofZmanShemaGraMethodKey = stringPreferencesKey("zmanim_shema_gra_method")
        val SofZmanShemaMethodKey = stringPreferencesKey("zmanim_shema_method")
        val SofZmanTefillahGraMethodKey = stringPreferencesKey("zmanim_tefillah_gra_method")
        val SofZmanTefillahMethodKey = stringPreferencesKey("zmanim_tefillah_method")
        val ChatzotMethodKey = stringPreferencesKey("zmanim_chatzot_method")
        val ChatzotHaLailaMethodKey = stringPreferencesKey("zmanim_chatzot_halaila_method")
        val MinchaGedolaMethodKey = stringPreferencesKey("zmanim_mincha_gedola_method")
        val MinchaKetanaMethodKey = stringPreferencesKey("zmanim_mincha_ketana_method")
        val PlagHaminchaMethodKey = stringPreferencesKey("zmanim_plag_method")
        val SunsetMethodKey = stringPreferencesKey("zmanim_sunset_method")
        val TzeitHakochavimMethodKey = stringPreferencesKey("zmanim_tzeit_method")
        val CandleLightingMethodKey = stringPreferencesKey("zmanim_candle_method")
        val MotzeiShabbatMethodKey = stringPreferencesKey("zmanim_motzei_method")
        val RabbeinuTamMethodKey = stringPreferencesKey("zmanim_rabbeinu_tam_method")
        val ChametzMethodKey = stringPreferencesKey("zmanim_chametz_method")
        val AteretTorahOffsetMinutes = intPreferencesKey("zmanim_ateret_torah_offset_minutes")
        val UseElevation = booleanPreferencesKey("zmanim_use_elevation")
        val AlotHashacharOffsetMinutes = intPreferencesKey("zmanim_alot_offset_minutes")
        val PlagHaminchaOffsetMinutes = intPreferencesKey("zmanim_plag_offset_minutes")
        val UseSeaLevelSunrise = booleanPreferencesKey("zmanim_use_sea_level_sunrise")
        val UseSeaLevelSunset = booleanPreferencesKey("zmanim_use_sea_level_sunset")
        val CandleLightingOffsetMinutes = intPreferencesKey("zmanim_candle_lighting_offset_minutes")
    }
}
