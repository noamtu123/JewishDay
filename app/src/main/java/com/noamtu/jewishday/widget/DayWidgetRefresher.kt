// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.noamtu.jewishday.data.AppSettings
import com.noamtu.jewishday.data.AppSettingsRepository
import com.noamtu.jewishday.data.CurrentLocationRepository
import com.noamtu.jewishday.di.ApplicationScope
import com.noamtu.jewishday.model.JewishLocation
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps the home-screen widget current: re-renders every instance and arms the alarm for the next
 * moment the day or the sky changes, so the sun keeps moving and the date flips on time.
 *
 * Everything funnels through [refreshAll] under one lock, so however many triggers coincide — the
 * alarm, a settings change, the launcher — the widget is rendered once and the alarm armed once.
 * The alarm chain is self-repairing: every refresh arms the next, a failure arms a retry, and the
 * launcher's own updates re-arm it after a reboot.
 */
@Singleton
class DayWidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSettingsRepository: AppSettingsRepository,
    private val currentLocationRepository: CurrentLocationRepository,
    private val stateLoader: DayWidgetStateLoader,
    private val clock: Clock,
    private val alarmScheduler: DayWidgetAlarmScheduler,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val started = AtomicBoolean(false)
    private val refreshing = Mutex()

    /** The settings as the process found them: what the widget was last built with, until a render here says otherwise. */
    private val baselineSettings = AtomicReference<AppSettings?>(null)

    /** When a render was last asked for, on the elapsed-realtime clock; zero before the first. */
    private val lastRenderElapsedMillis = AtomicLong(0L)

    /**
     * Re-renders every widget instance and arms the alarm for the next refresh. With no instance on
     * any home screen there is nothing to keep current, so the alarm is cancelled instead.
     *
     * Glance keeps a widget's composition alive for some 45 seconds after a render, and an update
     * inside that window only recomposes it — provideGlance, and with it the day, is not run again.
     * A refresh landing in that window is therefore not rendered now, which would show nothing new,
     * but armed as an exact alarm for the window's end, so a settings change a moment after a render
     * still reaches the widget within the minute.
     */
    suspend fun refreshAll() {
        refreshing.withLock {
            try {
                if (!hasWidgets()) {
                    alarmScheduler.cancel()
                    return
                }
                val lastRender = lastRenderElapsedMillis.get()
                val sinceRender = SystemClock.elapsedRealtime() - lastRender
                if (lastRender != 0L && sinceRender < SessionGraceMillis) {
                    val at = clock.instant().plusMillis(SessionGraceMillis - sinceRender)
                    alarmScheduler.schedule(DayWidgetRefresh(at, exact = true))
                    return
                }
                lastRenderElapsedMillis.set(SystemClock.elapsedRealtime())
                DayWidget().updateAll(context)
                scheduleNext()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                Log.w(Tag, "Widget refresh failed; retrying in $RetryMinutes minutes", exception)
                scheduleRetry()
            }
        }
    }

    /** [refreshAll] for callers that cannot suspend; overlapping requests share one render. */
    fun requestRefresh() {
        scope.launch { refreshAll() }
    }

    /**
     * Arms the alarm without rendering, for when the launcher has just had Glance render every
     * instance itself — a placement, a resize, the APPWIDGET_UPDATE after a reboot. Rendering again
     * would draw the same sky twice; the alarm is the part that may have lapsed. That render is
     * noted too, so a settings change in the next minute is deferred past Glance's live composition
     * rather than swallowed by it.
     */
    fun armAlarm() {
        lastRenderElapsedMillis.set(SystemClock.elapsedRealtime())
        scope.launch {
            refreshing.withLock {
                try {
                    scheduleNext()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (exception: Exception) {
                    Log.w(Tag, "Could not arm the widget alarm; retrying in $RetryMinutes minutes", exception)
                    scheduleRetry()
                }
            }
        }
    }

    /** The last instance is gone: nothing left to keep current. */
    fun onWidgetsRemoved() {
        alarmScheduler.cancel()
    }

    /**
     * Follows the settings and the location for the life of the process, so a change of language,
     * clock format, calculation method, learning track or place re-renders the widget without waiting
     * for the next alarm. Idempotent: the Application calls it once, and a second call is a no-op.
     *
     * The first emission is the state as it stands and is skipped. Every later one is judged against
     * what the widget was last built from (see [wouldChangeTheWidget]) rather than against the
     * previous emission, so the process's own first fix, or one that refines the last by a few
     * metres, does not render the same widget twice.
     */
    @OptIn(FlowPreview::class)
    fun start() {
        if (!started.compareAndSet(false, true)) return
        combine(appSettingsRepository.settings, currentLocationRepository.currentLocation) { settings, location ->
            baselineSettings.compareAndSet(null, settings)
            RenderInputs(settings, location)
        }
            .drop(1)
            .debounce(ChangeDebounceMillis)
            .filter(::wouldChangeTheWidget)
            .onEach { refreshAll() }
            .launchIn(scope)
    }

    /**
     * Whether rendering with [inputs] would put something new on the widget, judged against what the
     * widget was last built from. The process's first fix is not a move: the render that woke the
     * process awaited that same fix or, with none to be had in the background, drew the spot the
     * repository remembers — which is where the phone still is. So only a change of settings, of
     * zone, or of place by enough to move a displayed time counts; losing the fix altogether (the app
     * told to stop using location) counts too, since the widget must fall back with it.
     */
    private fun wouldChangeTheWidget(inputs: RenderInputs): Boolean {
        val rendered = stateLoader.lastRendered
        val renderedSettings = rendered?.settings ?: baselineSettings.get()
        val renderedLocation = rendered?.location ?: currentLocationRepository.rememberedLocation()
        return inputs.settings != renderedSettings || !inputs.location.isSameSpotAs(renderedLocation)
    }

    private suspend fun scheduleNext() {
        val settings = appSettingsRepository.settings.first()
        val location = currentLocationRepository.currentLocationOrDefault()
        alarmScheduler.schedule(nextDayWidgetRefresh(location, settings.zmanimSettings, clock.instant()))
    }

    /** A failure keeps the last render up and tries again soon, so the widget is never stale for good. */
    private fun scheduleRetry() {
        alarmScheduler.schedule(DayWidgetRefresh(clock.instant().plus(RetryMinutes, ChronoUnit.MINUTES), exact = false))
    }

    /**
     * Asked of the framework rather than Glance: Glance learns which receiver hosts the widget only
     * from the receiver's own first onUpdate, asynchronously, so its list can read empty for a widget
     * that has just been placed — and cancelling the alarm on that would leave the new widget still.
     */
    private fun hasWidgets(): Boolean {
        val manager = context.getSystemService(AppWidgetManager::class.java) ?: return false
        return manager.getAppWidgetIds(ComponentName(context, DayWidgetReceiver::class.java)).isNotEmpty()
    }

    private data class RenderInputs(val settings: AppSettings, val location: JewishLocation?)

    private companion object {
        const val Tag = "DayWidgetRefresher"
        const val RetryMinutes = 15L
        const val ChangeDebounceMillis = 500L

        /** Glance's 45-second session, with room for the worker to start and the composition to land. */
        const val SessionGraceMillis = 60_000L
    }
}

/**
 * Whether two fixes would put the same times and sky on the widget: the same zone and near enough
 * that no time the widget shows moves by the minute it is written to. Every fix the device delivers
 * differs in its last decimals, so exact equality would call a stationary phone moved — and a phone
 * on the road would otherwise re-render the same widget once a minute for the whole trip.
 */
private fun JewishLocation?.isSameSpotAs(other: JewishLocation?): Boolean {
    if (this == null || other == null) return this == other
    return zoneId == other.zoneId &&
        abs(latitude - other.latitude) < SameSpotDegrees &&
        abs(longitude - other.longitude) < SameSpotDegrees
}

/** A fifth of a degree — some twenty kilometres — shifts a zman by well under the minute shown. */
private const val SameSpotDegrees = 0.2
