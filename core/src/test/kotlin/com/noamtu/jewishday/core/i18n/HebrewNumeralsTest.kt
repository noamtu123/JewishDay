// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HebrewNumeralsTest {
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
}
