// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday

import android.app.Application
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
        // earlier version may have left scheduled so it stops firing after this update.
        WorkManager.getInstance(this).cancelUniqueWork(RemovedDailyNotificationWork)
        // Home-screen widgets follow the app's settings and location as they change.
        widgetUpdater.observeAppChanges()
    }

    private companion object {
        const val RemovedDailyNotificationWork = "daily_hebrew_date_notification"
    }
}