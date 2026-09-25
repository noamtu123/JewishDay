// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.widget

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.noamtu.jewishday.data.AppLanguage
import com.noamtu.jewishday.data.AppSettings
import com.noamtu.jewishday.data.CurrentLocationName
import com.noamtu.jewishday.feature.zmanim.zmanimTimeFormatters
import com.noamtu.jewishday.model.DailyLearningType
import com.noamtu.jewishday.model.ShabbatGroupTitle
import com.noamtu.jewishday.model.ZmanimCalculationSettings
import com.noamtu.jewishday.model.ZmanimDay
import com.noamtu.jewishday.model.ZmanimGroupTitle
import com.noamtu.jewishday.model.ZmanimTimeOption
import com.noamtu.jewishday.model.defaultJerusalemLocation
import com.noamtu.jewishday.model.skyDayFor
import com.noamtu.jewishday.model.upcomingChatzotHaLaila
import com.noamtu.jewishday.model.zmanimForDate
import com.noamtu.jewishday.ui.theme.skyAt
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The widget's state is built from the same day the zmanim screen shows, so these fix a clock at a
 * local time on a known date (Jerusalem) and check what the widget would say — by names, order and
 * language rather than exact clock strings, which shift with the sun and with DST. The times are
 * the ones still to come, so most checks read each shown time back to an instant and hold it
 * against the clock.
 *
 * Fixed dates: 2026-11-04 an ordinary Wednesday; 2026-09-23 the Wednesday before a Shabbat that is
 * Sukkot; 2026-09-25 Friday, Erev Sukkot, with Sukkot falling on the Shabbat of 2026-09-26;
 * 2026-07-23 Tisha B'Av; 2025-08-02 a Shabbat on which 9 Av fell, the fast observed on the Sunday;
 * 2026-04-01 Erev Pesach on a Wednesday; 2026-04-20 in the Omer; 2026-12-08 in Chanukah; 2026-07-14
 * an ordinary summer Tuesday, whose chatzot halaila falls past midnight.
 */
class DayWidgetStateBuilderTest {
    private val location = defaultJerusalemLocation
    private val zone = location.zoneId
    private val calculation = ZmanimCalculationSettings()

    private val ordinaryWednesday = LocalDate.of(2026, 11, 4)
    private val weekBeforeSukkot = LocalDate.of(2026, 9, 23)
    private val erevSukkotFriday = LocalDate.of(2026, 9, 25)
    private val shabbatBeforeTishaBeAv = LocalDate.of(2025, 8, 2)
    private val sukkotShabbat = LocalDate.of(2026, 9, 26)
    private val tishaBeAv = LocalDate.of(2026, 7, 23)
    private val erevPesach = LocalDate.of(2026, 4, 1)
    private val inTheOmer = LocalDate.of(2026, 4, 20)
    private val inChanukah = LocalDate.of(2026, 12, 8)
    private val summerTuesday = LocalDate.of(2026, 7, 14)

    private fun at(date: LocalDate, hour: Int, minute: Int = 0): Instant =
        date.atTime(hour, minute).atZone(zone).toInstant()

    /** The day as the app computes it for a clock reading [hour]:[minute] local time on [date]. */
    private fun dayAt(date: LocalDate, hour: Int, minute: Int = 0): ZmanimDay =
        zmanimForDate(location, date, calculation, now = at(date, hour, minute))

    /** The day after [date], as the widget's loader computes it at [now] to look ahead. */
    private fun nextDayAt(date: LocalDate, now: Instant): ZmanimDay =
        zmanimForDate(location, date.plusDays(1), calculation, now = now)

    private fun stateOf(
        day: ZmanimDay,
        now: Instant,
        settings: AppSettings = AppSettings(),
        nextDay: ZmanimDay? = null,
    ): DayWidgetState =
        buildDayWidgetState(day, settings, skyAt(now, skyDayFor(location, day.date, calculation), zone), now, nextDay)

    /** The state the widget would show at [hour]:[minute] on [date], tomorrow looked ahead to as the loader does. */
    private fun stateAt(
        date: LocalDate,
        hour: Int,
        minute: Int = 0,
        language: AppLanguage = AppLanguage.English,
    ): DayWidgetState {
        val now = at(date, hour, minute)
        return stateOf(dayAt(date, hour, minute), now, AppSettings(language = language), nextDayAt(date, now))
    }

    private fun formatted(instant: Instant?, hebrew: Boolean = false): String {
        val formatters = zmanimTimeFormatters(use24HourTime = true, zoneId = zone)
        return (if (hebrew) formatters.hebrew else formatters.english).format(requireNotNull(instant))
    }

    /**
     * When a shown English time falls, read back from its clock text: on [date], or the day after
     * when its label says tomorrow — and chatzot halaila a day on again once the clock is past midnight.
     */
    private fun shownInstant(time: DayWidgetTime, date: LocalDate): Instant {
        val clock = LocalTime.parse(time.time)
        val tomorrow = if (time.label.endsWith(" (tomorrow)")) 1L else 0L
        val pastMidnight = if (time.label.startsWith("Chatzot HaLaila") && clock.isBefore(LocalTime.NOON)) 1L else 0L
        return date.plusDays(tomorrow + pastMidnight).atTime(clock).atZone(zone).toInstant()
    }

    /** The widget's times are some, at most four, all still to come at [now], soonest first. */
    private fun assertAllStillToCome(state: DayWidgetState, date: LocalDate, now: Instant) {
        val shown = state.times.toString()
        assertTrue(shown, state.times.isNotEmpty())
        assertTrue(shown, state.times.size <= 4)
        val instants = state.times.map { shownInstant(it, date) }
        // The clock text drops the seconds, so a time within the current minute reads as now.
        assertTrue("$shown at $now", instants.none { it.isBefore(now) })
        assertEquals(shown, instants.sorted(), instants)
    }

    private fun jewishCalendar(date: LocalDate): JewishCalendar = JewishCalendar(date).apply {
        isUseModernHolidays = true
        setInIsrael(true)
    }

    private fun ZmanimDay.shabbatReadingRow() =
        requireNotNull(groups.first { it.title == ShabbatGroupTitle }.items.first { it.id == null && it.value != null })

    private fun List<DayWidgetTime>.labels(): List<String> = map { it.label }

    private fun isHebrewLetter(c: Char): Boolean = c in 'א'..'ת'

    @Test
    fun ordinaryWeekdayShowsWhatIsStillToComeAndTheComingParasha() {
        val now = at(ordinaryWednesday, 12)
        val day = dayAt(ordinaryWednesday, 12)
        assertNull(day.holyDayInfo)
        assertNull(day.fastDayInfo)

        val state = stateOf(day, now, nextDay = nextDayAt(ordinaryWednesday, now))

        assertFalse(state.useHebrew)
        assertAllStillToCome(state, ordinaryWednesday, now)
        assertEquals(4, state.times.size)
        assertFalse(state.times.labels().toString(), "Sunrise" in state.times.labels())
        assertTrue(state.times.none { it.pinned })
        // Nothing is under way and the day has no name of its own, so the badge looks ahead to
        // Shabbat, worded as the header will word it from Thursday night: the Shabbat section's
        // reading row is where the parasha lives on a weekday.
        assertEquals("Parashat " + day.shabbatReadingRow().value, state.chip)
        assertNull(state.eventLine)
        assertTrue(state.observanceLines.isEmpty())
        assertTrue(state.hebrewDate, state.hebrewDate.isNotBlank())
        assertTrue(state.weekdayAndDate, state.weekdayAndDate.startsWith("Wednesday, November 4"))
        // Jerusalem here is the fallback, not a fix, and the caption says so.
        assertEquals("Times based on Jerusalem", state.locationName)
    }

    @Test
    fun timesShowOnlyWhatIsStillToCome() {
        val early = stateAt(ordinaryWednesday, 5)
        assertAllStillToCome(early, ordinaryWednesday, at(ordinaryWednesday, 5))
        assertTrue(early.times.labels().toString(), "Sunrise" in early.times.labels())

        val afternoon = stateAt(ordinaryWednesday, 15)
        assertAllStillToCome(afternoon, ordinaryWednesday, at(ordinaryWednesday, 15))
        assertTrue(afternoon.times.labels().toString(), "Sunset" in afternoon.times.labels())
        assertFalse(afternoon.times.labels().toString(), "Sunrise" in afternoon.times.labels())
    }

    @Test
    fun lateAtNightTheTimesLookAheadToTomorrow() {
        // Tonight's chatzot halaila has passed by ten to midnight in November; all that is left is tomorrow's.
        val now = at(ordinaryWednesday, 23, 50)

        val state = stateAt(ordinaryWednesday, 23, 50)
        val hebrew = stateAt(ordinaryWednesday, 23, 50, language = AppLanguage.Hebrew)

        assertAllStillToCome(state, ordinaryWednesday, now)
        assertEquals(4, state.times.size)
        assertTrue(state.times.labels().toString(), state.times.all { it.label.endsWith(" (tomorrow)") })
        assertEquals("Alot (tomorrow)", state.times.first().label)
        assertTrue(hebrew.times.labels().toString(), hebrew.times.all { it.label.endsWith(" (מחר)") })
    }

    @Test
    fun tonightsChatzotReadsAsTonightsEvenPastMidnight() {
        val now = at(summerTuesday, 23)
        val chatzot = requireNotNull(upcomingChatzotHaLaila(location, calculation, now))
        assertEquals(summerTuesday.plusDays(1), chatzot.atZone(zone).toLocalDate())
        val settings = AppSettings(enabledZmanimTimes = setOf(ZmanimTimeOption.ChatzotHaLaila, ZmanimTimeOption.Sunrise))

        val state = stateOf(dayAt(summerTuesday, 23), now, settings, nextDayAt(summerTuesday, now))

        assertEquals(listOf("Chatzot HaLaila", "Sunrise (tomorrow)"), state.times.labels())
        assertEquals(formatted(chatzot), state.times.first().time)
    }

    @Test
    fun onlyTheEnabledZmanimAreListed() {
        val now = at(ordinaryWednesday, 12)
        val settings = AppSettings(enabledZmanimTimes = setOf(ZmanimTimeOption.Sunset))

        val state = stateOf(dayAt(ordinaryWednesday, 12), now, settings)

        assertEquals(listOf("Sunset"), state.times.labels())
    }

    @Test
    fun theLongDeadlineNamesAreShortenedToFitAColumn() {
        // By seven the sun is up; the morning's four deadlines are what come next.
        val english = stateAt(ordinaryWednesday, 7)
        val hebrew = stateAt(ordinaryWednesday, 7, language = AppLanguage.Hebrew)

        assertEquals(setOf("Shema MGA", "Shema GRA", "Tefillah MGA", "Tefillah GRA"), english.times.labels().toSet())
        assertEquals(setOf("ק״ש מג״א", "ק״ש גר״א", "תפילה מג״א", "תפילה גר״א"), hebrew.times.labels().toSet())
    }

    @Test
    fun hebrewInterfaceResolvesEveryTextToHebrew() {
        val now = at(ordinaryWednesday, 12)
        val day = dayAt(ordinaryWednesday, 12)
        val english = stateOf(day, now, AppSettings(language = AppLanguage.English))
        val hebrew = stateOf(day, now, AppSettings(language = AppLanguage.Hebrew))

        assertTrue(hebrew.useHebrew)
        assertEquals(day.hebrewDateHebrew, hebrew.hebrewDate)
        assertEquals(day.hebrewDateEnglish, english.hebrewDate)
        assertTrue(hebrew.hebrewDate, hebrew.hebrewDate.isNotBlank())
        assertTrue(hebrew.hebrewDate != english.hebrewDate)
        assertTrue(hebrew.times.labels().toString(), hebrew.times.all { it.label.any(::isHebrewLetter) })
        assertEquals("פרשת " + day.shabbatReadingRow().valueHebrew, hebrew.chip)
        assertEquals("הזמנים מבוססים על ירושלים", hebrew.locationName)
        // The clock times themselves are the same instants either way.
        assertEquals(english.times.map { it.time }, hebrew.times.map { it.time })
        assertTrue(hebrew.weekdayAndDate, hebrew.weekdayAndDate.contains("בנובמבר"))
        assertTrue(requireNotNull(hebrew.learning), hebrew.learning.startsWith("דף יומי בבלי: "))
    }

    @Test
    fun fridayAfternoonKeepsCandleLightingAmongTheTimesAndAnnouncesTheEntry() {
        val now = at(erevSukkotFriday, 15)
        val day = dayAt(erevSukkotFriday, 15)
        val holyDay = requireNotNull(day.holyDayInfo)
        assertFalse(holyDay.isUnderWay)

        val state = stateOf(day, now, nextDay = nextDayAt(erevSukkotFriday, now))

        // Sukkot falls on this Shabbat, so there is no weekly parasha to name the coming Shabbat by;
        // the reading row names the Yom Tov instead, and that is what the badge says.
        assertEquals(day.shabbatReadingRow().value, state.chip)
        assertTrue(state.observanceLines.toString(), state.observanceLines.any { it.contains(" starts ") })
        assertAllStillToCome(state, erevSukkotFriday, now)
        // The entry is the one the header announces — which on a Friday is also the Shabbat
        // section's own candle-lighting row.
        val candles = state.times.single { it.label == "Candle Lighting" }
        assertTrue(candles.pinned)
        assertEquals(formatted(holyDay.startTime), candles.time)
        val candleRow = day.groups.first { it.title == ShabbatGroupTitle }.items
            .first { it.id == ZmanimTimeOption.ShabbatCandleLighting.storageValue }
        assertEquals(formatted(candleRow.time), candles.time)

        val hebrew = stateAt(erevSukkotFriday, 15, language = AppLanguage.Hebrew)
        assertEquals(day.shabbatReadingRow().valueHebrew, hebrew.chip)
        assertTrue(hebrew.times.labels().toString(), "הדלקת נרות" in hebrew.times.labels())
        assertTrue(hebrew.observanceLines.toString(), hebrew.observanceLines.any { it.startsWith("כניסת ") })
    }

    @Test
    fun shabbatNamesTheChipAndKeepsTheExitAmongTheTimes() {
        val now = at(sukkotShabbat, 12)
        val day = dayAt(sukkotShabbat, 12)
        val holyDay = requireNotNull(day.holyDayInfo)
        assertTrue(holyDay.isUnderWay)
        // On Shabbat itself the Shabbat section is dropped (the header already gives the exit), so
        // the exit has to come from the header's holy-day info rather than a Motzei Shabbat row.
        assertNull(day.groups.firstOrNull { it.title == ShabbatGroupTitle })

        val state = stateOf(day, now, nextDay = nextDayAt(sukkotShabbat, now))

        assertEquals(holyDay.name, state.chip)
        assertTrue(requireNotNull(state.chip), state.chip.contains("Shabbat"))
        assertAllStillToCome(state, sukkotShabbat, now)
        val exit = state.times.single { it.label == "Motzei Shabbat" }
        assertTrue(exit.pinned)
        assertEquals(formatted(holyDay.endTime), exit.time)
        assertTrue(state.observanceLines.toString(), state.observanceLines.any { it.contains(" ends ") })

        val hebrew = stateAt(sukkotShabbat, 12, language = AppLanguage.Hebrew)
        assertEquals(holyDay.nameHebrew, hebrew.chip)
        assertTrue(requireNotNull(hebrew.chip), hebrew.chip.contains("שבת"))
        assertEquals(formatted(holyDay.endTime, hebrew = true), hebrew.times.single { it.label == "צאת שבת" }.time)
    }

    @Test
    fun fastUnderWayNamesTheChipAndKeepsItsEndAmongTheTimes() {
        val now = at(tishaBeAv, 12)
        val day = dayAt(tishaBeAv, 12)
        val fast = requireNotNull(day.fastDayInfo)
        assertTrue(fast.isUnderWay)
        assertNull(day.holyDayInfo)

        val state = stateOf(day, now, nextDay = nextDayAt(tishaBeAv, now))

        assertEquals(fast.name, state.chip)
        assertAllStillToCome(state, tishaBeAv, now)
        val end = state.times.single { it.label == "Fast ends" }
        assertTrue(end.pinned)
        assertEquals(formatted(fast.endTime), end.time)
        // Both ends of the fast are written out, in screen order.
        assertEquals(2, state.observanceLines.size)
        assertTrue(state.observanceLines[0], state.observanceLines[0].startsWith("Fast starts "))
        assertTrue(state.observanceLines[1], state.observanceLines[1].startsWith("Fast ends "))

        val hebrew = stateAt(tishaBeAv, 12, language = AppLanguage.Hebrew)
        assertEquals("תשעה באב", hebrew.chip)
        assertTrue(hebrew.times.labels().toString(), "צאת הצום" in hebrew.times.labels())
        assertTrue(hebrew.observanceLines[0], hebrew.observanceLines[0].startsWith("כניסת הצום "))
    }

    @Test
    fun chanukahShowsInTheEventLine() {
        assertTrue(jewishCalendar(inChanukah).isChanukah)

        val english = stateAt(inChanukah, 12)
        val hebrew = stateAt(inChanukah, 12, language = AppLanguage.Hebrew)

        assertTrue(requireNotNull(english.eventLine), english.eventLine.startsWith("Chanukah: "))
        assertTrue(requireNotNull(hebrew.eventLine), hebrew.eventLine.startsWith("חנוכה: "))
    }

    @Test
    fun omerShowsInTheEventLine() {
        assertTrue(jewishCalendar(inTheOmer).dayOfOmer != -1)

        val english = stateAt(inTheOmer, 12)
        val hebrew = stateAt(inTheOmer, 12, language = AppLanguage.Hebrew)

        assertTrue(requireNotNull(english.eventLine), english.eventLine.startsWith("Omer: "))
        assertTrue(requireNotNull(hebrew.eventLine), hebrew.eventLine.startsWith("עומר: "))
    }

    @Test
    fun roshChodeshShowsInTheEventLine() {
        val roshChodesh = generateSequence(LocalDate.of(2026, 10, 10)) { it.plusDays(1) }
            .first { jewishCalendar(it).isRoshChodesh }

        val english = stateAt(roshChodesh, 12)
        val hebrew = stateAt(roshChodesh, 12, language = AppLanguage.Hebrew)

        assertTrue(requireNotNull(english.eventLine), english.eventLine.startsWith("Rosh Chodesh: "))
        assertTrue(requireNotNull(hebrew.eventLine), hebrew.eventLine.startsWith("ראש חודש: "))
    }

    @Test
    fun erevPesachNamesTheDayListsTheChametzDeadlinesAndKeepsTonightsCandleLighting() {
        assertEquals(JewishCalendar.EREV_PESACH, jewishCalendar(erevPesach).yomTovIndex)
        val now = at(erevPesach, 9)
        val day = dayAt(erevPesach, 9)
        val holyDay = requireNotNull(day.holyDayInfo)
        assertFalse(holyDay.isUnderWay)

        val state = stateOf(day, now, nextDay = nextDayAt(erevPesach, now))

        assertEquals(day.dayName, state.chip)
        // The chametz rows carry a time rather than a text, and the line still says when.
        val eventLine = requireNotNull(state.eventLine)
        assertTrue(eventLine, eventLine.startsWith("Eat Chametz Until: "))
        assertTrue(eventLine, eventLine.contains(" · Burn Chametz Until: "))
        // Pesach enters tonight, on a Wednesday: the candle lighting kept is tonight's, not the
        // Shabbat section's Friday one — the morning's deadlines come first, and it closes the row.
        assertAllStillToCome(state, erevPesach, now)
        val candles = state.times.last()
        assertEquals("Candle Lighting", candles.label)
        assertTrue(candles.pinned)
        assertEquals(formatted(holyDay.startTime), candles.time)
        assertEquals(erevPesach, requireNotNull(holyDay.startTime).atZone(zone).toLocalDate())
    }

    @Test
    fun learningPrefersDafYomiBavliAndFollowsTheEnabledTracks() {
        val day = dayAt(ordinaryWednesday, 12)
        val now = at(ordinaryWednesday, 12)

        val default = stateOf(day, now, AppSettings())
        assertTrue(requireNotNull(default.learning), default.learning.startsWith("Daf Yomi Bavli: "))

        val tehillimOnly = stateOf(day, now, AppSettings(enabledDailyLearning = setOf(DailyLearningType.TehillimYomi)))
        assertTrue(requireNotNull(tehillimOnly.learning), tehillimOnly.learning.startsWith("Tehillim Yomi: "))

        val nothing = stateOf(day, now, AppSettings(enabledDailyLearning = emptySet()))
        assertNull(nothing.learning)
    }

    @Test
    fun rowsWithoutATimeAreDroppedAndNeverMoreThanFourShown() {
        val now = at(ordinaryWednesday, 12)
        val day = dayAt(ordinaryWednesday, 12)
        val sunsetMissing = day.copy(
            groups = day.groups.map { group ->
                if (group.title != ZmanimGroupTitle) return@map group
                group.copy(
                    items = group.items.map { item ->
                        if (item.id == ZmanimTimeOption.Sunset.storageValue) item.copy(time = null) else item
                    },
                )
            },
        )

        val state = stateOf(sunsetMissing, now)
        assertFalse(state.times.labels().toString(), "Sunset" in state.times.labels())
        assertAllStillToCome(state, ordinaryWednesday, now)

        val everyScenario = listOf(
            stateAt(ordinaryWednesday, 12),
            stateAt(ordinaryWednesday, 23, 50),
            stateAt(erevSukkotFriday, 15),
            stateAt(sukkotShabbat, 12),
            stateAt(tishaBeAv, 12),
            stateAt(erevPesach, 9),
            stateAt(shabbatBeforeTishaBeAv, 19, 50),
        )
        everyScenario.forEach { scenario ->
            assertTrue(scenario.times.toString(), scenario.times.size <= 4)
            assertTrue(scenario.observanceLines.toString(), scenario.observanceLines.size <= 3)
            scenario.times.forEach { row -> assertTrue(row.toString(), row.time != "--" && row.time.isNotBlank()) }
        }
    }

    @Test
    fun aLiveFixShowsNoLocationCaption() {
        val day = dayAt(ordinaryWednesday, 12).copy(locationName = CurrentLocationName)

        val state = stateOf(day, at(ordinaryWednesday, 12))

        assertNull(state.locationName)
    }

    @Test
    fun theWeekBeforeAYomTovShabbatHasNoBadge() {
        // Sukkot falls on the coming Shabbat, so the reading row names the Yom Tov rather than a
        // parasha; until the header announces the day on Thursday night, that name in the badge
        // would read as though Sukkot were today.
        val day = dayAt(weekBeforeSukkot, 12)
        assertNull(day.holyDayInfo)
        assertNull(day.dayName)
        assertEquals("Torah Reading", day.shabbatReadingRow().title)

        assertNull(stateOf(day, at(weekBeforeSukkot, 12)).chip)
    }

    @Test
    fun shabbatRunningIntoTishaBeAvKeepsBothMotzeiShabbatAndTheFastsEnd() {
        // 9 Av 5785 fell on Shabbat, so the fast was observed on the Sunday: it begins at Shabbat's
        // sunset while Shabbat runs on to its exit. The exit minutes away and the fast's end a day
        // away are both what is being waited for, and neither is crowded out by the zmanim between.
        val now = at(shabbatBeforeTishaBeAv, 19, 50)
        val day = dayAt(shabbatBeforeTishaBeAv, 19, 50)
        val holyDay = requireNotNull(day.holyDayInfo)
        val fast = requireNotNull(day.fastDayInfo)
        assertTrue(holyDay.isUnderWay)
        assertTrue(fast.isUnderWay)
        assertFalse(day.fastLeadsHeader)

        val state = stateOf(day, now, nextDay = nextDayAt(shabbatBeforeTishaBeAv, now))

        assertEquals(holyDay.name, state.chip)
        assertAllStillToCome(state, shabbatBeforeTishaBeAv, now)
        assertEquals(formatted(holyDay.endTime), state.times.single { it.label == "Motzei Shabbat" }.time)
        assertEquals(formatted(fast.endTime), state.times.single { it.label == "Fast ends (tomorrow)" }.time)
        // The exit that is minutes away outranks Friday's entry when the lines run out of room.
        assertTrue(state.observanceLines.toString(), state.observanceLines.any { it.startsWith("Shabbat ends ") })
    }

    @Test
    fun aDayWithNoGroupsStillBuilds() {
        val day = dayAt(ordinaryWednesday, 12).copy(groups = emptyList())

        val state = stateOf(day, at(ordinaryWednesday, 12))

        assertTrue(state.times.isEmpty())
        assertNull(state.eventLine)
        assertNull(state.learning)
        assertNull(state.chip)
        assertEquals(day.hebrewDateEnglish, state.hebrewDate)
        assertNotNull(state.sky)
    }
}
