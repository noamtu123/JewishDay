// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

import androidx.compose.runtime.Immutable
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import java.time.Instant
import java.time.LocalDate

/**
 * The fasts the date header announces. Yom Kippur is deliberately absent: it is a holy day, so the
 * header describes it as one — entered with the candle-lighting tosefet and left at the holy-day
 * exit, not as a plain fast.
 */
public enum class Fast(internal val yomTovIndex: Int) {
    Gedalyah(JewishCalendar.FAST_OF_GEDALYAH),
    TishaBav(JewishCalendar.TISHA_BEAV),
    SeventeenthOfTammuz(JewishCalendar.SEVENTEEN_OF_TAMMUZ),
    TenthOfTevet(JewishCalendar.TENTH_OF_TEVES),
    Esther(JewishCalendar.FAST_OF_ESTHER),
}

@Immutable
public data class FastDayInfo(
    val fast: Fast,
    val startTime: Instant?,
    val endTime: Instant?,
    // True while the fast is actually running. The times show a day ahead, but the name waits:
    // the Hebrew date rolls at sunset, so a dawn fast owns its date all night before it begins.
    val isUnderWay: Boolean = true,
)

/**
 * The fast to show in the date header, or null. A fast is announced one Jewish day before it
 * begins and disappears the moment it ends, so at most one is ever current.
 *
 * Today plus the next two civil days are considered: an evening fast begins at the sunset before
 * its own date, so its announcement starts two civil days ahead of that date.
 */
internal fun announcedFastDayInfo(
    date: LocalDate,
    displayedDate: LocalDate,
    now: Instant?,
    settings: ZmanimCalculationSettings,
    location: JewishLocation,
    inIsrael: Boolean,
): FastDayInfo? {
    for (offset in 0L..2L) {
        val fastDate = date.plusDays(offset)
        val fastCalendar = jewishCalendar(fastDate, inIsrael)
        val info = fastDayInfo(fastCalendar, fastDate, settings, location) ?: continue
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

/** Whether a fast falls on [date] — Tzom Gedalyah right after Rosh Hashana, say. */
internal fun isFastDay(
    date: LocalDate,
    settings: ZmanimCalculationSettings,
    location: JewishLocation,
    inIsrael: Boolean,
): Boolean = fastDayInfo(jewishCalendar(date, inIsrael), date, settings, location) != null

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

private fun startsPreviousEvening(jewishCalendar: JewishCalendar): Boolean =
    jewishCalendar.yomTovIndex == JewishCalendar.TISHA_BEAV

private fun fastDayInfo(
    jewishCalendar: JewishCalendar,
    date: LocalDate,
    settings: ZmanimCalculationSettings,
    location: JewishLocation,
): FastDayInfo? {
    if (!jewishCalendar.isTaanis) return null
    val fast = Fast.entries.firstOrNull { it.yomTovIndex == jewishCalendar.yomTovIndex } ?: return null
    val calendar = comprehensiveZmanimCalendar(location, date, settings)
    // Tisha B'Av begins at the previous sunset, the four dawn fasts at alot. None of them carries
    // a tosefet either side — they are not holy days — so all simply end at tzeit.
    val startTime = if (startsPreviousEvening(jewishCalendar)) {
        comprehensiveZmanimCalendar(location, date.minusDays(1), settings)
            .sunset(settings.sunsetMethod)
    } else {
        calendar.alotHashachar(settings)
    }
    val endTime = calendar.tzeit(settings)
    return FastDayInfo(fast = fast, startTime = startTime, endTime = endTime)
}
