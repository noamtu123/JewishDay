// SPDX-License-Identifier: GPL-3.0-or-later

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
import com.noamtu.jewishday.model.nextSunset
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
 * Hosts the persistent Hebrew date status-bar icon as a foreground service so it survives the app
 * being swiped from recents. An exact alarm wakes the service at the next tzeit so the Hebrew date
 * flips immediately at the boundary.
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
        notifier.ensureChannel()
        startForegroundCompat(immediateNotification())
        scope.launch {
            refresh(
                requestedHebrew = intent.requestedShowHebrew(),
            )
        }
        return START_STICKY
    }

    /**
     * A notification that can be posted synchronously to satisfy the foreground-service
     * deadline: the last rendered icon if one is cached, or a placeholder. The async
     * [refresh] immediately follows and re-renders with the user's stored calculation
     * settings — computing the date here with default settings could briefly show (and
     * cache) the wrong Hebrew date around sunset for users on non-default sunset methods.
     */
    private fun immediateNotification(): Notification =
        notifier.cachedRender()?.let(notifier::buildNotification) ?: notifier.buildSyncingNotification()

    private suspend fun refresh(
        permissionRetryCount: Int = 0,
        requestedHebrew: Boolean? = null,
    ) {
        val settings = appSettingsRepository.settings.first()
        val showHebrew = requestedHebrew ?: settings.hebrewDateStatusIconEnabled
        if (!showHebrew) {
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
                refresh(
                    permissionRetryCount = permissionRetryCount + 1,
                    requestedHebrew = requestedHebrew,
                )
            } else {
                Log.w(TAG, "Notification permission still unavailable after retries; stopping date icons")
                stopIcons()
            }
            return
        }
        try {
            val location = currentLocationRepository.currentLocationOrDefault()
            val dayInfo = jewishDayRepository.getToday(location = location, settings = settings.zmanimSettings)
            val rendered = notifier.render(dayInfo)
            startForegroundCompat(notifier.buildNotification(rendered))
            notifier.cancel(DateStatusIconNotifier.SecondaryId)
            val now = clock.instant()
            alarmScheduler.scheduleNext(nextSunset(location, settings.zmanimSettings, now))
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
        private const val PermissionPropagationRetryMillis = 100L
        private const val PermissionPropagationMaxRetries = 50
        private const val ExtraShowHebrew = "com.noamtu.jewishday.notification.extra.SHOW_HEBREW"

        /** Starts (or refreshes) the persistent date icons. */
        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, DateStatusIconService::class.java),
            )
        }

        /** Starts the service with enough state to render the selected date icon immediately. */
        fun start(context: Context, showHebrew: Boolean) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, DateStatusIconService::class.java).apply {
                    putExtra(ExtraShowHebrew, showHebrew)
                },
            )
        }
    }

    private val Intent?.hasRequestedState: Boolean
        get() = this?.hasExtra(ExtraShowHebrew) == true

    private fun Intent?.requestedShowHebrew(): Boolean? =
        if (hasRequestedState) this?.getBooleanExtra(ExtraShowHebrew, false) else null
}