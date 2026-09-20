// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

// Each zman used to offer a fixed ladder of degrees, fixed minutes and zmaniyot minutes — two dozen
// rungs for Alot Hashachar alone, and never the one rung somebody's own posek holds. The ladders are
// gone: what is left is the app's default, the opinions a typed-in number genuinely cannot express —
// the ones that define the day asymmetrically or by something other than an offset (the GRA, Ateret
// Torah, Ahavat Shalom, Baal Hatanya's own day, Bein Hashmashot, the fixed-local-chatzot spans) — and
// a custom option per unit, whose number the user types in. An option that was *only* a degree value
// is gone with the ladders: the Baal Hatanya's dawn was 16.9° and his nightfall 6°, so the name added
// nothing the number does not say. The value each one carries lives beside the
// method in [ZmanimCalculationSettings] — see [CustomZmanValue].
//
// Method options are still ordered for display: degrees, then minutes, then zmaniyot minutes, then
// special/named methods last.

enum class AlotHashacharMethod(val storageValue: String) {
    Degrees16Point1("degrees_16_1"),
    CustomDegrees("custom_degrees"),
    CustomMinutes("custom_minutes"),
    CustomZmaniyotMinutes("custom_zmaniyot"),
    ;

    companion object {
        fun fromStorageValue(value: String?): AlotHashacharMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

enum class MisheyakirMethod(val storageValue: String) {
    Degrees11("degrees_11"),
    CustomDegrees("custom_degrees"),
    CustomMinutesBeforeSunrise("custom_minutes_before_sunrise"),
    CustomZmaniyotMinutesBeforeSunrise("custom_zmaniyot_before_sunrise"),
    Minutes6AfterAlos("minutes_6_after_alos"),
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
    CustomDegrees("custom_degrees", ZmanOpinionFamily.MagenAvraham),
    CustomMinutes("custom_minutes", ZmanOpinionFamily.MagenAvraham),
    CustomZmaniyotMinutes("custom_zmaniyot", ZmanOpinionFamily.MagenAvraham),
    CustomDegreesToFixedLocalChatzot("custom_degrees_to_fixed_local_chatzot", ZmanOpinionFamily.MagenAvraham),
    CustomMinutesToFixedLocalChatzot("custom_minutes_to_fixed_local_chatzot", ZmanOpinionFamily.MagenAvraham),
    Alos16Point1ToSunset("alos_16_1_to_sunset", ZmanOpinionFamily.MagenAvraham),
    Alos16Point1ToTzeit7Point083("alos_16_1_to_tzeit_7_083", ZmanOpinionFamily.MagenAvraham),
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
    CustomDegrees("custom_degrees", ZmanOpinionFamily.MagenAvraham),
    CustomMinutes("custom_minutes", ZmanOpinionFamily.MagenAvraham),
    CustomZmaniyotMinutes("custom_zmaniyot", ZmanOpinionFamily.MagenAvraham),
    Alos16Point1ToSunset("alos_16_1_to_sunset", ZmanOpinionFamily.MagenAvraham),
    Alos16Point1ToTzeit7Point083("alos_16_1_to_tzeit_7_083", ZmanOpinionFamily.MagenAvraham),
    AteretTorah("ateret_torah", ZmanOpinionFamily.MagenAvraham),
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
    CustomDegrees("custom_degrees"),
    CustomMinutes("custom_minutes"),
    CustomZmaniyotMinutes("custom_zmaniyot"),
    ThirtyMinutes("thirty_minutes"),
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
    Standard("standard"),
    CustomDegrees("custom_degrees"),
    CustomMinutes("custom_minutes"),
    CustomZmaniyotMinutes("custom_zmaniyot"),
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
    CustomDegrees("custom_degrees"),
    CustomMinutes("custom_minutes"),
    CustomZmaniyotMinutes("custom_zmaniyot"),
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
    Degrees6Point2("degrees_6_2"),
    CustomDegrees("custom_degrees"),
    CustomMinutes("custom_minutes"),
    CustomZmaniyotMinutes("custom_zmaniyot"),
    ;

    companion object {
        fun fromStorageValue(value: String?): TzeitHakochavimMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

/**
 * Minutes before sunset, as a choice of minhag. The three fixed options are the practices common
 * enough to be worth a tap — they are communities' customs, not rungs of a calculation ladder — and
 * [Custom] covers any other number, 18 included, held in
 * [ZmanimCalculationSettings.candleLightingCustomMinutes]. [offsetMinutes] is null for [Custom]; read
 * the offset through [ZmanimCalculationSettings.candleLightingOffsetMinutes], which resolves both.
 */
enum class CandleLightingMethod(val storageValue: String, val offsetMinutes: Int?) {
    Minutes20("minutes_20", 20),
    Minutes30("minutes_30", 30),
    Minutes40("minutes_40", 40),
    Custom("custom_minutes", null),
    ;

    companion object {
        fun fromStorageValue(value: String?): CandleLightingMethod? =
            entries.firstOrNull { it.storageValue == value }

        /** The options the first-launch prompt offers: the common minhagim, not a typed-in value. */
        val PromptOptions: List<CandleLightingMethod> = entries.filter { it.offsetMinutes != null }
    }
}

enum class MotzeiShabbatMethod(val storageValue: String) {
    Degrees6Point2("degrees_6_2"),
    CustomDegrees("custom_degrees"),
    CustomMinutes("custom_minutes"),
    CustomZmaniyotMinutes("custom_zmaniyot"),
    ;

    companion object {
        fun fromStorageValue(value: String?): MotzeiShabbatMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

enum class RabbeinuTamMethod(val storageValue: String) {
    CustomDegrees("custom_degrees"),
    Minutes72("minutes_72"),
    CustomMinutes("custom_minutes"),
    CustomZmaniyotMinutes("custom_zmaniyot"),
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
    Gra("gra"),
    CustomDegrees("custom_degrees"),
    CustomMinutes("custom_minutes"),
    CustomZmaniyotMinutes("custom_zmaniyot"),
    BaalHatanya("baal_hatanya"),
    ;

    companion object {
        fun fromStorageValue(value: String?): ChametzMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}
