// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.noamtu.jewishday.R
import com.noamtu.jewishday.data.CurrentLocationRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Takes one location fix for the widgets, started by tapping a widget's "Jerusalem times · tap to
 * update" note, then re-renders them and stops.
 *
 * Location is foreground-only for this app, so a widget refreshing in the background cannot take a
 * fix. A foreground service of type location that the user starts by tapping a widget is the one way
 * Android lets it anyway, without the app opening. It runs for the few seconds a fix takes; Android
 * holds its notification back for the first seconds of a service, so usually none is ever shown.
 */
@AndroidEntryPoint
class WidgetLocationService : Service() {
    @Inject lateinit var currentLocationRepository: CurrentLocationRepository
    @Inject lateinit var widgetUpdater: WidgetUpdater

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var work: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!startInForeground()) {
            stopSelf()
            return START_NOT_STICKY
        }
        // A second tap while a fix is on its way joins the first.
        if (work?.isActive != true) {
            work = scope.launch {
                try {
                    withTimeoutOrNull(FixTimeoutMillis) {
                        currentLocationRepository.refreshCurrentLocation(force = true)
                        currentLocationRepository.currentLocation.filterNotNull().first()
                    }
                    // The new fix re-renders the widgets by itself; waiting for it here keeps the
                    // process up until they are drawn. Without a fix (the location switch off) the
                    // widgets stay as they are.
                    widgetUpdater.requestUpdate().join()
                } finally {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startInForeground(): Boolean = try {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(ChannelId, getString(R.string.widget_location_channel), NotificationManager.IMPORTANCE_LOW)
                .apply { setShowBadge(false) },
        )
        val notification = Notification.Builder(this, ChannelId)
            .setSmallIcon(R.drawable.ic_stat_jewishday)
            .setContentTitle(getString(R.string.widget_location_updating))
            .setLocalOnly(true)
            .setShowWhen(false)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NotificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NotificationId, notification)
        }
        true
    } catch (exception: Exception) {
        // Location permission revoked since the widget was drawn: nothing to do but stop.
        Log.w(Tag, "Could not start the widget location update", exception)
        false
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val Tag = "WidgetLocationService"
        private const val ChannelId = "widget_location"
        private const val NotificationId = 1130
        private const val RequestCode = 1131

        // A cold GPS fix can take a while; past this the phone is not going to give one.
        private const val FixTimeoutMillis = 20_000L

        /** What tapping a widget's location note does: take a fix without opening the app. */
        fun pendingIntent(context: Context): PendingIntent = PendingIntent.getForegroundService(
            context,
            RequestCode,
            Intent(context, WidgetLocationService::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
