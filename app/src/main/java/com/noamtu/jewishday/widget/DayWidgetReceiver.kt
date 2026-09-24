// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * The AppWidgetProvider the launcher talks to for [DayWidget]: Glance turns the broadcasts it
 * receives into runs of the widget's provideGlance. The app's own refresh schedule — what keeps the
 * sun moving between the launcher's sparse updates — hooks in here separately.
 */
class DayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DayWidget()

    /**
     * A launcher-driven update — a placement, the update after a reboot, a locale change — already
     * has Glance render every instance in super, so only the alarm is armed here: a full refresh
     * would draw the same sky twice on every such update. The alarm is the part that may have
     * lapsed — alarms do not survive a reboot, and a widget placed for the first time has none yet.
     */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        dayWidgetRefresher(context).armAlarm()
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        dayWidgetRefresher(context).requestRefresh()
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        dayWidgetRefresher(context).onWidgetsRemoved()
    }
}
