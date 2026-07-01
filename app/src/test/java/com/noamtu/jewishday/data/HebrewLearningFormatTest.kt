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
    fun shemiratHaLashonUsesKlalFormatAndCountsHalachot() {
        val item = listOf(
            HebcalLearningEntry(category = "shemiratHaLashon", title = "Book II 9.8-9.10", memo = "Book II 9.8-9.10"),
        ).toZmanItems().single()

        assertEquals("כלל ט׳ ח-י", item.valueHebrew)
        assertEquals("3 הלכות יומיות", item.descriptionHebrew)
    }
}
