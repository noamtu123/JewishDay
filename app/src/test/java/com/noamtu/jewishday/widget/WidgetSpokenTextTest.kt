// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import com.noamtu.jewishday.core.model.ZmanimCalculationSettings
import com.noamtu.jewishday.core.model.ZmanimTimeOption
import com.noamtu.jewishday.core.model.defaultJerusalemLocation
import com.noamtu.jewishday.core.model.nextZmanimRefreshBoundary
import com.noamtu.jewishday.core.model.sunsetForDate
import com.noamtu.jewishday.core.model.zmanimDayFacts
import com.noamtu.jewishday.i18n.TestText
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** What TalkBack reads for each widget, on a fixed Friday afternoon in Jerusalem (2026-09-04). */
class WidgetSpokenTextTest {
    private val friday = LocalDate.of(2026, 9, 4)
    private val settings = ZmanimCalculationSettings()
    private val fridayAfternoon = requireNotNull(sunsetForDate(date = friday)).minus(Duration.ofHours(3))

    private fun content(now: Instant = fridayAfternoon, hebrew: Boolean = false) = widgetContent(
        day = zmanimDayFacts(date = friday, settings = settings, now = now),
        tomorrow = { zmanimDayFacts(date = friday.plusDays(1), settings = settings, hebrewDateRolled = false) },
        text = if (hebrew) TestText.hebrew else TestText.english,
        use24HourTime = true,
        enabledZmanim = ZmanimTimeOption.Default,
        now = now,
        observanceBoundary = nextZmanimRefreshBoundary(defaultJerusalemLocation, settings, now),
    )

    @Test
    fun theNextZmanWidgetReadsAsTwoSentences() {
        val content = content()
        val next = requireNotNull(content.next)
        val following = requireNotNull(content.following)

        assertEquals(
            "Next zman, ${next.title}, ${next.time}. Then, ${following.title}, ${following.time}",
            nextZmanSpoken(content),
        )
    }

    @Test
    fun theDateWidgetReadsBothDates() {
        val content = content()

        assertEquals(
            listOfNotNull(content.hebrewDate, content.specialDay, content.gregorianDate).joinToString(". "),
            hebrewDateSpoken(content),
        )
    }

    @Test
    fun theZmanimListNamesTheNextRow() {
        val content = content()
        val rows = visibleRows(content, capacity = 4)
        val spoken = zmanimSpoken(content, rows)
        val next = content.zmanim[requireNotNull(content.nextIndex)]

        assertTrue(spoken, spoken.startsWith(content.hebrewDate))
        assertTrue(spoken, spoken.contains("Next zman, ${next.title}, ${next.time}"))
        // Only the rows on screen.
        assertEquals(rows.size + 1, spoken.split(". ").size)
    }

    @Test
    fun theShabbatWidgetSaysWhenItComesInAndGoesOut() {
        val content = content()
        val shabbat = requireNotNull(content.shabbat)
        val spoken = requireNotNull(shabbatSpoken(content))

        assertTrue(spoken, spoken.startsWith(shabbat.title))
        assertTrue(spoken, spoken.contains("${shabbat.entryHeading}, ${shabbat.entryTime}"))
        assertTrue(spoken, spoken.contains("${shabbat.exitHeading}, ${shabbat.exitTime}"))
        assertFalse(spoken, spoken.contains("—"))
    }

    @Test
    fun hebrewWidgetsReadInHebrew() {
        val content = content(hebrew = true)
        val next = requireNotNull(content.next)

        assertTrue(nextZmanSpoken(content).startsWith("${content.nextLabel}, ${next.title}, ${next.time}"))
        assertTrue(glassDateSpoken(content, emptyList()).startsWith(content.hebrewDate))
    }
}
