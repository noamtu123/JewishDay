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
    fun rambamThreeChaptersStayInTheSameRambamItem() {
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
            .single { it.title == "Rambam Yomi" }
        val withThreeChapters = entries.toZmanItems(includeRambamThreeChapters = true)
            .single { it.title == "Rambam Yomi" }

        assertEquals("1 chapter: Sabbath 17", oneChapterOnly.value)
        assertTrue(withThreeChapters.value?.contains("1 chapter: Sabbath 17") == true)
        assertTrue(withThreeChapters.value?.contains("3 chapters: Gifts to the Poor 8-10") == true)
    }
}
