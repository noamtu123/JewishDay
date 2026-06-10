package com.noamtu.jewishday.model

enum class ZmanimPreset(val storageValue: String) {
    Standard("standard"),
    MagenAvraham72("magen_avraham_72"),
    MagenAvraham16Point1("magen_avraham_16_1"),
    RabbeinuTam("rabbeinu_tam"),
    Chabad("chabad"),
    Sephardi("sephardi"),
    Ashkenazi("ashkenazi"),
    Israeli("israeli"),
    Custom("custom"),
    ;

    companion object {
        fun fromStorageValue(value: String?): ZmanimPreset? =
            entries.firstOrNull { it.storageValue == value }
    }
}

enum class HighLatitudeHandling(val storageValue: String) {
    Strict("strict"),
    FixedMinutesFallback("fixed_minutes_fallback"),
    ;

    companion object {
        fun fromStorageValue(value: String?): HighLatitudeHandling? =
            entries.firstOrNull { it.storageValue == value }
    }
}

enum class AlotHashacharMethod(val storageValue: String) {
    Minutes60("minutes_60"),
    Minutes72("minutes_72"),
    Minutes90("minutes_90"),
    Minutes96("minutes_96"),
    Minutes120("minutes_120"),
    Zmanis72("zmanis_72"),
    Zmanis90("zmanis_90"),
    Zmanis96("zmanis_96"),
    Zmanis120("zmanis_120"),
    Degrees16Point1("degrees_16_1"),
    Degrees18("degrees_18"),
    Degrees19("degrees_19"),
    Degrees19Point8("degrees_19_8"),
    Degrees26("degrees_26"),
    BaalHatanya("baal_hatanya"),
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

enum class SofZmanShemaMethod(val storageValue: String) {
    Gra("gra"),
    Mga72("mga_72"),
    Mga72Zmanis("mga_72_zmanis"),
    Mga90("mga_90"),
    Mga90Zmanis("mga_90_zmanis"),
    Mga96("mga_96"),
    Mga96Zmanis("mga_96_zmanis"),
    Mga120("mga_120"),
    Mga16Point1("mga_16_1"),
    Mga18("mga_18"),
    Mga19Point8("mga_19_8"),
    Alos16Point1ToSunset("alos_16_1_to_sunset"),
    Alos16Point1ToTzeit7Point083("alos_16_1_to_tzeit_7_083"),
    ThreeHoursBeforeChatzot("three_hours_before_chatzot"),
    FixedLocal("fixed_local"),
    FixedLocalGra("fixed_local_gra"),
    Mga18ToFixedLocalChatzot("mga_18_to_fixed_local_chatzot"),
    Mga16Point1ToFixedLocalChatzot("mga_16_1_to_fixed_local_chatzot"),
    Mga90ToFixedLocalChatzot("mga_90_to_fixed_local_chatzot"),
    Mga72ToFixedLocalChatzot("mga_72_to_fixed_local_chatzot"),
    BaalHatanya("baal_hatanya"),
    AteretTorah("ateret_torah"),
    KolEliyahu("kol_eliyahu"),
    ;

    companion object {
        fun fromStorageValue(value: String?): SofZmanShemaMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

enum class SofZmanTefillahMethod(val storageValue: String) {
    Gra("gra"),
    Mga72("mga_72"),
    Mga72Zmanis("mga_72_zmanis"),
    Mga90("mga_90"),
    Mga90Zmanis("mga_90_zmanis"),
    Mga96("mga_96"),
    Mga96Zmanis("mga_96_zmanis"),
    Mga120("mga_120"),
    Mga16Point1("mga_16_1"),
    Mga18("mga_18"),
    Mga19Point8("mga_19_8"),
    TwoHoursBeforeChatzot("two_hours_before_chatzot"),
    FixedLocal("fixed_local"),
    FixedLocalGra("fixed_local_gra"),
    BaalHatanya("baal_hatanya"),
    AteretTorah("ateret_torah"),
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
    Standard("standard"),
    ThirtyMinutes("thirty_minutes"),
    GreaterThan30("greater_than_30"),
    Mga72("mga_72"),
    Degrees16Point1("degrees_16_1"),
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
    Standard("standard"),
    Mga72("mga_72"),
    Degrees16Point1("degrees_16_1"),
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
    Gra("gra"),
    Mga60("mga_60"),
    Mga72("mga_72"),
    Mga72Zmanis("mga_72_zmanis"),
    Mga90("mga_90"),
    Mga90Zmanis("mga_90_zmanis"),
    Mga96("mga_96"),
    Mga96Zmanis("mga_96_zmanis"),
    Mga120("mga_120"),
    Mga120Zmanis("mga_120_zmanis"),
    Degrees16Point1("degrees_16_1"),
    Degrees18("degrees_18"),
    Degrees19Point8("degrees_19_8"),
    Degrees26("degrees_26"),
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
    Geonim3Point65("geonim_3_65"),
    Geonim3Point676("geonim_3_676"),
    Geonim4Point37("geonim_4_37"),
    Geonim4Point61("geonim_4_61"),
    Geonim4Point8("geonim_4_8"),
    Geonim5Point88("geonim_5_88"),
    Geonim5Point95("geonim_5_95"),
    Geonim6Point45("geonim_6_45"),
    Geonim7Point083("geonim_7_083"),
    Geonim7Point67("geonim_7_67"),
    Geonim8Point5("geonim_8_5"),
    Geonim9Point3("geonim_9_3"),
    Geonim9Point75("geonim_9_75"),
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
    Degrees16Point1("degrees_16_1"),
    Degrees18("degrees_18"),
    Degrees19Point8("degrees_19_8"),
    Degrees26("degrees_26"),
    BaalHatanya("baal_hatanya"),
    AteretTorah("ateret_torah"),
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
    Geonim5Point88("geonim_5_88"),
    Geonim7Point083("geonim_7_083"),
    Geonim8Point5("geonim_8_5"),
    Geonim9Point3("geonim_9_3"),
    Minutes50("minutes_50"),
    Minutes60("minutes_60"),
    Minutes72("minutes_72"),
    Minutes90("minutes_90"),
    Minutes96("minutes_96"),
    Minutes120("minutes_120"),
    RabbeinuTam72("rabbeinu_tam_72"),
    RabbeinuTam90("rabbeinu_tam_90"),
    RabbeinuTam120("rabbeinu_tam_120"),
    BaalHatanya("baal_hatanya"),
    AteretTorah("ateret_torah"),
    ;

    companion object {
        fun fromStorageValue(value: String?): MotzeiShabbatMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

enum class RabbeinuTamMethod(val storageValue: String) {
    Minutes72("minutes_72"),
    Minutes90("minutes_90"),
    Minutes120("minutes_120"),
    Zmanis72("zmanis_72"),
    Degrees16Point1("degrees_16_1"),
    Degrees18("degrees_18"),
    Degrees19Point8("degrees_19_8"),
    Degrees26("degrees_26"),
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

enum class BainHashmashotMethod(val storageValue: String) {
    RabbeinuTam13Point24("rabbeinu_tam_13_24"),
    RabbeinuTam58Point5("rabbeinu_tam_58_5"),
    RabbeinuTam13Point5Before7Point083("rabbeinu_tam_13_5_before_7_083"),
    RabbeinuTam2Stars("rabbeinu_tam_2_stars"),
    Yereim18Minutes("yereim_18_minutes"),
    Yereim3Point05("yereim_3_05"),
    Yereim16Point875Minutes("yereim_16_875_minutes"),
    Yereim2Point8("yereim_2_8"),
    Yereim13Point5Minutes("yereim_13_5_minutes"),
    Yereim2Point1("yereim_2_1"),
    ;

    companion object {
        fun fromStorageValue(value: String?): BainHashmashotMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

enum class FastDayMethod(val storageValue: String) {
    Alot72ToTzeit8Point5("alot_72_to_tzeit_8_5"),
    Alot72ToTzeit7Point083("alot_72_to_tzeit_7_083"),
    Alot72ToTzeit5Point88("alot_72_to_tzeit_5_88"),
    Alot16Point1ToTzeit8Point5("alot_16_1_to_tzeit_8_5"),
    Alot16Point1ToTzeit7Point083("alot_16_1_to_tzeit_7_083"),
    BaalHatanya("baal_hatanya"),
    ;

    companion object {
        fun fromStorageValue(value: String?): FastDayMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

enum class ChametzMethod(val storageValue: String) {
    Gra("gra"),
    Mga72("mga_72"),
    Mga16Point1("mga_16_1"),
    BaalHatanya("baal_hatanya"),
    ;

    companion object {
        fun fromStorageValue(value: String?): ChametzMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}
