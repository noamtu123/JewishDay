package com.noamtu.jewishday.model

val ZmanimPreset.label: String get() = when (this) {
    ZmanimPreset.Standard -> "Standard GRA/Geonim"
    ZmanimPreset.MagenAvraham72 -> "Magen Avraham 72"
    ZmanimPreset.MagenAvraham16Point1 -> "Magen Avraham 16.1"
    ZmanimPreset.RabbeinuTam -> "Rabbeinu Tam"
    ZmanimPreset.Chabad -> "Chabad/Baal Hatanya"
    ZmanimPreset.Sephardi -> "Ateret Torah"
    ZmanimPreset.Ashkenazi -> "MGA + Rabbeinu Tam"
    ZmanimPreset.Israeli -> "Fixed-local Israel"
    ZmanimPreset.Custom -> "Custom"
}

val ZmanimPreset.labelHebrew: String get() = when (this) {
    ZmanimPreset.Standard -> "רגיל גר״א/גאונים"
    ZmanimPreset.MagenAvraham72 -> "מגן אברהם 72"
    ZmanimPreset.MagenAvraham16Point1 -> "מגן אברהם 16.1"
    ZmanimPreset.RabbeinuTam -> "רבינו תם"
    ZmanimPreset.Chabad -> "חב״ד"
    ZmanimPreset.Sephardi -> "עטרת תורה"
    ZmanimPreset.Ashkenazi -> "מג״א ורבינו תם"
    ZmanimPreset.Israeli -> "ישראלי חצות מקומי"
    ZmanimPreset.Custom -> "מותאם אישית"
}

val ZmanimPreset.description: String get() = when (this) {
    ZmanimPreset.Standard -> "Widely used default: GRA for daytime zmanim, Geonim 8.5 for Tzeit, and Rabbeinu Tam shown for Shabbat."
    ZmanimPreset.MagenAvraham72 -> "Magen Avraham day from Alot 72 to Tzeit 72."
    ZmanimPreset.MagenAvraham16Point1 -> "Magen Avraham day from 16.1-degree Alot to 16.1-degree Tzeit."
    ZmanimPreset.RabbeinuTam -> "End-of-day and Motzei Shabbat centered on Rabbeinu Tam 72."
    ZmanimPreset.Chabad -> "Baal Hatanya/Chabad calculations."
    ZmanimPreset.Sephardi -> "Ateret Torah methods with a 40-minute Tzeit offset."
    ZmanimPreset.Ashkenazi -> "Magen Avraham 72 for day-based zmanim with Rabbeinu Tam Motzei Shabbat."
    ZmanimPreset.Israeli -> "Israel rules with fixed-local Chatzot-based zmanim."
    ZmanimPreset.Custom -> "Full manual control."
}

val HighLatitudeHandling.label: String get() = when (this) {
    HighLatitudeHandling.Strict -> "Strict"
    HighLatitudeHandling.FixedMinutesFallback -> "Fixed fallback"
}

val HighLatitudeHandling.labelHebrew: String get() = when (this) {
    HighLatitudeHandling.Strict -> "מדויק ללא fallback"
    HighLatitudeHandling.FixedMinutesFallback -> "גיבוי דקות קבועות"
}

val AlotHashacharMethod.label: String get() = when (this) {
    AlotHashacharMethod.Minutes60 -> "60 minutes"
    AlotHashacharMethod.Minutes72 -> "72 minutes"
    AlotHashacharMethod.Minutes90 -> "90 minutes"
    AlotHashacharMethod.Minutes96 -> "96 minutes"
    AlotHashacharMethod.Minutes120 -> "120 minutes"
    AlotHashacharMethod.Zmanis72 -> "72 zmaniyot"
    AlotHashacharMethod.Zmanis90 -> "90 zmaniyot"
    AlotHashacharMethod.Zmanis96 -> "96 zmaniyot"
    AlotHashacharMethod.Zmanis120 -> "120 zmaniyot"
    AlotHashacharMethod.Degrees16Point1 -> "16.1 degrees"
    AlotHashacharMethod.Degrees18 -> "18 degrees"
    AlotHashacharMethod.Degrees19 -> "19 degrees"
    AlotHashacharMethod.Degrees19Point8 -> "19.8 degrees"
    AlotHashacharMethod.Degrees26 -> "26 degrees"
    AlotHashacharMethod.BaalHatanya -> "Baal Hatanya"
}

val AlotHashacharMethod.labelHebrew: String get() = when (this) {
    AlotHashacharMethod.Minutes60 -> "60 דקות"
    AlotHashacharMethod.Minutes72 -> "72 דקות"
    AlotHashacharMethod.Minutes90 -> "90 דקות"
    AlotHashacharMethod.Minutes96 -> "96 דקות"
    AlotHashacharMethod.Minutes120 -> "120 דקות"
    AlotHashacharMethod.Zmanis72 -> "72 זמניות"
    AlotHashacharMethod.Zmanis90 -> "90 זמניות"
    AlotHashacharMethod.Zmanis96 -> "96 זמניות"
    AlotHashacharMethod.Zmanis120 -> "120 זמניות"
    AlotHashacharMethod.Degrees16Point1 -> "16.1 מעלות"
    AlotHashacharMethod.Degrees18 -> "18 מעלות"
    AlotHashacharMethod.Degrees19 -> "19 מעלות"
    AlotHashacharMethod.Degrees19Point8 -> "19.8 מעלות"
    AlotHashacharMethod.Degrees26 -> "26 מעלות"
    AlotHashacharMethod.BaalHatanya -> "בעל התניא"
}

val MisheyakirMethod.label: String get() = when (this) {
    MisheyakirMethod.Degrees7Point65 -> "7.65 degrees"
    MisheyakirMethod.Degrees9Point5 -> "9.5 degrees"
    MisheyakirMethod.Degrees10Point2 -> "10.2 degrees"
    MisheyakirMethod.Degrees11 -> "11 degrees"
    MisheyakirMethod.Degrees11Point5 -> "11.5 degrees"
}

val MisheyakirMethod.labelHebrew: String get() = when (this) {
    MisheyakirMethod.Degrees7Point65 -> "7.65 מעלות"
    MisheyakirMethod.Degrees9Point5 -> "9.5 מעלות"
    MisheyakirMethod.Degrees10Point2 -> "10.2 מעלות"
    MisheyakirMethod.Degrees11 -> "11 מעלות"
    MisheyakirMethod.Degrees11Point5 -> "11.5 מעלות"
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
    SofZmanShemaMethod.Mga72 -> "Magen Avraham 72"
    SofZmanShemaMethod.Mga72Zmanis -> "Magen Avraham 72 zmaniyot"
    SofZmanShemaMethod.Mga90 -> "Magen Avraham 90"
    SofZmanShemaMethod.Mga90Zmanis -> "Magen Avraham 90 zmaniyot"
    SofZmanShemaMethod.Mga96 -> "Magen Avraham 96"
    SofZmanShemaMethod.Mga96Zmanis -> "Magen Avraham 96 zmaniyot"
    SofZmanShemaMethod.Mga120 -> "Magen Avraham 120"
    SofZmanShemaMethod.Mga16Point1 -> "Magen Avraham 16.1"
    SofZmanShemaMethod.Mga18 -> "Magen Avraham 18"
    SofZmanShemaMethod.Mga19Point8 -> "Magen Avraham 19.8"
    SofZmanShemaMethod.Alos16Point1ToSunset -> "Alot 16.1 to sunset"
    SofZmanShemaMethod.Alos16Point1ToTzeit7Point083 -> "Alot 16.1 to Tzeit 7.083"
    SofZmanShemaMethod.ThreeHoursBeforeChatzot -> "3h before Chatzot"
    SofZmanShemaMethod.FixedLocal -> "Fixed local"
    SofZmanShemaMethod.FixedLocalGra -> "Fixed-local GRA"
    SofZmanShemaMethod.Mga18ToFixedLocalChatzot -> "MGA 18 to fixed Chatzot"
    SofZmanShemaMethod.Mga16Point1ToFixedLocalChatzot -> "MGA 16.1 to fixed Chatzot"
    SofZmanShemaMethod.Mga90ToFixedLocalChatzot -> "MGA 90 to fixed Chatzot"
    SofZmanShemaMethod.Mga72ToFixedLocalChatzot -> "MGA 72 to fixed Chatzot"
    SofZmanShemaMethod.BaalHatanya -> "Baal Hatanya"
    SofZmanShemaMethod.AteretTorah -> "Ateret Torah"
    SofZmanShemaMethod.KolEliyahu -> "Kol Eliyahu"
}

val SofZmanShemaMethod.labelHebrew: String get() = when (this) {
    SofZmanShemaMethod.Gra -> "גר״א"
    SofZmanShemaMethod.Mga72 -> "מגן אברהם 72"
    SofZmanShemaMethod.Mga72Zmanis -> "מגן אברהם 72 זמניות"
    SofZmanShemaMethod.Mga90 -> "מגן אברהם 90"
    SofZmanShemaMethod.Mga90Zmanis -> "מגן אברהם 90 זמניות"
    SofZmanShemaMethod.Mga96 -> "מגן אברהם 96"
    SofZmanShemaMethod.Mga96Zmanis -> "מגן אברהם 96 זמניות"
    SofZmanShemaMethod.Mga120 -> "מגן אברהם 120"
    SofZmanShemaMethod.Mga16Point1 -> "מגן אברהם 16.1"
    SofZmanShemaMethod.Mga18 -> "מגן אברהם 18"
    SofZmanShemaMethod.Mga19Point8 -> "מגן אברהם 19.8"
    SofZmanShemaMethod.Alos16Point1ToSunset -> "עלות 16.1 עד שקיעה"
    SofZmanShemaMethod.Alos16Point1ToTzeit7Point083 -> "עלות 16.1 עד צאת 7.083"
    SofZmanShemaMethod.ThreeHoursBeforeChatzot -> "3 שעות לפני חצות"
    SofZmanShemaMethod.FixedLocal -> "חצות מקומי קבוע"
    SofZmanShemaMethod.FixedLocalGra -> "גר״א חצות מקומי"
    SofZmanShemaMethod.Mga18ToFixedLocalChatzot -> "מג״א 18 עד חצות מקומי"
    SofZmanShemaMethod.Mga16Point1ToFixedLocalChatzot -> "מג״א 16.1 עד חצות מקומי"
    SofZmanShemaMethod.Mga90ToFixedLocalChatzot -> "מג״א 90 עד חצות מקומי"
    SofZmanShemaMethod.Mga72ToFixedLocalChatzot -> "מג״א 72 עד חצות מקומי"
    SofZmanShemaMethod.BaalHatanya -> "בעל התניא"
    SofZmanShemaMethod.AteretTorah -> "עטרת תורה"
    SofZmanShemaMethod.KolEliyahu -> "קול אליהו"
}

val SofZmanTefillahMethod.label: String get() = when (this) {
    SofZmanTefillahMethod.Gra -> "GRA"
    SofZmanTefillahMethod.Mga72 -> "Magen Avraham 72"
    SofZmanTefillahMethod.Mga72Zmanis -> "Magen Avraham 72 zmaniyot"
    SofZmanTefillahMethod.Mga90 -> "Magen Avraham 90"
    SofZmanTefillahMethod.Mga90Zmanis -> "Magen Avraham 90 zmaniyot"
    SofZmanTefillahMethod.Mga96 -> "Magen Avraham 96"
    SofZmanTefillahMethod.Mga96Zmanis -> "Magen Avraham 96 zmaniyot"
    SofZmanTefillahMethod.Mga120 -> "Magen Avraham 120"
    SofZmanTefillahMethod.Mga16Point1 -> "Magen Avraham 16.1"
    SofZmanTefillahMethod.Mga18 -> "Magen Avraham 18"
    SofZmanTefillahMethod.Mga19Point8 -> "Magen Avraham 19.8"
    SofZmanTefillahMethod.TwoHoursBeforeChatzot -> "2h before Chatzot"
    SofZmanTefillahMethod.FixedLocal -> "Fixed local"
    SofZmanTefillahMethod.FixedLocalGra -> "Fixed-local GRA"
    SofZmanTefillahMethod.BaalHatanya -> "Baal Hatanya"
    SofZmanTefillahMethod.AteretTorah -> "Ateret Torah"
}

val SofZmanTefillahMethod.labelHebrew: String get() = when (this) {
    SofZmanTefillahMethod.Gra -> "גר״א"
    SofZmanTefillahMethod.Mga72 -> "מגן אברהם 72"
    SofZmanTefillahMethod.Mga72Zmanis -> "מגן אברהם 72 זמניות"
    SofZmanTefillahMethod.Mga90 -> "מגן אברהם 90"
    SofZmanTefillahMethod.Mga90Zmanis -> "מגן אברהם 90 זמניות"
    SofZmanTefillahMethod.Mga96 -> "מגן אברהם 96"
    SofZmanTefillahMethod.Mga96Zmanis -> "מגן אברהם 96 זמניות"
    SofZmanTefillahMethod.Mga120 -> "מגן אברהם 120"
    SofZmanTefillahMethod.Mga16Point1 -> "מגן אברהם 16.1"
    SofZmanTefillahMethod.Mga18 -> "מגן אברהם 18"
    SofZmanTefillahMethod.Mga19Point8 -> "מגן אברהם 19.8"
    SofZmanTefillahMethod.TwoHoursBeforeChatzot -> "2 שעות לפני חצות"
    SofZmanTefillahMethod.FixedLocal -> "חצות מקומי קבוע"
    SofZmanTefillahMethod.FixedLocalGra -> "גר״א חצות מקומי"
    SofZmanTefillahMethod.BaalHatanya -> "בעל התניא"
    SofZmanTefillahMethod.AteretTorah -> "עטרת תורה"
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
    MinchaGedolaMethod.ThirtyMinutes -> "30 minutes after Chatzot"
    MinchaGedolaMethod.GreaterThan30 -> "Greater than 30"
    MinchaGedolaMethod.Mga72 -> "Magen Avraham 72"
    MinchaGedolaMethod.Degrees16Point1 -> "16.1 degrees"
    MinchaGedolaMethod.FixedLocal -> "Fixed-local"
    MinchaGedolaMethod.BaalHatanya -> "Baal Hatanya"
    MinchaGedolaMethod.BaalHatanyaGreaterThan30 -> "Baal Hatanya >30"
    MinchaGedolaMethod.AteretTorah -> "Ateret Torah"
    MinchaGedolaMethod.AhavatShalom -> "Ahavat Shalom"
}

val MinchaGedolaMethod.labelHebrew: String get() = when (this) {
    MinchaGedolaMethod.Standard -> "גר״א"
    MinchaGedolaMethod.ThirtyMinutes -> "30 דקות אחרי חצות"
    MinchaGedolaMethod.GreaterThan30 -> "המאוחר מ-30 דקות"
    MinchaGedolaMethod.Mga72 -> "מגן אברהם 72"
    MinchaGedolaMethod.Degrees16Point1 -> "16.1 מעלות"
    MinchaGedolaMethod.FixedLocal -> "חצות מקומי"
    MinchaGedolaMethod.BaalHatanya -> "בעל התניא"
    MinchaGedolaMethod.BaalHatanyaGreaterThan30 -> "בעל התניא המאוחר מ-30"
    MinchaGedolaMethod.AteretTorah -> "עטרת תורה"
    MinchaGedolaMethod.AhavatShalom -> "אהבת שלום"
}

val MinchaKetanaMethod.label: String get() = when (this) {
    MinchaKetanaMethod.Standard -> "GRA"
    MinchaKetanaMethod.Mga72 -> "Magen Avraham 72"
    MinchaKetanaMethod.Degrees16Point1 -> "16.1 degrees"
    MinchaKetanaMethod.FixedLocal -> "Fixed-local"
    MinchaKetanaMethod.BaalHatanya -> "Baal Hatanya"
    MinchaKetanaMethod.AteretTorah -> "Ateret Torah"
    MinchaKetanaMethod.AhavatShalom -> "Ahavat Shalom"
}

val MinchaKetanaMethod.labelHebrew: String get() = when (this) {
    MinchaKetanaMethod.Standard -> "גר״א"
    MinchaKetanaMethod.Mga72 -> "מגן אברהם 72"
    MinchaKetanaMethod.Degrees16Point1 -> "16.1 מעלות"
    MinchaKetanaMethod.FixedLocal -> "חצות מקומי"
    MinchaKetanaMethod.BaalHatanya -> "בעל התניא"
    MinchaKetanaMethod.AteretTorah -> "עטרת תורה"
    MinchaKetanaMethod.AhavatShalom -> "אהבת שלום"
}

val PlagHaminchaMethod.label: String get() = when (this) {
    PlagHaminchaMethod.Gra -> "GRA"
    PlagHaminchaMethod.Mga60 -> "Magen Avraham 60"
    PlagHaminchaMethod.Mga72 -> "Magen Avraham 72"
    PlagHaminchaMethod.Mga72Zmanis -> "Magen Avraham 72 zmaniyot"
    PlagHaminchaMethod.Mga90 -> "Magen Avraham 90"
    PlagHaminchaMethod.Mga90Zmanis -> "Magen Avraham 90 zmaniyot"
    PlagHaminchaMethod.Mga96 -> "Magen Avraham 96"
    PlagHaminchaMethod.Mga96Zmanis -> "Magen Avraham 96 zmaniyot"
    PlagHaminchaMethod.Mga120 -> "Magen Avraham 120"
    PlagHaminchaMethod.Mga120Zmanis -> "Magen Avraham 120 zmaniyot"
    PlagHaminchaMethod.Degrees16Point1 -> "16.1 degrees"
    PlagHaminchaMethod.Degrees18 -> "18 degrees"
    PlagHaminchaMethod.Degrees19Point8 -> "19.8 degrees"
    PlagHaminchaMethod.Degrees26 -> "26 degrees"
    PlagHaminchaMethod.AlotToSunset -> "Alot to sunset"
    PlagHaminchaMethod.Alot16Point1ToTzeit7Point083 -> "Alot 16.1 to Tzeit 7.083"
    PlagHaminchaMethod.FixedLocal -> "Fixed-local"
    PlagHaminchaMethod.BaalHatanya -> "Baal Hatanya"
    PlagHaminchaMethod.AteretTorah -> "Ateret Torah"
    PlagHaminchaMethod.AhavatShalom -> "Ahavat Shalom"
}

val PlagHaminchaMethod.labelHebrew: String get() = when (this) {
    PlagHaminchaMethod.Gra -> "גר״א"
    PlagHaminchaMethod.Mga60 -> "מגן אברהם 60"
    PlagHaminchaMethod.Mga72 -> "מגן אברהם 72"
    PlagHaminchaMethod.Mga72Zmanis -> "מגן אברהם 72 זמניות"
    PlagHaminchaMethod.Mga90 -> "מגן אברהם 90"
    PlagHaminchaMethod.Mga90Zmanis -> "מגן אברהם 90 זמניות"
    PlagHaminchaMethod.Mga96 -> "מגן אברהם 96"
    PlagHaminchaMethod.Mga96Zmanis -> "מגן אברהם 96 זמניות"
    PlagHaminchaMethod.Mga120 -> "מגן אברהם 120"
    PlagHaminchaMethod.Mga120Zmanis -> "מגן אברהם 120 זמניות"
    PlagHaminchaMethod.Degrees16Point1 -> "16.1 מעלות"
    PlagHaminchaMethod.Degrees18 -> "18 מעלות"
    PlagHaminchaMethod.Degrees19Point8 -> "19.8 מעלות"
    PlagHaminchaMethod.Degrees26 -> "26 מעלות"
    PlagHaminchaMethod.AlotToSunset -> "עלות עד שקיעה"
    PlagHaminchaMethod.Alot16Point1ToTzeit7Point083 -> "עלות 16.1 עד צאת 7.083"
    PlagHaminchaMethod.FixedLocal -> "חצות מקומי"
    PlagHaminchaMethod.BaalHatanya -> "בעל התניא"
    PlagHaminchaMethod.AteretTorah -> "עטרת תורה"
    PlagHaminchaMethod.AhavatShalom -> "אהבת שלום"
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
    TzeitHakochavimMethod.Geonim3Point7 -> "Geonim 3.7"
    TzeitHakochavimMethod.Geonim3Point8 -> "Geonim 3.8"
    TzeitHakochavimMethod.Geonim3Point65 -> "Geonim 3.65"
    TzeitHakochavimMethod.Geonim3Point676 -> "Geonim 3.676"
    TzeitHakochavimMethod.Geonim4Point37 -> "Geonim 4.37"
    TzeitHakochavimMethod.Geonim4Point61 -> "Geonim 4.61"
    TzeitHakochavimMethod.Geonim4Point8 -> "Geonim 4.8"
    TzeitHakochavimMethod.Geonim5Point88 -> "Geonim 5.88"
    TzeitHakochavimMethod.Geonim5Point95 -> "Geonim 5.95"
    TzeitHakochavimMethod.Geonim6Point45 -> "Geonim 6.45"
    TzeitHakochavimMethod.Geonim7Point083 -> "Geonim 7.083"
    TzeitHakochavimMethod.Geonim7Point67 -> "Geonim 7.67"
    TzeitHakochavimMethod.Geonim8Point5 -> "Geonim 8.5"
    TzeitHakochavimMethod.Geonim9Point3 -> "Geonim 9.3"
    TzeitHakochavimMethod.Geonim9Point75 -> "Geonim 9.75"
    TzeitHakochavimMethod.Minutes50 -> "50 minutes"
    TzeitHakochavimMethod.Minutes60 -> "60 minutes"
    TzeitHakochavimMethod.Minutes72 -> "72 minutes"
    TzeitHakochavimMethod.Minutes90 -> "90 minutes"
    TzeitHakochavimMethod.Minutes96 -> "96 minutes"
    TzeitHakochavimMethod.Minutes120 -> "120 minutes"
    TzeitHakochavimMethod.Zmanis72 -> "72 zmaniyot"
    TzeitHakochavimMethod.Zmanis90 -> "90 zmaniyot"
    TzeitHakochavimMethod.Zmanis96 -> "96 zmaniyot"
    TzeitHakochavimMethod.Zmanis120 -> "120 zmaniyot"
    TzeitHakochavimMethod.Degrees16Point1 -> "16.1 degrees"
    TzeitHakochavimMethod.Degrees18 -> "18 degrees"
    TzeitHakochavimMethod.Degrees19Point8 -> "19.8 degrees"
    TzeitHakochavimMethod.Degrees26 -> "26 degrees"
    TzeitHakochavimMethod.BaalHatanya -> "Baal Hatanya"
    TzeitHakochavimMethod.AteretTorah -> "Ateret Torah"
}

val TzeitHakochavimMethod.labelHebrew: String get() = when (this) {
    TzeitHakochavimMethod.Geonim3Point7 -> "גאונים 3.7"
    TzeitHakochavimMethod.Geonim3Point8 -> "גאונים 3.8"
    TzeitHakochavimMethod.Geonim3Point65 -> "גאונים 3.65"
    TzeitHakochavimMethod.Geonim3Point676 -> "גאונים 3.676"
    TzeitHakochavimMethod.Geonim4Point37 -> "גאונים 4.37"
    TzeitHakochavimMethod.Geonim4Point61 -> "גאונים 4.61"
    TzeitHakochavimMethod.Geonim4Point8 -> "גאונים 4.8"
    TzeitHakochavimMethod.Geonim5Point88 -> "גאונים 5.88"
    TzeitHakochavimMethod.Geonim5Point95 -> "גאונים 5.95"
    TzeitHakochavimMethod.Geonim6Point45 -> "גאונים 6.45"
    TzeitHakochavimMethod.Geonim7Point083 -> "גאונים 7.083"
    TzeitHakochavimMethod.Geonim7Point67 -> "גאונים 7.67"
    TzeitHakochavimMethod.Geonim8Point5 -> "גאונים 8.5"
    TzeitHakochavimMethod.Geonim9Point3 -> "גאונים 9.3"
    TzeitHakochavimMethod.Geonim9Point75 -> "גאונים 9.75"
    TzeitHakochavimMethod.Minutes50 -> "50 דקות"
    TzeitHakochavimMethod.Minutes60 -> "60 דקות"
    TzeitHakochavimMethod.Minutes72 -> "72 דקות"
    TzeitHakochavimMethod.Minutes90 -> "90 דקות"
    TzeitHakochavimMethod.Minutes96 -> "96 דקות"
    TzeitHakochavimMethod.Minutes120 -> "120 דקות"
    TzeitHakochavimMethod.Zmanis72 -> "72 זמניות"
    TzeitHakochavimMethod.Zmanis90 -> "90 זמניות"
    TzeitHakochavimMethod.Zmanis96 -> "96 זמניות"
    TzeitHakochavimMethod.Zmanis120 -> "120 זמניות"
    TzeitHakochavimMethod.Degrees16Point1 -> "16.1 מעלות"
    TzeitHakochavimMethod.Degrees18 -> "18 מעלות"
    TzeitHakochavimMethod.Degrees19Point8 -> "19.8 מעלות"
    TzeitHakochavimMethod.Degrees26 -> "26 מעלות"
    TzeitHakochavimMethod.BaalHatanya -> "בעל התניא"
    TzeitHakochavimMethod.AteretTorah -> "עטרת תורה"
}

val CandleLightingMethod.label: String get() = "${offsetMinutes} minutes"
val CandleLightingMethod.labelHebrew: String get() = "$offsetMinutes דקות"

val MotzeiShabbatMethod.label: String get() = when (this) {
    MotzeiShabbatMethod.Geonim5Point88 -> "Geonim 5.88"
    MotzeiShabbatMethod.Geonim7Point083 -> "Geonim 7.083"
    MotzeiShabbatMethod.Geonim8Point5 -> "Geonim 8.5"
    MotzeiShabbatMethod.Geonim9Point3 -> "Geonim 9.3"
    MotzeiShabbatMethod.Minutes50 -> "50 minutes"
    MotzeiShabbatMethod.Minutes60 -> "60 minutes"
    MotzeiShabbatMethod.Minutes72 -> "72 minutes"
    MotzeiShabbatMethod.Minutes90 -> "90 minutes"
    MotzeiShabbatMethod.Minutes96 -> "96 minutes"
    MotzeiShabbatMethod.Minutes120 -> "120 minutes"
    MotzeiShabbatMethod.RabbeinuTam72 -> "Rabbeinu Tam 72"
    MotzeiShabbatMethod.RabbeinuTam90 -> "Rabbeinu Tam 90"
    MotzeiShabbatMethod.RabbeinuTam120 -> "Rabbeinu Tam 120"
    MotzeiShabbatMethod.BaalHatanya -> "Baal Hatanya"
    MotzeiShabbatMethod.AteretTorah -> "Ateret Torah"
}

val MotzeiShabbatMethod.labelHebrew: String get() = when (this) {
    MotzeiShabbatMethod.Geonim5Point88 -> "גאונים 5.88"
    MotzeiShabbatMethod.Geonim7Point083 -> "גאונים 7.083"
    MotzeiShabbatMethod.Geonim8Point5 -> "גאונים 8.5"
    MotzeiShabbatMethod.Geonim9Point3 -> "גאונים 9.3"
    MotzeiShabbatMethod.Minutes50 -> "50 דקות"
    MotzeiShabbatMethod.Minutes60 -> "60 דקות"
    MotzeiShabbatMethod.Minutes72 -> "72 דקות"
    MotzeiShabbatMethod.Minutes90 -> "90 דקות"
    MotzeiShabbatMethod.Minutes96 -> "96 דקות"
    MotzeiShabbatMethod.Minutes120 -> "120 דקות"
    MotzeiShabbatMethod.RabbeinuTam72 -> "רבינו תם 72"
    MotzeiShabbatMethod.RabbeinuTam90 -> "רבינו תם 90"
    MotzeiShabbatMethod.RabbeinuTam120 -> "רבינו תם 120"
    MotzeiShabbatMethod.BaalHatanya -> "בעל התניא"
    MotzeiShabbatMethod.AteretTorah -> "עטרת תורה"
}

val RabbeinuTamMethod.label: String get() = when (this) {
    RabbeinuTamMethod.Minutes72 -> "72 minutes"
    RabbeinuTamMethod.Minutes90 -> "90 minutes"
    RabbeinuTamMethod.Minutes120 -> "120 minutes"
    RabbeinuTamMethod.Zmanis72 -> "72 zmaniyot"
    RabbeinuTamMethod.Degrees16Point1 -> "16.1 degrees"
    RabbeinuTamMethod.Degrees18 -> "18 degrees"
    RabbeinuTamMethod.Degrees19Point8 -> "19.8 degrees"
    RabbeinuTamMethod.Degrees26 -> "26 degrees"
    RabbeinuTamMethod.BainHashmashot13Point24 -> "RT 13.24 degrees"
    RabbeinuTamMethod.BainHashmashot58Point5 -> "RT 58.5 minutes"
    RabbeinuTamMethod.BainHashmashot13Point5Before7Point083 -> "RT 13.5 before 7.083"
    RabbeinuTamMethod.BainHashmashot2Stars -> "RT 2 stars"
}

val RabbeinuTamMethod.labelHebrew: String get() = when (this) {
    RabbeinuTamMethod.Minutes72 -> "72 דקות"
    RabbeinuTamMethod.Minutes90 -> "90 דקות"
    RabbeinuTamMethod.Minutes120 -> "120 דקות"
    RabbeinuTamMethod.Zmanis72 -> "72 זמניות"
    RabbeinuTamMethod.Degrees16Point1 -> "16.1 מעלות"
    RabbeinuTamMethod.Degrees18 -> "18 מעלות"
    RabbeinuTamMethod.Degrees19Point8 -> "19.8 מעלות"
    RabbeinuTamMethod.Degrees26 -> "26 מעלות"
    RabbeinuTamMethod.BainHashmashot13Point24 -> "ר״ת 13.24 מעלות"
    RabbeinuTamMethod.BainHashmashot58Point5 -> "ר״ת 58.5 דקות"
    RabbeinuTamMethod.BainHashmashot13Point5Before7Point083 -> "ר״ת 13.5 לפני 7.083"
    RabbeinuTamMethod.BainHashmashot2Stars -> "ר״ת שני כוכבים"
}

val BainHashmashotMethod.label: String get() = when (this) {
    BainHashmashotMethod.RabbeinuTam13Point24 -> "Rabbeinu Tam 13.24"
    BainHashmashotMethod.RabbeinuTam58Point5 -> "Rabbeinu Tam 58.5"
    BainHashmashotMethod.RabbeinuTam13Point5Before7Point083 -> "Rabbeinu Tam 13.5 before 7.083"
    BainHashmashotMethod.RabbeinuTam2Stars -> "Rabbeinu Tam 2 stars"
    BainHashmashotMethod.Yereim18Minutes -> "Yereim 18 minutes"
    BainHashmashotMethod.Yereim3Point05 -> "Yereim 3.05 degrees"
    BainHashmashotMethod.Yereim16Point875Minutes -> "Yereim 16.875 minutes"
    BainHashmashotMethod.Yereim2Point8 -> "Yereim 2.8 degrees"
    BainHashmashotMethod.Yereim13Point5Minutes -> "Yereim 13.5 minutes"
    BainHashmashotMethod.Yereim2Point1 -> "Yereim 2.1 degrees"
}

val BainHashmashotMethod.labelHebrew: String get() = when (this) {
    BainHashmashotMethod.RabbeinuTam13Point24 -> "רבינו תם 13.24"
    BainHashmashotMethod.RabbeinuTam58Point5 -> "רבינו תם 58.5"
    BainHashmashotMethod.RabbeinuTam13Point5Before7Point083 -> "רבינו תם 13.5 לפני 7.083"
    BainHashmashotMethod.RabbeinuTam2Stars -> "רבינו תם שני כוכבים"
    BainHashmashotMethod.Yereim18Minutes -> "יראים 18 דקות"
    BainHashmashotMethod.Yereim3Point05 -> "יראים 3.05 מעלות"
    BainHashmashotMethod.Yereim16Point875Minutes -> "יראים 16.875 דקות"
    BainHashmashotMethod.Yereim2Point8 -> "יראים 2.8 מעלות"
    BainHashmashotMethod.Yereim13Point5Minutes -> "יראים 13.5 דקות"
    BainHashmashotMethod.Yereim2Point1 -> "יראים 2.1 מעלות"
}

val FastDayMethod.label: String get() = when (this) {
    FastDayMethod.Alot72ToTzeit8Point5 -> "Alot 72 / Tzeit 8.5"
    FastDayMethod.Alot72ToTzeit7Point083 -> "Alot 72 / Tzeit 7.083"
    FastDayMethod.Alot72ToTzeit5Point88 -> "Alot 72 / Tzeit 5.88"
    FastDayMethod.Alot16Point1ToTzeit8Point5 -> "Alot 16.1 / Tzeit 8.5"
    FastDayMethod.Alot16Point1ToTzeit7Point083 -> "Alot 16.1 / Tzeit 7.083"
    FastDayMethod.BaalHatanya -> "Baal Hatanya"
}

val FastDayMethod.labelHebrew: String get() = when (this) {
    FastDayMethod.Alot72ToTzeit8Point5 -> "עלות 72 / צאת 8.5"
    FastDayMethod.Alot72ToTzeit7Point083 -> "עלות 72 / צאת 7.083"
    FastDayMethod.Alot72ToTzeit5Point88 -> "עלות 72 / צאת 5.88"
    FastDayMethod.Alot16Point1ToTzeit8Point5 -> "עלות 16.1 / צאת 8.5"
    FastDayMethod.Alot16Point1ToTzeit7Point083 -> "עלות 16.1 / צאת 7.083"
    FastDayMethod.BaalHatanya -> "בעל התניא"
}

val ChametzMethod.label: String get() = when (this) {
    ChametzMethod.Gra -> "GRA"
    ChametzMethod.Mga72 -> "Magen Avraham 72"
    ChametzMethod.Mga16Point1 -> "Magen Avraham 16.1"
    ChametzMethod.BaalHatanya -> "Baal Hatanya"
}

val ChametzMethod.labelHebrew: String get() = when (this) {
    ChametzMethod.Gra -> "גר״א"
    ChametzMethod.Mga72 -> "מגן אברהם 72"
    ChametzMethod.Mga16Point1 -> "מגן אברהם 16.1"
    ChametzMethod.BaalHatanya -> "בעל התניא"
}
