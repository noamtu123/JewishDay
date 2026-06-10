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
            -> DateStatusIconScheduler.enqueueRefresh(context.applicationContext)
        }
    }
}
