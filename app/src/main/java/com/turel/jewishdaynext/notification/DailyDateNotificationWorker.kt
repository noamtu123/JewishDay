package com.turel.jewishdaynext.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.turel.jewishdaynext.MainActivity
import com.turel.jewishdaynext.R
import com.turel.jewishdaynext.data.AppSettingsRepository
import com.turel.jewishdaynext.data.CurrentLocationRepository
import com.turel.jewishdaynext.data.JewishDayRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class DailyDateNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val appSettingsRepository: AppSettingsRepository,
    private val currentLocationRepository: CurrentLocationRepository,
    private val jewishDayRepository: JewishDayRepository,
) : CoroutineWorker(appContext, workerParameters) {
    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        if (!applicationContext.canPostNotifications()) return Result.success()

        val settings = appSettingsRepository.settings.first()
        if (!settings.dailyDateNotificationEnabled) return Result.success()

        ensureNotificationChannel()
        val dayInfo = jewishDayRepository.getToday(
            location = currentLocationRepository.currentLocationOrDefault(),
            settings = settings.zmanimSettings,
        )
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_jewishday)
            .setContentTitle(applicationContext.getString(R.string.notification_daily_title))
            .setContentText(dayInfo.hebrewDateHebrew)
            .setStyle(NotificationCompat.BigTextStyle().bigText(dayInfo.hebrewDateEnglish))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(false)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    private fun ensureNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            applicationContext.getString(R.string.notification_channel_jewish_date),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = applicationContext.getString(R.string.notification_channel_jewish_date_description)
        }
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun Context.canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val UNIQUE_WORK_NAME = "daily_hebrew_date_notification"
        private const val CHANNEL_ID = "jewish_date"
        private const val NOTIFICATION_ID = 1001
    }
}
