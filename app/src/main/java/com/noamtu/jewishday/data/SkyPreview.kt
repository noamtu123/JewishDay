// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import com.noamtu.jewishday.di.ApplicationScope
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A developer-only time of day for the Sky theme's backdrop alone. Unlike the developer clock it
 * moves nothing but the sky — the zmanim, the date and the icon stay on the real time — so the day
 * can be scrubbed and played through freely. Held in memory only.
 *
 * A preview is never left on by mistake: a play runs through one day and then hands the sky back
 * to the clock, leaving the developer tools ends any preview except a play started to be watched
 * on the Zmanim screen, and leaving the app ends every preview.
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

    // The scope runs on a thread pool, so the player's own ending and the UI's calls take turns.
    private val lock = Any()
    private var player: Job? = null

    /** Whether the play under way outlives the developer tools (it is watched on the Zmanim screen). */
    private var outlivesTools = false

    fun show(time: LocalTime) = synchronized(lock) {
        stopLocked()
        _time.value = time
    }

    /** Back to the app's clock. */
    fun clear() = synchronized(lock) {
        stopLocked()
        _time.value = null
    }

    /**
     * Runs once through a whole day in about half a minute, from wherever the preview stands (or
     * from [now] when there is none), then follows the clock again. With [outlivesTools] the play
     * goes on when the developer tools are left, so it can be watched behind another screen.
     */
    fun play(now: LocalTime, outlivesTools: Boolean = false) = synchronized(lock) {
        this.outlivesTools = this.outlivesTools || outlivesTools
        if (player != null) return@synchronized
        val start = (_time.value ?: now).truncatedTo(ChronoUnit.MINUTES)
        _playing.value = true
        lateinit var job: Job
        job = scope.launch {
            for (step in 0 until StepsPerDay) {
                _time.value = start.plusMinutes(step * StepMinutes)
                delay(StepMillis)
            }
            synchronized(lock) {
                // Unless it was paused or replaced meanwhile, the day is over: back to the clock.
                if (player === job) {
                    stopLocked()
                    _time.value = null
                }
            }
        }
        player = job
    }

    /** Pauses a play where it stands. */
    fun stop() = synchronized(lock) { stopLocked() }

    /** The developer tools were left: a preview ends there, unless it plays on for the Zmanim screen. */
    fun developerToolsLeft() = synchronized(lock) {
        if (player == null || !outlivesTools) clear()
    }

    /** The app went out of sight (another app, or the screen turned off): every preview ends. */
    fun appLeft() = clear()

    private fun stopLocked() {
        player?.cancel()
        player = null
        outlivesTools = false
        _playing.value = false
    }

    private companion object {
        const val StepMinutes = 5L
        const val StepMillis = 100L
        const val StepsPerDay = 24 * 60 / StepMinutes.toInt()
    }
}
