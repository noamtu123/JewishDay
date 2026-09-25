// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.noamtu.jewishday.ui.theme.SkyFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The widget's layout arithmetic is pure, so it is pinned here: what of the day each frame has room
 * for at the widget's reading sizes, in which order, how the Hebrew date is set, and that the sky
 * bitmap follows the widget's own pixels until the width cap, then shrinks in step so the sky keeps
 * its shape.
 */
class DayWidgetLayoutTest {
    private val sky = SkyFrame(
        base = Color.Black,
        blooms = listOf(Color.Black, Color.Black, Color.Black),
        bloomAlpha = 0f,
        stars = 0f,
        sun = 0f,
        moon = 0f,
        sunArc = 0f,
        dark = true,
    )

    private val fullDay = DayWidgetState(
        useHebrew = false,
        sky = sky,
        hebrewDate = "14 Tishrei 5787",
        weekdayAndDate = "Friday, September 25",
        chip = "Sukkot",
        observanceLines = listOf("Sukkot starts 18:10", "Sukkot ends 19:05"),
        eventLine = "Omer: 12",
        times = listOf(
            DayWidgetTime("Mincha", "12:30"),
            DayWidgetTime("Plag", "16:45"),
            DayWidgetTime("Sunset", "18:28"),
            DayWidgetTime("Tzeit", "18:50"),
        ),
        learning = "Daf Yomi Bavli: Sanhedrin 78",
        locationName = "Times based on Jerusalem",
    )

    private val quietDay = fullDay.copy(
        chip = null,
        observanceLines = emptyList(),
        eventLine = null,
        times = fullDay.times.take(2),
        learning = null,
        locationName = null,
    )

    /** A Hebrew-language day, for how the Hebrew date is set. */
    private val hebrewDay = fullDay.copy(useHebrew = true, hebrewDate = "י״ד תשרי תשפ״ז")

    // A four-by-two widget as a phone launcher sizes it, and the four-by-four above it.
    private val mediumSize = DpSize(358.dp, 202.dp)
    private val largeSize = DpSize(358.dp, 460.dp)

    @Test
    fun aStripKeepsOnlyTheHebrewDate() {
        val layout = dayWidgetLayoutFor(fullDay, DpSize(100.dp, 40.dp))

        assertEquals(listOf(DayWidgetSlot.HebrewDate), layout.slots)
        assertEquals(MinHebrewDateSp, layout.hebrewDateSp)
        assertEquals(1, layout.hebrewDateLines)
        assertEquals(0, layout.timesShown)
    }

    @Test
    fun aMediumWidgetShowsTheDateTheWeekdayAndTheNextTimes() {
        val layout = dayWidgetLayoutFor(fullDay, mediumSize)

        assertEquals(listOf(DayWidgetSlot.HebrewDate, DayWidgetSlot.WeekdayAndDate, DayWidgetSlot.Times), layout.slots)
        assertEquals(listOf("Mincha", "Plag", "Sunset"), layout.times.map { it.label })
    }

    @Test
    fun aTallWidgetShowsTheWholeDay() {
        val layout = dayWidgetLayoutFor(fullDay, largeSize)

        assertEquals(DayWidgetSlot.entries.toList(), layout.slots)
        assertEquals(2, layout.observanceLinesShown)
    }

    @Test
    fun theTimesRowWidensWithTheWidget() {
        val shown = listOf(150, 250, 340, 440).map { dayWidgetLayoutFor(fullDay, DpSize(it.dp, 400.dp)).timesShown }

        assertEquals(listOf(1, 2, 3, 4), shown)
    }

    @Test
    fun aNarrowWidgetDropsTheFillerTimesButNeverTheObservanceBoundary() {
        val friday = fullDay.copy(
            times = listOf(
                DayWidgetTime("Mincha", "15:10"),
                DayWidgetTime("Plag", "16:30"),
                DayWidgetTime("Candle Lighting", "17:50", pinned = true),
                DayWidgetTime("Sunset", "18:10"),
            ),
        )

        assertEquals(listOf("Mincha", "Candle Lighting"), dayWidgetLayoutFor(friday, DpSize(250.dp, 300.dp)).times.map { it.label })
        assertEquals(listOf("Candle Lighting"), dayWidgetLayoutFor(friday, DpSize(150.dp, 300.dp)).times.map { it.label })
    }

    @Test
    fun aLongLabelWrapsAndTheRowMakesRoomForIt() {
        val long = dayWidgetLayoutFor(
            fullDay.copy(times = listOf(DayWidgetTime("Candle Lighting (tomorrow)", "18:10")) + fullDay.times),
            mediumSize,
        )

        // The wrapped label takes the room the weekday had on the same frame, and "(tomorrow)" is
        // too wide a word for a third of the width even at the labels' smallest, so the row gives
        // up a column rather than break it.
        assertTrue(DayWidgetSlot.WeekdayAndDate in dayWidgetLayoutFor(fullDay, mediumSize).slots)
        assertFalse(DayWidgetSlot.WeekdayAndDate in long.slots)
        assertEquals(2, long.timesShown)
    }

    @Test
    fun aTimeLabelShrinksBeforeAWordOfItBreaks() {
        val layout = dayWidgetLayoutFor(fullDay.copy(times = listOf(DayWidgetTime("Plag Hamincha", "17:17")) + fullDay.times), mediumSize)

        assertEquals(3, layout.timesShown)
        assertTrue(layout.timeLabelSp in MinTimeLabelSp until TimeLabelSp)
    }

    @Test
    fun aWideWidgetSetsTheHebrewDateOnOneLineAtFullSize() {
        val layout = dayWidgetLayoutFor(hebrewDay, DpSize(440.dp, 202.dp))

        assertEquals(HebrewDateSp, layout.hebrewDateSp)
        assertEquals(1, layout.hebrewDateLines)
    }

    @Test
    fun aFourColumnWidgetShrinksTheHebrewDateALittleToKeepItOnOneLine() {
        val layout = dayWidgetLayoutFor(hebrewDay, mediumSize)

        assertEquals(41, layout.hebrewDateSp)
        assertEquals(1, layout.hebrewDateLines)
    }

    @Test
    fun aNarrowWidgetBreaksTheHebrewDateOntoTwoLargerLines() {
        val layout = dayWidgetLayoutFor(hebrewDay, DpSize(170.dp, 202.dp))

        assertEquals(2, layout.hebrewDateLines)
        assertEquals(32, layout.hebrewDateSp)
        // Still room for the next time under it.
        assertEquals(listOf(DayWidgetSlot.HebrewDate, DayWidgetSlot.Times), layout.slots)
    }

    @Test
    fun aLargerSystemFontLeavesRoomForLess() {
        val layout = dayWidgetLayoutFor(fullDay, mediumSize, fontScale = 1.3f)

        assertEquals(listOf(DayWidgetSlot.HebrewDate, DayWidgetSlot.Times), layout.slots)
        assertEquals(2, layout.timesShown)
    }

    @Test
    fun aQuietDayLeavesItsEmptySlotsOut() {
        val layout = dayWidgetLayoutFor(quietDay, DpSize(358.dp, 300.dp))

        assertEquals(listOf(DayWidgetSlot.HebrewDate, DayWidgetSlot.WeekdayAndDate, DayWidgetSlot.Times), layout.slots)
        // Never more columns than there are times to put in them.
        assertEquals(2, layout.timesShown)
        assertEquals(0, layout.observanceLinesShown)
    }

    @Test
    fun aDayWithNothingStillToComeHasNoTimesRow() {
        val layout = dayWidgetLayoutFor(quietDay.copy(times = emptyList()), DpSize(358.dp, 300.dp))

        assertEquals(listOf(DayWidgetSlot.HebrewDate, DayWidgetSlot.WeekdayAndDate), layout.slots)
        assertEquals(0, layout.timesShown)
    }

    @Test
    fun textWrapsAtWordsAndBreaksAWordWiderThanTheLine() {
        assertEquals(1, wrappedLines("a bb", charDp = 1f, lineWidthDp = 4f))
        assertEquals(2, wrappedLines("aaaa bbbb", charDp = 1f, lineWidthDp = 5f))
        assertEquals(3, wrappedLines("aaaaaaaaaa", charDp = 1f, lineWidthDp = 4f))
        assertEquals(1, wrappedLines("", charDp = 1f, lineWidthDp = 4f))
    }

    @Test
    fun theSkyIsRenderedAtTheWidgetsOwnPixels() {
        assertEquals(IntSize(525, 263), skyBitmapSize(DpSize(200.dp, 100.dp), 2.625f))
    }

    @Test
    fun aWideWidgetOnADenseScreenIsCappedInWidthAndScaledInHeightToMatch() {
        assertEquals(IntSize(720, 360), skyBitmapSize(DpSize(400.dp, 200.dp), 4f))
    }

    @Test
    fun aDegenerateSizeStillYieldsADrawableBitmap() {
        assertEquals(IntSize(1, 1), skyBitmapSize(DpSize(0.dp, 0.dp), 2f))
    }
}
