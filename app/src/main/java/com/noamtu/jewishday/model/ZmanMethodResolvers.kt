// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import com.kosherjava.zmanim.AstronomicalCalendar
import com.kosherjava.zmanim.ComplexZmanimCalendar
import java.util.Date

// ---------------------------------------------------------------------------------------------
// Custom-value primitives
//
// The method pickers no longer offer a ladder of degrees, minutes and zmaniyot minutes: the user
// types the number. KosherJava has a named getter per rung of the old ladders and nothing for an
// arbitrary value, so these four build the same things from first principles — the identical
// arithmetic the library's own getters do, with the number coming from settings.
// ---------------------------------------------------------------------------------------------

private const val MinuteMillis = 60_000L

/** A degree-based dawn: how long before sunrise the sun sits [degrees] below the horizon. */
private fun ComplexZmanimCalendar.sunriseByDegrees(degrees: Double): Date? =
    getSunriseOffsetByDegrees(AstronomicalCalendar.GEOMETRIC_ZENITH + degrees)

/** The same after sunset, for nightfall opinions. */
private fun ComplexZmanimCalendar.sunsetByDegrees(degrees: Double): Date? =
    getSunsetOffsetByDegrees(AstronomicalCalendar.GEOMETRIC_ZENITH + degrees)

/**
 * A number of *zmaniyot* minutes as a duration in millis: sixtieths of this day's GRA halachic hour,
 * so 72 of them run longer than 72 clock minutes in summer and shorter in winter. Null where the day
 * has no length to divide — inside the arctic circle the library reports [Long.MIN_VALUE].
 */
private fun ComplexZmanimCalendar.zmaniyotOffsetMillis(minutes: Int): Long? {
    val shaahZmanis = shaahZmanisGra
    if (shaahZmanis == Long.MIN_VALUE) return null
    return shaahZmanis * minutes / 60L
}

private fun ComplexZmanimCalendar.zmaniyotBeforeSunrise(minutes: Int): Date? =
    zmaniyotOffsetMillis(minutes)?.let { AstronomicalCalendar.getTimeOffset(seaLevelSunrise, -it) }

private fun ComplexZmanimCalendar.zmaniyotAfterSunset(minutes: Int): Date? =
    zmaniyotOffsetMillis(minutes)?.let { AstronomicalCalendar.getTimeOffset(seaLevelSunset, it) }

private fun ComplexZmanimCalendar.minutesBeforeSunrise(minutes: Int): Date? =
    AstronomicalCalendar.getTimeOffset(seaLevelSunrise, -minutes.toLong() * MinuteMillis)

private fun ComplexZmanimCalendar.minutesAfterSunset(minutes: Int): Date? =
    AstronomicalCalendar.getTimeOffset(seaLevelSunset, minutes.toLong() * MinuteMillis)

/**
 * The dawn a custom [unit]/[values] pair defines, which is the start of the halachic day for every
 * Magen Avraham-style opinion below.
 */
private fun ComplexZmanimCalendar.customDayStart(unit: CustomZmanUnit, values: CustomZmanValue): Date? =
    when (unit) {
        CustomZmanUnit.Degrees -> sunriseByDegrees(values.degrees)
        CustomZmanUnit.Minutes -> minutesBeforeSunrise(values.minutes)
        CustomZmanUnit.ZmaniyotMinutes -> zmaniyotBeforeSunrise(values.zmaniyotMinutes)
    }

/** Its mirror image at the other end of the day: the nightfall the same opinion ends at. */
private fun ComplexZmanimCalendar.customDayEnd(unit: CustomZmanUnit, values: CustomZmanValue): Date? =
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
private inline fun ComplexZmanimCalendar.graDay(
    settings: ZmanimCalculationSettings,
    zman: (Date, Date) -> Date?,
): Date? {
    val start = sunrise(settings.sunriseMethod) ?: return null
    val end = sunset(settings.sunsetMethod) ?: return null
    return zman(start, end)
}

/** Of two times, the later — or whichever one exists. */
private fun laterOf(first: Date?, second: Date?): Date? = when {
    first == null -> second
    second == null -> first
    else -> if (first.after(second)) first else second
}

internal fun ComplexZmanimCalendar.alotHashachar(settings: ZmanimCalculationSettings): Date? =
    when (settings.alotHashacharMethod) {
        AlotHashacharMethod.Degrees16Point1 -> alos16Point1Degrees ?: alos72
        AlotHashacharMethod.CustomDegrees -> sunriseByDegrees(settings.alotHashacharCustom.degrees)
        AlotHashacharMethod.CustomMinutes -> minutesBeforeSunrise(settings.alotHashacharCustom.minutes)
        AlotHashacharMethod.CustomZmaniyotMinutes -> zmaniyotBeforeSunrise(settings.alotHashacharCustom.zmaniyotMinutes)
    }

internal fun ComplexZmanimCalendar.misheyakir(settings: ZmanimCalculationSettings): Date? {
    val custom = settings.misheyakirCustom
    return when (settings.misheyakirMethod) {
        MisheyakirMethod.Degrees11 -> misheyakir11Degrees
        MisheyakirMethod.CustomDegrees -> sunriseByDegrees(custom.degrees)
        // Measured from the sunrise the user chose, since this is the one option stated as an offset
        // from sunrise itself rather than as a position of the sun.
        MisheyakirMethod.CustomMinutesBeforeSunrise -> sunrise(settings.sunriseMethod)
            ?.let { AstronomicalCalendar.getTimeOffset(it, -custom.minutes.toLong() * MinuteMillis) }
        MisheyakirMethod.CustomZmaniyotMinutesBeforeSunrise -> zmaniyotBeforeSunrise(custom.zmaniyotMinutes)
        MisheyakirMethod.Minutes6AfterAlos -> alotHashachar(settings)
            ?.let { AstronomicalCalendar.getTimeOffset(it, 6 * MinuteMillis) }
    }
}

internal fun ComplexZmanimCalendar.sunrise(method: SunriseMethod): Date? = when (method) {
    SunriseMethod.SeaLevel -> seaLevelSunrise
    SunriseMethod.ElevationAdjusted -> sunrise
}

internal fun ComplexZmanimCalendar.sunset(method: SunsetMethod): Date? = when (method) {
    SunsetMethod.SeaLevel -> seaLevelSunset
    SunsetMethod.ElevationAdjusted -> sunset
}

internal fun ComplexZmanimCalendar.sofZmanShema(
    method: SofZmanShemaMethod,
    settings: ZmanimCalculationSettings,
): Date? {
    val custom = settings.sofZmanShemaCustom
    /** Three of the twelve hours of a day that runs [start] to [end]. */
    fun mga(unit: CustomZmanUnit): Date? {
        val start = customDayStart(unit, custom) ?: return null
        val end = customDayEnd(unit, custom) ?: return null
        return getSofZmanShma(start, end)
    }
    // A day that runs from dawn to fixed-local chatzot is only half a day, so it is divided into six
    // — which is what the library's own ...ToFixedLocalChatzos getters do.
    fun toFixedLocalChatzot(unit: CustomZmanUnit): Date? {
        val start = customDayStart(unit, custom) ?: return null
        return getFixedLocalChatzosBasedZmanim(start, fixedLocalChatzos, 3.0)
    }
    return when (method) {
        // The GRA's day is sunrise to sunset, so it is the sunrise and sunset the user chose — which
        // is also the only place elevation is decided. Computed from those rather than through the
        // library's global isUseElevation flag, which could not express one of the two being at sea
        // level and the other observed.
        SofZmanShemaMethod.Gra -> graDay(settings) { start, end -> getSofZmanShma(start, end) }
        SofZmanShemaMethod.FixedLocalGra ->
            sunrise(settings.sunriseMethod)?.let { getFixedLocalChatzosBasedZmanim(it, fixedLocalChatzos, 3.0) }
        SofZmanShemaMethod.Mga16Point1 -> sofZmanShmaMGA16Point1Degrees ?: sofZmanShmaMGA72Minutes
        SofZmanShemaMethod.CustomDegrees -> mga(CustomZmanUnit.Degrees)
        SofZmanShemaMethod.CustomMinutes -> mga(CustomZmanUnit.Minutes)
        SofZmanShemaMethod.CustomZmaniyotMinutes -> mga(CustomZmanUnit.ZmaniyotMinutes)
        SofZmanShemaMethod.CustomDegreesToFixedLocalChatzot -> toFixedLocalChatzot(CustomZmanUnit.Degrees)
        SofZmanShemaMethod.CustomMinutesToFixedLocalChatzot -> toFixedLocalChatzot(CustomZmanUnit.Minutes)
        SofZmanShemaMethod.Alos16Point1ToSunset -> sofZmanShmaAlos16Point1ToSunset ?: sofZmanShmaMGA72Minutes
        SofZmanShemaMethod.Alos16Point1ToTzeit7Point083 -> sofZmanShmaAlos16Point1ToTzaisGeonim7Point083Degrees ?: sofZmanShmaMGA72Minutes
        SofZmanShemaMethod.AteretTorah -> sofZmanShmaAteretTorah
    }
}

internal fun ComplexZmanimCalendar.sofZmanTefillah(
    method: SofZmanTefillahMethod,
    settings: ZmanimCalculationSettings,
): Date? {
    val custom = settings.sofZmanTefillahCustom
    /** Four of the twelve hours of the day the custom [unit] defines. */
    fun mga(unit: CustomZmanUnit): Date? {
        val start = customDayStart(unit, custom) ?: return null
        val end = customDayEnd(unit, custom) ?: return null
        return getSofZmanTfila(start, end)
    }
    fun mgaFrom(alos: Date?, tzais: Date?): Date? {
        if (alos == null || tzais == null) return null
        return getSofZmanTfila(alos, tzais)
    }
    return when (method) {
        SofZmanTefillahMethod.Gra -> graDay(settings) { start, end -> getSofZmanTfila(start, end) }
        SofZmanTefillahMethod.FixedLocalGra ->
            sunrise(settings.sunriseMethod)?.let { getFixedLocalChatzosBasedZmanim(it, fixedLocalChatzos, 4.0) }
        SofZmanTefillahMethod.Mga16Point1 -> sofZmanTfilaMGA16Point1Degrees ?: sofZmanTfilaMGA72Minutes
        SofZmanTefillahMethod.CustomDegrees -> mga(CustomZmanUnit.Degrees)
        SofZmanTefillahMethod.CustomMinutes -> mga(CustomZmanUnit.Minutes)
        SofZmanTefillahMethod.CustomZmaniyotMinutes -> mga(CustomZmanUnit.ZmaniyotMinutes)
        SofZmanTefillahMethod.Alos16Point1ToSunset -> mgaFrom(alos16Point1Degrees, seaLevelSunset) ?: sofZmanTfilaMGA72Minutes
        SofZmanTefillahMethod.Alos16Point1ToTzeit7Point083 -> mgaFrom(alos16Point1Degrees, tzaisGeonim7Point083Degrees) ?: sofZmanTfilaMGA72Minutes
        SofZmanTefillahMethod.AteretTorah -> sofZmanTfilahAteretTorah
    }
}

internal fun ComplexZmanimCalendar.chatzot(method: ChatzotMethod): Date? = when (method) {
    ChatzotMethod.Solar -> chatzos
    ChatzotMethod.FixedLocal -> fixedLocalChatzos
}

// Midnight counterpart of chatzot: solar midnight, or 12h past fixed-local midday.
internal fun ComplexZmanimCalendar.chatzotHaLaila(method: ChatzotMethod): Date? = when (method) {
    ChatzotMethod.Solar -> solarMidnight
    ChatzotMethod.FixedLocal -> AstronomicalCalendar.getTimeOffset(fixedLocalChatzos, 12L * 60 * 60 * 1000)
}

internal fun ComplexZmanimCalendar.minchaGedola(settings: ZmanimCalculationSettings): Date? {
    val values = settings.minchaGedolaCustom
    fun custom(unit: CustomZmanUnit): Date? {
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
        MinchaGedolaMethod.BaalHatanyaGreaterThan30 -> minchaGedolaBaalHatanyaGreaterThan30
        MinchaGedolaMethod.AteretTorah -> minchaGedolaAteretTorah
        MinchaGedolaMethod.AhavatShalom -> minchaGedolaAhavatShalom
    }
}

internal fun ComplexZmanimCalendar.minchaKetana(settings: ZmanimCalculationSettings): Date? {
    val values = settings.minchaKetanaCustom
    fun custom(unit: CustomZmanUnit): Date? {
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
            ?.let { getFixedLocalChatzosBasedZmanim(fixedLocalChatzos, it, 3.5) }
        MinchaKetanaMethod.BaalHatanya -> minchaKetanaBaalHatanya
        MinchaKetanaMethod.AteretTorah -> minchaKetanaAteretTorah
        MinchaKetanaMethod.AhavatShalom -> minchaKetanaAhavatShalom
    }
}

internal fun ComplexZmanimCalendar.plagHamincha(settings: ZmanimCalculationSettings): Date? {
    val values = settings.plagHaminchaCustom
    fun custom(unit: CustomZmanUnit): Date? {
        val start = customDayStart(unit, values) ?: return null
        val end = customDayEnd(unit, values) ?: return null
        return getPlagHamincha(start, end)
    }
    return when (settings.plagHaminchaMethod) {
        PlagHaminchaMethod.Gra -> graDay(settings) { start, end -> getPlagHamincha(start, end) }
        PlagHaminchaMethod.CustomDegrees -> custom(CustomZmanUnit.Degrees)
        PlagHaminchaMethod.CustomMinutes -> custom(CustomZmanUnit.Minutes)
        PlagHaminchaMethod.CustomZmaniyotMinutes -> custom(CustomZmanUnit.ZmaniyotMinutes)
        PlagHaminchaMethod.Alot16Point1ToTzeit7Point083 -> plagAlos16Point1ToTzaisGeonim7Point083Degrees
        PlagHaminchaMethod.FixedLocal -> sunset(settings.sunsetMethod)
            ?.let { getFixedLocalChatzosBasedZmanim(fixedLocalChatzos, it, 4.75) }
        PlagHaminchaMethod.BaalHatanya -> plagHaminchaBaalHatanya
        PlagHaminchaMethod.AteretTorah -> plagHaminchaAteretTorah
        PlagHaminchaMethod.AhavatShalom -> plagAhavatShalom
    }
}

internal fun ComplexZmanimCalendar.tzeit(settings: ZmanimCalculationSettings): Date? =
    tzeit(settings.tzeitHakochavimMethod, settings.tzeitHakochavimCustom)

internal fun ComplexZmanimCalendar.tzeit(
    method: TzeitHakochavimMethod,
    custom: CustomZmanValue,
): Date? = when (method) {
    TzeitHakochavimMethod.Degrees6Point2 -> sunsetByDegrees(6.2) ?: tzais50
    TzeitHakochavimMethod.CustomDegrees -> sunsetByDegrees(custom.degrees)
    TzeitHakochavimMethod.CustomMinutes -> minutesAfterSunset(custom.minutes)
    TzeitHakochavimMethod.CustomZmaniyotMinutes -> zmaniyotAfterSunset(custom.zmaniyotMinutes)
}

/**
 * When a holy day — Shabbat, Yom Tov, Yom Kippur — goes out: the motzei method plus the user's
 * tosefet. Ordinary fasts do not get the tosefet; they simply end at tzeit.
 */
internal fun ComplexZmanimCalendar.holyDayExit(settings: ZmanimCalculationSettings): Date? =
    motzeiShabbat(settings)?.let {
        AstronomicalCalendar.getTimeOffset(it, settings.holyDayTosefetMinutes.toLong() * MinuteMillis)
    }

internal fun ComplexZmanimCalendar.motzeiShabbat(settings: ZmanimCalculationSettings): Date? {
    val custom = settings.motzeiShabbatCustom
    return when (settings.motzeiShabbatMethod) {
        MotzeiShabbatMethod.Degrees6Point2 -> sunsetByDegrees(6.2) ?: tzais50
        MotzeiShabbatMethod.CustomDegrees -> sunsetByDegrees(custom.degrees)
        MotzeiShabbatMethod.CustomMinutes -> minutesAfterSunset(custom.minutes)
        MotzeiShabbatMethod.CustomZmaniyotMinutes -> zmaniyotAfterSunset(custom.zmaniyotMinutes)
    }
}

internal fun ComplexZmanimCalendar.rabbeinuTam(settings: ZmanimCalculationSettings): Date? {
    val custom = settings.rabbeinuTamCustom
    return when (settings.rabbeinuTamMethod) {
        RabbeinuTamMethod.Minutes72 -> tzais72
        RabbeinuTamMethod.CustomDegrees -> sunsetByDegrees(custom.degrees)
        RabbeinuTamMethod.CustomMinutes -> minutesAfterSunset(custom.minutes)
        RabbeinuTamMethod.CustomZmaniyotMinutes -> zmaniyotAfterSunset(custom.zmaniyotMinutes)
        RabbeinuTamMethod.BainHashmashot58Point5 -> bainHashmashosRT58Point5Minutes
        RabbeinuTamMethod.BainHashmashot13Point5Before7Point083 -> bainHashmashosRT13Point5MinutesBefore7Point083Degrees
        RabbeinuTamMethod.BainHashmashot2Stars -> bainHashmashosRT2Stars
    }
}

internal fun ComplexZmanimCalendar.chametzTimes(settings: ZmanimCalculationSettings): Pair<Date?, Date?> {
    val values = settings.chametzCustom
    /**
     * Four and five halachic hours into the day the custom [unit] defines: the last moment chametz
     * may be eaten, and the last it may be owned.
     */
    fun custom(unit: CustomZmanUnit): Pair<Date?, Date?> {
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
