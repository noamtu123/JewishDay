// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Whether [location] has, in the year from [from], at least one night on which the user's tzeit
 * method gives no time — the only places where [NoTzeitFallback] ever does anything, and so the
 * only ones whose settings offer it. Everyone else never sees the choice.
 *
 * A year of tzeit is a few hundred sun positions, so the answer is remembered per place (to a tenth
 * of a degree, a few kilometres) and tzeit method. It does not depend on which year: the sun repeats.
 */
public fun hasNightWithoutTzeit(
    location: JewishLocation,
    settings: ZmanimCalculationSettings,
    from: LocalDate,
): Boolean {
    val key = NoTzeitKey(
        latitudeTenths = (location.latitude * 10).roundToInt(),
        longitudeTenths = (location.longitude * 10).roundToInt(),
        zoneId = location.zoneId.id,
        method = settings.tzeitHakochavimMethod,
        custom = settings.tzeitHakochavimCustom,
    )
    synchronized(noTzeitAnswers) { noTzeitAnswers[key] }?.let { return it }
    val calendar = comprehensiveZmanimCalendar(location, from, settings)
    val answer = (0L until DaysAhead).any { days ->
        calendar.localDate = from.plusDays(days)
        calendar.ownTzeit(settings) == null
    }
    synchronized(noTzeitAnswers) {
        if (noTzeitAnswers.size >= MaxRemembered) noTzeitAnswers.remove(noTzeitAnswers.keys.first())
        noTzeitAnswers[key] = answer
    }
    return answer
}

private data class NoTzeitKey(
    val latitudeTenths: Int,
    val longitudeTenths: Int,
    val zoneId: String,
    val method: TzeitHakochavimMethod,
    val custom: CustomZmanValue,
)

private const val DaysAhead = 366L
private const val MaxRemembered = 16
private val noTzeitAnswers = LinkedHashMap<NoTzeitKey, Boolean>()
