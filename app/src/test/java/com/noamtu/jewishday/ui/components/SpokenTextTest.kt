// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SpokenTextTest {
    @Test
    fun aRowReadsItsNameThenItsTimeThenItsCaption() {
        assertEquals("Sunrise, 6:42 AM, Sea level", spokenText("Sunrise", "6:42 AM", "Sea level"))
    }

    @Test
    fun aMissingOrBlankPieceIsLeftOut() {
        assertEquals("Sunrise, 6:42 AM", spokenText("Sunrise", "6:42 AM", ""))
        assertEquals("Sunrise, 6:42 AM", spokenText("Sunrise", null, "6:42 AM", "  "))
        assertEquals("", spokenText(null, ""))
    }

    @Test
    fun aValueOnTwoLinesReadsAsTwoClauses() {
        // The merged Rambam bubble: one track per line.
        assertEquals(
            "Rambam, 1 chapter: Shabbat 3, 3 chapters: Shabbat 4-6",
            spokenText("Rambam", "1 chapter: Shabbat 3\n3 chapters: Shabbat 4-6\n"),
        )
    }

    @Test
    fun sentencesAreJoinedAsSentences() {
        assertEquals("5 Kislev 5787. Wednesday, 26 November", spokenSentences("5 Kislev 5787", null, "Wednesday, 26 November"))
        assertEquals("a. b", spokenSentences(listOf("a", "", "b")))
    }

    @Test
    fun hebrewIsJoinedTheSameWay() {
        assertEquals("זריחה, 06:42", spokenText("זריחה", "06:42"))
    }
}
