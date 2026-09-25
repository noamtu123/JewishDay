// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import com.noamtu.jewishday.data.AppSettings
import com.noamtu.jewishday.data.LocationSource
import com.noamtu.jewishday.data.locationSourceForName
import com.noamtu.jewishday.feature.zmanim.ZmanimHeaderUi
import com.noamtu.jewishday.feature.zmanim.toHeaderUi
import com.noamtu.jewishday.feature.zmanim.zmanimTimeFormatters
import com.noamtu.jewishday.model.DailyLearningGroupTitle
import com.noamtu.jewishday.model.DailyLearningType
import com.noamtu.jewishday.model.ShabbatGroupTitle
import com.noamtu.jewishday.model.ZmanItem
import com.noamtu.jewishday.model.ZmanimDay
import com.noamtu.jewishday.model.ZmanimGroup
import com.noamtu.jewishday.model.ZmanimGroupTitle
import com.noamtu.jewishday.model.ZmanimTimeOption
import com.noamtu.jewishday.ui.theme.SkyFrame
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Turns a computed [day] into what the widget shows, in the language and clock format of [settings].
 *
 * The header texts come from the same formatting the zmanim screen uses ([toHeaderUi]), so the two
 * surfaces never word a date or an observance differently. The times are what is still to come at
 * [now]: the next of the zmanim the user has switched on, in order, with the day's observance
 * boundaries — candle lighting, the exit of Shabbat or a Yom Tov, a fast's start and end — always
 * among them, since those are the times being waited for; once today's are all past, [nextDay]'s
 * first ones follow, marked as tomorrow's. A time that has passed is of no use at a glance, so none
 * is shown. The boundaries are read from the day's [ZmanimDay.holyDayInfo] and
 * [ZmanimDay.fastDayInfo] rather than from the Shabbat section, which is dropped on Shabbat itself
 * and, on a weekday Yom Tov, only knows about the coming Friday.
 *
 * Pure Kotlin — no Android classes — so it runs under plain JVM tests, and never throws on a day
 * with groups missing: every lookup degrades to an absent row or a null line.
 */
fun buildDayWidgetState(
    day: ZmanimDay,
    settings: AppSettings,
    sky: SkyFrame,
    now: Instant,
    nextDay: ZmanimDay? = null,
): DayWidgetState {
    val useHebrew = settings.useHebrewInterface
    val header = day.toHeaderUi(settings.use24HourTime)
    val formatters = zmanimTimeFormatters(settings.use24HourTime, day.zoneId)
    val formatter = if (useHebrew) formatters.hebrew else formatters.english
    return DayWidgetState(
        useHebrew = useHebrew,
        sky = sky,
        hebrewDate = resolve(useHebrew, header.jewishDate, header.jewishDateHebrew),
        weekdayAndDate = resolve(useHebrew, header.gregorianDate, header.gregorianDateHebrew),
        chip = chipFor(header, day, useHebrew),
        observanceLines = observanceLinesFor(header, useHebrew),
        eventLine = eventLineFor(day, useHebrew, formatter),
        times = timesFor(day, nextDay, now, settings.enabledZmanimTimes, useHebrew, formatter),
        learning = learningFor(day, settings.enabledDailyLearning, useHebrew, formatter),
        locationName = locationCaptionFor(day.locationName, useHebrew),
    )
}

/**
 * The badge, resolved the way the zmanim screen resolves its chip: the observance actually current
 * — [ZmanimHeaderUi.fastLeadsHeader] arbitrates when a fast and a holy day are both showing — then
 * the day's own name, then the coming Shabbat. The header names the coming Shabbat only from
 * Thursday's tzeit, when it announces it; on the weekdays before that the Shabbat section's reading
 * row is where the parasha lives, so the badge takes it from there (see [comingShabbatLabel]).
 */
private fun chipFor(header: ZmanimHeaderUi, day: ZmanimDay, useHebrew: Boolean): String? {
    val holyDayName = resolve(useHebrew, header.holyDayName, header.holyDayNameHebrew)
    val fastName = resolve(useHebrew, header.fastName, header.fastNameHebrew)
    val underWayName = if (header.fastLeadsHeader) fastName ?: holyDayName else holyDayName ?: fastName
    return underWayName
        ?: resolve(useHebrew, header.dayName, header.dayNameHebrew)
        ?: resolve(useHebrew, header.parshaName, header.parshaNameHebrew)
        ?: day.comingShabbatLabel(useHebrew)
}

/**
 * Names the coming Shabbat on the weekdays before the header announces it (the header's parasha
 * only appears from Thursday's tzeit): the weekly parasha, worded as the header words it so the
 * badge does not change from "Chayei Sara" to "Parashat Chayei Sara" on Thursday night. A Shabbat
 * that is a Yom Tov has no parasha, and its bare name in the badge slot would read as "today", so
 * it is used only once the day is announced and the observance lines beneath qualify it.
 */
private fun ZmanimDay.comingShabbatLabel(useHebrew: Boolean): String? {
    val row = shabbatReadingRow() ?: return null
    val value = resolve(useHebrew, row.value, row.valueHebrew) ?: return null
    return when {
        row.title == WeeklyParshaTitle -> resolve(useHebrew, "Parashat ", "פרשת ") + value
        holyDayInfo != null -> value
        else -> null
    }
}

/** The Shabbat section's Torah-reading row: the one row there that is a text rather than a time. */
private fun ZmanimDay.shabbatReadingRow(): ZmanItem? =
    group(ShabbatGroupTitle)?.items?.firstOrNull { it.id == null && it.value != null }

/**
 * The entry and exit lines, the leading observance's first, so that what a crowded day loses to
 * [MaxObservanceLines] is the other one's: on the Saturday night Tisha B'Av begins, "Shabbat ends"
 * matters more than Friday's "Shabbat starts". Only that Saturday-evening overlap reorders anything;
 * every other day yields the screen's order.
 */
private fun observanceLinesFor(header: ZmanimHeaderUi, useHebrew: Boolean): List<String> {
    val fastLines = listOfNotNull(
        resolve(useHebrew, header.fastStart, header.fastStartHebrew),
        resolve(useHebrew, header.fastEnd, header.fastEndHebrew),
    )
    val holyDayLines = listOfNotNull(
        resolve(useHebrew, header.holyDayStart, header.holyDayStartHebrew),
        resolve(useHebrew, header.holyDayEnd, header.holyDayEndHebrew),
    )
    val sequel = listOfNotNull(resolve(useHebrew, header.holyDaySequel, header.holyDaySequelHebrew))
    val ordered = if (header.fastLeadsHeader) fastLines + holyDayLines else holyDayLines + fastLines
    return (ordered + sequel).take(MaxObservanceLines)
}

/**
 * The day's events on one line. They live in the one group without a heading (see zmanimForDate),
 * which is omitted altogether on an ordinary day.
 */
private fun eventLineFor(day: ZmanimDay, useHebrew: Boolean, formatter: DateTimeFormatter): String? {
    val events = day.group(EventsGroupTitle)?.items.orEmpty()
    if (events.isEmpty()) return null
    return events.take(MaxEvents).joinToString(EventSeparator) { it.asLine(useHebrew, formatter) }
}

private fun timesFor(
    day: ZmanimDay,
    nextDay: ZmanimDay?,
    now: Instant,
    shown: Set<ZmanimTimeOption>,
    useHebrew: Boolean,
    formatter: DateTimeFormatter,
): List<DayWidgetTime> {
    val shownIds = shown.mapTo(mutableSetOf()) { it.storageValue }
    val zmanim = listOfNotNull(day, nextDay).flatMap { source ->
        source.group(ZmanimGroupTitle)?.items.orEmpty()
            .filter { it.id in shownIds }
            .mapNotNull { row ->
                row.time?.let { Moment(row.widgetTitle(useHebrew), it, midnight = row.id == ChatzotHaLailaId) }
            }
    }
    // Observances first, so that where a boundary shares its instant with a zman (a fast ending at
    // tzeit) it is the boundary's name that survives the de-duplication.
    return (observanceMoments(day, useHebrew) + zmanim)
        .filter { it.time.isAfter(now) }
        .sortedBy { it.time }
        .distinctBy { it.time }
        .map { DayWidgetTime(it.labelOn(day, useHebrew), formatter.format(it.time), it.pinned) }
        .keepingPinned(MaxTimes)
}

/**
 * A moment the widget could show; [pinned] ones — observance boundaries — are never crowded out, and
 * a [midnight] one is the middle of a night, which belongs to the evening it follows.
 */
private class Moment(val label: String, val time: Instant, val pinned: Boolean = false, val midnight: Boolean = false)

/**
 * The label, saying "tomorrow" when the moment belongs to a later date than the day the widget is
 * showing. Chatzot halaila is dated by the evening before it, so tonight's reads as tonight's even
 * when the clock puts it past midnight, and tomorrow night's still says tomorrow.
 */
private fun Moment.labelOn(day: ZmanimDay, useHebrew: Boolean): String {
    val dated = if (midnight) time.minus(HalfADay) else time
    return if (dated.atZone(day.zoneId).toLocalDate() == day.date) {
        label
    } else {
        label + resolve(useHebrew, " (tomorrow)", " (מחר)")
    }
}

/**
 * The zman's name as the widget words it: the screen's title, shortened where that would not fit a
 * column of the times row — the four Shema and Tefillah deadlines differ only in their brackets, which
 * are exactly what a cut-off label loses.
 */
private fun ZmanItem.widgetTitle(useHebrew: Boolean): String =
    WidgetTitles[id]?.let { (english, hebrew) -> resolve(useHebrew, english, hebrew) }
        ?: resolve(useHebrew, title, titleHebrew)

private val WidgetTitles: Map<String, Pair<String, String>> = mapOf(
    ZmanimTimeOption.AlotHashachar.storageValue to ("Alot" to "עלות השחר"),
    ZmanimTimeOption.TallitTefillin.storageValue to ("Tallit" to "טלית ותפילין"),
    ZmanimTimeOption.SofZmanShemaMagenAvraham.storageValue to ("Shema MGA" to "ק״ש מג״א"),
    ZmanimTimeOption.SofZmanShemaGra.storageValue to ("Shema GRA" to "ק״ש גר״א"),
    ZmanimTimeOption.SofZmanTefillahMagenAvraham.storageValue to ("Tefillah MGA" to "תפילה מג״א"),
    ZmanimTimeOption.SofZmanTefillahGra.storageValue to ("Tefillah GRA" to "תפילה גר״א"),
)

private val ChatzotHaLailaId = ZmanimTimeOption.ChatzotHaLaila.storageValue
private val HalfADay: Duration = Duration.ofHours(12)

/**
 * The boundaries of the day's observances, as the header announces them: the entry while a holy day
 * is coming in, its exit while it is in, and both ends of a fast. Always kept, since on such a day
 * these are the times being waited for.
 */
private fun observanceMoments(day: ZmanimDay, useHebrew: Boolean): List<Moment> {
    val holyDay = day.holyDayInfo
    val fast = day.fastDayInfo
    return listOfNotNull(
        holyDay?.takeUnless { it.isUnderWay }?.startTime?.let {
            Moment(resolve(useHebrew, "Candle Lighting", "הדלקת נרות"), it, pinned = true)
        },
        holyDay?.takeIf { it.isUnderWay }?.endTime?.let {
            Moment(exitLabel(it, day.zoneId, useHebrew), it, pinned = true)
        },
        fast?.takeUnless { it.isUnderWay }?.startTime?.let {
            Moment(resolve(useHebrew, "Fast starts", "כניסת הצום"), it, pinned = true)
        },
        fast?.takeIf { it.isUnderWay }?.endTime?.let {
            Moment(resolve(useHebrew, "Fast ends", "צאת הצום"), it, pinned = true)
        },
    )
}

/** Names the exit by the day it falls on: a stretch going out on Saturday night is motzei Shabbat. */
private fun exitLabel(exit: Instant, zone: ZoneId, useHebrew: Boolean): String =
    if (exit.atZone(zone).dayOfWeek == DayOfWeek.SATURDAY) {
        resolve(useHebrew, "Motzei Shabbat", "צאת שבת")
    } else {
        resolve(useHebrew, "Motzei Yom Tov", "צאת החג")
    }

/**
 * One learning line, from the tracks the user has switched on. Daf Yomi Bavli is the one most
 * people follow, so it is preferred whenever it is among them; otherwise the first enabled row.
 */
private fun learningFor(
    day: ZmanimDay,
    enabled: Set<DailyLearningType>,
    useHebrew: Boolean,
    formatter: DateTimeFormatter,
): String? {
    val enabledIds = enabled.mapTo(mutableSetOf()) { it.storageValue }
    val rows = day.group(DailyLearningGroupTitle)?.items.orEmpty().filter { it.id in enabledIds }
    val row = rows.firstOrNull { it.id == DailyLearningType.DafYomiBavli.storageValue } ?: rows.firstOrNull()
    return row?.asLine(useHebrew, formatter)
}

/**
 * The location caption the zmanim screen shows: nothing for a live fix, a warning that the times
 * are Jerusalem's when there is no fix, and the place's name for a developer preset. The raw name
 * is never shown — for a fix it is an English sentinel, and a fallback has to say it is one. The
 * texts mirror zmanim_location_jerusalem and zmanim_location_named in strings.xml, which this has
 * no Context to read.
 */
private fun locationCaptionFor(name: String, useHebrew: Boolean): String? = when (locationSourceForName(name)) {
    LocationSource.CurrentFix -> null
    LocationSource.Jerusalem -> resolve(useHebrew, "Times based on Jerusalem", "הזמנים מבוססים על ירושלים")
    LocationSource.Named -> resolve(useHebrew, "Times based on $name", "הזמנים מבוססים על $name")
}

private fun ZmanimDay.group(title: String): ZmanimGroup? = groups.firstOrNull { it.title == title }

/** "Title: value" — the value being the row's text, or its time for the rows that carry one. */
private fun ZmanItem.asLine(useHebrew: Boolean, formatter: DateTimeFormatter): String {
    val title = resolve(useHebrew, title, titleHebrew)
    val value = resolve(useHebrew, value, valueHebrew) ?: time?.let(formatter::format)
    return if (value == null) title else "$title: $value"
}

private fun <T : String?> resolve(useHebrew: Boolean, english: T, hebrew: T): T = if (useHebrew) hebrew else english

// The events group is the one built without a heading; see zmanimForDate.
private const val EventsGroupTitle = ""
private const val EventSeparator = " · "
private const val MaxEvents = 2
private const val MaxObservanceLines = 3
private const val MaxTimes = 4

// The reading row carries no id; this is the title zmanimForDate gives the weekly-parasha row.
private const val WeeklyParshaTitle = "Weekly Parsha"
