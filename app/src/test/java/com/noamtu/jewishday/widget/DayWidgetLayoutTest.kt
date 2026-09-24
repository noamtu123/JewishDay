// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The widget's layout arithmetic is pure, so it is pinned here: which tier each reference height
 * gets, and that the sky bitmap follows the widget's own pixels until the width cap, then shrinks in
 * step so the sky keeps its shape.
 */
class DayWidgetLayoutTest {
    @Test
    fun eachReferenceSizeGetsItsOwnTier() {
        assertEquals(DayWidgetTier.Small, dayWidgetTierFor(DayWidget.Small))
        assertEquals(DayWidgetTier.Medium, dayWidgetTierFor(DayWidget.Medium))
        assertEquals(DayWidgetTier.Large, dayWidgetTierFor(DayWidget.Large))
        // The height alone decides: a wide but short frame is still the strip.
        assertEquals(DayWidgetTier.Small, dayWidgetTierFor(DpSize(340.dp, 60.dp)))
        assertEquals(DayWidgetTier.Large, dayWidgetTierFor(DpSize(340.dp, 300.dp)))
    }

    @Test
    fun theSkyIsRenderedAtTheWidgetsOwnPixels() {
        assertEquals(IntSize(525, 263), skyBitmapSize(DayWidget.Medium, 2.625f))
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
