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
 * The [glass] ones paint the Glass theme's live sky behind them; the rest are the minimal ones. The
 * names are the app's own, for its "add widget" list, which heads each group with [WidgetStyle] and so
 * leaves it out of the name; the picker shows the provider's bilingual label instead, which starts
 * with the group so the two sort apart there too.
 */
internal enum class WidgetKind(
    val provider: Class<out AppWidgetProvider>,
    val labelEnglish: String,
    val labelHebrew: String,
    // The size its picker preview is laid out for: its default cells on a small phone (a cell is
    // about 75dp wide and 100dp high there), so the preview is never fuller than the picker.
    val previewWidthDp: Int,
    val previewHeightDp: Int,
    val glass: Boolean = false,
) {
    GlassDate(GlassDateWidget::class.java, "Hebrew date", "תאריך עברי", 300, 200, glass = true),
    GlassNextZman(GlassNextZmanWidget::class.java, "Next zman", "הזמן הבא", 225, 200, glass = true),
    GlassZmanim(GlassZmanimWidget::class.java, "Today's zmanim", "זמני היום", 225, 330, glass = true),
    GlassShabbat(GlassShabbatWidget::class.java, "Shabbat & chag", "שבת וחג", 300, 200, glass = true),
    HebrewDate(HebrewDateWidget::class.java, "Hebrew date", "תאריך עברי", 150, 100),
    NextZman(NextZmanWidget::class.java, "Next zman", "הזמן הבא", 225, 100),
    Zmanim(ZmanimWidget::class.java, "Today's zmanim", "זמני היום", 225, 330),
    Shabbat(ShabbatWidget::class.java, "Shabbat & chag", "שבת וחג", 225, 100),
    ;

    val style: WidgetStyle get() = if (glass) WidgetStyle.Glass else WidgetStyle.Minimal
}

/** The two looks the widgets come in, in the order the "add widget" list shows them. */
internal enum class WidgetStyle(val labelEnglish: String, val labelHebrew: String) {
    Glass("Glassy sky", "שמי זכוכית"),
    Minimal("Minimal", "מינימליסטי"),
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
