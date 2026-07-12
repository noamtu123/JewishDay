// SPDX-License-Identifier: GPL-3.0-or-later

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
    now: Instant? = null,
): ZmanimDay {
    val calendar = complexZmanimCalendar(location, date, settings)
    // Israel vs. diaspora calendar is derived from where you are, not a manual setting.
    val inIsrael = location.isInIsrael
    val jewishCalendar = JewishCalendar(date).apply {
        isUseModernHolidays = true
        setInIsrael(inIsrael)
    }
    // The displayed Hebrew date rolls over to the next day at sunset (the day/night boundary),
    // so from sunset onward the header shows the current Jewish date. Day events and the zmanim
    // themselves stay on the civil date; the fast chip/card follows the active fast (see below).
    val sunset = calendar.sunset(settings.sunsetMethod)?.toInstant()
    val afterSunset = now != null && sunset != null && !now.isBefore(sunset)
    val displayJewishCalendar = if (afterSunset) {
        JewishCalendar(date.plusDays(1)).apply {
            isUseModernHolidays = true
            setInIsrael(inIsrael)
        }
    } else {
        jewishCalendar
    }
    val englishFormatter = HebrewDateFormatter()
    val hebrewFormatter = HebrewDateFormatter().apply { isHebrewFormat = true }
    val shabbatDates = shabbatDatesFor(date, location, settings, now)
    val shabbatStartCalendar = complexZmanimCalendar(location, shabbatDates.startDate, settings)
    val shabbatEndCalendar = complexZmanimCalendar(location, shabbatDates.endDate, settings)
    // The parsha is only attached to a Shabbat date, so read it from the upcoming
    // Shabbat to always show "this week's" reading even on a weekday.
    val shabbatJewishCalendar = JewishCalendar(shabbatDates.endDate).apply {
        isUseModernHolidays = true
        setInIsrael(inIsrael)
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
        location = location,
        date = date,
    )
    // The header's fast chip/card tracks the fast that is actually relevant right now:
    // the civil day's fast while it hasn't ended yet, and otherwise — once the displayed
    // date has rolled at sunset — the display day's fast. This closes the Erev Tisha B'Av /
    // Erev Yom Kippur evening gap (the fast begins at sunset, before the civil fast date),
    // and clears the chip after a fast ends instead of letting it linger until midnight.
    val civilFastInfo = fastDayInfo(jewishCalendar, calendar, settings, location, date)
    val civilFastStillRelevant = civilFastInfo != null &&
        (now == null || civilFastInfo.endTime == null || now.isBefore(civilFastInfo.endTime))
    val fastDayInfo = when {
        civilFastStillRelevant -> civilFastInfo
        afterSunset -> fastDayInfo(
            jewishCalendar = displayJewishCalendar,
            calendar = complexZmanimCalendar(location, date.plusDays(1), settings),
            settings = settings,
            location = location,
            date = date.plusDays(1),
        )
        else -> null
    }

    return ZmanimDay(
        locationName = location.name,
        date = date,
        zoneId = location.zoneId,
        hebrewDateEnglish = englishFormatter.format(displayJewishCalendar),
        hebrewDateHebrew = hebrewFormatter.format(displayJewishCalendar),
        fastDayInfo = fastDayInfo,
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
                    ZmanItem("Tallit & Tefillin", "זמן טלית ותפילין", calendar.misheyakir(settings)?.toInstant(), settings.misheyakirMethod.label, settings.misheyakirMethod.labelHebrew, id = ZmanimTimeOption.TallitTefillin.storageValue),
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
                title = ShabbatGroupTitle,
                titleHebrew = "שבת",
                // The parsha (no id) always shows; the four time rows carry their ZmanimTimeOption
                // id so the same "Zmanim to show" list can hide them.
                items = listOfNotNull(
                    parshaItem,
                    ZmanItem("Candle Lighting & Shabbat Entry", "הדלקת נרות וכניסת שבת", shabbatStartCalendar.candleLighting?.toInstant(), "Friday ${shabbatDates.startDate}; ${settings.candleLightingMethod.label}", "יום שישי ${shabbatDates.startDate}; ${settings.candleLightingMethod.labelHebrew}", id = ZmanimTimeOption.ShabbatCandleLighting.storageValue),
                    ZmanItem("Sunset", "שקיעה", shabbatStartCalendar.sunset(settings.sunsetMethod)?.toInstant(), "Friday ${shabbatDates.startDate}; ${settings.sunsetMethod.label}", "יום שישי ${shabbatDates.startDate}; ${settings.sunsetMethod.labelHebrew}", id = ZmanimTimeOption.ShabbatSunset.storageValue),
                    ZmanItem("Motzei Shabbat", "צאת שבת", shabbatEndCalendar.motzeiShabbat(settings)?.toInstant(), "Saturday ${shabbatDates.endDate}; ${settings.motzeiShabbatMethod.label}", "מוצאי שבת ${shabbatDates.endDate}; ${settings.motzeiShabbatMethod.labelHebrew}", id = ZmanimTimeOption.MotzeiShabbat.storageValue),
                    ZmanItem("Rabbeinu Tam", "רבינו תם", shabbatEndCalendar.rabbeinuTam(settings.rabbeinuTamMethod)?.toInstant(), "Saturday ${shabbatDates.endDate}; ${settings.rabbeinuTamMethod.label}", "מוצאי שבת ${shabbatDates.endDate}; ${settings.rabbeinuTamMethod.labelHebrew}", id = ZmanimTimeOption.RabbeinuTam.storageValue),
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

private fun shabbatDatesFor(
    date: LocalDate,
    location: JewishLocation,
    settings: ZmanimCalculationSettings,
    now: Instant?,
): ShabbatDates {
    val afterMotzeiShabbat = date.dayOfWeek == DayOfWeek.SATURDAY &&
        now != null &&
        motzeiShabbatForDate(location, date, settings)?.let { !now.isBefore(it) } == true
    val friday = if (date.dayOfWeek == DayOfWeek.SATURDAY && !afterMotzeiShabbat) {
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

/** Sunset for [date] — the instant at which the displayed Hebrew date rolls to the next day. */
fun sunsetForDate(
    location: JewishLocation = defaultJerusalemLocation,
    date: LocalDate,
    settings: ZmanimCalculationSettings = ZmanimCalculationSettings(),
): Instant? = complexZmanimCalendar(location, date, settings)
    .sunset(settings.sunsetMethod)
    ?.toInstant()

fun motzeiShabbatForDate(
    location: JewishLocation = defaultJerusalemLocation,
    date: LocalDate,
    settings: ZmanimCalculationSettings = ZmanimCalculationSettings(),
): Instant? = complexZmanimCalendar(location, date, settings)
    .motzeiShabbat(settings)
    ?.toInstant()

private fun dailyItems(
    jewishCalendar: JewishCalendar,
    englishFormatter: HebrewDateFormatter,
    hebrewFormatter: HebrewDateFormatter,
    calendar: ComplexZmanimCalendar,
    settings: ZmanimCalculationSettings,
    location: JewishLocation,
    date: LocalDate,
): List<ZmanItem> = buildList {
    // The Jewish date is shown in the date header at the top of the tab, and the parsha at the
    // top of the Shabbat section; this list is only the occasional day events.
    // On a fast day the fast already has its own name chip in the date header plus a start/end
    // card, so skip the duplicate "Day information" caption here.
    val yomTov = englishFormatter.formatYomTov(jewishCalendar)
    if (yomTov.isNotBlank() && !jewishCalendar.isTaanis) {
        add(ZmanItem("Yom Tov", "יום טוב", null, "Day information", "מידע על היום", yomTov, hebrewFormatter.formatYomTov(jewishCalendar)))
    }
    // Yom Tov candle lighting. Erev Shabbat candle lighting already lives in the Shabbat section, so
    // only handle the eve of a melacha-forbidden Yom Tov here. When today is already Shabbat/Yom Tov
    // (e.g. the 2nd night in the diaspora, or a Yom Tov right after Shabbat) one may not light before
    // — candles are lit after nightfall from an existing flame.
    val tomorrow = JewishCalendar(date.plusDays(1)).apply {
        isUseModernHolidays = true
        setInIsrael(inIsrael)
    }
    if (tomorrow.isYomTovAssurBemelacha) {
        if (jewishCalendar.isAssurBemelacha) {
            add(ZmanItem("Yom Tov Candle Lighting", "הדלקת נרות יום טוב", calendar.tzeit(settings)?.toInstant(), "After nightfall, from an existing flame", "אחרי צאת הכוכבים, מאש קיימת"))
        } else {
            add(ZmanItem("Yom Tov Candle Lighting", "הדלקת נרות יום טוב", calendar.candleLighting?.toInstant(), "Before sunset", "לפני השקיעה"))
        }
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
}

private val FastDayNames: Map<Int, Pair<String, String>> = mapOf(
    JewishCalendar.FAST_OF_GEDALYAH to ("Fast of Gedalyah" to "צום גדליה"),
    JewishCalendar.TISHA_BEAV to ("Tisha B'Av" to "תשעה באב"),
    JewishCalendar.SEVENTEEN_OF_TAMMUZ to ("17th of Tammuz" to "י״ז בתמוז"),
    JewishCalendar.TENTH_OF_TEVES to ("10th of Teves" to "עשרה בטבת"),
    JewishCalendar.FAST_OF_ESTHER to ("Fast of Esther" to "תענית אסתר"),
    JewishCalendar.YOM_KIPPUR to ("Yom Kippur" to "יום כיפור"),
)

private fun fastDayInfo(
    jewishCalendar: JewishCalendar,
    calendar: ComplexZmanimCalendar,
    settings: ZmanimCalculationSettings,
    location: JewishLocation,
    date: LocalDate,
): FastDayInfo? {
    if (!jewishCalendar.isTaanis) return null
    val (name, nameHebrew) = FastDayNames[jewishCalendar.yomTovIndex] ?: return null
    val startsPreviousEvening = jewishCalendar.yomTovIndex == JewishCalendar.YOM_KIPPUR ||
        jewishCalendar.yomTovIndex == JewishCalendar.TISHA_BEAV
    val startTime = if (startsPreviousEvening) {
        complexZmanimCalendar(location, date.minusDays(1), settings)
            .sunset(settings.sunsetMethod)?.toInstant()
    } else {
        calendar.alotHashachar(settings)?.toInstant()
    }
    val endTime = calendar.tzeit(settings)?.toInstant()
    return FastDayInfo(name = name, nameHebrew = nameHebrew, startTime = startTime, endTime = endTime)
}