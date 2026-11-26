// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.update

import com.noamtu.jewishday.i18n.normalizeTag

/**
 * A release body, parsed into the changelog the update dialog shows.
 *
 * The body is written in **English only**, as ordinary markdown, so the release page on GitHub reads
 * like any other project's. The Hebrew translation rides along inside a single HTML comment, which
 * GitHub renders as nothing at all — the public page stays entirely English:
 *
 * ```
 * ### New
 * - The prayer compass now announces its alignment to screen readers
 *
 * ### Improved
 * - Faster cold start
 *
 * ### Fixed
 * - The compass no longer points backwards in landscape
 *
 * <!-- lang:he
 * ### חדש
 * - מצפן התפילה מכריז על הכיוון לקוראי מסך
 *
 * ### שיפורים
 * - פתיחה מהירה יותר
 *
 * ### תיקונים
 * - המצפן כבר לא מצביע הפוך במצב לרוחב
 * -->
 * ```
 *
 * Headings name the category; bullets are the entries. Anything the app cannot classify still shows,
 * under [ReleaseNoteCategory.Other] — an unrecognised heading must never swallow its own bullets.
 *
 * Any other language can ride along the same way, in a block of its own named by its language code
 * (`<!-- lang:fr … -->`); the notes are shown in the app's language when they have it.
 *
 * A body with no block for the app's language falls back to the English one, and a body with no headings at all —
 * every release published before this convention, including `--generate-notes` output — comes back
 * as a single unlabelled section, so nothing historical renders blank.
 */
data class ReleaseNoteSection(
    val category: ReleaseNoteCategory,
    /** The heading as written, kept so an unrecognised one still reads as itself. */
    val heading: String,
    val items: List<String>,
)

enum class ReleaseNoteCategory { New, Improved, Fixed, Other }

/** A translation block, by language code. Non-greedy so a body may hold other comments without confusion. */
private val TranslationBlock = Regex("""<!--\s*lang:([A-Za-z-]+)\s*(.*?)-->""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))

private val HeadingLine = Regex("""^#{1,6}\s*(.+?)\s*#*$""")
private val BulletLine = Regex("""^[-*+]\s+(.*)$""")

/**
 * The plain text of the notes in [language] (a language code: "en", "he"), for anything that wants
 * it unstructured — English, the open body, when there is no block for it.
 */
fun releaseNotesFor(body: String, language: String): String {
    val blocks = TranslationBlock.findAll(body)
        .map { normalizeTag(it.groupValues[1].lowercase()) to it.groupValues[2].trim() }
        .filter { (_, text) -> text.isNotEmpty() }
        .toList()
    val translated = blocks.firstOrNull { (code, _) -> code == normalizeTag(language) }?.second
    val english = TranslationBlock.replace(body, "").trim()
    return when {
        translated != null -> translated
        english.isNotEmpty() -> english
        // A body that is *only* a translation block should still show something rather than nothing.
        else -> blocks.firstOrNull()?.second.orEmpty()
    }
}

/** The same notes, split into the categories the dialog renders. */
fun parseReleaseNotes(body: String, language: String): List<ReleaseNoteSection> =
    parseSections(releaseNotesFor(body, language))

private fun parseSections(notes: String): List<ReleaseNoteSection> {
    val sections = mutableListOf<ReleaseNoteSection>()
    var heading: String? = null
    var items = mutableListOf<String>()

    fun flush() {
        if (heading == null && items.isEmpty()) return
        sections += ReleaseNoteSection(
            category = categoryOf(heading),
            heading = heading.orEmpty(),
            items = items.toList(),
        )
        heading = null
        items = mutableListOf()
    }

    notes.lines().forEach { raw ->
        val line = raw.trim()
        val headingMatch = HeadingLine.find(line)
        when {
            line.isEmpty() -> Unit
            headingMatch != null -> {
                flush()
                heading = headingMatch.groupValues[1]
            }
            else -> items += BulletLine.find(line)?.groupValues?.get(1)?.trim() ?: line
        }
    }
    flush()
    return sections.filter { it.heading.isNotEmpty() || it.items.isNotEmpty() }
}

/**
 * Classifies a heading by what it says, in either language. Matching on the text rather than on a
 * fixed vocabulary means a release can write "Bug fixes" or "תיקוני באגים" and still be understood,
 * and anything genuinely unrecognised keeps its own heading instead of being forced into a bucket.
 */
private fun categoryOf(heading: String?): ReleaseNoteCategory {
    val text = heading?.lowercase()?.trim() ?: return ReleaseNoteCategory.Other
    return when {
        NewWords.any(text::contains) -> ReleaseNoteCategory.New
        FixedWords.any(text::contains) -> ReleaseNoteCategory.Fixed
        ImprovedWords.any(text::contains) -> ReleaseNoteCategory.Improved
        else -> ReleaseNoteCategory.Other
    }
}

// Checked before "improved" so "new features" does not match on a stray "change".
private val NewWords = listOf("new", "feature", "added", "addition", "חדש", "תוספ", "פיצ")
private val FixedWords = listOf("fix", "bug", "crash", "תיקון", "תיקונ", "באג", "קריס")
private val ImprovedWords = listOf("improve", "better", "change", "update", "polish", "faster", "שיפור", "שינוי", "שיפורים")
