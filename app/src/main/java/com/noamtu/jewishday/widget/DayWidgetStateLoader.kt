// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import com.noamtu.jewishday.data.AppSettings
import com.noamtu.jewishday.data.AppSettingsRepository
import com.noamtu.jewishday.data.CurrentLocationRepository
import com.noamtu.jewishday.data.DailyLearningCache
import com.noamtu.jewishday.data.JewishDayRepository
import com.noamtu.jewishday.data.toZmanItems
import com.noamtu.jewishday.model.JewishLocation
import com.noamtu.jewishday.model.isInIsrael
import com.noamtu.jewishday.model.skyDayFor
import com.noamtu.jewishday.model.withDailyLearningItems
import com.noamtu.jewishday.ui.theme.skyAt
import java.time.Clock
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Gathers one snapshot of today for the home-screen widget: the user's settings, where they are,
 * the day computed against the app's clock, and the sky at this moment.
 *
 * The widget never goes to the network. Daily learning comes from the cache the app fills when its
 * own Zmanim tab is open; until then the day keeps its offline KosherJava rows. Israel versus
 * diaspora follows the location, exactly as the zmanim screen decides it for the same rows.
 */
@Singleton
class DayWidgetStateLoader @Inject constructor(
    private val jewishDayRepository: JewishDayRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val currentLocationRepository: CurrentLocationRepository,
    private val dailyLearningCache: DailyLearningCache,
    private val clock: Clock,
) {
    private val lastRenderedInputs = AtomicReference<RenderedInputs?>(null)

    /** What the last render in this process was built from, or null before the first. */
    val lastRendered: RenderedInputs? get() = lastRenderedInputs.get()

    suspend fun load(): DayWidgetState {
        val settings = appSettingsRepository.settings.first()
        // Waits for a fix rather than reading the in-memory state, which is empty after a process
        // restart — the usual situation when a widget update wakes the app. Bounded short: location is
        // foreground-only for this app, so a process with no screen showing is handed no fix at all,
        // and a long wait would only hold Glance's worker before the repository falls back to the fix
        // it remembers from the last time the app was open.
        val location = currentLocationRepository.awaitCurrentLocation(timeoutMillis = FixTimeoutMillis)
        lastRenderedInputs.set(RenderedInputs(settings, location))
        val computed = jewishDayRepository.getZmanim(location, settings.zmanimSettings)
        // Tomorrow as well, so that once today's times are all past the widget looks ahead rather
        // than standing empty through the night.
        val nextDay = jewishDayRepository.getZmanim(location, settings.zmanimSettings, dayOffset = 1)
        val cached = dailyLearningCache.read(computed.date, location.isInIsrael)
        val day = if (cached.isEmpty()) computed else computed.withDailyLearningItems(cached.toZmanItems())
        val now = clock.instant()
        val sky = skyAt(now, skyDayFor(location, day.date, settings.zmanimSettings), location.zoneId)
        return buildDayWidgetState(day, settings, sky, now, nextDay)
    }

    private companion object {
        const val FixTimeoutMillis = 2_000L
    }
}

/** The settings and location a render was built from, so a change can be judged against what is on screen. */
data class RenderedInputs(val settings: AppSettings, val location: JewishLocation)
