// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.notification

import android.Manifest
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
import javax.inject.Inject
import javax.inject.Singleton

/** A single status-bar date icon: its notification id, the glyph, and the expanded text. */
data class DateIconSpec(
    val id: Int,
    val iconText: String,
    val title: String,
    val content: String,
)

@Singleton
class DateStatusIconNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val cache by lazy { context.getSharedPreferences(CachePrefs, Context.MODE_PRIVATE) }

    fun ensureChannel() {
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

    /**
     * Builds the icon spec for the current day and caches it so the foreground service can
     * re-post the right glyph instantly after a cold start, before recomputation finishes.
     */
    fun render(dayInfo: JewishDayInfo, useHebrew: Boolean): DateIconSpec {
        val titleRes = if (useHebrew) {
            R.string.notification_status_hebrew_title_hebrew
        } else {
            R.string.notification_status_hebrew_title
        }
        val rendered = DateIconSpec(
            id = ForegroundId,
            iconText = dayInfo.hebrewDayOfMonthHebrew,
            title = context.getString(titleRes),
            content = dayInfo.hebrewDateHebrew,
        )
        persist(rendered)
        return rendered
    }

    /** The last rendered icon, if any was saved, for an immediate post on cold start. */
    fun cachedRender(): DateIconSpec? {
        val primaryText = cache.getString(KeyPrimaryText, null) ?: return null
        return DateIconSpec(
            id = ForegroundId,
            iconText = primaryText,
            title = cache.getString(KeyPrimaryTitle, "").orEmpty(),
            content = cache.getString(KeyPrimaryContent, "").orEmpty(),
        )
    }

    fun buildNotification(spec: DateIconSpec): Notification {
        val pendingIntent = PendingIntent.getActivity(
            context,
            spec.id,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val deleteIntent = PendingIntent.getBroadcast(
            context,
            spec.id + DismissRequestCodeOffset,
            Intent(context, DateStatusIconRefreshReceiver::class.java).apply {
                action = DateStatusIconRefreshReceiver.ActionRefreshAfterDismissal
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(context, ChannelId)
            .setSmallIcon(Icon.createWithBitmap(dateIconBitmap(spec.iconText)))
            .setContentTitle(spec.title)
            .setContentText(spec.content)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(deleteIntent)
            .setCategory(Notification.CATEGORY_STATUS)
            .setLocalOnly(true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .applyForegroundServiceBehavior()
            .build()
            .apply {
                flags = flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT
            }
    }

    /** Minimal valid notification for the brief window before the real day is computed. */
    fun buildSyncingNotification(): Notification =
        Notification.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.ic_stat_jewishday)
            .setContentTitle(context.getString(R.string.notification_channel_date_status_icon))
            .setCategory(Notification.CATEGORY_STATUS)
            .setLocalOnly(true)
            .setOngoing(true)
            .setShowWhen(false)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .applyForegroundServiceBehavior()
            .build()

    private fun Notification.Builder.applyForegroundServiceBehavior(): Notification.Builder = apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
    }

    fun cancel(id: Int) = notificationManager.cancel(id)

    fun cancelAll() {
        notificationManager.cancel(ForegroundId)
        notificationManager.cancel(SecondaryId)
        cache.edit().clear().apply()
    }

    fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun persist(rendered: DateIconSpec) {
        cache.edit().apply {
            putString(KeyPrimaryText, rendered.iconText)
            putString(KeyPrimaryTitle, rendered.title)
            putString(KeyPrimaryContent, rendered.content)
            remove(KeySecondaryText)
            remove(KeySecondaryTitle)
            remove(KeySecondaryContent)
        }.apply()
    }

    private fun dateIconBitmap(text: String): Bitmap {
        val size = 128
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            // Medium weight + a hair of stroke makes the glyph read a touch bolder.
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 2.5f
            isLinearText = true
            isSubpixelText = true
            textSize = 126f
        }

        // Fill more of the canvas than before so the digit/letters render larger.
        while (paint.measureText(text) > size * 0.99f || paint.textHeight() > size * 0.97f) {
            paint.textSize -= 1f
        }

        val baseline = size / 2f - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f
        canvas.drawText(text, size / 2f, baseline, paint)
        return bitmap
    }

    private fun Paint.textHeight(): Float = fontMetrics.descent - fontMetrics.ascent

    companion object {
        const val ChannelId = "date_status_icon"
        const val ForegroundId = 1101
        const val SecondaryId = 1102
        private const val DismissRequestCodeOffset = 10_000
        private const val CachePrefs = "date_status_icon_cache"
        private const val KeyPrimaryText = "primary_text"
        private const val KeyPrimaryTitle = "primary_title"
        private const val KeyPrimaryContent = "primary_content"
        private const val KeySecondaryText = "secondary_text"
        private const val KeySecondaryTitle = "secondary_title"
        private const val KeySecondaryContent = "secondary_content"
    }
}