package com.noamtu.jewishday.notification

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.noamtu.jewishday.data.AppSettings
import com.noamtu.jewishday.data.AppSettingsRepository
import com.noamtu.jewishday.data.CurrentLocationRepository
import com.noamtu.jewishday.data.JewishDayRepository
import com.noamtu.jewishday.model.JewishLocation
import com.noamtu.jewishday.model.nextGregorianMidnight
import com.noamtu.jewishday.model.nextTzeit
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.first

@HiltWorker
class DateStatusIconWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val appSettingsRepository: AppSettingsRepository,
    private val currentLocationRepository: CurrentLocationRepository,
    private val jewishDayRepository: JewishDayRepository,
    private val dateStatusIconNotifier: DateStatusIconNotifier,
    private val dateStatusIconScheduler: DateStatusIconScheduler,
    private val clock: Clock,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result = try {
        refreshStatusIcon()
    } catch (exception: Exception) {
        // Keep the self-rescheduling chain alive: a one-off failure must not kill it.
        Log.w(TAG, "Status icon refresh failed, retrying", exception)
        Result.retry()
    }

    private suspend fun refreshStatusIcon(): Result {
        val settings = appSettingsRepository.settings.first()
        val showHebrew = settings.hebrewDateStatusIconEnabled
        val showEnglish = settings.englishDateStatusIconEnabled
        if (!showHebrew && !showEnglish) {
            dateStatusIconNotifier.cancelAll()
            return Result.success()
        }

        val location = currentLocationRepository.awaitCurrentLocation()
        val dayInfo = jewishDayRepository.getToday(
            location = location,
            settings = settings.zmanimSettings,
        )
        dateStatusIconNotifier.show(
            dayInfo = dayInfo,
            showHebrew = showHebrew,
            showEnglish = showEnglish,
        )
        dateStatusIconScheduler.scheduleNext(nextUpdateDelay(settings, location, clock.instant()))
        return Result.success()
    }

    private fun nextUpdateDelay(
        settings: AppSettings,
        location: JewishLocation,
        now: Instant,
    ): Duration {
        val candidates = buildList {
            if (settings.englishDateStatusIconEnabled) {
                add(nextGregorianMidnight(location, now))
            }
            if (settings.hebrewDateStatusIconEnabled) {
                add(nextTzeit(location, settings.zmanimSettings, now))
            }
        }
        val nextUpdate = candidates.minOrNull() ?: now.plus(1, ChronoUnit.DAYS)
        return Duration.between(now, nextUpdate)
    }

    private companion object {
        const val TAG = "DateStatusIconWorker"
    }
}
