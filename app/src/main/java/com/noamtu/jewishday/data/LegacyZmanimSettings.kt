// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import com.noamtu.jewishday.core.model.AlotHashacharMethod
import com.noamtu.jewishday.core.model.CandleLightingMethod
import com.noamtu.jewishday.core.model.ChametzMethod
import com.noamtu.jewishday.core.model.CustomZmanUnit
import com.noamtu.jewishday.core.model.LegacyLadderChoice
import com.noamtu.jewishday.core.model.MinchaGedolaMethod
import com.noamtu.jewishday.core.model.MinchaKetanaMethod
import com.noamtu.jewishday.core.model.MisheyakirMethod
import com.noamtu.jewishday.core.model.MotzeiShabbatMethod
import com.noamtu.jewishday.core.model.PlagHaminchaMethod
import com.noamtu.jewishday.core.model.RabbeinuTamMethod
import com.noamtu.jewishday.core.model.SofZmanShemaMethod
import com.noamtu.jewishday.core.model.SofZmanTefillahMethod
import com.noamtu.jewishday.core.model.TzeitHakochavimMethod
import com.noamtu.jewishday.core.model.legacyLadderChoice

// The opinions older builds stored that this one no longer offers by name — the fixed ladders and the
// options that were only a number — recovered as the custom option and value that give the same
// time. Read-only: nothing here is ever written back.

/**
 * A named option that was dropped because it was only a number in disguise, recovered as that number:
 * the Baal Hatanya's dawn is 16.9° and his nightfall 6°, Rabbeinu Tam's Bein Hashmashot 13.24° is
 * 13.24°, and Ateret Torah's nightfall was a fixed offset after sunset. Only the name is gone; the
 * time it produced is not.
 */
internal fun retiredOption(
    resolved: Any?,
    stored: String?,
    name: String,
    unit: CustomZmanUnit,
    value: Double,
): LegacyLadderChoice? =
    if (resolved != null || stored != name) null else LegacyLadderChoice(unit, value)

/** The ladder rung [stored] names, but only when this build did not recognise it as an option. */
internal fun Any?.orLadder(stored: String?): LegacyLadderChoice? =
    if (this != null) null else legacyLadderChoice(stored)

/** A bare minute count from a build that predates the method pickers entirely. */
internal fun minutesLadder(minutes: Int) = LegacyLadderChoice(CustomZmanUnit.Minutes, minutes.toDouble())

// Which custom option of each zman a recovered rung belongs to.

internal fun alotHashacharMethodFor(unit: CustomZmanUnit) = when (unit) {
    CustomZmanUnit.Degrees -> AlotHashacharMethod.CustomDegrees
    CustomZmanUnit.Minutes -> AlotHashacharMethod.CustomMinutes
    CustomZmanUnit.ZmaniyotMinutes -> AlotHashacharMethod.CustomZmaniyotMinutes
}

internal fun misheyakirMethodFor(unit: CustomZmanUnit) = when (unit) {
    CustomZmanUnit.Degrees -> MisheyakirMethod.CustomDegrees
    CustomZmanUnit.Minutes -> MisheyakirMethod.CustomMinutesBeforeSunrise
    CustomZmanUnit.ZmaniyotMinutes -> MisheyakirMethod.CustomZmaniyotMinutesBeforeSunrise
}

internal fun sofZmanShemaMethodFor(ladder: LegacyLadderChoice) = when {
    ladder.toFixedLocalChatzot && ladder.unit == CustomZmanUnit.Degrees ->
        SofZmanShemaMethod.CustomDegreesToFixedLocalChatzot
    ladder.toFixedLocalChatzot -> SofZmanShemaMethod.CustomMinutesToFixedLocalChatzot
    ladder.unit == CustomZmanUnit.Degrees -> SofZmanShemaMethod.CustomDegrees
    ladder.unit == CustomZmanUnit.Minutes -> SofZmanShemaMethod.CustomMinutes
    else -> SofZmanShemaMethod.CustomZmaniyotMinutes
}

internal fun sofZmanTefillahMethodFor(unit: CustomZmanUnit) = when (unit) {
    CustomZmanUnit.Degrees -> SofZmanTefillahMethod.CustomDegrees
    CustomZmanUnit.Minutes -> SofZmanTefillahMethod.CustomMinutes
    CustomZmanUnit.ZmaniyotMinutes -> SofZmanTefillahMethod.CustomZmaniyotMinutes
}

internal fun minchaGedolaMethodFor(unit: CustomZmanUnit) = when (unit) {
    CustomZmanUnit.Degrees -> MinchaGedolaMethod.CustomDegrees
    CustomZmanUnit.Minutes -> MinchaGedolaMethod.CustomMinutes
    CustomZmanUnit.ZmaniyotMinutes -> MinchaGedolaMethod.CustomZmaniyotMinutes
}

internal fun minchaKetanaMethodFor(unit: CustomZmanUnit) = when (unit) {
    CustomZmanUnit.Degrees -> MinchaKetanaMethod.CustomDegrees
    CustomZmanUnit.Minutes -> MinchaKetanaMethod.CustomMinutes
    CustomZmanUnit.ZmaniyotMinutes -> MinchaKetanaMethod.CustomZmaniyotMinutes
}

internal fun plagHaminchaMethodFor(unit: CustomZmanUnit) = when (unit) {
    CustomZmanUnit.Degrees -> PlagHaminchaMethod.CustomDegrees
    CustomZmanUnit.Minutes -> PlagHaminchaMethod.CustomMinutes
    CustomZmanUnit.ZmaniyotMinutes -> PlagHaminchaMethod.CustomZmaniyotMinutes
}

internal fun tzeitCustomMethodFor(unit: CustomZmanUnit) = when (unit) {
    CustomZmanUnit.Degrees -> TzeitHakochavimMethod.CustomDegrees
    CustomZmanUnit.Minutes -> TzeitHakochavimMethod.CustomMinutes
    CustomZmanUnit.ZmaniyotMinutes -> TzeitHakochavimMethod.CustomZmaniyotMinutes
}

internal fun motzeiCustomMethodFor(unit: CustomZmanUnit) = when (unit) {
    CustomZmanUnit.Degrees -> MotzeiShabbatMethod.CustomDegrees
    CustomZmanUnit.Minutes -> MotzeiShabbatMethod.CustomMinutes
    CustomZmanUnit.ZmaniyotMinutes -> MotzeiShabbatMethod.CustomZmaniyotMinutes
}

internal fun rabbeinuTamCustomMethodFor(unit: CustomZmanUnit) = when (unit) {
    CustomZmanUnit.Degrees -> RabbeinuTamMethod.CustomDegrees
    CustomZmanUnit.Minutes -> RabbeinuTamMethod.CustomMinutes
    CustomZmanUnit.ZmaniyotMinutes -> RabbeinuTamMethod.CustomZmaniyotMinutes
}

internal fun chametzCustomMethodFor(unit: CustomZmanUnit) = when (unit) {
    CustomZmanUnit.Degrees -> ChametzMethod.CustomDegrees
    CustomZmanUnit.Minutes -> ChametzMethod.CustomMinutes
    CustomZmanUnit.ZmaniyotMinutes -> ChametzMethod.CustomZmaniyotMinutes
}

/**
 * The fixed minhag options if the recovered offset is one of them, else the custom option
 * carrying it. Builds before the method picker existed stored nothing but a bare offset.
 */
internal fun legacyCandleMethod(minutes: Int?, default: CandleLightingMethod): CandleLightingMethod {
    if (minutes == null) return default
    return CandleLightingMethod.PromptOptions.firstOrNull { it.offsetMinutes == minutes }
        ?: CandleLightingMethod.Custom
}
