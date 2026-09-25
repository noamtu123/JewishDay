// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Arms the single alarm that keeps the home-screen widget current. One alarm at a time: each
 * refresh computes the next [DayWidgetRefresh] and re-arms, so the chain follows the day.
 *
 * A content boundary asks for an exact, idle-tolerant alarm, so the Hebrew date flips at tzeit
 * rather than whenever Doze next lets a batched alarm through; a sky tick asks for an inexact one
 * and lets the system fold it in with whatever else it wakes for.
 */
@Singleton
class DayWidgetAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) {
    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(refresh: DayWidgetRefresh) {
        val pendingIntent = alarmPendingIntent()
        val triggerMillis = triggerMillisFor(refresh.at)
        try {
            if (refresh.exact && canScheduleExact()) {
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

    /**
     * [at] was read off the app's clock, which the developer tools can pin or shift, while
     * AlarmManager keeps the device's own time. Carrying the *delay* across rather than the instant
     * keeps a virtual boundary from landing in the real past — an alarm there fires at once, and
     * every refresh would arm the next one straight away.
     */
    private fun triggerMillisFor(at: Instant): Long {
        val delayMillis = Duration.between(clock.instant(), at).toMillis().coerceAtLeast(0L)
        return System.currentTimeMillis() + delayMillis
    }

    private fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun alarmPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        AlarmRequestCode,
        Intent(context, DayWidgetAlarmReceiver::class.java).apply {
            action = DayWidgetAlarmReceiver.ActionRefresh
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val AlarmRequestCode = 1120
    }
}
