// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.noamtu.jewishday.core.model.AlotHashacharMethod
import com.noamtu.jewishday.core.model.CandleLighting
import com.noamtu.jewishday.core.model.CandleLightingMethod
import com.noamtu.jewishday.core.model.ChametzMethod
import com.noamtu.jewishday.core.model.ChatzotMethod
import com.noamtu.jewishday.core.model.CustomZmanUnit
import com.noamtu.jewishday.core.model.CustomZmanValue
import com.noamtu.jewishday.core.model.DailyLearningType
import com.noamtu.jewishday.core.model.HebcalLearningEntry
import com.noamtu.jewishday.core.model.JewishLocation
import com.noamtu.jewishday.core.model.MinchaGedolaMethod
import com.noamtu.jewishday.core.model.MinchaKetanaMethod
import com.noamtu.jewishday.core.model.MisheyakirMethod
import com.noamtu.jewishday.core.model.MotzeiShabbatMethod
import com.noamtu.jewishday.core.model.PlagHaminchaMethod
import com.noamtu.jewishday.core.model.RabbeinuTamMethod
import com.noamtu.jewishday.core.model.SofZmanShemaMethod
import com.noamtu.jewishday.core.model.SofZmanTefillahMethod
import com.noamtu.jewishday.core.model.SunriseMethod
import com.noamtu.jewishday.core.model.SunsetMethod
import com.noamtu.jewishday.core.model.TzeitHakochavimMethod
import com.noamtu.jewishday.core.model.ZmanimCalculationSettings
import com.noamtu.jewishday.core.model.ZmanimTimeOption
import com.noamtu.jewishday.core.model.defaultJerusalemLocation
import com.noamtu.jewishday.core.model.jewishCalendar
import com.noamtu.jewishday.core.model.jewishDayInfo
import com.noamtu.jewishday.core.model.nextZmanimRefreshBoundary
import com.noamtu.jewishday.core.model.sunsetForDate
import com.noamtu.jewishday.core.model.tzeitForDate
import com.noamtu.jewishday.core.model.zmanimDayFacts
import com.noamtu.jewishday.data.toZmanItems
import com.noamtu.jewishday.day.ZmanimDay
import com.noamtu.jewishday.day.withDailyLearningItems
import com.noamtu.jewishday.day.zmanimForDate
import com.noamtu.jewishday.feature.zmanim.ZmanimUiState
import com.noamtu.jewishday.feature.zmanim.filterForDisplay
import com.noamtu.jewishday.feature.zmanim.mergeRambamRows
import com.noamtu.jewishday.feature.zmanim.rambamRowText
import com.noamtu.jewishday.feature.zmanim.toUiState
import com.noamtu.jewishday.i18n.AppLocales
import com.noamtu.jewishday.i18n.TestText
import com.noamtu.jewishday.i18n.TextResolver
import com.noamtu.jewishday.i18n.caption
import com.noamtu.jewishday.i18n.customZmanLabel
import com.noamtu.jewishday.i18n.label
import com.noamtu.jewishday.notification.dateIconSpec
import com.noamtu.jewishday.widget.WidgetContent
import com.noamtu.jewishday.widget.WidgetKind
import com.noamtu.jewishday.widget.WidgetStyle
import com.noamtu.jewishday.widget.glassDateSpoken
import com.noamtu.jewishday.widget.hebrewDateSpoken
import com.noamtu.jewishday.widget.nextZmanSpoken
import com.noamtu.jewishday.widget.shabbatSpoken
import com.noamtu.jewishday.widget.widgetContent
import com.noamtu.jewishday.widget.zmanimSpoken
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.fail
import org.junit.Test

/**
 * Every text the app shows, for a year of days, checked against `golden-text.txt`: the zmanim
 * screen's header and rows (Jerusalem through the day, stepped to, and with other opinions; New York
 * at noon), the widgets, the status-bar icon, every method's label and caption, and the daily
 * learning, in English and Hebrew. Any change to a calculation or to a word shows up here.
 *
 * When a change is meant, look at the diff and take the new file:
 * `cp app/build/golden-text.actual.txt app/src/test/resources/golden-text.txt`.
 */
class GoldenTextTest {
    private val out = StringBuilder()
    private fun line(key: String, value: Any?) {
        out.append(key).append('=').append(value.toString().replace("\n", "\\n")).append('\n')
    }

    private val locations = listOf(
        defaultJerusalemLocation,
        JewishLocation("New York", 40.7128, -74.006, 10.0, ZoneId.of("America/New_York")),
        JewishLocation("London", 51.5074, -0.1278, 11.0, ZoneId.of("Europe/London")),
        JewishLocation("Stockholm", 59.3293, 18.0686, 28.0, ZoneId.of("Europe/Stockholm")),
        JewishLocation("Melbourne", -37.8136, 144.9631, 31.0, ZoneId.of("Australia/Melbourne")),
    )

    // A full leap year (5787, with Adar I and II) and a little either side.
    private val firstDay = LocalDate.of(2026, 9, 5)
    private val lastDay = LocalDate.of(2027, 10, 8)
    private val days = generateSequence(firstDay) { it.plusDays(1) }.takeWhile { !it.isAfter(lastDay) }.toList()

    private val alternate = ZmanimCalculationSettings(
        alotHashacharMethod = AlotHashacharMethod.CustomMinutes,
        misheyakirMethod = MisheyakirMethod.Minutes6AfterAlos,
        sunriseMethod = SunriseMethod.ElevationAdjusted,
        sofZmanShemaGraMethod = SofZmanShemaMethod.FixedLocalGra,
        sofZmanShemaMethod = SofZmanShemaMethod.AteretTorah,
        sofZmanTefillahGraMethod = SofZmanTefillahMethod.FixedLocalGra,
        sofZmanTefillahMethod = SofZmanTefillahMethod.Alos16Point1ToTzeit7Point083,
        chatzotMethod = ChatzotMethod.FixedLocal,
        chatzotHaLailaMethod = ChatzotMethod.FixedLocal,
        minchaGedolaMethod = MinchaGedolaMethod.BaalHatanyaGreaterThan30,
        minchaKetanaMethod = MinchaKetanaMethod.AhavatShalom,
        plagHaminchaMethod = PlagHaminchaMethod.BaalHatanya,
        sunsetMethod = SunsetMethod.ElevationAdjusted,
        tzeitHakochavimMethod = TzeitHakochavimMethod.CustomZmaniyotMinutes,
        candleLightingMethod = CandleLightingMethod.Minutes40,
        motzeiShabbatMethod = MotzeiShabbatMethod.CustomMinutes,
        rabbeinuTamMethod = RabbeinuTamMethod.BainHashmashot2Stars,
        chametzMethod = ChametzMethod.BaalHatanya,
        holyDayTosefetMinutes = 3,
    )

    private val odd = CustomZmanValue(degrees = 19.848, minutes = 90, zmaniyotMinutes = 96)
    private val custom = ZmanimCalculationSettings(
        alotHashacharMethod = AlotHashacharMethod.CustomDegrees,
        alotHashacharCustom = odd,
        misheyakirMethod = MisheyakirMethod.CustomZmaniyotMinutesBeforeSunrise,
        misheyakirCustom = odd,
        sofZmanShemaGraMethod = SofZmanShemaMethod.Gra,
        sofZmanShemaMethod = SofZmanShemaMethod.CustomMinutesToFixedLocalChatzot,
        sofZmanShemaCustom = odd,
        sofZmanTefillahMethod = SofZmanTefillahMethod.CustomZmaniyotMinutes,
        sofZmanTefillahCustom = odd,
        minchaGedolaMethod = MinchaGedolaMethod.CustomDegrees,
        minchaGedolaCustom = odd,
        minchaKetanaMethod = MinchaKetanaMethod.CustomMinutes,
        minchaKetanaCustom = odd,
        plagHaminchaMethod = PlagHaminchaMethod.CustomZmaniyotMinutes,
        plagHaminchaCustom = odd,
        tzeitHakochavimMethod = TzeitHakochavimMethod.CustomDegrees,
        tzeitHakochavimCustom = CustomZmanValue(degrees = 8.5, minutes = 42, zmaniyotMinutes = 45),
        candleLightingMethod = CandleLightingMethod.Custom,
        candleLightingCustomMinutes = 25,
        motzeiShabbatMethod = MotzeiShabbatMethod.CustomZmaniyotMinutes,
        motzeiShabbatCustom = odd,
        rabbeinuTamMethod = RabbeinuTamMethod.CustomDegrees,
        rabbeinuTamCustom = odd,
        chametzMethod = ChametzMethod.CustomMinutes,
        chametzCustom = odd,
    )

    private val settingsVariants = listOf("default" to ZmanimCalculationSettings(), "alternate" to alternate, "custom" to custom)

    // The days the calendar does something on, and the day before (which announces it); a fast two
    // days ahead, since Tisha B'Av is announced from the evening before its eve.
    private fun isSpecial(date: LocalDate): Boolean = listOf(true, false).any { israel ->
        (0L..2L).any { ahead ->
            val c = jewishCalendar(date.plusDays(ahead), israel)
            c.isTaanis || ahead < 2 && (
                c.isYomTovAssurBemelacha || c.isRoshChodesh || c.yomTovIndex in NamedDays ||
                    c.isChanukah && c.dayOfChanukah == 1
                )
        }
    }

    private val NamedDays = setOf(
        JewishCalendar.EREV_PESACH,
        JewishCalendar.CHOL_HAMOED_PESACH,
        JewishCalendar.PURIM,
        JewishCalendar.SHUSHAN_PURIM,
        JewishCalendar.HOSHANA_RABBA,
        JewishCalendar.LAG_BAOMER,
    )

    private fun times(location: JewishLocation, date: LocalDate): List<Pair<String, Instant>> {
        val zone = location.zoneId
        fun at(h: Int, m: Int) = date.atTime(LocalTime.of(h, m)).atZone(zone).toInstant()
        val sunset = sunsetForDate(location, date) ?: at(18, 0)
        val tzeit = tzeitForDate(location, date) ?: at(19, 0)
        return listOf(
            "06:00" to at(6, 0),
            "12:00" to at(12, 0),
            "sunset-90" to sunset.minus(Duration.ofMinutes(90)),
            "sunset+5" to sunset.plus(Duration.ofMinutes(5)),
            "tzeit+30" to tzeit.plus(Duration.ofMinutes(30)),
            "23:30" to at(23, 30),
        )
    }

    // Noon, just past sunset (the date not yet rolled) and after tzeit (rolled).
    private val GoldenTimes = setOf("12:00", "sunset+5", "tzeit+30")

    @Test
    fun everyTextMatchesTheGoldenFile() {
        dumpLabels()
        dumpLearning()
        val jerusalem = locations[0]
        val newYork = locations[1]
        for (date in days) dumpIcon("icon|$date", date)
        // Every special day, and the first Friday and Shabbat of each month: Jerusalem through the
        // day and stepped to, New York at noon, and the other opinions and the 12-hour clock.
        val weekends = days.filter { it.dayOfWeek.value >= 5 && it.dayOfMonth <= 7 }
        for (date in days.filter { isSpecial(it) || it in weekends }) {
            for ((label, now) in times(jerusalem, date).filter { (label, _) -> label in GoldenTimes }) {
                dumpDay("day|${jerusalem.name}|$date|$label", jerusalem, date, ZmanimCalculationSettings(), now)
                if (label == "12:00") {
                    dumpWidget("widget|${jerusalem.name}|$date|$label", jerusalem, date, ZmanimCalculationSettings(), now, use24 = true)
                }
            }
            dumpDay("stepped|${jerusalem.name}|$date", jerusalem, date, ZmanimCalculationSettings(), null)
            val noon = date.atTime(12, 0).atZone(newYork.zoneId).toInstant()
            dumpDay("day|${newYork.name}|$date|12:00", newYork, date, ZmanimCalculationSettings(), noon)
        }
        for (date in days.filter { isSpecial(it) }.filterIndexed { index, _ -> index % 3 == 0 }) {
            val noon = date.atTime(12, 0).atZone(jerusalem.zoneId).toInstant()
            for ((name, settings) in settingsVariants.drop(1)) {
                dumpDay("variant-$name|${jerusalem.name}|$date", jerusalem, date, settings, noon)
                dumpWidget("widget-$name|${jerusalem.name}|$date", jerusalem, date, settings, noon, use24 = true)
            }
            dumpDay("12h|${jerusalem.name}|$date", jerusalem, date, ZmanimCalculationSettings(), noon, use24 = false)
        }
        // Stockholm's December Friday holds candle lighting at plag.
        val stockholm = locations[3]
        val december = LocalDate.of(2026, 12, 18)
        dumpDay("day|${stockholm.name}|$december|12:00", stockholm, december, ZmanimCalculationSettings(), december.atTime(12, 0).atZone(stockholm.zoneId).toInstant())

        val actual = out.toString()
        File("build/golden-text.actual.txt").apply { parentFile?.mkdirs() }.writeText(actual)
        val expected = requireNotNull(javaClass.getResource("/golden-text.txt")) { "golden-text.txt is missing" }.readText()
        if (actual != expected) {
            val actualLines = actual.lines()
            val expectedLines = expected.lines()
            val first = actualLines.indices.firstOrNull { actualLines[it] != expectedLines.getOrNull(it) } ?: expectedLines.size
            fail(
                "Text differs from golden-text.txt at line ${first + 1}:\n" +
                    "  expected: ${expectedLines.getOrNull(first)}\n" +
                    "  actual:   ${actualLines.getOrNull(first)}\n" +
                    "The whole output is in app/build/golden-text.actual.txt.",
            )
        }
    }

    private fun dumpDay(
        scenario: String,
        location: JewishLocation,
        date: LocalDate,
        settings: ZmanimCalculationSettings,
        now: Instant?,
        use24: Boolean = true,
    ) {
        val day = if (now == null) {
            zmanimForDate(location, date, settings, null, false)
        } else {
            zmanimForDate(location, date, settings, now)
        }
        val state = day
            .filterForDisplay(ZmanimTimeOption.Default, DailyLearningType.entries.toSet())
            .mergeRambamRows()
            .toUiState(use24HourTime = use24, showCandleLightingPrompt = false, developerTimeOverrideActive = false, dayOffset = 0)
        for (hebrew in listOf(false, true)) dumpState("$scenario|${if (hebrew) "he" else "en"}", state, text(hebrew))
    }

    private fun text(hebrew: Boolean): TextResolver = if (hebrew) TestText.hebrew else TestText.english

    private fun dumpState(prefix: String, state: ZmanimUiState, text: TextResolver) {
        val h = requireNotNull(state.header)
        line(
            "$prefix|header",
            listOf(
                text.resolve(h.jewishDate), text.resolve(h.gregorianDate), h.locationName,
                h.fastName?.let(text::resolve), h.fastStart?.let(text::resolve), h.fastEnd?.let(text::resolve),
                h.holyDayName?.let(text::resolve), h.dayName?.let(text::resolve), h.parshaName?.let(text::resolve),
                h.holyDayStart?.let(text::resolve), h.holyDayEnd?.let(text::resolve), h.holyDaySequel?.let(text::resolve),
                h.fastLeadsHeader,
            ).joinToString(" | "),
        )
        state.groups.forEachIndexed { gi, group ->
            line("$prefix|g$gi", group.title?.let(text::resolve).orEmpty())
            group.rows.forEachIndexed { ri, row ->
                val value = text.resolve(row.value)
                val rambam = if (row.isRambam) rambamRowText(value, text.language) else null
                line(
                    "$prefix|g$gi.r$ri",
                    listOf(
                        row.key, text.resolve(row.title), text.resolve(row.description), rambam?.value ?: value,
                        rambam?.candidates.orEmpty().joinToString("||"),
                    ).joinToString(" | "),
                )
            }
        }
    }

    private fun dumpWidget(
        scenario: String,
        location: JewishLocation,
        date: LocalDate,
        settings: ZmanimCalculationSettings,
        now: Instant,
        use24: Boolean,
    ) {
        val day = zmanimDayFacts(location, date, settings, now)
        for (hebrew in listOf(false, true)) {
            val content = widgetContent(
                day = day,
                tomorrow = { zmanimDayFacts(location, date.plusDays(1), settings, null, false) },
                text = text(hebrew),
                use24HourTime = use24,
                enabledZmanim = ZmanimTimeOption.Default,
                now = now,
                observanceBoundary = nextZmanimRefreshBoundary(location, settings, now),
            )
            dumpWidgetContent("$scenario|${if (hebrew) "he" else "en"}", content)
        }
    }

    private fun dumpWidgetContent(p: String, c: WidgetContent) {
        line("$p|dates", listOf(c.rtl, c.hebrewDate, c.gregorianDate, c.weekday, c.civilDate, c.specialDay, c.date).joinToString(" | "))
        line("$p|zmanim", c.zmanim.joinToString("; ") { "${it.title}|${it.time}|${it.isPast}" })
        line("$p|next", listOf(c.nextIndex, c.next?.title, c.next?.time, c.following?.title, c.following?.time, c.nextLabel, c.followingLabel).joinToString(" | "))
        line("$p|ahead", c.ahead.joinToString("; ") { "${it.title}|${it.time}" })
        line(
            "$p|shabbat",
            c.shabbat?.let { s ->
                listOf(
                    s.title, s.subtitle, s.entryHeading, s.entryTime, s.exitHeading, s.exitTime, s.entry,
                    s.isUnderWay, s.announced, s.day,
                ).joinToString(" | ")
            },
        )
        line("$p|refreshAt", c.refreshAt)
        line("$p|spoken", listOf(hebrewDateSpoken(c), nextZmanSpoken(c), zmanimSpoken(c, c.zmanim.withIndex().toList()), shabbatSpoken(c), glassDateSpoken(c, c.ahead)).joinToString(" || "))
    }

    private fun dumpIcon(p: String, date: LocalDate) {
        for ((name, info) in listOf("same" to jewishDayInfo(date), "rolled" to jewishDayInfo(date, date.plusDays(1)))) {
            for (hebrew in listOf(false, true)) {
                val spec = dateIconSpec(info, text(hebrew))
                line("$p|$name|${if (hebrew) "he" else "en"}", "${spec.iconText} | ${spec.title} | ${spec.content}")
            }
        }
    }

    private fun dumpLabels() {
        for (hebrew in listOf(false, true)) {
            val l = if (hebrew) "he" else "en"
            val t = text(hebrew)
            ZmanimTimeOption.entries.forEach { line("label|$l|ZmanimTimeOption.$it", t.string(it.label)) }
            DailyLearningType.entries.forEach { line("label|$l|DailyLearningType.$it", t.string(it.label)) }
            WidgetKind.entries.forEach { line("label|$l|WidgetKind.$it", t.string(it.label)) }
            WidgetStyle.entries.forEach { line("label|$l|WidgetStyle.$it", t.string(it.label)) }
            for (settings in listOf(ZmanimCalculationSettings(), custom)) {
                val s = if (settings == custom) "custom" else "default"
                fun m(name: String, label: com.noamtu.jewishday.i18n.UiText, caption: com.noamtu.jewishday.i18n.UiText) {
                    line("method|$l|$s|$name.label", t.resolve(label))
                    line("method|$l|$s|$name.caption", t.resolve(caption))
                }
                AlotHashacharMethod.entries.forEach { m("Alot.$it", it.label, it.caption(settings)) }
                MisheyakirMethod.entries.forEach { m("Misheyakir.$it", it.label, it.caption(settings)) }
                SunriseMethod.entries.forEach { m("Sunrise.$it", it.label, it.label) }
                SofZmanShemaMethod.entries.forEach { m("Shema.$it", it.label, it.caption(settings)) }
                SofZmanTefillahMethod.entries.forEach { m("Tefillah.$it", it.label, it.caption(settings)) }
                ChatzotMethod.entries.forEach { m("Chatzot.$it", it.label, it.label) }
                MinchaGedolaMethod.entries.forEach { m("MinchaGedola.$it", it.label, it.caption(settings)) }
                MinchaKetanaMethod.entries.forEach { m("MinchaKetana.$it", it.label, it.caption(settings)) }
                PlagHaminchaMethod.entries.forEach { m("Plag.$it", it.label, it.caption(settings)) }
                SunsetMethod.entries.forEach { m("Sunset.$it", it.label, it.label) }
                TzeitHakochavimMethod.entries.forEach { m("Tzeit.$it", it.label, it.caption(settings)) }
                CandleLightingMethod.entries.forEach { m("Candles.$it", it.label, it.caption(settings)) }
                MotzeiShabbatMethod.entries.forEach { m("Motzei.$it", it.label, it.caption(settings)) }
                RabbeinuTamMethod.entries.forEach { m("RabbeinuTam.$it", it.label, it.caption(settings)) }
                ChametzMethod.entries.forEach { m("Chametz.$it", it.label, it.caption(settings)) }
                line("method|$l|$s|CandleLighting.plag", t.resolve(CandleLighting(Instant.EPOCH, isPlag = true).caption(settings)))
                line("method|$l|$s|CandleLighting.notPlag", t.resolve(CandleLighting(Instant.EPOCH, isPlag = false).caption(settings)))
                CustomZmanUnit.entries.forEach { line("method|$l|$s|custom.$it", t.resolve(customZmanLabel(it, settings.alotHashacharCustom))) }
            }
        }
        // The language picker's names, keyed as the old AppLanguage entries were.
        mapOf("English" to "en", "Hebrew" to "he").forEach { (entry, tag) ->
            check(tag in AppLocales.supported)
            line("label|AppLanguage.$entry", AppLocales.displayName(tag))
        }
    }

    private val hebcalSets = listOf(
        "fixtures" to listOf(
            HebcalLearningEntry(category = "dafyomi", title = "Sanhedrin 78", hebrew = "סנהדרין ע״ח"),
            HebcalLearningEntry(category = "yerushalmi", title = "Yerushalmi Berakhot 32", hebrew = "ברכות לב"),
            HebcalLearningEntry(category = "mishnayomi", title = "Kelim 11:7-8", hebrew = "כלים 11:7-8"),
            HebcalLearningEntry(category = "dailyRambam1", title = "Prayer 5", hebrew = "הלכות תפילה פרק ה"),
            HebcalLearningEntry(category = "dailyPsalms", title = "Psalms 106-107", hebrew = "תהלים ק״ו-ק״ז"),
            HebcalLearningEntry(category = "tanakhYomi", title = "Psalms Seder 3", hebrew = "תנ״ך ס׳ ג", memo = "Psalms 20:10-29:10"),
            HebcalLearningEntry(category = "shemiratHaLashon", title = "Book I, Shaar HaZechira 1.1-1.4", hebrew = "Book I, שער הזכירה 1.1-1.4"),
            HebcalLearningEntry(category = "kitzurShulchanAruch", title = "161:18-162:5", hebrew = "קסא:יח-קסב:ה"),
        ),
        "bothRambam" to listOf(
            HebcalLearningEntry(category = "dafyomi", title = "Berakhot 2", hebrew = "ברכות ב׳"),
            HebcalLearningEntry(category = "yerushalmi", title = "Yerushalmi Bava Kamma 5", hebrew = "בבא קמא ה"),
            HebcalLearningEntry(category = "mishnayomi", title = "Berakhot 1:1", hebrew = "ברכות 1:1"),
            HebcalLearningEntry(category = "dailyRambam1", title = "Sabbath 19", hebrew = "הלכות שבת פרקים יט", memo = "Sabbath 19\nhttps://www.sefaria.org/x"),
            HebcalLearningEntry(category = "dailyRambam3", title = "Prayer 3-5", hebrew = "הלכות תפילה פרק 3-5"),
            HebcalLearningEntry(category = "tanakhYomi", title = "Seder 3", hebrew = "תנ״ך ס׳ ג", memo = "Seder 3"),
            HebcalLearningEntry(category = "shemiratHaLashon", title = "Book II 1.1-1.2", hebrew = "Book II 1.1-1.2"),
            HebcalLearningEntry(category = "kitzurShulchanAruch", title = "12:3", hebrew = "יב:ג"),
        ),
        "multiBook" to listOf(
            HebcalLearningEntry(category = "yerushalmi", title = "Yerushalmi Berakhot", hebrew = "ירושלמי ברכות"),
            HebcalLearningEntry(category = "dailyRambam1", title = "Intro", hebrew = "הלכות תפילה פרק אחד"),
            HebcalLearningEntry(category = "dailyRambam3", title = "Prayer 8, Blessings 1-2", hebrew = "הלכות תפילה פרק ח, הלכות ברכות פרק 1-2"),
            HebcalLearningEntry(category = "shemiratHaLashon", title = "Book I, Hakdamah 3-5", hebrew = "Book I, הקדמה 3-5"),
            HebcalLearningEntry(category = "shemiratHaLashon", title = "Book I, Shaar HaTevuna 2.3-3.1", hebrew = "Book I, שער התבונה 2.3-3.1"),
            HebcalLearningEntry(category = "tanakhYomi", title = "II Kings 4", hebrew = "מלכים ב ד", memo = "II Kings 4:1-5:27"),
        ),
    )

    private fun dumpLearning() {
        val base: ZmanimDay = zmanimForDate(date = LocalDate.of(2026, 10, 7), now = null, hebrewDateRolled = false)
        for ((name, entries) in hebcalSets) {
            val state = base.withDailyLearningItems(entries.toZmanItems())
                .filterForDisplay(ZmanimTimeOption.Default, DailyLearningType.entries.toSet())
                .mergeRambamRows()
                .toUiState(use24HourTime = true, showCandleLightingPrompt = false, developerTimeOverrideActive = false, dayOffset = 0)
            for (hebrew in listOf(false, true)) dumpState("learning|$name|${if (hebrew) "he" else "en"}", state, text(hebrew))
            // Only one Rambam track shown.
            val single = base.withDailyLearningItems(entries.toZmanItems())
                .filterForDisplay(ZmanimTimeOption.Default, DailyLearningType.entries.toSet() - DailyLearningType.RambamYomi)
                .mergeRambamRows()
                .toUiState(use24HourTime = true, showCandleLightingPrompt = false, developerTimeOverrideActive = false, dayOffset = 0)
            for (hebrew in listOf(false, true)) dumpState("learning-single|$name|${if (hebrew) "he" else "en"}", single, text(hebrew))
        }
    }
}
