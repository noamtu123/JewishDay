// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.noamtu.jewishday.core.model.AlotHashacharMethod
import com.noamtu.jewishday.core.model.CandleLightingMethod
import com.noamtu.jewishday.core.model.ChametzMethod
import com.noamtu.jewishday.core.model.ChatzotMethod
import com.noamtu.jewishday.core.model.CustomZmanUnit
import com.noamtu.jewishday.core.model.CustomZmanValue
import com.noamtu.jewishday.core.model.LegacyLadderChoice
import com.noamtu.jewishday.core.model.MinchaGedolaMethod
import com.noamtu.jewishday.core.model.MinchaKetanaMethod
import com.noamtu.jewishday.core.model.MisheyakirMethod
import com.noamtu.jewishday.core.model.MotzeiShabbatMethod
import com.noamtu.jewishday.core.model.NoTzeitFallback
import com.noamtu.jewishday.core.model.PlagHaminchaMethod
import com.noamtu.jewishday.core.model.RabbeinuTamMethod
import com.noamtu.jewishday.core.model.SofZmanShemaMethod
import com.noamtu.jewishday.core.model.SofZmanTefillahMethod
import com.noamtu.jewishday.core.model.SunriseMethod
import com.noamtu.jewishday.core.model.SunsetMethod
import com.noamtu.jewishday.core.model.TzeitHakochavimMethod
import com.noamtu.jewishday.core.model.ZmanimCalculationSettings

// How the zman opinions are laid out in the settings DataStore. The key names and storage values are
// the on-disk format: renaming one silently resets what an install had chosen. What older builds
// wrote is read through LegacyZmanimSettings.kt, so an upgrade keeps its times.

internal fun Preferences.zmanimSettings(): ZmanimCalculationSettings {
    // Single source of truth for fallbacks: absent keys resolve to the model's own
    // defaults, so a default change in ZmanimCalculationSettings can never drift from
    // what a fresh install decodes.
    val defaults = ZmanimCalculationSettings()
    // The stored method, when this build still offers it. Options whose storage value survived
    // must be resolved first: several of them read like ladder rungs ("minutes_72", "degrees_6_2")
    // and would otherwise be mistaken for one and overwrite that zman's typed-in numbers.
    val storedAlot = AlotHashacharMethod.fromStorageValue(this[AlotHashacharMethodKey])
    val storedMisheyakir = MisheyakirMethod.fromStorageValue(this[MisheyakirMethodKey])
    val storedShema = SofZmanShemaMethod.fromStorageValue(this[SofZmanShemaMethodKey])
    val storedTefillah = SofZmanTefillahMethod.fromStorageValue(this[SofZmanTefillahMethodKey])
    val storedMinchaGedola = MinchaGedolaMethod.fromStorageValue(this[MinchaGedolaMethodKey])
    val storedMinchaKetana = MinchaKetanaMethod.fromStorageValue(this[MinchaKetanaMethodKey])
    val storedPlag = PlagHaminchaMethod.fromStorageValue(this[PlagHaminchaMethodKey])
    val storedTzeit = TzeitHakochavimMethod.fromStorageValue(this[TzeitHakochavimMethodKey])
    val storedMotzei = MotzeiShabbatMethod.fromStorageValue(this[MotzeiShabbatMethodKey])
    val storedRabbeinuTam = RabbeinuTamMethod.fromStorageValue(this[RabbeinuTamMethodKey])
    val storedChametz = ChametzMethod.fromStorageValue(this[ChametzMethodKey])

    // Otherwise: a rung of one of the old fixed ladders, recovered as the matching custom option
    // carrying the same number, so an upgrade does not silently move anyone's zmanim. Two zmanim
    // go back further still, to when the choice was stored as a bare minute offset.
    val alotLadder = storedAlot.orLadder(this[AlotHashacharMethodKey])
        ?: retiredOption(storedAlot, this[AlotHashacharMethodKey], BaalHatanyaValue, CustomZmanUnit.Degrees, 16.9)
        ?: this[AlotHashacharOffsetMinutes]?.takeIf { storedAlot == null }?.let { minutesLadder(it) }
    val misheyakirLadder = storedMisheyakir.orLadder(this[MisheyakirMethodKey])
    val shemaLadder = storedShema.orLadder(this[SofZmanShemaMethodKey])
    val tefillahLadder = storedTefillah.orLadder(this[SofZmanTefillahMethodKey])
    val minchaGedolaLadder = storedMinchaGedola.orLadder(this[MinchaGedolaMethodKey])
    val minchaKetanaLadder = storedMinchaKetana.orLadder(this[MinchaKetanaMethodKey])
    // Zero meant the GRA, which is the default anyway.
    val plagLadder = storedPlag.orLadder(this[PlagHaminchaMethodKey])
        ?: this[PlagHaminchaOffsetMinutes]
            ?.takeIf { storedPlag == null && it > 0 }
            ?.let { minutesLadder(it) }
    // Ateret Torah's nightfall was simply this many minutes after sunset, so it carries over as
    // that — it is no longer offered as a nightfall opinion, since he never published one.
    val ateretTorahOffset = (this[AteretTorahOffsetMinutes] ?: defaults.ateretTorahSunsetOffsetMinutes).toDouble()
    val tzeitLadder = storedTzeit.orLadder(this[TzeitHakochavimMethodKey])
        ?: retiredOption(storedTzeit, this[TzeitHakochavimMethodKey], BaalHatanyaValue, CustomZmanUnit.Degrees, 6.0)
        ?: retiredOption(storedTzeit, this[TzeitHakochavimMethodKey], AteretTorahValue, CustomZmanUnit.Minutes, ateretTorahOffset)
    val motzeiLadder = storedMotzei.orLadder(this[MotzeiShabbatMethodKey])
        ?: retiredOption(storedMotzei, this[MotzeiShabbatMethodKey], BaalHatanyaValue, CustomZmanUnit.Degrees, 6.0)
        ?: retiredOption(storedMotzei, this[MotzeiShabbatMethodKey], AteretTorahValue, CustomZmanUnit.Minutes, ateretTorahOffset)
    val rabbeinuTamLadder = storedRabbeinuTam.orLadder(this[RabbeinuTamMethodKey])
        ?: retiredOption(
            storedRabbeinuTam,
            this[RabbeinuTamMethodKey],
            BainHashmashot13Point24Value,
            CustomZmanUnit.Degrees,
            13.24,
        )
    val chametzLadder = storedChametz.orLadder(this[ChametzMethodKey])
    // 18 minutes was a fixed option until it became the custom field's starting value, so a
    // stored "minutes_18" is read as that: the custom option carrying 18.
    val storedCandle = CandleLightingMethod.fromStorageValue(this[CandleLightingMethodKey])
    val candleLadderMinutes = storedCandle.orLadder(this[CandleLightingMethodKey])?.value?.toInt()
        ?: this[CandleLightingOffsetMinutes]?.takeIf { storedCandle == null }
    return ZmanimCalculationSettings(
        alotHashacharMethod = storedAlot
            ?: alotLadder?.let { alotHashacharMethodFor(it.unit) }
            ?: defaults.alotHashacharMethod,
        alotHashacharCustom = customValues(AlotZman, defaults.alotHashacharCustom, alotLadder),
        misheyakirMethod = storedMisheyakir
            ?: misheyakirLadder?.let { misheyakirMethodFor(it.unit) }
            ?: defaults.misheyakirMethod,
        misheyakirCustom = customValues(MisheyakirZman, defaults.misheyakirCustom, misheyakirLadder),
        sunriseMethod = SunriseMethod.fromStorageValue(this[SunriseMethodKey])
            ?: if (this[UseSeaLevelSunrise] == false) SunriseMethod.ElevationAdjusted else defaults.sunriseMethod,
        sofZmanShemaGraMethod = SofZmanShemaMethod.fromStorageValue(this[SofZmanShemaGraMethodKey])
            ?: defaults.sofZmanShemaGraMethod,
        sofZmanShemaMethod = storedShema
            ?: shemaLadder?.let { sofZmanShemaMethodFor(it) }
            ?: defaults.sofZmanShemaMethod,
        sofZmanShemaCustom = customValues(ShemaZman, defaults.sofZmanShemaCustom, shemaLadder),
        sofZmanTefillahGraMethod = SofZmanTefillahMethod.fromStorageValue(this[SofZmanTefillahGraMethodKey])
            ?: defaults.sofZmanTefillahGraMethod,
        sofZmanTefillahMethod = storedTefillah
            ?: tefillahLadder?.let { sofZmanTefillahMethodFor(it.unit) }
            ?: defaults.sofZmanTefillahMethod,
        sofZmanTefillahCustom = customValues(TefillahZman, defaults.sofZmanTefillahCustom, tefillahLadder),
        chatzotMethod = ChatzotMethod.fromStorageValue(this[ChatzotMethodKey]) ?: defaults.chatzotMethod,
        chatzotHaLailaMethod = ChatzotMethod.fromStorageValue(this[ChatzotHaLailaMethodKey]) ?: defaults.chatzotHaLailaMethod,
        minchaGedolaMethod = storedMinchaGedola
            ?: minchaGedolaLadder?.let { minchaGedolaMethodFor(it.unit) }
            ?: defaults.minchaGedolaMethod,
        minchaGedolaCustom = customValues(MinchaGedolaZman, defaults.minchaGedolaCustom, minchaGedolaLadder),
        minchaKetanaMethod = storedMinchaKetana
            ?: minchaKetanaLadder?.let { minchaKetanaMethodFor(it.unit) }
            ?: defaults.minchaKetanaMethod,
        minchaKetanaCustom = customValues(MinchaKetanaZman, defaults.minchaKetanaCustom, minchaKetanaLadder),
        plagHaminchaMethod = storedPlag
            ?: plagLadder?.let { plagHaminchaMethodFor(it.unit) }
            ?: defaults.plagHaminchaMethod,
        plagHaminchaCustom = customValues(PlagZman, defaults.plagHaminchaCustom, plagLadder),
        sunsetMethod = SunsetMethod.fromStorageValue(this[SunsetMethodKey])
            ?: if (this[UseSeaLevelSunset] == false) SunsetMethod.ElevationAdjusted else defaults.sunsetMethod,
        tzeitHakochavimMethod = storedTzeit
            ?: tzeitLadder?.let { tzeitCustomMethodFor(it.unit) }
            ?: defaults.tzeitHakochavimMethod,
        tzeitHakochavimCustom = customValues(TzeitZman, defaults.tzeitHakochavimCustom, tzeitLadder),
        noTzeitFallback = NoTzeitFallback.fromStorageValue(this[NoTzeitFallbackKey]) ?: defaults.noTzeitFallback,
        candleLightingMethod = storedCandle
            ?: legacyCandleMethod(candleLadderMinutes, defaults.candleLightingMethod),
        candleLightingCustomMinutes = candleLadderMinutes
            ?: this[CandleLightingCustomMinutes]
            ?: defaults.candleLightingCustomMinutes,
        motzeiShabbatMethod = storedMotzei
            ?: motzeiLadder?.let { motzeiCustomMethodFor(it.unit) }
            ?: defaults.motzeiShabbatMethod,
        motzeiShabbatCustom = customValues(MotzeiZman, defaults.motzeiShabbatCustom, motzeiLadder),
        rabbeinuTamMethod = storedRabbeinuTam
            ?: rabbeinuTamLadder?.let { rabbeinuTamCustomMethodFor(it.unit) }
            ?: defaults.rabbeinuTamMethod,
        rabbeinuTamCustom = customValues(RabbeinuTamZman, defaults.rabbeinuTamCustom, rabbeinuTamLadder),
        chametzMethod = storedChametz
            ?: chametzLadder?.let { chametzCustomMethodFor(it.unit) }
            ?: defaults.chametzMethod,
        chametzCustom = customValues(ChametzZman, defaults.chametzCustom, chametzLadder),
        holyDayTosefetMinutes = this[HolyDayTosefetMinutes] ?: defaults.holyDayTosefetMinutes,
        ateretTorahSunsetOffsetMinutes = this[AteretTorahOffsetMinutes] ?: defaults.ateretTorahSunsetOffsetMinutes,
    )
}

/** Writes every opinion and every custom number, so switching units and back loses nothing typed. */
internal fun MutablePreferences.putZmanimSettings(settings: ZmanimCalculationSettings) {
    this[AlotHashacharMethodKey] = settings.alotHashacharMethod.storageValue
    this[MisheyakirMethodKey] = settings.misheyakirMethod.storageValue
    this[SunriseMethodKey] = settings.sunriseMethod.storageValue
    this[SofZmanShemaGraMethodKey] = settings.sofZmanShemaGraMethod.storageValue
    this[SofZmanShemaMethodKey] = settings.sofZmanShemaMethod.storageValue
    this[SofZmanTefillahGraMethodKey] = settings.sofZmanTefillahGraMethod.storageValue
    this[SofZmanTefillahMethodKey] = settings.sofZmanTefillahMethod.storageValue
    this[ChatzotMethodKey] = settings.chatzotMethod.storageValue
    this[ChatzotHaLailaMethodKey] = settings.chatzotHaLailaMethod.storageValue
    this[MinchaGedolaMethodKey] = settings.minchaGedolaMethod.storageValue
    this[MinchaKetanaMethodKey] = settings.minchaKetanaMethod.storageValue
    this[PlagHaminchaMethodKey] = settings.plagHaminchaMethod.storageValue
    this[SunsetMethodKey] = settings.sunsetMethod.storageValue
    this[TzeitHakochavimMethodKey] = settings.tzeitHakochavimMethod.storageValue
    this[NoTzeitFallbackKey] = settings.noTzeitFallback.storageValue
    this[CandleLightingMethodKey] = settings.candleLightingMethod.storageValue
    this[MotzeiShabbatMethodKey] = settings.motzeiShabbatMethod.storageValue
    this[RabbeinuTamMethodKey] = settings.rabbeinuTamMethod.storageValue
    this[ChametzMethodKey] = settings.chametzMethod.storageValue
    this[CandleLightingCustomMinutes] = settings.candleLightingCustomMinutes
    this[HolyDayTosefetMinutes] = settings.holyDayTosefetMinutes
    this[AteretTorahOffsetMinutes] = settings.ateretTorahSunsetOffsetMinutes
    // The numbers behind the custom options, one set per zman. All three units are stored,
    // not just the one selected, so switching units and back does not lose what was typed.
    putCustomValues(AlotZman, settings.alotHashacharCustom)
    putCustomValues(MisheyakirZman, settings.misheyakirCustom)
    putCustomValues(ShemaZman, settings.sofZmanShemaCustom)
    putCustomValues(TefillahZman, settings.sofZmanTefillahCustom)
    putCustomValues(MinchaGedolaZman, settings.minchaGedolaCustom)
    putCustomValues(MinchaKetanaZman, settings.minchaKetanaCustom)
    putCustomValues(PlagZman, settings.plagHaminchaCustom)
    putCustomValues(TzeitZman, settings.tzeitHakochavimCustom)
    putCustomValues(MotzeiZman, settings.motzeiShabbatCustom)
    putCustomValues(RabbeinuTamZman, settings.rabbeinuTamCustom)
    putCustomValues(ChametzZman, settings.chametzCustom)
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

internal fun MutablePreferences.putCustomValues(zman: String, values: CustomZmanValue) {
    this[customDegreesKey(zman)] = values.degrees
    this[customMinutesKey(zman)] = values.minutes
    this[customZmaniyotKey(zman)] = values.zmaniyotMinutes
}

/**
 * The stored numbers for [zman], with [legacy] — the ladder rung an older build had selected —
 * written over the matching one, so that rung becomes this build's custom value for it.
 */
internal fun Preferences.customValues(
    zman: String,
    defaults: CustomZmanValue,
    legacy: LegacyLadderChoice?,
): CustomZmanValue {
    // Any number is the user's to choose; only a stored degree that is not a number at all (never
    // written by the app) gives way to the default.
    val stored = CustomZmanValue(
        degrees = this[customDegreesKey(zman)]?.takeIf { it.isFinite() } ?: defaults.degrees,
        minutes = this[customMinutesKey(zman)] ?: defaults.minutes,
        zmaniyotMinutes = this[customZmaniyotKey(zman)] ?: defaults.zmaniyotMinutes,
    )
    return legacy?.let { stored.withValue(it.unit, it.value) } ?: stored
}

private val AlotHashacharMethodKey = stringPreferencesKey("zmanim_alot_method")
private val MisheyakirMethodKey = stringPreferencesKey("zmanim_misheyakir_method")
private val SunriseMethodKey = stringPreferencesKey("zmanim_sunrise_method")
private val SofZmanShemaGraMethodKey = stringPreferencesKey("zmanim_shema_gra_method")
private val SofZmanShemaMethodKey = stringPreferencesKey("zmanim_shema_method")
private val SofZmanTefillahGraMethodKey = stringPreferencesKey("zmanim_tefillah_gra_method")
private val SofZmanTefillahMethodKey = stringPreferencesKey("zmanim_tefillah_method")
private val ChatzotMethodKey = stringPreferencesKey("zmanim_chatzot_method")
private val ChatzotHaLailaMethodKey = stringPreferencesKey("zmanim_chatzot_halaila_method")
private val MinchaGedolaMethodKey = stringPreferencesKey("zmanim_mincha_gedola_method")
private val MinchaKetanaMethodKey = stringPreferencesKey("zmanim_mincha_ketana_method")
private val PlagHaminchaMethodKey = stringPreferencesKey("zmanim_plag_method")
private val SunsetMethodKey = stringPreferencesKey("zmanim_sunset_method")
private val TzeitHakochavimMethodKey = stringPreferencesKey("zmanim_tzeit_method")
private val NoTzeitFallbackKey = stringPreferencesKey("zmanim_no_tzeit_fallback")
private val CandleLightingMethodKey = stringPreferencesKey("zmanim_candle_method")
private val MotzeiShabbatMethodKey = stringPreferencesKey("zmanim_motzei_method")
private val RabbeinuTamMethodKey = stringPreferencesKey("zmanim_rabbeinu_tam_method")
private val ChametzMethodKey = stringPreferencesKey("zmanim_chametz_method")
private val HolyDayTosefetMinutes = intPreferencesKey("holy_day_tosefet_minutes")
private val AteretTorahOffsetMinutes = intPreferencesKey("zmanim_ateret_torah_offset_minutes")
private val AlotHashacharOffsetMinutes = intPreferencesKey("zmanim_alot_offset_minutes")
private val PlagHaminchaOffsetMinutes = intPreferencesKey("zmanim_plag_offset_minutes")
private val UseSeaLevelSunrise = booleanPreferencesKey("zmanim_use_sea_level_sunrise")
private val UseSeaLevelSunset = booleanPreferencesKey("zmanim_use_sea_level_sunset")
private val CandleLightingOffsetMinutes = intPreferencesKey("zmanim_candle_lighting_offset_minutes")
private val CandleLightingCustomMinutes = intPreferencesKey("zmanim_candle_custom_minutes")

// Key prefixes for the per-zman custom values. Short and stable: they are part of the
// on-disk format, so renaming one silently resets that zman's typed-in numbers.
private const val AlotZman = "alot"
private const val MisheyakirZman = "misheyakir"
private const val ShemaZman = "shema"
private const val TefillahZman = "tefillah"
private const val MinchaGedolaZman = "mincha_gedola"
private const val MinchaKetanaZman = "mincha_ketana"
private const val PlagZman = "plag"
private const val TzeitZman = "tzeit"
private const val MotzeiZman = "motzei"
private const val RabbeinuTamZman = "rabbeinu_tam"
private const val ChametzZman = "chametz"

// Storage values of the named options that were dropped because they were only a degree
// value. Kept here so an install that had one still computes the same times.
private const val BaalHatanyaValue = "baal_hatanya"
private const val BainHashmashot13Point24Value = "bain_hashmashot_13_24"
private const val AteretTorahValue = "ateret_torah"
