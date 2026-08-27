// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import com.noamtu.jewishday.model.DailyLearningType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyLearningCacheCodecTest {
    @Test
    fun cacheCodecRoundTripsLearningEntries() {
        val entries = listOf(
            HebcalLearningEntry(
                category = "dailyRambam1",
                title = "Sabbath 17",
                hebrew = "הלכות שבת פרק יז",
            ),
            HebcalLearningEntry(
                category = "shemiratHaLashon",
                title = "Book II 6.1-6.3",
                memo = "Book II 6.1-6.3",
            ),
        )

        val decoded = DailyLearningCacheCodec.decode(DailyLearningCacheCodec.encode(entries))

        assertEquals(entries, decoded)
    }

    @Test
    fun rambamOneAndThreeChaptersAreIndependentRowsUnderRambamYomi() {
        val entries = listOf(
            HebcalLearningEntry(
                category = "dailyRambam1",
                title = "Sabbath 17",
                hebrew = "הלכות שבת פרק יז",
            ),
            HebcalLearningEntry(
                category = "dailyRambam3",
                title = "Gifts to the Poor 8-10",
                hebrew = "הלכות מתנות עניים פרק 8-10",
            ),
        )

        // Both tracks are mapped as separate "Rambam Yomi" rows; visibility is decided later by
        // each row's own daily-learning id, so the two toggle independently.
        val rows = entries.toZmanItems().filter { it.title == "Rambam Yomi" }
        assertEquals(2, rows.size)

        val oneChapter = rows.single { it.id == DailyLearningType.RambamYomi.storageValue }
        assertEquals("Sabbath 17", oneChapter.value)
        assertEquals("1 chapter", oneChapter.description)

        val threeChapters = rows.single { it.id == DailyLearningType.RambamYomiThreeChapters.storageValue }
        assertEquals("Gifts to the Poor 8-10", threeChapters.value)
        assertEquals("3 chapters", threeChapters.description)
        assertEquals("3 פרקים", threeChapters.descriptionHebrew)
        // Arabic chapter range -> punctuated gematria + plural פרקים, matching the 1-chapter style.
        assertEquals("הלכות מתנות עניים פרקים ח׳-י׳", threeChapters.valueHebrew)
    }
}