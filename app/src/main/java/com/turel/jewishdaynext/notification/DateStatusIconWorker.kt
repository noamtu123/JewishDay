package com.turel.jewishdaynext.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.turel.jewishdaynext.data.AppSettings
import com.turel.jewishdaynext.data.AppSettingsRepository
import com.turel.jewishdaynext.data.JewishDayRepository
import com.turel.jewishdaynext.model.JewishLocation
import com.turel.jewishdaynext.model.ZmanimCalculationSettings
import com.turel.jewishdaynext.model.zmanimForDate
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.first

@HiltWorker
class DateStatusIconWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val appSettingsRepository: AppSettingsRepository,
    private val jewishDayRepository: JewishDayRepository,
    private val dateStatusIconNotifier: DateStatusIconNotifier,
    private val dateStatusIconScheduler: DateStatusIconScheduler,
    private val clock: Clock,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val settings = appSettingsRepository.settings.first()
        val showHebrew = settings.hebrewDateStatusIconEnabled
        val showEnglish = settings.englishDateStatusIconEnabled
        if (!showHebrew && !showEnglish) {
            dateStatusIconNotifier.cancelAll()
            return Result.success()
        }

        val dayInfo = jewishDayRepository.getToday(
            location = settings.selectedPlace.toJewishLocation(),
            settings = settings.zmanimSettings,
        )
        dateStatusIconNotifier.show(
            dayInfo = dayInfo,
            showHebrew = showHebrew,
            showEnglish = showEnglish,
        )
        dateStatusIconScheduler.scheduleNext(nextUpdateDelay(settings, clock.instant()))
        return Result.success()
    }

    private fun nextUpdateDelay(settings: AppSettings, now: Instant): Duration {
        val location = settings.selectedPlace.toJewishLocation()
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

    private fun nextGregorianMidnight(location: JewishLocation, now: Instant): Instant =
        now.atZone(location.zoneId)
            .toLocalDate()
            .plusDays(1)
            .atStartOfDay(location.zoneId)
            .plusMinutes(1)
            .toInstant()

    private fun nextTzeit(
        location: JewishLocation,
        settings: ZmanimCalculationSettings,
        now: Instant,
    ): Instant {
        val localToday = now.atZone(location.zoneId).toLocalDate()
        return listOf(localToday, localToday.plusDays(1), localToday.plusDays(2))
            .mapNotNull { date -> findTzeit(location, date, settings)?.plus(1, ChronoUnit.MINUTES) }
            .firstOrNull { it.isAfter(now) }
            ?: nextGregorianMidnight(location, now)
    }

    private fun findTzeit(
        location: JewishLocation,
        date: LocalDate,
        settings: ZmanimCalculationSettings,
    ): Instant? = zmanimForDate(location, date, settings)
        .groups
        .flatMap { it.items }
        .firstOrNull { it.title == "Tzeit" }
        ?.time
}
