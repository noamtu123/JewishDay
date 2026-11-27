// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import com.noamtu.jewishday.testing.TestStores
import com.noamtu.jewishday.testing.awaitFirst
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The widget and date-icon alarms, set on the real clock whatever time the developer tools pin. */
class AlarmTimeTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val realNow = Instant.parse("2026-11-27T10:00:00Z")

    private fun overrides(virtual: Instant, frozen: Boolean) = DeveloperOverrides(
        timeOverrideEnabled = true,
        anchorRealEpochMs = realNow.minus(Duration.ofHours(1)).toEpochMilli(),
        anchorVirtualEpochMs = virtual.toEpochMilli(),
        timeFrozen = frozen,
    )

    /** The app's clock as the developer tools leave it, at the real [realNow]. */
    private fun appClock(overrides: DeveloperOverrides): Clock =
        Clock.fixed(overrides.effectiveInstant(realNow), ZoneOffset.UTC)

    @Test
    fun aPinnedPastTimeStillWaitsForItsBoundary() {
        for (frozen in listOf(true, false)) {
            val clock = appClock(overrides(Instant.parse("2025-03-01T08:00:00Z"), frozen))
            val nextZman = clock.instant().plus(Duration.ofMinutes(40))
            // Set at the boundary's own instant, this alarm was a year in the past: it went off at once,
            // and every refresh set it again.
            assertTrue(nextZman.toEpochMilli() < realNow.toEpochMilli())
            assertEquals(realNow.plus(Duration.ofMinutes(40)).toEpochMilli(), alarmTriggerMillis(clock, nextZman, realNow.toEpochMilli()))
        }
    }

    @Test
    fun aPinnedFutureTimeDoesNotPutTheAlarmOffForYears() {
        val clock = appClock(overrides(Instant.parse("2031-06-01T08:00:00Z"), frozen = true))
        val midnight = clock.instant().plus(Duration.ofHours(16))
        assertEquals(realNow.plus(Duration.ofHours(16)).toEpochMilli(), alarmTriggerMillis(clock, midnight, realNow.toEpochMilli()))
    }

    @Test
    fun theRealClockKeepsItsOwnInstants() {
        val clock = Clock.fixed(realNow, ZoneOffset.UTC)
        val tzeit = realNow.plus(Duration.ofHours(7))
        assertEquals(tzeit.toEpochMilli(), alarmTriggerMillis(clock, tzeit, realNow.toEpochMilli()))
    }

    @Test
    fun aBoundaryAtOrBeforeNowWaitsTheMinimum() {
        val clock = Clock.fixed(realNow, ZoneOffset.UTC)
        for (at in listOf(realNow, realNow.minusSeconds(5))) {
            assertEquals(realNow.plus(MinimumAlarmWait).toEpochMilli(), alarmTriggerMillis(clock, at, realNow.toEpochMilli()))
        }
    }

    @Test
    fun aFrozenClockArmsOnlyAnHourlyFallback() = runBlocking {
        TestStores(folder).use { stores ->
            val overrides = stores.developerOverrides
            overrides.setVirtualTime(Instant.parse("2026-11-27T14:00:00Z").toEpochMilli())
            overrides.setTimeFrozen(true)
            overrides.state.awaitFirst { it.timeOverrideEnabled && it.timeFrozen }
            val clock = DeveloperClock(Clock.fixed(realNow, ZoneOffset.UTC), overrides)
            // Frozen 20 seconds short of tzeit, which it can never reach: this used to wake the phone
            // every 30 seconds.
            val tzeit = clock.instant().plusSeconds(20)
            assertEquals(realNow.plus(FrozenClockAlarmWait).toEpochMilli(), alarmTriggerMillis(clock, tzeit, realNow.toEpochMilli()))
            // Flowing again, the boundary is waited for as usual.
            overrides.setTimeFrozen(false)
            overrides.state.awaitFirst { !it.timeFrozen }
            val flowing = alarmTriggerMillis(clock, clock.instant().plus(Duration.ofMinutes(10)), realNow.toEpochMilli())
            assertEquals(realNow.plus(Duration.ofMinutes(10)).toEpochMilli(), flowing)
        }
    }
}
