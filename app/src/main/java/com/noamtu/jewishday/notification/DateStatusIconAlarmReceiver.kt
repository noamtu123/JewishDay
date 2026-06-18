package com.noamtu.jewishday.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires at the next date boundary and tells the service to recompute the date icons. */
class DateStatusIconAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ActionRefresh) {
            DateStatusIconService.start(context.applicationContext)
        }
    }

    companion object {
        const val ActionRefresh = "com.noamtu.jewishday.notification.ALARM_REFRESH_DATE_STATUS_ICONS"
    }
}
