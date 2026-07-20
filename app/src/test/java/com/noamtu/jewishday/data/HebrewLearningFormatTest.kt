// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun gematriaValueAcceptsNumeralsAndRejectsOrdinaryWords() {
        assertEquals(32, gematriaValue("לב"))
        assertEquals(78, gematriaValue("ע״ח")) // already punctuated
        assertEquals(15, gematriaValue("טו"))
        // Words only *sum* to a number, they aren't written canonically — so they must be rejected,
        // otherwise tractate names and halacha titles would be "punctuated" as numerals.
        assertNull(gematriaValue("ברכות"))
        assertNull(gematriaValue("שבת"))
        assertNull(gematriaValue("אחד"))
        assertNull(gematriaValue("הלכות"))
        assertNull(gematriaValue(""))
    }

    @Test
    fun yerushalmiDafGetsTheSameGershayimAsBavli() {
        val yerushalmi = listOf(
            HebcalLearningEntry(category = "yerushalmi", title = "Yerushalmi Berakhot 32", hebrew = "ברכות לב"),
        ).toZmanItems().single()
        assertEquals("ברכות ל״ב", yerushalmi.valueHebrew)

        // Bavli already arrives punctuated and must be left exactly as-is.
        val bavli = listOf(
            HebcalLearningEntry(category = "dafyomi", title = "Sanhedrin 78", hebrew = "סנהדרין ע״ח"),
        ).toZmanItems().single()
        assertEquals("סנהדרין ע״ח", bavli.valueHebrew)
    }

    @Test
    fun yerushalmiKeepsMultiWordTractateNamesAndUnnumberedValues() {
        val multiWord = listOf(
            HebcalLearningEntry(category = "yerushalmi", title = "Yerushalmi Bava Kamma 5", hebrew = "בבא קמא ה"),
        ).toZmanItems().single()
        assertEquals("בבא קמא ה׳", multiWord.valueHebrew)

        val noNumeral = listOf(
            HebcalLearningEntry(category = "yerushalmi", title = "Yerushalmi Berakhot", hebrew = "ירושלמי ברכות"),
        ).toZmanItems().single()
        assertEquals("ירושלמי ברכות", noNumeral.valueHebrew)
    }

    @Test
    fun rambamSingleChapterIsPunctuated() {
        val item = listOf(
            HebcalLearningEntry(category = "dailyRambam1", title = "Prayer 5", hebrew = "הלכות תפילה פרק ה"),
        ).toZmanItems().single()

        assertEquals("הלכות תפילה פרק ה׳", item.valueHebrew)
    }

    @Test
    fun rambamChapterRangePunctuatesBothEndsAndPluralizes() {
        val item = listOf(
            HebcalLearningEntry(category = "dailyRambam3", title = "Prayer 3-5", hebrew = "הלכות תפילה פרק 3-5"),
        ).toZmanItems().single()

        assertEquals("הלכות תפילה פרקים ג׳-ה׳", item.valueHebrew)
    }

    @Test
    fun rambamPluralizesEachBookByItsOwnChapterReference() {
        // Regression: a day spanning two books used to take a document-wide "is there a range?"
        // decision and rewrite only the *first* פרק, producing "פרקים ח" for the single chapter
        // and leaving the real range as "פרק א-ב" — both backwards.
        val item = listOf(
            HebcalLearningEntry(
                category = "dailyRambam3",
                title = "The Chosen Temple 8, Vessels 1-2",
                hebrew = "הלכות בית הבחירה פרק ח, הלכות כלי המקדש והעובדין בו פרק 1-2",
            ),
        ).toZmanItems().single()

        assertEquals(
            "הלכות בית הבחירה פרק ח׳, הלכות כלי המקדש והעובדין בו פרקים א׳-ב׳",
            item.valueHebrew,
        )
    }

    @Test
    fun rambamNormalizesAWrongPluralFromTheSource() {
        val item = listOf(
            HebcalLearningEntry(category = "dailyRambam1", title = "Sabbath 19", hebrew = "הלכות שבת פרקים יט"),
        ).toZmanItems().single()

        assertEquals("הלכות שבת פרק י״ט", item.valueHebrew)
    }

    @Test
    fun rambamLeavesNonNumeralChapterTextAlone() {
        val item = listOf(
            HebcalLearningEntry(category = "dailyRambam1", title = "Intro", hebrew = "הלכות תפילה פרק אחד"),
        ).toZmanItems().single()

        assertEquals("הלכות תפילה פרק אחד", item.valueHebrew)
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