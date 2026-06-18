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

/** Replaces every run of Arabic digits in a string with its punctuated gematria. */
internal fun String.arabicDigitsToGematria(): String =
    Regex("\\d+").replace(this) { match -> hebrewNumber(match.value.toInt()) }

/** The last Arabic integer in a string, or null if there is none. */
internal fun String.lastIntegerOrNull(): Int? =
    Regex("\\d+").findAll(this).lastOrNull()?.value?.toIntOrNull()
