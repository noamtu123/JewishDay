// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * The home-screen widgets the app offers, each with its own provider (and so its own picker entry).
 * The [glass] ones paint the Glass theme's live sky behind them. The names are the app's own, for its
 * "add widget" list; the picker shows the provider's bilingual label instead.
 */
internal enum class WidgetKind(
    val provider: Class<out AppWidgetProvider>,
    val labelEnglish: String,
    val labelHebrew: String,
    val glass: Boolean = false,
) {
    HebrewDate(HebrewDateWidget::class.java, "Hebrew date", "תאריך עברי"),
    NextZman(NextZmanWidget::class.java, "Next zman", "הזמן הבא"),
    Zmanim(ZmanimWidget::class.java, "Today's zmanim", "זמני היום"),
    Shabbat(ShabbatWidget::class.java, "Shabbat & chag", "שבת וחג"),
    GlassDate(GlassDateWidget::class.java, "Glassy sky · Hebrew date", "שמי זכוכית · תאריך עברי", glass = true),
    GlassNextZman(GlassNextZmanWidget::class.java, "Glassy sky · Next zman", "שמי זכוכית · הזמן הבא", glass = true),
    GlassZmanim(GlassZmanimWidget::class.java, "Glassy sky · Today's zmanim", "שמי זכוכית · זמני היום", glass = true),
    GlassShabbat(GlassShabbatWidget::class.java, "Glassy sky · Shabbat & chag", "שמי זכוכית · שבת וחג", glass = true),
    GlassDay(GlassDayWidget::class.java, "Glassy sky · The day (draft)", "שמי זכוכית · היום (טיוטה)", glass = true),
}

/**
 * Every widget renders from the same pass, so any provider being asked to update — a widget added,
 * resized, or the system's own periodic update — simply refreshes them all.
 */
abstract class JewishDayWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refreshWidgets(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        // A resize changes how many zmanim fit.
        refreshWidgets(context)
    }

    override fun onDisabled(context: Context) {
        // The last widget of this kind is gone; if it was the last of all, this drops the alarm.
        refreshWidgets(context)
    }
}

class HebrewDateWidget : JewishDayWidgetProvider()

class NextZmanWidget : JewishDayWidgetProvider()

class ZmanimWidget : JewishDayWidgetProvider()

class ShabbatWidget : JewishDayWidgetProvider()

class GlassDateWidget : JewishDayWidgetProvider()

class GlassNextZmanWidget : JewishDayWidgetProvider()

class GlassZmanimWidget : JewishDayWidgetProvider()

class GlassShabbatWidget : JewishDayWidgetProvider()

/** A draft: the whole day on the sky, fitting what it shows to the frame (DayWidgetLayout.kt). */
class GlassDayWidget : JewishDayWidgetProvider()

/** Refreshes the widgets at the alarm the updater sets, after a reboot or update, and when the clock or time zone changes. */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Exported for the system broadcasts: react only to those and to our own alarm.
        when (intent.action) {
            ActionRefresh,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> refreshWidgets(context)
        }
    }

    companion object {
        const val ActionRefresh = "com.noamtu.jewishday.widget.REFRESH_WIDGETS"
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun widgetUpdater(): WidgetUpdater
}

/** Updates off the main thread, keeping the receiver alive until the widgets are drawn. */
private fun BroadcastReceiver.refreshWidgets(context: Context) {
    val pendingResult = goAsync()
    EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        .widgetUpdater()
        .requestUpdate()
        .invokeOnCompletion { pendingResult.finish() }
}
