// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Hilt's door to the widget's refresher for the receivers that need it — the alarm, the launcher's
 * widget provider, the boot receiver — none of which Hilt can inject into directly.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DayWidgetRefreshEntryPoint {
    fun refresher(): DayWidgetRefresher
}

/** The app's [DayWidgetRefresher], for code that has only a [Context] to start from. */
fun dayWidgetRefresher(context: Context): DayWidgetRefresher =
    EntryPointAccessors.fromApplication(context.applicationContext, DayWidgetRefreshEntryPoint::class.java).refresher()
