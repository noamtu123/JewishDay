// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

/**
 * Which unit a custom method option is entered in. The three are what the old fixed ladders were
 * made of: a depression of the sun below the horizon, a number of clock minutes, or a number of
 * *zmaniyot* minutes — sixtieths of that day's own halachic hour, so they stretch with the seasons.
 */
enum class CustomZmanUnit { Degrees, Minutes, ZmaniyotMinutes }

/**
 * The numbers behind one zman's custom options. All three are kept, not just the one in use, so
 * switching between "16.4°" and "74 minutes" and back does not lose what was typed either time.
 */
data class CustomZmanValue(
    val degrees: Double,
    val minutes: Int,
    val zmaniyotMinutes: Int,
) {
    fun withValue(unit: CustomZmanUnit, value: Double): CustomZmanValue = when (unit) {
        // A degree value is entered to three decimals (19.848° is a real opinion); the minute ones
        // are whole minutes, which is the only precision any of these opinions is stated in.
        CustomZmanUnit.Degrees -> copy(degrees = value.coerceIn(0.0, 90.0))
        CustomZmanUnit.Minutes -> copy(minutes = value.toInt().coerceIn(0, 300))
        CustomZmanUnit.ZmaniyotMinutes -> copy(zmaniyotMinutes = value.toInt().coerceIn(0, 300))
    }

    fun value(unit: CustomZmanUnit): Double = when (unit) {
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

data class ZmanimCalculationSettings(
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
    /** Minutes before sunset for candle lighting, whether from a minhag option or a typed-in value. */
    val candleLightingOffsetMinutes: Int
        get() = candleLightingMethod.offsetMinutes ?: candleLightingCustomMinutes
}
