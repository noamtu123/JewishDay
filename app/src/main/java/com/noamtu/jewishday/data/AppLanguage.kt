// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import java.util.Locale

/**
 * UI language for the app's chrome (navigation, settings, labels).
 *
 * The app always shows the Hebrew date alongside a foreign-language rendering; this
 * only controls which language the interface text uses. Designed to grow: add an
 * entry with its [storageValue] and self-language [displayName] and the picker,
 * storage, and migration handle it automatically. [displayName] is intentionally
 * written in each language's own script so the picker is readable regardless of the
 * currently active language.
 */
enum class AppLanguage(val storageValue: String, val displayName: String) {
    English("english", "English"),
    Hebrew("hebrew", "עברית"),
    ;

    /** The rest of the app still switches on a single Hebrew/other boolean. */
    val useHebrewInterface: Boolean get() = this == Hebrew

    companion object {
        fun fromStorageValue(value: String?): AppLanguage? =
            entries.firstOrNull { it.storageValue == value }

        /**
         * First-launch default: follow the device language. A Hebrew device starts in
         * Hebrew; everything else starts in English (the broadest fallback today).
         * "iw" is the legacy ISO code some Android versions still report for Hebrew.
         */
        fun systemDefault(locale: Locale = Locale.getDefault()): AppLanguage =
            if (locale.language == "he" || locale.language == "iw") Hebrew else English
    }
}