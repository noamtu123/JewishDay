// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.i18n

import com.noamtu.jewishday.core.i18n.EnglishLanguageFormatter
import com.noamtu.jewishday.core.i18n.HebrewLanguageFormatter
import com.noamtu.jewishday.core.i18n.LanguageFormatter
import com.noamtu.jewishday.data.LanguageRecord
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocaleMatchingTest {
    private val supported = listOf("en", "he")

    @Test
    fun theTranslationShownIsTheFirstPreferredLanguageThatHasOne() {
        assertEquals("he", matchSupported(listOf(Locale.forLanguageTag("he-IL")), supported))
        assertEquals("en", matchSupported(listOf(Locale.forLanguageTag("en-GB")), supported))
        // Resources fall through the user's languages in order, and so does this.
        assertEquals("he", matchSupported(listOf(Locale.FRENCH, Locale.forLanguageTag("he")), supported))
        // A language without a translation shows the default, English.
        assertEquals("en", matchSupported(listOf(Locale.FRENCH), supported))
        assertEquals("en", matchSupported(emptyList(), supported))
    }

    @Test
    fun hebrewsOldCodeIsTheSameLanguage() {
        @Suppress("DEPRECATION")
        val legacy = Locale("iw")
        assertEquals("he", matchSupported(listOf(legacy), supported))
        assertEquals("he", normalizeTag("iw"))
        assertEquals("he-IL", normalizeTag("iw-IL"))
        assertEquals("pt-BR", normalizeTag("pt-BR"))
    }

    @Test
    fun theBuildListsEveryTranslationFolder() {
        // English is res/values; Hebrew is res/values-iw, under its modern tag.
        assertEquals(listOf("en", "he"), AppLocales.supported)
    }

    @Test
    fun eachLanguageIsNamedInItself() {
        assertEquals("English", AppLocales.displayName("en"))
        assertEquals("עברית", AppLocales.displayName("he"))
    }

    @Test
    fun theSettingsRecordKeepsTheNamesOlderVersionsRead() {
        assertEquals("english", LanguageRecord.encode("en"))
        assertEquals("hebrew", LanguageRecord.encode("he"))
        assertEquals("fr", LanguageRecord.encode("fr"))
        assertEquals("he", LanguageRecord.decode("hebrew"))
        assertEquals("en", LanguageRecord.decode("english"))
        assertEquals("pt-BR", LanguageRecord.decode("pt-BR"))
        assertNull(LanguageRecord.decode(null))
    }

    @Test
    fun eachLanguageGetsItsOwnFormatterAndTheRestFallBackToEnglish() {
        assertTrue(LanguageFormatter.forLocale(Locale.forLanguageTag("he")) is HebrewLanguageFormatter)
        @Suppress("DEPRECATION")
        assertTrue(LanguageFormatter.forLocale(Locale("iw")) is HebrewLanguageFormatter)
        assertTrue(LanguageFormatter.forLocale(Locale.ENGLISH) is EnglishLanguageFormatter)
        assertTrue(LanguageFormatter.forLocale(Locale.FRENCH) is EnglishLanguageFormatter)
    }
}
