// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseNotesTest {

    private val body = """
        ### New
        - Prayer compass announces its alignment to screen readers

        ### Improved
        - Faster cold start

        ### Fixed
        - The compass no longer points backwards in landscape
        - A location below sea level no longer crashes

        <!-- lang:he
        ### חדש
        - מצפן התפילה מכריז על הכיוון לקוראי מסך

        ### שיפורים
        - פתיחה מהירה יותר

        ### תיקונים
        - המצפן כבר לא מצביע הפוך במצב לרוחב
        - מיקום מתחת לפני הים כבר לא מקריס
        -->
    """.trimIndent()

    @Test
    fun theHebrewBlockNeverAppearsInTheEnglishNotes() {
        // The whole point: GitHub renders the English and nothing else, so the public release page
        // has no Hebrew on it at all.
        val english = releaseNotesFor(body, language = "en")
        assertTrue(english, english.none { it in '֐'..'׿' })
        assertTrue(english, "lang:he" !in english)
    }

    @Test
    fun eachLanguageIsGroupedIntoItsCategories() {
        val english = parseReleaseNotes(body, language = "en")
        assertEquals(
            listOf(ReleaseNoteCategory.New, ReleaseNoteCategory.Improved, ReleaseNoteCategory.Fixed),
            english.map { it.category },
        )
        assertEquals(listOf("Faster cold start"), english[1].items)
        assertEquals(2, english[2].items.size)

        val hebrew = parseReleaseNotes(body, language = "he")
        assertEquals(
            listOf(ReleaseNoteCategory.New, ReleaseNoteCategory.Improved, ReleaseNoteCategory.Fixed),
            hebrew.map { it.category },
        )
        assertEquals(listOf("פתיחה מהירה יותר"), hebrew[1].items)
    }

    @Test
    fun headingsAreRecognisedByWhatTheySayNotByAFixedList() {
        fun categoryOf(heading: String) =
            parseReleaseNotes("### $heading\n- something", language = "en").single().category

        assertEquals(ReleaseNoteCategory.New, categoryOf("New features"))
        assertEquals(ReleaseNoteCategory.New, categoryOf("Added"))
        assertEquals(ReleaseNoteCategory.Fixed, categoryOf("Bug fixes"))
        assertEquals(ReleaseNoteCategory.Fixed, categoryOf("Crashes"))
        assertEquals(ReleaseNoteCategory.Improved, categoryOf("Improvements"))
        assertEquals(ReleaseNoteCategory.Improved, categoryOf("Changes"))
        assertEquals(ReleaseNoteCategory.Other, categoryOf("Notes for translators"))
    }

    @Test
    fun anUnrecognisedHeadingKeepsItsOwnWordsAndItsBullets() {
        val section = parseReleaseNotes("## Under the hood\n- Rewrote the parser", language = "en").single()
        assertEquals(ReleaseNoteCategory.Other, section.category)
        assertEquals("Under the hood", section.heading)
        assertEquals(listOf("Rewrote the parser"), section.items)
    }

    @Test
    fun anOldUnstructuredBodyStillShows() {
        // Everything published before this convention, and anything gh --generate-notes produced.
        val plain = "* Fixed a crash\n* Faster startup"
        val sections = parseReleaseNotes(plain, language = "en")
        assertEquals(1, sections.size)
        assertEquals(ReleaseNoteCategory.Other, sections.single().category)
        assertEquals(listOf("Fixed a crash", "Faster startup"), sections.single().items)
    }

    @Test
    fun aMissingHebrewBlockFallsBackToEnglishRatherThanBlank() {
        val englishOnly = "### Fixed\n- One thing"
        val sections = parseReleaseNotes(englishOnly, language = "he")
        // The category is still recognised, so the label renders in Hebrew even though the text
        // could not be translated — better than an empty "What's new".
        assertEquals(ReleaseNoteCategory.Fixed, sections.single().category)
        assertEquals(listOf("One thing"), sections.single().items)
    }

    @Test
    fun bulletMarkersAndHeadingDepthsAreAllAccepted() {
        val mixed = "# Fixed\n- dash\n* star\n+ plus"
        assertEquals(listOf("dash", "star", "plus"), parseReleaseNotes(mixed, language = "en").single().items)
    }

    @Test
    fun aParagraphWithNoHeadingIsNotLost() {
        val loose = "Thanks to everyone who tested.\n\n### Fixed\n- One thing"
        val sections = parseReleaseNotes(loose, language = "en")
        assertEquals(2, sections.size)
        assertEquals(listOf("Thanks to everyone who tested."), sections[0].items)
        assertEquals(ReleaseNoteCategory.Fixed, sections[1].category)
    }

    @Test
    fun anyLanguageCanRideAlongInABlockOfItsOwn() {
        val withFrench = "$body\n\n<!-- lang:fr\n### Nouveautés\n- Démarrage plus rapide\n-->"
        assertEquals("### Nouveautés\n- Démarrage plus rapide", releaseNotesFor(withFrench, language = "fr"))
        // The other languages are untouched by it, and a language with no block reads English.
        assertEquals(parseReleaseNotes(body, language = "he"), parseReleaseNotes(withFrench, language = "he"))
        assertEquals(releaseNotesFor(body, language = "en"), releaseNotesFor(withFrench, language = "de"))
        // Android may still call Hebrew by its old code.
        assertEquals(parseReleaseNotes(body, language = "he"), parseReleaseNotes(body, language = "iw"))
    }
}
