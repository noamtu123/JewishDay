// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import com.kosherjava.zmanim.AstronomicalCalendar
import com.kosherjava.zmanim.ComprehensiveZmanimCalendar
import java.time.Duration
import java.time.Instant
import java.time.LocalTime

// ---------------------------------------------------------------------------------------------
// Custom-value primitives
//
// The method pickers no longer offer a ladder of degrees, minutes and zmaniyot minutes: the user
// types the number. KosherJava has a named getter per rung of the old ladders and nothing for an
// arbitrary value, so these four build the same things from first principles — the identical
// arithmetic the library's own getters do, with the number coming from settings.
// ---------------------------------------------------------------------------------------------

/** A degree-based dawn: how long before sunrise the sun sits [degrees] below the horizon. */
private fun ComprehensiveZmanimCalendar.sunriseByDegrees(degrees: Double): Instant? =
    getSunriseOffsetByDegrees(AstronomicalCalendar.GEOMETRIC_ZENITH + degrees)

/** The same after sunset, for nightfall opinions. */
private fun ComprehensiveZmanimCalendar.sunsetByDegrees(degrees: Double): Instant? =
    getSunsetOffsetByDegrees(AstronomicalCalendar.GEOMETRIC_ZENITH + degrees)

/**
 * A number of *zmaniyot* minutes as a duration: sixtieths of this day's GRA halachic hour, so 72 of
 * them run longer than 72 clock minutes in summer and shorter in winter. Null where the day has no
 * length to divide, as inside the arctic circle.
 */
private fun ComprehensiveZmanimCalendar.zmaniyotOffset(minutes: Int): Duration? =
    shaahZmanisGRA?.multipliedBy(minutes.toLong())?.dividedBy(60L)

private fun ComprehensiveZmanimCalendar.zmaniyotBeforeSunrise(minutes: Int): Instant? =
    zmaniyotOffset(minutes)?.let { AstronomicalCalendar.getTimeOffset(seaLevelSunrise, it.negated()) }

private fun ComprehensiveZmanimCalendar.zmaniyotAfterSunset(minutes: Int): Instant? =
    zmaniyotOffset(minutes)?.let { AstronomicalCalendar.getTimeOffset(seaLevelSunset, it) }

private fun ComprehensiveZmanimCalendar.minutesBeforeSunrise(minutes: Int): Instant? =
    AstronomicalCalendar.getTimeOffset(seaLevelSunrise, Duration.ofMinutes(-minutes.toLong()))

private fun ComprehensiveZmanimCalendar.minutesAfterSunset(minutes: Int): Instant? =
    AstronomicalCalendar.getTimeOffset(seaLevelSunset, Duration.ofMinutes(minutes.toLong()))

/**
 * The dawn a custom [unit]/[values] pair defines, which is the start of the halachic day for every
 * Magen Avraham-style opinion below.
 */
private fun ComprehensiveZmanimCalendar.customDayStart(unit: CustomZmanUnit, values: CustomZmanValue): Instant? =
    when (unit) {
        CustomZmanUnit.Degrees -> sunriseByDegrees(values.degrees)
        CustomZmanUnit.Minutes -> minutesBeforeSunrise(values.minutes)
        CustomZmanUnit.ZmaniyotMinutes -> zmaniyotBeforeSunrise(values.zmaniyotMinutes)
    }

/** Its mirror image at the other end of the day: the nightfall the same opinion ends at. */
private fun ComprehensiveZmanimCalendar.customDayEnd(unit: CustomZmanUnit, values: CustomZmanValue): Instant? =
    when (unit) {
        CustomZmanUnit.Degrees -> sunsetByDegrees(values.degrees)
        CustomZmanUnit.Minutes -> minutesAfterSunset(values.minutes)
        CustomZmanUnit.ZmaniyotMinutes -> zmaniyotAfterSunset(values.zmaniyotMinutes)
    }

/**
 * A zman of the GRA's day, which runs from sunrise to sunset — the very sunrise and sunset the user
 * picked, so the one elevation choice in the app reaches these rows too. [zman] is the library's own
 * (startOfDay, endOfDay) arithmetic; only the endpoints are ours.
 */
private inline fun ComprehensiveZmanimCalendar.graDay(
    settings: ZmanimCalculationSettings,
    zman: (Instant, Instant) -> Instant?,
): Instant? {
    val start = sunrise(settings.sunriseMethod) ?: return null
    val end = sunset(settings.sunsetMethod) ?: return null
    return zman(start, end)
}

/** Of two times, the later — or whichever one exists. */
private fun laterOf(first: Instant?, second: Instant?): Instant? = when {
    first == null -> second
    second == null -> first
    else -> maxOf(first, second)
}

internal fun ComprehensiveZmanimCalendar.alotHashachar(settings: ZmanimCalculationSettings): Instant? =
    when (settings.alotHashacharMethod) {
        AlotHashacharMethod.Degrees16Point1 -> alos16Point1Degrees ?: alos72Minutes
        AlotHashacharMethod.CustomDegrees -> sunriseByDegrees(settings.alotHashacharCustom.degrees)
        AlotHashacharMethod.CustomMinutes -> minutesBeforeSunrise(settings.alotHashacharCustom.minutes)
        AlotHashacharMethod.CustomZmaniyotMinutes -> zmaniyotBeforeSunrise(settings.alotHashacharCustom.zmaniyotMinutes)
    }

internal fun ComprehensiveZmanimCalendar.misheyakir(settings: ZmanimCalculationSettings): Instant? {
    val custom = settings.misheyakirCustom
    return when (settings.misheyakirMethod) {
        MisheyakirMethod.Degrees11 -> misheyakir11Degrees
        MisheyakirMethod.CustomDegrees -> sunriseByDegrees(custom.degrees)
        // Measured from the sunrise the user chose, since this is the one option stated as an offset
        // from sunrise itself rather than as a position of the sun.
        MisheyakirMethod.CustomMinutesBeforeSunrise -> sunrise(settings.sunriseMethod)
            ?.let { AstronomicalCalendar.getTimeOffset(it, Duration.ofMinutes(-custom.minutes.toLong())) }
        MisheyakirMethod.CustomZmaniyotMinutesBeforeSunrise -> zmaniyotBeforeSunrise(custom.zmaniyotMinutes)
        MisheyakirMethod.Minutes6AfterAlos -> alotHashachar(settings)
            ?.let { AstronomicalCalendar.getTimeOffset(it, Duration.ofMinutes(6)) }
    }
}

internal fun ComprehensiveZmanimCalendar.sunrise(method: SunriseMethod): Instant? = when (method) {
    SunriseMethod.SeaLevel -> seaLevelSunrise
    SunriseMethod.ElevationAdjusted -> sunrise
}

internal fun ComprehensiveZmanimCalendar.sunset(method: SunsetMethod): Instant? = when (method) {
    SunsetMethod.SeaLevel -> seaLevelSunset
    SunsetMethod.ElevationAdjusted -> sunset
}

internal fun ComprehensiveZmanimCalendar.sofZmanShema(
    method: SofZmanShemaMethod,
    settings: ZmanimCalculationSettings,
): Instant? {
    val custom = settings.sofZmanShemaCustom
    /** Three of the twelve hours of a day that runs [start] to [end]. */
    fun mga(unit: CustomZmanUnit): Instant? {
        val start = customDayStart(unit, custom) ?: return null
        val end = customDayEnd(unit, custom) ?: return null
        return getSofZmanShma(start, end)
    }
    // A day that runs from dawn to fixed-local chatzot is only half a day, so it is divided into six
    // — which is what the library's own ...ToFixedLocalChatzos getters do.
    fun toFixedLocalChatzot(unit: CustomZmanUnit): Instant? {
        val start = customDayStart(unit, custom) ?: return null
        return getHalfDayBasedZman(start, fixedLocalChatzosHayom, 3.0)
    }
    return when (method) {
        // The GRA's day is sunrise to sunset, so it is the sunrise and sunset the user chose — which
        // is also the only place elevation is decided. Computed from those rather than through the
        // library's global isUseElevation flag, which could not express one of the two being at sea
        // level and the other observed.
        SofZmanShemaMethod.Gra -> graDay(settings) { start, end -> getSofZmanShma(start, end) }
        SofZmanShemaMethod.FixedLocalGra ->
            sunrise(settings.sunriseMethod)?.let { getHalfDayBasedZman(it, fixedLocalChatzosHayom, 3.0) }
        SofZmanShemaMethod.Mga16Point1 -> sofZmanShmaMGA16Point1Degrees ?: sofZmanShmaMGA72Minutes
        SofZmanShemaMethod.CustomDegrees -> mga(CustomZmanUnit.Degrees)
        SofZmanShemaMethod.CustomMinutes -> mga(CustomZmanUnit.Minutes)
        SofZmanShemaMethod.CustomZmaniyotMinutes -> mga(CustomZmanUnit.ZmaniyotMinutes)
        SofZmanShemaMethod.CustomDegreesToFixedLocalChatzot -> toFixedLocalChatzot(CustomZmanUnit.Degrees)
        SofZmanShemaMethod.CustomMinutesToFixedLocalChatzot -> toFixedLocalChatzot(CustomZmanUnit.Minutes)
        SofZmanShemaMethod.Alos16Point1ToSunset -> sofZmanShmaAlos16Point1ToSunset ?: sofZmanShmaMGA72Minutes
        SofZmanShemaMethod.Alos16Point1ToTzeit7Point083 -> sofZmanShmaAlos16Point1DegreesToTzaisGeonim7Point083Degrees ?: sofZmanShmaMGA72Minutes
        SofZmanShemaMethod.AteretTorah -> sofZmanShmaAteretTorah
    }
}

internal fun ComprehensiveZmanimCalendar.sofZmanTefillah(
    method: SofZmanTefillahMethod,
    settings: ZmanimCalculationSettings,
): Instant? {
    val custom = settings.sofZmanTefillahCustom
    /** Four of the twelve hours of the day the custom [unit] defines. */
    fun mga(unit: CustomZmanUnit): Instant? {
        val start = customDayStart(unit, custom) ?: return null
        val end = customDayEnd(unit, custom) ?: return null
        return getSofZmanTfila(start, end)
    }
    fun mgaFrom(alos: Instant?, tzais: Instant?): Instant? {
        if (alos == null || tzais == null) return null
        return getSofZmanTfila(alos, tzais)
    }
    return when (method) {
        SofZmanTefillahMethod.Gra -> graDay(settings) { start, end -> getSofZmanTfila(start, end) }
        SofZmanTefillahMethod.FixedLocalGra ->
            sunrise(settings.sunriseMethod)?.let { getHalfDayBasedZman(it, fixedLocalChatzosHayom, 4.0) }
        SofZmanTefillahMethod.Mga16Point1 -> sofZmanTfilaMGA16Point1Degrees ?: sofZmanTfilaMGA72Minutes
        SofZmanTefillahMethod.CustomDegrees -> mga(CustomZmanUnit.Degrees)
        SofZmanTefillahMethod.CustomMinutes -> mga(CustomZmanUnit.Minutes)
        SofZmanTefillahMethod.CustomZmaniyotMinutes -> mga(CustomZmanUnit.ZmaniyotMinutes)
        SofZmanTefillahMethod.Alos16Point1ToSunset -> mgaFrom(alos16Point1Degrees, seaLevelSunset) ?: sofZmanTfilaMGA72Minutes
        SofZmanTefillahMethod.Alos16Point1ToTzeit7Point083 -> mgaFrom(alos16Point1Degrees, tzaisGeonim7Point083Degrees) ?: sofZmanTfilaMGA72Minutes
        SofZmanTefillahMethod.AteretTorah -> sofZmanTfilaAteretTorah
    }
}

internal fun ComprehensiveZmanimCalendar.chatzot(method: ChatzotMethod): Instant? = when (method) {
    ChatzotMethod.Solar -> chatzosHayom
    ChatzotMethod.FixedLocal -> fixedLocalChatzosHayom
}

// Midnight counterpart of chatzot: solar midnight, or 12h past fixed-local midday. Taken from local
// mean noon itself rather than fixedLocalChatzosHayom, which is null on a polar day too short to
// contain it — a reason to have no midday zman, not to have no midnight.
internal fun ComprehensiveZmanimCalendar.chatzotHaLaila(method: ChatzotMethod): Instant? = when (method) {
    ChatzotMethod.Solar -> solarMidnight
    ChatzotMethod.FixedLocal -> AstronomicalCalendar.getTimeOffset(getLocalMeanTime(LocalTime.NOON), Duration.ofHours(12))
}

internal fun ComprehensiveZmanimCalendar.minchaGedola(settings: ZmanimCalculationSettings): Instant? {
    val values = settings.minchaGedolaCustom
    fun custom(unit: CustomZmanUnit): Instant? {
        val start = customDayStart(unit, values) ?: return null
        val end = customDayEnd(unit, values) ?: return null
        return getMinchaGedola(start, end)
    }
    return when (settings.minchaGedolaMethod) {
        MinchaGedolaMethod.Standard -> graDay(settings) { start, end -> getMinchaGedola(start, end) }
        MinchaGedolaMethod.CustomDegrees -> custom(CustomZmanUnit.Degrees)
        MinchaGedolaMethod.CustomMinutes -> custom(CustomZmanUnit.Minutes)
        MinchaGedolaMethod.CustomZmaniyotMinutes -> custom(CustomZmanUnit.ZmaniyotMinutes)
        MinchaGedolaMethod.ThirtyMinutes -> minchaGedola30Minutes
        // In winter half a shaah zmanis is under 30 clock minutes, and this opinion takes whichever
        // is later. Computed here so the GRA half of it uses the chosen sunrise and sunset.
        MinchaGedolaMethod.GreaterThan30 -> laterOf(
            graDay(settings) { start, end -> getMinchaGedola(start, end) },
            minchaGedola30Minutes,
        )
        MinchaGedolaMethod.FixedLocal -> minchaGedolaGRAFixedLocalChatzos30Minutes
        MinchaGedolaMethod.BaalHatanya -> minchaGedolaBaalHatanya
        MinchaGedolaMethod.BaalHatanyaGreaterThan30 -> getMinchaGedolaGreaterThan30(minchaGedolaBaalHatanya)
        MinchaGedolaMethod.AteretTorah -> minchaGedolaAteretTorah
        MinchaGedolaMethod.AhavatShalom -> minchaGedolaAhavatShalom
    }
}

internal fun ComprehensiveZmanimCalendar.minchaKetana(settings: ZmanimCalculationSettings): Instant? {
    val values = settings.minchaKetanaCustom
    fun custom(unit: CustomZmanUnit): Instant? {
        val start = customDayStart(unit, values) ?: return null
        val end = customDayEnd(unit, values) ?: return null
        return getMinchaKetana(start, end)
    }
    return when (settings.minchaKetanaMethod) {
        MinchaKetanaMethod.Standard -> graDay(settings) { start, end -> getMinchaKetana(start, end) }
        MinchaKetanaMethod.CustomDegrees -> custom(CustomZmanUnit.Degrees)
        MinchaKetanaMethod.CustomMinutes -> custom(CustomZmanUnit.Minutes)
        MinchaKetanaMethod.CustomZmaniyotMinutes -> custom(CustomZmanUnit.ZmaniyotMinutes)
        MinchaKetanaMethod.FixedLocal -> sunset(settings.sunsetMethod)
            ?.let { getHalfDayBasedZman(fixedLocalChatzosHayom, it, 3.5) }
        MinchaKetanaMethod.BaalHatanya -> minchaKetanaBaalHatanya
        MinchaKetanaMethod.AteretTorah -> minchaKetanaAteretTorah
        MinchaKetanaMethod.AhavatShalom -> minchaKetanaAhavatShalom
    }
}

internal fun ComprehensiveZmanimCalendar.plagHamincha(settings: ZmanimCalculationSettings): Instant? {
    val values = settings.plagHaminchaCustom
    fun custom(unit: CustomZmanUnit): Instant? {
        val start = customDayStart(unit, values) ?: return null
        val end = customDayEnd(unit, values) ?: return null
        return getPlagHamincha(start, end)
    }
    return when (settings.plagHaminchaMethod) {
        PlagHaminchaMethod.Gra -> graDay(settings) { start, end -> getPlagHamincha(start, end) }
        PlagHaminchaMethod.CustomDegrees -> custom(CustomZmanUnit.Degrees)
        PlagHaminchaMethod.CustomMinutes -> custom(CustomZmanUnit.Minutes)
        PlagHaminchaMethod.CustomZmaniyotMinutes -> custom(CustomZmanUnit.ZmaniyotMinutes)
        PlagHaminchaMethod.Alot16Point1ToTzeit7Point083 -> plagAlos16Point1DegreesToTzaisGeonim7Point083Degrees
        PlagHaminchaMethod.FixedLocal -> sunset(settings.sunsetMethod)
            ?.let { getHalfDayBasedZman(fixedLocalChatzosHayom, it, 4.75) }
        PlagHaminchaMethod.BaalHatanya -> plagHaminchaBaalHatanya
        PlagHaminchaMethod.AteretTorah -> plagHaminchaAteretTorah
        PlagHaminchaMethod.AhavatShalom -> plagAhavatShalom
    }
}

/**
 * Candle lighting for the day this calendar is set to, and whether it had to be held at plag.
 * [isPlag] is true when the chosen minutes before sunset would fall before plag hamincha — on a short
 * winter day far north — since candles lit earlier than plag do not bring the day in.
 */
internal data class CandleLighting(val time: Instant, val isPlag: Boolean)

/**
 * The chosen minutes before the chosen sunset, but never earlier than the plag the user picked: the
 * same plag their own plag row shows, not KosherJava's built-in GRA one. Computed here rather than
 * through the library's getCandleLighting, whose answer on such a day is its own (null in 3.1).
 */
internal fun ComprehensiveZmanimCalendar.candleLighting(settings: ZmanimCalculationSettings): CandleLighting? {
    val lighting = sunset(settings.sunsetMethod)
        ?.minus(Duration.ofMinutes(settings.candleLightingOffsetMinutes.toLong()))
        ?: return null
    val plag = plagHamincha(settings)
    return if (plag != null && lighting.isBefore(plag)) {
        CandleLighting(plag, isPlag = true)
    } else {
        CandleLighting(lighting, isPlag = false)
    }
}

internal fun ComprehensiveZmanimCalendar.tzeit(settings: ZmanimCalculationSettings): Instant? =
    tzeit(settings.tzeitHakochavimMethod, settings.tzeitHakochavimCustom)

internal fun ComprehensiveZmanimCalendar.tzeit(
    method: TzeitHakochavimMethod,
    custom: CustomZmanValue,
): Instant? = when (method) {
    TzeitHakochavimMethod.Degrees6Point2 -> sunsetByDegrees(6.2) ?: tzais50Minutes
    TzeitHakochavimMethod.CustomDegrees -> sunsetByDegrees(custom.degrees)
    TzeitHakochavimMethod.CustomMinutes -> minutesAfterSunset(custom.minutes)
    TzeitHakochavimMethod.CustomZmaniyotMinutes -> zmaniyotAfterSunset(custom.zmaniyotMinutes)
}

/**
 * When a holy day — Shabbat, Yom Tov, Yom Kippur — goes out: the motzei method plus the user's
 * tosefet. Ordinary fasts do not get the tosefet; they simply end at tzeit.
 */
internal fun ComprehensiveZmanimCalendar.holyDayExit(settings: ZmanimCalculationSettings): Instant? =
    motzeiShabbat(settings)?.let {
        AstronomicalCalendar.getTimeOffset(it, Duration.ofMinutes(settings.holyDayTosefetMinutes.toLong()))
    }

internal fun ComprehensiveZmanimCalendar.motzeiShabbat(settings: ZmanimCalculationSettings): Instant? {
    val custom = settings.motzeiShabbatCustom
    return when (settings.motzeiShabbatMethod) {
        MotzeiShabbatMethod.Degrees6Point2 -> sunsetByDegrees(6.2) ?: tzais50Minutes
        MotzeiShabbatMethod.CustomDegrees -> sunsetByDegrees(custom.degrees)
        MotzeiShabbatMethod.CustomMinutes -> minutesAfterSunset(custom.minutes)
        MotzeiShabbatMethod.CustomZmaniyotMinutes -> zmaniyotAfterSunset(custom.zmaniyotMinutes)
    }
}

internal fun ComprehensiveZmanimCalendar.rabbeinuTam(settings: ZmanimCalculationSettings): Instant? {
    val custom = settings.rabbeinuTamCustom
    return when (settings.rabbeinuTamMethod) {
        RabbeinuTamMethod.Minutes72 -> tzais72Minutes
        RabbeinuTamMethod.CustomDegrees -> sunsetByDegrees(custom.degrees)
        RabbeinuTamMethod.CustomMinutes -> minutesAfterSunset(custom.minutes)
        RabbeinuTamMethod.CustomZmaniyotMinutes -> zmaniyotAfterSunset(custom.zmaniyotMinutes)
        RabbeinuTamMethod.BainHashmashot58Point5 -> bainHashmashosRT58Point5Minutes
        RabbeinuTamMethod.BainHashmashot13Point5Before7Point083 -> bainHashmashosRT13Point5MinutesBefore7Point083Degrees
        RabbeinuTamMethod.BainHashmashot2Stars -> bainHashmashosRT2Stars
    }
}

internal fun ComprehensiveZmanimCalendar.chametzTimes(settings: ZmanimCalculationSettings): Pair<Instant?, Instant?> {
    val values = settings.chametzCustom
    /**
     * Four and five halachic hours into the day the custom [unit] defines: the last moment chametz
     * may be eaten, and the last it may be owned.
     */
    fun custom(unit: CustomZmanUnit): Pair<Instant?, Instant?> {
        val start = customDayStart(unit, values) ?: return null to null
        val end = customDayEnd(unit, values) ?: return null to null
        return getShaahZmanisBasedZman(start, end, 4.0) to getShaahZmanisBasedZman(start, end, 5.0)
    }
    return when (settings.chametzMethod) {
        ChametzMethod.Gra -> sofZmanAchilasChametzGRA to sofZmanBiurChametzGRA
        ChametzMethod.CustomDegrees -> custom(CustomZmanUnit.Degrees)
        ChametzMethod.CustomMinutes -> custom(CustomZmanUnit.Minutes)
        ChametzMethod.CustomZmaniyotMinutes -> custom(CustomZmanUnit.ZmaniyotMinutes)
        ChametzMethod.BaalHatanya -> sofZmanAchilasChametzBaalHatanya to sofZmanBiurChametzBaalHatanya
    }
}
