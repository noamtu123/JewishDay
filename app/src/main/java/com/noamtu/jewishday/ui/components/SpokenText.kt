// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui.components

/**
 * What TalkBack reads for something shown as several separate texts — a zman row's name, time and
 * caption, the header's dates and chip, a widget — so that it is one stop that reads naturally
 * ("Sunrise, 6:42 AM, sea level") instead of a stop per text, or the texts in drawing order.
 *
 * The pieces are already worded in the app's language; this only joins them. Blank or missing
 * pieces are skipped, and a piece set on two lines (the merged Rambam bubble) reads as two clauses
 * rather than running its lines together.
 */
fun spokenText(vararg parts: String?): String = join(parts.asList(), ", ")

/** Like [spokenText], for pieces that are each a whole phrase: a widget's date, then its next zman. */
fun spokenSentences(vararg parts: String?): String = join(parts.asList(), ". ")

/** [spokenSentences] over a list. */
fun spokenSentences(parts: List<String?>): String = join(parts, ". ")

private fun join(parts: List<String?>, separator: String): String =
    parts.asSequence()
        .filterNotNull()
        .map { part -> part.lines().map(String::trim).filter(String::isNotEmpty).joinToString(", ") }
        .filter(String::isNotEmpty)
        .joinToString(separator)
