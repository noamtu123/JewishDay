package com.noamtu.jewishday.model

import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.util.Date

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
                title = "Zmanim",
                titleHebrew = "זמנים",
                // One continuous list (no morning/afternoon split). Sof Zman Shema and
                // Tefillah each appear twice — GRA and Magen Avraham — with the precise
                // method shown as the caption. The Magen Avraham basis is configurable in
                // advanced mode; GRA is always the fixed GRA opinion.
                items = listOf(
                    ZmanItem("Alot Hashachar", "עלות השחר", calendar.alotHashachar(settings)?.toInstant(), settings.alotHashacharMethod.label, settings.alotHashacharMethod.labelHebrew),
                    ZmanItem("Misheyakir", "משיכיר", calendar.misheyakir(settings.misheyakirMethod)?.toInstant(), settings.misheyakirMethod.label, settings.misheyakirMethod.labelHebrew),
                    ZmanItem("Sunrise", "הנץ החמה", calendar.sunrise(settings.sunriseMethod)?.toInstant(), settings.sunriseMethod.label, settings.sunriseMethod.labelHebrew),
                    ZmanItem("Sof Zman Shema (GRA)", "סוף זמן קריאת שמע (גר״א)", calendar.sofZmanShmaGRA?.toInstant(), "GRA", "גר״א"),
                    ZmanItem("Sof Zman Shema (Magen Avraham)", "סוף זמן קריאת שמע (מגן אברהם)", calendar.sofZmanShema(settings)?.toInstant(), settings.sofZmanShemaMethod.label, settings.sofZmanShemaMethod.labelHebrew),
                    ZmanItem("Sof Zman Tefillah (GRA)", "סוף זמן תפילה (גר״א)", calendar.sofZmanTfilaGRA?.toInstant(), "GRA", "גר״א"),
                    ZmanItem("Sof Zman Tefillah (Magen Avraham)", "סוף זמן תפילה (מגן אברהם)", calendar.sofZmanTefillah(settings)?.toInstant(), settings.sofZmanTefillahMethod.label, settings.sofZmanTefillahMethod.labelHebrew),
                    ZmanItem("Chatzot HaYom", "חצות היום", calendar.chatzot(settings.chatzotMethod)?.toInstant(), settings.chatzotMethod.label, settings.chatzotMethod.labelHebrew),
                    ZmanItem("Mincha Gedola", "מנחה גדולה", calendar.minchaGedola(settings)?.toInstant(), settings.minchaGedolaMethod.label, settings.minchaGedolaMethod.labelHebrew),
                    ZmanItem("Mincha Ketana", "מנחה קטנה", calendar.minchaKetana(settings)?.toInstant(), settings.minchaKetanaMethod.label, settings.minchaKetanaMethod.labelHebrew),
                    ZmanItem("Plag Hamincha", "פלג המנחה", calendar.plagHamincha(settings)?.toInstant(), settings.plagHaminchaMethod.label, settings.plagHaminchaMethod.labelHebrew),
                    ZmanItem("Sunset", "שקיעה", calendar.sunset(settings.sunsetMethod)?.toInstant(), settings.sunsetMethod.label, settings.sunsetMethod.labelHebrew),
                    ZmanItem("Tzeit", "צאת הכוכבים", calendar.tzeit(settings)?.toInstant(), settings.tzeitHakochavimMethod.label, settings.tzeitHakochavimMethod.labelHebrew),
                    ZmanItem("Chatzot HaLaila", "חצות הלילה", calendar.solarMidnight?.toInstant(), "Solar midnight", "חצות אסטרונומי"),
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
