// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.i18n

import com.noamtu.jewishday.core.model.KitzurReading
import com.noamtu.jewishday.core.model.MishnahPlace
import com.noamtu.jewishday.core.model.MishnahYomiReading
import com.noamtu.jewishday.core.model.RambamPart
import com.noamtu.jewishday.core.model.ShemiratHaLashonReading
import com.noamtu.jewishday.core.model.ShemiratHaLashonSection
import com.noamtu.jewishday.core.model.TanakhYomiReading

// How the daily-learning readings worked out on the phone are written. English writes references
// the way the sefarim are cited ("Kelim 6:4-7:1"); Hebrew writes chapters as punctuated numerals and
// what is inside them as bare letters, with ": " between ("כלים ו׳: ד - ז׳: א").

// ---- Mishnah Yomi ----

internal fun MishnahYomiReading.english(): String {
    val start = "${first.tractate.english} ${first.chapter}:${first.mishnah}"
    return when {
        second.tractate != first.tractate -> "$start-${second.tractate.english} ${second.chapter}:${second.mishnah}"
        second.chapter != first.chapter -> "$start-${second.chapter}:${second.mishnah}"
        else -> "$start-${second.mishnah}"
    }
}

internal fun MishnahYomiReading.hebrew(): String {
    fun place(place: MishnahPlace) = "${hebrewNumber(place.chapter)}: ${gematriaLetters(place.mishnah)}"
    val start = "${first.tractate.hebrew} ${place(first)}"
    return when {
        second.tractate != first.tractate -> "$start - ${second.tractate.hebrew} ${place(second)}"
        second.chapter != first.chapter -> "$start - ${place(second)}"
        else -> "$start-${gematriaLetters(second.mishnah)}"
    }
}

// ---- Rambam Yomi ----

internal fun List<RambamPart>.english(): String = joinToString(", ") { part ->
    val range = if (part.first == part.last) part.first else "${part.first}-${part.last}"
    "${part.book.english} $range"
}

/** "הלכות שבת פרק ח׳", "הלכות תפילה פרקים ג׳-ה׳"; the four introductory books' runs read the same way. */
internal fun List<RambamPart>.hebrew(): String = joinToString(", ") { part ->
    val chapters = if (part.first == part.last) {
        "פרק ${rambamPlace(part.first)}"
    } else if (':' in part.first) {
        "פרקים ${rambamPlace(part.first)} - ${rambamPlace(part.last)}"
    } else {
        "פרקים ${rambamPlace(part.first)}-${rambamPlace(part.last)}"
    }
    "${part.book.hebrew} $chapters"
}

/** "8" → "ח׳"; "5:1" (chapter:halacha) → "ה׳: א". */
private fun rambamPlace(place: String): String {
    val (chapter, halacha) = place.split(':').let { it.first() to it.getOrNull(1) }
    val number = hebrewNumber(chapter.toInt())
    return if (halacha == null) number else "$number: ${gematriaLetters(halacha.toInt())}"
}

// ---- Tanakh Yomi ----

internal fun TanakhYomiReading.english(): String = verses

private val VerseRange = Regex("^(.+?) (\\d+):(\\d+)-(?:(\\D+?) )?(?:(\\d+):)?(\\d+)$")

/** "I Kings 7:21-8:10" → "מלכים א ז׳: כא - ח׳: י", the book names as Hebrew readers know them. */
internal fun TanakhYomiReading.hebrew(): String {
    val match = VerseRange.find(verses) ?: return verses
    val (book, chapter, verse, endBook, endChapter, endVerse) = match.destructured
    val start = "${tanakhBook(book)} ${hebrewNumber(chapter.toInt())}: ${gematriaLetters(verse.toInt())}"
    val endVerseLetters = gematriaLetters(endVerse.toInt())
    return when {
        endBook.isNotEmpty() -> "$start - ${tanakhBook(endBook)} ${hebrewNumber(endChapter.toInt())}: $endVerseLetters"
        endChapter.isNotEmpty() -> "$start - ${hebrewNumber(endChapter.toInt())}: $endVerseLetters"
        else -> "$start-$endVerseLetters"
    }
}

private fun tanakhBook(english: String): String = TanakhBookNames[english] ?: english

private val TanakhBookNames = mapOf(
    "Joshua" to "יהושע",
    "Judges" to "שופטים",
    "I Samuel" to "שמואל א",
    "II Samuel" to "שמואל ב",
    "I Kings" to "מלכים א",
    "II Kings" to "מלכים ב",
    "Isaiah" to "ישעיה",
    "Jeremiah" to "ירמיה",
    "Ezekiel" to "יחזקאל",
    "Hosea" to "הושע",
    "Joel" to "יואל",
    "Amos" to "עמוס",
    "Obadiah" to "עובדיה",
    "Jonah" to "יונה",
    "Micah" to "מיכה",
    "Nahum" to "נחום",
    "Habakkuk" to "חבקוק",
    "Zephaniah" to "צפניה",
    "Haggai" to "חגי",
    "Zechariah" to "זכריה",
    "Malachi" to "מלאכי",
    "Psalms" to "תהלים",
    "Proverbs" to "משלי",
    "Job" to "איוב",
    "Song of Songs" to "שיר השירים",
    "Ruth" to "רות",
    "Lamentations" to "איכה",
    "Ecclesiastes" to "קהלת",
    "Esther" to "אסתר",
    "Daniel" to "דניאל",
    "Ezra" to "עזרא",
    "Nehemiah" to "נחמיה",
    "I Chronicles" to "דברי הימים א",
    "II Chronicles" to "דברי הימים ב",
)

// ---- Shemirat HaLashon ----

// One piece of chelek bet is a footnote to its eleventh halacha in chapter 19, read on a day of its own.
private const val ShemiratFootnote = "19.Footnote_in_11"

internal fun ShemiratHaLashonReading.english(): String {
    val book = if (book == 2) "Book II" else "Book I"
    val section = when (section) {
        ShemiratHaLashonSection.Introduction -> "Introduction"
        ShemiratHaLashonSection.Remembering -> "The Gate of Remembering"
        ShemiratHaLashonSection.Discerning -> "The Gate of Discerning"
        ShemiratHaLashonSection.Torah -> "The Gate of Torah"
        ShemiratHaLashonSection.Epilogue -> "Epilogue"
        ShemiratHaLashonSection.Chapters -> null
    }
    val reference = when {
        first == ShemiratFootnote -> "19.11 (footnote)"
        first == last -> first
        else -> "$first-$last"
    }
    return if (section == null) "$book $reference" else "$book, $section $reference"
}

/** "חלק א׳ שער התבונה ח׳: ד-ה"; chelek bet's chapters have no gate name ("חלק ב׳ ג׳: א-ב"). */
internal fun ShemiratHaLashonReading.hebrew(): String {
    val section = when (section) {
        ShemiratHaLashonSection.Introduction -> "הקדמה"
        ShemiratHaLashonSection.Remembering -> "שער הזכירה"
        ShemiratHaLashonSection.Discerning -> "שער התבונה"
        ShemiratHaLashonSection.Torah -> "שער התורה"
        ShemiratHaLashonSection.Epilogue -> "חתימת הספר"
        ShemiratHaLashonSection.Chapters -> null
    }
    val reference = if (first == ShemiratFootnote) "${hebrewNumber(19)}: ${gematriaLetters(11)} (הערה)" else shemiratReference()
    return listOfNotNull("חלק ${hebrewNumber(book)}", section, reference).joinToString(" ")
}

/** "8.4"-"8.5" → "ח׳: ד-ה"; across chapters "ב׳: ג - ג׳: א"; the introduction's plain numbers "ז-יג". */
private fun ShemiratHaLashonReading.shemiratReference(): String {
    fun split(place: String) = place.split('.').let { it.first().toInt() to it.getOrNull(1)?.toInt() }
    val (startChapter, startHalacha) = split(first)
    val (endChapter, endHalacha) = split(last)
    if (startHalacha == null || endHalacha == null) {
        return if (first == last) gematriaLetters(startChapter) else "${gematriaLetters(startChapter)}-${gematriaLetters(endChapter)}"
    }
    val start = "${hebrewNumber(startChapter)}: ${gematriaLetters(startHalacha)}"
    return when {
        first == last -> start
        startChapter == endChapter -> "$start-${gematriaLetters(endHalacha)}"
        else -> "$start - ${hebrewNumber(endChapter)}: ${gematriaLetters(endHalacha)}"
    }
}

// ---- Kitzur Shulchan Aruch ----

private const val KitzurEnd = "E"

internal fun KitzurReading.english(): String = alternative?.let {
    "Hilchot Shmita v'Terumah ${kitzurEnglish(reading)} / Hilchot Brachot v'Tefilah ${kitzurEnglish(it)}"
} ?: kitzurEnglish(reading)

internal fun KitzurReading.hebrew(): String = alternative?.let {
    "הלכות שמיטה ותרו״מ ${kitzurHebrew(reading)} / הלכות ברכות ותפלה ${kitzurHebrew(it)}"
} ?: kitzurHebrew(reading)

/** "40:5-40:13" → "40:5-13", "40:14-40:E" → "40:14-end"; a range across simanim keeps both. */
private fun kitzurEnglish(range: String): String {
    val (start, end) = kitzurEnds(range) ?: return range
    val endText = end.substringAfter(':').takeIf { end.substringBefore(':') == start.substringBefore(':') && ':' in end } ?: end
    return "$start-${endText.replace(KitzurEnd, "end")}"
}

/** "40:5-40:13" → "מ׳: ה-יג", "134:13-135:2" → "קל״ד: יג - קל״ה: ב", "Klalim" → "כללים". */
private fun kitzurHebrew(range: String): String {
    if (range == "Klalim") return "כללים"
    val (start, end) = kitzurEnds(range) ?: return range
    val startSiman = start.substringBefore(':', missingDelimiterValue = "")
    val startText = kitzurPlace(start)
    return when {
        ':' !in end -> "$startText-${kitzurPiece(end)}"
        end.substringBefore(':') == startSiman -> "$startText-${kitzurPiece(end.substringAfter(':'))}"
        else -> "$startText - ${kitzurPlace(end)}"
    }
}

private fun kitzurEnds(range: String): Pair<String, String>? =
    range.split('-').takeIf { it.size == 2 }?.let { (start, end) -> start to end }

/** "40:5" → "מ׳: ה"; "Shmita:1" → "שמיטה: א"; a lone number → its letters. */
private fun kitzurPlace(place: String): String {
    if (':' !in place) return kitzurPiece(place)
    val siman = place.substringBefore(':')
    val simanText = siman.toIntOrNull()?.let(::hebrewNumber) ?: if (siman == "Shmita") "שמיטה" else siman
    return "$simanText: ${kitzurPiece(place.substringAfter(':'))}"
}

private fun kitzurPiece(piece: String): String =
    if (piece == KitzurEnd) "סוף" else piece.toIntOrNull()?.let(::gematriaLetters) ?: piece
