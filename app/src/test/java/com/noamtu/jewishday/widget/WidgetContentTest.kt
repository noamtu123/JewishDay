// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import com.noamtu.jewishday.model.DailyLearningType
import com.noamtu.jewishday.model.ZmanimCalculationSettings
import com.noamtu.jewishday.model.ZmanimTimeOption
import com.noamtu.jewishday.model.defaultJerusalemLocation
import com.noamtu.jewishday.model.nextZmanimRefreshBoundary
import com.noamtu.jewishday.model.sunsetForDate
import com.noamtu.jewishday.model.tzeitForDate
import com.noamtu.jewishday.model.zmanimForDate
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fixed dates (Jerusalem): Friday 2026-09-04, Shabbat Nitzavim-Vayeilech the day after. */
class WidgetContentTest {
    private val friday = LocalDate.of(2026, 9, 4)
    private val settings = ZmanimCalculationSettings()

    private fun content(
        now: Instant,
        date: LocalDate = friday,
        hebrew: Boolean = false,
        enabled: Set<ZmanimTimeOption> = ZmanimTimeOption.Default,
        learning: Set<DailyLearningType> = emptySet(),
    ) = widgetContent(
        day = zmanimForDate(date = date, settings = settings, now = now),
        tomorrow = { zmanimForDate(date = date.plusDays(1), settings = settings, hebrewDateRolled = false) },
        hebrew = hebrew,
        use24HourTime = true,
        enabledZmanim = enabled,
        now = now,
        observanceBoundary = nextZmanimRefreshBoundary(defaultJerusalemLocation, settings, now),
        enabledLearning = learning,
    )

    private val fridayAfternoon = requireNotNull(sunsetForDate(date = friday)).minus(Duration.ofHours(3))

    @Test
    fun nextIsTheFirstTimeStillAheadAndEverythingBeforeItIsPast() {
        val content = content(fridayAfternoon)
        val nextIndex = requireNotNull(content.nextIndex)

        assertTrue(content.zmanim.take(nextIndex).all { it.isPast })
        assertFalse(content.zmanim[nextIndex].isPast)
        assertEquals(content.zmanim[nextIndex], content.next)
        assertEquals(content.zmanim[nextIndex + 1], content.following)
        // Refreshed as soon as that zman passes, if nothing else changes first.
        assertTrue(!content.refreshAt.isAfter(content.zmanim[nextIndex].instant.plusSeconds(1)))
    }

    @Test
    fun onFridayTheListIncludesShabbatComingIn() {
        val content = content(fridayAfternoon)

        assertTrue(content.zmanim.map { it.title }.toString(), content.zmanim.any { it.title == "Shabbat starts" })
        val times = content.zmanim.map { it.instant }
        assertEquals(times.sorted(), times)
    }

    @Test
    fun shabbatWidgetNamesTheParashaUntilShabbatComesIn() {
        val shabbat = requireNotNull(content(fridayAfternoon).shabbat)

        assertEquals("Parashat Nitzavim-Vayeilech", shabbat.title)
        assertNotNull(shabbat.entryTime)
        assertNotNull(shabbat.exitTime)
        // The exit is tomorrow, so it says which day.
        assertTrue(shabbat.exitTime, shabbat.exitTime!!.startsWith("Sat"))

        val hebrew = requireNotNull(content(fridayAfternoon, hebrew = true).shabbat)
        assertEquals("פרשת נצבים וילך", hebrew.title)
        assertEquals("כניסת שבת", hebrew.entryLabel)
    }

    @Test
    fun onceTodaysTimesArePastTheNextOnesAreTomorrows() {
        val evening = requireNotNull(sunsetForDate(date = friday.minusDays(2))).plus(Duration.ofHours(2))
        val content = content(evening, date = friday.minusDays(2), enabled = setOf(ZmanimTimeOption.Sunrise, ZmanimTimeOption.Sunset))

        assertEquals(null, content.nextIndex)
        val next = requireNotNull(content.next)
        assertEquals("Sunrise", next.title)
        assertTrue(next.time, next.time.startsWith("Thu"))
        assertEquals("Sunset", content.following?.title)
    }

    @Test
    fun theZmanimWindowKeepsTheNextOneInView() {
        val content = content(fridayAfternoon)
        val nextIndex = requireNotNull(content.nextIndex)

        val rows = visibleRows(content, capacity = 4)
        assertEquals(4, rows.size)
        assertEquals(nextIndex - 1, rows.first().index)
        assertTrue(rows.any { it.index == nextIndex })
        // Everything fits: nothing is dropped.
        assertEquals(content.zmanim.size, visibleRows(content, capacity = 50).size)
    }

    @Test
    fun onFridayTheUpcomingTimesKeepShabbatsEntryAndExit() {
        val upcoming = content(fridayAfternoon).upcoming

        assertEquals(4, upcoming.size)
        assertEquals(upcoming.map { it.instant }.sorted(), upcoming.map { it.instant })
        assertTrue(upcoming.toString(), upcoming.any { it.title == "Shabbat starts" && it.observance })
        // Tomorrow's exit is past a full day of zmanim, yet kept, and says which day it is.
        val exit = upcoming.single { it.title == "Shabbat ends" }
        assertTrue(exit.observance)
        assertTrue(exit.time, exit.time.startsWith("Sat"))
    }

    @Test
    fun aShabbatNotYetAnnouncedIsNotPinned() {
        val thursday = friday.minusDays(1)
        val morning = requireNotNull(sunsetForDate(date = thursday)).minus(Duration.ofHours(8))

        val upcoming = content(morning, date = thursday).upcoming

        assertTrue(upcoming.toString(), upcoming.none { it.observance })
    }

    @Test
    fun fromTheEveningBeforeTheComingShabbatsEntryIsAmongTheTimes() {
        val thursday = friday.minusDays(1)
        val night = requireNotNull(tzeitForDate(date = thursday)).plus(Duration.ofMinutes(30))

        val entry = content(night, date = thursday).upcoming.single { it.title == "Shabbat starts" }

        assertTrue(entry.observance)
        assertTrue(entry.time, entry.time.startsWith("Fri"))
    }

    @Test
    fun tonightsTimesKeepThePlainClockEvenPastMidnight() {
        // Wednesday night: tonight's chatzot halaila falls on Thursday's date, but it is tonight's,
        // as on the zmanim screen, so it reads as a plain time rather than "Thu 00:40".
        val wednesday = friday.minusDays(2)
        val night = requireNotNull(tzeitForDate(date = wednesday)).plus(Duration.ofMinutes(30))
        val content = content(night, date = wednesday)

        val tonights = content.upcoming.filter { upcoming -> content.zmanim.any { it.instant == upcoming.instant } }
        assertTrue(content.upcoming.toString(), tonights.isNotEmpty())
        assertTrue(tonights.toString(), tonights.all { zman -> zman.time.none(Char::isLetter) })
        // And tomorrow's follow them, saying which day they are.
        assertTrue(content.upcoming.toString(), content.upcoming.last().time.startsWith("Thu"))
    }

    @Test
    fun theLearningLineIsOnlyForTracksSwitchedOn() {
        assertEquals(null, content(fridayAfternoon).learning)
        assertNotNull(content(fridayAfternoon, learning = DailyLearningType.Default).learning)
    }

    @Test
    fun narrowColumnsGetTheShortNames() {
        val morning = requireNotNull(sunsetForDate(date = friday)).minus(Duration.ofHours(12))

        val shema = content(morning).zmanim.first { it.title == "Sof Zman Shema (GRA)" }

        assertEquals("Shema GRA", shema.shortTitle)
    }
}
