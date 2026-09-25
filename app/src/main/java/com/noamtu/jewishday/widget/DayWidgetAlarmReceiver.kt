// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fires when the widget's alarm comes due: re-renders every instance and arms the next alarm.
 *
 * The refresh suspends — Glance's manager and the settings store are both suspend APIs — so the
 * receiver goes async. What runs here is a settings read, a few date computations and Glance
 * enqueuing the render (the sky bitmap is drawn in Glance's own worker), well inside the ten
 * seconds a receiver has once it has called goAsync.
 */
class DayWidgetAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ActionRefresh) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                dayWidgetRefresher(context.applicationContext).refreshAll()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ActionRefresh = "com.noamtu.jewishday.widget.ALARM_REFRESH_DAY_WIDGET"
    }
}
