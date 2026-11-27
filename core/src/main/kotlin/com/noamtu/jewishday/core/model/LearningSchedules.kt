// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

// The daily-learning schedules worked out on the phone: Mishnah Yomi, the two Rambam tracks, Tanakh
// Yomi, Shemirat HaLashon and the Kitzur Shulchan Aruch. Each is its published cycle start (or Hebrew
// calendar) and the order of its units; the units themselves are in LearningScheduleData.kt. What is
// read is worked out here; how it is written is each language's formatter's business.

// ---- Mishnah Yomi: two mishnayot a day, through the whole Mishnah in order. ----

internal data class Tractate(val english: String, val hebrew: String, val mishnayot: List<Int>)

internal data class MishnahPlace(val tractate: Tractate, val chapter: Int, val mishnah: Int)

/** The day's two mishnayot, [first] then [second] (which may be in the next chapter or tractate). */
internal data class MishnahYomiReading(val first: MishnahPlace, val second: MishnahPlace)

private val MishnahTractates: List<Tractate> by lazy {
    tableRows(MishnahTractateTable).map { (english, hebrew, counts) ->
        Tractate(english, hebrew, counts.split(',').map(String::toInt))
    }
}

private val MishnahPlaces: List<MishnahPlace> by lazy {
    MishnahTractates.flatMap { tractate ->
        tractate.mishnayot.flatMapIndexed { index, count ->
            (1..count).map { mishnah -> MishnahPlace(tractate, index + 1, mishnah) }
        }
    }
}

// The Mishnah Yomi cycle began on 20 May 1947.
private val MishnahYomiStart: LocalDate = LocalDate.of(1947, 5, 20)

internal fun mishnahYomi(date: LocalDate): MishnahYomiReading? {
    val elapsed = ChronoUnit.DAYS.between(MishnahYomiStart, date)
    if (elapsed < 0) return null
    val day = (elapsed % (MishnahPlaces.size / 2)).toInt()
    return MishnahYomiReading(MishnahPlaces[day * 2], MishnahPlaces[day * 2 + 1])
}

// ---- Rambam Yomi: the Mishneh Torah a chapter a day, or three. ----

internal data class MishnehTorahBook(val english: String, val hebrew: String, val chapters: Int)

/**
 * One book's share of a day's Rambam: from [first] to [last], each a chapter ("8") or, in the four
 * introductory books, where the day's portion is a run of mitzvot ("84") or of chapter:halacha ("5:1").
 * [first] equals [last] when the share is a single chapter.
 */
internal data class RambamPart(val book: MishnehTorahBook, val first: String, val last: String)

private val MishnehTorahBooks: List<MishnehTorahBook> by lazy {
    tableRows(MishnehTorahTable).map { (english, hebrew, chapters) -> MishnehTorahBook(english, hebrew, chapters.toInt()) }
}

// The four introductory books are not read by chapter; each is cut into three days' portions.
private val IntroductionPortions = listOf(
    listOf("1-21", "22-33", "34-45"), // Transmission of the Oral Law
    listOf("1-83", "84-166", "167-248"), // Positive Mitzvot
    listOf("1-122", "123-245", "246-365"), // Negative Mitzvot
    listOf("1:1-4:8", "5:1-9:9", "10:1-14:10"), // Overview of Mishneh Torah Contents
)

private const val OrderOfPrayer = "The Order of Prayer"
private const val LeavenedBread = "Leavened and Unleavened Bread"

// Both tracks began on Sunday 27 Nisan 5744 (29 April 1984); the one-chapter cycle is 1017 days, and
// the three-chapter cycle reads the same units in 339.
private val RambamStart: LocalDate = LocalDate.of(1984, 4, 29)

/**
 * The day's Rambam for the one- or three-chapter track ([chaptersPerDay] 1 or 3): one [RambamPart] per
 * book the day touches.
 */
internal fun rambamYomi(date: LocalDate, chaptersPerDay: Int): List<RambamPart>? {
    val elapsed = ChronoUnit.DAYS.between(RambamStart, date)
    if (elapsed < 0) return null
    val units = if (chaptersPerDay == 3) RambamThreeChapterUnits else RambamOneChapterUnits
    val day = (elapsed % (units.size / chaptersPerDay)).toInt()
    return units.subList(day * chaptersPerDay, (day + 1) * chaptersPerDay).fold(emptyList()) { parts, unit ->
        val previous = parts.lastOrNull()
        if (previous != null && previous.book == unit.book) parts.dropLast(1) + previous.copy(last = unit.last) else parts + unit
    }
}

private val RambamOneChapterUnits: List<RambamPart> by lazy { rambamUnits(chaptersPerDay = 1) }
private val RambamThreeChapterUnits: List<RambamPart> by lazy { rambamUnits(chaptersPerDay = 3) }

/**
 * The track's 1017 days' units in order. Each track counts them its own way: the one-chapter track
 * reads the last chapter of The Order of Prayer with the one before it, and the three-chapter track
 * gives that chapter a place of its own, while reading the Haggadah (Leavened Bread's ninth) with
 * chapter eight.
 */
private fun rambamUnits(chaptersPerDay: Int): List<RambamPart> = MishnehTorahBooks.flatMapIndexed { index, book ->
    val chapters = when {
        chaptersPerDay == 3 && book.english == OrderOfPrayer -> book.chapters + 1
        chaptersPerDay == 3 && book.english == LeavenedBread -> book.chapters - 1
        else -> book.chapters
    }
    (1..chapters).map { chapter -> rambamUnit(index, book, chapter, chaptersPerDay) }
}

private fun rambamUnit(bookIndex: Int, book: MishnehTorahBook, chapter: Int, chaptersPerDay: Int): RambamPart {
    IntroductionPortions.getOrNull(bookIndex)?.let { portions ->
        val (first, last) = portions[chapter - 1].split('-')
        return RambamPart(book, first, last)
    }
    val last = when {
        chaptersPerDay == 1 && book.english == OrderOfPrayer && chapter == 4 -> 5
        chaptersPerDay == 3 && book.english == LeavenedBread && chapter == 8 -> 9
        else -> chapter
    }
    return RambamPart(book, chapter.toString(), last.toString())
}

// ---- Tanakh Yomi: Nach by its Masoretic sedarim, a seder each weekday, starting each 23 Tishrei. ----

/** The day's seder of Nach, as its verses ("I Kings 7:21-8:10", "Hosea 14:6-Joel 2:26"). */
internal data class TanakhYomiReading(val verses: String)

private val TanakhSedarim: Map<Pair<String, String>, String> by lazy {
    tableRows(TanakhSedarimTable).associate { (book, seder, verses) -> (book to seder) to verses }
}

private const val Joshua = "Joshua"
private const val Jeremiah = "Jeremiah"
private const val SongOfSongs = "Song of Songs"
private const val Ruth = "Ruth"
private const val Chronicles = "Chronicles"

// The books in the order they are read. Chronicles comes twice: a leap year's extra month reads it
// over again before the cycle turns.
private val TanakhYomiBooks = listOf(
    Joshua, "Judges", "Samuel", "Kings", "Isaiah", Jeremiah, "Ezekiel", "Minor Prophets", "Psalms",
    "Proverbs", "Job", SongOfSongs, Ruth, "Lamentations", "Ecclesiastes", "Esther", "Daniel",
    "Ezra and Nehemiah", Chronicles, Chronicles,
)

// The sedarim before the second reading of Chronicles: what a cycle reads in a year with no spare days.
private const val TanakhYomiBaseDays = 293

// The program's first cycle began on 23 Tishrei 5709 (26 October 1948).
private val TanakhYomiStart: LocalDate = LocalDate.of(1948, 10, 26)

internal fun tanakhYomi(date: LocalDate): TanakhYomiReading? {
    if (date < TanakhYomiStart) return null
    val hebrew = JewishDate(date)
    if (!isTanakhYomiDay(hebrew, date.dayOfWeek)) return null
    val year = hebrew.jewishYear
    val cycleStart = hebrewDate(year, JewishDate.TISHREI, 23)
    if (date < cycleStart) return endOfPreviousCycle(year, date)

    var seder = tanakhReadingDays(cycleStart, date)
    val plan = tanakhYomiPlan(year) ?: return null
    for (book in TanakhYomiBooks.indices) {
        val count = plan.sedarim[book]
        if (seder < count) return TanakhYomiReading(tanakhVerses(TanakhYomiBooks[book], plan.sederName(TanakhYomiBooks[book], seder + 1)))
        seder -= count
    }
    return null
}

/** From Rosh Hashana to Shemini Atzeret the old cycle finishes Chronicles, from its tenth seder or later. */
private fun endOfPreviousCycle(year: Int, date: LocalDate): TanakhYomiReading? {
    val roshHashana = hebrewDate(year, JewishDate.TISHREI, 1)
    val first = when (roshHashana.dayOfWeek) {
        DayOfWeek.THURSDAY -> 11
        DayOfWeek.SATURDAY -> 10
        else -> 12
    }
    val seder = first + tanakhReadingDays(roshHashana.plusDays(2), date)
    return if (seder > 25) null else TanakhYomiReading(tanakhVerses(Chronicles, seder.toString()))
}

/**
 * How [year]'s cycle stretches to fill its days: a year with more weekdays than sedarim splits a
 * seder or two across two days — Ruth first, then Song of Songs, Jeremiah's ninth, Joshua's fourth.
 */
private class TanakhYomiPlan(val sedarim: List<Int>, private val split: Set<String>) {
    fun sederName(book: String, day: Int): String = when {
        book !in split -> day.toString()
        book == Ruth || book == SongOfSongs -> "1.$day"
        book == Joshua -> splitSeder(day, 4)
        else -> splitSeder(day, 9)
    }

    private fun splitSeder(day: Int, at: Int): String = when {
        day < at -> day.toString()
        day == at -> "$at.1"
        day == at + 1 -> "$at.2"
        else -> (day - 1).toString()
    }
}

private fun tanakhYomiPlan(year: Int): TanakhYomiPlan? {
    val days = tanakhReadingDays(hebrewDate(year, JewishDate.TISHREI, 23), hebrewDate(year + 1, JewishDate.TISHREI, 23))
    val spare = (if (JewishDate.isJewishLeapYear(year)) days - 25 else days) - TanakhYomiBaseDays
    if (spare !in 0..4) return null
    val split = listOf(Ruth, SongOfSongs, Jeremiah, Joshua).take(spare).toSet()
    val sedarim = TanakhYomiBooks.map { book ->
        TanakhSedarim.keys.count { (name, seder) -> name == book && '.' !in seder } + if (book in split) 1 else 0
    }
    return TanakhYomiPlan(sedarim, split)
}

private fun tanakhVerses(book: String, seder: String): String {
    val verses = TanakhSedarim.getValue(book to seder)
    return if (verses.first().isDigit()) "$book $verses" else verses
}

/** Not a reading day: Shabbat, and the days Israel keeps as Yom Tov, Tisha B'Av, Purim, Yom Ha'atzmaut. */
private fun isTanakhYomiDay(hebrew: JewishDate, dayOfWeek: DayOfWeek): Boolean {
    if (dayOfWeek == DayOfWeek.SATURDAY) return false
    val day = hebrew.jewishDayOfMonth
    val skipped = when (hebrew.jewishMonth) {
        JewishDate.TISHREI -> day == 1 || day == 2 || day == 10 || day == 15 || day == 22
        JewishDate.NISSAN -> day == 15 || day == 21
        JewishDate.IYAR -> day == yomHaatzmautDay(hebrew.jewishYear)
        JewishDate.SIVAN -> day == 6
        JewishDate.AV -> day == 9 || (day == 10 && dayOfWeek == DayOfWeek.SUNDAY)
        JewishDate.ADAR -> day == 14 && !hebrew.isJewishLeapYear
        JewishDate.ADAR_II -> day == 14
        else -> false
    }
    return !skipped
}

/** The day of Iyar Yom Ha'atzmaut is kept on, moved off the days next to Shabbat. */
private fun yomHaatzmautDay(year: Int): Int = when (hebrewDate(year, JewishDate.NISSAN, 15).dayOfWeek) {
    DayOfWeek.SUNDAY -> 3
    DayOfWeek.SATURDAY -> 4
    DayOfWeek.TUESDAY -> if (year >= 5764) 6 else 5
    else -> 5
}

private val TanakhYearCache = ConcurrentHashMap<Int, IntArray>()

/** The reading days from [from] up to (not including) [until]. */
private fun tanakhReadingDays(from: LocalDate, until: LocalDate): Int {
    var count = 0
    var start = from
    while (start < until) {
        val year = JewishDate(start).jewishYear
        val roshHashana = hebrewDate(year, JewishDate.TISHREI, 1)
        val prefix = TanakhYearCache.getOrPut(year) { tanakhYearPrefix(roshHashana, JewishDate.getDaysInJewishYear(year)) }
        val stop = minOf(until, roshHashana.plusDays((prefix.size - 1).toLong()))
        count += prefix[ChronoUnit.DAYS.between(roshHashana, stop).toInt()] - prefix[ChronoUnit.DAYS.between(roshHashana, start).toInt()]
        start = stop
    }
    return count
}

/** prefix[i] = how many of the year's first i days are reading days. */
private fun tanakhYearPrefix(roshHashana: LocalDate, length: Int): IntArray {
    val prefix = IntArray(length + 1)
    for (i in 0 until length) {
        val date = roshHashana.plusDays(i.toLong())
        prefix[i + 1] = prefix[i] + if (isTanakhYomiDay(JewishDate(date), date.dayOfWeek)) 1 else 0
    }
    return prefix
}

// ---- Shemirat HaLashon: a yearly calendar by Hebrew date. ----

internal enum class ShemiratHaLashonSection { Introduction, Remembering, Discerning, Torah, Epilogue, Chapters }

/** The day's piece of Shemirat HaLashon: book (1 or 2), section, and its first and last "perek.halacha". */
internal data class ShemiratHaLashonReading(
    val book: Int,
    val section: ShemiratHaLashonSection,
    val first: String,
    val last: String,
)

private class ShemiratHaLashonRow(
    val plainDay: Int,
    val plainMonth: Int,
    val leapDay: Int,
    val leapMonth: Int,
    val reading: ShemiratHaLashonReading,
)

private val ShemiratHaLashonRows: List<ShemiratHaLashonRow> by lazy {
    tableRows(ShemiratHaLashonTable).map { row ->
        val section = when (row[5]) {
            "H" -> ShemiratHaLashonSection.Introduction
            "Z" -> ShemiratHaLashonSection.Remembering
            "V" -> ShemiratHaLashonSection.Discerning
            "T" -> ShemiratHaLashonSection.Torah
            "E" -> ShemiratHaLashonSection.Epilogue
            else -> ShemiratHaLashonSection.Chapters
        }
        ShemiratHaLashonRow(
            plainDay = row[0].toInt(),
            plainMonth = row[1].toInt(),
            leapDay = row[2].toInt(),
            leapMonth = row[3].toInt(),
            reading = ShemiratHaLashonReading(row[4].toInt(), section, row[6], row[7].ifEmpty { row[6] }),
        )
    }
}

// The sefer was finished on 21 Shevat 5636 (1876); the calendar starts there.
private val ShemiratHaLashonStart: LocalDate = hebrewDate(5636, JewishDate.SHEVAT, 21)

internal fun shemiratHaLashon(date: LocalDate): ShemiratHaLashonReading? {
    if (date < ShemiratHaLashonStart) return null
    val hebrew = JewishDate(date)
    val day = hebrew.jewishDayOfMonth
    val month = hebrew.jewishMonth
    val reading = shemiratHaLashonOn(day, month, hebrew.isJewishLeapYear) ?: return null
    // A 29-day Cheshvan or Kislev reads its missing 30th on the 29th.
    val shortMonth = (month == JewishDate.CHESHVAN && !hebrew.isCheshvanLong) || (month == JewishDate.KISLEV && hebrew.isKislevShort)
    if (day == 29 && shortMonth) {
        shemiratHaLashonOn(30, month, hebrew.isJewishLeapYear)?.let { return reading.copy(last = it.last) }
    }
    return reading
}

/** Every row of one date read together: the first row's start through the last row's end. */
private fun shemiratHaLashonOn(day: Int, month: Int, leapYear: Boolean): ShemiratHaLashonReading? {
    val rows = ShemiratHaLashonRows.filter { row ->
        if (leapYear) row.leapDay == day && row.leapMonth == month else row.plainDay == day && row.plainMonth == month
    }
    if (rows.isEmpty()) return null
    return rows.first().reading.copy(last = rows.last().reading.last)
}

// ---- Kitzur Shulchan Aruch: a yearly calendar by Hebrew date. ----

/**
 * The day's Kitzur Shulchan Aruch, as "siman:seif-siman:seif" ("E" for the end of the siman). In a leap
 * year's Adar II the cycle pauses: [reading] is then the Blum edition's Hilchot Shmita v'Terumah, and
 * [alternative] the review of Hilchot Brachot v'Tefilah offered beside it.
 */
internal data class KitzurReading(val reading: String, val alternative: String? = null)

private val KitzurByDate: Map<Pair<String, Int>, String> by lazy {
    tableRows(KitzurTable).associate { (month, day, reading) -> (month to day.toInt()) to reading }
}

internal fun kitzurShulchanAruch(date: LocalDate): KitzurReading? {
    val hebrew = JewishDate(date)
    val day = hebrew.jewishDayOfMonth
    return when (val month = hebrew.jewishMonth) {
        JewishDate.ADAR_II -> {
            val first = KitzurByDate["AdarIIOptionA" to day] ?: return null
            KitzurReading(first, KitzurByDate["AdarIIOptionB" to day])
        }
        else -> KitzurByDate[KitzurMonthKeys.getValue(month) to day]?.let(::KitzurReading)
    }
}

// A 30-day Cheshvan or Adar I has no reading on the 30th: the table has 29 days for each.
private val KitzurMonthKeys = mapOf(
    JewishDate.TISHREI to "Tishrei", JewishDate.CHESHVAN to "Cheshvan", JewishDate.KISLEV to "Kislev",
    JewishDate.TEVES to "Tevet", JewishDate.SHEVAT to "Shevat", JewishDate.ADAR to "Adar",
    JewishDate.NISSAN to "Nisan", JewishDate.IYAR to "Iyar", JewishDate.SIVAN to "Sivan",
    JewishDate.TAMMUZ to "Tammuz", JewishDate.AV to "Av", JewishDate.ELUL to "Elul",
)

// ---- Helpers ----

private fun hebrewDate(year: Int, month: Int, day: Int): LocalDate = JewishDate(year, month, day).localDate

/** The rows of one of LearningScheduleData's "|"-separated tables. */
private fun tableRows(table: String): List<List<String>> =
    table.lineSequence().filter(String::isNotBlank).map { it.split('|') }.toList()
