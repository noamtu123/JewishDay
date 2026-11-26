// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import com.kosherjava.zmanim.ComprehensiveZmanimCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.noamtu.jewishday.R
import com.noamtu.jewishday.i18n.CalendarText
import com.noamtu.jewishday.i18n.UiText
import com.noamtu.jewishday.i18n.asUiText
import com.noamtu.jewishday.i18n.uiText
import com.noamtu.jewishday.i18n.verbatim
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

fun zmanimForDate(
    location: JewishLocation = defaultJerusalemLocation,
    date: LocalDate,
    settings: ZmanimCalculationSettings = ZmanimCalculationSettings(),
    now: Instant? = null,
    // Overrides whether the Hebrew date has rolled past tzeit. A day stepped to from today has no
    // clock of its own, so it takes today's answer — otherwise stepping from yesterday (unrolled)
    // to an evening today (rolled) would jump the Hebrew date and weekday by two.
    hebrewDateRolled: Boolean? = null,
): ZmanimDay {
    val calendar = comprehensiveZmanimCalendar(location, date, settings)
    // Israel vs. diaspora calendar is derived from where you are, not a manual setting.
    val inIsrael = location.isInIsrael
    val jewishCalendar = JewishCalendar(date).apply {
        isUseModernHolidays = true
        setInIsrael(inIsrael)
    }
    // The displayed Hebrew date rolls over to the next day at tzeit hakochavim — not at sunset,
    // which only opens bein hashmashot, the doubtful stretch between the two days. From tzeit
    // onward the header shows the current Jewish date. Day events and the zmanim themselves stay
    // on [date] — the plain calendar day, which turns over at midnight; the fast chip/card follows
    // the active fast (see below).
    val tzeit = calendar.tzeit(settings)
    val afterTzeit = hebrewDateRolled ?: (now != null && tzeit != null && !now.isBefore(tzeit))
    val displayJewishCalendar = if (afterTzeit) {
        JewishCalendar(date.plusDays(1)).apply {
            isUseModernHolidays = true
            setInIsrael(inIsrael)
        }
    } else {
        jewishCalendar
    }
    val shabbatDates = shabbatDatesFor(date, location, settings, now)
    val shabbatStartCalendar = comprehensiveZmanimCalendar(location, shabbatDates.startDate, settings)
    val shabbatCandles = shabbatStartCalendar.candleLighting(settings)
    val shabbatEndCalendar = comprehensiveZmanimCalendar(location, shabbatDates.endDate, settings)
    // The parsha is only attached to a Shabbat date, so read it from the upcoming
    // Shabbat to always show "this week's" reading even on a weekday.
    val shabbatJewishCalendar = JewishCalendar(shabbatDates.endDate).apply {
        isUseModernHolidays = true
        setInIsrael(inIsrael)
    }

    // The upcoming parsha now lives at the top of the Shabbat section.
    val parshaItem = if (shabbatJewishCalendar.parshah != JewishCalendar.Parshah.NONE) {
        ZmanItem(
            title = uiText(R.string.row_weekly_parsha),
            time = null,
            description = uiText(R.string.row_upcoming_reading),
            value = CalendarText.Parsha(shabbatDates.endDate, inIsrael).asUiText(),
            kind = ZmanItemKind.WeeklyParsha,
        )
    } else {
        null
    }
    // A Shabbat that is a Yom Tov or Chol Hamoed has no weekly parsha at all, and KosherJava
    // returns a blank one — which left the section showing an entry and an exit with nothing to
    // say which Shabbat they belong to. Name the day instead ("ראש השנה"), under its own title,
    // since "פרשת השבוע: ראש השנה" would not be true.
    //
    // Only the section does this. On Shabbat itself the section is dropped and the row moves up to
    // the day's events, where the header is already naming the day — a second copy would just
    // repeat it, which is why the promoted row below stays [parshaItem].
    val shabbatReadingItem = parshaItem ?: run {
        if (!shabbatJewishCalendar.hasDayName()) {
            null
        } else {
            ZmanItem(
                title = uiText(R.string.row_torah_reading),
                time = null,
                description = uiText(R.string.row_upcoming_reading),
                value = CalendarText.YomTov(shabbatDates.endDate, inIsrael).asUiText(),
                kind = ZmanItemKind.TorahReading,
            )
        }
    }
    // Occasional day events (Yom Tov, Rosh Chodesh, Omer, fasts…); shown header-less at the
    // top of the tab and omitted entirely when empty (no standalone "Daily" section).
    val eventItems = dailyItems(
        jewishCalendar = jewishCalendar,
        calendar = calendar,
        settings = settings,
        date = date,
        inIsrael = inIsrael,
    )
    // The header's fast chip/card is announced one Jewish day before the fast begins and clears
    // the moment it ends: an evening fast (Yom Kippur, Tisha B'Av) begins at the sunset before its
    // own date, so it appears a sunset earlier still; a dawn fast appears at alot of the previous
    // morning. This also closes the Erev Tisha B'Av / Erev Yom Kippur evening gap and stops a
    // finished fast lingering until midnight.
    // The Hebrew date shown in the header, as a civil date: it rolls at tzeit, so from nightfall
    // onwards "today" is already tomorrow.
    val displayedDate = if (afterTzeit) date.plusDays(1) else date
    // Read off the same calendar as the Hebrew date in the header, so the two never disagree: at
    // sunset on Erev Pesach the date becomes Pesach and the chip stops saying "ערב פסח" with it.
    val dayName = minorDayName(displayJewishCalendar, displayedDate, inIsrael)
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
    )

    // Rabbeinu Tam is the one row not otherwise on screen, so it moves into the day's own list when
    // the section goes — but only on Shabbat itself, where today's calendar gives the same time.
    // On Friday night it would be tomorrow night's, landing out of order among today's zmanim.
    val shabbatIsToday = date.dayOfWeek == DayOfWeek.SATURDAY && shabbatDates.endDate == date

    // Once Shabbat is in, the section only repeats what is already on screen: the header names it
    // and gives its entry and exit, and the day's own zmanim list is the one that matters. Matching
    // on the exit is what decides it — "the header already tells you when this ends" — which also
    // covers a Yom Tov that ends together with the Shabbat it falls on. Before Shabbat comes in the
    // section is the only place the times are, so it stays; after motzei it already points at next
    // week's Shabbat, which is not a duplicate, so it stays then too. And when Shabbat is part of a
    // longer stretch whose exit is a later day's, the section is what says when Shabbat itself ends,
    // so it stays as well.
    val shabbatExit = shabbatEndCalendar.holyDayExit(settings)
    val shabbatSectionRepeatsToday = shabbatExit != null &&
        shabbatIsToday &&
        holyDayInfo != null &&
        holyDayInfo.isUnderWay &&
        holyDayInfo.endTime == shabbatExit

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
        displayedDate = displayedDate,
        zoneId = location.zoneId,
        hebrewDate = CalendarText.HebrewDate(displayedDate).asUiText(),
        dayName = dayName,
        fastDayInfo = fastDayInfo,
        holyDayInfo = holyDayInfo,
        fastLeadsHeader = fastLeadsHeader(fastDayInfo, holyDayInfo, now),
        groups = listOfNotNull(
            // The parsha lives at the top of the Shabbat section; when that section is dropped it
            // moves up here rather than disappearing.
            (eventItems + listOfNotNull(parshaItem.takeIf { shabbatSectionRepeatsToday }))
                .takeIf { it.isNotEmpty() }
                ?.let { items -> ZmanimGroup(ZmanimGroupKind.Events, items) },
            ZmanimGroup(
                kind = ZmanimGroupKind.Zmanim,
                // One continuous list (no morning/afternoon split). Sof Zman Shema and
                // Tefillah each appear twice — Magen Avraham first, then GRA — each with its
                // own configurable method (the caption shows the precise method chosen).
                // Each row carries its ZmanimTimeOption id so it can be shown/hidden.
                items = listOfNotNull(
                    zman(ZmanimTimeOption.AlotHashachar, calendar.alotHashachar(settings), settings.alotHashacharMethod.caption(settings)),
                    zman(ZmanimTimeOption.TallitTefillin, calendar.misheyakir(settings), settings.misheyakirMethod.caption(settings)),
                    zman(ZmanimTimeOption.Sunrise, calendar.sunrise(settings.sunriseMethod), settings.sunriseMethod.label),
                    zman(ZmanimTimeOption.SofZmanShemaMagenAvraham, calendar.sofZmanShema(settings.sofZmanShemaMethod, settings), settings.sofZmanShemaMethod.caption(settings)),
                    zman(ZmanimTimeOption.SofZmanShemaGra, calendar.sofZmanShema(settings.sofZmanShemaGraMethod, settings), settings.sofZmanShemaGraMethod.caption(settings)),
                    zman(ZmanimTimeOption.SofZmanTefillahMagenAvraham, calendar.sofZmanTefillah(settings.sofZmanTefillahMethod, settings), settings.sofZmanTefillahMethod.caption(settings)),
                    zman(ZmanimTimeOption.SofZmanTefillahGra, calendar.sofZmanTefillah(settings.sofZmanTefillahGraMethod, settings), settings.sofZmanTefillahGraMethod.caption(settings)),
                    zman(ZmanimTimeOption.ChatzotHaYom, calendar.chatzot(settings.chatzotMethod), settings.chatzotMethod.label),
                    zman(ZmanimTimeOption.MinchaGedola, calendar.minchaGedola(settings), settings.minchaGedolaMethod.caption(settings)),
                    zman(ZmanimTimeOption.MinchaKetana, calendar.minchaKetana(settings), settings.minchaKetanaMethod.caption(settings)),
                    zman(ZmanimTimeOption.PlagHamincha, calendar.plagHamincha(settings), settings.plagHaminchaMethod.caption(settings)),
                    zman(ZmanimTimeOption.Sunset, calendar.sunset(settings.sunsetMethod), settings.sunsetMethod.label),
                    zman(ZmanimTimeOption.Tzeit, calendar.tzeit(settings), settings.tzeitHakochavimMethod.caption(settings)),
                    // Only on Shabbat, where the Shabbat section it normally lives in is dropped.
                    zman(ZmanimTimeOption.RabbeinuTam, calendar.rabbeinuTam(settings), settings.rabbeinuTamMethod.caption(settings))
                        .takeIf { shabbatSectionRepeatsToday },
                    zman(ZmanimTimeOption.ChatzotHaLaila, upcomingChatzotHaLailaFor(location, date, settings, now), settings.chatzotHaLailaMethod.label),
                ),
            ),
            if (shabbatSectionRepeatsToday) null else ZmanimGroup(
                kind = ZmanimGroupKind.Shabbat,
                // The parsha (no id) always shows; the four time rows carry their ZmanimTimeOption
                // id so the same "Zmanim to show" list can hide them.
                items = listOfNotNull(
                    shabbatReadingItem,
                    ZmanItem(
                        title = uiText(R.string.shabbat_row_candles),
                        time = shabbatCandles?.time,
                        description = uiText(R.string.shabbat_caption_friday, shabbatCandles.caption(settings)),
                        id = ZmanimTimeOption.ShabbatCandleLighting.storageValue,
                    ),
                    ZmanItem(
                        title = uiText(R.string.zman_sunset),
                        time = shabbatStartCalendar.sunset(settings.sunsetMethod),
                        description = uiText(R.string.shabbat_caption_friday, settings.sunsetMethod.label),
                        id = ZmanimTimeOption.ShabbatSunset.storageValue,
                    ),
                    ZmanItem(
                        title = uiText(ZmanimTimeOption.MotzeiShabbat.label),
                        time = shabbatExit,
                        description = uiText(R.string.shabbat_caption_motzei, settings.motzeiShabbatMethod.caption(settings), settings.holyDayTosefetMinutes),
                        id = ZmanimTimeOption.MotzeiShabbat.storageValue,
                    ),
                    ZmanItem(
                        title = uiText(ZmanimTimeOption.RabbeinuTam.label),
                        time = shabbatEndCalendar.rabbeinuTam(settings),
                        description = uiText(R.string.shabbat_caption_saturday, settings.rabbeinuTamMethod.caption(settings)),
                        id = ZmanimTimeOption.RabbeinuTam.storageValue,
                    ),
                ),
            ),
            ZmanimGroup(
                kind = ZmanimGroupKind.DailyLearning,
                items = dailyLearningItems(jewishCalendar = jewishCalendar, date = date, inIsrael = inIsrael),
            ),
        ),
    )
}

/** A row of the zmanim list: the time [option] names, and the method that gave it. */
private fun zman(option: ZmanimTimeOption, time: Instant?, method: UiText): ZmanItem =
    ZmanItem(title = uiText(option.label), time = time, description = method, id = option.storageValue)

/** Whether KosherJava has a name for the day — a holiday, a fast, a named day — or it is ordinary. */
private fun JewishCalendar.hasDayName(): Boolean = yomTovIndex != -1

/**
 * The chatzot halaila to list: the midnight of the night in progress, and the coming one once that
 * has passed. Not the listed day's own — from tzeit the day has already rolled to tomorrow, and
 * tomorrow's midnight is a day away, while "when is midnight tonight" is the question this row is
 * read to answer. Without a clock there is no night in progress, so the day's own is used.
 */
private fun upcomingChatzotHaLailaFor(
    location: JewishLocation,
    date: LocalDate,
    settings: ZmanimCalculationSettings,
    now: Instant?,
): Instant? = now?.let { upcomingChatzotHaLaila(location, settings, it) }
    ?: chatzotHaLailaForDate(location, date, settings)

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
    // Shabbat is out when the header says it is out — at motzei *plus* the user's tosefet, not at
    // bare motzei. Rolling this section over at motzei instead left a tosefet-long window on
    // Saturday night where the section already showed next week's candle lighting while the header
    // still said Shabbat was in, so the screen contradicted itself.
    val afterMotzeiShabbat = date.dayOfWeek == DayOfWeek.SATURDAY &&
        now != null &&
        holyDayExitForDate(location, date, settings)?.let { !now.isBefore(it) } == true
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
): Instant? = comprehensiveZmanimCalendar(location, date, settings)
    .tzeit(settings)

/** Sunset for [date]. The Hebrew date rolls at tzeit, not here — see [tzeitForDate]. */
fun sunsetForDate(
    location: JewishLocation = defaultJerusalemLocation,
    date: LocalDate,
    settings: ZmanimCalculationSettings = ZmanimCalculationSettings(),
): Instant? = comprehensiveZmanimCalendar(location, date, settings)
    .sunset(settings.sunsetMethod)

/** When a holy day beginning or ending on [date] goes out: motzei plus the user's tosefet. */
fun holyDayExitForDate(
    location: JewishLocation = defaultJerusalemLocation,
    date: LocalDate,
    settings: ZmanimCalculationSettings = ZmanimCalculationSettings(),
): Instant? = comprehensiveZmanimCalendar(location, date, settings)
    .holyDayExit(settings)

private fun dailyItems(
    jewishCalendar: JewishCalendar,
    calendar: ComprehensiveZmanimCalendar,
    settings: ZmanimCalculationSettings,
    date: LocalDate,
    inIsrael: Boolean,
): List<ZmanItem> = buildList {
    // The Jewish date is shown in the date header at the top of the tab, and the parsha at the top
    // of the Shabbat section; this list is only the occasional day events.
    //
    // Named days no longer get a row: Chol Hamoed, Purim, Erev Pesach, Isru Chag and the modern
    // holidays are all said by the header chip instead (see [minorDayName]), which is where the
    // eye already is. A Yom Tov proper was never here either — the header names it along with its
    // entry and exit.
    if (jewishCalendar.isRoshChodesh) {
        add(
            ZmanItem(
                title = uiText(R.string.row_rosh_chodesh),
                time = null,
                description = uiText(R.string.row_rosh_chodesh_description),
                value = CalendarText.RoshChodesh(date, inIsrael).asUiText(),
                kind = ZmanItemKind.RoshChodesh,
            ),
        )
    }
    if (jewishCalendar.dayOfOmer != -1) {
        add(
            ZmanItem(
                title = uiText(R.string.row_omer),
                time = null,
                description = uiText(R.string.row_omer_description),
                value = CalendarText.Omer(date, inIsrael).asUiText(),
                kind = ZmanItemKind.Omer,
            ),
        )
    }
    if (jewishCalendar.isChanukah) {
        add(
            ZmanItem(
                title = uiText(R.string.row_chanukah),
                time = null,
                description = uiText(R.string.row_chanukah_description),
                value = verbatim(jewishCalendar.dayOfChanukah.toString()),
            ),
        )
    }
    if (jewishCalendar.yomTovIndex == JewishCalendar.EREV_PESACH) {
        val chametzTimes = calendar.chametzTimes(settings)
        add(ZmanItem(uiText(R.string.row_eat_chametz), chametzTimes.first, settings.chametzMethod.caption(settings)))
        add(ZmanItem(uiText(R.string.row_burn_chametz), chametzTimes.second, settings.chametzMethod.caption(settings)))
    }
}

private val FastDayNames: Map<Int, Int> = mapOf(
    JewishCalendar.FAST_OF_GEDALYAH to R.string.fast_gedalyah,
    JewishCalendar.TISHA_BEAV to R.string.fast_tisha_bav,
    JewishCalendar.SEVENTEEN_OF_TAMMUZ to R.string.fast_17_tammuz,
    JewishCalendar.TENTH_OF_TEVES to R.string.fast_10_tevet,
    JewishCalendar.FAST_OF_ESTHER to R.string.fast_esther,
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
            calendar = comprehensiveZmanimCalendar(location, fastDate, settings),
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
    // Begins at the sunset before [date]; announce it a Jewish day earlier, at the tzeit before it.
    tzeitForDate(location, date.minusDays(2), settings)
} else {
    // Begins at alot on [date]; announce it from alot the previous morning.
    comprehensiveZmanimCalendar(location, date.minusDays(1), settings).alotHashachar(settings)
}

/**
 * The holy day to show in the date header, or null.
 *
 * Back-to-back days — both days of Rosh Hashana, a Yom Tov running into Shabbat, Shabbat running
 * into a Yom Tov — are one span, not one card per day: the first day's candle lighting and the last
 * day's exit, since what is asked of the stretch is the same throughout and the boundaries inside it
 * are not something to act on. [HolyDayInfo.sequel] says what the stretch is made of, and the name
 * still follows whichever day is currently in.
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
): HolyDayInfo? {
    fun calendarFor(day: LocalDate) = JewishCalendar(day).apply {
        isUseModernHolidays = true
        setInIsrael(inIsrael)
    }
    fun isForbidden(day: LocalDate) = calendarFor(day).isAssurBemelacha
    fun exitOf(day: LocalDate) = comprehensiveZmanimCalendar(location, day, settings).holyDayExit(settings)

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

        // Announced from the tzeit one Jewish day before the run enters — the same boundary the
        // Hebrew date turns on. A run any later than that is further off still, so there is nothing
        // to show at all.
        val announcedFrom = tzeitForDate(location, runStart.minusDays(2), settings)
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

        // The stretch as a whole: in at the first day's candle lighting, out when the last day goes
        // out. The boundaries in between — where candles are lit from an existing flame — change
        // nothing about what is forbidden, so they are not times anyone needs from the header.
        val entry = comprehensiveZmanimCalendar(location, runStart.minusDays(1), settings)
            .candleLighting(settings)?.time
        val exit = exitOf(runEnd)
        // A fast beginning the day after the run ends is part of what is still ahead: Tzom
        // Gedalyah follows Rosh Hashana, so "חג כפול" alone would understate the stretch.
        val fastFollows = fastFollowsRun(runEnd, settings, location, inIsrael)
        // Which days of the run are Yom Tov, so the two ends can say *which* day of it they are.
        val yomTovDays = daysOf(runStart, runEnd).filter { calendarFor(it).isYomTovAssurBemelacha }
        return HolyDayInfo(
            name = holyDayName(current, calendarFor(current), inIsrael),
            parsha = parshaLabel(current, calendarFor(current), inIsrael),
            startTime = entry,
            endTime = exit,
            entryTerm = holyDayTerm(runStart, yomTovDays),
            exitTerm = holyDayTerm(runEnd, yomTovDays),
            // Named only while the stretch is genuinely in: from its entry until its exit. That
            // covers the tail end, when the displayed date has already rolled but the day is still
            // on (motzei carries a tosefet past tzeit), and keeps the name off before it has begun.
            isUnderWay = if (now == null) current == displayedDate else isUnderWay(entry, exit, now),
            // Describes what is still ahead from today, so it narrows as each day goes out.
            sequel = sequelFor(current, runEnd, ::calendarFor, fastFollows),
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
    inIsrael: Boolean,
): UiText {
    val yomTov = CalendarText.YomTov(day, inIsrael).asUiText()
    val isYomTov = calendar.isYomTovAssurBemelacha && calendar.hasDayName()
    // Chol Hamoed and Isru Chag often fall on Shabbat. The chip would say only "שבת" and the day's
    // own name would be lost with it, so the two are read together: "שבת חול המועד פסח". An erev is
    // already excluded by [minorDayName].
    val minorName = minorDayName(calendar, day, inIsrael)
    return when {
        isYomTov && day.dayOfWeek == DayOfWeek.SATURDAY -> uiText(R.string.holy_day_with_shabbat, yomTov)
        isYomTov -> yomTov
        minorName == null -> uiText(R.string.holy_day_shabbat)
        else -> uiText(R.string.holy_day_shabbat_named, minorName)
    }
}

/**
 * The erevs that go unnamed: on these the coming Yom Tov is already on screen with its entry and
 * exit, and "ערב ראש השנה" beside that card says nothing the card does not. Erev Pesach (the chametz
 * deadlines) and Erev Yom Kippur (the seudah mafseket) carry something of their own and keep their
 * names.
 *
 * Listed by index rather than read from `isErevYomTov`, which also counts Hoshana Rabba and the last
 * day of Chol Hamoed Pesach — days with a name of their own that must not be lost.
 */
private val UnnamedErevDays = setOf(
    JewishCalendar.EREV_ROSH_HASHANA,
    JewishCalendar.EREV_SUCCOS,
    JewishCalendar.EREV_SHAVUOS,
)

/**
 * The day's own name when nothing else in the header is going to say it — "פורים", "חול המועד
 * סוכות", "אסרו חג", "הושענא רבה", and Erev Pesach and Erev Yom Kippur.
 *
 * Null for a Yom Tov and for a fast, which the header names in full (with their entry and exit)
 * rather than as a bare chip.
 */
private fun minorDayName(calendar: JewishCalendar, date: LocalDate, inIsrael: Boolean): UiText? {
    if (calendar.isYomTovAssurBemelacha || calendar.isTaanis) return null
    if (calendar.yomTovIndex in UnnamedErevDays) return null
    return CalendarText.YomTov(date, inIsrael).asUiText().takeIf { calendar.hasDayName() }
}

/**
 * What names the coming Shabbat while it is only announced — the parasha, so the entry/exit card
 * has a heading without the chip claiming Shabbat is already in. Null for a Yom Tov, which has no
 * weekly parsha of its own, and for any day that is not a Shabbat.
 */
private fun parshaLabel(
    day: LocalDate,
    calendar: JewishCalendar,
    inIsrael: Boolean,
): UiText? {
    if (day.dayOfWeek != DayOfWeek.SATURDAY) return null
    if (calendar.parshah == JewishCalendar.Parshah.NONE) return null
    return uiText(R.string.parsha_named, CalendarText.Parsha(day, inIsrael).asUiText())
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
): UiText? {
    val remaining = daysOf(current, runEnd)
    val segments = mutableListOf<UiText>()

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
            val yomTov = uiText(
                when {
                    count >= 3 -> R.string.sequel_three_day_yom_tov
                    count == 2 -> R.string.sequel_two_day_yom_tov
                    else -> R.string.sequel_yom_tov
                },
            )
            segments += if (includesShabbat) uiText(R.string.sequel_with_shabbat, yomTov) else yomTov
        } else {
            segments += uiText(R.string.sequel_shabbat)
            index++
        }
    }
    if (fastFollows) segments += uiText(R.string.sequel_fast)

    // One thing ahead is just today, which the chip already names.
    return if (segments.size < 2) null else segments.reduce { ahead, next -> uiText(R.string.sequel_join, ahead, next) }
}

/**
 * Names one end of the span, by the day it actually falls on. The span covers a whole run, so its two
 * ends usually belong to different days and saying which one is the point: a two-day Yom Tov reads
 * "כניסת חג ראשון" … "צאת חג שני", and a Yom Tov running into Shabbat "כניסת החג" … "צאת שבת". Without
 * that, both ends would say "החג" and nothing on screen would say how far the stretch reaches.
 *
 * [day] is one end of the run and [yomTovDays] the Yom Tov days within it, so the ordinal counts
 * within the run rather than within the festival.
 */
private fun holyDayTerm(day: LocalDate, yomTovDays: List<LocalDate>): HolyDayTerm {
    val position = yomTovDays.indexOf(day)
    return when {
        position < 0 -> HolyDayTerm.Shabbat
        yomTovDays.size < 2 -> HolyDayTerm.YomTov
        position == 0 -> HolyDayTerm.FirstDay
        position == 1 -> HolyDayTerm.SecondDay
        else -> HolyDayTerm.ThirdDay
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
        calendar = comprehensiveZmanimCalendar(location, nextDay, settings),
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
    calendar: ComprehensiveZmanimCalendar,
    settings: ZmanimCalculationSettings,
    location: JewishLocation,
    date: LocalDate,
): FastDayInfo? {
    if (!jewishCalendar.isTaanis) return null
    val name = FastDayNames[jewishCalendar.yomTovIndex] ?: return null
    // Tisha B'Av begins at the previous sunset, the four dawn fasts at alot. None of them carries
    // a tosefet either side — they are not holy days — so all simply end at tzeit.
    val startTime = if (startsPreviousEvening(jewishCalendar)) {
        comprehensiveZmanimCalendar(location, date.minusDays(1), settings)
            .sunset(settings.sunsetMethod)
    } else {
        calendar.alotHashachar(settings)
    }
    val endTime = calendar.tzeit(settings)
    return FastDayInfo(name = uiText(name), startTime = startTime, endTime = endTime)
}