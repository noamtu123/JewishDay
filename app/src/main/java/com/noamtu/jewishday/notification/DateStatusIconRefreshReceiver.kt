// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DateStatusIconRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        DateStatusIconScheduler.refresh(context.applicationContext)
    }

    companion object {
        const val ActionRefreshAfterDismissal = "com.noamtu.jewishday.notification.REFRESH_DATE_STATUS_ICONS"
    }
}