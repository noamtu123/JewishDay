// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import com.noamtu.jewishday.di.ApplicationScope
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * A developer-only time of day for the Sky theme's backdrop alone. Unlike the developer clock it
 * moves nothing but the sky — the zmanim, the date and the icon stay on the real time — so the day
 * can be scrubbed and played through freely. Held in memory only: it is gone on the next launch.
 *
 * The player lives here, in the app's scope, so a day keeps playing while you leave the developer
 * tools to watch it behind the Zmanim or compass screen.
 */
@Singleton
class SkyPreview @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _time = MutableStateFlow<LocalTime?>(null)

    /** The time of day the sky shows, or null to follow the app's clock. */
    val time: StateFlow<LocalTime?> = _time.asStateFlow()

    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing.asStateFlow()

    private var player: Job? = null

    fun show(time: LocalTime) {
        stop()
        _time.value = time
    }

    /** Back to the app's clock. */
    fun clear() {
        stop()
        _time.value = null
    }

    /** Runs through a whole day in about half a minute, from wherever the preview stands. */
    fun play() {
        if (player?.isActive == true) return
        _playing.value = true
        player = scope.launch {
            var minute = _time.value?.let { it.hour * 60 + it.minute } ?: 0
            while (isActive) {
                _time.value = LocalTime.of(minute / 60, minute % 60)
                delay(StepMillis)
                minute = (minute + StepMinutes) % MinutesPerDay
            }
        }
    }

    fun stop() {
        player?.cancel()
        player = null
        _playing.value = false
    }

    private companion object {
        const val StepMinutes = 5
        const val StepMillis = 100L
        const val MinutesPerDay = 24 * 60
    }
}
