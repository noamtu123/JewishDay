// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.noamtu.jewishday.model.AlotHashacharMethod
import com.noamtu.jewishday.model.CandleLightingMethod
import com.noamtu.jewishday.model.ChametzMethod
import com.noamtu.jewishday.model.ChatzotMethod
import com.noamtu.jewishday.model.CustomZmanUnit
import com.noamtu.jewishday.model.CustomZmanValue
import com.noamtu.jewishday.model.DailyLearningType
import com.noamtu.jewishday.model.LegacyLadderChoice
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
import com.noamtu.jewishday.model.ZmanimTimeOption
import com.noamtu.jewishday.model.legacyLadderChoice
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Every theme is a fixed palette. None of them follows the system light/dark setting — a theme is a
 * look the user picked, not a mode, so the phone's setting must not quietly repaint it. That used
 * to be true of all but one: "Classic calm" swapped between two palettes with the system, and its
 * light half now stands alone as Olive grove.
 *
 * Ordered light first, then dark, which is the order the picker shows.
 */
enum class AppThemeOption(val storageValue: String) {
    BlueWhite("blue_white"),
    OliveGrove("olive_grove"),
    JerusalemStone("jerusalem_stone"),
    Sand("sand"),
    Midnight("midnight"),
    Slate("slate"),
    AmoledBlack("amoled_black"),
    Glass("glass"),
    ;

    companion object {
        /** The app-wide default applied on first launch and as a fallback. */
        val Default = BlueWhite

        fun fromStorageValue(value: String?): AppThemeOption? =
            entries.firstOrNull { it.storageValue == value }
    }
}

data class AppSettings(
    val hebrewDateStatusIconEnabled: Boolean = true,
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
    /**
     * The user answered the location prompt with "always use Jerusalem", so it is never shown
     * again. Granting location clears it: that is a clearer statement of intent than the setting.
     */
    val alwaysUseJerusalem: Boolean = false,
    /**
     * Offer pre-releases as updates too. Off by default: a stable install must never be pulled onto
     * a test build by accident, and a pre-release cannot be uninstalled back down from in place.
     */
    val includePreReleases: Boolean = false,
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
    suspend fun setAlwaysUseJerusalem(enabled: Boolean)
    suspend fun setIncludePreReleases(enabled: Boolean)
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
                hebrewDateStatusIconEnabled = preferences[HebrewDateStatusIconEnabled] ?: true,
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
                alwaysUseJerusalem = preferences[AlwaysUseJerusalem] ?: false,
                includePreReleases = preferences[IncludePreReleases] ?: false,
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
            preferences[CandleLightingCustomMinutes] = settings.candleLightingCustomMinutes
            preferences[HolyDayTosefetMinutes] = settings.holyDayTosefetMinutes
            preferences[AteretTorahOffsetMinutes] = settings.ateretTorahSunsetOffsetMinutes
            // The numbers behind the custom options, one set per zman. All three units are stored,
            // not just the one selected, so switching units and back does not lose what was typed.
            preferences.putCustomValues(AlotZman, settings.alotHashacharCustom)
            preferences.putCustomValues(MisheyakirZman, settings.misheyakirCustom)
            preferences.putCustomValues(ShemaZman, settings.sofZmanShemaCustom)
            preferences.putCustomValues(TefillahZman, settings.sofZmanTefillahCustom)
            preferences.putCustomValues(MinchaGedolaZman, settings.minchaGedolaCustom)
            preferences.putCustomValues(MinchaKetanaZman, settings.minchaKetanaCustom)
            preferences.putCustomValues(PlagZman, settings.plagHaminchaCustom)
            preferences.putCustomValues(TzeitZman, settings.tzeitHakochavimCustom)
            preferences.putCustomValues(MotzeiZman, settings.motzeiShabbatCustom)
            preferences.putCustomValues(RabbeinuTamZman, settings.rabbeinuTamCustom)
            preferences.putCustomValues(ChametzZman, settings.chametzCustom)
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

    override suspend fun setAlwaysUseJerusalem(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AlwaysUseJerusalem] = enabled
        }
    }

    override suspend fun setIncludePreReleases(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[IncludePreReleases] = enabled
        }
    }

    private fun decodeZmanimSettings(preferences: Preferences): ZmanimCalculationSettings {
        // Single source of truth for fallbacks: absent keys resolve to the model's own
        // defaults, so a default change in ZmanimCalculationSettings can never drift from
        // what a fresh install decodes.
        val defaults = ZmanimCalculationSettings()
        // The stored method, when this build still offers it. Options whose storage value survived
        // must be resolved first: several of them read like ladder rungs ("minutes_72", "degrees_6_2")
        // and would otherwise be mistaken for one and overwrite that zman's typed-in numbers.
        val storedAlot = AlotHashacharMethod.fromStorageValue(preferences[AlotHashacharMethodKey])
        val storedMisheyakir = MisheyakirMethod.fromStorageValue(preferences[MisheyakirMethodKey])
        val storedShema = SofZmanShemaMethod.fromStorageValue(preferences[SofZmanShemaMethodKey])
        val storedTefillah = SofZmanTefillahMethod.fromStorageValue(preferences[SofZmanTefillahMethodKey])
        val storedMinchaGedola = MinchaGedolaMethod.fromStorageValue(preferences[MinchaGedolaMethodKey])
        val storedMinchaKetana = MinchaKetanaMethod.fromStorageValue(preferences[MinchaKetanaMethodKey])
        val storedPlag = PlagHaminchaMethod.fromStorageValue(preferences[PlagHaminchaMethodKey])
        val storedTzeit = TzeitHakochavimMethod.fromStorageValue(preferences[TzeitHakochavimMethodKey])
        val storedMotzei = MotzeiShabbatMethod.fromStorageValue(preferences[MotzeiShabbatMethodKey])
        val storedRabbeinuTam = RabbeinuTamMethod.fromStorageValue(preferences[RabbeinuTamMethodKey])
        val storedChametz = ChametzMethod.fromStorageValue(preferences[ChametzMethodKey])

        // Otherwise: a rung of one of the old fixed ladders, recovered as the matching custom option
        // carrying the same number, so an upgrade does not silently move anyone's zmanim. Two zmanim
        // go back further still, to when the choice was stored as a bare minute offset.
        val alotLadder = storedAlot.orLadder(preferences[AlotHashacharMethodKey])
            ?: retiredOption(storedAlot, preferences[AlotHashacharMethodKey], BaalHatanyaValue, CustomZmanUnit.Degrees, 16.9)
            ?: preferences[AlotHashacharOffsetMinutes]?.takeIf { storedAlot == null }?.let { minutesLadder(it) }
        val misheyakirLadder = storedMisheyakir.orLadder(preferences[MisheyakirMethodKey])
        val shemaLadder = storedShema.orLadder(preferences[SofZmanShemaMethodKey])
        val tefillahLadder = storedTefillah.orLadder(preferences[SofZmanTefillahMethodKey])
        val minchaGedolaLadder = storedMinchaGedola.orLadder(preferences[MinchaGedolaMethodKey])
        val minchaKetanaLadder = storedMinchaKetana.orLadder(preferences[MinchaKetanaMethodKey])
        // Zero meant the GRA, which is the default anyway.
        val plagLadder = storedPlag.orLadder(preferences[PlagHaminchaMethodKey])
            ?: preferences[PlagHaminchaOffsetMinutes]
                ?.takeIf { storedPlag == null && it > 0 }
                ?.let { minutesLadder(it) }
        // Ateret Torah's nightfall was simply this many minutes after sunset, so it carries over as
        // that — it is no longer offered as a nightfall opinion, since he never published one.
        val ateretTorahOffset = (preferences[AteretTorahOffsetMinutes] ?: defaults.ateretTorahSunsetOffsetMinutes).toDouble()
        val tzeitLadder = storedTzeit.orLadder(preferences[TzeitHakochavimMethodKey])
            ?: retiredOption(storedTzeit, preferences[TzeitHakochavimMethodKey], BaalHatanyaValue, CustomZmanUnit.Degrees, 6.0)
            ?: retiredOption(storedTzeit, preferences[TzeitHakochavimMethodKey], AteretTorahValue, CustomZmanUnit.Minutes, ateretTorahOffset)
        val motzeiLadder = storedMotzei.orLadder(preferences[MotzeiShabbatMethodKey])
            ?: retiredOption(storedMotzei, preferences[MotzeiShabbatMethodKey], BaalHatanyaValue, CustomZmanUnit.Degrees, 6.0)
            ?: retiredOption(storedMotzei, preferences[MotzeiShabbatMethodKey], AteretTorahValue, CustomZmanUnit.Minutes, ateretTorahOffset)
        val rabbeinuTamLadder = storedRabbeinuTam.orLadder(preferences[RabbeinuTamMethodKey])
            ?: retiredOption(
                storedRabbeinuTam,
                preferences[RabbeinuTamMethodKey],
                BainHashmashot13Point24Value,
                CustomZmanUnit.Degrees,
                13.24,
            )
        val chametzLadder = storedChametz.orLadder(preferences[ChametzMethodKey])
        // 18 minutes was a fixed option until it became the custom field's starting value, so a
        // stored "minutes_18" is read as that: the custom option carrying 18.
        val storedCandle = CandleLightingMethod.fromStorageValue(preferences[CandleLightingMethodKey])
        val candleLadderMinutes = storedCandle.orLadder(preferences[CandleLightingMethodKey])?.value?.toInt()
            ?: preferences[CandleLightingOffsetMinutes]?.takeIf { storedCandle == null }
        return ZmanimCalculationSettings(
            alotHashacharMethod = storedAlot
                ?: alotLadder?.let { alotHashacharMethodFor(it.unit) }
                ?: defaults.alotHashacharMethod,
            alotHashacharCustom = preferences.customValues(AlotZman, defaults.alotHashacharCustom, alotLadder),
            misheyakirMethod = storedMisheyakir
                ?: misheyakirLadder?.let { misheyakirMethodFor(it.unit) }
                ?: defaults.misheyakirMethod,
            misheyakirCustom = preferences.customValues(MisheyakirZman, defaults.misheyakirCustom, misheyakirLadder),
            sunriseMethod = SunriseMethod.fromStorageValue(preferences[SunriseMethodKey])
                ?: if (preferences[UseSeaLevelSunrise] == false) SunriseMethod.ElevationAdjusted else defaults.sunriseMethod,
            sofZmanShemaGraMethod = SofZmanShemaMethod.fromStorageValue(preferences[SofZmanShemaGraMethodKey])
                ?: defaults.sofZmanShemaGraMethod,
            sofZmanShemaMethod = storedShema
                ?: shemaLadder?.let { sofZmanShemaMethodFor(it) }
                ?: defaults.sofZmanShemaMethod,
            sofZmanShemaCustom = preferences.customValues(ShemaZman, defaults.sofZmanShemaCustom, shemaLadder),
            sofZmanTefillahGraMethod = SofZmanTefillahMethod.fromStorageValue(preferences[SofZmanTefillahGraMethodKey])
                ?: defaults.sofZmanTefillahGraMethod,
            sofZmanTefillahMethod = storedTefillah
                ?: tefillahLadder?.let { sofZmanTefillahMethodFor(it.unit) }
                ?: defaults.sofZmanTefillahMethod,
            sofZmanTefillahCustom = preferences.customValues(TefillahZman, defaults.sofZmanTefillahCustom, tefillahLadder),
            chatzotMethod = ChatzotMethod.fromStorageValue(preferences[ChatzotMethodKey]) ?: defaults.chatzotMethod,
            chatzotHaLailaMethod = ChatzotMethod.fromStorageValue(preferences[ChatzotHaLailaMethodKey]) ?: defaults.chatzotHaLailaMethod,
            minchaGedolaMethod = storedMinchaGedola
                ?: minchaGedolaLadder?.let { minchaGedolaMethodFor(it.unit) }
                ?: defaults.minchaGedolaMethod,
            minchaGedolaCustom = preferences.customValues(MinchaGedolaZman, defaults.minchaGedolaCustom, minchaGedolaLadder),
            minchaKetanaMethod = storedMinchaKetana
                ?: minchaKetanaLadder?.let { minchaKetanaMethodFor(it.unit) }
                ?: defaults.minchaKetanaMethod,
            minchaKetanaCustom = preferences.customValues(MinchaKetanaZman, defaults.minchaKetanaCustom, minchaKetanaLadder),
            plagHaminchaMethod = storedPlag
                ?: plagLadder?.let { plagHaminchaMethodFor(it.unit) }
                ?: defaults.plagHaminchaMethod,
            plagHaminchaCustom = preferences.customValues(PlagZman, defaults.plagHaminchaCustom, plagLadder),
            sunsetMethod = SunsetMethod.fromStorageValue(preferences[SunsetMethodKey])
                ?: if (preferences[UseSeaLevelSunset] == false) SunsetMethod.ElevationAdjusted else defaults.sunsetMethod,
            tzeitHakochavimMethod = storedTzeit
                ?: tzeitLadder?.let { tzeitCustomMethodFor(it.unit) }
                ?: defaults.tzeitHakochavimMethod,
            tzeitHakochavimCustom = preferences.customValues(TzeitZman, defaults.tzeitHakochavimCustom, tzeitLadder),
            candleLightingMethod = storedCandle
                ?: legacyCandleMethod(candleLadderMinutes, defaults.candleLightingMethod),
            candleLightingCustomMinutes = candleLadderMinutes
                ?: preferences[CandleLightingCustomMinutes]
                ?: defaults.candleLightingCustomMinutes,
            motzeiShabbatMethod = storedMotzei
                ?: motzeiLadder?.let { motzeiCustomMethodFor(it.unit) }
                ?: defaults.motzeiShabbatMethod,
            motzeiShabbatCustom = preferences.customValues(MotzeiZman, defaults.motzeiShabbatCustom, motzeiLadder),
            rabbeinuTamMethod = storedRabbeinuTam
                ?: rabbeinuTamLadder?.let { rabbeinuTamCustomMethodFor(it.unit) }
                ?: defaults.rabbeinuTamMethod,
            rabbeinuTamCustom = preferences.customValues(RabbeinuTamZman, defaults.rabbeinuTamCustom, rabbeinuTamLadder),
            chametzMethod = storedChametz
                ?: chametzLadder?.let { chametzCustomMethodFor(it.unit) }
                ?: defaults.chametzMethod,
            chametzCustom = preferences.customValues(ChametzZman, defaults.chametzCustom, chametzLadder),
            holyDayTosefetMinutes = preferences[HolyDayTosefetMinutes] ?: defaults.holyDayTosefetMinutes,
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

    private fun decodeThemeOption(preferences: Preferences): AppThemeOption {
        // "Classic calm" was one theme that followed the system light/dark setting; what survives
        // of it is Olive grove, so that is where a stored "classic" lands.
        if (preferences[ThemeOption] == LegacyClassicTheme) return AppThemeOption.OliveGrove
        return AppThemeOption.fromStorageValue(preferences[ThemeOption])
            ?: when {
                preferences[AmoledBlackTheme] == true -> AppThemeOption.AmoledBlack
                preferences[BlueWhiteTheme] == true -> AppThemeOption.BlueWhite
                else -> AppThemeOption.Default
            }
    }

    /**
     * The fixed minhag options if the recovered offset is one of them, else the custom option
     * carrying it. Builds before the method picker existed stored nothing but a bare offset.
     */
    private fun legacyCandleMethod(minutes: Int?, default: CandleLightingMethod): CandleLightingMethod {
        if (minutes == null) return default
        return CandleLightingMethod.PromptOptions.firstOrNull { it.offsetMinutes == minutes }
            ?: CandleLightingMethod.Custom
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
        val AlwaysUseJerusalem = booleanPreferencesKey("always_use_jerusalem")
        val IncludePreReleases = booleanPreferencesKey("include_pre_releases")
        val ThemeOption = stringPreferencesKey("theme_option")
        val BlueWhiteTheme = booleanPreferencesKey("blue_white_theme")

        /** Retained read-only: the storage value of the theme that used to follow the system. */
        const val LegacyClassicTheme: String = "classic"

        val AmoledBlackTheme = booleanPreferencesKey("amoled_black_theme")
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
        val HolyDayTosefetMinutes = intPreferencesKey("holy_day_tosefet_minutes")
        val AteretTorahOffsetMinutes = intPreferencesKey("zmanim_ateret_torah_offset_minutes")
        val AlotHashacharOffsetMinutes = intPreferencesKey("zmanim_alot_offset_minutes")
        val PlagHaminchaOffsetMinutes = intPreferencesKey("zmanim_plag_offset_minutes")
        val UseSeaLevelSunrise = booleanPreferencesKey("zmanim_use_sea_level_sunrise")
        val UseSeaLevelSunset = booleanPreferencesKey("zmanim_use_sea_level_sunset")
        val CandleLightingOffsetMinutes = intPreferencesKey("zmanim_candle_lighting_offset_minutes")
        val CandleLightingCustomMinutes = intPreferencesKey("zmanim_candle_custom_minutes")

        // Key prefixes for the per-zman custom values. Short and stable: they are part of the
        // on-disk format, so renaming one silently resets that zman's typed-in numbers.
        const val AlotZman = "alot"
        const val MisheyakirZman = "misheyakir"
        const val ShemaZman = "shema"
        const val TefillahZman = "tefillah"
        const val MinchaGedolaZman = "mincha_gedola"
        const val MinchaKetanaZman = "mincha_ketana"
        const val PlagZman = "plag"
        const val TzeitZman = "tzeit"
        const val MotzeiZman = "motzei"
        const val RabbeinuTamZman = "rabbeinu_tam"
        const val ChametzZman = "chametz"

        // Storage values of the named options that were dropped because they were only a degree
        // value. Kept here so an install that had one still computes the same times.
        const val BaalHatanyaValue = "baal_hatanya"
        const val BainHashmashot13Point24Value = "bain_hashmashot_13_24"
        const val AteretTorahValue = "ateret_torah"
    }
}

// ---------------------------------------------------------------------------------------------
// Custom zman values on disk
//
// Each zman keeps three numbers — a degree value, a minute count and a zmaniyot-minute count — under
// keys built from its own short prefix, so adding a zman is one prefix rather than three new keys.
// ---------------------------------------------------------------------------------------------

private fun customDegreesKey(zman: String) = doublePreferencesKey("zmanim_${zman}_custom_degrees")
private fun customMinutesKey(zman: String) = intPreferencesKey("zmanim_${zman}_custom_minutes")
private fun customZmaniyotKey(zman: String) = intPreferencesKey("zmanim_${zman}_custom_zmaniyot")

private fun MutablePreferences.putCustomValues(zman: String, values: CustomZmanValue) {
    this[customDegreesKey(zman)] = values.degrees
    this[customMinutesKey(zman)] = values.minutes
    this[customZmaniyotKey(zman)] = values.zmaniyotMinutes
}

/**
 * The stored numbers for [zman], with [legacy] — the ladder rung an older build had selected —
 * written over the matching one, so that rung becomes this build's custom value for it.
 */
private fun Preferences.customValues(
    zman: String,
    defaults: CustomZmanValue,
    legacy: LegacyLadderChoice?,
): CustomZmanValue {
    val stored = CustomZmanValue(
        degrees = this[customDegreesKey(zman)] ?: defaults.degrees,
        minutes = this[customMinutesKey(zman)] ?: defaults.minutes,
        zmaniyotMinutes = this[customZmaniyotKey(zman)] ?: defaults.zmaniyotMinutes,
    )
    return legacy?.let { stored.withValue(it.unit, it.value) } ?: stored
}

/**
 * A named option that was dropped because it was only a number in disguise, recovered as that number:
 * the Baal Hatanya's dawn is 16.9° and his nightfall 6°, Rabbeinu Tam's Bein Hashmashot 13.24° is
 * 13.24°, and Ateret Torah's nightfall was a fixed offset after sunset. Only the name is gone; the
 * time it produced is not.
 */
private fun retiredOption(
    resolved: Any?,
    stored: String?,
    name: String,
    unit: CustomZmanUnit,
    value: Double,
): LegacyLadderChoice? =
    if (resolved != null || stored != name) null else LegacyLadderChoice(unit, value)

/** The ladder rung [stored] names, but only when this build did not recognise it as an option. */
private fun Any?.orLadder(stored: String?): LegacyLadderChoice? =
    if (this != null) null else legacyLadderChoice(stored)

/** A bare minute count from a build that predates the method pickers entirely. */
private fun minutesLadder(minutes: Int) = LegacyLadderChoice(CustomZmanUnit.Minutes, minutes.toDouble())

// Which custom option of each zman a recovered rung belongs to.

private fun alotHashacharMethodFor(unit: CustomZmanUnit) = when (unit) {
    CustomZmanUnit.Degrees -> AlotHashacharMethod.CustomDegrees
    CustomZmanUnit.Minutes -> AlotHashacharMethod.CustomMinutes
    CustomZmanUnit.ZmaniyotMinutes -> AlotHashacharMethod.CustomZmaniyotMinutes
}

private fun misheyakirMethodFor(unit: CustomZmanUnit) = when (unit) {
    CustomZmanUnit.Degrees -> MisheyakirMethod.CustomDegrees
    CustomZmanUnit.Minutes -> MisheyakirMethod.CustomMinutesBeforeSunrise
    CustomZmanUnit.ZmaniyotMinutes -> MisheyakirMethod.CustomZmaniyotMinutesBeforeSunrise
}

private fun sofZmanShemaMethodFor(ladder: LegacyLadderChoice) = when {
    ladder.toFixedLocalChatzot && ladder.unit == CustomZmanUnit.Degrees ->
        SofZmanShemaMethod.CustomDegreesToFixedLocalChatzot
    ladder.toFixedLocalChatzot -> SofZmanShemaMethod.CustomMinutesToFixedLocalChatzot
    ladder.unit == CustomZmanUnit.Degrees -> SofZmanShemaMethod.CustomDegrees
    ladder.unit == CustomZmanUnit.Minutes -> SofZmanShemaMethod.CustomMinutes
    else -> SofZmanShemaMethod.CustomZmaniyotMinutes
}

private fun sofZmanTefillahMethodFor(unit: CustomZmanUnit) = when (unit) {
    CustomZmanUnit.Degrees -> SofZmanTefillahMethod.CustomDegrees
    CustomZmanUnit.Minutes -> SofZmanTefillahMethod.CustomMinutes
    CustomZmanUnit.ZmaniyotMinutes -> SofZmanTefillahMethod.CustomZmaniyotMinutes
}

private fun minchaGedolaMethodFor(unit: CustomZmanUnit) = when (unit) {
    CustomZmanUnit.Degrees -> MinchaGedolaMethod.CustomDegrees
    CustomZmanUnit.Minutes -> MinchaGedolaMethod.CustomMinutes
    CustomZmanUnit.ZmaniyotMinutes -> MinchaGedolaMethod.CustomZmaniyotMinutes
}

private fun minchaKetanaMethodFor(unit: CustomZmanUnit) = when (unit) {
    CustomZmanUnit.Degrees -> MinchaKetanaMethod.CustomDegrees
    CustomZmanUnit.Minutes -> MinchaKetanaMethod.CustomMinutes
    CustomZmanUnit.ZmaniyotMinutes -> MinchaKetanaMethod.CustomZmaniyotMinutes
}

private fun plagHaminchaMethodFor(unit: CustomZmanUnit) = when (unit) {
    CustomZmanUnit.Degrees -> PlagHaminchaMethod.CustomDegrees
    CustomZmanUnit.Minutes -> PlagHaminchaMethod.CustomMinutes
    CustomZmanUnit.ZmaniyotMinutes -> PlagHaminchaMethod.CustomZmaniyotMinutes
}

private fun tzeitCustomMethodFor(unit: CustomZmanUnit) = when (unit) {
    CustomZmanUnit.Degrees -> TzeitHakochavimMethod.CustomDegrees
    CustomZmanUnit.Minutes -> TzeitHakochavimMethod.CustomMinutes
    CustomZmanUnit.ZmaniyotMinutes -> TzeitHakochavimMethod.CustomZmaniyotMinutes
}

private fun motzeiCustomMethodFor(unit: CustomZmanUnit) = when (unit) {
    CustomZmanUnit.Degrees -> MotzeiShabbatMethod.CustomDegrees
    CustomZmanUnit.Minutes -> MotzeiShabbatMethod.CustomMinutes
    CustomZmanUnit.ZmaniyotMinutes -> MotzeiShabbatMethod.CustomZmaniyotMinutes
}

private fun rabbeinuTamCustomMethodFor(unit: CustomZmanUnit) = when (unit) {
    CustomZmanUnit.Degrees -> RabbeinuTamMethod.CustomDegrees
    CustomZmanUnit.Minutes -> RabbeinuTamMethod.CustomMinutes
    CustomZmanUnit.ZmaniyotMinutes -> RabbeinuTamMethod.CustomZmaniyotMinutes
}

private fun chametzCustomMethodFor(unit: CustomZmanUnit) = when (unit) {
    CustomZmanUnit.Degrees -> ChametzMethod.CustomDegrees
    CustomZmanUnit.Minutes -> ChametzMethod.CustomMinutes
    CustomZmanUnit.ZmaniyotMinutes -> ChametzMethod.CustomZmaniyotMinutes
}
