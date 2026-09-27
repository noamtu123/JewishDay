// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import com.kosherjava.zmanim.hebrewcalendar.Daf
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TransliterationTest {
    private val formatter = englishHebrewDateFormatter()

    private fun calendar(date: LocalDate, inIsrael: Boolean = true) = JewishCalendar(date).apply {
        isUseModernHolidays = true
        setInIsrael(inIsrael)
    }

    @Test
    fun holidaysUseTheModernSpelling() {
        assertEquals("Sukkot", formatter.formatYomTov(calendar(LocalDate.of(2026, 9, 26))))
        assertEquals("Chol Hamoed Sukkot", formatter.formatYomTov(calendar(LocalDate.of(2026, 9, 28))))
        assertEquals("Shemini Atzeret", formatter.formatYomTov(calendar(LocalDate.of(2026, 10, 3), inIsrael = false)))
        assertEquals("Simchat Torah", formatter.formatYomTov(calendar(LocalDate.of(2026, 10, 4), inIsrael = false)))
        assertEquals("Shavuot", formatter.formatYomTov(calendar(LocalDate.of(2026, 5, 22))))
    }

    @Test
    fun monthsAndParshiotUseTheModernSpelling() {
        // 10 Tevet 5787.
        assertEquals("10 Tevet, 5787", formatter.format(calendar(LocalDate.of(2026, 12, 20))))
        // Shabbat Bereshit 5787.
        assertEquals("Bereshit", formatter.formatParsha(calendar(LocalDate.of(2026, 10, 10))))
    }

    @Test
    fun masechtotUseTheModernSpelling() {
        // Every Bavli masechta, via the Daf the formatter is given.
        val names = (0..39).map { masechta ->
            formatter.formatDafYomiBavli(Daf(masechta, 2))
        }
        assertFalse(names.toString(), names.any { it.contains("Shabbos") || it.contains("Kesubos") })
        assertEquals("Ketubot 2", names[14])
    }
}
