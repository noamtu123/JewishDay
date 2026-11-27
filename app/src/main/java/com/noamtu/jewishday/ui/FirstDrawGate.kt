// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Holds the window's first draw until the screen has what it needs (the sky, the day's zmanim), so
 * an opening app goes from the system splash straight to the real screen instead of flashing a
 * placeholder for a quarter of a second. Both arrive within a few frames; [timeoutMillis] is only a
 * safety net, after which whatever is ready is drawn. Once open it stays open.
 */
class FirstDrawGate(private val startedAt: Long, private val timeoutMillis: Long = 1_000) {
    private var holds = 0
    private var open = false

    fun hold() {
        holds++
    }

    fun release() {
        holds--
    }

    /** Whether a frame may be drawn at [now] (uptime millis). */
    fun mayDraw(now: Long): Boolean {
        if (!open && (holds <= 0 || now - startedAt >= timeoutMillis)) open = true
        return open
    }
}

val LocalFirstDrawGate = staticCompositionLocalOf<FirstDrawGate?> { null }

/** While [ready] is false, the app's first frame waits (see [FirstDrawGate]). */
@Composable
fun HoldFirstDraw(ready: Boolean) {
    val gate = LocalFirstDrawGate.current ?: return
    DisposableEffect(gate, ready) {
        if (!ready) gate.hold()
        onDispose { if (!ready) gate.release() }
    }
}
