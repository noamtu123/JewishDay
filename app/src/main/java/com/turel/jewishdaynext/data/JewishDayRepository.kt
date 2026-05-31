package com.turel.jewishdaynext.data

import com.turel.jewishdaynext.model.JewishDayInfo
import com.turel.jewishdaynext.model.JewishLocation
import com.turel.jewishdaynext.model.MizrachInfo
import com.turel.jewishdaynext.model.ZmanimCalculationSettings
import com.turel.jewishdaynext.model.ZmanimDay
import com.turel.jewishdaynext.model.defaultJerusalemLocation
import com.turel.jewishdaynext.model.jewishDayInfo
import com.turel.jewishdaynext.model.mizrachInfo
import com.turel.jewishdaynext.model.zmanimForDate
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
    fun getToday(date: LocalDate): JewishDayInfo
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
        val tzeit = findTzeit(location, gregorianDate, settings)
        val jewishDate = if (tzeit != null && !now.isBefore(tzeit)) {
            gregorianDate.plusDays(1)
        } else {
            gregorianDate
        }
        return jewishDayInfo(gregorianDate = gregorianDate, jewishDate = jewishDate)
    }

    override fun getToday(date: LocalDate): JewishDayInfo = jewishDayInfo(date)

    override fun getZmanim(
        location: JewishLocation,
        settings: ZmanimCalculationSettings,
    ): ZmanimDay {
        val date = clock.instant().atZone(location.zoneId).toLocalDate()
        return zmanimForDate(location, date, settings)
    }

    override fun getMizrach(location: JewishLocation): MizrachInfo = mizrachInfo(location)

    private fun findTzeit(
        location: JewishLocation,
        date: LocalDate,
        settings: ZmanimCalculationSettings,
    ): Instant? = zmanimForDate(location, date, settings)
        .groups
        .flatMap { it.items }
        .firstOrNull { it.title == "Tzeit" }
        ?.time
}
