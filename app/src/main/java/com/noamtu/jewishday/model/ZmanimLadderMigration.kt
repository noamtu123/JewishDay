// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

/**
 * One rung of a method ladder an older build offered — 90 fixed minutes, 19.848°, 72 zmaniyot —
 * read back as the unit and number behind it.
 */
data class LegacyLadderChoice(
    val unit: CustomZmanUnit,
    val value: Double,
    /** The rung measured its half-day to fixed-local chatzot, which is its own option now. */
    val toFixedLocalChatzot: Boolean = false,
)

/**
 * Reads a stored method value that no longer names an option, because the ladder it belonged to was
 * replaced by a typed-in value. Someone upgrading had picked a rung deliberately, and quietly
 * resetting them to the app's default would move every zman on their screen — so the rung is
 * recovered as the equivalent custom option and its number.
 *
 * Returns null for anything that is not one of those ladder values, including the options that
 * survived (they are resolved by `fromStorageValue` before this is ever reached) and any future
 * value this build has never heard of.
 */
fun legacyLadderChoice(stored: String?): LegacyLadderChoice? {
    if (stored.isNullOrEmpty()) return null
    var body = stored
    val toFixedLocalChatzot = body.endsWith(FixedLocalChatzotSuffix)
    if (toFixedLocalChatzot) body = body.dropLast(FixedLocalChatzotSuffix.length)
    val zmaniyotSuffix = body.endsWith(ZmanisSuffix)
    if (zmaniyotSuffix) body = body.dropLast(ZmanisSuffix.length)

    val parts = body.split('_')
    val family = parts.first()
    // "minutes_35_before_sunrise" spells out what it is measured from after the number, so only the
    // leading numeric parts belong to the value.
    val value = ladderNumber(parts.drop(1).takeWhile { part -> part.isNotEmpty() && part.all(Char::isDigit) })
        ?: return null
    val unit = when {
        zmaniyotSuffix || family == "zmanis" -> CustomZmanUnit.ZmaniyotMinutes
        family == "minutes" -> CustomZmanUnit.Minutes
        family == "degrees" || family == "geonim" -> CustomZmanUnit.Degrees
        // The Magen Avraham rungs gave no unit in their name: "mga_72" was 72 minutes, "mga_26" was
        // 26 degrees. No ladder ever held a degree value as high as 50, nor a minute count below it,
        // so the number itself settles which one is meant.
        family == "mga" -> if (value >= LadderMinutesFloor) CustomZmanUnit.Minutes else CustomZmanUnit.Degrees
        else -> return null
    }
    return LegacyLadderChoice(unit, value, toFixedLocalChatzot)
}

/** "16" and "013" were how "16.013" was spelled in a preference key. */
private fun ladderNumber(digitParts: List<String>): Double? = when {
    digitParts.isEmpty() -> null
    digitParts.size == 1 -> digitParts.first().toDoubleOrNull()
    else -> "${digitParts.first()}.${digitParts.drop(1).joinToString("")}".toDoubleOrNull()
}

private const val FixedLocalChatzotSuffix = "_to_fixed_local_chatzot"
private const val ZmanisSuffix = "_zmanis"
private const val LadderMinutesFloor = 50.0
