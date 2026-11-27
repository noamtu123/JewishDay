// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

import com.noamtu.jewishday.core.i18n.CalendarText
import com.noamtu.jewishday.core.i18n.EnglishLanguageFormatter
import com.noamtu.jewishday.core.i18n.HebrewLanguageFormatter
import com.noamtu.jewishday.core.i18n.gematriaLetters
import com.noamtu.jewishday.core.i18n.gematriaValue
import com.noamtu.jewishday.core.i18n.hebrewNumber
import java.time.LocalDate
import java.util.zip.GZIPInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The five daily-learning tracks worked out on the phone, against what Hebcal published for every day
 * of 2026-2028 and for a few days around each cycle's start (in Israel and abroad; see the fixture's
 * header). For each day the test asks: does the app have a reading exactly when Hebcal does, and does it
 * read the same in English and in Hebrew?
 *
 * The expected text is built from Hebcal's own fields, not from the app's tables: English is Hebcal's
 * reference as it is (its title, or for Tanakh Yomi and Shemirat HaLashon its memo, which is what the
 * app showed from it), and Hebrew is Hebcal's Hebrew re-punctuated to the app's convention — chapters as
 * punctuated numerals, what is inside them as bare letters ("כלים ו׳: ד - ז׳: א"). Where Hebcal's data
 * has a typo the app's copy corrects, the correction is listed in [HebcalTypos] and the test expects it.
 */
class HebcalLearningComparisonTest {
    private class Row(val date: LocalDate, val where: String, val category: String, val title: String, val hebrew: String, val memo: String)

    private val rows: List<Row> = run {
        val stream = checkNotNull(javaClass.getResourceAsStream("/hebcal-learning.tsv.gz"))
        GZIPInputStream(stream).bufferedReader().useLines { lines ->
            lines.filterNot { it.startsWith("#") }.map { line ->
                val f = line.split('\t')
                Row(LocalDate.parse(f[0]), f[1], f[2], f[3], f[4], f[5])
            }.toList()
        }
    }

    private val english = EnglishLanguageFormatter()
    private val hebrew = HebrewLanguageFormatter()

    @Test
    fun everyDayMatchesHebcalInBothLanguagesInIsraelAndAbroad() {
        val mismatches = mutableListOf<String>()
        var compared = 0
        for ((key, dayRows) in rows.groupBy { it.date to it.where }) {
            val (date, where) = key
            val hebcal = dayRows.associateBy { it.category }
            for (category in Categories) {
                val text = calendarText(category, date)
                val row = hebcal[category]
                if (row == null || text == null) {
                    if (row != null || text != null) {
                        mismatches += "$date $where $category: Hebcal ${row?.title ?: "has none"}, app ${text?.let(english::format) ?: "has none"}"
                    }
                    continue
                }
                val (expectedEnglish, expectedHebrew) = expected(row)
                val actualEnglish = english.format(text)
                val actualHebrew = hebrew.format(text)
                compared++
                if (actualEnglish != expectedEnglish) mismatches += "$date $where $category en: expected $expectedEnglish, was $actualEnglish"
                if (actualHebrew != expectedHebrew) mismatches += "$date $where $category he: expected $expectedHebrew, was $actualHebrew"
            }
        }
        assertEquals(mismatches.take(40).joinToString("\n"), 0, mismatches.size)
        // 3 years x 2 places x 5 tracks (6 rows a day, less Tanakh Yomi's and the Kitzur's days off),
        // plus the start-of-cycle days.
        assertTrue("compared only $compared readings", compared > 12_500)
    }

    @Test
    fun theSampleCoversEveryCycleStartAndWrap() {
        fun reading(category: String, date: String) = calendarText(category, LocalDate.parse(date))?.let(english::format)
        // The first day of each cycle.
        assertEquals("Berakhot 1:1-2", reading("mishnayomi", "1947-05-20"))
        assertEquals("Transmission of the Oral Law 1-21", reading("dailyRambam1", "1984-04-29"))
        assertEquals("Transmission of the Oral Law 1-45", reading("dailyRambam3", "1984-04-29"))
        assertEquals("Joshua 1:1-3:6", reading("tanakhYomi", "1948-10-26"))
        assertEquals("Book I, The Gate of Discerning 16.13", reading("shemiratHaLashon", "1876-02-16"))
        // And nothing the day before.
        assertEquals(null, reading("mishnayomi", "1947-05-19"))
        assertEquals(null, reading("dailyRambam1", "1984-04-28"))
        assertEquals(null, reading("tanakhYomi", "1948-10-25"))
        assertEquals(null, reading("shemiratHaLashon", "1876-02-15"))
        // A wrap inside the sample: the last day of a cycle, then the first of the next.
        assertEquals("Oktzin 3:11-12", reading("mishnayomi", "2027-09-20"))
        assertEquals("Berakhot 1:1-2", reading("mishnayomi", "2027-09-21"))
        assertEquals("Kings and Wars 12", reading("dailyRambam1", "2026-02-02"))
        assertEquals("Transmission of the Oral Law 1-21", reading("dailyRambam1", "2026-02-03"))
        assertEquals("Transmission of the Oral Law 1-45", reading("dailyRambam3", "2027-01-08"))
        assertEquals("Joshua 1:1-3:6", reading("tanakhYomi", "2027-10-24"))
        assertEquals("Book I, Introduction 1-2", reading("shemiratHaLashon", "2027-10-02"))
        assertEquals("1:1-4", reading("kitzurShulchanAruch", "2027-10-25"))
        // Each of those dates is in the Hebcal sample, so the comparison above checked it too.
        val sampled = rows.map { it.date.toString() }.toSet()
        listOf("1947-05-20", "1984-04-29", "1948-10-26", "1876-02-16", "2027-09-21", "2026-02-03", "2027-01-08", "2027-10-24", "2027-10-02")
            .forEach { assertTrue(it, it in sampled) }
    }

    @Test
    fun everyDayForTwoHundredYearsHasItsReadings() {
        // No table runs out and no year shape is left unplanned, from 1900 to 2100.
        var date = LocalDate.of(1900, 1, 1)
        while (date.year < 2100) {
            for (category in Categories) calendarText(category, date)?.let { english.format(it) + hebrew.format(it) }
            if (date.year >= 1985) {
                assertTrue("$date", listOf("mishnayomi", "dailyRambam1", "dailyRambam3", "shemiratHaLashon").all { calendarText(it, date) != null })
            }
            date = date.plusDays(1)
        }
    }

    private fun calendarText(category: String, date: LocalDate): CalendarText? = when (category) {
        "mishnayomi" -> mishnahYomi(date)?.let { CalendarText.MishnahYomi(date) }
        "dailyRambam1" -> rambamYomi(date, 1)?.let { CalendarText.RambamYomi(date, 1) }
        "dailyRambam3" -> rambamYomi(date, 3)?.let { CalendarText.RambamYomi(date, 3) }
        "tanakhYomi" -> tanakhYomi(date)?.let { CalendarText.TanakhYomi(date) }
        "shemiratHaLashon" -> shemiratHaLashon(date)?.let { CalendarText.ShemiratHaLashon(date) }
        "kitzurShulchanAruch" -> kitzurShulchanAruch(date)?.let { CalendarText.KitzurShulchanAruch(date) }
        else -> error(category)
    }

    /** What the app should show for Hebcal's [row], in English and in Hebrew. */
    private fun expected(row: Row): Pair<String, String> {
        val (title, hebrewText) = HebcalTypos[row.title] ?: (row.title to row.hebrew)
        return when (row.category) {
            "mishnayomi" -> title to hebrewMishnah(hebrewText)
            "dailyRambam1", "dailyRambam3" -> title to hebrewText.split(", ").joinToString(", ", transform = ::hebrewRambam)
            "tanakhYomi" -> row.memo to hebrewTanakh(hebrewText)
            "shemiratHaLashon" -> if ("Footnote" in title) {
                "Book II 19.11 (footnote)" to "חלק ב׳ י״ט: יא (הערה)"
            } else {
                row.memo to hebrewShemirat(hebrewText)
            }
            "kitzurShulchanAruch" -> title.replace(Regex("(?<=[-:])E\\b"), "end") to hebrewKitzur(hebrewText)
            else -> error(row.category)
        }
    }

    private fun number(token: String): Int = token.toIntOrNull() ?: gematriaValue(token) ?: error("not a number: $token")

    /** "בכורות 9:5-6" → "בכורות ט׳: ה-ו"; "כלים 6:4-7:1" → "כלים ו׳: ד - ז׳: א"; across tractates likewise. */
    private fun hebrewMishnah(text: String): String = hebrewRange(Regex("^(.+?) (\\d+):(\\d+)-(?:(\\D+?) )?(?:(\\d+):)?(\\d+)$"), text, text)

    /** Hebcal's "מלכים ס׳ ו (מלכים א׳ ז:כא-ח:י)": the verses in the brackets, under the app's book names. */
    private fun hebrewTanakh(text: String): String {
        val seder = text.substringBefore(" ס׳")
        val verses = text.substringAfter('(').substringBefore(')')
        val withBook = if (verses.first().isLetter() && verses.substringBefore(':').contains(' ')) verses else "$seder $verses"
        return hebrewRange(Regex("^(.+?) ([א-ת]+):([א-ת]+)-(?:(.+?) )?(?:([א-ת]+):)?([א-ת]+)$"), withBook, text) { book ->
            HebcalTanakhBooks[book] ?: book
        }
    }

    private fun hebrewRange(pattern: Regex, text: String, original: String, bookName: (String) -> String = { it }): String {
        val match = pattern.find(text) ?: error("unexpected Hebcal Hebrew: $original")
        val (book, chapter, first, endBook, endChapter, last) = match.destructured
        val start = "${bookName(book)} ${hebrewNumber(number(chapter))}: ${gematriaLetters(number(first))}"
        val end = gematriaLetters(number(last))
        return when {
            endBook.isNotEmpty() -> "$start - ${bookName(endBook)} ${hebrewNumber(number(endChapter))}: $end"
            endChapter.isNotEmpty() && endChapter != chapter -> "$start - ${hebrewNumber(number(endChapter))}: $end"
            else -> "$start-$end"
        }
    }

    /** "הלכות ממרים פרק א" → "הלכות ממרים פרק א׳"; "… פרק 10-12" → "… פרקים י׳-י״ב"; "1:1-4:8" → "א׳: א - ד׳: ח". */
    private fun hebrewRambam(part: String): String {
        val match = Regex("^(.*?) פרק(?:ים)? (\\S+)$").find(part) ?: error("unexpected Hebcal Rambam: $part")
        val (name, chapters) = match.destructured
        fun place(token: String): String = token.split(':').let { pieces ->
            if (pieces.size == 1) hebrewNumber(number(token)) else "${hebrewNumber(number(pieces[0]))}: ${gematriaLetters(number(pieces[1]))}"
        }
        val ends = chapters.split('-')
        return when {
            ends.size == 1 -> "$name פרק ${place(chapters)}"
            ':' in chapters -> "$name פרקים ${place(ends[0])} - ${place(ends[1])}"
            else -> "$name פרקים ${place(ends[0])}-${place(ends[1])}"
        }
    }

    /** "Book I, שער התבונה 8.4-8.5" → "חלק א׳ שער התבונה ח׳: ד-ה"; "Book II 3.1-3.2" → "חלק ב׳ ג׳: א-ב". */
    private fun hebrewShemirat(text: String): String {
        val book = if (text.startsWith("Book II")) 2 else 1
        val rest = text.removePrefix(if (book == 2) "Book II" else "Book I").removePrefix(",").trim()
        val gate = rest.substringBeforeLast(' ', missingDelimiterValue = "").trim()
        val reference = rest.substringAfterLast(' ')
        val ends = reference.split('-').map { place -> place.split('.').map(String::toInt) }
        val referenceText = when {
            ends[0].size == 1 -> ends.joinToString("-") { gematriaLetters(it[0]) }
            ends.size == 1 -> "${hebrewNumber(ends[0][0])}: ${gematriaLetters(ends[0][1])}"
            ends[0][0] == ends[1][0] -> "${hebrewNumber(ends[0][0])}: ${gematriaLetters(ends[0][1])}-${gematriaLetters(ends[1][1])}"
            else -> "${hebrewNumber(ends[0][0])}: ${gematriaLetters(ends[0][1])} - ${hebrewNumber(ends[1][0])}: ${gematriaLetters(ends[1][1])}"
        }
        return listOf("חלק ${hebrewNumber(book)}", gate, referenceText).filter(String::isNotBlank).joinToString(" ")
    }

    /** "מ:ה-יג" → "מ׳: ה-יג"; "קלד:יג-קלה:ב" → "קל״ד: יג - קל״ה: ב"; "-סוף" stays; the Adar II pair one by one. */
    private fun hebrewKitzur(text: String): String {
        if (" / " in text) {
            val (first, second) = text.split(" / ")
            val firstName = "הלכות שמיטה ותרו״מ "
            val secondName = "הלכות ברכות ותפלה "
            return firstName + hebrewKitzur(first.removePrefix(firstName)) + " / " + secondName + hebrewKitzur(second.removePrefix(secondName))
        }
        if ('-' !in text) return text
        val (start, end) = text.split('-')
        fun piece(token: String) = if (token == "סוף") token else gematriaLetters(number(token))
        fun place(token: String): String {
            if (':' !in token) return piece(token)
            val (siman, seif) = token.split(':')
            val simanText = if (siman == "שמיטה") siman else hebrewNumber(number(siman))
            return "$simanText: ${piece(seif)}"
        }
        val startSiman = start.substringBefore(':', missingDelimiterValue = "")
        return when {
            ':' !in end -> "${place(start)}-${piece(end)}"
            end.substringBefore(':') == startSiman -> "${place(start)}-${piece(end.substringAfter(':'))}"
            else -> "${place(start)} - ${place(end)}"
        }
    }

    private companion object {
        val Categories = listOf("mishnayomi", "dailyRambam1", "dailyRambam3", "tanakhYomi", "shemiratHaLashon", "kitzurShulchanAruch")

        // Hebcal's Hebrew book names where the app writes them differently.
        val HebcalTanakhBooks = mapOf(
            "ישעיהו" to "ישעיה",
            "ירמיהו" to "ירמיה",
            "שמואל א׳" to "שמואל א",
            "שמואל ב׳" to "שמואל ב",
            "מלכים א׳" to "מלכים א",
            "מלכים ב׳" to "מלכים ב",
            "דברי הימים א׳" to "דברי הימים א",
            "דברי הימים ב׳" to "דברי הימים ב",
        )

        // Typos in Hebcal's Kitzur calendar that the app's copy corrects (see LearningScheduleData.kt):
        // Hebcal's title → the title and Hebrew it should have had.
        val HebcalTypos = mapOf(
            "5:9-6:16" to ("5:9-16" to "ה:ט-טז"),
            "6:17-3" to ("5:17-6:3" to "ה:יז-ו:ג"),
            "133:27-4:1" to ("133:27-134:1" to "קלג:כז-קלד:א"),
            "98:33-32" to ("98:23-32" to "צח:כג-לב"),
            "80:267-35" to ("80:27-35" to "פ:כז-לה"),
            "123:3-1234:3" to ("123:3-124:3" to "קכג:ג-קכד:ג"),
            "Hilchot Shmita v'Terumah 32-34 / Hilchot Brachot v'Tefilah 11:1-17:7" to
                ("Hilchot Shmita v'Terumah 32-34 / Hilchot Brachot v'Tefilah 17:1-7" to "הלכות שמיטה ותרו״מ לב-לד / הלכות ברכות ותפלה יז:א-ז"),
            "Hilchot Shmita v'Terumah 35-E / Hilchot Brachot v'Tefilah 17-8:18" to
                ("Hilchot Shmita v'Terumah 35-E / Hilchot Brachot v'Tefilah 17:8-18:2" to "הלכות שמיטה ותרו״מ לה-סוף / הלכות ברכות ותפלה יז:ח-יח:ב"),
            "Hilchot Shmita v'Terumah 54-46 / Hilchot Brachot v'Tefilah 53:3-54:3" to
                ("Hilchot Shmita v'Terumah 54-56 / Hilchot Brachot v'Tefilah 53:3-54:3" to "הלכות שמיטה ותרו״מ נד-נו / הלכות ברכות ותפלה נג:ג-נד:ג"),
            "Hilchot Shmita v'Terumah 62-66 / Hilchot Brachot v'Tefilah 55:2-56" to
                ("Hilchot Shmita v'Terumah 62-66 / Hilchot Brachot v'Tefilah 55:2-56:4" to "הלכות שמיטה ותרו״מ סב-סו / הלכות ברכות ותפלה נה:ב-נו:ד"),
        )
    }
}
