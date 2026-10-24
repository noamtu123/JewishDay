// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import com.noamtu.jewishday.model.ShabbatGroupTitle
import com.noamtu.jewishday.model.ZmanItem
import com.noamtu.jewishday.model.ZmanimDay
import com.noamtu.jewishday.model.ZmanimGroupTitle
import com.noamtu.jewishday.model.ZmanimTimeOption
import java.time.DayOfWeek
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Everything the home-screen widgets show, already worded in the app's language. Built from the
 * same [ZmanimDay] as the main screen, so a widget can never disagree with the app.
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
) {
    // What a widget heads each time with: its label, and its day when that is not obvious.
    val entryHeading: String get() = listOfNotNull(entryLabel, entryDay).joinToString(" · ")
    val exitHeading: String get() = listOfNotNull(exitLabel, exitDay).joinToString(" · ")
}

/**
 * @param tomorrow the next civil day's zmanim.
 * @param observanceBoundary the next instant the header's state changes (see
 * `nextZmanimRefreshBoundary`); the refresh is whichever of that and the next zman comes first.
 */
fun widgetContent(
    day: ZmanimDay,
    tomorrow: () -> ZmanimDay,
    hebrew: Boolean,
    use24HourTime: Boolean,
    enabledZmanim: Set<ZmanimTimeOption>,
    now: Instant,
    observanceBoundary: Instant,
): WidgetContent {
    val locale = if (hebrew) Locale.forLanguageTag("he") else Locale.ENGLISH
    val timePattern = if (use24HourTime) "HH:mm" else "h:mm a"
    val timeFormatter = DateTimeFormatter.ofPattern(timePattern, locale).withZone(day.zoneId)
    // Once today's times are over the next ones are tomorrow's, and say so.
    val dayAndTimeFormatter = DateTimeFormatter.ofPattern("EEE $timePattern", locale).withZone(day.zoneId)
    fun format(instant: Instant): String =
        if (instant.atZone(day.zoneId).toLocalDate() == day.date) {
            timeFormatter.format(instant)
        } else {
            dayAndTimeFormatter.format(instant)
        }

    val zmanim = todaysTimes(day, hebrew, enabledZmanim)
        .map { it.toWidgetZman(timeFormatter.format(it.instant), isPast = !it.instant.isAfter(now)) }
    val nextIndex = zmanim.indexOfFirst { !it.isPast }.takeIf { it >= 0 }

    // Past the last of today's times the next ones are tomorrow's, whose times then carry the day.
    val upcoming = zmanim.filterNot { it.isPast }
    val tomorrowsDay by lazy(tomorrow)
    val tomorrowsZmanim by lazy {
        todaysTimes(tomorrowsDay, hebrew, enabledZmanim).map { it.toWidgetZman(format(it.instant), isPast = false) }
    }
    val ahead = upcoming.take(MaxAhead).let { today ->
        if (today.size < MaxAhead) today + tomorrowsZmanim.take(MaxAhead - today.size) else today
    }

    return WidgetContent(
        rtl = hebrew,
        hebrewDate = if (hebrew) day.hebrewDateHebrew else day.hebrewDateEnglish,
        gregorianDate = gregorianDate(day, hebrew),
        weekday = day.date.format(DateTimeFormatter.ofPattern("EEEE", locale)),
        civilDate = if (hebrew) {
            day.date.format(DateTimeFormatter.ofPattern("d 'ב'MMMM", locale))
        } else {
            day.date.format(DateTimeFormatter.ofPattern("MMMM d", locale))
        },
        specialDay = specialDay(day, hebrew),
        zmanim = zmanim,
        nextIndex = nextIndex,
        next = ahead.getOrNull(0),
        following = ahead.getOrNull(1),
        ahead = ahead,
        nextLabel = if (hebrew) "הזמן הבא" else "Next zman",
        followingLabel = if (hebrew) "אחריו" else "Then",
        shabbat = shabbat(day, hebrew, { timeFormatter.format(it) }) { instant, expected ->
            dayUnlessObvious(instant, expected, day, locale)
        },
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
    day: ZmanimDay,
    hebrew: Boolean,
    enabledZmanim: Set<ZmanimTimeOption>,
): List<DayTime> {
    val observances = observanceTimes(day, hebrew).filter { it.instant.atZone(day.zoneId).toLocalDate() == day.date }
    return (zmanimTimes(day, hebrew, enabledZmanim) + observances).sortedBy { it.instant }
}

/** The user's zmanim for [day], in the zmanim section's order. */
private fun zmanimTimes(day: ZmanimDay, hebrew: Boolean, enabledZmanim: Set<ZmanimTimeOption>): List<DayTime> {
    val enabledIds = enabledZmanim.mapTo(mutableSetOf()) { it.storageValue }
    return day.groups
        .firstOrNull { it.title == ZmanimGroupTitle }
        ?.items.orEmpty()
        .filter { it.id == null || it.id in enabledIds }
        .mapNotNull { item -> item.time?.let { DayTime(item.title(hebrew), it) } }
}

/** The entry and exit of the holy day and the fast [day] announces, whichever day they fall on. */
private fun observanceTimes(day: ZmanimDay, hebrew: Boolean): List<DayTime> {
    val holyDay = day.holyDayInfo
    val fast = day.fastDayInfo
    return listOfNotNull(
        holyDay?.startTime?.let { DayTime(if (hebrew) "כניסת ${holyDay.entryTermHebrew}" else "${holyDay.entryTerm} starts", it) },
        holyDay?.endTime?.let { DayTime(if (hebrew) "צאת ${holyDay.exitTermHebrew}" else "${holyDay.exitTerm} ends", it) },
        fast?.startTime?.let { DayTime(if (hebrew) "כניסת הצום" else "Fast starts", it) },
        fast?.endTime?.let { DayTime(if (hebrew) "צאת הצום" else "Fast ends", it) },
    )
}

// A widget's rows are narrow, so the Shema rows take the short form everyone reads: "סוף זמן ק״ש".
private fun ZmanItem.title(hebrew: Boolean): String =
    if (hebrew) titleHebrew.replace("קריאת שמע", "ק״ש") else title

private fun gregorianDate(day: ZmanimDay, hebrew: Boolean): String =
    if (hebrew) {
        // "ב" before the month is a literal in Hebrew's own date pattern, not part of the month name.
        day.date.format(DateTimeFormatter.ofPattern("EEEE, d 'ב'MMMM", Locale.forLanguageTag("he")))
    } else {
        day.date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH))
    }

private fun specialDay(day: ZmanimDay, hebrew: Boolean): String? {
    val named = listOfNotNull(
        day.holyDayInfo?.takeIf { it.isUnderWay }?.let { if (hebrew) it.nameHebrew else it.name },
        day.fastDayInfo?.takeIf { it.isUnderWay }?.let { if (hebrew) it.nameHebrew else it.name },
        if (hebrew) day.dayNameHebrew else day.dayName,
    )
    // Rosh Chodesh and the Omer are rows of their own on the main screen, not header names.
    val events = day.groups
        .firstOrNull { it.title.isEmpty() }
        ?.items.orEmpty()
        .filter { it.time == null && (it.title == "Rosh Chodesh" || it.title == "Omer") }
        .mapNotNull { if (hebrew) it.valueHebrew else it.value }
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
private fun dayUnlessObvious(instant: Instant, expected: DayOfWeek, day: ZmanimDay, locale: Locale): String? {
    val date = instant.atZone(day.zoneId).toLocalDate()
    if (date == day.date || date.dayOfWeek == expected) return null
    return date.format(DateTimeFormatter.ofPattern("EEE", locale))
}

private fun shabbat(
    day: ZmanimDay,
    hebrew: Boolean,
    format: (Instant) -> String,
    dayOf: (Instant, DayOfWeek) -> String?,
): WidgetShabbat? {
    // From a day before a holy day enters until it goes out, the header's own card is the answer,
    // and it covers a Yom Tov as well as Shabbat.
    day.holyDayInfo?.let { holy ->
        val title = if (holy.isUnderWay) {
            if (hebrew) holy.nameHebrew else holy.name
        } else {
            (if (hebrew) holy.parshaHebrew else holy.parsha) ?: if (hebrew) holy.nameHebrew else holy.name
        }
        return WidgetShabbat(
            title = title,
            subtitle = if (hebrew) holy.sequelHebrew else holy.sequel,
            entryLabel = if (hebrew) "כניסת ${holy.entryTermHebrew}" else "${holy.entryTerm} starts",
            entryTime = holy.startTime?.let(format),
            entryDay = holy.startTime?.let { dayOf(it, DayOfWeek.FRIDAY) },
            exitLabel = if (hebrew) "צאת ${holy.exitTermHebrew}" else "${holy.exitTerm} ends",
            exitTime = holy.endTime?.let(format),
            exitDay = holy.endTime?.let { dayOf(it, DayOfWeek.SATURDAY) },
            entry = holy.startTime,
            isUnderWay = holy.isUnderWay,
            announced = true,
        )
    }
    // Otherwise the coming Shabbat, from the main screen's Shabbat section.
    val items = day.groups.firstOrNull { it.title == ShabbatGroupTitle }?.items ?: return null
    val reading = items.firstOrNull { it.id == null }
    val candles = items.firstOrNull { it.id == ZmanimTimeOption.ShabbatCandleLighting.storageValue }
    val motzei = items.firstOrNull { it.id == ZmanimTimeOption.MotzeiShabbat.storageValue }
    val readingName = reading?.let { if (hebrew) it.valueHebrew else it.value }
    return WidgetShabbat(
        title = when {
            readingName == null -> if (hebrew) "שבת" else "Shabbat"
            // A Shabbat that is a Yom Tov has no parsha; the row then names the day instead.
            reading?.title != "Weekly Parsha" -> readingName
            hebrew -> "פרשת $readingName"
            else -> "Parashat $readingName"
        },
        subtitle = null,
        entryLabel = if (hebrew) "הדלקת נרות" else "Candle lighting",
        entryTime = candles?.time?.let(format),
        entryDay = null,
        exitLabel = if (hebrew) "צאת שבת" else "Shabbat ends",
        exitTime = motzei?.time?.let(format),
        exitDay = null,
        entry = candles?.time,
        isUnderWay = false,
    )
}
