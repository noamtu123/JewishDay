// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday

import com.noamtu.jewishday.core.model.AlotHashacharMethod
import com.noamtu.jewishday.core.model.CandleLightingMethod
import com.noamtu.jewishday.core.model.ChametzMethod
import com.noamtu.jewishday.core.model.CustomZmanUnit
import com.noamtu.jewishday.core.model.CustomZmanValue
import com.noamtu.jewishday.core.model.JewishLocation
import com.noamtu.jewishday.core.model.MinchaGedolaMethod
import com.noamtu.jewishday.core.model.MinchaKetanaMethod
import com.noamtu.jewishday.core.model.MisheyakirMethod
import com.noamtu.jewishday.core.model.MotzeiShabbatMethod
import com.noamtu.jewishday.core.model.PlagHaminchaMethod
import com.noamtu.jewishday.core.model.RabbeinuTamMethod
import com.noamtu.jewishday.core.model.SofZmanShemaMethod
import com.noamtu.jewishday.core.model.SofZmanTefillahMethod
import com.noamtu.jewishday.core.model.TzeitHakochavimMethod
import com.noamtu.jewishday.core.model.ZmanimCalculationSettings
import com.noamtu.jewishday.core.model.ZmanimTimeOption
import com.noamtu.jewishday.core.model.defaultJerusalemLocation
import com.noamtu.jewishday.core.model.jewishDayCivilDate
import com.noamtu.jewishday.core.model.jewishDayInfo
import com.noamtu.jewishday.core.model.nextStatusIconBoundary
import com.noamtu.jewishday.core.model.nextZmanimRefreshBoundary
import com.noamtu.jewishday.core.model.skyDayFor
import com.noamtu.jewishday.core.model.zmanimDayFacts
import com.noamtu.jewishday.day.zmanimForDate
import com.noamtu.jewishday.feature.zmanim.filterForDisplay
import com.noamtu.jewishday.feature.zmanim.toUiState
import com.noamtu.jewishday.i18n.TestText
import com.noamtu.jewishday.ui.theme.nextSkyRedraw
import com.noamtu.jewishday.ui.theme.skyAt
import com.noamtu.jewishday.widget.widgetContent
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * No number is refused, so none may break anything: 9,999,999 minutes (about 19 years) in every
 * custom option, and an angle the sun never reaches (200°), through the screen, the widgets, the
 * icon and the sky.
 */
class HugeCustomValuesTest {
    private val stockholm = JewishLocation("Stockholm", 59.3293, 18.0686, 28.0, ZoneId.of("Europe/Stockholm"))
    private val places = listOf(defaultJerusalemLocation, stockholm)
    private val days = listOf(LocalDate.of(2026, 12, 18), LocalDate.of(2027, 4, 21), LocalDate.of(2027, 6, 25))
    private val hours = listOf(LocalTime.of(0, 10), LocalTime.of(6, 0), LocalTime.of(12, 0), LocalTime.of(19, 0), LocalTime.of(23, 30))

    private fun everyCustom(unit: CustomZmanUnit, values: CustomZmanValue) = ZmanimCalculationSettings(
        alotHashacharMethod = pick(unit, AlotHashacharMethod.CustomDegrees, AlotHashacharMethod.CustomMinutes, AlotHashacharMethod.CustomZmaniyotMinutes),
        alotHashacharCustom = values,
        misheyakirMethod = pick(
            unit,
            MisheyakirMethod.CustomDegrees,
            MisheyakirMethod.CustomMinutesBeforeSunrise,
            MisheyakirMethod.CustomZmaniyotMinutesBeforeSunrise,
        ),
        misheyakirCustom = values,
        sofZmanShemaMethod = pick(unit, SofZmanShemaMethod.CustomDegrees, SofZmanShemaMethod.CustomMinutes, SofZmanShemaMethod.CustomZmaniyotMinutes),
        sofZmanShemaCustom = values,
        sofZmanTefillahMethod = pick(
            unit,
            SofZmanTefillahMethod.CustomDegrees,
            SofZmanTefillahMethod.CustomMinutes,
            SofZmanTefillahMethod.CustomZmaniyotMinutes,
        ),
        sofZmanTefillahCustom = values,
        minchaGedolaMethod = pick(unit, MinchaGedolaMethod.CustomDegrees, MinchaGedolaMethod.CustomMinutes, MinchaGedolaMethod.CustomZmaniyotMinutes),
        minchaGedolaCustom = values,
        minchaKetanaMethod = pick(unit, MinchaKetanaMethod.CustomDegrees, MinchaKetanaMethod.CustomMinutes, MinchaKetanaMethod.CustomZmaniyotMinutes),
        minchaKetanaCustom = values,
        plagHaminchaMethod = pick(unit, PlagHaminchaMethod.CustomDegrees, PlagHaminchaMethod.CustomMinutes, PlagHaminchaMethod.CustomZmaniyotMinutes),
        plagHaminchaCustom = values,
        tzeitHakochavimMethod = pick(
            unit,
            TzeitHakochavimMethod.CustomDegrees,
            TzeitHakochavimMethod.CustomMinutes,
            TzeitHakochavimMethod.CustomZmaniyotMinutes,
        ),
        tzeitHakochavimCustom = values,
        motzeiShabbatMethod = pick(unit, MotzeiShabbatMethod.CustomDegrees, MotzeiShabbatMethod.CustomMinutes, MotzeiShabbatMethod.CustomZmaniyotMinutes),
        motzeiShabbatCustom = values,
        rabbeinuTamMethod = pick(unit, RabbeinuTamMethod.CustomDegrees, RabbeinuTamMethod.CustomMinutes, RabbeinuTamMethod.CustomZmaniyotMinutes),
        rabbeinuTamCustom = values,
        chametzMethod = pick(unit, ChametzMethod.CustomDegrees, ChametzMethod.CustomMinutes, ChametzMethod.CustomZmaniyotMinutes),
        chametzCustom = values,
        candleLightingMethod = CandleLightingMethod.Custom,
        candleLightingCustomMinutes = values.minutes,
    )

    private fun <T> pick(unit: CustomZmanUnit, degrees: T, minutes: T, zmaniyot: T): T = when (unit) {
        CustomZmanUnit.Degrees -> degrees
        CustomZmanUnit.Minutes -> minutes
        CustomZmanUnit.ZmaniyotMinutes -> zmaniyot
    }

    private val huge = CustomZmanValue(degrees = 200.0, minutes = 9_999_999, zmaniyotMinutes = 9_999_999)
    private val largest = CustomZmanValue(degrees = 1e300, minutes = Int.MAX_VALUE, zmaniyotMinutes = Int.MAX_VALUE)
    private val allSettings = CustomZmanUnit.entries.flatMap { unit -> listOf(everyCustom(unit, huge), everyCustom(unit, largest)) }

    @Test
    fun nothingBreaksAnywhere() {
        for (settings in allSettings) {
            for (place in places) {
                for (day in days) {
                    for (hour in hours) {
                        val now = day.atTime(hour).atZone(place.zoneId).toInstant()
                        val facts = zmanimDayFacts(place, day, settings, now)
                        // The screen, worded in both languages.
                        val state = zmanimForDate(place, day, settings, now)
                            .filterForDisplay(ZmanimTimeOption.Default, emptySet())
                            .toUiState(use24HourTime = true, showCandleLightingPrompt = false, developerTimeOverrideActive = false, dayOffset = 0)
                        for (text in listOf(TestText.english, TestText.hebrew)) {
                            state.groups.forEach { group -> group.rows.forEach { row -> text.resolve(row.value) } }
                            // The widgets.
                            widgetContent(
                                day = facts,
                                tomorrow = { zmanimDayFacts(place, day.plusDays(1), settings, null, false) },
                                text = text,
                                use24HourTime = true,
                                enabledZmanim = ZmanimTimeOption.Default,
                                now = now,
                                observanceBoundary = nextZmanimRefreshBoundary(place, settings, now),
                            )
                        }
                        // The icon.
                        jewishDayInfo(gregorianDate = day, jewishDate = jewishDayCivilDate(place, settings, now))
                        assertTrue(nextStatusIconBoundary(place, settings, now).isAfter(now))
                        // The sky.
                        val sky = skyDayFor(place, day, settings)
                        skyAt(now, sky, place.zoneId)
                        assertTrue(nextSkyRedraw(now, sky, place.zoneId).isAfter(now))
                        // A stepped-to day.
                        zmanimForDate(place, day.plusDays(2), settings, null, false)
                    }
                }
            }
        }
    }

    @Test
    fun withNoPlagAHugeCandleOffsetIsHeldAtSunrise() {
        // A plag at 200° never comes, so there is nothing to hold 999,999 minutes (694 days) at but the day itself.
        listOf(999_999, Int.MAX_VALUE).forEach { minutes ->
            val settings = ZmanimCalculationSettings(
                plagHaminchaMethod = PlagHaminchaMethod.CustomDegrees,
                plagHaminchaCustom = huge,
                candleLightingMethod = CandleLightingMethod.Custom,
                candleLightingCustomMinutes = minutes,
            )
            for (place in places) {
                val friday = days[0]
                val facts = zmanimDayFacts(place, friday, settings, friday.atTime(9, 0).atZone(place.zoneId).toInstant())
                assertNull(facts.zmanim.single { it.option == ZmanimTimeOption.PlagHamincha }.time)
                val sunrise = requireNotNull(facts.zmanim.single { it.option == ZmanimTimeOption.Sunrise }.time)
                val lighting = requireNotNull(requireNotNull(facts.shabbat).candleLighting)
                assertEquals("${place.name} $minutes", sunrise, lighting.time)
                assertEquals(friday, lighting.time.atZone(place.zoneId).toLocalDate())
            }
        }
    }

    @Test
    fun anOffsetTooLongToWorkWithGivesNoTimeAndOneWithinItStillDoes() {
        // Int.MAX_VALUE minutes is about 4,000 years: past what a halachic hour can be measured in.
        for (unit in listOf(CustomZmanUnit.Minutes, CustomZmanUnit.ZmaniyotMinutes)) {
            val facts = zmanimDayFacts(defaultJerusalemLocation, days[0], everyCustom(unit, largest))
            for (option in listOf(ZmanimTimeOption.AlotHashachar, ZmanimTimeOption.TallitTefillin, ZmanimTimeOption.SofZmanShemaMagenAvraham)) {
                assertNull("$unit $option", facts.zmanim.single { it.option == option }.time)
            }
        }
        // 9,999,999 minutes (about 19 years) is still worked out.
        val facts = zmanimDayFacts(defaultJerusalemLocation, days[0], everyCustom(CustomZmanUnit.Minutes, huge))
        assertNotNull(facts.zmanim.single { it.option == ZmanimTimeOption.AlotHashachar }.time)
        assertNotNull(facts.zmanim.single { it.option == ZmanimTimeOption.SofZmanShemaMagenAvraham }.time)
    }

    @Test
    fun anAngleTheSunNeverReachesGivesNoTime() {
        val settings = everyCustom(CustomZmanUnit.Degrees, huge)
        val facts = zmanimDayFacts(defaultJerusalemLocation, days[0], settings)
        val empty = listOf(ZmanimTimeOption.AlotHashachar, ZmanimTimeOption.TallitTefillin, ZmanimTimeOption.SofZmanShemaMagenAvraham)
        for (option in empty) assertNull(option.name, facts.zmanim.single { it.option == option }.time)
        // Nightfall takes its stand-in, exactly as on a far-north night the sun never gets that dark.
        assertNotNull(facts.zmanim.single { it.option == ZmanimTimeOption.Tzeit }.fallback)
    }
}
