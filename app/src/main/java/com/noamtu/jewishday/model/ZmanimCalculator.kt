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
    // The parsha is only attached to a Shabbat date, so read it from the upcoming
    // Shabbat to always show "this week's" reading even on a weekday.
    val shabbatJewishCalendar = JewishCalendar(shabbatDates.endDate).apply {
        isUseModernHolidays = true
        setInIsrael(settings.inIsrael)
    }

    val weeklyParshaEnglish = englishFormatter.formatParsha(shabbatJewishCalendar)
    val weeklyParshaHebrew = hebrewFormatter.formatParsha(shabbatJewishCalendar)
    // The upcoming parsha now lives at the top of the Shabbat section.
    val parshaItem = if (weeklyParshaEnglish.isNotBlank()) {
        ZmanItem("Weekly Parsha", "פרשת השבוע", null, "Upcoming Torah reading", "קריאת התורה הקרובה", weeklyParshaEnglish, weeklyParshaHebrew)
    } else {
        null
    }
    // Occasional day events (Yom Tov, Rosh Chodesh, Omer, fasts…); shown header-less at the
    // top of the tab and omitted entirely when empty (no standalone "Daily" section).
    val eventItems = dailyItems(
        jewishCalendar = jewishCalendar,
        englishFormatter = englishFormatter,
        hebrewFormatter = hebrewFormatter,
        calendar = calendar,
        settings = settings,
    )

    return ZmanimDay(
        locationName = location.name,
        date = date,
        zoneId = location.zoneId,
        hebrewDateEnglish = englishFormatter.format(jewishCalendar),
        hebrewDateHebrew = hebrewFormatter.format(jewishCalendar),
        groups = listOfNotNull(
            eventItems.takeIf { it.isNotEmpty() }?.let { items ->
                ZmanimGroup(title = "", titleHebrew = "", items = items)
            },
            ZmanimGroup(
                title = ZmanimGroupTitle,
                titleHebrew = "זמנים",
                // One continuous list (no morning/afternoon split). Sof Zman Shema and
                // Tefillah each appear twice — Magen Avraham first, then GRA — each with its
                // own configurable method (the caption shows the precise method chosen).
                // Each row carries its ZmanimTimeOption id so it can be shown/hidden.
                items = listOf(
                    ZmanItem("Alot Hashachar", "עלות השחר", calendar.alotHashachar(settings)?.toInstant(), settings.alotHashacharMethod.label, settings.alotHashacharMethod.labelHebrew, id = ZmanimTimeOption.AlotHashachar.storageValue),
                    ZmanItem("Tallit & Tefillin", "זמן טלית ותפילין", calendar.misheyakir(settings.misheyakirMethod)?.toInstant(), settings.misheyakirMethod.label, settings.misheyakirMethod.labelHebrew, id = ZmanimTimeOption.TallitTefillin.storageValue),
                    ZmanItem("Sunrise", "הנץ החמה", calendar.sunrise(settings.sunriseMethod)?.toInstant(), settings.sunriseMethod.label, settings.sunriseMethod.labelHebrew, id = ZmanimTimeOption.Sunrise.storageValue),
                    ZmanItem("Sof Zman Shema (Magen Avraham)", "סוף זמן קריאת שמע (מג״א)", calendar.sofZmanShema(settings.sofZmanShemaMethod, settings)?.toInstant(), settings.sofZmanShemaMethod.label, settings.sofZmanShemaMethod.labelHebrew, id = ZmanimTimeOption.SofZmanShemaMagenAvraham.storageValue),
                    ZmanItem("Sof Zman Shema (GRA)", "סוף זמן קריאת שמע (גר״א)", calendar.sofZmanShema(settings.sofZmanShemaGraMethod, settings)?.toInstant(), settings.sofZmanShemaGraMethod.label, settings.sofZmanShemaGraMethod.labelHebrew, id = ZmanimTimeOption.SofZmanShemaGra.storageValue),
                    ZmanItem("Sof Zman Tefillah (Magen Avraham)", "סוף זמן תפילה (מג״א)", calendar.sofZmanTefillah(settings.sofZmanTefillahMethod, settings)?.toInstant(), settings.sofZmanTefillahMethod.label, settings.sofZmanTefillahMethod.labelHebrew, id = ZmanimTimeOption.SofZmanTefillahMagenAvraham.storageValue),
                    ZmanItem("Sof Zman Tefillah (GRA)", "סוף זמן תפילה (גר״א)", calendar.sofZmanTefillah(settings.sofZmanTefillahGraMethod, settings)?.toInstant(), settings.sofZmanTefillahGraMethod.label, settings.sofZmanTefillahGraMethod.labelHebrew, id = ZmanimTimeOption.SofZmanTefillahGra.storageValue),
                    ZmanItem("Chatzot HaYom", "חצות היום", calendar.chatzot(settings.chatzotMethod)?.toInstant(), settings.chatzotMethod.label, settings.chatzotMethod.labelHebrew, id = ZmanimTimeOption.ChatzotHaYom.storageValue),
                    ZmanItem("Mincha Gedola", "מנחה גדולה", calendar.minchaGedola(settings)?.toInstant(), settings.minchaGedolaMethod.label, settings.minchaGedolaMethod.labelHebrew, id = ZmanimTimeOption.MinchaGedola.storageValue),
                    ZmanItem("Mincha Ketana", "מנחה קטנה", calendar.minchaKetana(settings)?.toInstant(), settings.minchaKetanaMethod.label, settings.minchaKetanaMethod.labelHebrew, id = ZmanimTimeOption.MinchaKetana.storageValue),
                    ZmanItem("Plag Hamincha", "פלג המנחה", calendar.plagHamincha(settings)?.toInstant(), settings.plagHaminchaMethod.label, settings.plagHaminchaMethod.labelHebrew, id = ZmanimTimeOption.PlagHamincha.storageValue),
                    ZmanItem("Sunset", "שקיעה", calendar.sunset(settings.sunsetMethod)?.toInstant(), settings.sunsetMethod.label, settings.sunsetMethod.labelHebrew, id = ZmanimTimeOption.Sunset.storageValue),
                    ZmanItem("Tzeit", "צאת הכוכבים", calendar.tzeit(settings)?.toInstant(), settings.tzeitHakochavimMethod.label, settings.tzeitHakochavimMethod.labelHebrew, id = ZmanimTimeOption.Tzeit.storageValue),
                    ZmanItem("Chatzot HaLaila", "חצות הלילה", calendar.chatzotHaLaila(settings.chatzotHaLailaMethod)?.toInstant(), settings.chatzotHaLailaMethod.label, settings.chatzotHaLailaMethod.labelHebrew, id = ZmanimTimeOption.ChatzotHaLaila.storageValue),
                ),
            ),
            ZmanimGroup(
                title = "Shabbat",
                titleHebrew = "שבת",
                items = listOfNotNull(
                    parshaItem,
                    ZmanItem("Candle Lighting & Shabbat Entry", "הדלקת נרות וכניסת שבת", shabbatStartCalendar.candleLighting?.toInstant(), "Friday ${shabbatDates.startDate}; ${settings.candleLightingMethod.label}", "יום שישי ${shabbatDates.startDate}; ${settings.candleLightingMethod.labelHebrew}"),
                    ZmanItem("Sunset", "שקיעה", shabbatStartCalendar.sunset(settings.sunsetMethod)?.toInstant(), "Friday ${shabbatDates.startDate}; ${settings.sunsetMethod.label}", "יום שישי ${shabbatDates.startDate}; ${settings.sunsetMethod.labelHebrew}"),
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
    // The Jewish date is shown in the date header at the top of the tab, and the parsha at the
    // top of the Shabbat section; this list is only the occasional day events.
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
