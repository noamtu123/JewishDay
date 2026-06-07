package com.turel.jewishdaynext.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DateStatusIconBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        DateStatusIconScheduler.enqueueRefresh(context.applicationContext)
    }
}
