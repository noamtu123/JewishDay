// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import com.noamtu.jewishday.R
import com.noamtu.jewishday.core.i18n.CalendarText
import com.noamtu.jewishday.core.model.DayEvent
import com.noamtu.jewishday.core.model.ZmanimDayFacts
import com.noamtu.jewishday.core.model.ZmanimTimeOption
import com.noamtu.jewishday.i18n.DateStyle
import com.noamtu.jewishday.i18n.TextResolver
import com.noamtu.jewishday.i18n.UiText
import com.noamtu.jewishday.i18n.asUiText
import com.noamtu.jewishday.i18n.ends
import com.noamtu.jewishday.i18n.nameText
import com.noamtu.jewishday.i18n.parshaText
import com.noamtu.jewishday.i18n.sequelText
import com.noamtu.jewishday.i18n.shortLabel
import com.noamtu.jewishday.i18n.starts
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/**
 * Everything the home-screen widgets show, already worded in the app's language. Built from the
 * same facts ([ZmanimDayFacts]) as the main screen's day, so a widget can never disagree with the app.
 */
data class WidgetContent(
    val rtl: Boolean,
    val hebrewDate: String,
    // The civil weekday and date. A widget is read on its own, like the status-bar icon, so it
    // follows the calendar day rather than rolling at tzeit with the Hebrew date above it.
    val gregorianDate: String,
    // The same date in two parts, for a widget that sets them on two lines: "יום ראשון" and "11 באוקטובר".
    val weekday: String,
    val civilDate: String,
    // What makes today special — "חול המועד סוכות · ראש חודש חשוון" — or null on an ordinary day.
    val specialDay: String?,
    // Today's times in order: the zmanim the user shows, plus a holy day's or fast's own start and
    // end when they fall today.
    val zmanim: List<WidgetZman>,
    // The first of [zmanim] still ahead, or null once all of today's are past.
    val nextIndex: Int?,
    // The next time and the one after it, reaching into tomorrow once today's are over.
    val next: WidgetZman?,
    val following: WidgetZman?,
    // The next few times from [next] on, for a widget with room for more than two.
    val ahead: List<WidgetZman>,
    val nextLabel: String,
    val followingLabel: String,
    val shabbat: WidgetShabbat?,
    // The civil date all of the above is for; a tap on the widget opens the app on it.
    val date: LocalDate,
    // When anything above next changes: a zman passing, the Hebrew date rolling, a holy day
    // coming in or going out.
    val refreshAt: Instant,
)

data class WidgetZman(
    val title: String,
    val time: String,
    val instant: Instant,
    val isPast: Boolean,
)

/** The coming (or current) Shabbat or Yom Tov: what it is, and when it comes in and goes out. */
data class WidgetShabbat(
    val title: String,
    val subtitle: String?,
    val entryLabel: String,
    // The bare time; [entryDay] names its day when that is not obvious — see [dayUnlessObvious].
    val entryTime: String?,
    val entryDay: String?,
    val exitLabel: String,
    val exitTime: String?,
    val exitDay: String?,
    // When it comes in, and whether it has: the glass widget shows the sky of that moment until then.
    val entry: Instant?,
    val isUnderWay: Boolean,
    // Whether the header has announced it (from the Jewish day before it comes in) or it is under
    // way, as opposed to the week's coming Shabbat taken from the Shabbat section.
    val announced: Boolean = false,
    // The civil day it is — the one after its candle lighting, a stretch's first day — so a tap on
    // the widget opens the app on that day's times.
    val day: LocalDate? = null,
) {
    // What a widget heads each time with: its label, and its day when that is not obvious.
    val entryHeading: String get() = listOfNotNull(entryLabel, entryDay).joinToString(" · ")
    val exitHeading: String get() = listOfNotNull(exitLabel, exitDay).joinToString(" · ")
}

/**
 * @param tomorrow the next civil day's zmanim.
 * @param text words everything in the app's language (see `TextResolver.of`).
 * @param observanceBoundary the next instant the header's state changes (see
 * `nextZmanimRefreshBoundary`); the refresh is whichever of that and the next zman comes first.
 */
fun widgetContent(
    day: ZmanimDayFacts,
    tomorrow: () -> ZmanimDayFacts,
    text: TextResolver,
    use24HourTime: Boolean,
    enabledZmanim: Set<ZmanimTimeOption>,
    now: Instant,
    observanceBoundary: Instant,
): WidgetContent {
    val zoneId = day.location.zoneId
    fun timeOf(instant: Instant) = text.resolve(UiText.Time(instant, zoneId, use24HourTime))
    // Once today's times are over the next ones are tomorrow's, and say so.
    fun format(instant: Instant): String =
        text.resolve(UiText.Time(instant, zoneId, use24HourTime, withWeekday = instant.atZone(zoneId).toLocalDate() != day.date))

    val zmanim = todaysTimes(day, text, enabledZmanim)
        .map { it.toWidgetZman(timeOf(it.instant), isPast = !it.instant.isAfter(now)) }
    val nextIndex = zmanim.indexOfFirst { !it.isPast }.takeIf { it >= 0 }

    // Past the last of today's times the next ones are tomorrow's, whose times then carry the day.
    val upcoming = zmanim.filterNot { it.isPast }
    val tomorrowsDay by lazy(tomorrow)
    val tomorrowsZmanim by lazy {
        todaysTimes(tomorrowsDay, text, enabledZmanim).map { it.toWidgetZman(format(it.instant), isPast = false) }
    }
    val ahead = upcoming.take(MaxAhead).let { today ->
        if (today.size < MaxAhead) today + tomorrowsZmanim.take(MaxAhead - today.size) else today
    }
    val weekdayText = UiText.Date(day.date, DateStyle.Weekday)
    val civilDateText = UiText.Date(day.date, DateStyle.DayMonth)
    val weekday = text.resolve(weekdayText)
    val civilDate = text.resolve(civilDateText)

    return WidgetContent(
        rtl = text.isRtl,
        hebrewDate = text.resolve(CalendarText.HebrewDate(day.displayedDate).asUiText()),
        gregorianDate = text.string(R.string.header_date, weekdayText, civilDateText),
        weekday = weekday,
        civilDate = civilDate,
        specialDay = specialDay(day, text),
        zmanim = zmanim,
        nextIndex = nextIndex,
        next = ahead.getOrNull(0),
        following = ahead.getOrNull(1),
        ahead = ahead,
        nextLabel = text.string(R.string.widget_next_label),
        followingLabel = text.string(R.string.widget_following_label),
        shabbat = shabbat(day, text, ::timeOf) { instant, expected -> dayUnlessObvious(instant, expected, day, text) },
        date = day.date,
        refreshAt = listOfNotNull(observanceBoundary, ahead.firstOrNull()?.instant?.plusSeconds(1)).min(),
    )
}

// The most times a widget lists ahead: the next, and up to four more under it.
private const val MaxAhead = 5

/** One time of the day before it is formatted: its name and when it is. */
private class DayTime(val title: String, val instant: Instant) {
    fun toWidgetZman(time: String, isPast: Boolean) = WidgetZman(title, time, instant, isPast)
}

/** Today's times, in order: the user's zmanim, and a holy day's or fast's own ends when today. */
private fun todaysTimes(
    day: ZmanimDayFacts,
    text: TextResolver,
    enabledZmanim: Set<ZmanimTimeOption>,
): List<DayTime> {
    val observances = observanceTimes(day, text).filter { it.instant.atZone(day.location.zoneId).toLocalDate() == day.date }
    return (zmanimTimes(day, text, enabledZmanim) + observances).sortedBy { it.instant }
}

/**
 * The user's zmanim for [day], in the zmanim list's order. A widget's rows are narrow, so each takes
 * its short name where it has one: "סוף זמן ק״ש".
 */
private fun zmanimTimes(day: ZmanimDayFacts, text: TextResolver, enabledZmanim: Set<ZmanimTimeOption>): List<DayTime> =
    day.zmanim
        .filter { it.option in enabledZmanim }
        .mapNotNull { zman -> zman.time?.let { DayTime(text.string(zman.option.shortLabel), it) } }

/** The entry and exit of the holy day and the fast [day] announces, whichever day they fall on. */
private fun observanceTimes(day: ZmanimDayFacts, text: TextResolver): List<DayTime> {
    val holyDay = day.holyDayInfo
    val fast = day.fastDayInfo
    return listOfNotNull(
        holyDay?.startTime?.let { DayTime(text.string(holyDay.entryTerm.starts), it) },
        holyDay?.endTime?.let { DayTime(text.string(holyDay.exitTerm.ends), it) },
        fast?.startTime?.let { DayTime(text.string(R.string.fast_starts), it) },
        fast?.endTime?.let { DayTime(text.string(R.string.fast_ends), it) },
    )
}

private fun specialDay(day: ZmanimDayFacts, text: TextResolver): String? {
    val named = listOfNotNull(
        day.holyDayInfo?.takeIf { it.isUnderWay }?.nameText,
        day.fastDayInfo?.takeIf { it.isUnderWay }?.nameText,
        day.dayName?.asUiText(),
    ).map(text::resolve)
    // Rosh Chodesh and the Omer are rows of their own on the main screen, not header names.
    val events = day.events.mapNotNull { event ->
        when (event) {
            is DayEvent.RoshChodesh -> event.name
            is DayEvent.Omer -> event.count
            is DayEvent.Chanukah, is DayEvent.Chametz -> null
        }?.let { text.resolve(it.asUiText()) }
    }
    val parts = (named + events).filter { it.isNotBlank() }.distinct()
    // KosherJava names Rosh Chodesh twice — as the day ("ראש חודש") and with its month — so keep
    // only the fuller one.
    return parts
        .filterNot { part -> parts.any { it != part && it.contains(part) } }
        .joinToString(" · ")
        .ifBlank { null }
}

/**
 * The weekday of a Shabbat or chag time, unless the label already says it: today's time needs no
 * day, and nor does candle lighting on a Friday or Shabbat going out on a Saturday. A chag's
 * times on other days are named ("יום ג׳"). Kept off the time itself, which stays short enough to
 * fit a small widget.
 */
private fun dayUnlessObvious(instant: Instant, expected: DayOfWeek, day: ZmanimDayFacts, text: TextResolver): String? {
    val date = instant.atZone(day.location.zoneId).toLocalDate()
    if (date == day.date || date.dayOfWeek == expected) return null
    return text.resolve(UiText.Date(date, DateStyle.ShortWeekday))
}

private fun shabbat(
    day: ZmanimDayFacts,
    text: TextResolver,
    format: (Instant) -> String,
    dayOf: (Instant, DayOfWeek) -> String?,
): WidgetShabbat? {
    // From a day before a holy day enters until it goes out, the header's own card is the answer,
    // and it covers a Yom Tov as well as Shabbat.
    day.holyDayInfo?.let { holy ->
        val title = if (holy.isUnderWay) holy.nameText else holy.parshaText ?: holy.nameText
        return WidgetShabbat(
            title = text.resolve(title),
            subtitle = holy.sequelText?.let(text::resolve),
            entryLabel = text.string(holy.entryTerm.starts),
            entryTime = holy.startTime?.let(format),
            entryDay = holy.startTime?.let { dayOf(it, DayOfWeek.FRIDAY) },
            exitLabel = text.string(holy.exitTerm.ends),
            exitTime = holy.endTime?.let(format),
            exitDay = holy.endTime?.let { dayOf(it, DayOfWeek.SATURDAY) },
            entry = holy.startTime,
            isUnderWay = holy.isUnderWay,
            announced = true,
            day = dayOf(holy.startTime, holy.endTime, day),
        )
    }
    // Otherwise the coming Shabbat, from the main screen's Shabbat section.
    val section = day.shabbat ?: return null
    val candles = section.candleLighting?.time
    val motzei = section.exit
    val parsha = day.weeklyParsha
    // A Shabbat that is a Yom Tov has no parsha; the section then names the day instead.
    val dayName = section.dayName
    return WidgetShabbat(
        title = when {
            parsha != null -> text.string(R.string.parsha_named, parsha.asUiText())
            dayName != null -> text.resolve(dayName.asUiText())
            else -> text.string(R.string.holy_day_shabbat)
        },
        subtitle = null,
        entryLabel = text.string(R.string.widget_candle_lighting),
        entryTime = candles?.let(format),
        entryDay = null,
        exitLabel = text.string(R.string.widget_shabbat_ends),
        exitTime = motzei?.let(format),
        exitDay = null,
        entry = candles,
        isUnderWay = false,
        day = dayOf(candles, motzei, day),
    )
}

/**
 * The civil day a Shabbat or chag coming in at [entry] is: the day after its candle lighting, or
 * the day it goes out when there is no candle lighting to go by.
 */
private fun dayOf(entry: Instant?, exit: Instant?, day: ZmanimDayFacts): LocalDate? =
    entry?.atZone(day.location.zoneId)?.toLocalDate()?.plusDays(1) ?: exit?.atZone(day.location.zoneId)?.toLocalDate()
