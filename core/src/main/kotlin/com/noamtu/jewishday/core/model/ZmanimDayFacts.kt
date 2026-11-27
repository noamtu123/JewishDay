// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

import androidx.compose.runtime.Immutable
import com.noamtu.jewishday.core.i18n.CalendarText
import java.time.Instant
import java.time.LocalDate

/**
 * Everything the zmanim screen shows for one civil day, worked out but not yet worded: the times,
 * the day's events and observances, and the calendar facts each language names its own way. The
 * app turns this into rows (`zmanimForDate`); nothing here knows what any of it is called.
 */
@Immutable
public data class ZmanimDayFacts(
    val location: JewishLocation,
    // The civil date these zmanim are for. It turns over at midnight like any calendar day, and is
    // whatever day the user has stepped to.
    val date: LocalDate,
    // The civil date the Hebrew date belongs to: [date] until tzeit, the day after from tzeit on —
    // or, after midnight but before a northern summer's late tzeit of the day before, that day.
    val displayedDate: LocalDate,
    // The day's own name — "ערב פסח", "פורים", "חול המועד סוכות" — on the days no fast or holy day
    // already claims it. Read off the same calendar as the displayed Hebrew date, so the two never
    // disagree. Null on an ordinary day.
    val dayName: CalendarText.YomTov?,
    // Populated from one Jewish day before one of the six fasts begins until it ends.
    val fastDayInfo: FastDayInfo?,
    // Populated from one Jewish day before a holy day enters until it ends.
    val holyDayInfo: HolyDayInfo?,
    // Which of the two names the header chip, when a fast and a holy day are both showing.
    val fastLeadsHeader: Boolean,
    // The day's occasional events (Rosh Chodesh, the Omer, Erev Pesach's chametz times…).
    val events: List<DayEvent>,
    // The day's zmanim in the order they are listed.
    val zmanim: List<ZmanTime>,
    // This week's parsha, read off the coming Shabbat; null when that Shabbat has none (a Yom Tov or
    // Chol Hamoed).
    val weeklyParsha: CalendarText.Parsha?,
    // The coming Shabbat's own section, or null once Shabbat is in and the header already gives
    // everything it would (see `zmanimDayFacts`).
    val shabbat: ShabbatTimes?,
    // The daily learning KosherJava can work out offline, shown until (or unless) Hebcal answers.
    val offlineLearning: List<OfflineLearning>,
)

/**
 * One zman of the day's list: which it is, and when (null when it does not occur that day).
 * [fallback] says which stand-in gave [time] on a night with no tzeit of its own (see [NoTzeitFallback]).
 */
@Immutable
public data class ZmanTime(
    val option: ZmanimTimeOption,
    val time: Instant?,
    val fallback: NoTzeitFallback? = null,
)

/** An occasional event of the day, shown above its zmanim. */
@Immutable
public sealed interface DayEvent {
    public data class RoshChodesh(val name: CalendarText.RoshChodesh) : DayEvent

    public data class Omer(val count: CalendarText.Omer) : DayEvent

    public data class Chanukah(val day: Int) : DayEvent

    /**
     * Erev Pesach: the last time to eat chametz, and to burn it. When Erev Pesach falls on Shabbat
     * the burning moves to Friday: Friday has only the burn ([eats] false), Shabbat only the eating
     * ([burns] false).
     */
    public data class Chametz(
        val eatUntil: Instant?,
        val burnUntil: Instant?,
        val eats: Boolean = true,
        val burns: Boolean = true,
    ) : DayEvent
}

/** The coming Shabbat: what names it, when it comes in, and when it goes out. */
@Immutable
public data class ShabbatTimes(
    // When the Shabbat has no weekly parsha (a Yom Tov or Chol Hamoed), the day it is instead —
    // "ראש השנה" — so the section still says which Shabbat its times belong to.
    val dayName: CalendarText.YomTov?,
    val candleLighting: CandleLighting?,
    val sunset: Instant?,
    val exit: Instant?,
    val rabbeinuTam: Instant?,
    // Which stand-in gave [exit], on a Saturday night with no nightfall of its own.
    val exitFallback: NoTzeitFallback? = null,
)

/** The daily-learning tracks KosherJava can work out offline, in the order they are listed. */
public enum class OfflineLearningTrack(public val type: DailyLearningType) {
    DafYomiBavli(DailyLearningType.DafYomiBavli),
    DafYomiYerushalmi(DailyLearningType.DafYomiYerushalmi),
    TehillimYomi(DailyLearningType.TehillimYomi),
}

/** One offline daily-learning reading: its track, and the day's portion of it. */
@Immutable
public data class OfflineLearning(val track: OfflineLearningTrack, val reading: CalendarText)
