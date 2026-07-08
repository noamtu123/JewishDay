// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

enum class ZmanimPreset(val storageValue: String) {
    Standard("standard"),
    MagenAvraham72("magen_avraham_72"),
    MagenAvraham16Point1("magen_avraham_16_1"),
    RabbeinuTam("rabbeinu_tam"),
    Chabad("chabad"),
    Ashkenazi("ashkenazi"),
    Israeli("israeli"),
    Custom("custom"),
    ;

    companion object {
        fun fromStorageValue(value: String?): ZmanimPreset? =
            entries.firstOrNull { it.storageValue == value }
    }
}

// Method options are ordered for display: degrees (ascending), then minutes (ascending),
// then zmaniyot minutes (ascending), then special/named methods last. Baal Hatanya for
// Alot Hashachar is an ordinary degree method (16.9°), so it sits within the degrees group.

enum class AlotHashacharMethod(val storageValue: String) {
    Degrees12("degrees_12"),
    Degrees14("degrees_14"),
    Degrees16("degrees_16"),
    Degrees16Point013("degrees_16_013"),
    Degrees16Point04("degrees_16_04"),
    Degrees16Point08("degrees_16_08"),
    Degrees16Point1("degrees_16_1"),
    BaalHatanya("baal_hatanya"),
    Degrees17Point5("degrees_17_5"),
    Degrees18("degrees_18"),
    Degrees19("degrees_19"),
    Degrees19Point75("degrees_19_75"),
    Degrees19Point784("degrees_19_784"),
    Degrees19Point8("degrees_19_8"),
    Degrees19Point848("degrees_19_848"),
    Degrees20("degrees_20"),
    Degrees26("degrees_26"),
    Minutes60("minutes_60"),
    Minutes72("minutes_72"),
    Minutes90("minutes_90"),
    Minutes96("minutes_96"),
    Minutes120("minutes_120"),
    Zmanis72("zmanis_72"),
    Zmanis90("zmanis_90"),
    Zmanis96("zmanis_96"),
    Zmanis120("zmanis_120"),
    ;

    companion object {
        fun fromStorageValue(value: String?): AlotHashacharMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

enum class MisheyakirMethod(val storageValue: String) {
    Degrees7Point65("degrees_7_65"),
    Degrees9Point5("degrees_9_5"),
    Degrees10Point2("degrees_10_2"),
    Degrees11("degrees_11"),
    Degrees11Point5("degrees_11_5"),
    Degrees12("degrees_12"),
    Degrees12Point85("degrees_12_85"),
    Minutes6AfterAlos("minutes_6_after_alos"),
    Minutes35BeforeSunrise("minutes_35_before_sunrise"),
    Minutes36BeforeSunrise("minutes_36_before_sunrise"),
    Minutes40BeforeSunrise("minutes_40_before_sunrise"),
    Minutes42BeforeSunrise("minutes_42_before_sunrise"),
    Minutes45BeforeSunrise("minutes_45_before_sunrise"),
    Minutes48BeforeSunrise("minutes_48_before_sunrise"),
    Minutes50BeforeSunrise("minutes_50_before_sunrise"),
    Minutes52BeforeSunrise("minutes_52_before_sunrise"),
    Minutes57BeforeSunrise("minutes_57_before_sunrise"),
    Minutes60BeforeSunrise("minutes_60_before_sunrise"),
    ;

    companion object {
        fun fromStorageValue(value: String?): MisheyakirMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

enum class SunriseMethod(val storageValue: String) {
    SeaLevel("sea_level"),
    ElevationAdjusted("elevation_adjusted"),
    ;

    companion object {
        fun fromStorageValue(value: String?): SunriseMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

/**
 * Which halachic school a Sof Zman Shema / Tefillah opinion belongs to. The Zmanim
 * list shows a GRA row and a Magen Avraham row separately, and each row's method
 * picker only offers options from its own family.
 */
enum class ZmanOpinionFamily { Gra, MagenAvraham }

enum class SofZmanShemaMethod(val storageValue: String, val family: ZmanOpinionFamily) {
    Gra("gra", ZmanOpinionFamily.Gra),
    FixedLocalGra("fixed_local_gra", ZmanOpinionFamily.Gra),
    Mga16Point1("mga_16_1", ZmanOpinionFamily.MagenAvraham),
    Mga18("mga_18", ZmanOpinionFamily.MagenAvraham),
    Mga19Point8("mga_19_8", ZmanOpinionFamily.MagenAvraham),
    Mga72("mga_72", ZmanOpinionFamily.MagenAvraham),
    Mga90("mga_90", ZmanOpinionFamily.MagenAvraham),
    Mga96("mga_96", ZmanOpinionFamily.MagenAvraham),
    Mga120("mga_120", ZmanOpinionFamily.MagenAvraham),
    Mga72Zmanis("mga_72_zmanis", ZmanOpinionFamily.MagenAvraham),
    Mga90Zmanis("mga_90_zmanis", ZmanOpinionFamily.MagenAvraham),
    Mga96Zmanis("mga_96_zmanis", ZmanOpinionFamily.MagenAvraham),
    Alos16Point1ToSunset("alos_16_1_to_sunset", ZmanOpinionFamily.MagenAvraham),
    Alos16Point1ToTzeit7Point083("alos_16_1_to_tzeit_7_083", ZmanOpinionFamily.MagenAvraham),
    Mga16Point1ToFixedLocalChatzot("mga_16_1_to_fixed_local_chatzot", ZmanOpinionFamily.MagenAvraham),
    Mga18ToFixedLocalChatzot("mga_18_to_fixed_local_chatzot", ZmanOpinionFamily.MagenAvraham),
    Mga72ToFixedLocalChatzot("mga_72_to_fixed_local_chatzot", ZmanOpinionFamily.MagenAvraham),
    Mga90ToFixedLocalChatzot("mga_90_to_fixed_local_chatzot", ZmanOpinionFamily.MagenAvraham),
    AteretTorah("ateret_torah", ZmanOpinionFamily.MagenAvraham),
    ;

    companion object {
        fun fromStorageValue(value: String?): SofZmanShemaMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

enum class SofZmanTefillahMethod(val storageValue: String, val family: ZmanOpinionFamily) {
    Gra("gra", ZmanOpinionFamily.Gra),
    FixedLocalGra("fixed_local_gra", ZmanOpinionFamily.Gra),
    Mga16Point1("mga_16_1", ZmanOpinionFamily.MagenAvraham),
    Mga18("mga_18", ZmanOpinionFamily.MagenAvraham),
    Mga19Point8("mga_19_8", ZmanOpinionFamily.MagenAvraham),
    Mga26("mga_26", ZmanOpinionFamily.MagenAvraham),
    Mga60("mga_60", ZmanOpinionFamily.MagenAvraham),
    Mga72("mga_72", ZmanOpinionFamily.MagenAvraham),
    Mga90("mga_90", ZmanOpinionFamily.MagenAvraham),
    Mga96("mga_96", ZmanOpinionFamily.MagenAvraham),
    Mga120("mga_120", ZmanOpinionFamily.MagenAvraham),
    Mga72Zmanis("mga_72_zmanis", ZmanOpinionFamily.MagenAvraham),
    Mga90Zmanis("mga_90_zmanis", ZmanOpinionFamily.MagenAvraham),
    Mga96Zmanis("mga_96_zmanis", ZmanOpinionFamily.MagenAvraham),
    Mga120Zmanis("mga_120_zmanis", ZmanOpinionFamily.MagenAvraham),
    Alos16Point1ToSunset("alos_16_1_to_sunset", ZmanOpinionFamily.MagenAvraham),
    Alos16Point1ToTzeit7Point083("alos_16_1_to_tzeit_7_083", ZmanOpinionFamily.MagenAvraham),
    ;

    companion object {
        fun fromStorageValue(value: String?): SofZmanTefillahMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

enum class ChatzotMethod(val storageValue: String) {
    Solar("solar"),
    FixedLocal("fixed_local"),
    ;

    companion object {
        fun fromStorageValue(value: String?): ChatzotMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

enum class MinchaGedolaMethod(val storageValue: String) {
    Degrees16Point1("degrees_16_1"),
    ThirtyMinutes("thirty_minutes"),
    Mga72("mga_72"),
    Standard("standard"),
    GreaterThan30("greater_than_30"),
    FixedLocal("fixed_local"),
    BaalHatanya("baal_hatanya"),
    BaalHatanyaGreaterThan30("baal_hatanya_greater_than_30"),
    AteretTorah("ateret_torah"),
    AhavatShalom("ahavat_shalom"),
    ;

    companion object {
        fun fromStorageValue(value: String?): MinchaGedolaMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

enum class MinchaKetanaMethod(val storageValue: String) {
    Degrees16Point1("degrees_16_1"),
    Mga72("mga_72"),
    Standard("standard"),
    FixedLocal("fixed_local"),
    BaalHatanya("baal_hatanya"),
    AteretTorah("ateret_torah"),
    AhavatShalom("ahavat_shalom"),
    ;

    companion object {
        fun fromStorageValue(value: String?): MinchaKetanaMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

enum class PlagHaminchaMethod(val storageValue: String) {
    Degrees16Point1("degrees_16_1"),
    Degrees18("degrees_18"),
    Degrees19Point8("degrees_19_8"),
    Degrees26("degrees_26"),
    Mga60("mga_60"),
    Mga72("mga_72"),
    Mga90("mga_90"),
    Mga96("mga_96"),
    Mga120("mga_120"),
    Mga72Zmanis("mga_72_zmanis"),
    Mga90Zmanis("mga_90_zmanis"),
    Mga96Zmanis("mga_96_zmanis"),
    Mga120Zmanis("mga_120_zmanis"),
    Gra("gra"),
    AlotToSunset("alot_to_sunset"),
    Alot16Point1ToTzeit7Point083("alos_16_1_to_tzeit_7_083"),
    FixedLocal("fixed_local"),
    BaalHatanya("baal_hatanya"),
    AteretTorah("ateret_torah"),
    AhavatShalom("ahavat_shalom"),
    ;

    companion object {
        fun fromStorageValue(value: String?): PlagHaminchaMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

enum class SunsetMethod(val storageValue: String) {
    SeaLevel("sea_level"),
    ElevationAdjusted("elevation_adjusted"),
    ;

    companion object {
        fun fromStorageValue(value: String?): SunsetMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

enum class TzeitHakochavimMethod(val storageValue: String) {
    Geonim3Point7("geonim_3_7"),
    Geonim3Point8("geonim_3_8"),
    Geonim4Point42("geonim_4_42"),
    Geonim4Point66("geonim_4_66"),
    Geonim4Point8("geonim_4_8"),
    Geonim5Point95("geonim_5_95"),
    Degrees6Point2("degrees_6_2"),
    Geonim6Point45("geonim_6_45"),
    Geonim7Point083("geonim_7_083"),
    Geonim7Point67("geonim_7_67"),
    Geonim8Point5("geonim_8_5"),
    Geonim9Point3("geonim_9_3"),
    Geonim9Point75("geonim_9_75"),
    Degrees16Point1("degrees_16_1"),
    Degrees18("degrees_18"),
    Degrees19Point8("degrees_19_8"),
    Degrees26("degrees_26"),
    Minutes50("minutes_50"),
    Minutes60("minutes_60"),
    Minutes72("minutes_72"),
    Minutes90("minutes_90"),
    Minutes96("minutes_96"),
    Minutes120("minutes_120"),
    Zmanis72("zmanis_72"),
    Zmanis90("zmanis_90"),
    Zmanis96("zmanis_96"),
    Zmanis120("zmanis_120"),
    AteretTorah("ateret_torah"),
    BaalHatanya("baal_hatanya"),
    ;

    companion object {
        fun fromStorageValue(value: String?): TzeitHakochavimMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

enum class CandleLightingMethod(val storageValue: String, val offsetMinutes: Int) {
    Minutes18("minutes_18", 18),
    Minutes20("minutes_20", 20),
    Minutes30("minutes_30", 30),
    Minutes40("minutes_40", 40),
    ;

    companion object {
        fun fromStorageValue(value: String?): CandleLightingMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

enum class MotzeiShabbatMethod(val storageValue: String) {
    Geonim3Point7("geonim_3_7"),
    Geonim3Point8("geonim_3_8"),
    Geonim4Point42("geonim_4_42"),
    Geonim4Point66("geonim_4_66"),
    Geonim4Point8("geonim_4_8"),
    Geonim5Point95("geonim_5_95"),
    Degrees6Point2("degrees_6_2"),
    Geonim6Point45("geonim_6_45"),
    Geonim7Point083("geonim_7_083"),
    Geonim7Point67("geonim_7_67"),
    Geonim8Point5("geonim_8_5"),
    Geonim9Point3("geonim_9_3"),
    Geonim9Point75("geonim_9_75"),
    Degrees16Point1("degrees_16_1"),
    Degrees18("degrees_18"),
    Degrees19Point8("degrees_19_8"),
    Degrees26("degrees_26"),
    Minutes50("minutes_50"),
    Minutes60("minutes_60"),
    Minutes72("minutes_72"),
    Minutes90("minutes_90"),
    Minutes96("minutes_96"),
    Minutes120("minutes_120"),
    Zmanis72("zmanis_72"),
    Zmanis90("zmanis_90"),
    Zmanis96("zmanis_96"),
    Zmanis120("zmanis_120"),
    AteretTorah("ateret_torah"),
    BaalHatanya("baal_hatanya"),
    ;

    companion object {
        fun fromStorageValue(value: String?): MotzeiShabbatMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

enum class RabbeinuTamMethod(val storageValue: String) {
    Degrees16Point1("degrees_16_1"),
    Degrees18("degrees_18"),
    Degrees19Point8("degrees_19_8"),
    Degrees26("degrees_26"),
    Minutes72("minutes_72"),
    Minutes90("minutes_90"),
    Minutes120("minutes_120"),
    Zmanis72("zmanis_72"),
    BainHashmashot13Point24("bain_hashmashot_13_24"),
    BainHashmashot58Point5("bain_hashmashot_58_5"),
    BainHashmashot13Point5Before7Point083("bain_hashmashot_13_5_before_7_083"),
    BainHashmashot2Stars("bain_hashmashot_2_stars"),
    ;

    companion object {
        fun fromStorageValue(value: String?): RabbeinuTamMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

enum class ChametzMethod(val storageValue: String) {
    Mga16Point1("mga_16_1"),
    Mga72("mga_72"),
    Mga72Zmanis("mga_72_zmanis"),
    Gra("gra"),
    BaalHatanya("baal_hatanya"),
    ;

    companion object {
        fun fromStorageValue(value: String?): ChametzMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}