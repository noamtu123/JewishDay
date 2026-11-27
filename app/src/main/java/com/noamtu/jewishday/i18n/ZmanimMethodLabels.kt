// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.i18n

import com.noamtu.jewishday.R
import com.noamtu.jewishday.core.model.AlotHashacharMethod
import com.noamtu.jewishday.core.model.CandleLighting
import com.noamtu.jewishday.core.model.CandleLightingMethod
import com.noamtu.jewishday.core.model.ChametzMethod
import com.noamtu.jewishday.core.model.ChatzotMethod
import com.noamtu.jewishday.core.model.CustomZmanUnit
import com.noamtu.jewishday.core.model.CustomZmanValue
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
import com.noamtu.jewishday.core.model.ZmanimTimeOption
import com.noamtu.jewishday.core.model.customUnit

/**
 * How a custom option reads once its number is filled in — "16.4 degrees", "74 minutes", "74
 * zmaniyot". This is what the zmanim row's caption shows, so the row says which opinion produced the
 * time it is displaying, exactly as it does for a named one.
 */
fun customZmanLabel(unit: CustomZmanUnit, values: CustomZmanValue): UiText = when (unit) {
    CustomZmanUnit.Degrees -> degrees(formatDegrees(values.degrees))
    CustomZmanUnit.Minutes -> minutes(values.minutes)
    CustomZmanUnit.ZmaniyotMinutes -> pluralText(R.plurals.method_zmaniyot_minutes_value, values.zmaniyotMinutes)
}

private fun degrees(value: String): UiText = uiText(R.string.method_degrees_value, verbatim(value))

private fun minutes(value: Int): UiText = pluralText(R.plurals.method_minutes_value, value)

/** Three decimals at most, and no trailing zeros: 19.848°, 16.1°, 18°. */
private fun formatDegrees(value: Double): String {
    val rounded = Math.round(value * 1000.0) / 1000.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}

val AlotHashacharMethod.label: UiText get() = when (this) {
    AlotHashacharMethod.Degrees16Point1 -> degrees("16.1")
    AlotHashacharMethod.CustomDegrees -> uiText(R.string.method_degrees)
    AlotHashacharMethod.CustomMinutes -> uiText(R.string.method_minutes)
    AlotHashacharMethod.CustomZmaniyotMinutes -> uiText(R.string.method_zmaniyot_minutes)
}

val MisheyakirMethod.label: UiText get() = when (this) {
    MisheyakirMethod.Degrees11 -> degrees("11")
    MisheyakirMethod.CustomDegrees -> uiText(R.string.method_degrees)
    MisheyakirMethod.CustomMinutesBeforeSunrise -> uiText(R.string.method_minutes_before_sunrise)
    MisheyakirMethod.CustomZmaniyotMinutesBeforeSunrise -> uiText(R.string.method_zmaniyot_minutes_before_sunrise)
    MisheyakirMethod.Minutes6AfterAlos -> uiText(R.string.method_6_minutes_after_alot)
}

val SunriseMethod.label: UiText get() = when (this) {
    SunriseMethod.SeaLevel -> uiText(R.string.method_sunrise_sea_level)
    SunriseMethod.ElevationAdjusted -> uiText(R.string.method_sunrise_observed)
}

val SofZmanShemaMethod.label: UiText get() = when (this) {
    SofZmanShemaMethod.Gra -> uiText(R.string.method_gra)
    SofZmanShemaMethod.FixedLocalGra -> uiText(R.string.method_rav_moshe_fixed_local)
    SofZmanShemaMethod.Mga16Point1 -> degrees("16.1")
    SofZmanShemaMethod.CustomDegrees -> uiText(R.string.method_degrees)
    SofZmanShemaMethod.CustomMinutes -> uiText(R.string.method_minutes)
    SofZmanShemaMethod.CustomZmaniyotMinutes -> uiText(R.string.method_zmaniyot_minutes)
    SofZmanShemaMethod.CustomDegreesToFixedLocalChatzot -> uiText(R.string.method_degrees_to_fixed_local_chatzot)
    SofZmanShemaMethod.CustomMinutesToFixedLocalChatzot -> uiText(R.string.method_minutes_to_fixed_local_chatzot)
    SofZmanShemaMethod.Alos16Point1ToSunset -> uiText(R.string.method_shema_alot_to_sunset)
    SofZmanShemaMethod.Alos16Point1ToTzeit7Point083 -> uiText(R.string.method_shema_alot_to_tzeit)
    SofZmanShemaMethod.AteretTorah -> uiText(R.string.method_ateret_torah)
}

val SofZmanTefillahMethod.label: UiText get() = when (this) {
    SofZmanTefillahMethod.Gra -> uiText(R.string.method_gra)
    SofZmanTefillahMethod.FixedLocalGra -> uiText(R.string.method_rav_moshe_fixed_local)
    SofZmanTefillahMethod.Mga16Point1 -> degrees("16.1")
    SofZmanTefillahMethod.CustomDegrees -> uiText(R.string.method_degrees)
    SofZmanTefillahMethod.CustomMinutes -> uiText(R.string.method_minutes)
    SofZmanTefillahMethod.CustomZmaniyotMinutes -> uiText(R.string.method_zmaniyot_minutes)
    SofZmanTefillahMethod.Alos16Point1ToSunset -> uiText(R.string.method_alot_to_sunset)
    SofZmanTefillahMethod.Alos16Point1ToTzeit7Point083 -> uiText(R.string.method_alot_to_tzeit)
    SofZmanTefillahMethod.AteretTorah -> uiText(R.string.method_ateret_torah)
}

val ChatzotMethod.label: UiText get() = when (this) {
    ChatzotMethod.Solar -> uiText(R.string.method_chatzot_solar)
    ChatzotMethod.FixedLocal -> uiText(R.string.method_chatzot_fixed_local)
}

val MinchaGedolaMethod.label: UiText get() = when (this) {
    MinchaGedolaMethod.Standard -> uiText(R.string.method_gra)
    MinchaGedolaMethod.CustomDegrees -> uiText(R.string.method_degrees)
    MinchaGedolaMethod.CustomMinutes -> uiText(R.string.method_minutes)
    MinchaGedolaMethod.CustomZmaniyotMinutes -> uiText(R.string.method_zmaniyot_minutes)
    MinchaGedolaMethod.ThirtyMinutes -> uiText(R.string.method_30_minutes_after_chatzot)
    MinchaGedolaMethod.GreaterThan30 -> uiText(R.string.method_later_of_gra_and_30)
    MinchaGedolaMethod.FixedLocal -> uiText(R.string.method_rav_moshe_30_after_fixed_local)
    MinchaGedolaMethod.BaalHatanya -> uiText(R.string.method_baal_hatanya)
    MinchaGedolaMethod.BaalHatanyaGreaterThan30 -> uiText(R.string.method_later_of_baal_hatanya_and_30)
    MinchaGedolaMethod.AteretTorah -> uiText(R.string.method_ateret_torah)
    MinchaGedolaMethod.AhavatShalom -> uiText(R.string.method_ahavat_shalom)
}

val MinchaKetanaMethod.label: UiText get() = when (this) {
    MinchaKetanaMethod.Standard -> uiText(R.string.method_gra)
    MinchaKetanaMethod.CustomDegrees -> uiText(R.string.method_degrees)
    MinchaKetanaMethod.CustomMinutes -> uiText(R.string.method_minutes)
    MinchaKetanaMethod.CustomZmaniyotMinutes -> uiText(R.string.method_zmaniyot_minutes)
    MinchaKetanaMethod.FixedLocal -> uiText(R.string.method_rav_moshe_fixed_local)
    MinchaKetanaMethod.BaalHatanya -> uiText(R.string.method_baal_hatanya)
    MinchaKetanaMethod.AteretTorah -> uiText(R.string.method_ateret_torah)
    MinchaKetanaMethod.AhavatShalom -> uiText(R.string.method_ahavat_shalom)
}

val PlagHaminchaMethod.label: UiText get() = when (this) {
    PlagHaminchaMethod.Gra -> uiText(R.string.method_gra)
    PlagHaminchaMethod.CustomDegrees -> uiText(R.string.method_degrees)
    PlagHaminchaMethod.CustomMinutes -> uiText(R.string.method_minutes)
    PlagHaminchaMethod.CustomZmaniyotMinutes -> uiText(R.string.method_zmaniyot_minutes)
    PlagHaminchaMethod.Alot16Point1ToTzeit7Point083 -> uiText(R.string.method_alot_to_tzeit)
    PlagHaminchaMethod.FixedLocal -> uiText(R.string.method_rav_moshe_fixed_local)
    PlagHaminchaMethod.BaalHatanya -> uiText(R.string.method_baal_hatanya)
    PlagHaminchaMethod.AteretTorah -> uiText(R.string.method_ateret_torah)
    PlagHaminchaMethod.AhavatShalom -> uiText(R.string.method_ahavat_shalom)
}

val SunsetMethod.label: UiText get() = when (this) {
    SunsetMethod.SeaLevel -> uiText(R.string.method_sunset_sea_level)
    SunsetMethod.ElevationAdjusted -> uiText(R.string.method_sunset_observed)
}

val TzeitHakochavimMethod.label: UiText get() = when (this) {
    TzeitHakochavimMethod.Degrees6Point2 -> uiText(R.string.method_tzeit_6_2)
    TzeitHakochavimMethod.CustomDegrees -> uiText(R.string.method_degrees)
    TzeitHakochavimMethod.CustomMinutes -> uiText(R.string.method_minutes)
    TzeitHakochavimMethod.CustomZmaniyotMinutes -> uiText(R.string.method_zmaniyot_minutes)
}

val NoTzeitFallback.label: UiText get() = when (this) {
    NoTzeitFallback.Minutes72AfterSunset -> uiText(R.string.method_no_tzeit_72)
    NoTzeitFallback.ChatzotHaLaila -> uiText(R.string.method_no_tzeit_chatzot)
}

/** What a nightfall row says under its time when this stand-in gave it, because the night had no tzeit. */
val NoTzeitFallback.nightfallCaption: UiText get() = when (this) {
    NoTzeitFallback.Minutes72AfterSunset -> uiText(R.string.method_no_tzeit_caption_72)
    NoTzeitFallback.ChatzotHaLaila -> uiText(R.string.method_no_tzeit_caption_chatzot)
}

/**
 * What [option]'s row says under its time when this stand-in gave it: the night had no tzeit, or the
 * morning no dawn, misheyakir or Magen Avraham day, by the user's own method.
 */
fun NoTzeitFallback.caption(option: ZmanimTimeOption): UiText = when (option) {
    ZmanimTimeOption.AlotHashachar -> when (this) {
        NoTzeitFallback.Minutes72AfterSunset -> uiText(R.string.method_no_dawn_caption_72)
        NoTzeitFallback.ChatzotHaLaila -> uiText(R.string.method_no_dawn_caption_chatzot)
    }
    ZmanimTimeOption.TallitTefillin -> when (this) {
        NoTzeitFallback.Minutes72AfterSunset -> uiText(R.string.method_no_dawn_caption_misheyakir_50)
        NoTzeitFallback.ChatzotHaLaila -> uiText(R.string.method_no_dawn_caption_chatzot)
    }
    ZmanimTimeOption.SofZmanShemaMagenAvraham, ZmanimTimeOption.SofZmanTefillahMagenAvraham -> when (this) {
        NoTzeitFallback.Minutes72AfterSunset -> uiText(R.string.method_no_dawn_caption_mga_72)
        NoTzeitFallback.ChatzotHaLaila -> uiText(R.string.method_no_dawn_caption_mga_chatzot)
    }
    else -> nightfallCaption
}

val CandleLightingMethod.label: UiText get() = when (this) {
    CandleLightingMethod.AtPlag -> uiText(R.string.method_candles_option_plag)
    else -> offsetMinutes?.let(::minutes) ?: uiText(R.string.method_minutes)
}

val MotzeiShabbatMethod.label: UiText get() = when (this) {
    MotzeiShabbatMethod.Degrees6Point2 -> uiText(R.string.method_tzeit_6_2)
    MotzeiShabbatMethod.CustomDegrees -> uiText(R.string.method_degrees)
    MotzeiShabbatMethod.CustomMinutes -> uiText(R.string.method_minutes)
    MotzeiShabbatMethod.CustomZmaniyotMinutes -> uiText(R.string.method_zmaniyot_minutes)
}

val RabbeinuTamMethod.label: UiText get() = when (this) {
    RabbeinuTamMethod.Minutes72 -> minutes(72)
    RabbeinuTamMethod.CustomDegrees -> uiText(R.string.method_degrees)
    RabbeinuTamMethod.CustomMinutes -> uiText(R.string.method_minutes)
    RabbeinuTamMethod.CustomZmaniyotMinutes -> uiText(R.string.method_zmaniyot_minutes)
    RabbeinuTamMethod.BainHashmashot58Point5 -> uiText(R.string.method_bein_hashmashot_58_5)
    RabbeinuTamMethod.BainHashmashot13Point5Before7Point083 -> uiText(R.string.method_bein_hashmashot_13_5)
    RabbeinuTamMethod.BainHashmashot2Stars -> uiText(R.string.method_bein_hashmashot_2_stars)
}

val ChametzMethod.label: UiText get() = when (this) {
    ChametzMethod.Gra -> uiText(R.string.method_gra)
    ChametzMethod.CustomDegrees -> uiText(R.string.method_degrees)
    ChametzMethod.CustomMinutes -> uiText(R.string.method_minutes)
    ChametzMethod.CustomZmaniyotMinutes -> uiText(R.string.method_zmaniyot_minutes)
    ChametzMethod.BaalHatanya -> uiText(R.string.method_baal_hatanya)
}

// ---------------------------------------------------------------------------------------------
// Captions
//
// What a zmanim row says underneath the time, and what the settings row shows as its value. A named
// option reads as its name; a custom one reads as the number that was typed in, since "Custom
// degrees" alone would not tell anyone which opinion produced the time above it.
// ---------------------------------------------------------------------------------------------

private fun captionOf(unit: CustomZmanUnit?, values: CustomZmanValue, fixed: UiText): UiText =
    if (unit == null) fixed else customZmanLabel(unit, values)

fun AlotHashacharMethod.caption(settings: ZmanimCalculationSettings): UiText =
    captionOf(customUnit, settings.alotHashacharCustom, label)

fun MisheyakirMethod.caption(settings: ZmanimCalculationSettings): UiText {
    val unit = customUnit ?: return label
    val value = customZmanLabel(unit, settings.misheyakirCustom)
    // Every option here but the degree one is an offset from something, and which something is the
    // whole point, so the caption says it: "45 דקות לפני הנץ", not "45 דקות".
    return when (this) {
        MisheyakirMethod.CustomMinutesBeforeSunrise,
        MisheyakirMethod.CustomZmaniyotMinutesBeforeSunrise,
        -> uiText(R.string.method_before_sunrise, value)
        else -> value
    }
}

fun SofZmanShemaMethod.caption(settings: ZmanimCalculationSettings): UiText {
    val unit = customUnit ?: return label
    val value = customZmanLabel(unit, settings.sofZmanShemaCustom)
    return when (this) {
        SofZmanShemaMethod.CustomDegreesToFixedLocalChatzot,
        SofZmanShemaMethod.CustomMinutesToFixedLocalChatzot,
        -> uiText(R.string.method_to_fixed_local_chatzot, value)
        else -> value
    }
}

fun SofZmanTefillahMethod.caption(settings: ZmanimCalculationSettings): UiText =
    captionOf(customUnit, settings.sofZmanTefillahCustom, label)

fun MinchaGedolaMethod.caption(settings: ZmanimCalculationSettings): UiText =
    captionOf(customUnit, settings.minchaGedolaCustom, label)

fun MinchaKetanaMethod.caption(settings: ZmanimCalculationSettings): UiText =
    captionOf(customUnit, settings.minchaKetanaCustom, label)

fun PlagHaminchaMethod.caption(settings: ZmanimCalculationSettings): UiText =
    captionOf(customUnit, settings.plagHaminchaCustom, label)

fun TzeitHakochavimMethod.caption(settings: ZmanimCalculationSettings): UiText =
    captionOf(customUnit, settings.tzeitHakochavimCustom, label)

fun MotzeiShabbatMethod.caption(settings: ZmanimCalculationSettings): UiText =
    captionOf(customUnit, settings.motzeiShabbatCustom, label)

fun RabbeinuTamMethod.caption(settings: ZmanimCalculationSettings): UiText =
    captionOf(customUnit, settings.rabbeinuTamCustom, label)

fun ChametzMethod.caption(settings: ZmanimCalculationSettings): UiText =
    captionOf(customUnit, settings.chametzCustom, label)

fun CandleLightingMethod.caption(settings: ZmanimCalculationSettings): UiText = when (this) {
    CandleLightingMethod.AtPlag -> uiText(R.string.method_candles_at_plag)
    else -> minutes(offsetMinutes ?: settings.candleLightingCustomMinutes)
}

/** What a candle-lighting row says under its time: the method, or that it was held at plag. */
fun CandleLighting?.caption(settings: ZmanimCalculationSettings): UiText = when {
    this?.fallback == NoTzeitFallback.ChatzotHaLaila -> uiText(R.string.method_no_sunset_caption_chatzot)
    this?.isPlag == true -> uiText(R.string.method_candles_at_plag)
    else -> settings.candleLightingMethod.caption(settings)
}
