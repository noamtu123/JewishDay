// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.day

import com.noamtu.jewishday.core.model.ZmanimCalculationSettings
import com.noamtu.jewishday.core.model.defaultJerusalemLocation
import com.noamtu.jewishday.core.model.holyDayExitForDate
import com.noamtu.jewishday.core.model.sunsetForDate
import com.noamtu.jewishday.core.model.tzeitForDate
import com.noamtu.jewishday.day.zmanimForDate
import com.noamtu.jewishday.i18n.en
import com.noamtu.jewishday.i18n.he
import com.noamtu.jewishday.i18n.nameText
import com.noamtu.jewishday.i18n.sequelText
import java.time.Duration
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every holy day — Shabbat, Yom Tov, Yom Kippur — goes out at tzeit 6.2° plus five minutes.
 * Ordinary fasts still simply end at tzeit, with no added minutes.
 */
class HolyDayExitTest {
    private val zone = defaultJerusalemLocation.zoneId

    private fun rowTime(date: LocalDate, title: String) =
        zmanimForDate(date = date).groups.flatMap { it.items }.firstOrNull { it.title.en() == title }?.time

    @Test
    fun holyDayExitIsTzeitPlusTheTosefetMinutes() {
        val saturday = LocalDate.of(2026, 9, 5)
        val tzeit = requireNotNull(tzeitForDate(date = saturday))

        // Default tosefet is five minutes on top of the motzei method (6.2°, the same as tzeit).
        assertEquals(Duration.ofMinutes(5), Duration.between(tzeit, requireNotNull(holyDayExitForDate(date = saturday))))

        // And it follows the setting.
        val customised = ZmanimCalculationSettings(holyDayTosefetMinutes = 20)
        val exit = requireNotNull(holyDayExitForDate(date = saturday, settings = customised))
        assertEquals(Duration.ofMinutes(20), Duration.between(tzeit, exit))
    }

    @Test
    fun yomKippurIsDescribedAsAHolyDayNotAFast() {
        // 2028-09-30 is Yom Kippur on Shabbat.
        val yomKippur = LocalDate.of(2028, 9, 30)
        val day = zmanimForDate(date = yomKippur)

        // It is a holy day, so it carries no separate fast card at all.
        assertNull(day.fastDayInfo)
        val holyDay = requireNotNull(day.holyDayInfo)
        // A single day that is both Shabbat and Yom Kippur: named for the Yom Tov, and since
        // nothing follows it there is no back-to-back warning.
        assertTrue(holyDay.nameText.he(), holyDay.nameText.he().contains("כיפור"))
        assertNull(holyDay.sequelText?.he())

        assertEquals(holyDayExitForDate(date = yomKippur), holyDay.endTime)
        // Entry carries the tosefet: before sunset, not at it.
        assertTrue(requireNotNull(holyDay.startTime).isBefore(requireNotNull(sunsetForDate(date = yomKippur.minusDays(1)))))
    }

    @Test
    fun ordinaryFastsStillEndAtPlainTzeit() {
        val seventeenTammuz = LocalDate.of(2026, 7, 2)
        val fast = requireNotNull(zmanimForDate(date = seventeenTammuz).fastDayInfo)

        assertEquals(tzeitForDate(date = seventeenTammuz), fast.endTime)
        // And they begin at dawn, with no tosefet.
        assertTrue(requireNotNull(fast.startTime).atZone(zone).hour < 7)
    }

}
