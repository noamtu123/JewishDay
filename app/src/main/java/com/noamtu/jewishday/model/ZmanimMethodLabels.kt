// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

/**
 * How a custom option reads once its number is filled in — "16.4 degrees", "74 minutes", "74
 * zmaniyot". This is what the zmanim row's caption shows, so the row says which opinion produced the
 * time it is displaying, exactly as it does for a named one.
 */
fun customZmanLabel(unit: CustomZmanUnit, values: CustomZmanValue, hebrew: Boolean): String = when (unit) {
    CustomZmanUnit.Degrees ->
        if (hebrew) "${formatDegrees(values.degrees)} מעלות" else "${formatDegrees(values.degrees)} degrees"
    CustomZmanUnit.Minutes ->
        if (hebrew) "${values.minutes} דקות" else "${values.minutes} minutes"
    CustomZmanUnit.ZmaniyotMinutes ->
        if (hebrew) "${values.zmaniyotMinutes} דקות זמניות" else "${values.zmaniyotMinutes} zmaniyot"
}

/** Three decimals at most, and no trailing zeros: 19.848°, 16.1°, 18°. */
private fun formatDegrees(value: Double): String {
    val rounded = Math.round(value * 1000.0) / 1000.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}

val AlotHashacharMethod.label: String get() = when (this) {
    AlotHashacharMethod.Degrees16Point1 -> "16.1 degrees"
    AlotHashacharMethod.CustomDegrees -> "Degrees"
    AlotHashacharMethod.CustomMinutes -> "Minutes"
    AlotHashacharMethod.CustomZmaniyotMinutes -> "Zmaniyot minutes"
}

val AlotHashacharMethod.labelHebrew: String get() = when (this) {
    AlotHashacharMethod.Degrees16Point1 -> "16.1 מעלות"
    AlotHashacharMethod.CustomDegrees -> "מעלות"
    AlotHashacharMethod.CustomMinutes -> "דקות"
    AlotHashacharMethod.CustomZmaniyotMinutes -> "דקות זמניות"
}

/** Which unit this option is entered in, or null when it is a fixed or named opinion. */
val AlotHashacharMethod.customUnit: CustomZmanUnit? get() = when (this) {
    AlotHashacharMethod.CustomDegrees -> CustomZmanUnit.Degrees
    AlotHashacharMethod.CustomMinutes -> CustomZmanUnit.Minutes
    AlotHashacharMethod.CustomZmaniyotMinutes -> CustomZmanUnit.ZmaniyotMinutes
    AlotHashacharMethod.Degrees16Point1 -> null
}

val MisheyakirMethod.label: String get() = when (this) {
    MisheyakirMethod.Degrees11 -> "11 degrees"
    MisheyakirMethod.CustomDegrees -> "Degrees"
    MisheyakirMethod.CustomMinutesBeforeSunrise -> "Minutes before sunrise"
    MisheyakirMethod.CustomZmaniyotMinutesBeforeSunrise -> "Zmaniyot minutes before sunrise"
    MisheyakirMethod.Minutes6AfterAlos -> "6 min after alot hashachar"
}

val MisheyakirMethod.labelHebrew: String get() = when (this) {
    MisheyakirMethod.Degrees11 -> "11 מעלות"
    MisheyakirMethod.CustomDegrees -> "מעלות"
    MisheyakirMethod.CustomMinutesBeforeSunrise -> "דקות לפני הנץ"
    MisheyakirMethod.CustomZmaniyotMinutesBeforeSunrise -> "דקות זמניות לפני הנץ"
    MisheyakirMethod.Minutes6AfterAlos -> "6 דקות אחרי עלות השחר"
}

val MisheyakirMethod.customUnit: CustomZmanUnit? get() = when (this) {
    MisheyakirMethod.CustomDegrees -> CustomZmanUnit.Degrees
    MisheyakirMethod.CustomMinutesBeforeSunrise -> CustomZmanUnit.Minutes
    MisheyakirMethod.CustomZmaniyotMinutesBeforeSunrise -> CustomZmanUnit.ZmaniyotMinutes
    MisheyakirMethod.Degrees11,
    MisheyakirMethod.Minutes6AfterAlos,
    -> null
}

val SunriseMethod.label: String get() = when (this) {
    SunriseMethod.SeaLevel -> "Sea-level sunrise"
    SunriseMethod.ElevationAdjusted -> "Observed sunrise"
}

val SunriseMethod.labelHebrew: String get() = when (this) {
    SunriseMethod.SeaLevel -> "זריחה במישור"
    SunriseMethod.ElevationAdjusted -> "זריחה נראית"
}

val SofZmanShemaMethod.label: String get() = when (this) {
    SofZmanShemaMethod.Gra -> "GRA"
    SofZmanShemaMethod.FixedLocalGra -> "Rav Moshe Feinstein (fixed local Chatzot)"
    SofZmanShemaMethod.Mga16Point1 -> "16.1 degrees"
    SofZmanShemaMethod.CustomDegrees -> "Degrees"
    SofZmanShemaMethod.CustomMinutes -> "Minutes"
    SofZmanShemaMethod.CustomZmaniyotMinutes -> "Zmaniyot minutes"
    SofZmanShemaMethod.CustomDegreesToFixedLocalChatzot -> "Degrees to fixed local Chatzot"
    SofZmanShemaMethod.CustomMinutesToFixedLocalChatzot -> "Minutes to fixed local Chatzot"
    SofZmanShemaMethod.Alos16Point1ToSunset -> "Alot 16.1° to sunset"
    SofZmanShemaMethod.Alos16Point1ToTzeit7Point083 -> "Alot 16.1° to Tzeit 7.083°"
    SofZmanShemaMethod.AteretTorah -> "Ateret Torah"
}

val SofZmanShemaMethod.labelHebrew: String get() = when (this) {
    SofZmanShemaMethod.Gra -> "גר״א"
    SofZmanShemaMethod.FixedLocalGra -> "הרב משה פיינשטיין (חצות מקומי קבוע)"
    SofZmanShemaMethod.Mga16Point1 -> "16.1 מעלות"
    SofZmanShemaMethod.CustomDegrees -> "מעלות"
    SofZmanShemaMethod.CustomMinutes -> "דקות"
    SofZmanShemaMethod.CustomZmaniyotMinutes -> "דקות זמניות"
    SofZmanShemaMethod.CustomDegreesToFixedLocalChatzot -> "מעלות עד חצות מקומי קבוע"
    SofZmanShemaMethod.CustomMinutesToFixedLocalChatzot -> "דקות עד חצות מקומי קבוע"
    SofZmanShemaMethod.Alos16Point1ToSunset -> "עלות 16.1 מעלות עד שקיעה"
    SofZmanShemaMethod.Alos16Point1ToTzeit7Point083 -> "עלות 16.1 מעלות עד צאת 7.083 מעלות"
    SofZmanShemaMethod.AteretTorah -> "עטרת תורה"
}

val SofZmanShemaMethod.customUnit: CustomZmanUnit? get() = when (this) {
    SofZmanShemaMethod.CustomDegrees,
    SofZmanShemaMethod.CustomDegreesToFixedLocalChatzot,
    -> CustomZmanUnit.Degrees
    SofZmanShemaMethod.CustomMinutes,
    SofZmanShemaMethod.CustomMinutesToFixedLocalChatzot,
    -> CustomZmanUnit.Minutes
    SofZmanShemaMethod.CustomZmaniyotMinutes -> CustomZmanUnit.ZmaniyotMinutes
    SofZmanShemaMethod.Gra,
    SofZmanShemaMethod.FixedLocalGra,
    SofZmanShemaMethod.Mga16Point1,
    SofZmanShemaMethod.Alos16Point1ToSunset,
    SofZmanShemaMethod.Alos16Point1ToTzeit7Point083,
    SofZmanShemaMethod.AteretTorah,
    -> null
}

val SofZmanTefillahMethod.label: String get() = when (this) {
    SofZmanTefillahMethod.Gra -> "GRA"
    SofZmanTefillahMethod.FixedLocalGra -> "Rav Moshe Feinstein (fixed local Chatzot)"
    SofZmanTefillahMethod.Mga16Point1 -> "16.1 degrees"
    SofZmanTefillahMethod.CustomDegrees -> "Degrees"
    SofZmanTefillahMethod.CustomMinutes -> "Minutes"
    SofZmanTefillahMethod.CustomZmaniyotMinutes -> "Zmaniyot minutes"
    SofZmanTefillahMethod.Alos16Point1ToSunset -> "Alot 16.1° to sunset"
    SofZmanTefillahMethod.Alos16Point1ToTzeit7Point083 -> "Alot 16.1° to Tzeit 7.083°"
    SofZmanTefillahMethod.AteretTorah -> "Ateret Torah"
}

val SofZmanTefillahMethod.labelHebrew: String get() = when (this) {
    SofZmanTefillahMethod.Gra -> "גר״א"
    SofZmanTefillahMethod.FixedLocalGra -> "הרב משה פיינשטיין (חצות מקומי קבוע)"
    SofZmanTefillahMethod.Mga16Point1 -> "16.1 מעלות"
    SofZmanTefillahMethod.CustomDegrees -> "מעלות"
    SofZmanTefillahMethod.CustomMinutes -> "דקות"
    SofZmanTefillahMethod.CustomZmaniyotMinutes -> "דקות זמניות"
    SofZmanTefillahMethod.Alos16Point1ToSunset -> "עלות 16.1° עד שקיעה"
    SofZmanTefillahMethod.Alos16Point1ToTzeit7Point083 -> "עלות 16.1° עד צאת 7.083°"
    SofZmanTefillahMethod.AteretTorah -> "עטרת תורה"
}

val SofZmanTefillahMethod.customUnit: CustomZmanUnit? get() = when (this) {
    SofZmanTefillahMethod.CustomDegrees -> CustomZmanUnit.Degrees
    SofZmanTefillahMethod.CustomMinutes -> CustomZmanUnit.Minutes
    SofZmanTefillahMethod.CustomZmaniyotMinutes -> CustomZmanUnit.ZmaniyotMinutes
    SofZmanTefillahMethod.Gra,
    SofZmanTefillahMethod.FixedLocalGra,
    SofZmanTefillahMethod.Mga16Point1,
    SofZmanTefillahMethod.Alos16Point1ToSunset,
    SofZmanTefillahMethod.Alos16Point1ToTzeit7Point083,
    SofZmanTefillahMethod.AteretTorah,
    -> null
}

val ChatzotMethod.label: String get() = when (this) {
    ChatzotMethod.Solar -> "Solar Chatzot"
    ChatzotMethod.FixedLocal -> "Fixed-local Chatzot"
}

val ChatzotMethod.labelHebrew: String get() = when (this) {
    ChatzotMethod.Solar -> "חצות שמשי"
    ChatzotMethod.FixedLocal -> "חצות מקומי קבוע"
}

val MinchaGedolaMethod.label: String get() = when (this) {
    MinchaGedolaMethod.Standard -> "GRA"
    MinchaGedolaMethod.CustomDegrees -> "Degrees"
    MinchaGedolaMethod.CustomMinutes -> "Minutes"
    MinchaGedolaMethod.CustomZmaniyotMinutes -> "Zmaniyot minutes"
    MinchaGedolaMethod.ThirtyMinutes -> "30 minutes after Chatzot"
    MinchaGedolaMethod.GreaterThan30 -> "Later of GRA and 30 min after Chatzot"
    MinchaGedolaMethod.FixedLocal -> "Rav Moshe Feinstein (30 min after fixed local Chatzot)"
    MinchaGedolaMethod.BaalHatanya -> "Baal Hatanya"
    MinchaGedolaMethod.BaalHatanyaGreaterThan30 -> "Later of Baal Hatanya and 30 min after Chatzot"
    MinchaGedolaMethod.AteretTorah -> "Ateret Torah"
    MinchaGedolaMethod.AhavatShalom -> "Ahavat Shalom"
}

val MinchaGedolaMethod.labelHebrew: String get() = when (this) {
    MinchaGedolaMethod.Standard -> "גר״א"
    MinchaGedolaMethod.CustomDegrees -> "מעלות"
    MinchaGedolaMethod.CustomMinutes -> "דקות"
    MinchaGedolaMethod.CustomZmaniyotMinutes -> "דקות זמניות"
    MinchaGedolaMethod.ThirtyMinutes -> "30 דקות אחרי חצות"
    MinchaGedolaMethod.GreaterThan30 -> "המאוחר מבין גר״א ו־30 דקות אחרי חצות"
    MinchaGedolaMethod.FixedLocal -> "הרב משה פיינשטיין (30 דקות אחרי חצות מקומי קבוע)"
    MinchaGedolaMethod.BaalHatanya -> "בעל התניא"
    MinchaGedolaMethod.BaalHatanyaGreaterThan30 -> "המאוחר מבין בעל התניא ו־30 דקות אחרי חצות"
    MinchaGedolaMethod.AteretTorah -> "עטרת תורה"
    MinchaGedolaMethod.AhavatShalom -> "אהבת שלום"
}

val MinchaGedolaMethod.customUnit: CustomZmanUnit? get() = when (this) {
    MinchaGedolaMethod.CustomDegrees -> CustomZmanUnit.Degrees
    MinchaGedolaMethod.CustomMinutes -> CustomZmanUnit.Minutes
    MinchaGedolaMethod.CustomZmaniyotMinutes -> CustomZmanUnit.ZmaniyotMinutes
    MinchaGedolaMethod.Standard,
    MinchaGedolaMethod.ThirtyMinutes,
    MinchaGedolaMethod.GreaterThan30,
    MinchaGedolaMethod.FixedLocal,
    MinchaGedolaMethod.BaalHatanya,
    MinchaGedolaMethod.BaalHatanyaGreaterThan30,
    MinchaGedolaMethod.AteretTorah,
    MinchaGedolaMethod.AhavatShalom,
    -> null
}

val MinchaKetanaMethod.label: String get() = when (this) {
    MinchaKetanaMethod.Standard -> "GRA"
    MinchaKetanaMethod.CustomDegrees -> "Degrees"
    MinchaKetanaMethod.CustomMinutes -> "Minutes"
    MinchaKetanaMethod.CustomZmaniyotMinutes -> "Zmaniyot minutes"
    MinchaKetanaMethod.FixedLocal -> "Rav Moshe Feinstein (fixed local Chatzot)"
    MinchaKetanaMethod.BaalHatanya -> "Baal Hatanya"
    MinchaKetanaMethod.AteretTorah -> "Ateret Torah"
    MinchaKetanaMethod.AhavatShalom -> "Ahavat Shalom"
}

val MinchaKetanaMethod.labelHebrew: String get() = when (this) {
    MinchaKetanaMethod.Standard -> "גר״א"
    MinchaKetanaMethod.CustomDegrees -> "מעלות"
    MinchaKetanaMethod.CustomMinutes -> "דקות"
    MinchaKetanaMethod.CustomZmaniyotMinutes -> "דקות זמניות"
    MinchaKetanaMethod.FixedLocal -> "הרב משה פיינשטיין (חצות מקומי קבוע)"
    MinchaKetanaMethod.BaalHatanya -> "בעל התניא"
    MinchaKetanaMethod.AteretTorah -> "עטרת תורה"
    MinchaKetanaMethod.AhavatShalom -> "אהבת שלום"
}

val MinchaKetanaMethod.customUnit: CustomZmanUnit? get() = when (this) {
    MinchaKetanaMethod.CustomDegrees -> CustomZmanUnit.Degrees
    MinchaKetanaMethod.CustomMinutes -> CustomZmanUnit.Minutes
    MinchaKetanaMethod.CustomZmaniyotMinutes -> CustomZmanUnit.ZmaniyotMinutes
    MinchaKetanaMethod.Standard,
    MinchaKetanaMethod.FixedLocal,
    MinchaKetanaMethod.BaalHatanya,
    MinchaKetanaMethod.AteretTorah,
    MinchaKetanaMethod.AhavatShalom,
    -> null
}

val PlagHaminchaMethod.label: String get() = when (this) {
    PlagHaminchaMethod.Gra -> "GRA"
    PlagHaminchaMethod.CustomDegrees -> "Degrees"
    PlagHaminchaMethod.CustomMinutes -> "Minutes"
    PlagHaminchaMethod.CustomZmaniyotMinutes -> "Zmaniyot minutes"
    PlagHaminchaMethod.Alot16Point1ToTzeit7Point083 -> "Alot 16.1° to Tzeit 7.083°"
    PlagHaminchaMethod.FixedLocal -> "Rav Moshe Feinstein (fixed local Chatzot)"
    PlagHaminchaMethod.BaalHatanya -> "Baal Hatanya"
    PlagHaminchaMethod.AteretTorah -> "Ateret Torah"
    PlagHaminchaMethod.AhavatShalom -> "Ahavat Shalom"
}

val PlagHaminchaMethod.labelHebrew: String get() = when (this) {
    PlagHaminchaMethod.Gra -> "גר״א"
    PlagHaminchaMethod.CustomDegrees -> "מעלות"
    PlagHaminchaMethod.CustomMinutes -> "דקות"
    PlagHaminchaMethod.CustomZmaniyotMinutes -> "דקות זמניות"
    PlagHaminchaMethod.Alot16Point1ToTzeit7Point083 -> "עלות 16.1° עד צאת 7.083°"
    PlagHaminchaMethod.FixedLocal -> "הרב משה פיינשטיין (חצות מקומי קבוע)"
    PlagHaminchaMethod.BaalHatanya -> "בעל התניא"
    PlagHaminchaMethod.AteretTorah -> "עטרת תורה"
    PlagHaminchaMethod.AhavatShalom -> "אהבת שלום"
}

val PlagHaminchaMethod.customUnit: CustomZmanUnit? get() = when (this) {
    PlagHaminchaMethod.CustomDegrees -> CustomZmanUnit.Degrees
    PlagHaminchaMethod.CustomMinutes -> CustomZmanUnit.Minutes
    PlagHaminchaMethod.CustomZmaniyotMinutes -> CustomZmanUnit.ZmaniyotMinutes
    PlagHaminchaMethod.Gra,
    PlagHaminchaMethod.Alot16Point1ToTzeit7Point083,
    PlagHaminchaMethod.FixedLocal,
    PlagHaminchaMethod.BaalHatanya,
    PlagHaminchaMethod.AteretTorah,
    PlagHaminchaMethod.AhavatShalom,
    -> null
}

val SunsetMethod.label: String get() = when (this) {
    SunsetMethod.SeaLevel -> "Sea-level sunset"
    SunsetMethod.ElevationAdjusted -> "Observed sunset"
}

val SunsetMethod.labelHebrew: String get() = when (this) {
    SunsetMethod.SeaLevel -> "שקיעה במישור"
    SunsetMethod.ElevationAdjusted -> "שקיעה נראית"
}

val TzeitHakochavimMethod.label: String get() = when (this) {
    TzeitHakochavimMethod.Degrees6Point2 -> "6.2°"
    TzeitHakochavimMethod.CustomDegrees -> "Degrees"
    TzeitHakochavimMethod.CustomMinutes -> "Minutes"
    TzeitHakochavimMethod.CustomZmaniyotMinutes -> "Zmaniyot minutes"
}

val TzeitHakochavimMethod.labelHebrew: String get() = when (this) {
    TzeitHakochavimMethod.Degrees6Point2 -> "צאת 6.2°"
    TzeitHakochavimMethod.CustomDegrees -> "מעלות"
    TzeitHakochavimMethod.CustomMinutes -> "דקות"
    TzeitHakochavimMethod.CustomZmaniyotMinutes -> "דקות זמניות"
}

val TzeitHakochavimMethod.customUnit: CustomZmanUnit? get() = when (this) {
    TzeitHakochavimMethod.CustomDegrees -> CustomZmanUnit.Degrees
    TzeitHakochavimMethod.CustomMinutes -> CustomZmanUnit.Minutes
    TzeitHakochavimMethod.CustomZmaniyotMinutes -> CustomZmanUnit.ZmaniyotMinutes
    TzeitHakochavimMethod.Degrees6Point2 -> null
}

val CandleLightingMethod.label: String get() = offsetMinutes?.let { "$it minutes" } ?: "Minutes"
val CandleLightingMethod.labelHebrew: String get() = offsetMinutes?.let { "$it דקות" } ?: "דקות"

val MotzeiShabbatMethod.label: String get() = when (this) {
    MotzeiShabbatMethod.Degrees6Point2 -> "6.2°"
    MotzeiShabbatMethod.CustomDegrees -> "Degrees"
    MotzeiShabbatMethod.CustomMinutes -> "Minutes"
    MotzeiShabbatMethod.CustomZmaniyotMinutes -> "Zmaniyot minutes"
}

val MotzeiShabbatMethod.labelHebrew: String get() = when (this) {
    MotzeiShabbatMethod.Degrees6Point2 -> "צאת 6.2°"
    MotzeiShabbatMethod.CustomDegrees -> "מעלות"
    MotzeiShabbatMethod.CustomMinutes -> "דקות"
    MotzeiShabbatMethod.CustomZmaniyotMinutes -> "דקות זמניות"
}

val MotzeiShabbatMethod.customUnit: CustomZmanUnit? get() = when (this) {
    MotzeiShabbatMethod.CustomDegrees -> CustomZmanUnit.Degrees
    MotzeiShabbatMethod.CustomMinutes -> CustomZmanUnit.Minutes
    MotzeiShabbatMethod.CustomZmaniyotMinutes -> CustomZmanUnit.ZmaniyotMinutes
    MotzeiShabbatMethod.Degrees6Point2 -> null
}

val RabbeinuTamMethod.label: String get() = when (this) {
    RabbeinuTamMethod.Minutes72 -> "72 minutes"
    RabbeinuTamMethod.CustomDegrees -> "Degrees"
    RabbeinuTamMethod.CustomMinutes -> "Minutes"
    RabbeinuTamMethod.CustomZmaniyotMinutes -> "Zmaniyot minutes"
    RabbeinuTamMethod.BainHashmashot58Point5 -> "Bein Hashmashot 58.5 min"
    RabbeinuTamMethod.BainHashmashot13Point5Before7Point083 -> "Bein Hashmashot 13.5 min before 7.083°"
    RabbeinuTamMethod.BainHashmashot2Stars -> "Bein Hashmashot 2 stars"
}

val RabbeinuTamMethod.labelHebrew: String get() = when (this) {
    RabbeinuTamMethod.Minutes72 -> "72 דקות"
    RabbeinuTamMethod.CustomDegrees -> "מעלות"
    RabbeinuTamMethod.CustomMinutes -> "דקות"
    RabbeinuTamMethod.CustomZmaniyotMinutes -> "דקות זמניות"
    RabbeinuTamMethod.BainHashmashot58Point5 -> "בין השמשות 58.5 דקות"
    RabbeinuTamMethod.BainHashmashot13Point5Before7Point083 -> "בין השמשות 13.5 דקות לפני 7.083°"
    RabbeinuTamMethod.BainHashmashot2Stars -> "בין השמשות שני כוכבים"
}

val RabbeinuTamMethod.customUnit: CustomZmanUnit? get() = when (this) {
    RabbeinuTamMethod.CustomDegrees -> CustomZmanUnit.Degrees
    RabbeinuTamMethod.CustomMinutes -> CustomZmanUnit.Minutes
    RabbeinuTamMethod.CustomZmaniyotMinutes -> CustomZmanUnit.ZmaniyotMinutes
    RabbeinuTamMethod.Minutes72,
    RabbeinuTamMethod.BainHashmashot58Point5,
    RabbeinuTamMethod.BainHashmashot13Point5Before7Point083,
    RabbeinuTamMethod.BainHashmashot2Stars,
    -> null
}

val ChametzMethod.label: String get() = when (this) {
    ChametzMethod.Gra -> "GRA"
    ChametzMethod.CustomDegrees -> "Degrees"
    ChametzMethod.CustomMinutes -> "Minutes"
    ChametzMethod.CustomZmaniyotMinutes -> "Zmaniyot minutes"
    ChametzMethod.BaalHatanya -> "Baal Hatanya"
}

val ChametzMethod.labelHebrew: String get() = when (this) {
    ChametzMethod.Gra -> "גר״א"
    ChametzMethod.CustomDegrees -> "מעלות"
    ChametzMethod.CustomMinutes -> "דקות"
    ChametzMethod.CustomZmaniyotMinutes -> "דקות זמניות"
    ChametzMethod.BaalHatanya -> "בעל התניא"
}

val ChametzMethod.customUnit: CustomZmanUnit? get() = when (this) {
    ChametzMethod.CustomDegrees -> CustomZmanUnit.Degrees
    ChametzMethod.CustomMinutes -> CustomZmanUnit.Minutes
    ChametzMethod.CustomZmaniyotMinutes -> CustomZmanUnit.ZmaniyotMinutes
    ChametzMethod.Gra,
    ChametzMethod.BaalHatanya,
    -> null
}

// ---------------------------------------------------------------------------------------------
// Captions
//
// What a zmanim row says underneath the time, and what the settings row shows as its value. A named
// option reads as its name; a custom one reads as the number that was typed in, since "Custom
// degrees" alone would not tell anyone which opinion produced the time above it.
// ---------------------------------------------------------------------------------------------

private fun captionOf(unit: CustomZmanUnit?, values: CustomZmanValue, fixed: String, hebrew: Boolean): String =
    if (unit == null) fixed else customZmanLabel(unit, values, hebrew)

fun AlotHashacharMethod.caption(settings: ZmanimCalculationSettings, hebrew: Boolean): String =
    captionOf(customUnit, settings.alotHashacharCustom, if (hebrew) labelHebrew else label, hebrew)

fun MisheyakirMethod.caption(settings: ZmanimCalculationSettings, hebrew: Boolean): String {
    val unit = customUnit ?: return if (hebrew) labelHebrew else label
    val value = customZmanLabel(unit, settings.misheyakirCustom, hebrew)
    // Every option here but the degree one is an offset from something, and which something is the
    // whole point, so the caption says it: "45 דקות לפני הנץ", not "45 דקות".
    return when (this) {
        MisheyakirMethod.CustomMinutesBeforeSunrise,
        MisheyakirMethod.CustomZmaniyotMinutesBeforeSunrise,
        -> if (hebrew) "$value לפני הנץ" else "$value before sunrise"
        else -> value
    }
}

fun SofZmanShemaMethod.caption(settings: ZmanimCalculationSettings, hebrew: Boolean): String {
    val unit = customUnit ?: return if (hebrew) labelHebrew else label
    val value = customZmanLabel(unit, settings.sofZmanShemaCustom, hebrew)
    return when (this) {
        SofZmanShemaMethod.CustomDegreesToFixedLocalChatzot,
        SofZmanShemaMethod.CustomMinutesToFixedLocalChatzot,
        -> if (hebrew) "$value עד חצות מקומי קבוע" else "$value to fixed local Chatzot"
        else -> value
    }
}

fun SofZmanTefillahMethod.caption(settings: ZmanimCalculationSettings, hebrew: Boolean): String =
    captionOf(customUnit, settings.sofZmanTefillahCustom, if (hebrew) labelHebrew else label, hebrew)

fun MinchaGedolaMethod.caption(settings: ZmanimCalculationSettings, hebrew: Boolean): String =
    captionOf(customUnit, settings.minchaGedolaCustom, if (hebrew) labelHebrew else label, hebrew)

fun MinchaKetanaMethod.caption(settings: ZmanimCalculationSettings, hebrew: Boolean): String =
    captionOf(customUnit, settings.minchaKetanaCustom, if (hebrew) labelHebrew else label, hebrew)

fun PlagHaminchaMethod.caption(settings: ZmanimCalculationSettings, hebrew: Boolean): String =
    captionOf(customUnit, settings.plagHaminchaCustom, if (hebrew) labelHebrew else label, hebrew)

fun TzeitHakochavimMethod.caption(settings: ZmanimCalculationSettings, hebrew: Boolean): String =
    captionOf(customUnit, settings.tzeitHakochavimCustom, if (hebrew) labelHebrew else label, hebrew)

fun MotzeiShabbatMethod.caption(settings: ZmanimCalculationSettings, hebrew: Boolean): String =
    captionOf(customUnit, settings.motzeiShabbatCustom, if (hebrew) labelHebrew else label, hebrew)

fun RabbeinuTamMethod.caption(settings: ZmanimCalculationSettings, hebrew: Boolean): String =
    captionOf(customUnit, settings.rabbeinuTamCustom, if (hebrew) labelHebrew else label, hebrew)

fun ChametzMethod.caption(settings: ZmanimCalculationSettings, hebrew: Boolean): String =
    captionOf(customUnit, settings.chametzCustom, if (hebrew) labelHebrew else label, hebrew)

fun CandleLightingMethod.caption(settings: ZmanimCalculationSettings, hebrew: Boolean): String {
    val minutes = offsetMinutes ?: settings.candleLightingCustomMinutes
    return if (hebrew) "$minutes דקות" else "$minutes minutes"
}
