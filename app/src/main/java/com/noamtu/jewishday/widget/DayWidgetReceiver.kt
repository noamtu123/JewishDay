// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * The AppWidgetProviders the launcher talks to for [DayWidget], one per size the widget picker
 * offers. They differ only in their provider info — the default size and the preview — since the
 * widget itself fits whatever frame it is given, so each is [DayWidget] under its own entry, and
 * every instance of every size is kept current by the one [DayWidgetRefresher].
 *
 * Glance turns the broadcasts each receives into runs of the widget's provideGlance. The app's own
 * refresh schedule — what keeps the sun moving between the launcher's sparse updates — hooks in here.
 */
abstract class DayWidgetSizeReceiver : GlanceAppWidgetReceiver() {
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

    /** The last widget of this size is gone; the refresher keeps going while any other size is placed. */
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        dayWidgetRefresher(context).onWidgetsRemoved()
    }
}

/** The small widget: the Hebrew date and the next time, two cells square. */
class DayWidgetSmallReceiver : DayWidgetSizeReceiver()

/**
 * The medium widget: the Hebrew date, the weekday and the times still to come, four cells by two.
 * It keeps the name the widget had when it came in one size, so widgets placed then stay placed.
 */
class DayWidgetReceiver : DayWidgetSizeReceiver()

/** The large widget: the whole day, four cells square. */
class DayWidgetLargeReceiver : DayWidgetSizeReceiver()

/** Every size's receiver, for asking the framework whether any widget is placed. */
internal val dayWidgetReceivers: List<Class<out DayWidgetSizeReceiver>> = listOf(
    DayWidgetSmallReceiver::class.java,
    DayWidgetReceiver::class.java,
    DayWidgetLargeReceiver::class.java,
)
