// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import com.kosherjava.zmanim.AstronomicalCalendar
import com.kosherjava.zmanim.ComplexZmanimCalendar
import java.util.Date

internal fun ComplexZmanimCalendar.alotHashachar(settings: ZmanimCalculationSettings): Date? =
    when (settings.alotHashacharMethod) {
        AlotHashacharMethod.Minutes60 -> alos60
        AlotHashacharMethod.Minutes72 -> alos72
        AlotHashacharMethod.Minutes90 -> alos90
        AlotHashacharMethod.Minutes96 -> alos96
        AlotHashacharMethod.Minutes120 -> alos120
        AlotHashacharMethod.Zmanis72 -> alos72Zmanis
        AlotHashacharMethod.Zmanis90 -> alos90Zmanis
        AlotHashacharMethod.Zmanis96 -> alos96Zmanis
        AlotHashacharMethod.Zmanis120 -> alos120Zmanis
        AlotHashacharMethod.Degrees12 -> getSunriseOffsetByDegrees(AstronomicalCalendar.GEOMETRIC_ZENITH + 12.0) ?: alos60
        AlotHashacharMethod.Degrees14 -> getSunriseOffsetByDegrees(AstronomicalCalendar.GEOMETRIC_ZENITH + 14.0) ?: alos60
        AlotHashacharMethod.Degrees16 -> getSunriseOffsetByDegrees(AstronomicalCalendar.GEOMETRIC_ZENITH + 16.0) ?: alos72
        AlotHashacharMethod.Degrees16Point013 -> getSunriseOffsetByDegrees(AstronomicalCalendar.GEOMETRIC_ZENITH + 16.013) ?: alos72
        AlotHashacharMethod.Degrees16Point04 -> getSunriseOffsetByDegrees(AstronomicalCalendar.GEOMETRIC_ZENITH + 16.04) ?: alos72
        AlotHashacharMethod.Degrees16Point08 -> getSunriseOffsetByDegrees(AstronomicalCalendar.GEOMETRIC_ZENITH + 16.08) ?: alos72
        AlotHashacharMethod.Degrees16Point1 -> alos16Point1Degrees ?: alos72
        AlotHashacharMethod.Degrees17Point5 -> getSunriseOffsetByDegrees(AstronomicalCalendar.GEOMETRIC_ZENITH + 17.5) ?: alos72
        AlotHashacharMethod.Degrees18 -> alos18Degrees ?: alos90
        AlotHashacharMethod.Degrees19 -> alos19Degrees ?: alos90
        AlotHashacharMethod.Degrees19Point75 -> getSunriseOffsetByDegrees(AstronomicalCalendar.GEOMETRIC_ZENITH + 19.75) ?: alos96
        AlotHashacharMethod.Degrees19Point784 -> getSunriseOffsetByDegrees(AstronomicalCalendar.GEOMETRIC_ZENITH + 19.784) ?: alos96
        AlotHashacharMethod.Degrees19Point8 -> alos19Point8Degrees ?: alos96
        AlotHashacharMethod.Degrees19Point848 -> getSunriseOffsetByDegrees(AstronomicalCalendar.GEOMETRIC_ZENITH + 19.848) ?: alos96
        AlotHashacharMethod.Degrees20 -> getSunriseOffsetByDegrees(AstronomicalCalendar.GEOMETRIC_ZENITH + 20.0) ?: alos96
        AlotHashacharMethod.Degrees26 -> alos26Degrees ?: alos120
        AlotHashacharMethod.BaalHatanya -> alosBaalHatanya ?: alos72
    }

internal fun ComplexZmanimCalendar.misheyakir(settings: ZmanimCalculationSettings): Date? {
    fun minutesBeforeSunrise(minutes: Long): Date? =
        sunrise(settings.sunriseMethod)?.let { AstronomicalCalendar.getTimeOffset(it, -minutes * 60_000L) }
    return when (settings.misheyakirMethod) {
        MisheyakirMethod.Degrees12Point85 -> getSunriseOffsetByDegrees(AstronomicalCalendar.GEOMETRIC_ZENITH + 12.85)
        MisheyakirMethod.Degrees12 -> getSunriseOffsetByDegrees(AstronomicalCalendar.GEOMETRIC_ZENITH + 12.0)
        MisheyakirMethod.Degrees11Point5 -> misheyakir11Point5Degrees
        MisheyakirMethod.Degrees11 -> misheyakir11Degrees
        MisheyakirMethod.Degrees10Point2 -> misheyakir10Point2Degrees
        MisheyakirMethod.Degrees9Point5 -> misheyakir9Point5Degrees
        MisheyakirMethod.Degrees7Point65 -> misheyakir7Point65Degrees
        MisheyakirMethod.Minutes35BeforeSunrise -> minutesBeforeSunrise(35)
        MisheyakirMethod.Minutes36BeforeSunrise -> minutesBeforeSunrise(36)
        MisheyakirMethod.Minutes40BeforeSunrise -> minutesBeforeSunrise(40)
        MisheyakirMethod.Minutes42BeforeSunrise -> minutesBeforeSunrise(42)
        MisheyakirMethod.Minutes45BeforeSunrise -> minutesBeforeSunrise(45)
        MisheyakirMethod.Minutes48BeforeSunrise -> minutesBeforeSunrise(48)
        MisheyakirMethod.Minutes50BeforeSunrise -> minutesBeforeSunrise(50)
        MisheyakirMethod.Minutes52BeforeSunrise -> minutesBeforeSunrise(52)
        MisheyakirMethod.Minutes57BeforeSunrise -> minutesBeforeSunrise(57)
        MisheyakirMethod.Minutes60BeforeSunrise -> minutesBeforeSunrise(60)
        MisheyakirMethod.Minutes6AfterAlos -> alotHashachar(settings)?.let { AstronomicalCalendar.getTimeOffset(it, 6 * 60_000L) }
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
): Date? =
    when (method) {
        SofZmanShemaMethod.Gra -> {
            isUseElevation = settings.useElevation
            val result = sofZmanShmaGRA ?: sofZmanShmaMGA72Minutes
            isUseElevation = false
            result
        }
        SofZmanShemaMethod.FixedLocalGra -> {
            isUseElevation = settings.useElevation
            val result = sofZmanShmaGRASunriseToFixedLocalChatzos ?: sofZmanShmaMGA72Minutes
            isUseElevation = false
            result
        }
        SofZmanShemaMethod.Mga72 -> sofZmanShmaMGA72Minutes
        SofZmanShemaMethod.Mga72Zmanis -> sofZmanShmaMGA72MinutesZmanis
        SofZmanShemaMethod.Mga90 -> sofZmanShmaMGA90Minutes
        SofZmanShemaMethod.Mga90Zmanis -> sofZmanShmaMGA90MinutesZmanis
        SofZmanShemaMethod.Mga96 -> sofZmanShmaMGA96Minutes
        SofZmanShemaMethod.Mga96Zmanis -> sofZmanShmaMGA96MinutesZmanis
        SofZmanShemaMethod.Mga120 -> sofZmanShmaMGA120Minutes
        SofZmanShemaMethod.Mga16Point1 -> sofZmanShmaMGA16Point1Degrees ?: sofZmanShmaMGA72Minutes
        SofZmanShemaMethod.Mga18 -> sofZmanShmaMGA18Degrees ?: sofZmanShmaMGA90Minutes
        SofZmanShemaMethod.Mga19Point8 -> sofZmanShmaMGA19Point8Degrees ?: sofZmanShmaMGA96Minutes
        SofZmanShemaMethod.Alos16Point1ToSunset -> sofZmanShmaAlos16Point1ToSunset ?: sofZmanShmaMGA72Minutes
        SofZmanShemaMethod.Alos16Point1ToTzeit7Point083 -> sofZmanShmaAlos16Point1ToTzaisGeonim7Point083Degrees ?: sofZmanShmaMGA72Minutes
        SofZmanShemaMethod.Mga18ToFixedLocalChatzot -> sofZmanShmaMGA18DegreesToFixedLocalChatzos ?: sofZmanShmaMGA90Minutes
        SofZmanShemaMethod.Mga16Point1ToFixedLocalChatzot -> sofZmanShmaMGA16Point1DegreesToFixedLocalChatzos ?: sofZmanShmaMGA72Minutes
        SofZmanShemaMethod.Mga90ToFixedLocalChatzot -> sofZmanShmaMGA90MinutesToFixedLocalChatzos
        SofZmanShemaMethod.Mga72ToFixedLocalChatzot -> sofZmanShmaMGA72MinutesToFixedLocalChatzos
        SofZmanShemaMethod.AteretTorah -> sofZmanShmaAteretTorah
    }

internal fun ComplexZmanimCalendar.sofZmanTefillah(
    method: SofZmanTefillahMethod,
    settings: ZmanimCalculationSettings,
): Date? {
    fun mga(alos: Date?, tzais: Date?): Date? {
        if (alos == null || tzais == null) return null
        return AstronomicalCalendar.getTimeOffset(alos, 4L * (tzais.time - alos.time) / 12)
    }
    return when (method) {
        SofZmanTefillahMethod.Gra -> {
            isUseElevation = settings.useElevation
            val result = sofZmanTfilaGRA
            isUseElevation = false
            result
        }
        SofZmanTefillahMethod.FixedLocalGra -> {
            isUseElevation = settings.useElevation
            val result = sofZmanTfilaGRASunriseToFixedLocalChatzos ?: sofZmanTfilaGRA
            isUseElevation = false
            result
        }
        SofZmanTefillahMethod.Mga72 -> sofZmanTfilaMGA72Minutes
        SofZmanTefillahMethod.Mga72Zmanis -> sofZmanTfilaMGA72MinutesZmanis
        SofZmanTefillahMethod.Mga90 -> sofZmanTfilaMGA90Minutes
        SofZmanTefillahMethod.Mga90Zmanis -> sofZmanTfilaMGA90MinutesZmanis
        SofZmanTefillahMethod.Mga96 -> sofZmanTfilaMGA96Minutes
        SofZmanTefillahMethod.Mga96Zmanis -> sofZmanTfilaMGA96MinutesZmanis
        SofZmanTefillahMethod.Mga120 -> sofZmanTfilaMGA120Minutes
        SofZmanTefillahMethod.Mga16Point1 -> sofZmanTfilaMGA16Point1Degrees ?: sofZmanTfilaMGA72Minutes
        SofZmanTefillahMethod.Mga18 -> sofZmanTfilaMGA18Degrees ?: sofZmanTfilaMGA90Minutes
        SofZmanTefillahMethod.Mga19Point8 -> sofZmanTfilaMGA19Point8Degrees ?: sofZmanTfilaMGA96Minutes
        SofZmanTefillahMethod.Mga60 -> mga(alos60, tzais60) ?: sofZmanTfilaMGA72Minutes
        SofZmanTefillahMethod.Mga120Zmanis -> mga(alos120Zmanis, tzais120Zmanis) ?: sofZmanTfilaMGA120Minutes
        SofZmanTefillahMethod.Mga26 -> mga(alos26Degrees, tzais26Degrees) ?: sofZmanTfilaMGA120Minutes
        SofZmanTefillahMethod.Alos16Point1ToSunset -> mga(alos16Point1Degrees, seaLevelSunset) ?: sofZmanTfilaMGA72Minutes
        SofZmanTefillahMethod.Alos16Point1ToTzeit7Point083 -> mga(alos16Point1Degrees, tzaisGeonim7Point083Degrees) ?: sofZmanTfilaMGA72Minutes
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

internal fun ComplexZmanimCalendar.minchaGedola(settings: ZmanimCalculationSettings): Date? = when (settings.minchaGedolaMethod) {
    MinchaGedolaMethod.Standard -> {
        isUseElevation = settings.useElevation
        val result = minchaGedola
        isUseElevation = false
        result
    }
    MinchaGedolaMethod.ThirtyMinutes -> minchaGedola30Minutes
    MinchaGedolaMethod.GreaterThan30 -> minchaGedolaGreaterThan30
    MinchaGedolaMethod.Mga72 -> minchaGedola72Minutes
    MinchaGedolaMethod.Degrees16Point1 -> minchaGedola16Point1Degrees
    MinchaGedolaMethod.FixedLocal -> minchaGedolaGRAFixedLocalChatzos30Minutes
    MinchaGedolaMethod.BaalHatanya -> minchaGedolaBaalHatanya
    MinchaGedolaMethod.BaalHatanyaGreaterThan30 -> minchaGedolaBaalHatanyaGreaterThan30
    MinchaGedolaMethod.AteretTorah -> minchaGedolaAteretTorah
    MinchaGedolaMethod.AhavatShalom -> minchaGedolaAhavatShalom
}

internal fun ComplexZmanimCalendar.minchaKetana(settings: ZmanimCalculationSettings): Date? = when (settings.minchaKetanaMethod) {
    MinchaKetanaMethod.Standard -> {
        isUseElevation = settings.useElevation
        val result = minchaKetana
        isUseElevation = false
        result
    }
    MinchaKetanaMethod.Mga72 -> minchaKetana72Minutes
    MinchaKetanaMethod.Degrees16Point1 -> minchaKetana16Point1Degrees
    MinchaKetanaMethod.FixedLocal -> minchaKetanaGRAFixedLocalChatzosToSunset
    MinchaKetanaMethod.BaalHatanya -> minchaKetanaBaalHatanya
    MinchaKetanaMethod.AteretTorah -> minchaKetanaAteretTorah
    MinchaKetanaMethod.AhavatShalom -> minchaKetanaAhavatShalom
}

internal fun ComplexZmanimCalendar.plagHamincha(settings: ZmanimCalculationSettings): Date? = when (settings.plagHaminchaMethod) {
    PlagHaminchaMethod.Gra -> {
        isUseElevation = settings.useElevation
        val result = plagHamincha
        isUseElevation = false
        result
    }
    PlagHaminchaMethod.Mga60 -> plagHamincha60Minutes
    PlagHaminchaMethod.Mga72 -> plagHamincha72Minutes
    PlagHaminchaMethod.Mga72Zmanis -> plagHamincha72MinutesZmanis
    PlagHaminchaMethod.Mga90 -> plagHamincha90Minutes
    PlagHaminchaMethod.Mga90Zmanis -> plagHamincha90MinutesZmanis
    PlagHaminchaMethod.Mga96 -> plagHamincha96Minutes
    PlagHaminchaMethod.Mga96Zmanis -> plagHamincha96MinutesZmanis
    PlagHaminchaMethod.Mga120 -> plagHamincha120Minutes
    PlagHaminchaMethod.Mga120Zmanis -> plagHamincha120MinutesZmanis
    PlagHaminchaMethod.Degrees16Point1 -> plagHamincha16Point1Degrees
    PlagHaminchaMethod.Degrees18 -> plagHamincha18Degrees
    PlagHaminchaMethod.Degrees19Point8 -> plagHamincha19Point8Degrees
    PlagHaminchaMethod.Degrees26 -> plagHamincha26Degrees
    PlagHaminchaMethod.AlotToSunset -> plagAlosToSunset
    PlagHaminchaMethod.Alot16Point1ToTzeit7Point083 -> plagAlos16Point1ToTzaisGeonim7Point083Degrees
    PlagHaminchaMethod.FixedLocal -> plagHaminchaGRAFixedLocalChatzosToSunset
    PlagHaminchaMethod.BaalHatanya -> plagHaminchaBaalHatanya
    PlagHaminchaMethod.AteretTorah -> plagHaminchaAteretTorah
    PlagHaminchaMethod.AhavatShalom -> plagAhavatShalom
}

internal fun ComplexZmanimCalendar.tzeit(settings: ZmanimCalculationSettings): Date? =
    tzeit(settings.tzeitHakochavimMethod)

internal fun ComplexZmanimCalendar.tzeit(method: TzeitHakochavimMethod): Date? = when (method) {
    TzeitHakochavimMethod.Degrees6Point2 -> getSunsetOffsetByDegrees(AstronomicalCalendar.GEOMETRIC_ZENITH + 6.2) ?: tzais50
    TzeitHakochavimMethod.Geonim3Point7 -> tzaisGeonim3Point7Degrees ?: AstronomicalCalendar.getTimeOffset(seaLevelSunset, 18L * 60_000)
    TzeitHakochavimMethod.Geonim3Point8 -> tzaisGeonim3Point8Degrees ?: AstronomicalCalendar.getTimeOffset(seaLevelSunset, 18L * 60_000)
    TzeitHakochavimMethod.Geonim4Point42 -> getSunsetOffsetByDegrees(AstronomicalCalendar.GEOMETRIC_ZENITH + 4.42) ?: AstronomicalCalendar.getTimeOffset(seaLevelSunset, 20L * 60_000)
    TzeitHakochavimMethod.Geonim4Point66 -> getSunsetOffsetByDegrees(AstronomicalCalendar.GEOMETRIC_ZENITH + 4.66) ?: AstronomicalCalendar.getTimeOffset(seaLevelSunset, 20L * 60_000)
    TzeitHakochavimMethod.Geonim4Point8 -> tzaisGeonim4Point8Degrees ?: AstronomicalCalendar.getTimeOffset(seaLevelSunset, 20L * 60_000)
    TzeitHakochavimMethod.Geonim5Point95 -> tzaisGeonim5Point95Degrees ?: AstronomicalCalendar.getTimeOffset(seaLevelSunset, 24L * 60_000)
    TzeitHakochavimMethod.Geonim6Point45 -> tzaisGeonim6Point45Degrees ?: tzais50
    TzeitHakochavimMethod.Geonim7Point083 -> tzaisGeonim7Point083Degrees ?: tzais50
    TzeitHakochavimMethod.Geonim7Point67 -> tzaisGeonim7Point67Degrees ?: tzais50
    TzeitHakochavimMethod.Geonim8Point5 -> tzaisGeonim8Point5Degrees ?: tzais50
    TzeitHakochavimMethod.Geonim9Point3 -> tzaisGeonim9Point3Degrees ?: tzais60
    TzeitHakochavimMethod.Geonim9Point75 -> tzaisGeonim9Point75Degrees ?: tzais60
    TzeitHakochavimMethod.Minutes50 -> tzais50
    TzeitHakochavimMethod.Minutes60 -> tzais60
    TzeitHakochavimMethod.Minutes72 -> tzais72
    TzeitHakochavimMethod.Minutes90 -> tzais90
    TzeitHakochavimMethod.Minutes96 -> tzais96
    TzeitHakochavimMethod.Minutes120 -> tzais120
    TzeitHakochavimMethod.Zmanis72 -> tzais72Zmanis
    TzeitHakochavimMethod.Zmanis90 -> tzais90Zmanis
    TzeitHakochavimMethod.Zmanis96 -> tzais96Zmanis
    TzeitHakochavimMethod.Zmanis120 -> tzais120Zmanis
    TzeitHakochavimMethod.Degrees16Point1 -> tzais16Point1Degrees ?: tzais72
    TzeitHakochavimMethod.Degrees18 -> tzais18Degrees ?: tzais90
    TzeitHakochavimMethod.Degrees19Point8 -> tzais19Point8Degrees ?: tzais96
    TzeitHakochavimMethod.Degrees26 -> tzais26Degrees ?: tzais120
    TzeitHakochavimMethod.AteretTorah -> tzaisAteretTorah
    TzeitHakochavimMethod.BaalHatanya -> tzaisBaalHatanya ?: tzais72
}

internal fun ComplexZmanimCalendar.motzeiShabbat(settings: ZmanimCalculationSettings): Date? = when (settings.motzeiShabbatMethod) {
    MotzeiShabbatMethod.Degrees6Point2 -> getSunsetOffsetByDegrees(AstronomicalCalendar.GEOMETRIC_ZENITH + 6.2) ?: tzais50
    MotzeiShabbatMethod.Geonim3Point7 -> tzaisGeonim3Point7Degrees ?: AstronomicalCalendar.getTimeOffset(seaLevelSunset, 18L * 60_000)
    MotzeiShabbatMethod.Geonim3Point8 -> tzaisGeonim3Point8Degrees ?: AstronomicalCalendar.getTimeOffset(seaLevelSunset, 18L * 60_000)
    MotzeiShabbatMethod.Geonim4Point42 -> getSunsetOffsetByDegrees(AstronomicalCalendar.GEOMETRIC_ZENITH + 4.42) ?: AstronomicalCalendar.getTimeOffset(seaLevelSunset, 20L * 60_000)
    MotzeiShabbatMethod.Geonim4Point66 -> getSunsetOffsetByDegrees(AstronomicalCalendar.GEOMETRIC_ZENITH + 4.66) ?: AstronomicalCalendar.getTimeOffset(seaLevelSunset, 20L * 60_000)
    MotzeiShabbatMethod.Geonim4Point8 -> tzaisGeonim4Point8Degrees ?: AstronomicalCalendar.getTimeOffset(seaLevelSunset, 20L * 60_000)
    MotzeiShabbatMethod.Geonim5Point95 -> tzaisGeonim5Point95Degrees ?: AstronomicalCalendar.getTimeOffset(seaLevelSunset, 24L * 60_000)
    MotzeiShabbatMethod.Geonim6Point45 -> tzaisGeonim6Point45Degrees ?: tzais50
    MotzeiShabbatMethod.Geonim7Point083 -> tzaisGeonim7Point083Degrees ?: tzais50
    MotzeiShabbatMethod.Geonim7Point67 -> tzaisGeonim7Point67Degrees ?: tzais50
    MotzeiShabbatMethod.Geonim8Point5 -> tzaisGeonim8Point5Degrees ?: tzais50
    MotzeiShabbatMethod.Geonim9Point3 -> tzaisGeonim9Point3Degrees ?: tzais60
    MotzeiShabbatMethod.Geonim9Point75 -> tzaisGeonim9Point75Degrees ?: tzais60
    MotzeiShabbatMethod.Minutes50 -> tzais50
    MotzeiShabbatMethod.Minutes60 -> tzais60
    MotzeiShabbatMethod.Minutes72 -> tzais72
    MotzeiShabbatMethod.Minutes90 -> tzais90
    MotzeiShabbatMethod.Minutes96 -> tzais96
    MotzeiShabbatMethod.Minutes120 -> tzais120
    MotzeiShabbatMethod.Zmanis72 -> tzais72Zmanis
    MotzeiShabbatMethod.Zmanis90 -> tzais90Zmanis
    MotzeiShabbatMethod.Zmanis96 -> tzais96Zmanis
    MotzeiShabbatMethod.Zmanis120 -> tzais120Zmanis
    MotzeiShabbatMethod.Degrees16Point1 -> tzais16Point1Degrees ?: tzais72
    MotzeiShabbatMethod.Degrees18 -> tzais18Degrees ?: tzais90
    MotzeiShabbatMethod.Degrees19Point8 -> tzais19Point8Degrees ?: tzais96
    MotzeiShabbatMethod.Degrees26 -> tzais26Degrees ?: tzais120
    MotzeiShabbatMethod.AteretTorah -> tzaisAteretTorah
    MotzeiShabbatMethod.BaalHatanya -> tzaisBaalHatanya ?: tzais72
}

internal fun ComplexZmanimCalendar.rabbeinuTam(method: RabbeinuTamMethod): Date? = when (method) {
    RabbeinuTamMethod.Minutes72 -> tzais72
    RabbeinuTamMethod.Minutes90 -> tzais90
    RabbeinuTamMethod.Minutes120 -> tzais120
    RabbeinuTamMethod.Zmanis72 -> tzais72Zmanis
    RabbeinuTamMethod.Degrees16Point1 -> tzais16Point1Degrees
    RabbeinuTamMethod.Degrees18 -> tzais18Degrees
    RabbeinuTamMethod.Degrees19Point8 -> tzais19Point8Degrees
    RabbeinuTamMethod.Degrees26 -> tzais26Degrees
    RabbeinuTamMethod.BainHashmashot13Point24 -> bainHashmashosRT13Point24Degrees
    RabbeinuTamMethod.BainHashmashot58Point5 -> bainHashmashosRT58Point5Minutes
    RabbeinuTamMethod.BainHashmashot13Point5Before7Point083 -> bainHashmashosRT13Point5MinutesBefore7Point083Degrees
    RabbeinuTamMethod.BainHashmashot2Stars -> bainHashmashosRT2Stars
}

internal fun ComplexZmanimCalendar.chametzTimes(method: ChametzMethod): Pair<Date?, Date?> = when (method) {
    ChametzMethod.Gra -> sofZmanAchilasChametzGRA to sofZmanBiurChametzGRA
    ChametzMethod.Mga72 -> sofZmanAchilasChametzMGA72Minutes to sofZmanBiurChametzMGA72Minutes
    ChametzMethod.Mga72Zmanis -> {
        val alos = alos72Zmanis
        val shaahZmanis = shaahZmanis72MinutesZmanis
        val achilah = alos?.let { AstronomicalCalendar.getTimeOffset(it, shaahZmanis * 4) }
        val biur = alos?.let { AstronomicalCalendar.getTimeOffset(it, shaahZmanis * 5) }
        achilah to biur
    }
    ChametzMethod.Mga16Point1 -> sofZmanAchilasChametzMGA16Point1Degrees to sofZmanBiurChametzMGA16Point1Degrees
    ChametzMethod.BaalHatanya -> sofZmanAchilasChametzBaalHatanya to sofZmanBiurChametzBaalHatanya
}