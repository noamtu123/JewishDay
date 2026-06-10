package com.noamtu.jewishday.data

import android.util.Log
import com.noamtu.jewishday.model.ZmanItem
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

interface DailyLearningRepository {
    fun learningItems(
        date: LocalDate,
        inIsrael: Boolean,
        includeRambamThreeChapters: Boolean,
    ): Flow<List<ZmanItem>>
}

@Singleton
class HebcalDailyLearningRepository @Inject constructor(
    private val cache: DailyLearningCache,
    private val client: HebcalDailyLearningClient,
    private val clock: Clock,
) : DailyLearningRepository {
    override fun learningItems(
        date: LocalDate,
        inIsrael: Boolean,
        includeRambamThreeChapters: Boolean,
    ): Flow<List<ZmanItem>> = flow {
        val cached = cache.read(date, inIsrael)
        if (cached.isNotEmpty()) {
            emit(cached.toZmanItems(includeRambamThreeChapters))
        }

        val now = clock.instant()
        if (cache.shouldRefresh(date = date, inIsrael = inIsrael, now = now)) {
            val endDate = date.plusDays(CacheWindowDays - 1)
            runCatching {
                client.fetchWindow(startDate = date, endDate = endDate, inIsrael = inIsrael)
            }.onSuccess { days ->
                cache.writeWindow(
                    startDate = date,
                    endDate = endDate,
                    inIsrael = inIsrael,
                    refreshedAt = now,
                    days = days,
                )
                val updated = cache.read(date, inIsrael)
                if (updated.isNotEmpty()) {
                    emit(updated.toZmanItems(includeRambamThreeChapters))
                }
            }.onFailure { exception ->
                if (exception is CancellationException) throw exception
                // Stale-but-present cache was already emitted above; just record why
                // the refresh failed instead of disappearing it.
                Log.w(TAG, "Hebcal daily learning refresh failed", exception)
            }
        }
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val CacheWindowDays = 7L
        const val TAG = "DailyLearning"
    }
}
