// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.content.edit
import com.noamtu.jewishday.data.AppSettingsRepository
import com.noamtu.jewishday.data.CurrentLocationRepository
import com.noamtu.jewishday.data.DeveloperOverridesRepository
import com.noamtu.jewishday.data.JewishDayRepository
import com.noamtu.jewishday.di.ApplicationScope
import com.noamtu.jewishday.model.JewishLocation
import com.noamtu.jewishday.model.SkyDay
import com.noamtu.jewishday.model.ZmanimCalculationSettings
import com.noamtu.jewishday.model.nextZmanimRefreshBoundary
import com.noamtu.jewishday.model.skyDayFor
import com.noamtu.jewishday.model.zmanimForDate
import com.noamtu.jewishday.ui.theme.SkyFrame
import com.noamtu.jewishday.ui.theme.skyAt
import com.noamtu.jewishday.ui.theme.nextSkyRedraw
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
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
        val placed = widgets.values.any { it.isNotEmpty() }
        if (!placed) cancelRefresh()
        try {
            val settings = appSettingsRepository.settings.first()
            val zmanimSettings = settings.zmanimSettings
            val location = currentLocationRepository.currentLocationOrDefault()
            val now = clock.instant()
            val today = now.atZone(location.zoneId).toLocalDate()
            // Each install (an update, or a test build over the same version) publishes them anew.
            val installed = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
            val previewsFor = "$installed|$today|${settings.language.storageValue}"
            val previewsDue = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM &&
                previewPreferences.getString(PreviewsForKey, null) != previewsFor
            if (!placed && !previewsDue) return@withLock
            val day = jewishDayRepository.getZmanim(location, zmanimSettings)
            fun contentAt(at: Instant) = widgetContent(
                day = day,
                tomorrow = { zmanimForDate(location, day.date.plusDays(1), zmanimSettings, null, false) },
                hebrew = settings.useHebrewInterface,
                use24HourTime = settings.use24HourTime,
                enabledZmanim = settings.enabledZmanimTimes,
                now = at,
                observanceBoundary = nextZmanimRefreshBoundary(location, zmanimSettings, at),
            )
            val content = contentAt(now)
            val skies = WidgetSkies(
                now = skyFor(location, zmanimSettings, now),
                // Until Shabbat or the chag comes in, its widget shows the sky it will come in under.
                shabbat = content.shabbat
                    ?.takeUnless { it.isUnderWay }
                    ?.entry
                    ?.let { entry -> skyFor(location, zmanimSettings, entry) }
                    ?: skyFor(location, zmanimSettings, now),
            )
            val skyDay = skyDayFor(location, today, zmanimSettings)
            if (previewsDue && Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                publishPreviews(manager, ::contentAt, skyDay, location, previewsFor)
            }
            if (!placed) return@withLock
            drawn.keys.retainAll(widgets.values.flatMap { it.asIterable() }.toSet())
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
                    )
                    if (drawn[id] != from) {
                        manager.updateAppWidget(id, renderWidget(context, kind, content, skies, options))
                        drawn[id] = from
                    }
                }
            }
            val hasGlass = widgets.any { (kind, ids) -> kind.glass && ids.isNotEmpty() }
            val skyRedraw = nextSkyRedraw(now, skyDay, location.zoneId)
            scheduleRefresh(if (hasGlass) minOf(content.refreshAt, skyRedraw) else content.refreshAt)
        } catch (exception: Exception) {
            // Leave the widgets as they are and try again soon, rather than blanking them.
            Log.w(TAG, "Widget update failed; retrying later", exception)
            if (placed) scheduleRefresh(clock.instant().plus(RetryMinutes, ChronoUnit.MINUTES))
        }
    }

    private val previewPreferences by lazy { context.getSharedPreferences(PreviewPreferences, Context.MODE_PRIVATE) }

    /**
     * On Android 15 and up the picker can show each widget as it really draws, rather than the
     * layout/widget_preview_* sketches older versions fall back to: today's date and times in the
     * app's language, as they stand on a morning of today whatever the hour — the sky by day, and
     * the list's past and coming times as they are under it — so a preview never shows a night sky.
     * Published once a day and after each install (the system rate-limits it), and again whenever it was refused.
     */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun publishPreviews(
        manager: AppWidgetManager,
        contentAt: (Instant) -> WidgetContent,
        skyDay: SkyDay,
        location: JewishLocation,
        previewsFor: String,
    ) {
        val sunrise = skyDay.sunrise
        val sunset = skyDay.sunset
        // A morning sun, a little short of its height, sits clear of the widgets' text.
        val morning = if (sunrise != null && sunset != null) {
            sunrise.plus(Duration.between(sunrise, sunset).multipliedBy(2).dividedBy(5))
        } else {
            LocalDate.now(clock.withZone(location.zoneId)).atTime(10, 0).atZone(location.zoneId).toInstant()
        }
        val daySky = skyAt(morning, skyDay, location.zoneId)
        val skies = WidgetSkies(now = daySky, shabbat = daySky)
        val content = contentAt(morning)
        val published = WidgetKind.entries.all { kind ->
            // Laid out for the size it is placed at; the zmanim lists stretch to whatever the
            // picker draws, so on a larger phone they fill it rather than leave a gap.
            val size = Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, kind.previewWidthDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, kind.previewHeightDp)
            }
            val preview = renderWidget(context, kind, content, skies, size)
            manager.setWidgetPreview(ComponentName(context, kind.provider), AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN, preview)
        }
        if (published) previewPreferences.edit { putString(PreviewsForKey, previewsFor) }
    }

    /**
     * Everything a widget's picture is drawn from. Equal to what it was last drawn from means the
     * same picture: the content and sky are plain data, and the size is all that a renderer reads
     * from the widget's options.
     */
    private data class DrawnFrom(val content: WidgetContent, val sky: SkyFrame?, val size: List<Int>)

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
        // Not backed up: what this install last published belongs to this install.
        const val PreviewPreferences = "widget_previews"
        const val PreviewsForKey = "published_for"
    }
}
