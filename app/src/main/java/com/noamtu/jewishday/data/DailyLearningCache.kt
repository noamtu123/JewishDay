// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

interface DailyLearningCache {
    fun read(date: LocalDate, inIsrael: Boolean): List<HebcalLearningEntry>
    fun shouldRefresh(date: LocalDate, inIsrael: Boolean, now: Instant): Boolean
    fun writeWindow(
        startDate: LocalDate,
        endDate: LocalDate,
        inIsrael: Boolean,
        refreshedAt: Instant,
        days: Map<LocalDate, List<HebcalLearningEntry>>,
    )
}

@Singleton
class SharedPreferencesDailyLearningCache @Inject constructor(
    @ApplicationContext context: Context,
) : DailyLearningCache {
    private val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)

    override fun read(date: LocalDate, inIsrael: Boolean): List<HebcalLearningEntry> =
        preferences.getString(dayKey(date, inIsrael), null)
            ?.let(DailyLearningCacheCodec::decode)
            ?: emptyList()

    override fun shouldRefresh(date: LocalDate, inIsrael: Boolean, now: Instant): Boolean {
        if (read(date, inIsrael).isEmpty()) return true
        val windowEnd = preferences.getString(windowEndKey(inIsrael), null)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val refreshedAt = preferences.getLong(refreshedAtKey(inIsrael), 0L)
        return windowEnd == null ||
            date.plusDays(CacheWindowDays - 1).isAfter(windowEnd) ||
            refreshedAt <= 0L ||
            now.toEpochMilli() - refreshedAt > CacheMaxAge.toMillis()
    }

    override fun writeWindow(
        startDate: LocalDate,
        endDate: LocalDate,
        inIsrael: Boolean,
        refreshedAt: Instant,
        days: Map<LocalDate, List<HebcalLearningEntry>>,
    ) {
        val scopePrefix = "${scope(inIsrael)}:day:"
        // Drop day entries older than the new window so the prefs file doesn't grow forever.
        val staleDayKeys = preferences.all.keys.filter { key ->
            key.startsWith(scopePrefix) &&
                runCatching { LocalDate.parse(key.removePrefix(scopePrefix)) }
                    .getOrNull()
                    ?.isBefore(startDate) == true
        }
        preferences.edit().apply {
            staleDayKeys.forEach(::remove)
            putString(windowEndKey(inIsrael), endDate.toString())
            putLong(refreshedAtKey(inIsrael), refreshedAt.toEpochMilli())
            days.forEach { (date, entries) ->
                putString(dayKey(date, inIsrael), DailyLearningCacheCodec.encode(entries))
            }
        }.apply()
    }

    private fun dayKey(date: LocalDate, inIsrael: Boolean): String = "${scope(inIsrael)}:day:$date"
    private fun windowEndKey(inIsrael: Boolean): String = "${scope(inIsrael)}:window_end"
    private fun refreshedAtKey(inIsrael: Boolean): String = "${scope(inIsrael)}:refreshed_at"
    private fun scope(inIsrael: Boolean): String = if (inIsrael) "israel" else "diaspora"

    private companion object {
        const val PreferencesName = "daily_learning_cache"
        const val CacheWindowDays = 7L
        val CacheMaxAge: Duration = Duration.ofDays(7)
    }
}

internal object DailyLearningCacheCodec {
    fun encode(entries: List<HebcalLearningEntry>): String = entries.joinToString("\n") { entry ->
        listOf(
            entry.category.encodeField(),
            entry.title.encodeField(),
            entry.hebrew.encodeField(),
            entry.memo.encodeField(),
        ).joinToString("\t")
    }

    fun decode(value: String): List<HebcalLearningEntry> = value.lineSequence()
        .mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size != 4) return@mapNotNull null
            val category = parts[0].decodeField() ?: return@mapNotNull null
            val title = parts[1].decodeField() ?: return@mapNotNull null
            HebcalLearningEntry(
                category = category,
                title = title,
                hebrew = parts[2].decodeField(),
                memo = parts[3].decodeField(),
            )
        }
        .toList()

    private fun String?.encodeField(): String = this
        ?.let { Base64.getEncoder().encodeToString(it.toByteArray(Charsets.UTF_8)) }
        ?: ""

    // Base64.getDecoder() is strict and throws on malformed input. This is read inside the
    // daily-learning flow, so a corrupted preferences file would surface as an exception in the
    // ViewModel's scope rather than as a missing row. A damaged field simply reads as absent, and
    // decode() then drops the whole entry — the Hebcal refresh rewrites it on the next run.
    private fun String.decodeField(): String? = takeIf(String::isNotEmpty)
        ?.let { encoded ->
            runCatching { String(Base64.getDecoder().decode(encoded), Charsets.UTF_8) }.getOrNull()
        }
}