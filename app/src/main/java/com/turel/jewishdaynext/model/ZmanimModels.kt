package com.turel.jewishdaynext.model

import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.YerushalmiYomiCalculator
import com.kosherjava.zmanim.util.GeoLocation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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

data class ZmanimCalculationSettings(
    val inIsrael: Boolean = true,
    val useMgaForShemaAndTefila: Boolean = false,
    val alotHashacharOffsetMinutes: Int = 72,
    val plagHaminchaOffsetMinutes: Int = 0,
    val useSeaLevelSunrise: Boolean = true,
    val useSeaLevelSunset: Boolean = true,
    val candleLightingOffsetMinutes: Int = 18,
)

fun zmanimForDate(
    location: JewishLocation = defaultJerusalemLocation,
    date: LocalDate,
    settings: ZmanimCalculationSettings = ZmanimCalculationSettings(),
): ZmanimDay {
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
    val calendar = ComplexZmanimCalendar(geoLocation).apply {
        setCalendar(calculationDate)
        candleLightingOffset = settings.candleLightingOffsetMinutes.toDouble()
    }
    val jewishCalendar = JewishCalendar(date).apply {
        isUseModernHolidays = true
        setInIsrael(settings.inIsrael)
    }
    val englishFormatter = HebrewDateFormatter()
    val hebrewFormatter = HebrewDateFormatter().apply { isHebrewFormat = true }

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
                ),
            ),
            ZmanimGroup(
                title = "Morning",
                titleHebrew = "בוקר",
                items = buildList {
                    add(ZmanItem("Alot Hashachar", "עלות השחר", calendar.alotHashachar(settings.alotHashacharOffsetMinutes)?.toInstant(), "${settings.alotHashacharOffsetMinutes} minutes before sunrise", "${settings.alotHashacharOffsetMinutes} דקות לפני זריחה"))
                    addAll(listOf(
                    ZmanItem("Tallit & Tefillin", "טלית ותפילין", calendar.misheyakir11Point5Degrees?.toInstant(), "Earliest practical time", "הזמן המוקדם למעשה"),
                    ZmanItem("Sunrise", "הנץ החמה", if (settings.useSeaLevelSunrise) calendar.seaLevelSunrise?.toInstant() else calendar.sunrise?.toInstant(), if (settings.useSeaLevelSunrise) "Sea-level sunrise" else "Observed sunrise", if (settings.useSeaLevelSunrise) "זריחה במישור" else "זריחה נראית"),
                    ))
                    if (settings.useMgaForShemaAndTefila) {
                        add(ZmanItem("Shema MGA", "קריאת שמע מג״א", calendar.sofZmanShmaMGA?.toInstant(), "Latest Shema according to MGA", "זמן אחרון לקריאת שמע לפי מג״א"))
                        add(ZmanItem("Tefila MGA", "תפילה מג״א", calendar.sofZmanTfilaMGA?.toInstant(), "Latest Shacharit according to MGA", "זמן אחרון לתפילת שחרית לפי מג״א"))
                    } else {
                        add(ZmanItem("Shema GRA", "קריאת שמע גר״א", calendar.sofZmanShmaGRA?.toInstant(), "Latest Shema according to GRA", "זמן אחרון לקריאת שמע לפי גר״א"))
                        add(ZmanItem("Tefila GRA", "תפילה גר״א", calendar.sofZmanTfilaGRA?.toInstant(), "Latest Shacharit according to GRA", "זמן אחרון לתפילת שחרית לפי גר״א"))
                    }
                },
            ),
            ZmanimGroup(
                title = "Afternoon & Evening",
                titleHebrew = "צהריים וערב",
                items = listOf(
                    ZmanItem("Chatzot", "חצות", calendar.chatzos?.toInstant(), "Halachic midday", "חצות היום"),
                    ZmanItem("Mincha Gedola", "מנחה גדולה", calendar.minchaGedola?.toInstant(), "Earliest regular Mincha", "תחילת זמן מנחה גדולה"),
                    ZmanItem("Plag Hamincha", "פלג המנחה", calendar.plagHamincha(settings.plagHaminchaOffsetMinutes)?.toInstant(), if (settings.plagHaminchaOffsetMinutes == 0) "Late afternoon boundary" else "${settings.plagHaminchaOffsetMinutes} minute offset", if (settings.plagHaminchaOffsetMinutes == 0) "גבול אחרון של אחר הצהריים" else "היסט ${settings.plagHaminchaOffsetMinutes} דקות"),
                    ZmanItem("Sunset", "שקיעה", if (settings.useSeaLevelSunset) calendar.seaLevelSunset?.toInstant() else calendar.sunset?.toInstant(), if (settings.useSeaLevelSunset) "Sea-level sunset" else "Observed sunset", if (settings.useSeaLevelSunset) "שקיעה במישור" else "שקיעה נראית"),
                    ZmanItem("Tzeit", "צאת הכוכבים", calendar.tzais?.toInstant(), "Nightfall", "צאת הכוכבים"),
                ),
            ),
            ZmanimGroup(
                title = "Shabbat",
                titleHebrew = "שבת",
                items = listOf(
                    ZmanItem("Candle Lighting", "הדלקת נרות", calendar.candleLighting?.toInstant(), "${settings.candleLightingOffsetMinutes} minutes before sunset", "${settings.candleLightingOffsetMinutes} דקות לפני שקיעה"),
                    ZmanItem("Plag Hamincha", "פלג המנחה", calendar.plagHamincha(settings.plagHaminchaOffsetMinutes)?.toInstant(), "Earliest Shabbat boundary", "גבול מוקדם לשבת"),
                    ZmanItem("Tzeit Shabbat", "צאת שבת", calendar.tzais72?.toInstant(), "72 minutes after sunset", "72 דקות אחרי שקיעה"),
                ),
            ),
            ZmanimGroup(
                title = "Daily Learning",
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
    addCyclePlaceholder("Rambam Yomi", "רמב״ם יומי")
    addCyclePlaceholder("Shmirat HaLashon Yomi", "שמירת הלשון יומי")
    addCyclePlaceholder("Halacha Yomit", "הלכה יומית")
}

private fun MutableList<ZmanItem>.addCyclePlaceholder(title: String, titleHebrew: String) {
    add(
        ZmanItem(
            title = title,
            titleHebrew = titleHebrew,
            time = null,
            description = "Needs a verified program cycle before calculation",
            descriptionHebrew = "נדרש מקור מחזור מדויק לפני חישוב",
            value = "Not calculated yet",
            valueHebrew = "עדיין לא מחושב",
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

private fun ComplexZmanimCalendar.alotHashachar(offsetMinutes: Int) = when (offsetMinutes) {
    60 -> alos60
    72 -> alos72
    90 -> alos90
    120 -> alos120
    else -> alos72
}

private fun ComplexZmanimCalendar.plagHamincha(offsetMinutes: Int) = when (offsetMinutes) {
    60 -> plagHamincha60Minutes
    72 -> plagHamincha72Minutes
    90 -> plagHamincha90Minutes
    96 -> plagHamincha96Minutes
    120 -> plagHamincha120Minutes
    else -> plagHamincha
}

private fun dailyItems(
    jewishCalendar: JewishCalendar,
    englishFormatter: HebrewDateFormatter,
    hebrewFormatter: HebrewDateFormatter,
    calendar: ComplexZmanimCalendar,
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
    if (jewishCalendar.isTaanis) {
        add(ZmanItem("Fast Starts", "תחילת תענית", calendar.alos72?.toInstant(), "Stop eating", "זמן הפסקת אכילה"))
        add(ZmanItem("Fast Ends", "סוף תענית", calendar.tzais?.toInstant(), "Fast ends", "זמן היתר אכילה"))
    }
}
