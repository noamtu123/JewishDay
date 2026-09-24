// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.noamtu.jewishday.widget.dayWidgetRefresher

class DateStatusIconBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Exported receiver: only react to the system broadcasts we registered for.
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> {
                DateStatusIconScheduler.refresh(context.applicationContext)
                dayWidgetRefresher(context.applicationContext).requestRefresh()
            }
            // The widget's alarms are wall-clock, so a new zone moves every boundary; this broadcast
            // still reaches a manifest receiver when the process is dead. Only the widget is re-armed:
            // the date icon re-stamps the zone itself while alive (CurrentLocationRepository), and
            // starting its foreground service from here would flash a notification for every user
            // who never turned the icon on, only to stop again.
            Intent.ACTION_TIMEZONE_CHANGED -> dayWidgetRefresher(context.applicationContext).requestRefresh()
        }
    }
}
