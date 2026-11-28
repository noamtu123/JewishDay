// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import com.noamtu.jewishday.ui.components.spokenSentences
import com.noamtu.jewishday.ui.components.spokenText

/*
 * What TalkBack reads for each widget. A widget is one tap target (it opens the app), so the launcher
 * reads it as one item; without a description that item is its texts run together in layout order —
 * "Next zman Sunrise 6:42 AM Then: Mincha 1:30 PM", or a lone "—". These say the same things as
 * sentences, in the app's language, from the same [WidgetContent] the widget is drawn from. The sky
 * bitmaps behind the glass ones are decoration and say nothing.
 */

/** The Hebrew date, what makes the day special, and the civil date. */
internal fun hebrewDateSpoken(content: WidgetContent): String =
    spokenSentences(content.hebrewDate, content.specialDay, content.gregorianDate)

/** "Next zman, Sunrise, 6:42 AM. Then, Mincha, 1:30 PM." */
internal fun nextZmanSpoken(content: WidgetContent): String =
    spokenSentences(
        content.next?.let { spokenText(content.nextLabel, it.title, it.time) },
        content.following?.let { spokenText(content.followingLabel, it.title, it.time) },
    )

/** The date, then the [rows] the list shows, the next of them named as such. */
internal fun zmanimSpoken(content: WidgetContent, rows: List<IndexedValue<WidgetZman>>): String =
    spokenSentences(
        listOf(content.hebrewDate, content.specialDay) + rows.map { (index, zman) ->
            if (index == content.nextIndex) spokenText(content.nextLabel, zman.title, zman.time) else spokenText(zman.title, zman.time)
        },
    )

/** What is coming, and when it comes in and goes out; null when there is nothing to say. */
internal fun shabbatSpoken(content: WidgetContent): String? = content.shabbat?.let { shabbat ->
    spokenSentences(
        shabbat.title,
        shabbat.subtitle,
        shabbat.entryTime?.let { spokenText(shabbat.entryHeading, it) },
        shabbat.exitTime?.let { spokenText(shabbat.exitHeading, it) },
    )
}

/** The glass date widget: the dates, then the next time and the [ahead] ones listed under it. */
internal fun glassDateSpoken(content: WidgetContent, ahead: List<WidgetZman>): String =
    spokenSentences(
        listOf(hebrewDateSpoken(content), nextZmanLine(content)) + ahead.map { spokenText(it.title, it.time) },
    )

private fun nextZmanLine(content: WidgetContent): String? =
    content.next?.let { spokenText(content.nextLabel, it.title, it.time) }
