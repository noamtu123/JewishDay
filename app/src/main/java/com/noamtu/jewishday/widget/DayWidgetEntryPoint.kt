// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Hilt's door into the widget's data layer. A Glance widget and the receiver that hosts it are not
 * classes Hilt can inject into with @AndroidEntryPoint — provideGlance runs in a GlanceAppWidget,
 * which Hilt knows nothing about — so the loader is fetched from the singleton component by hand.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DayWidgetEntryPoint {
    fun stateLoader(): DayWidgetStateLoader
}

/** The app's [DayWidgetStateLoader], for the widget code that has only a [Context] to start from. */
fun dayWidgetStateLoader(context: Context): DayWidgetStateLoader =
    EntryPointAccessors.fromApplication(context.applicationContext, DayWidgetEntryPoint::class.java).stateLoader()
