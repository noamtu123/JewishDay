// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.notification

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns the persistent Hebrew date icon on or off. When on, it (re)starts the foreground service;
 * when off, it tears down the service, its alarm, and its notifications.
 */
interface DateStatusIconScheduler {
    /** Shows the icon (starting its service) when [enabled], or takes it down entirely when not. */
    fun sync(enabled: Boolean)

    companion object {
        /** Re-establishes the icons after a dismissal, an alarm, or a reboot. */
        fun refresh(context: Context) = DateStatusIconService.start(context)
    }
}

/** [DateStatusIconScheduler] over the foreground service, its alarm and its notifications. */
@Singleton
class ServiceDateStatusIconScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notifier: DateStatusIconNotifier,
    private val alarmScheduler: DateStatusIconAlarmScheduler,
) : DateStatusIconScheduler {
    override fun sync(enabled: Boolean) {
        if (enabled) {
            DateStatusIconService.start(context, showHebrew = true)
        } else {
            alarmScheduler.cancel()
            notifier.cancelAll()
            context.stopService(Intent(context, DateStatusIconService::class.java))
        }
    }
}
