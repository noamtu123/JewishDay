// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JewishDayInfoTest {
    @Test
    fun jewishDayInfoFormatsKnownPesachDate() {
        val info = jewishDayInfo(LocalDate.of(2024, 4, 23))

        assertEquals(LocalDate.of(2024, 4, 23), info.gregorianDate)
        assertEquals(15, info.hebrewDayOfMonth)
        assertEquals("ט״ו", info.hebrewDayOfMonthHebrew)
        assertTrue(info.hebrewDateEnglish.contains("Nissan"))
        assertTrue(info.hebrewDateHebrew.isNotBlank())
    }

    @Test
    fun hebrewDayOfMonthTextUsesStandardHebrewNumberRules() {
        assertEquals("א׳", hebrewDayOfMonthText(1))
        assertEquals("י״ד", hebrewDayOfMonthText(14))
        assertEquals("ט״ו", hebrewDayOfMonthText(15))
        assertEquals("ט״ז", hebrewDayOfMonthText(16))
        assertEquals("ל׳", hebrewDayOfMonthText(30))
    }
}