// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

import androidx.compose.runtime.Immutable

/**
 * Which unit a custom method option is entered in. The three are what the old fixed ladders were
 * made of: a depression of the sun below the horizon, a number of clock minutes, or a number of
 * *zmaniyot* minutes — sixtieths of that day's own halachic hour, so they stretch with the seasons.
 */
public enum class CustomZmanUnit { Degrees, Minutes, ZmaniyotMinutes }

/**
 * The numbers behind one zman's custom options. All three are kept, not just the one in use, so
 * switching between "16.4°" and "74 minutes" and back does not lose what was typed either time.
 */
@Immutable
public data class CustomZmanValue(
    val degrees: Double,
    val minutes: Int,
    val zmaniyotMinutes: Int,
) {
    /**
     * [value] in [unit], taken as given: what anyone holds is their own choice. A degree value is
     * entered to three decimals (19.848° is a real opinion); the minute ones are whole minutes, the
     * only precision these opinions are stated in. Only what cannot be a number at all is refused
     * (the old value stays), and a minute count past what an Int holds stops there; an angle the sun
     * never reaches simply gives no time, as on a far-north day.
     */
    public fun withValue(unit: CustomZmanUnit, value: Double): CustomZmanValue = when {
        value.isNaN() -> this
        unit == CustomZmanUnit.Degrees -> if (value.isFinite()) copy(degrees = value) else this
        // Double.toInt() saturates at Int.MIN_VALUE / Int.MAX_VALUE rather than overflowing.
        unit == CustomZmanUnit.Minutes -> copy(minutes = value.toInt())
        else -> copy(zmaniyotMinutes = value.toInt())
    }

    public fun value(unit: CustomZmanUnit): Double = when (unit) {
        CustomZmanUnit.Degrees -> degrees
        CustomZmanUnit.Minutes -> minutes.toDouble()
        CustomZmanUnit.ZmaniyotMinutes -> zmaniyotMinutes.toDouble()
    }
}

/**
 * Starting points for a zman's custom options: whatever that zman's own default opinion is, so the
 * first thing the entry field offers is the number already in use rather than an arbitrary one.
 */
internal fun customValuesAround(degrees: Double, minutes: Int): CustomZmanValue =
    CustomZmanValue(degrees = degrees, minutes = minutes, zmaniyotMinutes = minutes)

@Immutable
public data class ZmanimCalculationSettings(
    // Sunrise and sunset carry the only elevation choice there is: every zman measured from them
    // follows it, including the GRA-based ones, whose day *is* sunrise to sunset. There is no
    // separate "use elevation" switch — that asked the same question a second time, and answered it
    // for only some of the rows.
    val alotHashacharMethod: AlotHashacharMethod = AlotHashacharMethod.Degrees16Point1,
    val alotHashacharCustom: CustomZmanValue = customValuesAround(degrees = 16.1, minutes = 72),
    val misheyakirMethod: MisheyakirMethod = MisheyakirMethod.Degrees11,
    val misheyakirCustom: CustomZmanValue = customValuesAround(degrees = 11.0, minutes = 45),
    val sunriseMethod: SunriseMethod = SunriseMethod.SeaLevel,
    // Sof Zman Shema / Tefillah each show a GRA row and a Magen Avraham row. Each has its
    // own configurable method, and each picker only offers options from its own family.
    val sofZmanShemaGraMethod: SofZmanShemaMethod = SofZmanShemaMethod.Gra,
    val sofZmanShemaMethod: SofZmanShemaMethod = SofZmanShemaMethod.Mga16Point1,
    val sofZmanShemaCustom: CustomZmanValue = customValuesAround(degrees = 16.1, minutes = 72),
    val sofZmanTefillahGraMethod: SofZmanTefillahMethod = SofZmanTefillahMethod.Gra,
    val sofZmanTefillahMethod: SofZmanTefillahMethod = SofZmanTefillahMethod.Mga16Point1,
    val sofZmanTefillahCustom: CustomZmanValue = customValuesAround(degrees = 16.1, minutes = 72),
    val chatzotMethod: ChatzotMethod = ChatzotMethod.Solar,
    val chatzotHaLailaMethod: ChatzotMethod = ChatzotMethod.Solar,
    val minchaGedolaMethod: MinchaGedolaMethod = MinchaGedolaMethod.Standard,
    val minchaGedolaCustom: CustomZmanValue = customValuesAround(degrees = 16.1, minutes = 72),
    val minchaKetanaMethod: MinchaKetanaMethod = MinchaKetanaMethod.Standard,
    val minchaKetanaCustom: CustomZmanValue = customValuesAround(degrees = 16.1, minutes = 72),
    val plagHaminchaMethod: PlagHaminchaMethod = PlagHaminchaMethod.Gra,
    val plagHaminchaCustom: CustomZmanValue = customValuesAround(degrees = 16.1, minutes = 72),
    val sunsetMethod: SunsetMethod = SunsetMethod.SeaLevel,
    val tzeitHakochavimMethod: TzeitHakochavimMethod = TzeitHakochavimMethod.Degrees6Point2,
    val tzeitHakochavimCustom: CustomZmanValue = customValuesAround(degrees = 6.2, minutes = 50),
    // What stands in for tzeit on a night that has none (see NoTzeitFallback). Only ever read there.
    val noTzeitFallback: NoTzeitFallback = NoTzeitFallback.Minutes72AfterSunset,
    val candleLightingMethod: CandleLightingMethod = CandleLightingMethod.Minutes20,
    // Where the custom field starts: 18 minutes, the common practice that is not one of the three
    // fixed options, so the most likely reason to reach for it is one edit away.
    val candleLightingCustomMinutes: Int = 18,
    val motzeiShabbatMethod: MotzeiShabbatMethod = MotzeiShabbatMethod.Degrees6Point2,
    val motzeiShabbatCustom: CustomZmanValue = customValuesAround(degrees = 6.2, minutes = 50),
    val rabbeinuTamMethod: RabbeinuTamMethod = RabbeinuTamMethod.Minutes72,
    val rabbeinuTamCustom: CustomZmanValue = customValuesAround(degrees = 16.1, minutes = 72),
    val chametzMethod: ChametzMethod = ChametzMethod.Gra,
    val chametzCustom: CustomZmanValue = customValuesAround(degrees = 16.1, minutes = 72),
    // Minutes added after motzei when leaving a holy day (tosefet Shabbat/Yom Tov). Ordinary
    // fasts never get it — they end at plain tzeit.
    val holyDayTosefetMinutes: Int = 5,
    val ateretTorahSunsetOffsetMinutes: Int = 40,
) {
    /**
     * Minutes before sunset for candle lighting, whether from a minhag option or a typed-in value;
     * null when it is at plag hamincha, which is no fixed number of minutes.
     */
    val candleLightingOffsetMinutes: Int?
        get() = when (candleLightingMethod) {
            CandleLightingMethod.AtPlag -> null
            CandleLightingMethod.Custom -> candleLightingCustomMinutes
            else -> candleLightingMethod.offsetMinutes
        }
}
