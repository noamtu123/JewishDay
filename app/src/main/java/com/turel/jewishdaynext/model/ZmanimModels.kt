package com.turel.jewishdaynext.model

import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.YerushalmiYomiCalculator
import com.kosherjava.zmanim.util.GeoLocation
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone

data class ZmanItem(
    val title: String,
    val titleHebrew: String,
    val time: Instant?,
    val description: String,
    val descriptionHebrew: String,
    val value: String? = null,
    val valueHebrew: String? = value,
)

data class ZmanimDay(
    val locationName: String,
    val date: LocalDate,
    val zoneId: ZoneId,
    val groups: List<ZmanimGroup>,
)

data class ZmanimGroup(
    val title: String,
    val titleHebrew: String,
    val items: List<ZmanItem>,
)

fun ZmanimDay.withDailyLearningItems(items: List<ZmanItem>): ZmanimDay = copy(
    groups = groups.map { group ->
        if (group.title == DailyLearningGroupTitle) {
            group.copy(items = items.ifEmpty { group.items })
        } else {
            group
        }
    },
)

fun zmanimForDate(
    location: JewishLocation = defaultJerusalemLocation,
    date: LocalDate,
    settings: ZmanimCalculationSettings = ZmanimCalculationSettings(),
): ZmanimDay {
    val calendar = complexZmanimCalendar(location, date, settings)
    val jewishCalendar = JewishCalendar(date).apply {
        isUseModernHolidays = true
        setInIsrael(settings.inIsrael)
    }
    val englishFormatter = HebrewDateFormatter()
    val hebrewFormatter = HebrewDateFormatter().apply { isHebrewFormat = true }
    val shabbatDates = shabbatDatesFor(date)
    val shabbatStartCalendar = complexZmanimCalendar(location, shabbatDates.startDate, settings)
    val shabbatEndCalendar = complexZmanimCalendar(location, shabbatDates.endDate, settings)

    return ZmanimDay(
        locationName = location.name,
        date = date,
        zoneId = location.zoneId,
        groups = listOf(
            ZmanimGroup(
                title = "Daily",
                titleHebrew = "יומי",
                items = dailyItems(
                    jewishCalendar = jewishCalendar,
                    englishFormatter = englishFormatter,
                    hebrewFormatter = hebrewFormatter,
                    calendar = calendar,
                    settings = settings,
                ),
            ),
            ZmanimGroup(
                title = "Morning",
                titleHebrew = "בוקר",
                items = buildList {
                    add(ZmanItem("Alot Hashachar", "עלות השחר", calendar.alotHashachar(settings)?.toInstant(), settings.alotHashacharMethod.label, settings.alotHashacharMethod.labelHebrew))
                    add(ZmanItem("Misheyakir", "משיכיר", calendar.misheyakir(settings.misheyakirMethod)?.toInstant(), settings.misheyakirMethod.label, settings.misheyakirMethod.labelHebrew))
                    add(ZmanItem("Sunrise", "הנץ החמה", calendar.sunrise(settings.sunriseMethod)?.toInstant(), settings.sunriseMethod.label, settings.sunriseMethod.labelHebrew))
                    add(ZmanItem("Shema", "קריאת שמע", calendar.sofZmanShema(settings)?.toInstant(), settings.sofZmanShemaMethod.label, settings.sofZmanShemaMethod.labelHebrew))
                    if (settings.sofZmanShemaMethod != SofZmanShemaMethod.Mga72) {
                        add(ZmanItem("Shema Magen Avraham", "קריאת שמע מגן אברהם", calendar.sofZmanShmaMGA72Minutes?.toInstant(), "Magen Avraham 72", "מגן אברהם 72"))
                    }
                    add(ZmanItem("Tefillah", "תפילה", calendar.sofZmanTefillah(settings)?.toInstant(), settings.sofZmanTefillahMethod.label, settings.sofZmanTefillahMethod.labelHebrew))
                    if (settings.sofZmanTefillahMethod != SofZmanTefillahMethod.Mga72) {
                        add(ZmanItem("Tefillah Magen Avraham", "תפילה מגן אברהם", calendar.sofZmanTfilaMGA72Minutes?.toInstant(), "Magen Avraham 72", "מגן אברהם 72"))
                    }
                },
            ),
            ZmanimGroup(
                title = "Afternoon & Evening",
                titleHebrew = "צהריים וערב",
                items = listOf(
                    ZmanItem("Chatzot", "חצות", calendar.chatzot(settings.chatzotMethod)?.toInstant(), settings.chatzotMethod.label, settings.chatzotMethod.labelHebrew),
                    ZmanItem("Mincha Gedola", "מנחה גדולה", calendar.minchaGedola(settings)?.toInstant(), settings.minchaGedolaMethod.label, settings.minchaGedolaMethod.labelHebrew),
                    ZmanItem("Samuch LeMincha Ketana", "סמוך למנחה קטנה", calendar.samuchLeMinchaKetana(settings.samuchLeMinchaKetanaMethod)?.toInstant(), settings.samuchLeMinchaKetanaMethod.label, settings.samuchLeMinchaKetanaMethod.labelHebrew),
                    ZmanItem("Mincha Ketana", "מנחה קטנה", calendar.minchaKetana(settings)?.toInstant(), settings.minchaKetanaMethod.label, settings.minchaKetanaMethod.labelHebrew),
                    ZmanItem("Plag Hamincha", "פלג המנחה", calendar.plagHamincha(settings)?.toInstant(), settings.plagHaminchaMethod.label, settings.plagHaminchaMethod.labelHebrew),
                    ZmanItem("Sunset", "שקיעה", calendar.sunset(settings.sunsetMethod)?.toInstant(), settings.sunsetMethod.label, settings.sunsetMethod.labelHebrew),
                    ZmanItem("Tzeit", "צאת הכוכבים", calendar.tzeit(settings)?.toInstant(), settings.tzeitHakochavimMethod.label, settings.tzeitHakochavimMethod.labelHebrew),
                ),
            ),
            ZmanimGroup(
                title = "Shabbat",
                titleHebrew = "שבת",
                items = listOf(
                    ZmanItem("Candle Lighting", "הדלקת נרות", shabbatStartCalendar.candleLighting?.toInstant(), "Friday ${shabbatDates.startDate}; ${settings.candleLightingMethod.label}", "יום שישי ${shabbatDates.startDate}; ${settings.candleLightingMethod.labelHebrew}"),
                    ZmanItem("Plag Hamincha", "פלג המנחה", shabbatStartCalendar.plagHamincha(settings)?.toInstant(), "Friday ${shabbatDates.startDate}; earliest Shabbat boundary", "יום שישי ${shabbatDates.startDate}; גבול מוקדם לשבת"),
                    ZmanItem("Bain Hashmashot", "בין השמשות", shabbatStartCalendar.bainHashmashot(settings.bainHashmashotMethod)?.toInstant(), "Friday ${shabbatDates.startDate}; ${settings.bainHashmashotMethod.label}", "יום שישי ${shabbatDates.startDate}; ${settings.bainHashmashotMethod.labelHebrew}"),
                    ZmanItem("Motzei Shabbat", "צאת שבת", shabbatEndCalendar.motzeiShabbat(settings)?.toInstant(), "Saturday ${shabbatDates.endDate}; ${settings.motzeiShabbatMethod.label}", "מוצאי שבת ${shabbatDates.endDate}; ${settings.motzeiShabbatMethod.labelHebrew}"),
                    ZmanItem("Rabbeinu Tam", "רבינו תם", shabbatEndCalendar.rabbeinuTam(settings.rabbeinuTamMethod)?.toInstant(), "Saturday ${shabbatDates.endDate}; ${settings.rabbeinuTamMethod.label}", "מוצאי שבת ${shabbatDates.endDate}; ${settings.rabbeinuTamMethod.labelHebrew}"),
                ),
            ),
            ZmanimGroup(
                title = DailyLearningGroupTitle,
                titleHebrew = "לימוד יומי",
                items = dailyLearningItems(
                    jewishCalendar = jewishCalendar,
                    englishFormatter = englishFormatter,
                    hebrewFormatter = hebrewFormatter,
                ),
            ),
            ZmanimGroup(
                title = "Location",
                titleHebrew = "מיקום",
                items = listOf(
                    ZmanItem("Current Location", "מיקום נוכחי", null, "Automatic device location", "מיקום אוטומטי מהמכשיר", location.name, location.name),
                    ZmanItem("Coordinates", "קואורדינטות", null, "Latitude, longitude", "קו רוחב וקו אורך", "%.4f, %.4f".format(location.latitude, location.longitude), "%.4f, %.4f".format(location.latitude, location.longitude)),
                ),
            ),
        ),
    )
}

private const val DailyLearningGroupTitle = "Daily Learning"

private data class ShabbatDates(
    val startDate: LocalDate,
    val endDate: LocalDate,
)

private fun shabbatDatesFor(date: LocalDate): ShabbatDates {
    val friday = if (date.dayOfWeek == DayOfWeek.SATURDAY) {
        date.minusDays(1)
    } else {
        date.plusDays(daysUntil(date.dayOfWeek, DayOfWeek.FRIDAY).toLong())
    }
    return ShabbatDates(startDate = friday, endDate = friday.plusDays(1))
}

private fun daysUntil(current: DayOfWeek, target: DayOfWeek): Int =
    (target.value - current.value + 7) % 7

fun tzeitForDate(
    location: JewishLocation = defaultJerusalemLocation,
    date: LocalDate,
    settings: ZmanimCalculationSettings = ZmanimCalculationSettings(),
): Instant? = complexZmanimCalendar(location, date, settings)
    .tzeit(settings)
    ?.toInstant()

private fun complexZmanimCalendar(
    location: JewishLocation,
    date: LocalDate,
    settings: ZmanimCalculationSettings,
): ComplexZmanimCalendar {
    val timeZone = TimeZone.getTimeZone(location.zoneId)
    val geoLocation = GeoLocation(
        location.name,
        location.latitude,
        location.longitude,
        location.elevationMeters,
        timeZone,
    )
    val calculationDate = GregorianCalendar(timeZone).apply {
        clear()
        set(date.year, date.monthValue - 1, date.dayOfMonth)
    }
    return ComplexZmanimCalendar(geoLocation).apply {
        setCalendar(calculationDate)
        candleLightingOffset = settings.candleLightingMethod.offsetMinutes.toDouble()
        ateretTorahSunsetOffset = settings.ateretTorahSunsetOffsetMinutes.toDouble()
    }
}

private fun dailyLearningItems(
    jewishCalendar: JewishCalendar,
    englishFormatter: HebrewDateFormatter,
    hebrewFormatter: HebrewDateFormatter,
): List<ZmanItem> = buildList {
    add(
        ZmanItem(
            title = "Daf Yomi Bavli",
            titleHebrew = "דף יומי בבלי",
            time = null,
            description = "KosherJava Daf Yomi cycle",
            descriptionHebrew = "מחזור דף יומי של KosherJava",
            value = englishFormatter.formatDafYomiBavli(jewishCalendar.dafYomiBavli),
            valueHebrew = hebrewFormatter.formatDafYomiBavli(jewishCalendar.dafYomiBavli),
        ),
    )
    val yerushalmiDaf = runCatching { YerushalmiYomiCalculator.getDafYomiYerushalmi(jewishCalendar) }.getOrNull()
    if (yerushalmiDaf != null) {
        add(
            ZmanItem(
                title = "Daf Yomi Yerushalmi",
                titleHebrew = "דף יומי ירושלמי",
                time = null,
                description = "KosherJava Yerushalmi cycle",
                descriptionHebrew = "מחזור ירושלמי של KosherJava",
                value = englishFormatter.formatDafYomiYerushalmi(yerushalmiDaf),
                valueHebrew = hebrewFormatter.formatDafYomiYerushalmi(yerushalmiDaf),
            ),
        )
    }
    add(
        ZmanItem(
            title = "Tehillim Yomi",
            titleHebrew = "תהילים יומי",
            time = null,
            description = "Monthly Tehillim division by Hebrew date",
            descriptionHebrew = "חלוקה חודשית לפי היום בחודש העברי",
            value = jewishCalendar.tehillimYomiEnglish(),
            valueHebrew = jewishCalendar.tehillimYomiHebrew(hebrewFormatter),
        ),
    )
}

private fun JewishCalendar.tehillimYomiEnglish(): String {
    val range = tehillimRangeForDay(jewishDayOfMonth, daysInJewishMonth)
    return if (range.verseStart == null) {
        "Psalms ${range.chapterStart}-${range.chapterEnd}"
    } else {
        "Psalm ${range.chapterStart}:${range.verseStart}-${range.verseEnd}"
    }
}

private fun JewishCalendar.tehillimYomiHebrew(formatter: HebrewDateFormatter): String {
    val range = tehillimRangeForDay(jewishDayOfMonth, daysInJewishMonth)
    return if (range.verseStart == null) {
        "תהילים ${formatter.formatHebrewNumber(range.chapterStart)}-${formatter.formatHebrewNumber(range.chapterEnd)}"
    } else {
        "תהילים ${formatter.formatHebrewNumber(range.chapterStart)}:${formatter.formatHebrewNumber(range.verseStart)}-${formatter.formatHebrewNumber(range.verseEnd ?: range.verseStart)}"
    }
}

private data class TehillimRange(
    val chapterStart: Int,
    val chapterEnd: Int,
    val verseStart: Int? = null,
    val verseEnd: Int? = null,
)

private fun tehillimRangeForDay(dayOfMonth: Int, daysInMonth: Int): TehillimRange = when (dayOfMonth) {
    1 -> TehillimRange(1, 9)
    2 -> TehillimRange(10, 17)
    3 -> TehillimRange(18, 22)
    4 -> TehillimRange(23, 28)
    5 -> TehillimRange(29, 34)
    6 -> TehillimRange(35, 38)
    7 -> TehillimRange(39, 43)
    8 -> TehillimRange(44, 48)
    9 -> TehillimRange(49, 54)
    10 -> TehillimRange(55, 59)
    11 -> TehillimRange(60, 65)
    12 -> TehillimRange(66, 68)
    13 -> TehillimRange(69, 71)
    14 -> TehillimRange(72, 76)
    15 -> TehillimRange(77, 78)
    16 -> TehillimRange(79, 82)
    17 -> TehillimRange(83, 87)
    18 -> TehillimRange(88, 89)
    19 -> TehillimRange(90, 96)
    20 -> TehillimRange(97, 103)
    21 -> TehillimRange(104, 105)
    22 -> TehillimRange(106, 107)
    23 -> TehillimRange(108, 112)
    24 -> TehillimRange(113, 118)
    25 -> TehillimRange(119, 119, 1, 96)
    26 -> TehillimRange(119, 119, 97, 176)
    27 -> TehillimRange(120, 134)
    28 -> TehillimRange(135, 139)
    29 -> if (daysInMonth == 29) TehillimRange(140, 150) else TehillimRange(140, 144)
    else -> TehillimRange(145, 150)
}

private fun ComplexZmanimCalendar.alotHashachar(settings: ZmanimCalculationSettings): Date? =
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

private fun ComplexZmanimCalendar.misheyakir(method: MisheyakirMethod): Date? = when (method) {
    MisheyakirMethod.Degrees7Point65 -> misheyakir7Point65Degrees
    MisheyakirMethod.Degrees9Point5 -> misheyakir9Point5Degrees
    MisheyakirMethod.Degrees10Point2 -> misheyakir10Point2Degrees
    MisheyakirMethod.Degrees11 -> misheyakir11Degrees
    MisheyakirMethod.Degrees11Point5 -> misheyakir11Point5Degrees
}

private fun ComplexZmanimCalendar.sunrise(method: SunriseMethod): Date? = when (method) {
    SunriseMethod.SeaLevel -> seaLevelSunrise
    SunriseMethod.ElevationAdjusted -> sunrise
}

private fun ComplexZmanimCalendar.sunset(method: SunsetMethod): Date? = when (method) {
    SunsetMethod.SeaLevel -> seaLevelSunset
    SunsetMethod.ElevationAdjusted -> sunset
}

private fun ComplexZmanimCalendar.sofZmanShema(settings: ZmanimCalculationSettings): Date? =
    when (settings.sofZmanShemaMethod) {
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

private fun ComplexZmanimCalendar.sofZmanTefillah(settings: ZmanimCalculationSettings): Date? =
    when (settings.sofZmanTefillahMethod) {
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

private fun ComplexZmanimCalendar.chatzot(method: ChatzotMethod): Date? = when (method) {
    ChatzotMethod.Solar -> chatzos
    ChatzotMethod.FixedLocal -> fixedLocalChatzos
}

private fun ComplexZmanimCalendar.minchaGedola(settings: ZmanimCalculationSettings): Date? = when (settings.minchaGedolaMethod) {
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

private fun ComplexZmanimCalendar.samuchLeMinchaKetana(method: SamuchLeMinchaKetanaMethod): Date? = when (method) {
    SamuchLeMinchaKetanaMethod.Gra -> samuchLeMinchaKetanaGRA
    SamuchLeMinchaKetanaMethod.Mga72 -> samuchLeMinchaKetana72Minutes
    SamuchLeMinchaKetanaMethod.Degrees16Point1 -> samuchLeMinchaKetana16Point1Degrees
}

private fun ComplexZmanimCalendar.minchaKetana(settings: ZmanimCalculationSettings): Date? = when (settings.minchaKetanaMethod) {
    MinchaKetanaMethod.Standard -> minchaKetana
    MinchaKetanaMethod.Mga72 -> minchaKetana72Minutes
    MinchaKetanaMethod.Degrees16Point1 -> minchaKetana16Point1Degrees
    MinchaKetanaMethod.FixedLocal -> minchaKetanaGRAFixedLocalChatzosToSunset
    MinchaKetanaMethod.BaalHatanya -> minchaKetanaBaalHatanya
    MinchaKetanaMethod.AteretTorah -> minchaKetanaAteretTorah
    MinchaKetanaMethod.AhavatShalom -> minchaKetanaAhavatShalom
}

private fun ComplexZmanimCalendar.plagHamincha(settings: ZmanimCalculationSettings): Date? = when (settings.plagHaminchaMethod) {
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

private fun ComplexZmanimCalendar.tzeit(settings: ZmanimCalculationSettings): Date? =
    tzeit(settings.tzeitHakochavimMethod).withHighLatitudeFallback(settings) { tzais72 }

private fun ComplexZmanimCalendar.tzeit(method: TzeitHakochavimMethod): Date? = when (method) {
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

private fun ComplexZmanimCalendar.motzeiShabbat(settings: ZmanimCalculationSettings): Date? = when (settings.motzeiShabbatMethod) {
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

private fun ComplexZmanimCalendar.rabbeinuTam(method: RabbeinuTamMethod): Date? = when (method) {
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

private fun ComplexZmanimCalendar.bainHashmashot(method: BainHashmashotMethod): Date? = when (method) {
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

private fun dailyItems(
    jewishCalendar: JewishCalendar,
    englishFormatter: HebrewDateFormatter,
    hebrewFormatter: HebrewDateFormatter,
    calendar: ComplexZmanimCalendar,
    settings: ZmanimCalculationSettings,
): List<ZmanItem> = buildList {
    add(
        ZmanItem(
            title = "Jewish Date",
            titleHebrew = "תאריך עברי",
            time = null,
            description = "Today after nightfall follows the next Jewish date",
            descriptionHebrew = "אחרי צאת הכוכבים מתחיל התאריך הבא",
            value = englishFormatter.format(jewishCalendar),
            valueHebrew = hebrewFormatter.format(jewishCalendar),
        ),
    )
    val parsha = englishFormatter.formatParsha(jewishCalendar)
    if (parsha.isNotBlank()) {
        add(
            ZmanItem("Weekly Parsha", "פרשת השבוע", null, "Upcoming Torah reading", "קריאת התורה הקרובה", parsha, hebrewFormatter.formatParsha(jewishCalendar)),
        )
    }
    val yomTov = englishFormatter.formatYomTov(jewishCalendar)
    if (yomTov.isNotBlank()) {
        add(ZmanItem("Yom Tov", "יום טוב", null, "Day information", "מידע על היום", yomTov, hebrewFormatter.formatYomTov(jewishCalendar)))
    }
    if (jewishCalendar.isRoshChodesh) {
        add(ZmanItem("Rosh Chodesh", "ראש חודש", null, "New Jewish month", "ראש חודש", englishFormatter.formatRoshChodesh(jewishCalendar), hebrewFormatter.formatRoshChodesh(jewishCalendar)))
    }
    if (jewishCalendar.dayOfOmer != -1) {
        add(ZmanItem("Omer", "עומר", null, "Sefirat HaOmer", "ספירת העומר", englishFormatter.formatOmer(jewishCalendar), hebrewFormatter.formatOmer(jewishCalendar)))
    }
    if (jewishCalendar.isChanukah) {
        add(ZmanItem("Chanukah", "חנוכה", null, "Day of Chanukah", "יום בחנוכה", jewishCalendar.dayOfChanukah.toString(), jewishCalendar.dayOfChanukah.toString()))
    }
    if (jewishCalendar.yomTovIndex == JewishCalendar.EREV_PESACH) {
        val chametzTimes = calendar.chametzTimes(settings.chametzMethod)
        add(ZmanItem("Eat Chametz Until", "סוף זמן אכילת חמץ", chametzTimes.first?.toInstant(), settings.chametzMethod.label, settings.chametzMethod.labelHebrew))
        add(ZmanItem("Burn Chametz Until", "סוף זמן ביעור חמץ", chametzTimes.second?.toInstant(), settings.chametzMethod.label, settings.chametzMethod.labelHebrew))
    }
    if (jewishCalendar.isTaanis) {
        val fastTimes = calendar.fastDayTimes(settings.fastDayMethod)
        add(ZmanItem("Fast Starts", "תחילת תענית", fastTimes.first?.toInstant(), settings.fastDayMethod.label, settings.fastDayMethod.labelHebrew))
        add(ZmanItem("Fast Ends", "סוף תענית", fastTimes.second?.toInstant(), settings.fastDayMethod.label, settings.fastDayMethod.labelHebrew))
    }
}

private fun ComplexZmanimCalendar.fastDayTimes(method: FastDayMethod): Pair<Date?, Date?> = when (method) {
    FastDayMethod.Alot72ToTzeit8Point5 -> alos72 to tzaisGeonim8Point5Degrees
    FastDayMethod.Alot72ToTzeit7Point083 -> alos72 to tzaisGeonim7Point083Degrees
    FastDayMethod.Alot72ToTzeit5Point88 -> alos72 to tzaisGeonim5Point88Degrees
    FastDayMethod.Alot16Point1ToTzeit8Point5 -> alos16Point1Degrees to tzaisGeonim8Point5Degrees
    FastDayMethod.Alot16Point1ToTzeit7Point083 -> alos16Point1Degrees to tzaisGeonim7Point083Degrees
    FastDayMethod.BaalHatanya -> alosBaalHatanya to tzaisBaalHatanya
}

private fun ComplexZmanimCalendar.chametzTimes(method: ChametzMethod): Pair<Date?, Date?> = when (method) {
    ChametzMethod.Gra -> sofZmanAchilasChametzGRA to sofZmanBiurChametzGRA
    ChametzMethod.Mga72 -> sofZmanAchilasChametzMGA72Minutes to sofZmanBiurChametzMGA72Minutes
    ChametzMethod.Mga16Point1 -> sofZmanAchilasChametzMGA16Point1Degrees to sofZmanBiurChametzMGA16Point1Degrees
    ChametzMethod.BaalHatanya -> sofZmanAchilasChametzBaalHatanya to sofZmanBiurChametzBaalHatanya
}
