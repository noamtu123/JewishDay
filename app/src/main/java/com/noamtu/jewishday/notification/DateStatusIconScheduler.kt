package com.noamtu.jewishday.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DateStatusIconScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dateStatusIconNotifier: DateStatusIconNotifier,
) {
    fun sync(hebrewEnabled: Boolean, englishEnabled: Boolean) {
        if (hebrewEnabled || englishEnabled) {
            WorkManager.getInstance(context).cancelUniqueWork(NextWorkName)
            refreshNow()
        } else {
            cancel()
        }
    }

    fun scheduleNext(delay: Duration) {
        val request = OneTimeWorkRequestBuilder<DateStatusIconWorker>()
            .setInitialDelay(delay.toMillis().coerceAtLeast(MinimumDelayMillis), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            NextWorkName,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(RefreshWorkName)
        WorkManager.getInstance(context).cancelUniqueWork(NextWorkName)
        dateStatusIconNotifier.cancelAll()
    }

    private fun refreshNow() = enqueueRefresh(context)

    companion object {
        fun enqueueRefresh(context: Context) {
            val request = OneTimeWorkRequestBuilder<DateStatusIconWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                RefreshWorkName,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        private const val RefreshWorkName = "date_status_icon_refresh"
        private const val NextWorkName = "date_status_icon_next"
        private const val MinimumDelayMillis = 60_000L
    }
}
