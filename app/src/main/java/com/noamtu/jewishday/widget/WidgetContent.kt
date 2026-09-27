// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import com.noamtu.jewishday.data.LocationSource
import com.noamtu.jewishday.data.locationSourceForName
import com.noamtu.jewishday.model.DailyLearningGroupTitle
import com.noamtu.jewishday.model.DailyLearningType
import com.noamtu.jewishday.model.Festival
import com.noamtu.jewishday.model.ShabbatGroupTitle
import com.noamtu.jewishday.model.ZmanItem
import com.noamtu.jewishday.model.ZmanimDay
import com.noamtu.jewishday.model.ZmanimGroupTitle
import com.noamtu.jewishday.model.ZmanimTimeOption
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
    val nextLabel: String,
    val followingLabel: String,
    val shabbat: WidgetShabbat?,
    // When anything above next changes: a zman passing, the Hebrew date rolling, a holy day
    // coming in or going out.
    val refreshAt: Instant,
    // The festival the Hebrew date belongs to, erev included, for its picture; null otherwise.
    val festival: Festival? = null,
    // The next few times still ahead, reaching into tomorrow: today's zmanim, tomorrow's once
    // today's run out, and the entry and exit of whatever Shabbat, chag or fast is announced, which
    // are kept however few fit (see [WidgetZman.observance]).
    val upcoming: List<WidgetZman> = emptyList(),
    // One daily-learning line — "Daf Yomi Bavli: Sanhedrin 78" — or null when none is enabled.
    val learning: String? = null,
    // "Jerusalem times · tap to update" when the times are not for where the phone is, the place's
    // name for a developer preset; null for a real fix.
    val locationNote: String? = null,
    // No location at all: the times are Jerusalem's, and tapping the note can take a fix.
    val locationMissing: Boolean = false,
)

data class WidgetZman(
    val title: String,
    val time: String,
    val instant: Instant,
    val isPast: Boolean,
    // The name where a narrow column has no room for the whole one: "ק״ש מג״א".
    val shortTitle: String = title,
    // A Shabbat's, chag's or fast's own entry or exit: the time being waited for, so a short list
    // keeps it over the zmanim around it.
    val observance: Boolean = false,
)

/** The coming (or current) Shabbat or Yom Tov: what it is, and when it comes in and goes out. */
data class WidgetShabbat(
    val title: String,
    val subtitle: String?,
    val entryLabel: String,
    val entryTime: String?,
    val exitLabel: String,
    val exitTime: String?,
    // When it comes in, and whether it has: the glass widget shows the sky of that moment until then.
    val entry: Instant?,
    val isUnderWay: Boolean,
    // Whether the header has announced it (from the Jewish day before it comes in) or it is under
    // way, as opposed to the week's coming Shabbat taken from the Shabbat section.
    val announced: Boolean = false,
)

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
    enabledLearning: Set<DailyLearningType> = emptySet(),
): WidgetContent {
    val locale = if (hebrew) Locale.forLanguageTag("he") else Locale.ENGLISH
    val timePattern = if (use24HourTime) "HH:mm" else "h:mm a"
    val timeFormatter = DateTimeFormatter.ofPattern(timePattern, locale).withZone(day.zoneId)
    // A time on another day says which: a Yom Tov is announced the evening before it comes in.
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
    val next = upcoming.getOrNull(0) ?: tomorrowsZmanim.getOrNull(0)
    val following = upcoming.getOrNull(1) ?: tomorrowsZmanim.getOrNull(if (upcoming.isEmpty()) 1 else 0)

    return WidgetContent(
        rtl = hebrew,
        hebrewDate = if (hebrew) day.hebrewDateHebrew else day.hebrewDateEnglish,
        gregorianDate = gregorianDate(day, hebrew),
        specialDay = specialDay(day, hebrew),
        zmanim = zmanim,
        nextIndex = nextIndex,
        next = next,
        following = following,
        nextLabel = if (hebrew) "הזמן הבא" else "Next zman",
        followingLabel = if (hebrew) "אחריו" else "Then",
        shabbat = shabbat(day, hebrew, ::format),
        refreshAt = listOfNotNull(observanceBoundary, next?.instant?.plusSeconds(1)).min(),
        festival = day.festival,
        upcoming = upcomingTimes(day, tomorrowsDay, hebrew, enabledZmanim, now, timeFormatter, ::format),
        learning = learning(day, enabledLearning, hebrew, timeFormatter),
        locationNote = locationNote(day.locationName, hebrew),
        locationMissing = locationSourceForName(day.locationName) == LocationSource.Jerusalem,
    )
}

/**
 * The next [MaxUpcoming] times: the rest of today's zmanim, then tomorrow's, with the entry and exit
 * of what the day announces kept among them however far off they are. Only the announced ones: on
 * a Thursday morning, Friday's candle lighting is not yet something the day speaks of. Today's rows
 * keep the plain clock time, as on the zmanim screen — tonight's chatzot halaila is tonight's even
 * past midnight — while a time on another day says which.
 */
private fun upcomingTimes(
    day: ZmanimDay,
    tomorrow: ZmanimDay,
    hebrew: Boolean,
    enabledZmanim: Set<ZmanimTimeOption>,
    now: Instant,
    timeFormatter: DateTimeFormatter,
    format: (Instant) -> String,
): List<WidgetZman> {
    // Observances first, so where a boundary shares its instant with a zman (a fast ending at tzeit)
    // it is the boundary's name that survives.
    val times = observanceTimes(day, hebrew).map { it.toWidgetZman(format(it.instant), isPast = false) } +
        zmanimTimes(day, hebrew, enabledZmanim).map { it.toWidgetZman(timeFormatter.format(it.instant), isPast = false) } +
        zmanimTimes(tomorrow, hebrew, enabledZmanim).map { it.toWidgetZman(format(it.instant), isPast = false) }
    return times
        .filter { it.instant.isAfter(now) }
        .sortedBy { it.instant }
        .distinctBy { it.instant }
        .keepingPinned(MaxUpcoming) { it.observance }
}

/**
 * The first [limit] items, in order, keeping every pinned one: what a shorter list drops is the
 * times filling in around a pinned one, never the pinned one itself.
 */
internal fun <T> List<T>.keepingPinned(limit: Int, pinned: (T) -> Boolean): List<T> {
    var fill = (limit - count(pinned)).coerceAtLeast(0)
    return filter { pinned(it) || fill-- > 0 }.take(limit)
}

/**
 * One learning line from the tracks the user has switched on. Daf Yomi Bavli is the one most people
 * follow, so it is preferred whenever it is among them; otherwise the first enabled row.
 */
private fun learning(
    day: ZmanimDay,
    enabled: Set<DailyLearningType>,
    hebrew: Boolean,
    timeFormatter: DateTimeFormatter,
): String? {
    val enabledIds = enabled.mapTo(mutableSetOf()) { it.storageValue }
    val rows = day.groups.firstOrNull { it.title == DailyLearningGroupTitle }?.items.orEmpty().filter { it.id in enabledIds }
    val row = rows.firstOrNull { it.id == DailyLearningType.DafYomiBavli.storageValue } ?: rows.firstOrNull() ?: return null
    val value = (if (hebrew) row.valueHebrew else row.value) ?: row.time?.let(timeFormatter::format)
    return if (value == null) row.title(hebrew) else "${row.title(hebrew)}: $value"
}

/**
 * The zmanim screen's location caption: nothing for a real fix, a warning that the times are
 * Jerusalem's when there is none, the place's name for a developer preset. Mirrors
 * zmanim_location_jerusalem and zmanim_location_named in strings.xml, which this has no Context to read.
 * Tapping the warning takes a fix without opening the app (see [WidgetLocationService]).
 */
private fun locationNote(name: String, hebrew: Boolean): String? = when (locationSourceForName(name)) {
    LocationSource.CurrentFix -> null
    LocationSource.Jerusalem -> if (hebrew) "זמני ירושלים · הקישו לעדכון" else "Jerusalem times · tap to update"
    LocationSource.Named -> if (hebrew) "הזמנים מבוססים על $name" else "Times based on $name"
}

/** One time of the day before it is formatted: its names, when it is, and whether it is an entry or exit. */
private class DayTime(val title: String, val shortTitle: String, val instant: Instant, val observance: Boolean = false) {
    fun toWidgetZman(time: String, isPast: Boolean) = WidgetZman(title, time, instant, isPast, shortTitle, observance)
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
        .mapNotNull { item -> item.time?.let { DayTime(item.title(hebrew), item.shortTitle(hebrew), it) } }
}

/** The entry and exit of the holy day and the fast [day] announces, whichever day they fall on. */
private fun observanceTimes(day: ZmanimDay, hebrew: Boolean): List<DayTime> {
    val holyDay = day.holyDayInfo
    val fast = day.fastDayInfo
    fun observance(title: String, instant: Instant) = DayTime(title, title, instant, observance = true)
    return listOfNotNull(
        holyDay?.startTime?.let { observance(if (hebrew) "כניסת ${holyDay.entryTermHebrew}" else "${holyDay.entryTerm} starts", it) },
        holyDay?.endTime?.let { observance(if (hebrew) "צאת ${holyDay.exitTermHebrew}" else "${holyDay.exitTerm} ends", it) },
        fast?.startTime?.let { observance(if (hebrew) "כניסת הצום" else "Fast starts", it) },
        fast?.endTime?.let { observance(if (hebrew) "צאת הצום" else "Fast ends", it) },
    )
}

private fun ZmanItem.title(hebrew: Boolean): String = if (hebrew) titleHebrew else title

/**
 * The name for a narrow column: the four Shema and Tefillah deadlines differ only in their brackets,
 * which are exactly what a cut-off label loses, so those few are shortened; the rest keep their own.
 */
private fun ZmanItem.shortTitle(hebrew: Boolean): String =
    ShortTitles[id]?.let { (english, hebrewTitle) -> if (hebrew) hebrewTitle else english } ?: title(hebrew)

private val ShortTitles: Map<String, Pair<String, String>> = mapOf(
    ZmanimTimeOption.AlotHashachar.storageValue to ("Alot" to "עלות השחר"),
    ZmanimTimeOption.TallitTefillin.storageValue to ("Tallit" to "טלית ותפילין"),
    ZmanimTimeOption.SofZmanShemaMagenAvraham.storageValue to ("Shema MGA" to "ק״ש מג״א"),
    ZmanimTimeOption.SofZmanShemaGra.storageValue to ("Shema GRA" to "ק״ש גר״א"),
    ZmanimTimeOption.SofZmanTefillahMagenAvraham.storageValue to ("Tefillah MGA" to "תפילה מג״א"),
    ZmanimTimeOption.SofZmanTefillahGra.storageValue to ("Tefillah GRA" to "תפילה גר״א"),
)

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

private fun shabbat(day: ZmanimDay, hebrew: Boolean, format: (Instant) -> String): WidgetShabbat? {
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
            exitLabel = if (hebrew) "צאת ${holy.exitTermHebrew}" else "${holy.exitTerm} ends",
            exitTime = holy.endTime?.let(format),
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
        exitLabel = if (hebrew) "צאת שבת" else "Shabbat ends",
        exitTime = motzei?.time?.let(format),
        entry = candles?.time,
        isUnderWay = false,
    )
}

private const val MaxUpcoming = 4
