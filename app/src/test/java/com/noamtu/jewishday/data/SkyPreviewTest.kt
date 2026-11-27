// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import java.time.LocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// advanceTimeBy and runCurrent drive the virtual clock; both are still marked experimental.
@OptIn(ExperimentalCoroutinesApi::class)
class SkyPreviewTest {
    // 288 steps of five minutes, 100 ms apiece.
    private val dayMillis = 28_800L

    private fun TestScope.preview() = SkyPreview(backgroundScope)

    @Test
    fun playRunsThroughOneDayThenFollowsTheClock() = runTest {
        val sky = preview()
        sky.play(now = LocalTime.of(6, 0, 42))
        runCurrent()
        assertTrue(sky.playing.value)
        assertEquals(LocalTime.of(6, 0), sky.time.value)

        advanceTimeBy(100)
        runCurrent()
        assertEquals(LocalTime.of(6, 5), sky.time.value)

        // Halfway round.
        advanceTimeBy(dayMillis / 2 - 100)
        runCurrent()
        assertEquals(LocalTime.of(18, 0), sky.time.value)

        advanceTimeBy(dayMillis / 2)
        runCurrent()
        assertNull(sky.time.value)
        assertFalse(sky.playing.value)
    }

    @Test
    fun playStartsFromTheShownTimeRatherThanNow() = runTest {
        val sky = preview()
        sky.show(LocalTime.of(20, 15))
        sky.play(now = LocalTime.of(6, 0))
        runCurrent()
        assertEquals(LocalTime.of(20, 15), sky.time.value)
    }

    @Test
    fun aPausedPlayHoldsItsTimeUntilTheToolsAreLeft() = runTest {
        val sky = preview()
        sky.play(now = LocalTime.of(6, 0))
        advanceTimeBy(dayMillis)
        sky.stop()
        val paused = sky.time.value
        advanceTimeBy(dayMillis)
        runCurrent()
        assertEquals(paused, sky.time.value)

        sky.developerToolsLeft()
        assertNull(sky.time.value)
    }

    @Test
    fun leavingTheToolsEndsAScrubbedOrPlainPreview() = runTest {
        val sky = preview()
        sky.show(LocalTime.of(10, 0))
        sky.developerToolsLeft()
        assertNull(sky.time.value)

        sky.play(now = LocalTime.of(10, 0))
        runCurrent()
        sky.developerToolsLeft()
        assertNull(sky.time.value)
        assertFalse(sky.playing.value)
        advanceTimeBy(dayMillis)
        runCurrent()
        assertNull(sky.time.value)
    }

    @Test
    fun aPlayForTheZmanimScreenOutlivesTheToolsButNotTheDay() = runTest {
        val sky = preview()
        sky.play(now = LocalTime.of(10, 0), outlivesTools = true)
        runCurrent()
        sky.developerToolsLeft()
        assertTrue(sky.playing.value)

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(LocalTime.of(10, 50), sky.time.value)

        advanceTimeBy(dayMillis)
        runCurrent()
        assertNull(sky.time.value)
        assertFalse(sky.playing.value)
    }

    @Test
    fun leavingTheAppEndsEveryPreview() = runTest {
        val sky = preview()
        sky.play(now = LocalTime.of(10, 0), outlivesTools = true)
        runCurrent()
        sky.appLeft()
        assertNull(sky.time.value)
        assertFalse(sky.playing.value)

        sky.show(LocalTime.of(3, 0))
        sky.appLeft()
        assertNull(sky.time.value)
    }
}
