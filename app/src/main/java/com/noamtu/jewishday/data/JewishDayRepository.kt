// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import com.noamtu.jewishday.model.JewishDayInfo
import com.noamtu.jewishday.model.JewishLocation
import com.noamtu.jewishday.model.MizrachInfo
import com.noamtu.jewishday.model.ZmanimCalculationSettings
import com.noamtu.jewishday.model.ZmanimDay
import com.noamtu.jewishday.model.defaultJerusalemLocation
import com.noamtu.jewishday.model.jewishDayInfo
import com.noamtu.jewishday.model.mizrachInfo
import com.noamtu.jewishday.model.jewishDayCivilDate
import com.noamtu.jewishday.model.zmanimForDate
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

interface JewishDayRepository {
    fun getToday(): JewishDayInfo
    fun getToday(
        location: JewishLocation,
        settings: ZmanimCalculationSettings = ZmanimCalculationSettings(),
    ): JewishDayInfo
    /**
     * The zmanim for the civil day [dayOffset] days from today — 0 being today, which is the only
     * one computed against the clock. A stepped-to day is a day being read about rather than lived
     * through, so it carries no "happening now" state.
     */
    fun getZmanim(
        location: JewishLocation = defaultJerusalemLocation,
        settings: ZmanimCalculationSettings = ZmanimCalculationSettings(),
        dayOffset: Int = 0,
    ): ZmanimDay
    fun getMizrach(location: JewishLocation = defaultJerusalemLocation): MizrachInfo
}

class DefaultJewishDayRepository @Inject constructor(
    private val clock: Clock,
) : JewishDayRepository {
    override fun getToday(): JewishDayInfo = getToday(defaultJerusalemLocation)

    override fun getToday(
        location: JewishLocation,
        settings: ZmanimCalculationSettings,
    ): JewishDayInfo {
        val now = clock.instant()
        // The icon's two lines move on different boundaries. The weekday is the plain civil
        // weekday, so it turns over at midnight — read on its own in the status bar, a day name
        // that jumps at nightfall reads as a mistake. The Hebrew date under it rolls at tzeit,
        // so it is the same date the app's own header is showing at that moment.
        val civilDate = now.atZone(location.zoneId).toLocalDate()
        // The Jewish day already rolls at tzeit, so its own civil date is the Hebrew date to show.
        val jewishDate = jewishDayCivilDate(location, settings, now)
        return jewishDayInfo(gregorianDate = civilDate, jewishDate = jewishDate)
    }

    override fun getZmanim(
        location: JewishLocation,
        settings: ZmanimCalculationSettings,
        dayOffset: Int,
    ): ZmanimDay {
        val now = clock.instant()
        // A plain calendar day, turning over at midnight. The Hebrew date in the header still rolls
        // at tzeit — zmanimForDate works that out from [now] — but the times themselves do not move
        // under you during the evening; stepping to another day is something you ask for.
        val today = now.atZone(location.zoneId).toLocalDate()
        return if (dayOffset == 0) {
            zmanimForDate(location, today, settings, now)
        } else {
            // Carry today's tzeit roll over, so each step moves the Hebrew date by exactly one.
            val hebrewDateRolled = jewishDayCivilDate(location, settings, now) != today
            zmanimForDate(location, today.plusDays(dayOffset.toLong()), settings, null, hebrewDateRolled)
        }
    }

    override fun getMizrach(location: JewishLocation): MizrachInfo = mizrachInfo(location)
}