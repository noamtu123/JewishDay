package com.noamtu.jewishday.model

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
        AlotHashacharMethod.Degrees16Point1 -> alos16Point1Degrees
        AlotHashacharMethod.Degrees18 -> alos18Degrees
        AlotHashacharMethod.Degrees19 -> alos19Degrees
        AlotHashacharMethod.Degrees19Point8 -> alos19Point8Degrees
        AlotHashacharMethod.Degrees26 -> alos26Degrees
        AlotHashacharMethod.BaalHatanya -> alosBaalHatanya
    }.withHighLatitudeFallback(settings) { alos72 }

internal fun ComplexZmanimCalendar.misheyakir(method: MisheyakirMethod): Date? = when (method) {
    MisheyakirMethod.Degrees7Point65 -> misheyakir7Point65Degrees
    MisheyakirMethod.Degrees9Point5 -> misheyakir9Point5Degrees
    MisheyakirMethod.Degrees10Point2 -> misheyakir10Point2Degrees
    MisheyakirMethod.Degrees11 -> misheyakir11Degrees
    MisheyakirMethod.Degrees11Point5 -> misheyakir11Point5Degrees
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
        SofZmanShemaMethod.Gra -> sofZmanShmaGRA
        SofZmanShemaMethod.Mga72 -> sofZmanShmaMGA72Minutes
        SofZmanShemaMethod.Mga72Zmanis -> sofZmanShmaMGA72MinutesZmanis
        SofZmanShemaMethod.Mga90 -> sofZmanShmaMGA90Minutes
        SofZmanShemaMethod.Mga90Zmanis -> sofZmanShmaMGA90MinutesZmanis
        SofZmanShemaMethod.Mga96 -> sofZmanShmaMGA96Minutes
        SofZmanShemaMethod.Mga96Zmanis -> sofZmanShmaMGA96MinutesZmanis
        SofZmanShemaMethod.Mga120 -> sofZmanShmaMGA120Minutes
        SofZmanShemaMethod.Mga16Point1 -> sofZmanShmaMGA16Point1Degrees
        SofZmanShemaMethod.Mga18 -> sofZmanShmaMGA18Degrees
        SofZmanShemaMethod.Mga19Point8 -> sofZmanShmaMGA19Point8Degrees
        SofZmanShemaMethod.Alos16Point1ToSunset -> sofZmanShmaAlos16Point1ToSunset
        SofZmanShemaMethod.Alos16Point1ToTzeit7Point083 -> sofZmanShmaAlos16Point1ToTzaisGeonim7Point083Degrees
        SofZmanShemaMethod.ThreeHoursBeforeChatzot -> sofZmanShma3HoursBeforeChatzos
        SofZmanShemaMethod.FixedLocal -> sofZmanShmaFixedLocal
        SofZmanShemaMethod.FixedLocalGra -> sofZmanShmaGRASunriseToFixedLocalChatzos
        SofZmanShemaMethod.Mga18ToFixedLocalChatzot -> sofZmanShmaMGA18DegreesToFixedLocalChatzos
        SofZmanShemaMethod.Mga16Point1ToFixedLocalChatzot -> sofZmanShmaMGA16Point1DegreesToFixedLocalChatzos
        SofZmanShemaMethod.Mga90ToFixedLocalChatzot -> sofZmanShmaMGA90MinutesToFixedLocalChatzos
        SofZmanShemaMethod.Mga72ToFixedLocalChatzot -> sofZmanShmaMGA72MinutesToFixedLocalChatzos
        SofZmanShemaMethod.BaalHatanya -> sofZmanShmaBaalHatanya
        SofZmanShemaMethod.AteretTorah -> sofZmanShmaAteretTorah
        SofZmanShemaMethod.KolEliyahu -> sofZmanShmaKolEliyahu
    }.withHighLatitudeFallback(settings) { sofZmanShmaMGA72Minutes }

internal fun ComplexZmanimCalendar.sofZmanTefillah(
    method: SofZmanTefillahMethod,
    settings: ZmanimCalculationSettings,
): Date? =
    when (method) {
        SofZmanTefillahMethod.Gra -> sofZmanTfilaGRA
        SofZmanTefillahMethod.Mga72 -> sofZmanTfilaMGA72Minutes
        SofZmanTefillahMethod.Mga72Zmanis -> sofZmanTfilaMGA72MinutesZmanis
        SofZmanTefillahMethod.Mga90 -> sofZmanTfilaMGA90Minutes
        SofZmanTefillahMethod.Mga90Zmanis -> sofZmanTfilaMGA90MinutesZmanis
        SofZmanTefillahMethod.Mga96 -> sofZmanTfilaMGA96Minutes
        SofZmanTefillahMethod.Mga96Zmanis -> sofZmanTfilaMGA96MinutesZmanis
        SofZmanTefillahMethod.Mga120 -> sofZmanTfilaMGA120Minutes
        SofZmanTefillahMethod.Mga16Point1 -> sofZmanTfilaMGA16Point1Degrees
        SofZmanTefillahMethod.Mga18 -> sofZmanTfilaMGA18Degrees
        SofZmanTefillahMethod.Mga19Point8 -> sofZmanTfilaMGA19Point8Degrees
        SofZmanTefillahMethod.TwoHoursBeforeChatzot -> sofZmanTfila2HoursBeforeChatzos
        SofZmanTefillahMethod.FixedLocal -> sofZmanTfilaFixedLocal
        SofZmanTefillahMethod.FixedLocalGra -> sofZmanTfilaGRASunriseToFixedLocalChatzos
        SofZmanTefillahMethod.BaalHatanya -> sofZmanTfilaBaalHatanya
        SofZmanTefillahMethod.AteretTorah -> sofZmanTfilahAteretTorah
    }.withHighLatitudeFallback(settings) { sofZmanTfilaMGA72Minutes }

internal fun ComplexZmanimCalendar.chatzot(method: ChatzotMethod): Date? = when (method) {
    ChatzotMethod.Solar -> chatzos
    ChatzotMethod.FixedLocal -> fixedLocalChatzos
}

internal fun ComplexZmanimCalendar.minchaGedola(settings: ZmanimCalculationSettings): Date? = when (settings.minchaGedolaMethod) {
    MinchaGedolaMethod.Standard -> minchaGedola
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
    MinchaKetanaMethod.Standard -> minchaKetana
    MinchaKetanaMethod.Mga72 -> minchaKetana72Minutes
    MinchaKetanaMethod.Degrees16Point1 -> minchaKetana16Point1Degrees
    MinchaKetanaMethod.FixedLocal -> minchaKetanaGRAFixedLocalChatzosToSunset
    MinchaKetanaMethod.BaalHatanya -> minchaKetanaBaalHatanya
    MinchaKetanaMethod.AteretTorah -> minchaKetanaAteretTorah
    MinchaKetanaMethod.AhavatShalom -> minchaKetanaAhavatShalom
}

internal fun ComplexZmanimCalendar.plagHamincha(settings: ZmanimCalculationSettings): Date? = when (settings.plagHaminchaMethod) {
    PlagHaminchaMethod.Gra -> plagHamincha
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
    tzeit(settings.tzeitHakochavimMethod).withHighLatitudeFallback(settings) { tzais72 }

internal fun ComplexZmanimCalendar.tzeit(method: TzeitHakochavimMethod): Date? = when (method) {
    TzeitHakochavimMethod.Geonim3Point7 -> tzaisGeonim3Point7Degrees
    TzeitHakochavimMethod.Geonim3Point8 -> tzaisGeonim3Point8Degrees
    TzeitHakochavimMethod.Geonim3Point65 -> tzaisGeonim3Point65Degrees
    TzeitHakochavimMethod.Geonim3Point676 -> tzaisGeonim3Point676Degrees
    TzeitHakochavimMethod.Geonim4Point37 -> tzaisGeonim4Point37Degrees
    TzeitHakochavimMethod.Geonim4Point61 -> tzaisGeonim4Point61Degrees
    TzeitHakochavimMethod.Geonim4Point8 -> tzaisGeonim4Point8Degrees
    TzeitHakochavimMethod.Geonim5Point88 -> tzaisGeonim5Point88Degrees
    TzeitHakochavimMethod.Geonim5Point95 -> tzaisGeonim5Point95Degrees
    TzeitHakochavimMethod.Geonim6Point45 -> tzaisGeonim6Point45Degrees
    TzeitHakochavimMethod.Geonim7Point083 -> tzaisGeonim7Point083Degrees
    TzeitHakochavimMethod.Geonim7Point67 -> tzaisGeonim7Point67Degrees
    TzeitHakochavimMethod.Geonim8Point5 -> tzaisGeonim8Point5Degrees
    TzeitHakochavimMethod.Geonim9Point3 -> tzaisGeonim9Point3Degrees
    TzeitHakochavimMethod.Geonim9Point75 -> tzaisGeonim9Point75Degrees
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
    TzeitHakochavimMethod.Degrees16Point1 -> tzais16Point1Degrees
    TzeitHakochavimMethod.Degrees18 -> tzais18Degrees
    TzeitHakochavimMethod.Degrees19Point8 -> tzais19Point8Degrees
    TzeitHakochavimMethod.Degrees26 -> tzais26Degrees
    TzeitHakochavimMethod.BaalHatanya -> tzaisBaalHatanya
    TzeitHakochavimMethod.AteretTorah -> tzaisAteretTorah
}

internal fun ComplexZmanimCalendar.motzeiShabbat(settings: ZmanimCalculationSettings): Date? = when (settings.motzeiShabbatMethod) {
    MotzeiShabbatMethod.Geonim5Point88 -> tzaisGeonim5Point88Degrees
    MotzeiShabbatMethod.Geonim7Point083 -> tzaisGeonim7Point083Degrees
    MotzeiShabbatMethod.Geonim8Point5 -> tzaisGeonim8Point5Degrees
    MotzeiShabbatMethod.Geonim9Point3 -> tzaisGeonim9Point3Degrees
    MotzeiShabbatMethod.Minutes50 -> tzais50
    MotzeiShabbatMethod.Minutes60 -> tzais60
    MotzeiShabbatMethod.Minutes72 -> tzais72
    MotzeiShabbatMethod.Minutes90 -> tzais90
    MotzeiShabbatMethod.Minutes96 -> tzais96
    MotzeiShabbatMethod.Minutes120 -> tzais120
    MotzeiShabbatMethod.RabbeinuTam72 -> tzais72
    MotzeiShabbatMethod.RabbeinuTam90 -> tzais90
    MotzeiShabbatMethod.RabbeinuTam120 -> tzais120
    MotzeiShabbatMethod.BaalHatanya -> tzaisBaalHatanya
    MotzeiShabbatMethod.AteretTorah -> tzaisAteretTorah
}.withHighLatitudeFallback(settings) { tzais72 }

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

internal fun ComplexZmanimCalendar.bainHashmashot(method: BainHashmashotMethod): Date? = when (method) {
    BainHashmashotMethod.RabbeinuTam13Point24 -> bainHashmashosRT13Point24Degrees
    BainHashmashotMethod.RabbeinuTam58Point5 -> bainHashmashosRT58Point5Minutes
    BainHashmashotMethod.RabbeinuTam13Point5Before7Point083 -> bainHashmashosRT13Point5MinutesBefore7Point083Degrees
    BainHashmashotMethod.RabbeinuTam2Stars -> bainHashmashosRT2Stars
    BainHashmashotMethod.Yereim18Minutes -> bainHashmashosYereim18Minutes
    BainHashmashotMethod.Yereim3Point05 -> bainHashmashosYereim3Point05Degrees
    BainHashmashotMethod.Yereim16Point875Minutes -> bainHashmashosYereim16Point875Minutes
    BainHashmashotMethod.Yereim2Point8 -> bainHashmashosYereim2Point8Degrees
    BainHashmashotMethod.Yereim13Point5Minutes -> bainHashmashosYereim13Point5Minutes
    BainHashmashotMethod.Yereim2Point1 -> bainHashmashosYereim2Point1Degrees
}

private fun Date?.withHighLatitudeFallback(
    settings: ZmanimCalculationSettings,
    fallback: () -> Date?,
): Date? = this ?: if (settings.highLatitudeHandling == HighLatitudeHandling.FixedMinutesFallback) fallback() else null

internal fun ComplexZmanimCalendar.fastDayTimes(method: FastDayMethod): Pair<Date?, Date?> = when (method) {
    FastDayMethod.Alot72ToTzeit8Point5 -> alos72 to tzaisGeonim8Point5Degrees
    FastDayMethod.Alot72ToTzeit7Point083 -> alos72 to tzaisGeonim7Point083Degrees
    FastDayMethod.Alot72ToTzeit5Point88 -> alos72 to tzaisGeonim5Point88Degrees
    FastDayMethod.Alot16Point1ToTzeit8Point5 -> alos16Point1Degrees to tzaisGeonim8Point5Degrees
    FastDayMethod.Alot16Point1ToTzeit7Point083 -> alos16Point1Degrees to tzaisGeonim7Point083Degrees
    FastDayMethod.BaalHatanya -> alosBaalHatanya to tzaisBaalHatanya
}

internal fun ComplexZmanimCalendar.chametzTimes(method: ChametzMethod): Pair<Date?, Date?> = when (method) {
    ChametzMethod.Gra -> sofZmanAchilasChametzGRA to sofZmanBiurChametzGRA
    ChametzMethod.Mga72 -> sofZmanAchilasChametzMGA72Minutes to sofZmanBiurChametzMGA72Minutes
    ChametzMethod.Mga16Point1 -> sofZmanAchilasChametzMGA16Point1Degrees to sofZmanBiurChametzMGA16Point1Degrees
    ChametzMethod.BaalHatanya -> sofZmanAchilasChametzBaalHatanya to sofZmanBiurChametzBaalHatanya
}
