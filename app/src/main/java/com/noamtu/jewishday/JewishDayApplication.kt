// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday

import android.app.Application
import androidx.core.content.edit
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.noamtu.jewishday.widget.WidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class JewishDayApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    override val workManagerConfiguration: Configuration by lazy {
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        // The daily Hebrew-date notification was removed. Cancel any periodic work an
        // earlier version may have left scheduled so it stops firing after this update — once:
        // nothing schedules it any more, and starting WorkManager (its database and schedulers) on
        // every process start, each widget and icon alarm included, only to cancel nothing, is waste.
        val housekeeping = getSharedPreferences(HousekeepingPrefs, MODE_PRIVATE)
        if (!housekeeping.getBoolean(KeyRemovedWorkCancelled, false)) {
            // Marked done only once WorkManager has carried it out, so a process killed first tries again.
            WorkManager.getInstance(this).cancelUniqueWork(RemovedDailyNotificationWork).result.addListener(
                { housekeeping.edit { putBoolean(KeyRemovedWorkCancelled, true) } },
                Runnable::run,
            )
        }
        // Home-screen widgets follow the app's settings and location as they change.
        widgetUpdater.observeAppChanges()
    }

    private companion object {
        const val RemovedDailyNotificationWork = "daily_hebrew_date_notification"

        // Per-install, like every SharedPreferences file here (kept out of backup).
        const val HousekeepingPrefs = "housekeeping"
        const val KeyRemovedWorkCancelled = "removed_daily_notification_work_cancelled"
    }
}
