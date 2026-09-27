// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.noamtu.jewishday.data.AppSettingsRepository
import com.noamtu.jewishday.data.CurrentLocationRepository
import com.noamtu.jewishday.data.DailyLearningCache
import com.noamtu.jewishday.data.DeveloperOverridesRepository
import com.noamtu.jewishday.data.JewishDayRepository
import com.noamtu.jewishday.data.toZmanItems
import com.noamtu.jewishday.di.ApplicationScope
import com.noamtu.jewishday.model.JewishLocation
import com.noamtu.jewishday.model.ZmanimCalculationSettings
import com.noamtu.jewishday.model.isInIsrael
import com.noamtu.jewishday.model.nextZmanimRefreshBoundary
import com.noamtu.jewishday.model.skyDayFor
import com.noamtu.jewishday.model.withDailyLearningItems
import com.noamtu.jewishday.model.zmanimForDate
import com.noamtu.jewishday.ui.theme.SkyFrame
import com.noamtu.jewishday.ui.theme.skyAt
import com.noamtu.jewishday.ui.theme.nextSkyRedraw
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps every home-screen widget current. One pass renders all of them from the same [WidgetContent]
 * and sets one alarm for the next moment anything on them changes — a zman passing, the Hebrew date
 * rolling at tzeit, a holy day coming in or going out.
 *
 * The alarm does not wake the phone: a widget nobody is looking at does not need to change, and the
 * alarm is delivered as soon as the screen comes back on, which is exactly when it does. While a glass
 * widget is on the home screen the alarm also comes when its sky has noticeably moved on
 * ([nextSkyRedraw]): every few minutes through dawn and dusk, rarely by day, never through the still
 * night.
 *
 * Only a widget whose picture changed is redrawn: a sky-only update leaves the plain widgets alone,
 * and one that changes nothing on screen draws nothing.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSettingsRepository: AppSettingsRepository,
    private val currentLocationRepository: CurrentLocationRepository,
    private val developerOverridesRepository: DeveloperOverridesRepository,
    private val jewishDayRepository: JewishDayRepository,
    private val dailyLearningCache: DailyLearningCache,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val mutex = Mutex()

    /** What each widget on screen was last drawn from, by widget id; empty after a process restart. */
    private val drawn = mutableMapOf<Int, DrawnFrom>()

    fun requestUpdate(): Job = scope.launch { update() }

    /**
     * Re-renders the widgets whenever something they show is changed in the app: the language,
     * the time format, which zmanim to show, how they are calculated, the location, or the developer
     * clock. The first value renders them too, so opening the app always leaves them fresh.
     */
    fun observeAppChanges() {
        scope.launch {
            combine(
                appSettingsRepository.settings.map { settings ->
                    listOf(
                        settings.language,
                        settings.use24HourTime,
                        settings.enabledZmanimTimes,
                        settings.enabledDailyLearning,
                        settings.zmanimSettings,
                    )
                },
                currentLocationRepository.currentLocation,
                developerOverridesRepository.state,
            ) { settings, location, overrides -> Triple(settings, location, overrides) }
                .distinctUntilChanged()
                .collect { update() }
        }
    }

    private suspend fun update() = mutex.withLock {
        val manager = AppWidgetManager.getInstance(context)
        val widgets = WidgetKind.entries.associateWith { kind ->
            manager.getAppWidgetIds(ComponentName(context, kind.provider))
        }
        if (widgets.values.all { it.isEmpty() }) {
            cancelRefresh()
            return@withLock
        }
        try {
            val settings = appSettingsRepository.settings.first()
            val zmanimSettings = settings.zmanimSettings
            val location = currentLocationRepository.currentLocationOrDefault()
            val now = clock.instant()
            // The widgets never go to the network: daily learning comes from what the app last
            // fetched for today, else the day keeps its offline rows.
            val day = jewishDayRepository.getZmanim(location, zmanimSettings).let { computed ->
                val cached = dailyLearningCache.read(computed.date, location.isInIsrael)
                if (cached.isEmpty()) computed else computed.withDailyLearningItems(cached.toZmanItems())
            }
            val content = widgetContent(
                day = day,
                tomorrow = { zmanimForDate(location, day.date.plusDays(1), zmanimSettings, null, false) },
                hebrew = settings.useHebrewInterface,
                use24HourTime = settings.use24HourTime,
                enabledZmanim = settings.enabledZmanimTimes,
                now = now,
                observanceBoundary = nextZmanimRefreshBoundary(location, zmanimSettings, now),
                enabledLearning = settings.enabledDailyLearning,
            )
            val skies = WidgetSkies(
                now = skyFor(location, zmanimSettings, now),
                // Until Shabbat or the chag comes in, its widget shows the sky it will come in under.
                shabbat = content.shabbat
                    ?.takeUnless { it.isUnderWay }
                    ?.entry
                    ?.let { entry -> skyFor(location, zmanimSettings, entry) }
                    ?: skyFor(location, zmanimSettings, now),
            )
            val placed = widgets.values.flatMap { it.asIterable() }.toSet()
            drawn.keys.retainAll(placed)
            val fontScale = context.resources.configuration.fontScale
            widgets.forEach { (kind, ids) ->
                ids.forEach { id ->
                    val options = manager.getAppWidgetOptions(id)
                    val from = DrawnFrom(
                        content = content,
                        sky = when {
                            kind == WidgetKind.GlassShabbat -> skies.shabbat
                            kind.glass -> skies.now
                            else -> null
                        },
                        size = listOf(
                            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
                            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT),
                            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT),
                        ),
                        fontScale = fontScale,
                    )
                    if (drawn[id] != from) {
                        manager.updateAppWidget(id, renderWidget(context, kind, content, skies, options))
                        drawn[id] = from
                    }
                }
            }
            val hasGlass = widgets.any { (kind, ids) -> kind.glass && ids.isNotEmpty() }
            val skyRedraw = nextSkyRedraw(now, skyDayFor(location, now.atZone(location.zoneId).toLocalDate(), zmanimSettings), location.zoneId)
            scheduleRefresh(if (hasGlass) minOf(content.refreshAt, skyRedraw) else content.refreshAt)
        } catch (exception: Exception) {
            // Leave the widgets as they are and try again soon, rather than blanking them.
            Log.w(TAG, "Widget update failed; retrying later", exception)
            scheduleRefresh(clock.instant().plus(RetryMinutes, ChronoUnit.MINUTES))
        }
    }

    /**
     * Everything a widget's picture is drawn from. Equal to what it was last drawn from means the
     * same picture: the content and sky are plain data, and the size and font scale are all that a
     * renderer reads from the widget's options and the configuration.
     */
    private data class DrawnFrom(val content: WidgetContent, val sky: SkyFrame?, val size: List<Int>, val fontScale: Float)

    private fun skyFor(location: JewishLocation, settings: ZmanimCalculationSettings, at: Instant): SkyFrame {
        val date = at.atZone(location.zoneId).toLocalDate()
        return skyAt(at, skyDayFor(location, date, settings), location.zoneId)
    }

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun scheduleRefresh(at: Instant) {
        val pendingIntent = refreshPendingIntent()
        val triggerMillis = at.toEpochMilli()
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExact(AlarmManager.RTC, triggerMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC, triggerMillis, pendingIntent)
            }
        } catch (securityException: SecurityException) {
            // Exact-alarm permission can be revoked at runtime; an inexact alarm is still fine here.
            alarmManager.set(AlarmManager.RTC, triggerMillis, pendingIntent)
        }
    }

    private fun cancelRefresh() {
        alarmManager.cancel(refreshPendingIntent())
    }

    private fun refreshPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        AlarmRequestCode,
        Intent(context, WidgetRefreshReceiver::class.java).apply {
            action = WidgetRefreshReceiver.ActionRefresh
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val TAG = "WidgetUpdater"
        const val RetryMinutes = 15L
        const val AlarmRequestCode = 1120
    }
}
