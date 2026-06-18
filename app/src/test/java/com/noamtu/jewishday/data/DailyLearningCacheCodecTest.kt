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
        val rambamOff = oneChapterOnly.filter { it.title == "Rambam Yomi" }
        assertEquals(1, rambamOff.size)
        assertEquals("Sabbath 17", rambamOff.single().value)

        // With the toggle on, the 3-chapter track is a second "Rambam Yomi" row (same title and
        // format as the 1-chapter row), distinguished by its description.
        val withThreeChapters = entries.toZmanItems(includeRambamThreeChapters = true)
        val rambamOn = withThreeChapters.filter { it.title == "Rambam Yomi" }
        assertEquals(2, rambamOn.size)
        assertEquals("Sabbath 17", rambamOn.single { it.description == "Hebcal Rambam, 1 chapter" }.value)
        assertEquals("Gifts to the Poor 8-10", rambamOn.single { it.description == "Hebcal Rambam, 3 chapters" }.value)
        assertTrue(rambamOn.all { it.id == DailyLearningType.RambamYomi.storageValue })
    }
}
