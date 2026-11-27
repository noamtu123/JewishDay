// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

import com.kosherjava.zmanim.ComprehensiveZmanimCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.YerushalmiYomiCalculator
import com.noamtu.jewishday.core.i18n.CalendarText
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/**
 * Works out everything the zmanim screen shows for [date] — see [ZmanimDayFacts].
 *
 * [now] places the day in time: it decides whether the Hebrew date has rolled, which fast or holy
 * day is announced, and which Shabbat the section is about. Without it the day is read whole.
 * [hebrewDateRolled] overrides whether the Hebrew date has rolled past tzeit.
 */
public fun zmanimDayFacts(
    location: JewishLocation = defaultJerusalemLocation,
    date: LocalDate,
    settings: ZmanimCalculationSettings = ZmanimCalculationSettings(),
    now: Instant? = null,
    hebrewDateRolled: Boolean? = null,
): ZmanimDayFacts {
    val calendar = comprehensiveZmanimCalendar(location, date, settings)
    // Israel vs. diaspora calendar is derived from where you are, not a manual setting.
    val inIsrael = location.isInIsrael
    val jewishCalendar = jewishCalendar(date, inIsrael)
    // The displayed Hebrew date rolls over to the next day at tzeit hakochavim — not at sunset,
    // which only opens bein hashmashot, the doubtful stretch between the two days. From tzeit
    // onward the header shows the current Jewish date. Day events and the zmanim themselves stay
    // on [date] — the plain calendar day, which turns over at midnight; the fast chip/card follows
    // the active fast (see [announcedFastDayInfo]).
    val tzeit = calendar.tzeit(settings)
    val afterTzeit = hebrewDateRolled ?: (now != null && tzeit != null && !now.isBefore(tzeit))
    // Far enough north in summer yesterday's tzeit comes after midnight, and until it does the Hebrew
    // date is still yesterday's (see [jewishDayCivilDate]). Only for the day [now] is on: any other
    // day is read as it stands.
    val stillYesterday = hebrewDateRolled == null && now != null &&
        now.atZone(location.zoneId).toLocalDate() == date &&
        beforeYesterdaysTzeit(location, date, settings, now)
    val displayedDate = when {
        stillYesterday -> date.minusDays(1)
        afterTzeit -> date.plusDays(1)
        else -> date
    }
    val displayJewishCalendar = if (displayedDate == date) jewishCalendar else jewishCalendar(displayedDate, inIsrael)

    val shabbatDates = shabbatDatesFor(date, location, settings, now)
    val shabbatStartCalendar = comprehensiveZmanimCalendar(location, shabbatDates.startDate, settings)
    val shabbatEndCalendar = comprehensiveZmanimCalendar(location, shabbatDates.endDate, settings)
    // The parsha is only attached to a Shabbat date, so read it from the upcoming Shabbat to always
    // show "this week's" reading even on a weekday.
    val shabbatJewishCalendar = jewishCalendar(shabbatDates.endDate, inIsrael)
    val weeklyParsha = CalendarText.Parsha(shabbatDates.endDate, inIsrael)
        .takeIf { shabbatJewishCalendar.parshah != JewishCalendar.Parshah.NONE }

    // The header's fast chip/card is announced one Jewish day before the fast begins and clears
    // the moment it ends: an evening fast (Yom Kippur, Tisha B'Av) begins at the sunset before its
    // own date, so it appears a sunset earlier still; a dawn fast appears at alot of the previous
    // morning. This also closes the Erev Tisha B'Av / Erev Yom Kippur evening gap and stops a
    // finished fast lingering until midnight.
    val announcedFast = announcedFastDayInfo(date, displayedDate, now, settings, location, inIsrael)
    // Shabbat and Yom Tov get the same header treatment, over the same window: the whole
    // melacha-forbidden stretch is one card, announced one Jewish day before it enters and cleared
    // once it goes out.
    val holyDayInfo = announcedHolyDayInfo(date, displayedDate, now, settings, location, inIsrael)

    // Once Shabbat is in, the section only repeats what is already on screen: the header names it
    // and gives its entry and exit, and the day's own zmanim list is the one that matters. Matching
    // on the exit is what decides it — "the header already tells you when this ends" — which also
    // covers a Yom Tov that ends together with the Shabbat it falls on. Before Shabbat comes in the
    // section is the only place the times are, so it stays; after motzei it already points at next
    // week's Shabbat, which is not a duplicate, so it stays then too. And when Shabbat is part of a
    // longer stretch whose exit is a later day's, the section is what says when Shabbat itself ends,
    // so it stays as well.
    //
    // Only on Shabbat itself, where today's calendar gives the same times: on Friday night they
    // would be tomorrow night's.
    val shabbatIsToday = date.dayOfWeek == DayOfWeek.SATURDAY && shabbatDates.endDate == date
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

    return ZmanimDayFacts(
        location = location,
        date = date,
        displayedDate = displayedDate,
        // Read off the same calendar as the Hebrew date in the header, so the two never disagree: at
        // sunset on Erev Pesach the date becomes Pesach and the chip stops saying "ערב פסח" with it.
        dayName = minorDayName(displayJewishCalendar, displayedDate, inIsrael),
        fastDayInfo = fastDayInfo,
        holyDayInfo = holyDayInfo,
        fastLeadsHeader = fastLeadsHeader(fastDayInfo, holyDayInfo, now),
        events = dayEvents(jewishCalendar, calendar, settings, date, inIsrael),
        zmanim = dayZmanim(
            calendar = calendar,
            settings = settings,
            // Rabbeinu Tam is the one row not otherwise on screen, so it moves into the day's own
            // list when the section goes.
            includeRabbeinuTam = shabbatSectionRepeatsToday,
            chatzotHaLaila = upcomingChatzotHaLailaFor(location, date, settings, now),
        ),
        weeklyParsha = weeklyParsha,
        shabbat = if (shabbatSectionRepeatsToday) {
            null
        } else {
            ShabbatTimes(
                // A Shabbat that is a Yom Tov or Chol Hamoed has no weekly parsha at all, and
                // KosherJava returns a blank one — which left the section showing an entry and an exit
                // with nothing to say which Shabbat they belong to. The day is named instead.
                dayName = CalendarText.YomTov(shabbatDates.endDate, inIsrael)
                    .takeIf { weeklyParsha == null && shabbatJewishCalendar.hasDayName() },
                candleLighting = shabbatStartCalendar.candleLighting(settings),
                sunset = shabbatStartCalendar.sunset(settings.sunsetMethod),
                exit = shabbatExit,
                rabbeinuTam = shabbatEndCalendar.rabbeinuTam(settings),
                exitFallback = shabbatEndCalendar.holyDayExitFallback(settings),
            )
        },
        offlineLearning = offlineLearning(jewishCalendar, date, inIsrael),
    )
}

/**
 * One continuous list (no morning/afternoon split). Sof Zman Shema and Tefillah each appear twice —
 * Magen Avraham first, then GRA — each with its own configurable method.
 */
private fun dayZmanim(
    calendar: ComprehensiveZmanimCalendar,
    settings: ZmanimCalculationSettings,
    includeRabbeinuTam: Boolean,
    chatzotHaLaila: Instant?,
): List<ZmanTime> = listOfNotNull(
    ZmanTime(ZmanimTimeOption.AlotHashachar, calendar.alotHashachar(settings), calendar.alotFallback(settings)),
    ZmanTime(ZmanimTimeOption.TallitTefillin, calendar.misheyakir(settings), calendar.misheyakirFallback(settings)),
    ZmanTime(ZmanimTimeOption.Sunrise, calendar.sunrise(settings.sunriseMethod)),
    ZmanTime(
        ZmanimTimeOption.SofZmanShemaMagenAvraham,
        calendar.sofZmanShema(settings.sofZmanShemaMethod, settings),
        calendar.sofZmanShemaFallback(settings.sofZmanShemaMethod, settings),
    ),
    ZmanTime(ZmanimTimeOption.SofZmanShemaGra, calendar.sofZmanShema(settings.sofZmanShemaGraMethod, settings)),
    ZmanTime(
        ZmanimTimeOption.SofZmanTefillahMagenAvraham,
        calendar.sofZmanTefillah(settings.sofZmanTefillahMethod, settings),
        calendar.sofZmanTefillahFallback(settings.sofZmanTefillahMethod, settings),
    ),
    ZmanTime(ZmanimTimeOption.SofZmanTefillahGra, calendar.sofZmanTefillah(settings.sofZmanTefillahGraMethod, settings)),
    ZmanTime(ZmanimTimeOption.ChatzotHaYom, calendar.chatzot(settings.chatzotMethod)),
    ZmanTime(ZmanimTimeOption.MinchaGedola, calendar.minchaGedola(settings)),
    ZmanTime(ZmanimTimeOption.MinchaKetana, calendar.minchaKetana(settings)),
    ZmanTime(ZmanimTimeOption.PlagHamincha, calendar.plagHamincha(settings)),
    ZmanTime(ZmanimTimeOption.Sunset, calendar.sunset(settings.sunsetMethod)),
    ZmanTime(ZmanimTimeOption.Tzeit, calendar.tzeit(settings), calendar.tzeitFallback(settings)),
    ZmanTime(ZmanimTimeOption.RabbeinuTam, calendar.rabbeinuTam(settings)).takeIf { includeRabbeinuTam },
    ZmanTime(ZmanimTimeOption.ChatzotHaLaila, chatzotHaLaila),
)

/**
 * The occasional day events. Named days get none: Chol Hamoed, Purim, Erev Pesach, Isru Chag and the
 * modern holidays are all said by the header chip instead (see [minorDayName]), which is where the
 * eye already is. A Yom Tov proper was never here either — the header names it along with its entry
 * and exit.
 */
private fun dayEvents(
    jewishCalendar: JewishCalendar,
    calendar: ComprehensiveZmanimCalendar,
    settings: ZmanimCalculationSettings,
    date: LocalDate,
    inIsrael: Boolean,
): List<DayEvent> = buildList {
    if (jewishCalendar.isRoshChodesh) add(DayEvent.RoshChodesh(CalendarText.RoshChodesh(date, inIsrael)))
    if (jewishCalendar.dayOfOmer != -1) add(DayEvent.Omer(CalendarText.Omer(date, inIsrael)))
    if (jewishCalendar.isChanukah) add(DayEvent.Chanukah(jewishCalendar.dayOfChanukah))
    if (jewishCalendar.yomTovIndex == JewishCalendar.EREV_PESACH) {
        val (eatUntil, burnUntil) = calendar.chametzTimes(settings)
        add(DayEvent.Chametz(eatUntil, burnUntil))
    }
}

private fun offlineLearning(jewishCalendar: JewishCalendar, date: LocalDate, inIsrael: Boolean): List<OfflineLearning> =
    buildList {
        add(OfflineLearning(OfflineLearningTrack.DafYomiBavli, CalendarText.DafYomiBavli(date, inIsrael)))
        // The Yerushalmi cycle skips a few days a year (Yom Kippur, Tisha B'Av); the row goes with them.
        val hasYerushalmiDaf = runCatching { YerushalmiYomiCalculator.getDafYomiYerushalmi(jewishCalendar) }.getOrNull() != null
        if (hasYerushalmiDaf) {
            add(OfflineLearning(OfflineLearningTrack.DafYomiYerushalmi, CalendarText.DafYomiYerushalmi(date, inIsrael)))
        }
        add(OfflineLearning(OfflineLearningTrack.TehillimYomi, CalendarText.TehillimYomi(date, inIsrael)))
    }

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

private data class ShabbatDates(val startDate: LocalDate, val endDate: LocalDate)

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

/** Tzeit for [date] — or, on a night that has none, its stand-in (see [NoTzeitFallback]). */
public fun tzeitForDate(
    location: JewishLocation = defaultJerusalemLocation,
    date: LocalDate,
    settings: ZmanimCalculationSettings = ZmanimCalculationSettings(),
): Instant? = comprehensiveZmanimCalendar(location, date, settings)
    .tzeit(settings)

/** Sunset for [date]. The Hebrew date rolls at tzeit, not here — see [tzeitForDate]. */
public fun sunsetForDate(
    location: JewishLocation = defaultJerusalemLocation,
    date: LocalDate,
    settings: ZmanimCalculationSettings = ZmanimCalculationSettings(),
): Instant? = comprehensiveZmanimCalendar(location, date, settings)
    .sunset(settings.sunsetMethod)

/** When a holy day beginning or ending on [date] goes out: motzei plus the user's tosefet. */
public fun holyDayExitForDate(
    location: JewishLocation = defaultJerusalemLocation,
    date: LocalDate,
    settings: ZmanimCalculationSettings = ZmanimCalculationSettings(),
): Instant? = comprehensiveZmanimCalendar(location, date, settings)
    .holyDayExit(settings)
