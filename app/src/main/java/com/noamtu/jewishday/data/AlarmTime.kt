// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * When, on the phone's real clock, an alarm for [at] has to go off. [at] is an instant on the app's
 * [clock], which the developer tools can pin to another time ([DeveloperClock]); AlarmManager only
 * knows the real one. Handing it [at] itself set an alarm in the real past whenever the pinned time
 * was earlier than now: it went off at once, the refresh set the same alarm again, and so on without
 * end. What carries over is the wait, never the instant.
 *
 * The wait is at least [MinimumAlarmWait], so nothing that computes a boundary at or before now can
 * turn the alarm into a loop either.
 */
fun alarmTriggerMillis(clock: Clock, at: Instant, realNowMillis: Long = System.currentTimeMillis()): Long {
    // A frozen developer clock never reaches [at]: the wait would be re-armed at the same length
    // forever (every 30 s, frozen just short of a boundary, for an alarm that wakes the phone). Any
    // change to the override refreshes everything by itself, so an hourly fallback is all it needs.
    if ((clock as? DeveloperClock)?.isFrozen == true) return realNowMillis + FrozenClockAlarmWait.toMillis()
    return realNowMillis + Duration.between(clock.instant(), at).coerceAtLeast(MinimumAlarmWait).toMillis()
}

/** The wait while the developer tools hold the clock still: no boundary can come, so only a fallback. */
val FrozenClockAlarmWait: Duration = Duration.ofHours(1)

/** The shortest wait an alarm is set for. Every boundary the app wakes for is minutes apart at least. */
val MinimumAlarmWait: Duration = Duration.ofSeconds(30)
