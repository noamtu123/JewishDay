// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.noamtu.jewishday.model.Festival
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The day widget's layout arithmetic is pure, so it is pinned here: what of the day each frame has
 * room for at the widget's reading sizes, in which order, and how the Hebrew date is set.
 */
class DayWidgetLayoutTest {
    private val fullDay = DayWidgetState(
        useHebrew = false,
        hebrewDate = "14 Tishrei 5787",
        gregorianDate = "Friday, September 25",
        festival = null,
        chip = "Sukkot",
        times = listOf(
            DayWidgetTime("Mincha", "12:30"),
            DayWidgetTime("Plag", "16:45"),
            DayWidgetTime("Sunset", "18:28"),
            DayWidgetTime("Tzeit", "18:50"),
        ),
        learning = "Daf Yomi Bavli: Sanhedrin 78",
        locationNote = null,
    )

    /** No location: the times are Jerusalem's, and the widget says so. */
    private val noLocation = "Jerusalem times · tap to update"

    private val quietDay = fullDay.copy(
        chip = null,
        times = fullDay.times.take(2),
        learning = null,
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
    fun aMediumWidgetPutsTheDaysNameAheadOfTheCivilDate() {
        val layout = dayWidgetLayoutFor(fullDay, mediumSize)

        // No room for the civil date's line: on a chag, its name says more. The learning line, smaller
        // still, fits in what is left.
        assertEquals(
            listOf(DayWidgetSlot.HebrewDate, DayWidgetSlot.Chip, DayWidgetSlot.Times, DayWidgetSlot.Learning),
            layout.slots,
        )
        assertEquals(listOf("Mincha", "Plag", "Sunset"), layout.times.map { it.label })
        assertEquals(TimeLabelSp, layout.timeLabelSp)
    }

    @Test
    fun aTallWidgetShowsTheWholeDay() {
        val layout = dayWidgetLayoutFor(fullDay.copy(locationNote = noLocation), largeSize)

        assertEquals(DayWidgetSlot.entries.toList(), layout.slots)
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

        // The wrapped label takes the room the chip had on the same frame, and "(tomorrow)" is too
        // wide a word for a third of the width even at the labels' smallest, so the row gives up a
        // column rather than break it.
        assertTrue(DayWidgetSlot.Chip in dayWidgetLayoutFor(fullDay, mediumSize).slots)
        assertFalse(DayWidgetSlot.Chip in long.slots)
        assertEquals(2, long.timesShown)
    }

    @Test
    fun aTimeLabelShrinksBeforeAWordOfItBreaks() {
        val layout = dayWidgetLayoutFor(fullDay.copy(times = listOf(DayWidgetTime("Tzeit Hakochavim", "19:01")) + fullDay.times), mediumSize)

        // Too long for one line of a third of the width at any size, so it wraps, a little smaller
        // than the labels' own size so that "Hakochavim" still fits its column whole.
        assertEquals(3, layout.timesShown)
        assertTrue(layout.timeLabelSp in MinTimeLabelSp until TimeLabelSp)
    }

    @Test
    fun aFourColumnWidgetSetsTheHebrewDateOnOneLineAtFullSize() {
        val layout = dayWidgetLayoutFor(hebrewDay, mediumSize)

        assertEquals(HebrewDateSp, layout.hebrewDateSp)
        assertEquals(1, layout.hebrewDateLines)
        assertEquals(0, layout.festivalIconDp)
    }

    @Test
    fun aFestivalsPictureTakesItsRoomFromTheHebrewDate() {
        val plain = dayWidgetLayoutFor(hebrewDay, mediumSize)
        val sukkot = dayWidgetLayoutFor(hebrewDay.copy(festival = Festival.Sukkot), mediumSize)

        assertEquals(FestivalIconDp, sukkot.festivalIconDp)
        assertTrue("${sukkot.hebrewDateSp} < ${plain.hebrewDateSp}", sukkot.hebrewDateSp < plain.hebrewDateSp)
        assertEquals(1, sukkot.hebrewDateLines)
    }

    @Test
    fun aFestivalsPictureShrinksWithASmallWidget() {
        val layout = dayWidgetLayoutFor(hebrewDay.copy(festival = Festival.Sukkot), DpSize(170.dp, 202.dp))

        assertTrue(layout.festivalIconDp in 1 until FestivalIconDp)
    }

    @Test
    fun aFrameTooNarrowForTheDateAndThePictureKeepsTheDate() {
        val tight = DpSize(130.dp, 150.dp)
        val layout = dayWidgetLayoutFor(hebrewDay.copy(festival = Festival.Sukkot), tight)

        assertEquals(0, layout.festivalIconDp)
        assertEquals(dayWidgetLayoutFor(hebrewDay, tight), layout)
    }

    @Test
    fun theTimeLabelsShrinkToSitOnOneLine() {
        val friday = hebrewDay.copy(
            times = listOf(
                DayWidgetTime("מנחה קטנה", "16:01"),
                DayWidgetTime("פלג המנחה", "17:17"),
                DayWidgetTime("הדלקת נרות", "18:02", pinned = true),
            ),
        )

        val layout = dayWidgetLayoutFor(friday, mediumSize)

        assertEquals(3, layout.timesShown)
        assertTrue(layout.timeLabelSp in MinTimeLabelSp until TimeLabelSp)
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
    fun withoutALocationTheWarningComesBeforeTheTimes() {
        // Where the frame has room for both, both show.
        assertEquals(
            listOf(DayWidgetSlot.HebrewDate, DayWidgetSlot.Chip, DayWidgetSlot.Times, DayWidgetSlot.Location),
            dayWidgetLayoutFor(fullDay.copy(locationNote = noLocation), mediumSize).slots,
        )
        // Where it has room for one, times shown unmarked for the wrong place would be worse than none.
        val narrow = dayWidgetLayoutFor(hebrewDay.copy(locationNote = noLocation), DpSize(170.dp, 202.dp))
        assertEquals(listOf(DayWidgetSlot.HebrewDate, DayWidgetSlot.Chip, DayWidgetSlot.Location), narrow.slots)
        assertEquals(0, narrow.timesShown)
    }

    @Test
    fun aQuietDayLeavesItsEmptySlotsOut() {
        val layout = dayWidgetLayoutFor(quietDay, DpSize(358.dp, 300.dp))

        assertEquals(listOf(DayWidgetSlot.HebrewDate, DayWidgetSlot.GregorianDate, DayWidgetSlot.Times), layout.slots)
        // Never more columns than there are times to put in them.
        assertEquals(2, layout.timesShown)
    }

    @Test
    fun aDayWithNothingStillToComeHasNoTimesRow() {
        val layout = dayWidgetLayoutFor(quietDay.copy(times = emptyList()), DpSize(358.dp, 300.dp))

        assertEquals(listOf(DayWidgetSlot.HebrewDate, DayWidgetSlot.GregorianDate), layout.slots)
        assertEquals(0, layout.timesShown)
    }

    @Test
    fun textWrapsAtWordsAndBreaksAWordWiderThanTheLine() {
        assertEquals(1, wrappedLines("a bb", charDp = 1f, lineWidthDp = 4f))
        assertEquals(2, wrappedLines("aaaa bbbb", charDp = 1f, lineWidthDp = 5f))
        assertEquals(3, wrappedLines("aaaaaaaaaa", charDp = 1f, lineWidthDp = 4f))
        assertEquals(1, wrappedLines("", charDp = 1f, lineWidthDp = 4f))
    }
}
