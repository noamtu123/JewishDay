// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Back-to-back holy days are shown one at a time, each carrying a warning that another begins the
 * moment it ends, and each replaced by the next as it goes out.
 *
 * Fixed dates (Jerusalem): 2029-09-10/11 Rosh Hashana on Monday and Tuesday (a plain two-day Yom
 * Tov); 2026-09-12/13 Rosh Hashana starting on Shabbat; 2026-05-22 Shavuot on a Friday, running
 * into Shabbat.
 */
class HolyDaySequenceTest {
    private val zone = defaultJerusalemLocation.zoneId

    private fun infoOn(date: LocalDate, now: Instant?) = zmanimForDate(date = date, now = now).holyDayInfo
    private fun hoursBeforeSunset(date: LocalDate, hours: Long): Instant =
        requireNotNull(sunsetForDate(date = date)).minus(Duration.ofHours(hours))

    @Test
    fun twoDayYomTovShowsTheFirstDayWithADoubleHolidayWarning() {
        val first = LocalDate.of(2029, 9, 10)
        val info = requireNotNull(infoOn(first, hoursBeforeSunset(first, 3)))

        // The first day's own window, not a span across both.
        assertEquals(first.minusDays(1), requireNotNull(info.startTime).atZone(zone).toLocalDate())
        assertEquals(holyDayExitForDate(date = first), info.endTime)
        // Tzom Gedalyah begins the morning after the chag, so it belongs in the warning.
        assertEquals("חג כפול + צום", info.sequelHebrew)
    }

    @Test
    fun onceTheFirstDayGoesOutItIsReplacedByTheSecond() {
        val first = LocalDate.of(2029, 9, 10)
        val second = first.plusDays(1)
        val firstExit = requireNotNull(holyDayExitForDate(date = first))

        val info = requireNotNull(infoOn(first, firstExit.plus(Duration.ofMinutes(5))))
        // The second day begins exactly where the first ended — candles from an existing flame.
        assertEquals(firstExit, info.startTime)
        assertEquals(holyDayExitForDate(date = second), info.endTime)
        // One chag day left, then the fast.
        assertEquals("חג + צום", info.sequelHebrew)
    }

    @Test
    fun yomTovRunningIntoShabbatWarnsInThatOrder() {
        val shavuot = LocalDate.of(2026, 5, 22)
        val info = requireNotNull(infoOn(shavuot, hoursBeforeSunset(shavuot, 3)))

        assertEquals(holyDayExitForDate(date = shavuot), info.endTime)
        assertEquals("חג + שבת", info.sequelHebrew)
        assertTrue(info.nameHebrew, info.nameHebrew.contains("שבועות"))

        // And after it goes out, Shabbat itself takes over.
        val shabbat = shavuot.plusDays(1)
        val next = requireNotNull(infoOn(shavuot, requireNotNull(info.endTime).plus(Duration.ofMinutes(5))))
        assertEquals("שבת", next.nameHebrew)
        assertEquals(holyDayExitForDate(date = shabbat), next.endTime)
        assertNull(next.sequelHebrew)
    }

    @Test
    fun shabbatRunningIntoYomTovWarnsInTheOtherOrder() {
        // 2029-05-19 is an ordinary Shabbat running straight into Shavuot on the Sunday.
        val shabbat = LocalDate.of(2029, 5, 19)
        val info = requireNotNull(infoOn(shabbat, hoursBeforeSunset(shabbat, 3)))

        assertEquals("שבת", info.nameHebrew)
        assertEquals("שבת + חג", info.sequelHebrew)
    }

    @Test
    fun aYomTovFallingOnShabbatIsOneDayNamedForBoth() {
        // Rosh Hashana beginning on Shabbat: one day that is both, so its name joins them with
        // "and" — distinct from the "+" used for two days in a row.
        val bothAtOnce = LocalDate.of(2026, 9, 12)
        val info = requireNotNull(infoOn(bothAtOnce, hoursBeforeSunset(bothAtOnce, 3)))

        assertTrue(info.nameHebrew, info.nameHebrew.contains("ושבת"))
        assertTrue(info.nameHebrew, info.nameHebrew.contains("השנה"))
        // Two days of chag, one of which is itself Shabbat — said with "and", not "+".
        assertEquals("חג כפול ושבת + צום", info.sequelHebrew)
    }

    @Test
    fun threeDaysInARowWarnOfATripleAndThenNarrowDownAsTheyPass() {
        // 2028-09-21/22 is Rosh Hashana on Thursday and Friday, running into Shabbat on the 23rd.
        val first = LocalDate.of(2028, 9, 21)
        val second = LocalDate.of(2028, 9, 22)
        val shabbat = LocalDate.of(2028, 9, 23)

        val onFirst = requireNotNull(infoOn(first, hoursBeforeSunset(first, 3)))
        assertEquals("חג כפול + שבת + צום", onFirst.sequelHebrew)

        // With one day gone, only the pair is left, so the warning narrows to it.
        val onSecond = requireNotNull(infoOn(second, hoursBeforeSunset(second, 3)))
        assertEquals("חג + שבת + צום", onSecond.sequelHebrew)

        // And the last day has nothing after it.
        val onShabbat = requireNotNull(infoOn(shabbat, hoursBeforeSunset(shabbat, 3)))
        assertEquals("שבת", onShabbat.nameHebrew)
        // Shabbat is the last forbidden day, but the deferred fast still follows it.
        assertEquals("שבת + צום", onShabbat.sequelHebrew)
    }

    @Test
    fun shabbatRunningIntoATwoDayYomTovNamesBothHalves() {
        // 2029-05-19 Shabbat runs into Shavuot; in the diaspora that Yom Tov is two days, so the
        // Israel calendar gives the single-day form and the ordering is what matters here.
        val shabbat = LocalDate.of(2029, 5, 19)
        val info = requireNotNull(infoOn(shabbat, hoursBeforeSunset(shabbat, 3)))

        assertTrue(requireNotNull(info.sequelHebrew), requireNotNull(info.sequelHebrew).startsWith("שבת +"))
    }

    @Test
    fun anAnnouncedFastWaitsUntilTheHolyDayIsOut() {
        // The second day of Rosh Hashana already announces Tzom Gedalyah, which begins the next
        // morning — but the chag is still on, so only the chag is on screen.
        val secondDay = LocalDate.of(2029, 9, 11)
        val duringChag = zmanimForDate(date = secondDay, now = hoursBeforeSunset(secondDay, 3))

        assertNull(duringChag.fastDayInfo)
        assertTrue(requireNotNull(duringChag.holyDayInfo).nameHebrew.contains("השנה"))

        // Once the chag goes out the fast takes over the header.
        val chagOut = requireNotNull(holyDayExitForDate(date = secondDay)).plus(Duration.ofMinutes(5))
        val afterChag = zmanimForDate(date = secondDay, now = chagOut)
        assertEquals("צום גדליה", requireNotNull(afterChag.fastDayInfo).nameHebrew)
        assertNull(afterChag.holyDayInfo)
    }

    @Test
    fun aFastUnderWayKeepsTheChipWhileAHolyDayIsOnlyAnnounced() {
        // A dawn fast on its own day, with Shabbat still ahead: the fast is what is happening.
        val fastDay = LocalDate.of(2026, 7, 2)
        val day = zmanimForDate(date = fastDay, now = hoursBeforeSunset(fastDay, 4))

        assertNotNull(day.fastDayInfo)
        assertEquals(true, day.fastLeadsHeader)
    }

    @Test
    fun eachDayOfAMultiDayYomTovNamesItsOwnTimes() {
        val first = LocalDate.of(2029, 9, 10)
        val second = first.plusDays(1)

        assertEquals("חג ראשון", requireNotNull(infoOn(first, hoursBeforeSunset(first, 3))).termHebrew)
        assertEquals("חג שני", requireNotNull(infoOn(second, hoursBeforeSunset(second, 3))).termHebrew)

        // A single-day Yom Tov is just "the chag", and Shabbat is Shabbat.
        val yomKippur = LocalDate.of(2026, 9, 21)
        assertEquals("החג", requireNotNull(infoOn(yomKippur, hoursBeforeSunset(yomKippur, 3))).termHebrew)
        val saturday = LocalDate.of(2026, 9, 5)
        assertEquals("שבת", requireNotNull(infoOn(saturday, hoursBeforeSunset(saturday, 3))).termHebrew)
    }

    @Test
    fun theNameAppearsOnlyOnceTheDayIsActuallyIn() {
        // Thursday evening: the displayed date has rolled to Friday, so Shabbat's times are up, but
        // Shabbat itself has not come in and the chip must stay unnamed.
        val thursday = LocalDate.of(2026, 9, 3)
        val announced = requireNotNull(
            infoOn(thursday, requireNotNull(sunsetForDate(date = thursday)).plus(Duration.ofMinutes(30))),
        )
        assertNotNull(announced.startTime)
        assertEquals(false, announced.isUnderWay)

        // Friday after sunset the displayed date is Shabbat itself, so the name shows.
        val friday = LocalDate.of(2026, 9, 4)
        val entered = requireNotNull(
            infoOn(friday, requireNotNull(sunsetForDate(date = friday)).plus(Duration.ofMinutes(30))),
        )
        assertEquals(true, entered.isUnderWay)
    }

    @Test
    fun onShabbatTheShabbatSectionIsDroppedAndRabbeinuTamJoinsTheDay() {
        val saturday = LocalDate.of(2026, 9, 5)
        val onShabbat = zmanimForDate(date = saturday, now = hoursBeforeSunset(saturday, 4))

        // The section's times are the header's entry and exit, so it goes.
        assertEquals(null, onShabbat.groups.firstOrNull { it.title == "Shabbat" })
        // Rabbeinu Tam is the one row it carried that is not a duplicate, so it moves to the day.
        val zmanim = requireNotNull(onShabbat.groups.firstOrNull { it.title == "Zmanim" }).items
        val rabbeinuTam = requireNotNull(zmanim.first { it.title == "Rabbeinu Tam" }.time)
        // Computed from today, which on Shabbat is the same day the section used.
        assertTrue(rabbeinuTam.isAfter(requireNotNull(tzeitForDate(date = saturday))))
        // And the parsha moves up rather than disappearing with the section.
        assertNotNull(onShabbat.groups.flatMap { it.items }.firstOrNull { it.title == "Weekly Parsha" })
    }

    @Test
    fun onOtherDaysTheShabbatSectionStays() {
        val friday = LocalDate.of(2026, 9, 4)
        val onFriday = zmanimForDate(date = friday, now = hoursBeforeSunset(friday, 4))

        // Friday's list is not Shabbat's, so the section is still doing work.
        assertNotNull(onFriday.groups.firstOrNull { it.title == "Shabbat" })
        val zmanim = requireNotNull(onFriday.groups.firstOrNull { it.title == "Zmanim" }).items
        assertEquals(null, zmanim.firstOrNull { it.title == "Rabbeinu Tam" })

        // Nor is it a duplicate once Shabbat is over and it points at next week.
        val afterMotzei = LocalDate.of(2026, 9, 5)
        val later = zmanimForDate(
            date = afterMotzei,
            now = requireNotNull(holyDayExitForDate(date = afterMotzei)).plus(Duration.ofMinutes(30)),
        )
        assertNotNull(later.groups.firstOrNull { it.title == "Shabbat" })
    }

    @Test
    fun theNameSurvivesSunsetUntilTheDayActuallyGoesOut() {
        // Regression: the displayed date rolls at sunset, but a holy day runs on to tzeit plus the
        // tosefet. In that gap the header read ג׳ תשרי with no name at all, while its card still
        // showed חג שני.
        val secondDay = LocalDate.of(2029, 9, 11)
        val exit = requireNotNull(holyDayExitForDate(date = secondDay))

        val afterSunset = requireNotNull(
            infoOn(secondDay, requireNotNull(sunsetForDate(date = secondDay)).plus(Duration.ofMinutes(2))),
        )
        assertTrue(afterSunset.isUnderWay)
        assertTrue(afterSunset.nameHebrew, afterSunset.nameHebrew.contains("השנה"))

        val justBeforeExit = requireNotNull(infoOn(secondDay, exit.minus(Duration.ofMinutes(2))))
        assertTrue(justBeforeExit.isUnderWay)

        // And once it is genuinely out, the chag is gone entirely.
        assertNull(infoOn(secondDay, exit.plus(Duration.ofMinutes(5))))
    }

    @Test
    fun aDawnFastIsNotNamedTheNightBeforeItBegins() {
        // Motzei Rosh Hashana: the Hebrew date has already rolled to 3 Tishrei, Tzom Gedalyah's own
        // day, and its times are on screen — but nobody is fasting until dawn, so it stays unnamed.
        val secondDay = LocalDate.of(2029, 9, 11)
        val chagOut = requireNotNull(holyDayExitForDate(date = secondDay)).plus(Duration.ofMinutes(5))
        val fast = requireNotNull(zmanimForDate(date = secondDay, now = chagOut).fastDayInfo)

        assertEquals("צום גדליה", fast.nameHebrew)
        assertEquals(false, fast.isUnderWay)

        // Come dawn, it is named.
        val fastDay = secondDay.plusDays(1)
        val duringFast = requireNotNull(
            zmanimForDate(date = fastDay, now = requireNotNull(tzeitForDate(date = fastDay)).minus(Duration.ofHours(4))).fastDayInfo,
        )
        assertEquals(true, duringFast.isUnderWay)
    }

    @Test
    fun aFastKeepsItsNameBetweenSunsetAndTzeit() {
        // The same gap: a dawn fast ends at tzeit, well after the displayed date has rolled.
        val fastDay = LocalDate.of(2026, 7, 2)
        val tzeit = requireNotNull(tzeitForDate(date = fastDay))
        val afterSunset = requireNotNull(sunsetForDate(date = fastDay)).plus(Duration.ofMinutes(2))

        val fast = requireNotNull(zmanimForDate(date = fastDay, now = afterSunset).fastDayInfo)
        assertTrue(fast.isUnderWay)
        assertTrue(afterSunset.isBefore(tzeit))
    }

    @Test
    fun aLoneShabbatCarriesNoWarning() {
        val saturday = LocalDate.of(2026, 9, 5)
        val info = requireNotNull(infoOn(saturday, hoursBeforeSunset(saturday, 3)))

        assertEquals("שבת", info.nameHebrew)
        assertNull(info.sequelHebrew)
    }

    @Test
    fun stillAnnouncedOneJewishDayBeforeTheRunEnters() {
        // Shavuot enters Thursday evening, so the announcement begins at Wednesday's sunset.
        // (Rosh Hashana is no use here: two days before a Monday Yom Tov is Shabbat, which is
        // itself a holy day and rightly showing its own card.)
        val wednesday = LocalDate.of(2026, 5, 20)
        val sunset = requireNotNull(sunsetForDate(date = wednesday))

        assertNull(infoOn(wednesday, sunset.minus(Duration.ofHours(2))))
        assertNotNull(infoOn(wednesday, sunset.plus(Duration.ofMinutes(30))))
    }
}
