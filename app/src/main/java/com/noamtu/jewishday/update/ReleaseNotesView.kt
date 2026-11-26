// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.update

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.noamtu.jewishday.R
import com.noamtu.jewishday.i18n.LocalTextResolver
import com.noamtu.jewishday.i18n.ProvideLanguage

/**
 * What's new: the release's own changelog, grouped into New / Improved / Fixed.
 *
 * The body carries English in the open and each translation inside an HTML comment (see
 * [parseReleaseNotes]); this picks the app's language, and the hidden "show in English" switch takes
 * the headings and text direction to English with it — an English changelog laid out right-to-left
 * puts the bullets on the wrong side and mangles the punctuation. Its own scrolling area, so long notes never push the buttons off screen.
 */
@Composable
internal fun ReleaseNotes(notes: String, showInEnglish: Boolean) {
    if (showInEnglish) {
        ProvideLanguage("en") { ReleaseNotesContent(notes) }
    } else {
        ReleaseNotesContent(notes)
    }
}

@Composable
private fun ReleaseNotesContent(notes: String) {
    val language = LocalTextResolver.current.locale.language
    val sections = remember(notes, language) { parseReleaseNotes(notes, language) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            modifier = Modifier.semantics { heading() },
            text = stringResource(R.string.update_notes_title),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            sections.forEach { section -> ReleaseNoteGroup(section) }
        }
    }
}

/** One category and its entries: a tinted label, then the bullets under it. */
@Composable
private fun ReleaseNoteGroup(section: ReleaseNoteSection) {
    val tone = section.category.tone()
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        section.label()?.let { label ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                section.category.icon()?.let { icon ->
                    Icon(
                        modifier = Modifier.size(16.dp),
                        imageVector = ImageVector.vectorResource(icon),
                        contentDescription = null,
                        tint = tone,
                    )
                }
                Text(
                    modifier = Modifier.semantics { heading() },
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = tone,
                )
            }
        }
        section.items.forEach { item ->
            // One TalkBack stop per entry, without the bullet, which TalkBack would read as "bullet".
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    modifier = Modifier.clearAndSetSemantics {},
                    text = "\u2022",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = item.asInlineMarkdown(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * A recognised category is labelled in the interface language, not with the heading as written — so
 * a release whose Hebrew block is missing still says "תיקונים" rather than "Bug fixes". An
 * unrecognised heading keeps its own words, which is the only honest thing to do with it.
 */
@Composable
internal fun ReleaseNoteSection.label(): String? = when (category) {
    ReleaseNoteCategory.New -> stringResource(R.string.update_notes_new)
    ReleaseNoteCategory.Improved -> stringResource(R.string.update_notes_improved)
    ReleaseNoteCategory.Fixed -> stringResource(R.string.update_notes_fixed)
    ReleaseNoteCategory.Other -> heading.takeIf(String::isNotBlank)
}

@Composable
internal fun ReleaseNoteCategory.tone(): Color = when (this) {
    ReleaseNoteCategory.New -> MaterialTheme.colorScheme.tertiary
    ReleaseNoteCategory.Improved -> MaterialTheme.colorScheme.primary
    ReleaseNoteCategory.Fixed -> MaterialTheme.colorScheme.secondary
    ReleaseNoteCategory.Other -> MaterialTheme.colorScheme.onSurfaceVariant
}

@DrawableRes
internal fun ReleaseNoteCategory.icon(): Int? = when (this) {
    ReleaseNoteCategory.New -> R.drawable.ic_auto_awesome
    ReleaseNoteCategory.Improved -> R.drawable.ic_trending_up
    ReleaseNoteCategory.Fixed -> R.drawable.ic_bug_report
    ReleaseNoteCategory.Other -> null
}

/** Bold runs and code spans inside one changelog entry. */
private fun String.asInlineMarkdown(): AnnotatedString =
    buildAnnotatedString { appendInline(this@asInlineMarkdown) }

/** Bold runs and code spans within one line; everything else is left as written. */
private fun AnnotatedString.Builder.appendInline(line: String) {
    var rest = line.replace("`", "")
    while (true) {
        val open = rest.indexOf(BoldMark)
        val close = if (open < 0) -1 else rest.indexOf(BoldMark, open + BoldMark.length)
        if (open < 0 || close < 0) {
            append(rest)
            return
        }
        append(rest.substring(0, open))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(rest.substring(open + BoldMark.length, close))
        }
        rest = rest.substring(close + BoldMark.length)
    }
}

private const val BoldMark = "**"
