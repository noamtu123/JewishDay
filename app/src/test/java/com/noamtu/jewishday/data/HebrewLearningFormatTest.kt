// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import org.junit.Assert.assertEquals
import org.junit.Test

class HebrewLearningFormatTest {
    @Test
    fun gematriaUsesGereshAndGershayimAndAvoidsTheDivineName() {
        assertEquals("ה׳", hebrewNumber(5))
        assertEquals("י״א", hebrewNumber(11))
        assertEquals("ט״ו", hebrewNumber(15))
        assertEquals("ט״ז", hebrewNumber(16))
        assertEquals("י״ח", hebrewNumber(18))
        assertEquals("קס״א", hebrewNumber(161))
        assertEquals("קס״ב", hebrewNumber(162))
    }

    @Test
    fun mishnahYomiNumbersBecomeHebrewLetters() {
        val item = listOf(
            HebcalLearningEntry(category = "mishnayomi", title = "Kelim 11:7-8", hebrew = "כלים 11:7-8"),
        ).toZmanItems().single()

        assertEquals("כלים י״א: ז-ח", item.valueHebrew)
    }

    @Test
    fun kitzurShulchanAruchIsFormattedWithGershayimAndSpacing() {
        val item = listOf(
            HebcalLearningEntry(category = "kitzurShulchanAruch", title = "161:18-162:5", hebrew = "קסא:יח-קסב:ה"),
        ).toZmanItems().single()

        assertEquals("קס״א: יח - קס״ב: ה", item.valueHebrew)
    }

    @Test
    fun tanakhYomiShowsTheRealBookName() {
        val item = listOf(
            HebcalLearningEntry(category = "tanakhYomi", title = "Psalms Seder 3", hebrew = "תנ״ך ס׳ ג", memo = "Psalms 20:10-29:10"),
        ).toZmanItems().single()

        assertEquals("תהלים ס׳ ג׳", item.valueHebrew)
    }

    @Test
    fun tanakhYomiFallsBackToGenericWhenNoBookMatches() {
        val item = listOf(
            HebcalLearningEntry(category = "tanakhYomi", title = "Seder 3", hebrew = "תנ״ך ס׳ ג", memo = "Seder 3"),
        ).toZmanItems().single()

        assertEquals("תנ״ך ס׳ ג׳", item.valueHebrew)
    }

    @Test
    fun tehillimYomiUsesFullHebrewSpelling() {
        val item = listOf(
            HebcalLearningEntry(category = "dailyPsalms", title = "Psalms 106-107", hebrew = "תהלים ק״ו-ק״ז"),
        ).toZmanItems().single()

        assertEquals("תהילים ק״ו-ק״ז", item.valueHebrew)
    }

    @Test
    fun rambamEnglishNeverUsesTheSefariaLinkMemo() {
        // On multi-book days Hebcal's memo is a list of Sefaria URLs; the English value must
        // come from the title instead of the memo.
        val item = listOf(
            HebcalLearningEntry(
                category = "dailyRambam3",
                title = "The Chosen Temple 8, Vessels of the Sanctuary and Those who Serve Therein 1-2",
                hebrew = "הלכות בית הבחירה פרק ח, הלכות כלי המקדש והעובדין בו פרק 1-2",
                memo = "The Chosen Temple 8\nhttps://www.sefaria.org/Mishneh_Torah\n\nVessels 1-2\nhttps://www.sefaria.org/Mishneh_Torah2",
            ),
        ).toZmanItems().single()

        assertEquals("The Chosen Temple 8, Vessels of the Sanctuary and Those who Serve Therein 1-2", item.value)
    }

    @Test
    fun shemiratHaLashonTranslatesBookPrefixAndFormatsChapterHalacha() {
        val item = listOf(
            HebcalLearningEntry(
                category = "shemiratHaLashon",
                title = "Book I, Shar Hazechira 1.1-1.4",
                hebrew = "Book I, שער הזכירה 1.1-1.4",
                memo = "Book I, The Gate of Remembering 1.1-1.4",
            ),
        ).toZmanItems().single()

        assertEquals("חלק א׳ שער הזכירה א׳: א-ד", item.valueHebrew)
    }

    @Test
    fun shemiratHaLashonHandlesIntroDaysWithoutChapterHalacha() {
        val item = listOf(
            HebcalLearningEntry(
                category = "shemiratHaLashon",
                title = "Book I, Hakdamah 1-2",
                hebrew = "Book I, הקדמה 1-2",
                memo = "Book I, Introduction 1-2",
            ),
        ).toZmanItems().single()

        assertEquals("חלק א׳ הקדמה א-ב", item.valueHebrew)
    }

    @Test
    fun shemiratHaLashonBookTwoHasNoNamedGateSoHebrewSourceIsUntranslated() {
        // Chelek Bet has no named shaarim, so Hebcal's Hebrew field for its body is literally
        // identical to the English title (e.g. "Book II 1.1-1.2") — must be parsed structurally.
        val item = listOf(
            HebcalLearningEntry(
                category = "shemiratHaLashon",
                title = "Book II 9.8-9.10",
                hebrew = "Book II 9.8-9.10",
                memo = "Book II 9.8-9.10",
            ),
        ).toZmanItems().single()

        assertEquals("חלק ב׳ ט׳: ח-י", item.valueHebrew)
    }
}