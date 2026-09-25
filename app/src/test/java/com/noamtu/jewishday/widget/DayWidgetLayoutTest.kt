// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.noamtu.jewishday.ui.theme.SkyFrame
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The widget's layout arithmetic is pure, so it is pinned here: what of the day each frame has room
 * for at the widget's reading sizes, in which order, and that the sky bitmap follows the widget's own
 * pixels until the width cap, then shrinks in step so the sky keeps its shape.
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

    @Test
    fun aStripKeepsOnlyTheHebrewDate() {
        val layout = dayWidgetLayoutFor(fullDay, DpSize(100.dp, 40.dp))

        assertEquals(listOf(DayWidgetSlot.HebrewDate), layout.slots)
        assertEquals(NarrowHebrewDateSp, layout.hebrewDateSp)
        assertEquals(0, layout.timesShown)
    }

    @Test
    fun aLowWideWidgetPutsTheTimesBeforeTheWeekday() {
        val layout = dayWidgetLayoutFor(fullDay, DpSize(250.dp, 90.dp))

        assertEquals(listOf(DayWidgetSlot.HebrewDate, DayWidgetSlot.Times), layout.slots)
        assertEquals(HebrewDateSp, layout.hebrewDateSp)
        assertEquals(3, layout.timesShown)
    }

    @Test
    fun aTallWidgetShowsTheWholeDay() {
        val layout = dayWidgetLayoutFor(fullDay, DpSize(340.dp, 320.dp))

        assertEquals(DayWidgetSlot.entries.toList(), layout.slots)
        assertEquals(4, layout.timesShown)
        assertEquals(2, layout.observanceLinesShown)
    }

    @Test
    fun theTimesRowWidensWithTheWidget() {
        assertEquals(2, dayWidgetLayoutFor(fullDay, DpSize(150.dp, 300.dp)).timesShown)
        assertEquals(3, dayWidgetLayoutFor(fullDay, DpSize(250.dp, 300.dp)).timesShown)
        assertEquals(4, dayWidgetLayoutFor(fullDay, DpSize(340.dp, 300.dp)).timesShown)
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

        val layout = dayWidgetLayoutFor(friday, DpSize(150.dp, 300.dp))

        assertEquals(listOf("Mincha", "Candle Lighting"), layout.times.map { it.label })
    }

    @Test
    fun aLongLabelWrapsAndTheRowMakesRoomForIt() {
        val size = DpSize(250.dp, 110.dp)
        val short = dayWidgetLayoutFor(fullDay, size)
        val long = dayWidgetLayoutFor(
            fullDay.copy(times = listOf(DayWidgetTime("Candle Lighting (tomorrow)", "18:10")) + fullDay.times),
            size,
        )

        assertEquals(listOf(DayWidgetSlot.HebrewDate, DayWidgetSlot.WeekdayAndDate, DayWidgetSlot.Times), short.slots)
        // The wrapped label takes the line the weekday had.
        assertEquals(listOf(DayWidgetSlot.HebrewDate, DayWidgetSlot.Times), long.slots)
    }

    @Test
    fun aQuietDayLeavesItsEmptySlotsOut() {
        val layout = dayWidgetLayoutFor(quietDay, DpSize(340.dp, 200.dp))

        assertEquals(listOf(DayWidgetSlot.HebrewDate, DayWidgetSlot.WeekdayAndDate, DayWidgetSlot.Times), layout.slots)
        // Never more columns than there are times to put in them.
        assertEquals(2, layout.timesShown)
        assertEquals(0, layout.observanceLinesShown)
    }

    @Test
    fun aDayWithNothingStillToComeHasNoTimesRow() {
        val layout = dayWidgetLayoutFor(quietDay.copy(times = emptyList()), DpSize(340.dp, 200.dp))

        assertEquals(listOf(DayWidgetSlot.HebrewDate, DayWidgetSlot.WeekdayAndDate), layout.slots)
        assertEquals(0, layout.timesShown)
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
