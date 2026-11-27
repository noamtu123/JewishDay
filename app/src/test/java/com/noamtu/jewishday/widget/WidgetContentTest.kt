// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import com.noamtu.jewishday.core.model.ZmanimCalculationSettings
import com.noamtu.jewishday.core.model.ZmanimTimeOption
import com.noamtu.jewishday.core.model.defaultJerusalemLocation
import com.noamtu.jewishday.core.model.nextZmanimRefreshBoundary
import com.noamtu.jewishday.core.model.sunsetForDate
import com.noamtu.jewishday.core.model.upcomingChatzotHaLaila
import com.noamtu.jewishday.core.model.zmanimDayFacts
import com.noamtu.jewishday.i18n.TestText
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
    ) = widgetContent(
        day = zmanimDayFacts(date = date, settings = settings, now = now),
        tomorrow = { zmanimDayFacts(date = date.plusDays(1), settings = settings, hebrewDateRolled = false) },
        text = if (hebrew) TestText.hebrew else TestText.english,
        use24HourTime = true,
        enabledZmanim = enabled,
        now = now,
        observanceBoundary = nextZmanimRefreshBoundary(defaultJerusalemLocation, settings, now),
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
        // Shabbat's own days go without saying, and the times stay bare so they fit.
        assertEquals(null, shabbat.entryDay)
        assertEquals(null, shabbat.exitDay)
        assertTrue(shabbat.exitTime, shabbat.exitTime!!.matches(Regex("\\d{2}:\\d{2}")))

        val hebrew = requireNotNull(content(fridayAfternoon, hebrew = true).shabbat)
        assertEquals("פרשת נצבים וילך", hebrew.title)
        assertEquals("כניסת שבת", hebrew.entryLabel)
    }

    @Test
    fun widgetsCallShemaKufShin() {
        val titles = content(fridayAfternoon, hebrew = true, enabled = ZmanimTimeOption.entries.toSet()).zmanim.map { it.title }

        assertTrue(titles.toString(), titles.contains("סוף זמן ק״ש (גר״א)"))
        assertFalse(titles.toString(), titles.any { "קריאת שמע" in it })
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
    fun aTapOpensTheShabbatWidgetOnShabbatItselfAndTheOthersOnTheirDay() {
        val saturday = friday.plusDays(1)
        // Announced (the header's card, from Friday) and not yet (the week's Shabbat section).
        val announced = content(fridayAfternoon)
        val tuesday = friday.minusDays(3)
        val earlyInTheWeek = content(tuesday.atTime(12, 0).atZone(defaultJerusalemLocation.zoneId).toInstant(), date = tuesday)

        assertTrue(requireNotNull(announced.shabbat).announced)
        assertEquals(saturday, announced.shabbat.day)
        assertFalse(requireNotNull(earlyInTheWeek.shabbat).announced)
        assertEquals(saturday, earlyInTheWeek.shabbat.day)
        assertEquals(friday, announced.date)
        assertEquals(tuesday, earlyInTheWeek.date)
    }

    @Test
    fun justAfterMidnightTheNextZmanIsTonightsChatzotHaLaila() {
        // In a Jerusalem summer chatzot halaila is about 00:45, so from 00:00 until then it is still ahead.
        val summerDay = LocalDate.of(2026, 7, 1)
        val now = summerDay.atTime(0, 10).atZone(defaultJerusalemLocation.zoneId).toInstant()
        val chatzot = requireNotNull(upcomingChatzotHaLaila(defaultJerusalemLocation, settings, now))
        assertEquals(summerDay, chatzot.atZone(defaultJerusalemLocation.zoneId).toLocalDate())

        val content = content(now, date = summerDay)
        val next = requireNotNull(content.next)
        assertEquals(chatzot, next.instant)
        assertEquals(0, content.nextIndex)
    }
}
