// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.i18n

import java.util.Locale

/**
 * Everything about a language that a string resource cannot hold: how it writes the calendar's own
 * names ([CalendarText]) and how its daily-learning references are trimmed to fit a row.
 *
 * A language needs one of these only if it writes those names differently from English. Any
 * language without its own falls back to [EnglishLanguageFormatter] — the modern transliteration —
 * while its string resources still come from its own translation folder. To add one, implement this
 * interface and return it from [forLocale].
 */
public interface LanguageFormatter {
    /** The calendar fact, worded in this language. */
    public fun format(text: CalendarText): String

    /**
     * A Rambam Yomi reference as its row shows it. Hebrew drops the "הלכות" every reference starts
     * with; the row has no room for a word that says nothing.
     */
    public fun rambamRowReference(reference: String): String = reference

    /**
     * The words for "chapter" in a Rambam Yomi reference, longest first, which a row too narrow for
     * the whole reference may drop ("שבת פרק כח" → "שבת כח").
     */
    public val rambamChapterWords: List<String>

    public companion object {
        /** The formatter for [locale]'s language: its own if it has one, English otherwise. */
        public fun forLocale(locale: Locale): LanguageFormatter = when (locale.language) {
            // "iw" is the code older Android versions (and Java) still report for Hebrew.
            "he", "iw" -> HebrewLanguageFormatter()
            else -> EnglishLanguageFormatter()
        }
    }
}
