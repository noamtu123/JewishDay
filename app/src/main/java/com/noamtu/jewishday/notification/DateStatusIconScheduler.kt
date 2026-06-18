package com.noamtu.jewishday.notification

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns the persistent date icons on or off. When on, it (re)starts the foreground service
 * that owns the icons; when off, it tears down the service, its alarm, and its notifications.
 */
@Singleton
class DateStatusIconScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notifier: DateStatusIconNotifier,
    private val alarmScheduler: DateStatusIconAlarmScheduler,
) {
    fun sync(hebrewEnabled: Boolean, englishEnabled: Boolean) {
        if (hebrewEnabled || englishEnabled) {
            DateStatusIconService.start(context)
        } else {
            alarmScheduler.cancel()
            notifier.cancelAll()
            context.stopService(Intent(context, DateStatusIconService::class.java))
        }
    }

    companion object {
        /** Re-establishes the icons after a dismissal, an alarm, or a reboot. */
        fun refresh(context: Context) = DateStatusIconService.start(context)
    }
}
