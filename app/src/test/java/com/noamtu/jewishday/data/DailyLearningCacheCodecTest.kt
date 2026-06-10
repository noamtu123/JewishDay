package com.noamtu.jewishday.data

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
    fun rambamThreeChaptersAppearAsTheirOwnRow() {
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

        val oneChapterOnly = entries.toZmanItems(includeRambamThreeChapters = false)
        assertEquals("Sabbath 17", oneChapterOnly.single { it.title == "Rambam Yomi" }.value)
        assertTrue(oneChapterOnly.none { it.title == "Rambam Yomi · 3 Chapters" })

        val withThreeChapters = entries.toZmanItems(includeRambamThreeChapters = true)
        assertEquals("Sabbath 17", withThreeChapters.single { it.title == "Rambam Yomi" }.value)
        assertEquals("Gifts to the Poor 8-10", withThreeChapters.single { it.title == "Rambam Yomi · 3 Chapters" }.value)
    }
}
