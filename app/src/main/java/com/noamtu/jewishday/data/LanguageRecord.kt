// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

/**
 * How the settings record the app's language: a BCP 47 tag ("fr", "pt-BR"), except English and
 * Hebrew, which keep the names every earlier version stored ("english", "hebrew") so that going back
 * to one — a tester leaving the pre-release channel — still reads them.
 *
 * The language itself is Android's per-app language (see `AppLocales`); this record is what carries
 * it in the settings backup to a new phone, and what an install upgraded from a version without
 * per-app languages starts from.
 */
internal object LanguageRecord {
    private val LegacyNames = mapOf("en" to "english", "he" to "hebrew")

    fun encode(tag: String): String = LegacyNames[tag] ?: tag

    fun decode(value: String?): String? = when (value) {
        null, "" -> null
        else -> LegacyNames.entries.firstOrNull { it.value == value }?.key ?: value
    }
}
