// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.zmanim

import com.noamtu.jewishday.model.ZmanimDay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The pair of clock-time formatters a day's times are rendered with: one per app language, both
 * pinned to the day's zone so the same [Instant] reads correctly in either.
 */
internal data class ZmanimTimeFormatters(
    val english: DateTimeFormatter,
    val hebrew: DateTimeFormatter,
)

/**
 * Builds the English/Hebrew time formatters for a day in [zoneId]. Shared by the zmanim screen and
 * the home-screen widget so a time reads the same wherever it is shown.
 */
internal fun zmanimTimeFormatters(use24HourTime: Boolean, zoneId: ZoneId): ZmanimTimeFormatters {
    // Always format the "English" date/time in English regardless of the device locale — otherwise
    // a Hebrew system locale makes Locale.getDefault() render the English header in Hebrew too.
    val englishLocale = Locale.ENGLISH
    val hebrewLocale = Locale.forLanguageTag("he")
    val timePattern = if (use24HourTime) "HH:mm" else "h:mm a"
    return ZmanimTimeFormatters(
        english = DateTimeFormatter.ofPattern(timePattern, englishLocale).withZone(zoneId),
        hebrew = DateTimeFormatter.ofPattern(timePattern, hebrewLocale).withZone(zoneId),
    )
}

/**
 * Formats the day's header — dates, observance names and their entry/exit lines — exactly as the
 * zmanim screen shows it, so the widget can reuse the same wording rather than re-deriving it.
 */
internal fun ZmanimDay.toHeaderUi(use24HourTime: Boolean): ZmanimHeaderUi {
    val englishLocale = Locale.ENGLISH
    val hebrewLocale = Locale.forLanguageTag("he")
    val (englishTimeFormatter, hebrewTimeFormatter) = zmanimTimeFormatters(use24HourTime, zoneId)
    // The weekday and the day-of-month come from different days, so they are formatted separately:
    // the weekday belongs to the Jewish day and rolls at tzeit — Thursday evening is already
    // "Friday", along with the Hebrew date above it — while the day-of-month is the calendar date
    // the times below belong to, which turns over at midnight.
    val englishWeekdayFormatter = DateTimeFormatter.ofPattern("EEEE", englishLocale)
    val englishDayMonthFormatter = DateTimeFormatter.ofPattern("MMMM d", englishLocale)
    // Hebrew writes the month with a "ב" prefix ("17 ביולי"). CLDR keeps that prefix as a literal
    // in the locale's own date pattern rather than in the month name, so a custom pattern has to
    // carry it explicitly — MMMM alone yields the bare "יולי".
    val hebrewWeekdayFormatter = DateTimeFormatter.ofPattern("EEEE", hebrewLocale)
    val hebrewDayMonthFormatter = DateTimeFormatter.ofPattern("d 'ב'MMMM", hebrewLocale)

    return ZmanimHeaderUi(
        jewishDate = hebrewDateEnglish,
        jewishDateHebrew = hebrewDateHebrew,
        gregorianDate = "${displayedDate.format(englishWeekdayFormatter)}, ${date.format(englishDayMonthFormatter)}",
        gregorianDateHebrew = "${displayedDate.format(hebrewWeekdayFormatter)}, ${date.format(hebrewDayMonthFormatter)}",
        weekday = displayedDate.format(englishWeekdayFormatter),
        weekdayHebrew = displayedDate.format(hebrewWeekdayFormatter),
        festival = festival,
        locationName = locationName,
        // The fast's name belongs to it only while it is on; the times show a day ahead.
        fastName = fastDayInfo?.takeIf { it.isUnderWay }?.name,
        fastNameHebrew = fastDayInfo?.takeIf { it.isUnderWay }?.nameHebrew,
        fastStart = fastDayInfo?.startTime?.let { observanceLine("Fast starts", it, englishTimeFormatter) },
        fastStartHebrew = fastDayInfo?.startTime?.let { observanceLine("כניסת הצום", it, hebrewTimeFormatter) },
        fastEnd = fastDayInfo?.endTime?.let { observanceLine("Fast ends", it, englishTimeFormatter) },
        fastEndHebrew = fastDayInfo?.endTime?.let { observanceLine("צאת הצום", it, hebrewTimeFormatter) },
        // The name belongs to the holy day only while it is in; the times show a day ahead.
        holyDayName = holyDayInfo?.takeIf { it.isUnderWay }?.name,
        holyDayNameHebrew = holyDayInfo?.takeIf { it.isUnderWay }?.nameHebrew,
        dayName = dayName,
        dayNameHebrew = dayNameHebrew,
        // While Shabbat is only announced — from the moment the entry/exit card appears — the
        // parasha gives the card a heading without claiming Shabbat has begun.
        parshaName = holyDayInfo?.takeUnless { it.isUnderWay }?.parsha,
        parshaNameHebrew = holyDayInfo?.takeUnless { it.isUnderWay }?.parshaHebrew,
        // The two ends of the span are named separately: a Yom Tov entering on Friday goes out
        // on Shabbat, so "כניסת החג" is paired with "צאת שבת".
        holyDayStart = holyDayInfo?.startTime?.let {
            observanceLine("${holyDayInfo.entryTerm} starts", it, englishTimeFormatter)
        },
        holyDayStartHebrew = holyDayInfo?.startTime?.let {
            observanceLine("כניסת ${holyDayInfo.entryTermHebrew}", it, hebrewTimeFormatter)
        },
        holyDayEnd = holyDayInfo?.endTime?.let {
            observanceLine("${holyDayInfo.exitTerm} ends", it, englishTimeFormatter)
        },
        holyDayEndHebrew = holyDayInfo?.endTime?.let {
            observanceLine("צאת ${holyDayInfo.exitTermHebrew}", it, hebrewTimeFormatter)
        },
        holyDaySequel = holyDayInfo?.sequel,
        holyDaySequelHebrew = holyDayInfo?.sequelHebrew,
        fastLeadsHeader = fastLeadsHeader,
    )
}

/** One line of an observance card: what the time is, then the time — "צאת חג שני 19:20". */
internal fun observanceLine(label: String, time: Instant, formatter: DateTimeFormatter): String =
    "$label ${formatter.format(time)}"

/** A clock time for a row, or the "--" placeholder shown when the zman does not occur that day. */
internal fun Instant?.formatTime(formatter: DateTimeFormatter): String = this?.let(formatter::format) ?: "--"
