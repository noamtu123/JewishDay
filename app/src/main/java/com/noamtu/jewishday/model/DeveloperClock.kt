// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import com.noamtu.jewishday.data.DeveloperOverridesRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * A [Clock] that returns the developer-overridden virtual time when the hidden developer tools
 * pin one, and the real system time otherwise. Because a single Clock singleton feeds every
 * date/time consumer in the app, wrapping it here makes date/time overrides propagate everywhere
 * (zmanim, the date status icon, the date-boundary ticker, daily learning) with no other changes.
 */
class DeveloperClock(
    private val base: Clock,
    private val overrides: DeveloperOverridesRepository,
) : Clock() {
    override fun getZone(): ZoneId = base.zone

    override fun withZone(zone: ZoneId): Clock = DeveloperClock(base.withZone(zone), overrides)

    override fun instant(): Instant = overrides.snapshot().effectiveInstant(base.instant())

    override fun millis(): Long = instant().toEpochMilli()
}