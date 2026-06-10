package com.noamtu.jewishday.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.noamtu.jewishday.MainActivity
import com.noamtu.jewishday.R
import com.noamtu.jewishday.model.JewishDayInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DateStatusIconNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @SuppressLint("MissingPermission")
    fun show(
        dayInfo: JewishDayInfo,
        showHebrew: Boolean,
        showEnglish: Boolean,
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!context.canPostNotifications()) {
            notificationManager.cancel(HebrewNotificationId)
            notificationManager.cancel(EnglishNotificationId)
            return
        }

        ensureNotificationChannel(notificationManager)
        if (showHebrew) {
            notificationManager.notify(
                HebrewNotificationId,
                buildDateNotification(
                    iconText = dayInfo.hebrewDayOfMonthHebrew,
                    title = context.getString(R.string.notification_status_hebrew_title),
                    content = dayInfo.hebrewDateHebrew,
                    requestCode = HebrewNotificationId,
                ),
            )
        } else {
            notificationManager.cancel(HebrewNotificationId)
        }

        if (showEnglish) {
            val englishDate = dayInfo.gregorianDate.format(
                DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault()),
            )
            notificationManager.notify(
                EnglishNotificationId,
                buildDateNotification(
                    iconText = dayInfo.gregorianDate.dayOfMonth.toString(),
                    title = context.getString(R.string.notification_status_english_title),
                    content = englishDate,
                    requestCode = EnglishNotificationId,
                ),
            )
        } else {
            notificationManager.cancel(EnglishNotificationId)
        }
    }

    fun cancelAll() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(HebrewNotificationId)
        notificationManager.cancel(EnglishNotificationId)
    }

    private fun buildDateNotification(
        iconText: String,
        title: String,
        content: String,
        requestCode: Int,
    ): Notification {
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val deleteIntent = PendingIntent.getBroadcast(
            context,
            requestCode + DismissRequestCodeOffset,
            Intent(context, DateStatusIconRefreshReceiver::class.java).apply {
                action = DateStatusIconRefreshReceiver.ActionRefreshAfterDismissal
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(context, ChannelId)
            .setSmallIcon(Icon.createWithBitmap(dateIconBitmap(iconText)))
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(deleteIntent)
            .setCategory(Notification.CATEGORY_STATUS)
            .setLocalOnly(true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()
            .apply {
                flags = flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT
            }
    }

    private fun ensureNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            ChannelId,
            context.getString(R.string.notification_channel_date_status_icon),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_date_status_icon_description)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun dateIconBitmap(text: String): Bitmap {
        val size = 128
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            isLinearText = true
            isSubpixelText = true
            textSize = 118f
        }

        while (paint.measureText(text) > size * 0.98f || paint.textHeight() > size * 0.9f) {
            paint.textSize -= 1f
        }

        val baseline = size / 2f - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f
        canvas.drawText(text, size / 2f, baseline, paint)
        return bitmap
    }

    private fun Paint.textHeight(): Float = fontMetrics.descent - fontMetrics.ascent

    private fun Context.canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val ChannelId = "date_status_icon"
        const val HebrewNotificationId = 1101
        const val EnglishNotificationId = 1102
        private const val DismissRequestCodeOffset = 10_000
    }
}
