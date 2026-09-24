// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import com.noamtu.jewishday.ui.theme.SkyFrame

/** One time row of the widget: what the moment is, in the app's language, and the clock time. */
data class DayWidgetTime(val label: String, val time: String)

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
    /** Up to three moments that shape the day, with an observance's own boundary swapped in. */
    val times: List<DayWidgetTime>,
    /** One daily-learning line — "Daf Yomi Bavli: Sanhedrin 78" — or null when none is enabled. */
    val learning: String?,
    /**
     * The caption for where the times were computed — null for a live fix, the same rule the zmanim
     * screen applies, else "Times based on Jerusalem" or the named place, in the app's language.
     */
    val locationName: String?,
)
