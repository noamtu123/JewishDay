// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Whether [location] has, in the year from [from], at least one night that never gets dark enough
 * for the user's methods: no tzeit, no alot or no misheyakir by them, or no Magen Avraham sof zman
 * shema or tefillah. Those are the only places where [NoTzeitFallback] ever does anything, and so the
 * only ones whose settings offer it. Everyone else never sees the choice.
 *
 * A year of sun positions is a few hundred calculations a zman, so the answer is remembered per
 * place (to a tenth of a degree, a few kilometres) and the methods it depends on. It does not depend
 * on which year: the sun repeats.
 */
public fun hasNightThatStaysLight(
    location: JewishLocation,
    settings: ZmanimCalculationSettings,
    from: LocalDate,
): Boolean {
    // The stand-in itself is not part of the question; chatzot halaila is one that always exists, so
    // "has a stand-in" below means exactly "the user's own method gave nothing".
    val asked = settings.copy(noTzeitFallback = NoTzeitFallback.ChatzotHaLaila)
    val key = LightNightKey(
        latitudeTenths = (location.latitude * 10).roundToInt(),
        longitudeTenths = (location.longitude * 10).roundToInt(),
        zoneId = location.zoneId.id,
        methods = asked.lightNightMethods(),
    )
    synchronized(lightNightAnswers) { lightNightAnswers[key] }?.let { return it }
    val calendar = comprehensiveZmanimCalendar(location, from, asked)
    val answer = (0L until DaysAhead).any { days ->
        calendar.localDate = from.plusDays(days)
        calendar.ownTzeit(asked) == null ||
            calendar.alotFallback(asked) != null ||
            calendar.misheyakirFallback(asked) != null ||
            calendar.sofZmanShemaFallback(asked.sofZmanShemaMethod, asked) != null ||
            calendar.sofZmanTefillahFallback(asked.sofZmanTefillahMethod, asked) != null
    }
    synchronized(lightNightAnswers) {
        if (lightNightAnswers.size >= MaxRemembered) lightNightAnswers.remove(lightNightAnswers.keys.first())
        lightNightAnswers[key] = answer
    }
    return answer
}

/** The opinions [hasNightThatStaysLight] depends on: its answer changes only when these do. */
public fun ZmanimCalculationSettings.lightNightMethods(): List<Any> = listOf(
    tzeitHakochavimMethod, tzeitHakochavimCustom,
    alotHashacharMethod, alotHashacharCustom,
    misheyakirMethod, misheyakirCustom,
    sofZmanShemaMethod, sofZmanShemaCustom,
    sofZmanTefillahMethod, sofZmanTefillahCustom,
    sunriseMethod,
)

private data class LightNightKey(
    val latitudeTenths: Int,
    val longitudeTenths: Int,
    val zoneId: String,
    val methods: List<Any>,
)

private const val DaysAhead = 366L
private const val MaxRemembered = 16
private val lightNightAnswers = LinkedHashMap<LightNightKey, Boolean>()
