// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

private const val GERESH = '׳' // ׳ — marks a single-letter numeral
private const val GERSHAYIM = '״' // ״ — marks a multi-letter numeral

/**
 * Gematria letters for a positive number, without punctuation (e.g. 161 -> "קסא", 15 -> "טו").
 * Falls back to the decimal string for non-positive input.
 */
internal fun gematriaLetters(value: Int): String {
    if (value <= 0) return value.toString()
    val sb = StringBuilder()
    var n = value
    while (n >= 400) {
        sb.append('ת')
        n -= 400
    }
    when (n / 100) {
        1 -> sb.append('ק')
        2 -> sb.append('ר')
        3 -> sb.append('ש')
    }
    n %= 100
    when (n) {
        15 -> return sb.append("טו").toString() // avoid spelling the Divine Name
        16 -> return sb.append("טז").toString()
    }
    when (n / 10) {
        1 -> sb.append('י')
        2 -> sb.append('כ')
        3 -> sb.append('ל')
        4 -> sb.append('מ')
        5 -> sb.append('נ')
        6 -> sb.append('ס')
        7 -> sb.append('ע')
        8 -> sb.append('פ')
        9 -> sb.append('צ')
    }
    when (n % 10) {
        1 -> sb.append('א')
        2 -> sb.append('ב')
        3 -> sb.append('ג')
        4 -> sb.append('ד')
        5 -> sb.append('ה')
        6 -> sb.append('ו')
        7 -> sb.append('ז')
        8 -> sb.append('ח')
        9 -> sb.append('ט')
    }
    return sb.toString()
}

/**
 * Gematria with standard punctuation: a lone letter gets a geresh (ג -> ג׳); multi-letter
 * numerals get a gershayim before the final letter (קסא -> קס״א, יח -> י״ח).
 */
internal fun hebrewNumber(value: Int): String {
    if (value <= 0) return value.toString()
    val letters = gematriaLetters(value)
    return if (letters.length == 1) {
        "$letters$GERESH"
    } else {
        letters.dropLast(1) + GERSHAYIM + letters.last()
    }
}

private val GematriaLetterValues = mapOf(
    'א' to 1, 'ב' to 2, 'ג' to 3, 'ד' to 4, 'ה' to 5, 'ו' to 6, 'ז' to 7, 'ח' to 8, 'ט' to 9,
    'י' to 10, 'כ' to 20, 'ל' to 30, 'מ' to 40, 'נ' to 50, 'ס' to 60, 'ע' to 70, 'פ' to 80, 'צ' to 90,
    'ק' to 100, 'ר' to 200, 'ש' to 300, 'ת' to 400,
)

/**
 * The value of a *canonical* gematria numeral ("לב" -> 32, "ע״ח" -> 78), ignoring any existing
 * punctuation, or null when the text is not one.
 *
 * The round-trip check is what makes this safe to point at arbitrary Hebrew text: an ordinary word
 * only sums to a number, it isn't *written* the canonical way (e.g. "ברכות" sums to 628, but 628 is
 * written "תרכח"), so tractate names and halacha titles are correctly rejected. Final letter forms
 * are absent from the table, which rejects most words outright.
 */
internal fun gematriaValue(text: String): Int? {
    val letters = text.filter { it != GERESH && it != GERSHAYIM }
    if (letters.isEmpty()) return null
    var total = 0
    for (character in letters) total += GematriaLetterValues[character] ?: return null
    return total.takeIf { it > 0 && gematriaLetters(it) == letters }
}

/**
 * Punctuates a bare numeral token, whether it arrives as Arabic digits or as unpunctuated Hebrew
 * letters ("2" -> "ב׳", "לב" -> "ל״ב"), and leaves an already-punctuated one unchanged
 * ("ע״ח" -> "ע״ח"). Null when the token is not a numeral at all.
 */
internal fun punctuatedNumeralOrNull(token: String): String? {
    token.toIntOrNull()?.let { return if (it > 0) hebrewNumber(it) else null }
    return gematriaValue(token)?.let(::hebrewNumber)
}

/** Replaces every run of Arabic digits in a string with its punctuated gematria. */
internal fun String.arabicDigitsToGematria(): String =
    Regex("\\d+").replace(this) { match -> hebrewNumber(match.value.toInt()) }

/** The last Arabic integer in a string, or null if there is none. */
internal fun String.lastIntegerOrNull(): Int? =
    Regex("\\d+").findAll(this).lastOrNull()?.value?.toIntOrNull()