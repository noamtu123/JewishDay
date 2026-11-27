// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService

class DateStatusIconBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Exported receiver: only react to the system broadcasts we registered for.
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            // The phone's language, or the app's own one (Android 13+ sends this broadcast when it
            // changes, from the app or from the system settings): the icon's words follow it.
            Intent.ACTION_LOCALE_CHANGED,
            -> DateStatusIconScheduler.refresh(context.applicationContext)
            // A new time zone moves midnight and tzeit, and a clock set by hand can jump past one:
            // the alarm already set was worked out for the old ones. Only while the icon is up — the
            // clock is adjusted far more often than the phone boots, and starting the service just
            // to find the icon switched off would flash its notification each time.
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            -> if (iconIsShowing(context)) DateStatusIconScheduler.refresh(context.applicationContext)
        }
    }
}

/** Whether the date icon's notification is up — the icon is switched on and allowed. */
private fun iconIsShowing(context: Context): Boolean =
    context.getSystemService<NotificationManager>()
        ?.activeNotifications
        ?.any { it.id == DateStatusIconNotifier.ForegroundId } == true
