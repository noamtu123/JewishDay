// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

import androidx.compose.runtime.Immutable
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.noamtu.jewishday.core.i18n.CalendarText
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/**
 * A melacha-forbidden stretch — Shabbat, a Yom Tov, or several of them running back to back — as
 * one span: it enters when its first day enters and goes out when its last day does, so nothing has
 * to be pieced together from a card that changes at every boundary in between. [sequel] says what
 * the stretch is made of, and [day] still follows the day currently in.
 */
@Immutable
public data class HolyDayInfo(
    val day: HolyDayName,
    // What the header chip says while the day is only announced — "פרשת נצבים" — so the entry/exit
    // card is labelled without the chip claiming Shabbat is in. Null for a Yom Tov (no parsha).
    val parsha: CalendarText.Parsha? = null,
    // The whole stretch: its first day's entry and its last day's exit.
    val startTime: Instant?,
    val endTime: Instant?,
    // What the two times are named, each by the day it falls on, so the pair says how far the stretch
    // reaches: "כניסת חג ראשון" … "צאת חג שני" for a two-day Yom Tov, and "כניסת החג" … "צאת שבת" for
    // one running into Shabbat.
    val entryTerm: HolyDayTerm,
    val exitTerm: HolyDayTerm,
    // True while the day is actually in — from its entry until its exit. The times show a day
    // earlier than that; the name does not.
    val isUnderWay: Boolean = true,
    // What is still ahead, when more than one observance runs together: "חג כפול + צום". Null when
    // there is only today, which the chip already names.
    val sequel: List<SequelPart>? = null,
)

/**
 * Names a single day of a run. A Yom Tov that falls *on* Shabbat is one day that is both, and says
 * so with "and" — "יום כיפור ושבת". That is deliberately different from the "+" of the back-to-back
 * [HolyDayInfo.sequel], which is about two separate days in a row.
 */
@Immutable
public sealed interface HolyDayName {
    public data class YomTov(val name: CalendarText.YomTov, val onShabbat: Boolean) : HolyDayName

    /**
     * Shabbat, read together with the named day it falls on, if any — "שבת חול המועד פסח" — since a
     * chip saying only "שבת" would lose the day's own name. An erev is already excluded.
     */
    public data class Shabbat(val dayName: CalendarText.YomTov?) : HolyDayName
}

/**
 * One observance of what is still ahead in a run. Consecutive Yom Tov days are one part — "חג כפול" —
 * so a single day that is both a Yom Tov and Shabbat reads "חג כפול ושבת", matching the way that
 * day's own name is written, while separate days are joined with "+".
 */
@Immutable
public sealed interface SequelPart {
    public data class YomTov(val days: Int, val includesShabbat: Boolean) : SequelPart

    public data object Shabbat : SequelPart

    /** A fast beginning the day after the run ends: Tzom Gedalyah after Rosh Hashana. */
    public data object Fast : SequelPart
}

/**
 * Which day one end of a holy-day span falls on, so its label can say so: "כניסת חג ראשון" …
 * "צאת חג שני". [YomTov] is a span's only Yom Tov day; the ordinals count Yom Tov days in a row.
 */
public enum class HolyDayTerm { Shabbat, YomTov, FirstDay, SecondDay, ThirdDay }

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
internal fun announcedHolyDayInfo(
    date: LocalDate,
    displayedDate: LocalDate,
    now: Instant?,
    settings: ZmanimCalculationSettings,
    location: JewishLocation,
    inIsrael: Boolean,
): HolyDayInfo? {
    fun calendarFor(day: LocalDate) = jewishCalendar(day, inIsrael)
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
            else -> daysOf(runStart, runEnd).firstOrNull { exitOf(it)?.isAfter(now) ?: true }
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
        val fastFollows = isFastDay(runEnd.plusDays(1), settings, location, inIsrael)
        // Which days of the run are Yom Tov, so the two ends can say *which* day of it they are.
        val yomTovDays = daysOf(runStart, runEnd).filter { calendarFor(it).isYomTovAssurBemelacha }
        return HolyDayInfo(
            day = holyDayName(current, calendarFor(current), inIsrael),
            parsha = shabbatParsha(current, calendarFor(current), inIsrael),
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

private fun holyDayName(day: LocalDate, calendar: JewishCalendar, inIsrael: Boolean): HolyDayName =
    if (calendar.isYomTovAssurBemelacha && calendar.hasDayName()) {
        HolyDayName.YomTov(CalendarText.YomTov(day, inIsrael), onShabbat = day.dayOfWeek == DayOfWeek.SATURDAY)
    } else {
        // Chol Hamoed and Isru Chag often fall on Shabbat.
        HolyDayName.Shabbat(minorDayName(calendar, day, inIsrael))
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
internal fun minorDayName(calendar: JewishCalendar, date: LocalDate, inIsrael: Boolean): CalendarText.YomTov? {
    if (calendar.isYomTovAssurBemelacha || calendar.isTaanis) return null
    if (calendar.yomTovIndex in UnnamedErevDays) return null
    return CalendarText.YomTov(date, inIsrael).takeIf { calendar.hasDayName() }
}

/** Whether KosherJava has a name for the day — a holiday, a fast, a named day — or it is ordinary. */
internal fun JewishCalendar.hasDayName(): Boolean = yomTovIndex != -1

/**
 * What names the coming Shabbat while it is only announced — the parasha, so the entry/exit card
 * has a heading without the chip claiming Shabbat is already in. Null for a Yom Tov, which has no
 * weekly parsha of its own, and for any day that is not a Shabbat.
 */
private fun shabbatParsha(day: LocalDate, calendar: JewishCalendar, inIsrael: Boolean): CalendarText.Parsha? {
    if (day.dayOfWeek != DayOfWeek.SATURDAY) return null
    if (calendar.parshah == JewishCalendar.Parshah.NONE) return null
    return CalendarText.Parsha(day, inIsrael)
}

/** How the rest of the stretch reads from [current] onwards, or null when there is nothing to warn about. */
private fun sequelFor(
    current: LocalDate,
    runEnd: LocalDate,
    calendarFor: (LocalDate) -> JewishCalendar,
    fastFollows: Boolean,
): List<SequelPart>? {
    val remaining = daysOf(current, runEnd)
    val parts = mutableListOf<SequelPart>()

    var index = 0
    while (index < remaining.size) {
        if (calendarFor(remaining[index]).isYomTovAssurBemelacha) {
            // Group the consecutive Yom Tov days so they read as one "double"/"triple" unit.
            var count = 0
            var includesShabbat = false
            while (index < remaining.size && calendarFor(remaining[index]).isYomTovAssurBemelacha) {
                if (remaining[index].dayOfWeek == DayOfWeek.SATURDAY) includesShabbat = true
                count++
                index++
            }
            parts += SequelPart.YomTov(count, includesShabbat)
        } else {
            parts += SequelPart.Shabbat
            index++
        }
    }
    if (fastFollows) parts += SequelPart.Fast

    // One thing ahead is just today, which the chip already names.
    return parts.takeIf { it.size >= 2 }
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

private fun daysOf(from: LocalDate, to: LocalDate): List<LocalDate> =
    generateSequence(from) { previous -> previous.plusDays(1).takeIf { !it.isAfter(to) } }.toList()

private const val HolyDayScanBack = 3L
private const val HolyDayScanForward = 5L
