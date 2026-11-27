// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.day

import com.noamtu.jewishday.R
import com.noamtu.jewishday.core.i18n.CalendarText
import com.noamtu.jewishday.core.model.DayEvent
import com.noamtu.jewishday.core.model.JewishLocation
import com.noamtu.jewishday.core.model.OfflineLearning
import com.noamtu.jewishday.core.model.ShabbatTimes
import com.noamtu.jewishday.core.model.ZmanTime
import com.noamtu.jewishday.core.model.ZmanimCalculationSettings
import com.noamtu.jewishday.core.model.ZmanimDayFacts
import com.noamtu.jewishday.core.model.ZmanimTimeOption
import com.noamtu.jewishday.core.model.defaultJerusalemLocation
import com.noamtu.jewishday.core.model.zmanimDayFacts
import com.noamtu.jewishday.i18n.UiText
import com.noamtu.jewishday.i18n.asUiText
import com.noamtu.jewishday.i18n.caption
import com.noamtu.jewishday.i18n.description
import com.noamtu.jewishday.i18n.label
import com.noamtu.jewishday.i18n.methodCaption
import com.noamtu.jewishday.i18n.nightfallCaption
import com.noamtu.jewishday.i18n.title
import com.noamtu.jewishday.i18n.uiText
import com.noamtu.jewishday.i18n.verbatim
import java.time.Instant
import java.time.LocalDate

/**
 * The zmanim screen's day for [date]: the core works out what is true (`zmanimDayFacts`), and this
 * lays it out as the header and rows the screen, the widgets and the repository show.
 *
 * [hebrewDateRolled] overrides whether the Hebrew date has rolled past tzeit. A day stepped to from
 * today has no clock of its own, so it takes today's answer — otherwise stepping from yesterday
 * (unrolled) to an evening today (rolled) would jump the Hebrew date and weekday by two.
 */
fun zmanimForDate(
    location: JewishLocation = defaultJerusalemLocation,
    date: LocalDate,
    settings: ZmanimCalculationSettings = ZmanimCalculationSettings(),
    now: Instant? = null,
    hebrewDateRolled: Boolean? = null,
): ZmanimDay = zmanimDayFacts(location, date, settings, now, hebrewDateRolled).toZmanimDay(settings)

private fun ZmanimDayFacts.toZmanimDay(settings: ZmanimCalculationSettings): ZmanimDay {
    val parshaRow = weeklyParsha?.let { parsha ->
        ZmanItem(
            title = uiText(R.string.row_weekly_parsha),
            time = null,
            description = uiText(R.string.row_upcoming_reading),
            value = parsha.asUiText(),
            kind = ZmanItemKind.WeeklyParsha,
        )
    }
    return ZmanimDay(
        locationName = location.name,
        date = date,
        displayedDate = displayedDate,
        zoneId = location.zoneId,
        hebrewDate = CalendarText.HebrewDate(displayedDate).asUiText(),
        dayName = dayName?.asUiText(),
        fastDayInfo = fastDayInfo,
        holyDayInfo = holyDayInfo,
        fastLeadsHeader = fastLeadsHeader,
        groups = listOfNotNull(
            // The parsha lives at the top of the Shabbat section; when that section is dropped it
            // moves up here rather than disappearing.
            (events.flatMap { it.rows(settings) } + listOfNotNull(parshaRow.takeIf { shabbat == null }))
                .takeIf { it.isNotEmpty() }
                ?.let { items -> ZmanimGroup(ZmanimGroupKind.Events, items) },
            // Each row carries its ZmanimTimeOption id so it can be shown/hidden.
            ZmanimGroup(ZmanimGroupKind.Zmanim, zmanim.map { it.row(settings) }),
            shabbat?.let { ZmanimGroup(ZmanimGroupKind.Shabbat, it.rows(parshaRow, settings)) },
            ZmanimGroup(ZmanimGroupKind.DailyLearning, offlineLearning.map { it.row() }),
        ),
    )
}

/** A row of the zmanim list: the time its option names, and the method that gave it. */
private fun ZmanTime.row(settings: ZmanimCalculationSettings): ZmanItem = ZmanItem(
    title = uiText(option.label),
    time = time,
    // On a night with no tzeit the method did not give this time, so the caption names what did.
    description = fallback?.nightfallCaption ?: option.methodCaption(settings),
    id = option.storageValue,
)

private fun DayEvent.rows(settings: ZmanimCalculationSettings): List<ZmanItem> = when (this) {
    is DayEvent.RoshChodesh -> listOf(
        ZmanItem(
            title = uiText(R.string.row_rosh_chodesh),
            time = null,
            description = uiText(R.string.row_rosh_chodesh_description),
            value = name.asUiText(),
            kind = ZmanItemKind.RoshChodesh,
        ),
    )
    is DayEvent.Omer -> listOf(
        ZmanItem(
            title = uiText(R.string.row_omer),
            time = null,
            description = uiText(R.string.row_omer_description),
            value = count.asUiText(),
            kind = ZmanItemKind.Omer,
        ),
    )
    is DayEvent.Chanukah -> listOf(
        ZmanItem(
            title = uiText(R.string.row_chanukah),
            time = null,
            description = uiText(R.string.row_chanukah_description),
            value = verbatim(day.toString()),
        ),
    )
    is DayEvent.Chametz -> {
        val method = settings.chametzMethod.caption(settings)
        listOf(
            ZmanItem(uiText(R.string.row_eat_chametz), eatUntil, method),
            ZmanItem(uiText(R.string.row_burn_chametz), burnUntil, method),
        )
    }
}

/**
 * The Shabbat section: what names it, then its four times. The parsha and reading rows (no id)
 * always show; the four time rows carry their ZmanimTimeOption id so the same "Zmanim to show" list
 * can hide them.
 */
private fun ShabbatTimes.rows(parshaRow: ZmanItem?, settings: ZmanimCalculationSettings): List<ZmanItem> = listOfNotNull(
    // Named "קריאת התורה", not "פרשת השבוע: ראש השנה", which would not be true.
    parshaRow ?: dayName?.let { name ->
        ZmanItem(
            title = uiText(R.string.row_torah_reading),
            time = null,
            description = uiText(R.string.row_upcoming_reading),
            value = name.asUiText(),
            kind = ZmanItemKind.TorahReading,
        )
    },
    ZmanItem(
        title = uiText(R.string.shabbat_row_candles),
        time = candleLighting?.time,
        description = uiText(R.string.shabbat_caption_friday, candleLighting.caption(settings)),
        id = ZmanimTimeOption.ShabbatCandleLighting.storageValue,
    ),
    ZmanItem(
        title = uiText(R.string.zman_sunset),
        time = sunset,
        description = uiText(R.string.shabbat_caption_friday, ZmanimTimeOption.ShabbatSunset.methodCaption(settings)),
        id = ZmanimTimeOption.ShabbatSunset.storageValue,
    ),
    ZmanItem(
        title = uiText(ZmanimTimeOption.MotzeiShabbat.label),
        time = exit,
        description = uiText(
            R.string.shabbat_caption_motzei,
            exitFallback?.nightfallCaption ?: ZmanimTimeOption.MotzeiShabbat.methodCaption(settings),
            UiText.Number(settings.holyDayTosefetMinutes),
        ),
        id = ZmanimTimeOption.MotzeiShabbat.storageValue,
    ),
    ZmanItem(
        title = uiText(ZmanimTimeOption.RabbeinuTam.label),
        time = rabbeinuTam,
        description = uiText(R.string.shabbat_caption_saturday, ZmanimTimeOption.RabbeinuTam.methodCaption(settings)),
        id = ZmanimTimeOption.RabbeinuTam.storageValue,
    ),
)

/** One daily-learning row: the track, what the day reads of it, and a word on where it comes from. */
private fun OfflineLearning.row(): ZmanItem = ZmanItem(
    title = uiText(track.title),
    time = null,
    description = track.description,
    value = reading.asUiText(),
    id = track.type.storageValue,
)
