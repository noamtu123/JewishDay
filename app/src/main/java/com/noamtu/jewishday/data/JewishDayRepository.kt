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
import com.noamtu.jewishday.model.tzeitForDate
import com.noamtu.jewishday.model.zmanimDateFor
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
    fun getZmanim(
        location: JewishLocation = defaultJerusalemLocation,
        settings: ZmanimCalculationSettings = ZmanimCalculationSettings(),
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
        // measured against the day the zmanim belong to (which turns over at chatzot halaila), so
        // that the date the icon shows is the one the app is showing at that moment.
        val civilDate = now.atZone(location.zoneId).toLocalDate()
        val zmanimDate = zmanimDateFor(location, settings, now)
        val tzeit = tzeitForDate(location, zmanimDate, settings)
        val jewishDate = if (tzeit != null && !now.isBefore(tzeit)) {
            zmanimDate.plusDays(1)
        } else {
            zmanimDate
        }
        return jewishDayInfo(gregorianDate = civilDate, jewishDate = jewishDate)
    }

    override fun getZmanim(
        location: JewishLocation,
        settings: ZmanimCalculationSettings,
    ): ZmanimDay {
        val now = clock.instant()
        // The night belongs to the day it started on, so the times only move on at chatzot halaila.
        val date = zmanimDateFor(location, settings, now)
        return zmanimForDate(location, date, settings, now)
    }

    override fun getMizrach(location: JewishLocation): MizrachInfo = mizrachInfo(location)
}