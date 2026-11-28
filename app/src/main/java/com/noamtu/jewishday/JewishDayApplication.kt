// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday

import android.app.Application
import com.noamtu.jewishday.widget.WidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class JewishDayApplication : Application() {
    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    override fun onCreate() {
        super.onCreate()
        // Home-screen widgets follow the app's settings and location as they change.
        widgetUpdater.observeAppChanges()
    }
}
