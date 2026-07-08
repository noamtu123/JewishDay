// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules an exact, idle-tolerant alarm at the next date boundary so the status-bar icon
 * flips right at tzeit / midnight instead of whenever Doze next lets a deferred job run.
 */
@Singleton
class DateStatusIconAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleNext(triggerAt: Instant) {
        val pendingIntent = alarmPendingIntent()
        val triggerMillis = triggerAt.toEpochMilli()
        try {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } catch (securityException: SecurityException) {
            // Exact-alarm permission can be revoked at runtime; fall back to an inexact alarm.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    fun cancel() {
        alarmManager.cancel(alarmPendingIntent())
    }

    private fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun alarmPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        AlarmRequestCode,
        Intent(context, DateStatusIconAlarmReceiver::class.java).apply {
            action = DateStatusIconAlarmReceiver.ActionRefresh
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val AlarmRequestCode = 1110
    }
}