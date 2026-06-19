package com.noamtu.jewishday.notification

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.noamtu.jewishday.data.AppSettingsRepository
import com.noamtu.jewishday.data.CurrentLocationRepository
import com.noamtu.jewishday.data.JewishDayRepository
import com.noamtu.jewishday.model.nextGregorianMidnight
import com.noamtu.jewishday.model.nextTzeit
import dagger.hilt.android.AndroidEntryPoint
import java.time.Clock
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Hosts the persistent Hebrew/English date status-bar icons as a foreground service so they
 * survive the app being swiped from recents. An exact alarm (scheduled here) wakes the service
 * at the next tzeit / midnight so the date flips immediately at the boundary.
 */
@AndroidEntryPoint
class DateStatusIconService : Service() {
    @Inject lateinit var appSettingsRepository: AppSettingsRepository
    @Inject lateinit var currentLocationRepository: CurrentLocationRepository
    @Inject lateinit var jewishDayRepository: JewishDayRepository
    @Inject lateinit var notifier: DateStatusIconNotifier
    @Inject lateinit var alarmScheduler: DateStatusIconAlarmScheduler
    @Inject lateinit var clock: Clock

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must enter the foreground within ~5s of startForegroundService(); post the cached
        // glyph immediately (no generic-icon flash) and recompute the real day in the background.
        notifier.ensureChannel()
        val cached = notifier.cachedRender()
        startForegroundCompat(
            cached?.primary?.let(notifier::buildNotification) ?: notifier.buildSyncingNotification(),
        )
        cached?.secondary?.let(notifier::postSecondary)
        scope.launch { refresh() }
        return START_STICKY
    }

    private suspend fun refresh(permissionRetryCount: Int = 0) {
        val settings = appSettingsRepository.settings.first()
        val showHebrew = settings.hebrewDateStatusIconEnabled
        val showEnglish = settings.englishDateStatusIconEnabled
        if (!showHebrew && !showEnglish) {
            stopIcons()
            return
        }
        if (!notifier.canPostNotifications()) {
            // Permission was just granted but may not have propagated to the app process yet
            // (race between the OS permission dialog callback and the process permission cache).
            // Retry inside this foreground service instead of relying on AlarmManager for a
            // seconds-long in-process race. If permission is genuinely absent, give up cleanly.
            if (permissionRetryCount < PermissionPropagationMaxRetries) {
                Log.d(TAG, "Notification permission not yet visible; retrying in ${PermissionPropagationRetryMillis}ms")
                delay(PermissionPropagationRetryMillis)
                refresh(permissionRetryCount + 1)
            } else {
                Log.w(TAG, "Notification permission still unavailable after retries; stopping date icons")
                stopIcons()
            }
            return
        }
        try {
            val location = currentLocationRepository.awaitCurrentLocation()
            val dayInfo = jewishDayRepository.getToday(location = location, settings = settings.zmanimSettings)
            val rendered = notifier.render(dayInfo, showHebrew, showEnglish)
            startForegroundCompat(notifier.buildNotification(rendered.primary))
            if (rendered.secondary != null) {
                notifier.postSecondary(rendered.secondary)
            } else {
                notifier.cancel(DateStatusIconNotifier.SecondaryId)
            }
            val now = clock.instant()
            val next = when {
                showHebrew && showEnglish -> minOf(
                    nextTzeit(location, settings.zmanimSettings, now),
                    nextGregorianMidnight(location, now),
                )
                showHebrew -> nextTzeit(location, settings.zmanimSettings, now)
                else -> nextGregorianMidnight(location, now)
            }
            alarmScheduler.scheduleNext(next)
        } catch (exception: Exception) {
            // Keep the existing icon up and retry soon rather than disappearing it.
            Log.w(TAG, "Date status icon refresh failed; retrying later", exception)
            alarmScheduler.scheduleNext(clock.instant().plus(RetryMinutes, ChronoUnit.MINUTES))
        }
    }

    private fun stopIcons() {
        alarmScheduler.cancel()
        notifier.cancelAll()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                DateStatusIconNotifier.ForegroundId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(DateStatusIconNotifier.ForegroundId, notification)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "DateStatusIconService"
        private const val RetryMinutes = 15L
        private const val PermissionPropagationRetryMillis = 1_000L
        private const val PermissionPropagationMaxRetries = 5

        /** Starts (or refreshes) the persistent date icons. */
        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, DateStatusIconService::class.java),
            )
        }
    }
}
