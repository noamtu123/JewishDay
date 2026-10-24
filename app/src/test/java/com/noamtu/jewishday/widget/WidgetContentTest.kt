// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import com.noamtu.jewishday.model.ZmanimCalculationSettings
import com.noamtu.jewishday.model.ZmanimTimeOption
import com.noamtu.jewishday.model.defaultJerusalemLocation
import com.noamtu.jewishday.model.nextZmanimRefreshBoundary
import com.noamtu.jewishday.model.sunsetForDate
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
    ) = widgetContent(
        day = zmanimForDate(date = date, settings = settings, now = now),
        tomorrow = { zmanimForDate(date = date.plusDays(1), settings = settings, hebrewDateRolled = false) },
        hebrew = hebrew,
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
}
