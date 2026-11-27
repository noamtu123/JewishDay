// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

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

public enum class AlotHashacharMethod(public val storageValue: String) {
    Degrees16Point1("degrees_16_1"),
    CustomDegrees("custom_degrees"),
    CustomMinutes("custom_minutes"),
    CustomZmaniyotMinutes("custom_zmaniyot"),
    ;

    public companion object {
        public fun fromStorageValue(value: String?): AlotHashacharMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

public enum class MisheyakirMethod(public val storageValue: String) {
    Degrees11("degrees_11"),
    CustomDegrees("custom_degrees"),
    CustomMinutesBeforeSunrise("custom_minutes_before_sunrise"),
    CustomZmaniyotMinutesBeforeSunrise("custom_zmaniyot_before_sunrise"),
    Minutes6AfterAlos("minutes_6_after_alos"),
    ;

    public companion object {
        public fun fromStorageValue(value: String?): MisheyakirMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

public enum class SunriseMethod(public val storageValue: String) {
    SeaLevel("sea_level"),
    ElevationAdjusted("elevation_adjusted"),
    ;

    public companion object {
        public fun fromStorageValue(value: String?): SunriseMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

/**
 * Which halachic school a Sof Zman Shema / Tefillah opinion belongs to. The Zmanim
 * list shows a GRA row and a Magen Avraham row separately, and each row's method
 * picker only offers options from its own family.
 */
public enum class ZmanOpinionFamily { Gra, MagenAvraham }

public enum class SofZmanShemaMethod(public val storageValue: String, public val family: ZmanOpinionFamily) {
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

    public companion object {
        public fun fromStorageValue(value: String?): SofZmanShemaMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

public enum class SofZmanTefillahMethod(public val storageValue: String, public val family: ZmanOpinionFamily) {
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

    public companion object {
        public fun fromStorageValue(value: String?): SofZmanTefillahMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

public enum class ChatzotMethod(public val storageValue: String) {
    Solar("solar"),
    FixedLocal("fixed_local"),
    ;

    public companion object {
        public fun fromStorageValue(value: String?): ChatzotMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

public enum class MinchaGedolaMethod(public val storageValue: String) {
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

    public companion object {
        public fun fromStorageValue(value: String?): MinchaGedolaMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

public enum class MinchaKetanaMethod(public val storageValue: String) {
    Standard("standard"),
    CustomDegrees("custom_degrees"),
    CustomMinutes("custom_minutes"),
    CustomZmaniyotMinutes("custom_zmaniyot"),
    FixedLocal("fixed_local"),
    BaalHatanya("baal_hatanya"),
    AteretTorah("ateret_torah"),
    AhavatShalom("ahavat_shalom"),
    ;

    public companion object {
        public fun fromStorageValue(value: String?): MinchaKetanaMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

public enum class PlagHaminchaMethod(public val storageValue: String) {
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

    public companion object {
        public fun fromStorageValue(value: String?): PlagHaminchaMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

public enum class SunsetMethod(public val storageValue: String) {
    SeaLevel("sea_level"),
    ElevationAdjusted("elevation_adjusted"),
    ;

    public companion object {
        public fun fromStorageValue(value: String?): SunsetMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

public enum class TzeitHakochavimMethod(public val storageValue: String) {
    Degrees6Point2("degrees_6_2"),
    CustomDegrees("custom_degrees"),
    CustomMinutes("custom_minutes"),
    CustomZmaniyotMinutes("custom_zmaniyot"),
    ;

    public companion object {
        public fun fromStorageValue(value: String?): TzeitHakochavimMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

/**
 * What stands in for tzeit hakochavim on a night it never comes: far enough north or south in summer
 * the sun sets but never sinks to the user's nightfall angle, or does not set at all. Used only on
 * such a night; wherever the user's own tzeit exists it is never consulted.
 *
 * Plain civil midnight is deliberately absent: no posek gives it.
 */
public enum class NoTzeitFallback(public val storageValue: String) {
    /**
     * 72 minutes after the user's own sunset (sea level or observed). Peninei Halakha (1:3, expanded,
     * א,כה): in the northern countries the custom is to set tzeit all year at 72 minutes after sunset,
     * and people relied on it for melacha on motzei Shabbat — he names the "white nights" outright.
     * On a day with no sunset at all there is nothing to count from, and chatzot halaila is used.
     */
    Minutes72AfterSunset("minutes_72_after_sunset"),

    /**
     * Solar midnight of that night, the darkest moment it has. Rav Shlomo Goren: where there is no
     * nightfall, Shabbat comes in and goes out at chatzot halaila.
     */
    ChatzotHaLaila("chatzot_halaila"),

    /**
     * The moment tzeit comes in Jerusalem that civil date, by the user's own method, shown in local
     * time. Divrei Yatziv: in such places Eretz Yisrael decides.
     */
    EretzYisrael("eretz_yisrael"),
    ;

    public companion object {
        public fun fromStorageValue(value: String?): NoTzeitFallback? =
            entries.firstOrNull { it.storageValue == value }
    }
}

/**
 * Minutes before sunset, as a choice of minhag. The three fixed options are the practices common
 * enough to be worth a tap — they are communities' customs, not rungs of a calculation ladder — and
 * [Custom] covers any other number, 18 included, held in
 * [ZmanimCalculationSettings.candleLightingCustomMinutes]. [AtPlag] brings Shabbat in as early as it
 * can be, at the user's own plag hamincha. [offsetMinutes] is null for both; read the offset through
 * [ZmanimCalculationSettings.candleLightingOffsetMinutes], which resolves them (null for [AtPlag]).
 */
public enum class CandleLightingMethod(public val storageValue: String, public val offsetMinutes: Int?) {
    Minutes20("minutes_20", 20),
    Minutes30("minutes_30", 30),
    Minutes40("minutes_40", 40),
    // A storage value of its own, which no older build ever wrote.
    AtPlag("plag_hamincha", null),
    Custom("custom_minutes", null),
    ;

    public companion object {
        public fun fromStorageValue(value: String?): CandleLightingMethod? =
            entries.firstOrNull { it.storageValue == value }

        /** The options the first-launch prompt offers: the common minhagim, not a typed-in value. */
        public val PromptOptions: List<CandleLightingMethod> = entries.filter { it.offsetMinutes != null }
    }
}

public enum class MotzeiShabbatMethod(public val storageValue: String) {
    Degrees6Point2("degrees_6_2"),
    CustomDegrees("custom_degrees"),
    CustomMinutes("custom_minutes"),
    CustomZmaniyotMinutes("custom_zmaniyot"),
    ;

    public companion object {
        public fun fromStorageValue(value: String?): MotzeiShabbatMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

public enum class RabbeinuTamMethod(public val storageValue: String) {
    CustomDegrees("custom_degrees"),
    Minutes72("minutes_72"),
    CustomMinutes("custom_minutes"),
    CustomZmaniyotMinutes("custom_zmaniyot"),
    BainHashmashot58Point5("bain_hashmashot_58_5"),
    BainHashmashot13Point5Before7Point083("bain_hashmashot_13_5_before_7_083"),
    BainHashmashot2Stars("bain_hashmashot_2_stars"),
    ;

    public companion object {
        public fun fromStorageValue(value: String?): RabbeinuTamMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}

public enum class ChametzMethod(public val storageValue: String) {
    Gra("gra"),
    CustomDegrees("custom_degrees"),
    CustomMinutes("custom_minutes"),
    CustomZmaniyotMinutes("custom_zmaniyot"),
    BaalHatanya("baal_hatanya"),
    ;

    public companion object {
        public fun fromStorageValue(value: String?): ChametzMethod? =
            entries.firstOrNull { it.storageValue == value }
    }
}
