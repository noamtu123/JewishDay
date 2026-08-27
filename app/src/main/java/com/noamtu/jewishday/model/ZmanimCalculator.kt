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
        inIsrael = inIsrael,
    )
    // The header's fast chip/card is announced one Jewish day before the fast begins and clears
    // the moment it ends: an evening fast (Yom Kippur, Tisha B'Av) begins at the sunset before its
    // own date, so it appears a sunset earlier still; a dawn fast appears at alot of the previous
    // morning. This also closes the Erev Tisha B'Av / Erev Yom Kippur evening gap and stops a
    // finished fast lingering until midnight.
    // The Hebrew date shown in the header, as a civil date: it rolls at sunset, so from sunset
    // onwards "today" is already tomorrow.
    val displayedDate = if (afterSunset) date.plusDays(1) else date
    val announcedFast = announcedFastDayInfo(
        date = date,
        displayedDate = displayedDate,
        now = now,
        settings = settings,
        location = location,
        inIsrael = inIsrael,
    )
    // Shabbat and Yom Tov get the same header treatment, over the same window: the whole
    // melacha-forbidden stretch is one card, announced one Jewish day before it enters and cleared
    // once it goes out.
    val holyDayInfo = announcedHolyDayInfo(
        date = date,
        displayedDate = displayedDate,
        now = now,
        settings = settings,
        location = location,
        inIsrael = inIsrael,
        englishFormatter = englishFormatter,
        hebrewFormatter = hebrewFormatter,
    )

    // On Shabbat itself the Shabbat section only repeats what is already on screen: its candle
    // lighting and motzei are the header's entry and exit, and its sunset is Friday's, long past.
    // Only Rabbeinu Tam is still worth showing, and today's calendar gives the same time, so it
    // joins the day's own list and the section goes away. (After motzei the section already points
    // at next week's Shabbat, which is not a duplicate, so it stays.)
    val shabbatSectionRepeatsToday = date.dayOfWeek == DayOfWeek.SATURDAY && shabbatDates.endDate == date

    // A fast that has not begun yet waits its turn while a holy day is still on: the second day of
    // Rosh Hashana announces Tzom Gedalyah, and two cards would claim two things are happening at
    // once. It appears the moment the chag goes out. A fast already under way — one falling on a
    // Friday, with Shabbat still ahead — keeps its card, since you need to know when it ends.
    val fastDayInfo = announcedFast?.takeUnless { fast ->
        now != null && !hasBegun(fast.startTime, now) && isUnderWay(holyDayInfo?.startTime, holyDayInfo?.endTime, now)
    }

    return ZmanimDay(
        locationName = location.name,
        date = date,
        zoneId = location.zoneId,
        hebrewDateEnglish = englishFormatter.format(displayJewishCalendar),
        hebrewDateHebrew = hebrewFormatter.format(displayJewishCalendar),
        fastDayInfo = fastDayInfo,
        holyDayInfo = holyDayInfo,
        fastLeadsHeader = fastLeadsHeader(fastDayInfo, holyDayInfo, now),
        groups = listOfNotNull(
            // The parsha lives at the top of the Shabbat section; when that section is dropped it
            // moves up here rather than disappearing.
            (eventItems + listOfNotNull(parshaItem.takeIf { shabbatSectionRepeatsToday }))
                .takeIf { it.isNotEmpty() }
                ?.let { items -> ZmanimGroup(title = "", titleHebrew = "", items = items) },
            ZmanimGroup(
                title = ZmanimGroupTitle,
                titleHebrew = "זמנים",
                // One continuous list (no morning/afternoon split). Sof Zman Shema and
                // Tefillah each appear twice — Magen Avraham first, then GRA — each with its
                // own configurable method (the caption shows the precise method chosen).
                // Each row carries its ZmanimTimeOption id so it can be shown/hidden.
                items = listOfNotNull(
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
                    // Only on Shabbat, where the Shabbat section it normally lives in is dropped.
                    ZmanItem("Rabbeinu Tam", "רבינו תם", calendar.rabbeinuTam(settings.rabbeinuTamMethod)?.toInstant(), settings.rabbeinuTamMethod.label, settings.rabbeinuTamMethod.labelHebrew, id = ZmanimTimeOption.RabbeinuTam.storageValue)
                        .takeIf { shabbatSectionRepeatsToday },
                    ZmanItem("Chatzot HaLaila", "חצות הלילה", calendar.chatzotHaLaila(settings.chatzotHaLailaMethod)?.toInstant(), settings.chatzotHaLailaMethod.label, settings.chatzotHaLailaMethod.labelHebrew, id = ZmanimTimeOption.ChatzotHaLaila.storageValue),
                ),
            ),
            if (shabbatSectionRepeatsToday) null else ZmanimGroup(
                title = ShabbatGroupTitle,
                titleHebrew = "שבת",
                // The parsha (no id) always shows; the four time rows carry their ZmanimTimeOption
                // id so the same "Zmanim to show" list can hide them.
                items = listOfNotNull(
                    parshaItem,
                    ZmanItem("Candle Lighting & Shabbat Entry", "הדלקת נרות וכניסת שבת", shabbatStartCalendar.candleLighting?.toInstant(), "Friday; ${settings.candleLightingMethod.label}", "יום שישי; ${settings.candleLightingMethod.labelHebrew}", id = ZmanimTimeOption.ShabbatCandleLighting.storageValue),
                    ZmanItem("Sunset", "שקיעה", shabbatStartCalendar.sunset(settings.sunsetMethod)?.toInstant(), "Friday; ${settings.sunsetMethod.label}", "יום שישי; ${settings.sunsetMethod.labelHebrew}", id = ZmanimTimeOption.ShabbatSunset.storageValue),
                    ZmanItem("Motzei Shabbat", "צאת שבת", shabbatEndCalendar.holyDayExit(settings)?.toInstant(), "Saturday; ${settings.motzeiShabbatMethod.label} + ${settings.holyDayTosefetMinutes}m", "מוצאי שבת; ${settings.motzeiShabbatMethod.labelHebrew} + ${settings.holyDayTosefetMinutes} דק׳", id = ZmanimTimeOption.MotzeiShabbat.storageValue),
                    ZmanItem("Rabbeinu Tam", "רבינו תם", shabbatEndCalendar.rabbeinuTam(settings.rabbeinuTamMethod)?.toInstant(), "Saturday; ${settings.rabbeinuTamMethod.label}", "מוצאי שבת; ${settings.rabbeinuTamMethod.labelHebrew}", id = ZmanimTimeOption.RabbeinuTam.storageValue),
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

/** When a holy day beginning or ending on [date] goes out: motzei plus the user's tosefet. */
fun holyDayExitForDate(
    location: JewishLocation = defaultJerusalemLocation,
    date: LocalDate,
    settings: ZmanimCalculationSettings = ZmanimCalculationSettings(),
): Instant? = complexZmanimCalendar(location, date, settings)
    .holyDayExit(settings)
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
    inIsrael: Boolean,
): List<ZmanItem> = buildList {
    // The Jewish date is shown in the date header at the top of the tab, and the parsha at the top
    // of the Shabbat section; this list is only the occasional day events.
    //
    // A Yom Tov is named by the header, along with its entry and exit — including the second
    // night's candle lighting — so it gets no row here. Every other named day still needs one:
    // Chol Hamoed, Purim, Isru Chag and the modern holidays never reach the header. Note the test
    // is on the Yom Tov itself, not on melacha: Isru Chag or Chol Hamoed falling on Shabbat is
    // named "שבת" by the header, so without a row its own name would be lost.
    val yomTov = englishFormatter.formatYomTov(jewishCalendar)
    if (yomTov.isNotBlank() && !jewishCalendar.isTaanis && !jewishCalendar.isYomTovAssurBemelacha) {
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
}

private val FastDayNames: Map<Int, Pair<String, String>> = mapOf(
    JewishCalendar.FAST_OF_GEDALYAH to ("Fast of Gedalyah" to "צום גדליה"),
    JewishCalendar.TISHA_BEAV to ("Tisha B'Av" to "תשעה באב"),
    JewishCalendar.SEVENTEEN_OF_TAMMUZ to ("17th of Tammuz" to "י״ז בתמוז"),
    JewishCalendar.TENTH_OF_TEVES to ("10th of Teves" to "עשרה בטבת"),
    JewishCalendar.FAST_OF_ESTHER to ("Fast of Esther" to "תענית אסתר"),
    // Yom Kippur is deliberately absent: it is a holy day, so the header describes it as one —
    // entered with the candle-lighting tosefet and left at the holy-day exit, not as a plain fast.
)

/**
 * The fast to show in the date header, or null. A fast is announced one Jewish day before it
 * begins and disappears the moment it ends, so at most one is ever current.
 *
 * Today plus the next two civil days are considered: an evening fast begins at the sunset before
 * its own date, so its announcement starts two civil days ahead of that date.
 */
private fun announcedFastDayInfo(
    date: LocalDate,
    displayedDate: LocalDate,
    now: Instant?,
    settings: ZmanimCalculationSettings,
    location: JewishLocation,
    inIsrael: Boolean,
): FastDayInfo? {
    for (offset in 0L..2L) {
        val fastDate = date.plusDays(offset)
        val fastCalendar = JewishCalendar(fastDate).apply {
            isUseModernHolidays = true
            setInIsrael(inIsrael)
        }
        val info = fastDayInfo(
            jewishCalendar = fastCalendar,
            calendar = complexZmanimCalendar(location, fastDate, settings),
            settings = settings,
            location = location,
            date = fastDate,
        ) ?: continue
        // Named only while the fast is actually running. The Hebrew date rolls at sunset, so a
        // dawn fast owns its date from the previous nightfall — naming it then would announce
        // צום גדליה the moment Rosh Hashana goes out, hours before anyone starts fasting.
        val named = if (now == null) fastDate == displayedDate else isUnderWay(info.startTime, info.endTime, now)
        // Without a clock there is no window to place us in, so only the civil day's fast shows.
        if (now == null) return info.takeIf { offset == 0L }
        val announcedFrom = fastAnnouncedFrom(fastCalendar, fastDate, location, settings) ?: continue
        if (now.isBefore(announcedFrom)) continue
        if (info.endTime != null && !now.isBefore(info.endTime)) continue
        return info.copy(isUnderWay = named)
    }
    return null
}

/** The instant a fast starts being shown: one Jewish day before it begins. */
private fun fastAnnouncedFrom(
    jewishCalendar: JewishCalendar,
    date: LocalDate,
    location: JewishLocation,
    settings: ZmanimCalculationSettings,
): Instant? = if (startsPreviousEvening(jewishCalendar)) {
    // Begins at the sunset before [date]; announce it from the sunset before that.
    sunsetForDate(location, date.minusDays(2), settings)
} else {
    // Begins at alot on [date]; announce it from alot the previous morning.
    complexZmanimCalendar(location, date.minusDays(1), settings).alotHashachar(settings)?.toInstant()
}

/**
 * The holy day to show in the date header, or null.
 *
 * Back-to-back days — both days of Rosh Hashana, a Yom Tov running into Shabbat, Shabbat running
 * into a Yom Tov — are shown one at a time rather than as a single span: the current day's own
 * entry and exit, carrying a warning that another begins the moment this one ends. As each goes
 * out it is replaced by the next. The first day of a run is entered at candle lighting; every later
 * one begins exactly when its predecessor ends, since candles are lit then from an existing flame.
 *
 * The run is announced one Jewish day before it enters — the sunset two days before its first day.
 */
private fun announcedHolyDayInfo(
    date: LocalDate,
    displayedDate: LocalDate,
    now: Instant?,
    settings: ZmanimCalculationSettings,
    location: JewishLocation,
    inIsrael: Boolean,
    englishFormatter: HebrewDateFormatter,
    hebrewFormatter: HebrewDateFormatter,
): HolyDayInfo? {
    fun calendarFor(day: LocalDate) = JewishCalendar(day).apply {
        isUseModernHolidays = true
        setInIsrael(inIsrael)
    }
    fun isForbidden(day: LocalDate) = calendarFor(day).isAssurBemelacha
    fun exitOf(day: LocalDate) = complexZmanimCalendar(location, day, settings).holyDayExit(settings)?.toInstant()

    // Start far enough back to catch a run already under way, and far enough forward to catch the
    // next one while it is being announced.
    var day = date.minusDays(HolyDayScanBack)
    val limit = date.plusDays(HolyDayScanForward)
    while (day.isBefore(limit)) {
        if (!isForbidden(day)) {
            day = day.plusDays(1)
            continue
        }
        val runStart = day
        var runEnd = day
        while (isForbidden(runEnd.plusDays(1))) runEnd = runEnd.plusDays(1)

        // Announced from the sunset one Jewish day before the run enters. A run any later than that
        // is further off still, so there is nothing to show at all.
        val announcedFrom = sunsetForDate(location, runStart.minusDays(2), settings)
        if (now != null && announcedFrom != null && now.isBefore(announcedFrom)) return null

        // Which day of the run is current: the first that has not gone out yet.
        val current = when {
            now == null -> date.takeIf { !it.isBefore(runStart) && !it.isAfter(runEnd) }
            else -> generateSequence(runStart) { previous ->
                previous.plusDays(1).takeIf { !it.isAfter(runEnd) }
            }.firstOrNull { exitOf(it)?.isAfter(now) ?: true }
        }
        if (current == null) {
            // The whole run is behind us (or today falls outside it) — try the next one.
            day = runEnd.plusDays(1)
            continue
        }

        val entry = if (current == runStart) {
            complexZmanimCalendar(location, runStart.minusDays(1), settings).candleLighting?.toInstant()
        } else {
            // Lit from an existing flame the moment the previous day goes out.
            exitOf(current.minusDays(1))
        }
        // A fast beginning the day after the run ends is part of what is still ahead: Tzom
        // Gedalyah follows Rosh Hashana, so "חג כפול" alone would understate the stretch.
        val fastFollows = fastFollowsRun(runEnd, settings, location, inIsrael)
        val yomTovDays = daysOf(runStart, runEnd).filter { calendarFor(it).isYomTovAssurBemelacha }
        return HolyDayInfo(
            name = holyDayName(current, calendarFor(current), englishFormatter, hebrew = false),
            nameHebrew = holyDayName(current, calendarFor(current), hebrewFormatter, hebrew = true),
            startTime = entry,
            endTime = exitOf(current),
            term = holyDayTerm(current, yomTovDays, hebrew = false),
            termHebrew = holyDayTerm(current, yomTovDays, hebrew = true),
            // Named only while it is genuinely in: from its entry until its exit. That covers the
            // sunset-to-tzeit gap at the end, when the displayed date has already rolled but the
            // day is still on, and it keeps the name off before the day has begun.
            isUnderWay = if (now == null) current == displayedDate else isUnderWay(entry, exitOf(current), now),
            // Describes what is still ahead from today, so it narrows as each day goes out.
            sequel = sequelFor(current, runEnd, ::calendarFor, fastFollows, hebrew = false),
            sequelHebrew = sequelFor(current, runEnd, ::calendarFor, fastFollows, hebrew = true),
        )
    }
    return null
}

/**
 * Names a single day of a run. A Yom Tov that falls *on* Shabbat is one day that is both, and says
 * so with "and" — "יום כיפור ושבת". That is deliberately different from the "+" of the back-to-back
 * warning, which is about two separate days in a row.
 */
private fun holyDayName(
    day: LocalDate,
    calendar: JewishCalendar,
    formatter: HebrewDateFormatter,
    hebrew: Boolean,
): String {
    val yomTov = formatter.formatYomTov(calendar)
    val isYomTov = calendar.isYomTovAssurBemelacha && yomTov.isNotBlank()
    return when {
        isYomTov && day.dayOfWeek == DayOfWeek.SATURDAY ->
            if (hebrew) "$yomTov ושבת" else "$yomTov & Shabbat"
        isYomTov -> yomTov
        hebrew -> "שבת"
        else -> "Shabbat"
    }
}

/**
 * How the rest of the stretch reads from [current] onwards, or null when there is nothing to warn
 * about. Consecutive observances are joined with "+" — "חג כפול + שבת + צום". A single day that is
 * both a Yom Tov and Shabbat is joined with "and" instead — "חג כפול ושבת" — matching the way that
 * day's own name is written, so the two situations never look alike.
 */
private fun sequelFor(
    current: LocalDate,
    runEnd: LocalDate,
    calendarFor: (LocalDate) -> JewishCalendar,
    fastFollows: Boolean,
    hebrew: Boolean,
): String? {
    val remaining = daysOf(current, runEnd)
    val segments = mutableListOf<String>()

    var index = 0
    while (index < remaining.size) {
        val day = remaining[index]
        if (calendarFor(day).isYomTovAssurBemelacha) {
            // Group the consecutive Yom Tov days so they read as one "double"/"triple" unit.
            var count = 0
            var includesShabbat = false
            while (index < remaining.size && calendarFor(remaining[index]).isYomTovAssurBemelacha) {
                if (remaining[index].dayOfWeek == DayOfWeek.SATURDAY) includesShabbat = true
                count++
                index++
            }
            val yomTov = when {
                hebrew && count >= 3 -> "חג משולש"
                hebrew && count == 2 -> "חג כפול"
                hebrew -> "חג"
                count >= 3 -> "Three-day Yom Tov"
                count == 2 -> "Two-day Yom Tov"
                else -> "Yom Tov"
            }
            segments += when {
                !includesShabbat -> yomTov
                hebrew -> "$yomTov ושבת"
                else -> "$yomTov and Shabbat"
            }
        } else {
            segments += if (hebrew) "שבת" else "Shabbat"
            index++
        }
    }
    if (fastFollows) segments += if (hebrew) "צום" else "fast"

    // One thing ahead is just today, which the chip already names.
    return if (segments.size < 2) null else segments.joinToString(" + ")
}

/** Names this day's own times: Shabbat, the Yom Tov, or which day of a multi-day Yom Tov it is. */
private fun holyDayTerm(day: LocalDate, yomTovDays: List<LocalDate>, hebrew: Boolean): String {
    val position = yomTovDays.indexOf(day)
    return when {
        position < 0 -> if (hebrew) "שבת" else "Shabbat"
        yomTovDays.size < 2 -> if (hebrew) "החג" else "Yom Tov"
        hebrew -> when (position) {
            0 -> "חג ראשון"
            1 -> "חג שני"
            else -> "חג שלישי"
        }
        else -> when (position) {
            0 -> "first day"
            1 -> "second day"
            else -> "third day"
        }
    }
}

/** Whether a fast begins on the day after the run ends — Tzom Gedalyah right after Rosh Hashana. */
private fun fastFollowsRun(
    runEnd: LocalDate,
    settings: ZmanimCalculationSettings,
    location: JewishLocation,
    inIsrael: Boolean,
): Boolean {
    val nextDay = runEnd.plusDays(1)
    val calendar = JewishCalendar(nextDay).apply {
        isUseModernHolidays = true
        setInIsrael(inIsrael)
    }
    return fastDayInfo(
        jewishCalendar = calendar,
        calendar = complexZmanimCalendar(location, nextDay, settings),
        settings = settings,
        location = location,
        date = nextDay,
    ) != null
}

private fun daysOf(from: LocalDate, to: LocalDate): List<LocalDate> =
    generateSequence(from) { previous -> previous.plusDays(1).takeIf { !it.isAfter(to) } }.toList()

private const val HolyDayScanBack = 3L
private const val HolyDayScanForward = 5L

/**
 * Which observance names the header chip when a fast and a holy day overlap. Both can be on screen
 * at once — the second day of Rosh Hashana already announces Tzom Gedalyah, and a fast can fall on
 * a Friday — so the one actually happening wins over the one merely announced. When both are under
 * way the one ending sooner wins; when neither has started, the one starting sooner.
 */
private fun fastLeadsHeader(fast: FastDayInfo?, holy: HolyDayInfo?, now: Instant?): Boolean {
    if (fast == null) return false
    if (holy == null || now == null) return true

    val fastUnderWay = isUnderWay(fast.startTime, fast.endTime, now)
    val holyUnderWay = isUnderWay(holy.startTime, holy.endTime, now)
    return when {
        fastUnderWay != holyUnderWay -> fastUnderWay
        fastUnderWay -> !(fast.endTime ?: Instant.MAX).isAfter(holy.endTime ?: Instant.MAX)
        else -> !(fast.startTime ?: Instant.MAX).isAfter(holy.startTime ?: Instant.MAX)
    }
}

/** Whether [now] falls inside the window, so the observance is happening rather than announced. */
private fun isUnderWay(start: Instant?, end: Instant?, now: Instant): Boolean =
    start != null && end != null && !now.isBefore(start) && now.isBefore(end)

private fun hasBegun(start: Instant?, now: Instant): Boolean = start != null && !now.isBefore(start)

private fun startsPreviousEvening(jewishCalendar: JewishCalendar): Boolean =
    jewishCalendar.yomTovIndex == JewishCalendar.TISHA_BEAV

private fun fastDayInfo(
    jewishCalendar: JewishCalendar,
    calendar: ComplexZmanimCalendar,
    settings: ZmanimCalculationSettings,
    location: JewishLocation,
    date: LocalDate,
): FastDayInfo? {
    if (!jewishCalendar.isTaanis) return null
    val (name, nameHebrew) = FastDayNames[jewishCalendar.yomTovIndex] ?: return null
    // Tisha B'Av begins at the previous sunset, the four dawn fasts at alot. None of them carries
    // a tosefet either side — they are not holy days — so all simply end at tzeit.
    val startTime = if (startsPreviousEvening(jewishCalendar)) {
        complexZmanimCalendar(location, date.minusDays(1), settings)
            .sunset(settings.sunsetMethod)?.toInstant()
    } else {
        calendar.alotHashachar(settings)?.toInstant()
    }
    val endTime = calendar.tzeit(settings)?.toInstant()
    return FastDayInfo(name = name, nameHebrew = nameHebrew, startTime = startTime, endTime = endTime)
}