// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import com.noamtu.jewishday.ui.theme.SkyFrame

/**
 * One time of the widget: what the moment is, in the app's language, and the clock time. A [pinned]
 * time is an observance boundary — candle lighting, a fast's end — which the widget keeps however
 * few times it has room for.
 */
data class DayWidgetTime(val label: String, val time: String, val pinned: Boolean = false)

/**
 * The first [limit] of these times, in their order, keeping every pinned one: what a narrower row
 * drops is the zmanim filling in around an observance boundary, never the boundary itself.
 */
internal fun List<DayWidgetTime>.keepingPinned(limit: Int): List<DayWidgetTime> {
    var fill = (limit - count { it.pinned }).coerceAtLeast(0)
    return filter { it.pinned || fill-- > 0 }.take(limit)
}

/**
 * Everything the home-screen widget draws, already in one language and formatted.
 *
 * A widget is a RemoteViews snapshot rather than a live screen, so it cannot pick strings or format
 * times as it renders — every text is resolved here, once, and the layout only places it. The
 * [sky] is the same frame the Glass theme paints, so the widget and the app agree on the time of
 * day.
 */
data class DayWidgetState(
    /** Which language the texts below are in, so the layout can mirror for Hebrew. */
    val useHebrew: Boolean,
    /** The sky at the moment the state was built: sun by day, moon and stars by night. */
    val sky: SkyFrame,
    /** The Hebrew date as the app's header shows it, rolled past tzeit like the header. */
    val hebrewDate: String,
    /** The weekday and civil date beneath it — "Friday, September 25". */
    val weekdayAndDate: String,
    /**
     * The day's badge: the holy day or fast under way, else the day's own name ("Erev Pesach"),
     * else the coming Shabbat — its parasha, worded "Parashat Noach" all week long, or the Yom Tov
     * it falls on once the header has announced it. Null on a day with nothing to say, and on the
     * weekdays ahead of a Shabbat Yom Tov before it is announced, when its bare name would read as
     * today's.
     */
    val chip: String?,
    /** Up to three fully written entry/exit lines — "Shabbat starts 18:10", "Fast ends 19:40". */
    val observanceLines: List<String>,
    /** The day's events — Rosh Chodesh, the Omer count, Chanukah — on one line, or null. */
    val eventLine: String?,
    /**
     * Up to four moments still to come, in order: the next of the user's zmanim, with the day's
     * observance boundaries always among them, and tomorrow's first ones (marked so) once today's
     * are past. Nothing that has already passed.
     */
    val times: List<DayWidgetTime>,
    /** One daily-learning line — "Daf Yomi Bavli: Sanhedrin 78" — or null when none is enabled. */
    val learning: String?,
    /**
     * The caption for where the times were computed — null for a live fix, the same rule the zmanim
     * screen applies, else "Times based on Jerusalem" or the named place, in the app's language.
     */
    val locationName: String?,
)
