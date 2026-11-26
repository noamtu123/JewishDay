// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

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
        }
    }
}
