// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import com.noamtu.jewishday.core.i18n.CalendarText
import com.noamtu.jewishday.core.i18n.EnglishLanguageFormatter
import com.noamtu.jewishday.core.i18n.HebrewLanguageFormatter
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sweeps 2026–2035 across Israeli and diaspora cities and checks the calendar the app shows against
 * the rules themselves: which days are Yom Tov where, how a run of them is entered and left, when
 * each fast is and when it begins and ends, the parsha split abroad, the Omer, Chanukah, Rosh
 * Chodesh, the modern days' weekday shifts, leap years, the date line and the far north.
 *
 * The expected dates are worked out here from the Hebrew-date rules (KosherJava is used only to turn
 * a civil date into a Hebrew one), so a slip in what the app derives from them would show.
 */
class HalachicCalendarSweepTest {
    private val settings = ZmanimCalculationSettings()

    private fun city(name: String, latitude: Double, longitude: Double, elevation: Double, zone: String) =
        JewishLocation(name, latitude, longitude, elevation, ZoneId.of(zone))

    private val jerusalem = defaultJerusalemLocation
    private val telAviv = city("Tel Aviv", 32.0853, 34.7818, 5.0, "Asia/Jerusalem")
    private val newYork = city("New York", 40.7128, -74.0060, 10.0, "America/New_York")
    private val london = city("London", 51.5074, -0.1278, 11.0, "Europe/London")
    private val losAngeles = city("Los Angeles", 34.0522, -118.2437, 89.0, "America/Los_Angeles")
    private val melbourne = city("Melbourne", -37.8136, 144.9631, 31.0, "Australia/Melbourne")
    private val helsinki = city("Helsinki", 60.1699, 24.9384, 17.0, "Europe/Helsinki")
    private val reykjavik = city("Reykjavik", 64.1466, -21.9426, 20.0, "Atlantic/Reykjavik")
    private val cities = listOf(jerusalem, telAviv, newYork, london, losAngeles, melbourne, helsinki, reykjavik)

    private val days: List<LocalDate> =
        generateSequence(LocalDate.of(2026, 1, 1)) { it.plusDays(1) }.takeWhile { it.year <= 2035 }.toList()

    private fun hebrew(date: LocalDate) = JewishDate(date)
    private fun noon(location: JewishLocation, date: LocalDate): Instant = date.atTime(12, 0).atZone(location.zoneId).toInstant()
    private fun calendar(location: JewishLocation, date: LocalDate) = comprehensiveZmanimCalendar(location, date, settings)

    // ---- Yom Tov, Israel and abroad ----

    /** The melacha-forbidden Yom Tov days, by Hebrew date: Israel keeps one day, the diaspora two. */
    private fun isYomTov(date: LocalDate, inIsrael: Boolean): Boolean {
        val h = hebrew(date)
        val day = h.jewishDayOfMonth
        return when (h.jewishMonth) {
            JewishDate.TISHREI -> day in if (inIsrael) setOf(1, 2, 10, 15, 22) else setOf(1, 2, 10, 15, 16, 22, 23)
            JewishDate.NISSAN -> day in if (inIsrael) setOf(15, 21) else setOf(15, 16, 21, 22)
            JewishDate.SIVAN -> day in if (inIsrael) setOf(6) else setOf(6, 7)
            else -> false
        }
    }

    @Test
    fun yomTovIsOneDayInIsraelAndTwoAbroadButRoshHashanaIsTwoEverywhere() {
        for (date in days) {
            for (inIsrael in listOf(true, false)) {
                assertEquals("$date inIsrael=$inIsrael", isYomTov(date, inIsrael), jewishCalendar(date, inIsrael).isYomTovAssurBemelacha)
            }
        }
        // Rosh Hashana 5787 is Shabbat 12 and Sunday 13 September 2026, in Jerusalem as in New York.
        for (date in listOf(LocalDate.of(2026, 9, 12), LocalDate.of(2026, 9, 13))) {
            assertTrue(jewishCalendar(date, inIsrael = true).isRoshHashana)
            assertTrue(jewishCalendar(date, inIsrael = false).isRoshHashana)
        }
    }

    @Test
    fun isruChagIsTheDayAfterTheLastYomTovDayAndIsNamed() {
        for (date in days) {
            for (inIsrael in listOf(true, false)) {
                val h = hebrew(date)
                val expected = when (h.jewishMonth) {
                    JewishDate.TISHREI -> h.jewishDayOfMonth == if (inIsrael) 23 else 24
                    JewishDate.NISSAN -> h.jewishDayOfMonth == if (inIsrael) 22 else 23
                    JewishDate.SIVAN -> h.jewishDayOfMonth == if (inIsrael) 7 else 8
                    else -> false
                }
                val jc = jewishCalendar(date, inIsrael)
                assertEquals("$date inIsrael=$inIsrael", expected, jc.yomTovIndex == JewishCalendar.ISRU_CHAG)
                if (expected) assertNotNull("$date: Isru Chag is named", minorDayName(jc, date, inIsrael))
            }
        }
    }

    /** Each run of melacha-forbidden days (Shabbat and Yom Tov) in the sweep, for [inIsrael]. */
    private fun runs(inIsrael: Boolean): List<ClosedRange<LocalDate>> {
        val forbidden = days.filter { jewishCalendar(it, inIsrael).isAssurBemelacha }.toSet()
        val result = mutableListOf<ClosedRange<LocalDate>>()
        for (date in forbidden.sorted()) {
            if (date.minusDays(1) in forbidden) continue
            var end = date
            while (end.plusDays(1) in forbidden) end = end.plusDays(1)
            result += date..end
        }
        // The first and last runs may be cut off by the sweep's edges.
        return result.drop(1).dropLast(1)
    }

    private fun expectedTerm(day: LocalDate, yomTovDays: List<LocalDate>): HolyDayTerm {
        if (day !in yomTovDays) return HolyDayTerm.Shabbat
        if (yomTovDays.size == 1) return HolyDayTerm.YomTov
        return listOf(HolyDayTerm.FirstDay, HolyDayTerm.SecondDay, HolyDayTerm.ThirdDay)[yomTovDays.indexOf(day)]
    }

    @Test
    fun everyRunOfShabbatAndYomTovIsOneSpanFromTheFirstCandlesToTheLastExit() {
        for (location in cities) {
            val inIsrael = location.isInIsrael
            for (run in runs(inIsrael)) {
                val runDays = generateSequence(run.start) { it.plusDays(1) }.takeWhile { !it.isAfter(run.endInclusive) }.toList()
                assertTrue("${location.name} $run: at most three days in a row", runDays.size <= 3)
                val yomTovDays = runDays.filter { jewishCalendar(it, inIsrael).isYomTovAssurBemelacha }
                val eve = run.start.minusDays(1)
                val entry = requireNotNull(calendar(location, eve).candleLighting(settings)).time
                val exit = requireNotNull(calendar(location, run.endInclusive).holyDayExit(settings))
                assertTrue("${location.name} $run: candles before sunset", entry.isBefore(calendar(location, eve).sunset(settings.sunsetMethod)))
                assertTrue("${location.name} $run: out after sunset", exit.isAfter(calendar(location, run.endInclusive).sunset(settings.sunsetMethod)))
                assertEquals("${location.name} $run: candles on the eve", eve, entry.atZone(location.zoneId).toLocalDate())
                // Out on the last day's evening — or just past midnight, at a northern midsummer.
                val exitDate = exit.atZone(location.zoneId).toLocalDate()
                assertTrue("${location.name} $run: out on $exitDate", exitDate == run.endInclusive || exitDate == run.endInclusive.plusDays(1))

                for (day in runDays) {
                    val info = announcedHolyDayInfo(day, day, noon(location, day), settings, location, inIsrael)
                    assertNotNull("${location.name} $day", info)
                    assertEquals("${location.name} $day entry", entry, info!!.startTime)
                    assertEquals("${location.name} $day exit", exit, info.endTime)
                    assertTrue("${location.name} $day under way", info.isUnderWay)
                    assertEquals("${location.name} $day entry term", expectedTerm(run.start, yomTovDays), info.entryTerm)
                    assertEquals("${location.name} $day exit term", expectedTerm(run.endInclusive, yomTovDays), info.exitTerm)
                }
            }
        }
    }

    @Test
    fun diasporaPesachAndSukkotRunIntoShabbatAsThreeDaySpans() {
        // Pesach 5786: Thursday 2 and Friday 3 April 2026 abroad, then Shabbat. In Israel the Friday is
        // Chol Hamoed, so Thursday stands alone and Shabbat is a span of its own.
        val nyPesach = announcedHolyDayInfo(LocalDate.of(2026, 4, 3), LocalDate.of(2026, 4, 3), noon(newYork, LocalDate.of(2026, 4, 3)), settings, newYork, inIsrael = false)!!
        assertEquals(calendar(newYork, LocalDate.of(2026, 4, 1)).candleLighting(settings)!!.time, nyPesach.startTime)
        assertEquals(calendar(newYork, LocalDate.of(2026, 4, 4)).holyDayExit(settings), nyPesach.endTime)
        assertEquals(HolyDayTerm.FirstDay, nyPesach.entryTerm)
        assertEquals(HolyDayTerm.Shabbat, nyPesach.exitTerm)
        assertEquals(listOf(SequelPart.YomTov(1, includesShabbat = false), SequelPart.Shabbat), nyPesach.sequel)

        val jlmPesach = announcedHolyDayInfo(LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 2), noon(jerusalem, LocalDate.of(2026, 4, 2)), settings, jerusalem, inIsrael = true)!!
        assertEquals(calendar(jerusalem, LocalDate.of(2026, 4, 2)).holyDayExit(settings), jlmPesach.endTime)
        assertEquals(HolyDayTerm.YomTov, jlmPesach.exitTerm)
    }

    // ---- Fasts ----

    /** The civil date of each fast in the sweep, from the Hebrew-date rules and their postponements. */
    private fun expectedFasts(): Map<LocalDate, Fast> = buildMap {
        for (date in days) {
            val h = hebrew(date)
            val day = h.jewishDayOfMonth
            val saturday = date.dayOfWeek == DayOfWeek.SATURDAY
            val sunday = date.dayOfWeek == DayOfWeek.SUNDAY
            val lastAdar = if (h.isJewishLeapYear) JewishDate.ADAR_II else JewishDate.ADAR
            when {
                h.jewishMonth == JewishDate.TISHREI && ((day == 3 && !saturday) || (day == 4 && sunday)) -> put(date, Fast.Gedalyah)
                h.jewishMonth == JewishDate.TEVES && day == 10 -> put(date, Fast.TenthOfTevet)
                // 13 Adar, or the Thursday before when it falls on Shabbat.
                h.jewishMonth == lastAdar && ((day == 13 && !saturday) || (day == 11 && date.dayOfWeek == DayOfWeek.THURSDAY)) -> put(date, Fast.Esther)
                h.jewishMonth == JewishDate.TAMMUZ && ((day == 17 && !saturday) || (day == 18 && sunday)) -> put(date, Fast.SeventeenthOfTammuz)
                h.jewishMonth == JewishDate.AV && ((day == 9 && !saturday) || (day == 10 && sunday)) -> put(date, Fast.TishaBav)
            }
        }
    }

    @Test
    fun theFastsFallOnTheirDatesOrArePostponedOrBroughtForward() {
        val expected = expectedFasts()
        for (date in days) {
            val jc = jewishCalendar(date, inIsrael = false)
            val fast = Fast.entries.firstOrNull { jc.isTaanis && it.yomTovIndex == jc.yomTovIndex }
            assertEquals("$date", expected[date], fast)
        }
        // Each year has each of the five, exactly once.
        val perYear = expected.entries.groupBy { hebrew(it.key).jewishYear }.filterKeys { it in 5787..5795 }
        for ((year, fasts) in perYear) assertEquals("$year", Fast.entries.toSet(), fasts.map { it.value }.toSet())
        // 13 Adar on Shabbat brings Taanit Esther forward to Thursday 11 Adar: 9 March 2028.
        assertEquals(Fast.Esther, expected[LocalDate.of(2028, 3, 9)])
        // 9 Av on Shabbat pushes Tisha B'Av to Sunday: 22 July 2029 and 18 July 2032.
        assertEquals(Fast.TishaBav, expected[LocalDate.of(2029, 7, 22)])
        assertEquals(Fast.TishaBav, expected[LocalDate.of(2032, 7, 18)])
    }

    @Test
    fun dawnFastsRunFromAlotAndTishaBavFromTheSunsetBeforeAllToTzeit() {
        val expected = expectedFasts()
        for (location in cities) {
            for ((date, fast) in expected) {
                val info = zmanimDayFacts(location, date, settings, now = noon(location, date)).fastDayInfo
                assertNotNull("${location.name} $date $fast", info)
                assertEquals(fast, info!!.fast)
                val start = if (fast == Fast.TishaBav) {
                    calendar(location, date.minusDays(1)).sunset(settings.sunsetMethod)
                } else {
                    calendar(location, date).alotHashachar(settings)
                }
                assertNotNull("${location.name} $date: a start", start)
                assertEquals("${location.name} $date start", start, info.startTime)
                assertEquals("${location.name} $date end", calendar(location, date).tzeit(settings), info.endTime)
                assertTrue("${location.name} $date: begins before sunrise", info.startTime!!.isBefore(calendar(location, date).sunrise(settings.sunriseMethod)))
                assertTrue("${location.name} $date: ends after sunset", info.endTime!!.isAfter(calendar(location, date).sunset(settings.sunsetMethod)))
                assertTrue("${location.name} $date: under way at noon", info.isUnderWay)
            }
        }
    }

    @Test
    fun yomKippurIsAHolyDayFromCandleLightingToItsExitNotAPlainFast() {
        for (location in cities) {
            for (date in days.filter { jewishCalendar(it, location.isInIsrael).yomTovIndex == JewishCalendar.YOM_KIPPUR }) {
                assertFalse("$date is never Friday or Sunday", date.dayOfWeek == DayOfWeek.FRIDAY || date.dayOfWeek == DayOfWeek.SUNDAY)
                val facts = zmanimDayFacts(location, date, settings, now = noon(location, date))
                assertNull("${location.name} $date: no fast card", facts.fastDayInfo)
                val holy = requireNotNull(facts.holyDayInfo)
                assertEquals(calendar(location, date.minusDays(1)).candleLighting(settings)!!.time, holy.startTime)
                assertEquals(calendar(location, date).holyDayExit(settings), holy.endTime)
                // Roughly 25 hours, a little more or less with the season.
                val hours = Duration.between(holy.startTime, holy.endTime).toHours()
                assertTrue("${location.name} $date: $hours h", hours in 24L..27L)
            }
        }
    }

    @Test
    fun tishaBavOnMotzeiShabbatBeginsAtShabbatsSunsetWhileShabbatIsStillIn() {
        for (location in listOf(jerusalem, newYork)) {
            for (shabbat in listOf(LocalDate.of(2029, 7, 21), LocalDate.of(2032, 7, 17))) {
                val sunset = requireNotNull(calendar(location, shabbat).sunset(settings.sunsetMethod))
                val facts = zmanimDayFacts(location, shabbat, settings, now = sunset.plusSeconds(60))
                val fast = requireNotNull(facts.fastDayInfo)
                val holy = requireNotNull(facts.holyDayInfo)
                assertEquals(Fast.TishaBav, fast.fast)
                assertEquals(sunset, fast.startTime)
                assertTrue(fast.isUnderWay && holy.isUnderWay)
                assertTrue("Shabbat goes out after the fast begins", holy.endTime!!.isAfter(fast.startTime))
                // Both are under way; Shabbat ends first, so it names the header until it goes out.
                assertFalse(facts.fastLeadsHeader)
            }
        }
    }

    // ---- Erev Pesach, the Omer, Chanukah, Rosh Chodesh ----

    @Test
    fun theChametzTimesAreShownOnFourteenNissanOnly() {
        for (location in listOf(jerusalem, newYork, melbourne)) {
            for (date in days) {
                val events = zmanimDayFacts(location, date, settings).events
                val chametz = events.filterIsInstance<DayEvent.Chametz>().singleOrNull()
                val h = hebrew(date)
                val isErevPesach = h.jewishMonth == JewishDate.NISSAN && h.jewishDayOfMonth == 14
                assertEquals("${location.name} $date", isErevPesach, chametz != null)
                if (chametz != null) {
                    val day = calendar(location, date)
                    val eat = requireNotNull(chametz.eatUntil)
                    val burn = requireNotNull(chametz.burnUntil)
                    assertTrue(eat.isAfter(day.sunrise(settings.sunriseMethod)))
                    assertTrue(eat.isBefore(burn))
                    assertTrue(burn.isBefore(day.chatzot(settings.chatzotMethod)))
                }
            }
        }
    }

    @Test
    fun theOmerIsCountedFromSixteenNissanForFortyNineDaysWordedInBothLanguages() {
        val english = EnglishLanguageFormatter()
        val hebrewFormatter = HebrewLanguageFormatter()
        for (location in listOf(jerusalem, newYork)) {
            val inIsrael = location.isInIsrael
            for (year in 2026..2035) {
                val omerDays = days.filter { it.year == year }.mapNotNull { date ->
                    zmanimDayFacts(location, date, settings).events.filterIsInstance<DayEvent.Omer>().singleOrNull()?.let { date to it }
                }
                assertEquals("$year", 49, omerDays.size)
                val first = omerDays.first().first
                assertEquals(JewishDate.NISSAN, hebrew(first).jewishMonth)
                assertEquals(16, hebrew(first).jewishDayOfMonth)
                assertEquals(JewishDate.SIVAN, hebrew(omerDays.last().first).jewishMonth)
                assertEquals(5, hebrew(omerDays.last().first).jewishDayOfMonth)
                omerDays.forEachIndexed { index, (date, event) ->
                    assertEquals("$date", first.plusDays(index.toLong()), date)
                    val count = index + 1
                    assertEquals(count, jewishCalendar(date, inIsrael).dayOfOmer)
                    assertEquals(if (count == 33) "Lag BaOmer" else "Omer $count", english.format(event.count))
                    val hebrewText = hebrewFormatter.format(event.count)
                    assertTrue("$date: $hebrewText", hebrewText.endsWith("בעומר"))
                }
                assertEquals("ל״ג בעומר", hebrewFormatter.format(omerDays[32].second.count))
                assertEquals("א׳ בעומר", hebrewFormatter.format(omerDays[0].second.count))
                assertEquals("מ״ט בעומר", hebrewFormatter.format(omerDays[48].second.count))
            }
        }
    }

    @Test
    fun chanukahIsEightDaysFromTwentyFiveKislevIntoTevet() {
        val chanukah = days.mapNotNull { date ->
            zmanimDayFacts(jerusalem, date, settings).events.filterIsInstance<DayEvent.Chanukah>().singleOrNull()?.let { date to it.day }
        }
        // 5787 through 5795: the Chanukahs that lie whole inside the sweep.
        val byYear = chanukah.groupBy { hebrew(it.first).jewishYear }.filterKeys { it in 5787..5795 }
        assertEquals(9, byYear.size)
        for ((year, daysOf) in byYear) {
            assertEquals("$year", (1..8).toList(), daysOf.map { it.second })
            val first = hebrew(daysOf.first().first)
            assertEquals(JewishDate.KISLEV, first.jewishMonth)
            assertEquals(25, first.jewishDayOfMonth)
            val last = hebrew(daysOf.last().first)
            assertEquals(JewishDate.TEVES, last.jewishMonth)
            // Kislev has 29 or 30 days, so the eighth day is 3 or 2 Tevet.
            assertEquals(if (first.daysInJewishMonth == 30) 2 else 3, last.jewishDayOfMonth)
            daysOf.zipWithNext().forEach { (a, b) -> assertEquals(a.first.plusDays(1), b.first) }
        }
    }

    @Test
    fun roshChodeshIsTheFirstOfTheMonthAndTheThirtiethBeforeItButNeverRoshHashana() {
        for (date in days) {
            val h = hebrew(date)
            val expected = (h.jewishDayOfMonth == 1 && h.jewishMonth != JewishDate.TISHREI) || h.jewishDayOfMonth == 30
            val events = zmanimDayFacts(newYork, date, settings).events
            assertEquals("$date", expected, events.any { it is DayEvent.RoshChodesh })
        }
    }

    // ---- Purim, leap years, the modern days ----

    @Test
    fun leapYearsHaveAdarIAndIIWithPurimKatanInTheFirstAndPurimInTheSecond() {
        val english = EnglishLanguageFormatter()
        val hebrewFormatter = HebrewLanguageFormatter()
        for (date in days) {
            val h = hebrew(date)
            val jc = jewishCalendar(date, inIsrael = false)
            val dateEn = english.format(CalendarText.HebrewDate(date))
            val dateHe = hebrewFormatter.format(CalendarText.HebrewDate(date))
            when (h.jewishMonth) {
                JewishDate.ADAR -> if (h.isJewishLeapYear) {
                    assertTrue("$date: $dateEn", dateEn.contains("Adar I,"))
                    assertTrue("$date: $dateHe", dateHe.contains("אדר א׳"))
                    if (h.jewishDayOfMonth == 14) assertEquals(JewishCalendar.PURIM_KATAN, jc.yomTovIndex)
                    assertFalse("$date", jc.yomTovIndex == JewishCalendar.PURIM || jc.yomTovIndex == JewishCalendar.FAST_OF_ESTHER)
                } else {
                    assertTrue("$date: $dateEn", dateEn.contains("Adar,"))
                    assertFalse("$date: $dateHe", dateHe.contains("אדר א׳") || dateHe.contains("אדר ב׳"))
                    if (h.jewishDayOfMonth == 14) assertEquals(JewishCalendar.PURIM, jc.yomTovIndex)
                }
                JewishDate.ADAR_II -> {
                    assertTrue("$date: $dateEn", dateEn.contains("Adar II"))
                    assertTrue("$date: $dateHe", dateHe.contains("אדר ב׳"))
                    if (h.jewishDayOfMonth == 14) assertEquals(JewishCalendar.PURIM, jc.yomTovIndex)
                    if (h.jewishDayOfMonth == 15) assertEquals(JewishCalendar.SHUSHAN_PURIM, jc.yomTovIndex)
                }
            }
            // Purim is never on Monday, Wednesday or Shabbat.
            if (jc.yomTovIndex == JewishCalendar.PURIM) {
                assertFalse("$date", date.dayOfWeek in setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY))
            }
        }
    }

    @Test
    fun yomHaAtzmautAndYomHaZikaronMoveOffFridayShabbatAndMonday() {
        for (year in 2026..2035) {
            val atzmaut = days.filter { it.year == year && jewishCalendar(it, inIsrael = true).yomTovIndex == JewishCalendar.YOM_HAATZMAUT }
            val zikaron = days.filter { it.year == year && jewishCalendar(it, inIsrael = true).yomTovIndex == JewishCalendar.YOM_HAZIKARON }
            assertEquals("$year", 1, atzmaut.size)
            assertEquals("$year", listOf(atzmaut.single().minusDays(1)), zikaron)
            val date = atzmaut.single()
            val fifth = days.first { it.year == year && hebrew(it).jewishMonth == JewishDate.IYAR && hebrew(it).jewishDayOfMonth == 5 }
            val expected = when (fifth.dayOfWeek) {
                DayOfWeek.FRIDAY -> fifth.minusDays(1)
                DayOfWeek.SATURDAY -> fifth.minusDays(2)
                DayOfWeek.MONDAY -> fifth.plusDays(1)
                else -> fifth
            }
            assertEquals("$year", expected, date)
            assertNotNull("$year: named", minorDayName(jewishCalendar(date, inIsrael = true), date, inIsrael = true))
        }
    }

    // ---- The parsha abroad ----

    @Test
    fun theDiasporaParshaFallsBehindAfterAnEighthDayOrSecondDayOnShabbatAndCatchesUpWithADoubleReading() {
        val shabbatot = days.filter { it.dayOfWeek == DayOfWeek.SATURDAY }
        var blockStart: LocalDate? = null
        for (shabbat in shabbatot) {
            val israel = jewishCalendar(shabbat, inIsrael = true).parshah
            val diaspora = jewishCalendar(shabbat, inIsrael = false).parshah
            if (israel == diaspora) {
                assertTrue("$shabbat: a block must close with a double reading", blockStart == null)
                continue
            }
            if (blockStart == null) {
                blockStart = shabbat
                // It opens on a Shabbat that is Yom Tov abroad only: 22 Nissan or 7 Sivan.
                assertEquals("$shabbat", JewishCalendar.Parshah.NONE, diaspora)
                val h = hebrew(shabbat)
                assertTrue("$shabbat", (h.jewishMonth == JewishDate.NISSAN && h.jewishDayOfMonth == 22) || (h.jewishMonth == JewishDate.SIVAN && h.jewishDayOfMonth == 7))
            }
            // The week it closes, the diaspora reads two portions, the second being Israel's.
            if (diaspora.name.endsWith("_${israel.name}")) {
                assertTrue("$shabbat: closes within a season", ChronoUnit.WEEKS.between(blockStart, shabbat) <= 13)
                blockStart = null
            }
        }
        // And the screen reads it with the user's own calendar: in the week after Shavuot 5786.
        val wednesday = LocalDate.of(2026, 5, 27)
        val inJerusalem = zmanimDayFacts(jerusalem, wednesday, settings).weeklyParsha
        val inNewYork = zmanimDayFacts(newYork, wednesday, settings).weeklyParsha
        assertEquals(CalendarText.Parsha(LocalDate.of(2026, 5, 30), inIsrael = true), inJerusalem)
        assertEquals(CalendarText.Parsha(LocalDate.of(2026, 5, 30), inIsrael = false), inNewYork)
        // Israel is a week ahead: Beha'alotcha there, Nasso abroad.
        assertEquals(JewishCalendar.Parshah.BEHAALOSCHA, jewishCalendar(inJerusalem!!.date, inJerusalem.inIsrael).parshah)
        assertEquals(JewishCalendar.Parshah.NASSO, jewishCalendar(inNewYork!!.date, inNewYork.inIsrael).parshah)
    }

    // ---- Clocks, the date line, the far north ----

    @Test
    fun daylightSavingChangesMoveTheClockButNotTheDay() {
        for (location in cities) {
            val rules = location.zoneId.rules
            var transition = rules.nextTransition(days.first().atStartOfDay(location.zoneId).toInstant())
            while (transition != null && transition.instant.atZone(location.zoneId).year <= 2035) {
                val date = transition.instant.atZone(location.zoneId).toLocalDate()
                for (day in listOf(date.minusDays(1), date, date.plusDays(1))) {
                    val sunset = calendar(location, day).sunset(settings.sunsetMethod) ?: continue
                    assertEquals("${location.name} $day", day, sunset.atZone(location.zoneId).toLocalDate())
                    val facts = zmanimDayFacts(location, day, settings, now = noon(location, day))
                    assertEquals(day, facts.date)
                }
                val before = calendar(location, date.minusDays(1)).sunset(settings.sunsetMethod)
                val after = calendar(location, date.plusDays(1)).sunset(settings.sunsetMethod)
                if (before != null && after != null) {
                    // The sun itself moves a few minutes in two days, never an hour.
                    val drift = Duration.between(before.plus(Duration.ofDays(2)), after).abs().toMinutes()
                    assertTrue("${location.name} $date: $drift min", drift < 15)
                }
                transition = rules.nextTransition(transition.instant)
            }
        }
    }

    @Test
    fun nearTheDateLineTheTimesBelongToTheLocalDay() {
        val places = listOf(
            city("Apia", -13.8333, -171.7667, 2.0, "Pacific/Apia"),
            city("Kiritimati", 1.8721, -157.4278, 3.0, "Pacific/Kiritimati"),
            city("Auckland", -36.8485, 174.7633, 30.0, "Pacific/Auckland"),
            city("Suva", -18.1248, 178.4501, 5.0, "Pacific/Fiji"),
            city("Honolulu", 21.3069, -157.8583, 5.0, "Pacific/Honolulu"),
        )
        for (place in places) {
            for (date in days.filter { it.year == 2026 }) {
                val day = calendar(place, date)
                assertEquals("${place.name} $date sunrise", date, day.sunrise(settings.sunriseMethod)!!.atZone(place.zoneId).toLocalDate())
                assertEquals("${place.name} $date sunset", date, day.sunset(settings.sunsetMethod)!!.atZone(place.zoneId).toLocalDate())
                // A plain Shabbat, with no Yom Tov either side of it.
                val plainShabbat = date.dayOfWeek == DayOfWeek.SATURDAY &&
                    listOf(date.minusDays(1), date, date.plusDays(1)).none { jewishCalendar(it, inIsrael = false).isYomTovAssurBemelacha }
                if (plainShabbat) {
                    val info = requireNotNull(announcedHolyDayInfo(date, date, noon(place, date), settings, place, inIsrael = false))
                    assertEquals("${place.name} $date", DayOfWeek.FRIDAY, info.startTime!!.atZone(place.zoneId).dayOfWeek)
                    assertEquals("${place.name} $date", DayOfWeek.SATURDAY, info.endTime!!.atZone(place.zoneId).dayOfWeek)
                }
            }
        }
    }

    /**
     * Where the sun never sinks 16.1° or 6.2° (a London, Helsinki or Reykjavik summer), alot and tzeit
     * fall back to 72 and 50 minutes, so every zman the day and its observances are built on is still
     * there. Misheyakir at 11° has no fallback, so it is missing for some summer weeks in Helsinki
     * and Reykjavik; nothing else is.
     */
    @Test
    fun inTheNorthernSummerEveryZmanButMisheyakirIsKeptByItsFallback() {
        for (location in listOf(reykjavik, helsinki, london)) {
            for (date in days.filter { it.year == 2026 }) {
                val facts = zmanimDayFacts(location, date, settings, now = noon(location, date))
                val missing = facts.zmanim.filter { it.time == null }.map { it.option } - ZmanimTimeOption.TallitTefillin
                assertTrue("${location.name} $date: missing $missing", missing.isEmpty())
            }
        }
    }

    @Test
    fun aboveTheArcticCircleNothingThrowsAndTheScreenStillRefreshes() {
        val places = listOf(
            city("Tromso", 69.6492, 18.9553, 10.0, "Europe/Oslo"),
            city("Longyearbyen", 78.2232, 15.6267, 10.0, "Arctic/Longyearbyen"),
            reykjavik,
        )
        for (place in places) {
            for (date in days.filter { it.year in 2026..2027 }) {
                for (time in listOf(0 to 30, 12 to 0, 23 to 30)) {
                    val now = date.atTime(time.first, time.second).atZone(place.zoneId).toInstant()
                    zmanimDayFacts(place, date, settings, now = now)
                    for (next in listOf(
                        nextZmanimRefreshBoundary(place, settings, now),
                        nextStatusIconBoundary(place, settings, now),
                        nextDateBoundary(place, settings, now),
                    )) {
                        assertTrue("${place.name} $now → $next", next.isAfter(now))
                        assertTrue("${place.name} $now → $next", Duration.between(now, next) <= Duration.ofDays(2))
                    }
                }
            }
        }
    }
}
