// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

// Which unit each option is entered in, or null when it is a fixed or named opinion. A custom option
// reads as the number typed in (its caption), and its settings dialog asks for a value in this unit.

public val AlotHashacharMethod.customUnit: CustomZmanUnit? get() = when (this) {
    AlotHashacharMethod.CustomDegrees -> CustomZmanUnit.Degrees
    AlotHashacharMethod.CustomMinutes -> CustomZmanUnit.Minutes
    AlotHashacharMethod.CustomZmaniyotMinutes -> CustomZmanUnit.ZmaniyotMinutes
    AlotHashacharMethod.Degrees16Point1 -> null
}

public val MisheyakirMethod.customUnit: CustomZmanUnit? get() = when (this) {
    MisheyakirMethod.CustomDegrees -> CustomZmanUnit.Degrees
    MisheyakirMethod.CustomMinutesBeforeSunrise -> CustomZmanUnit.Minutes
    MisheyakirMethod.CustomZmaniyotMinutesBeforeSunrise -> CustomZmanUnit.ZmaniyotMinutes
    MisheyakirMethod.Degrees11,
    MisheyakirMethod.Minutes6AfterAlos,
    -> null
}

public val SofZmanShemaMethod.customUnit: CustomZmanUnit? get() = when (this) {
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

public val SofZmanTefillahMethod.customUnit: CustomZmanUnit? get() = when (this) {
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

public val MinchaGedolaMethod.customUnit: CustomZmanUnit? get() = when (this) {
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

public val MinchaKetanaMethod.customUnit: CustomZmanUnit? get() = when (this) {
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

public val PlagHaminchaMethod.customUnit: CustomZmanUnit? get() = when (this) {
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

public val TzeitHakochavimMethod.customUnit: CustomZmanUnit? get() = when (this) {
    TzeitHakochavimMethod.CustomDegrees -> CustomZmanUnit.Degrees
    TzeitHakochavimMethod.CustomMinutes -> CustomZmanUnit.Minutes
    TzeitHakochavimMethod.CustomZmaniyotMinutes -> CustomZmanUnit.ZmaniyotMinutes
    TzeitHakochavimMethod.Degrees6Point2 -> null
}

public val MotzeiShabbatMethod.customUnit: CustomZmanUnit? get() = when (this) {
    MotzeiShabbatMethod.CustomDegrees -> CustomZmanUnit.Degrees
    MotzeiShabbatMethod.CustomMinutes -> CustomZmanUnit.Minutes
    MotzeiShabbatMethod.CustomZmaniyotMinutes -> CustomZmanUnit.ZmaniyotMinutes
    MotzeiShabbatMethod.Degrees6Point2 -> null
}

public val RabbeinuTamMethod.customUnit: CustomZmanUnit? get() = when (this) {
    RabbeinuTamMethod.CustomDegrees -> CustomZmanUnit.Degrees
    RabbeinuTamMethod.CustomMinutes -> CustomZmanUnit.Minutes
    RabbeinuTamMethod.CustomZmaniyotMinutes -> CustomZmanUnit.ZmaniyotMinutes
    RabbeinuTamMethod.Minutes72,
    RabbeinuTamMethod.BainHashmashot58Point5,
    RabbeinuTamMethod.BainHashmashot13Point5Before7Point083,
    RabbeinuTamMethod.BainHashmashot2Stars,
    -> null
}

public val ChametzMethod.customUnit: CustomZmanUnit? get() = when (this) {
    ChametzMethod.CustomDegrees -> CustomZmanUnit.Degrees
    ChametzMethod.CustomMinutes -> CustomZmanUnit.Minutes
    ChametzMethod.CustomZmaniyotMinutes -> CustomZmanUnit.ZmaniyotMinutes
    ChametzMethod.Gra,
    ChametzMethod.BaalHatanya,
    -> null
}
