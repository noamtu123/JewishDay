// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

import java.time.Instant

/**
 * Which observance names the header chip when a fast and a holy day overlap. Both can be on screen
 * at once — the second day of Rosh Hashana already announces Tzom Gedalyah, and a fast can fall on
 * a Friday — so the one actually happening wins over the one merely announced. When both are under
 * way the one ending sooner wins; when neither has started, the one starting sooner.
 */
internal fun fastLeadsHeader(fast: FastDayInfo?, holy: HolyDayInfo?, now: Instant?): Boolean {
    if (fast == null) return false
    if (holy == null || now == null) return true

    val fastUnderWay = isUnderWay(fast.startTime, fast.endTime, now)
    val holyUnderWay = isUnderWay(holy.startTime, holy.endTime, now)
    return when {
        fastUnderWay != holyUnderWay -> fastUnderWay
        fastUnderWay -> !(fast.endTime ?: Instant.MAX).isAfter(holy.endTime ?: Instant.MAX)
        else -> !(fast.startTime ?: Instant.MAX).isAfter(holy.startTime ?: Instant.MAX)
    }
}

/** Whether [now] falls inside the window, so the observance is happening rather than announced. */
internal fun isUnderWay(start: Instant?, end: Instant?, now: Instant): Boolean =
    start != null && end != null && !now.isBefore(start) && now.isBefore(end)

internal fun hasBegun(start: Instant?, now: Instant): Boolean = start != null && !now.isBefore(start)
