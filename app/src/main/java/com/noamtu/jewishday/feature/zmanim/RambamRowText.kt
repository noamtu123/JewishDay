// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.zmanim

import com.noamtu.jewishday.core.i18n.LanguageFormatter
import kotlinx.coroutines.flow.map

/** How a Rambam Yomi row reads once worded: the value it shows, and the forms of it that may fit. */
data class RambamRowText(val value: String, val candidates: List<String>)

/**
 * A worded Rambam Yomi value as its row shows it ("פרק אחד: <ref1>\n3 פרקים: <ref2>" when both
 * tracks share the row), with the forms the row may place beside its title. The "פרק אחד"/"3 פרקים"
 * labels are always kept; the only variation offered is dropping the inner "פרק"/"chapter" word from
 * the references so the (still two-line) bubble is narrow enough to sit beside the title when the
 * halacha names are short. The row places whichever candidate fits beside the title there, and stacks
 * the full one below only when neither fits.
 */
fun rambamRowText(value: String, language: LanguageFormatter): RambamRowText {
    val display = value.lines().joinToString("\n") { line ->
        val (label, reference) = rambamPrefixAndReference(line)
        label + language.rambamRowReference(reference)
    }
    val lines = display.lines().map { rambamPrefixAndReference(it) }
    val full = lines.joinToString("\n") { (label, reference) -> label + reference }
    val abbreviated = lines.joinToString("\n") { (label, reference) ->
        label + abbreviateRambamReference(reference, language.rambamChapterWords)
    }
    return RambamRowText(display, listOf(full, abbreviated).distinct())
}

/** Splits a Rambam line into its "פרק אחד: "/"3 פרקים: " label (empty when unmerged) and reference. */
private fun rambamPrefixAndReference(line: String): Pair<String, String> {
    val prefixEnd = line.indexOf(": ")
    return if (prefixEnd >= 0) {
        line.substring(0, prefixEnd + 2) to line.substring(prefixEnd + 2)
    } else {
        "" to line
    }
}

/** Drops the "chapter"/"פרק" word from a reference (e.g. "שבת פרק כח" -> "שבת כח") so it fits. */
private fun abbreviateRambamReference(reference: String, chapterWords: List<String>): String {
    var result = reference
    for (word in chapterWords) {
        result = result
            .replace(Regex("\\s+" + Regex.escape(word) + "\\s+"), " ")
            .replace(Regex("^" + Regex.escape(word) + "\\s+"), "")
    }
    return result.replace(Regex("\\s+"), " ").trim()
}
