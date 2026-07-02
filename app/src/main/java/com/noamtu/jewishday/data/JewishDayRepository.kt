package com.noamtu.jewishday.data

import com.noamtu.jewishday.model.JewishDayInfo
import com.noamtu.jewishday.model.JewishLocation
import com.noamtu.jewishday.model.MizrachInfo
import com.noamtu.jewishday.model.ZmanimCalculationSettings
import com.noamtu.jewishday.model.ZmanimDay
import com.noamtu.jewishday.model.defaultJerusalemLocation
import com.noamtu.jewishday.model.jewishDayInfo
import com.noamtu.jewishday.model.mizrachInfo
import com.noamtu.jewishday.model.sunsetForDate
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
        val gregorianDate = now.atZone(location.zoneId).toLocalDate()
        // The Hebrew date rolls over to the next day at sunset (the day/night boundary).
        val sunset = sunsetForDate(location, gregorianDate, settings)
        val jewishDate = if (sunset != null && !now.isBefore(sunset)) {
            gregorianDate.plusDays(1)
        } else {
            gregorianDate
        }
        return jewishDayInfo(gregorianDate = gregorianDate, jewishDate = jewishDate)
    }

    override fun getZmanim(
        location: JewishLocation,
        settings: ZmanimCalculationSettings,
    ): ZmanimDay {
        val now = clock.instant()
        val date = now.atZone(location.zoneId).toLocalDate()
        return zmanimForDate(location, date, settings, now)
    }

    override fun getMizrach(location: JewishLocation): MizrachInfo = mizrachInfo(location)
}
