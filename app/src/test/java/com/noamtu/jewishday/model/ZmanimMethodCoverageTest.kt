// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every configurable option, not just the defaults — including the custom ones, which resolve
 * against the numbers a fresh install starts with.
 *
 * `ZmanMethodResolversTest` covers the default of each zman. A resolver wired to the wrong KosherJava
 * getter, or to one that returns null, does not fail loudly — the row simply renders "--", or worse, a
 * plausible time from the wrong opinion. These walk every entry of every enum.
 *
 * Bounds are deliberately generous. The point is to catch a null or a wildly wrong getter, not to
 * assert a halachic ordering between opinions that legitimately disagree by hours.
 */
class ZmanimMethodCoverageTest {

    private val date = LocalDate.of(2026, 7, 2)
    private val defaults = ZmanimCalculationSettings()

    private fun calendar(
        location: JewishLocation = defaultJerusalemLocation,
        settings: ZmanimCalculationSettings = defaults,
    ) = complexZmanimCalendar(location, date, settings)

    private val sunrise = requireNotNull(calendar().sunrise(SunriseMethod.SeaLevel))
    private val sunset = requireNotNull(calendar().sunset(SunsetMethod.SeaLevel))

    /** Anything a Jerusalem summer day can legitimately produce, and nothing beyond it. */
    private val dayStart = Date(sunrise.time - 6 * HourMillis)
    private val dayEnd = Date(sunset.time + 6 * HourMillis)

    private fun assertWithinTheDay(label: String, time: Date?) {
        assertNotNull("$label resolved to null in Jerusalem", time)
        assertTrue(
            "$label produced $time, outside $dayStart..$dayEnd",
            !time!!.before(dayStart) && !time.after(dayEnd),
        )
    }

    @Test
    fun everyAlotHashacharOptionResolves() {
        AlotHashacharMethod.entries.forEach { method ->
            val time = calendar(settings = defaults.copy(alotHashacharMethod = method))
                .alotHashachar(defaults.copy(alotHashacharMethod = method))
            assertWithinTheDay("Alot $method", time)
            assertTrue("Alot $method is not before sunrise", time!!.before(sunrise))
        }
    }

    @Test
    fun everyMisheyakirOptionResolves() {
        MisheyakirMethod.entries.forEach { method ->
            val settings = defaults.copy(misheyakirMethod = method)
            val time = calendar(settings = settings).misheyakir(settings)
            assertWithinTheDay("Misheyakir $method", time)
            assertTrue("Misheyakir $method is not before sunrise", time!!.before(sunrise))
        }
    }

    @Test
    fun everySunriseAndSunsetOptionResolves() {
        SunriseMethod.entries.forEach { assertWithinTheDay("Sunrise $it", calendar().sunrise(it)) }
        SunsetMethod.entries.forEach { assertWithinTheDay("Sunset $it", calendar().sunset(it)) }
    }

    @Test
    fun everySofZmanShemaOptionResolves() {
        SofZmanShemaMethod.entries.forEach { method ->
            assertWithinTheDay("Sof zman shema $method", calendar().sofZmanShema(method, defaults))
        }
    }

    @Test
    fun everySofZmanTefillahOptionResolves() {
        SofZmanTefillahMethod.entries.forEach { method ->
            assertWithinTheDay("Sof zman tefillah $method", calendar().sofZmanTefillah(method, defaults))
        }
    }

    @Test
    fun everyChatzotOptionResolves() {
        ChatzotMethod.entries.forEach { method ->
            assertWithinTheDay("Chatzot $method", calendar().chatzot(method))
            // Chatzot halaila is the following midnight, so it is allowed past the day window.
            assertNotNull("Chatzot halaila $method", calendar().chatzotHaLaila(method))
        }
    }

    @Test
    fun everyMinchaAndPlagOptionResolves() {
        MinchaGedolaMethod.entries.forEach { method ->
            val settings = defaults.copy(minchaGedolaMethod = method)
            assertWithinTheDay("Mincha gedola $method", calendar(settings = settings).minchaGedola(settings))
        }
        MinchaKetanaMethod.entries.forEach { method ->
            val settings = defaults.copy(minchaKetanaMethod = method)
            assertWithinTheDay("Mincha ketana $method", calendar(settings = settings).minchaKetana(settings))
        }
        PlagHaminchaMethod.entries.forEach { method ->
            val settings = defaults.copy(plagHaminchaMethod = method)
            assertWithinTheDay("Plag $method", calendar(settings = settings).plagHamincha(settings))
        }
    }

    @Test
    fun everyNightfallOptionResolves() {
        TzeitHakochavimMethod.entries.forEach { method ->
            val settings = defaults.copy(tzeitHakochavimMethod = method)
            val time = calendar(settings = settings).tzeit(settings)
            assertWithinTheDay("Tzeit $method", time)
            assertTrue("Tzeit $method is not after sunset", time!!.after(sunset))
        }
        MotzeiShabbatMethod.entries.forEach { method ->
            val settings = defaults.copy(motzeiShabbatMethod = method)
            val time = calendar(settings = settings).motzeiShabbat(settings)
            assertWithinTheDay("Motzei $method", time)
            assertTrue("Motzei $method is not after sunset", time!!.after(sunset))
        }
        RabbeinuTamMethod.entries.forEach { method ->
            val settings = defaults.copy(rabbeinuTamMethod = method)
            val time = calendar(settings = settings).rabbeinuTam(settings)
            assertWithinTheDay("Rabbeinu Tam $method", time)
            assertTrue("Rabbeinu Tam $method is not after sunset", time!!.after(sunset))
        }
    }

    @Test
    fun everyChametzOptionResolvesInOrder() {
        ChametzMethod.entries.forEach { method ->
            val settings = defaults.copy(chametzMethod = method)
            val (eating, burning) = calendar(settings = settings).chametzTimes(settings)
            assertWithinTheDay("Sof zman achilat chametz $method", eating)
            assertWithinTheDay("Sof zman biur chametz $method", burning)
            assertTrue("$method: eating deadline must precede burning", eating!!.before(burning!!))
        }
    }

    @Test
    fun everyCandleLightingOptionIsAWholeNumberOfMinutesBeforeSunset() {
        CandleLightingMethod.entries.forEach { method ->
            val settings = defaults.copy(candleLightingMethod = method)
            val lighting = requireNotNull(complexZmanimCalendar(defaultJerusalemLocation, date, settings).candleLighting) {
                "Candle lighting $method resolved to null"
            }
            val minutesBefore = (sunset.time - lighting.time) / 60_000L
            // The custom option takes its offset from the settings rather than from the enum.
            val expected = settings.candleLightingOffsetMinutes.toLong()
            assertTrue("$method reported $minutesBefore min before sunset", minutesBefore == expected)
        }
    }

    /**
     * Every custom option, in each of its three units, against a value that is not the default one —
     * the resolvers build these by hand rather than calling a KosherJava getter, so a unit wired to
     * the wrong end of the day would otherwise go unnoticed.
     */
    @Test
    fun everyCustomOptionHonoursTheValueItIsGiven() {
        val dawnUnits = mapOf(
            CustomZmanUnit.Degrees to 17.3,
            CustomZmanUnit.Minutes to 84.0,
            CustomZmanUnit.ZmaniyotMinutes to 84.0,
        )
        dawnUnits.forEach { (unit, value) ->
            val alot = defaults.copy(
                alotHashacharMethod = when (unit) {
                    CustomZmanUnit.Degrees -> AlotHashacharMethod.CustomDegrees
                    CustomZmanUnit.Minutes -> AlotHashacharMethod.CustomMinutes
                    CustomZmanUnit.ZmaniyotMinutes -> AlotHashacharMethod.CustomZmaniyotMinutes
                },
                alotHashacharCustom = defaults.alotHashacharCustom.withValue(unit, value),
            )
            val time = calendar(settings = alot).alotHashachar(alot)
            assertWithinTheDay("Custom alot $unit", time)
            assertTrue("Custom alot $unit is not before sunrise", time!!.before(sunrise))

            val nightfall = defaults.copy(
                tzeitHakochavimMethod = when (unit) {
                    CustomZmanUnit.Degrees -> TzeitHakochavimMethod.CustomDegrees
                    CustomZmanUnit.Minutes -> TzeitHakochavimMethod.CustomMinutes
                    CustomZmanUnit.ZmaniyotMinutes -> TzeitHakochavimMethod.CustomZmaniyotMinutes
                },
                tzeitHakochavimCustom = defaults.tzeitHakochavimCustom.withValue(unit, value),
            )
            val tzeit = calendar(settings = nightfall).tzeit(nightfall)
            assertWithinTheDay("Custom tzeit $unit", tzeit)
            assertTrue("Custom tzeit $unit is not after sunset", tzeit!!.after(sunset))
        }

        // A larger degree value is a lower sun, so dawn moves earlier and nightfall later.
        val shallow = defaults.copy(
            alotHashacharMethod = AlotHashacharMethod.CustomDegrees,
            alotHashacharCustom = defaults.alotHashacharCustom.withValue(CustomZmanUnit.Degrees, 14.0),
        )
        val deep = shallow.copy(
            alotHashacharCustom = shallow.alotHashacharCustom.withValue(CustomZmanUnit.Degrees, 20.0),
        )
        assertTrue(
            "20° dawn should precede 14° dawn",
            requireNotNull(calendar(settings = deep).alotHashachar(deep))
                .before(requireNotNull(calendar(settings = shallow).alotHashachar(shallow))),
        )
    }

    @Test
    fun noOptionThrowsWhereTheSunBarelySets() {
        // Stockholm in midsummer: many degree-based zmanim have no solution at all. Every option
        // must still come back with something or nothing — never an exception, and never a NaN
        // dressed up as a time. The app renders a null as "--", which is the honest answer.
        val stockholm = JewishLocation("Stockholm", 59.3293, 18.0686, 28.0, ZoneId.of("Europe/Stockholm"))
        val midsummer = LocalDate.of(2026, 6, 21)

        AlotHashacharMethod.entries.forEach { method ->
            val settings = defaults.copy(alotHashacharMethod = method)
            complexZmanimCalendar(stockholm, midsummer, settings).alotHashachar(settings)
        }
        MisheyakirMethod.entries.forEach { method ->
            val settings = defaults.copy(misheyakirMethod = method)
            complexZmanimCalendar(stockholm, midsummer, settings).misheyakir(settings)
        }
        SofZmanShemaMethod.entries.forEach { method ->
            complexZmanimCalendar(stockholm, midsummer, defaults).sofZmanShema(method, defaults)
        }
        SofZmanTefillahMethod.entries.forEach { method ->
            complexZmanimCalendar(stockholm, midsummer, defaults).sofZmanTefillah(method, defaults)
        }
        TzeitHakochavimMethod.entries.forEach { method ->
            val settings = defaults.copy(tzeitHakochavimMethod = method)
            complexZmanimCalendar(stockholm, midsummer, settings).tzeit(settings)
        }
        RabbeinuTamMethod.entries.forEach { method ->
            val settings = defaults.copy(rabbeinuTamMethod = method)
            complexZmanimCalendar(stockholm, midsummer, settings).rabbeinuTam(settings)
        }
        ChametzMethod.entries.forEach { method ->
            val settings = defaults.copy(chametzMethod = method)
            complexZmanimCalendar(stockholm, midsummer, settings).chametzTimes(settings)
        }
        MinchaGedolaMethod.entries.forEach { method ->
            val settings = defaults.copy(minchaGedolaMethod = method)
            complexZmanimCalendar(stockholm, midsummer, settings).minchaGedola(settings)
        }
        MinchaKetanaMethod.entries.forEach { method ->
            val settings = defaults.copy(minchaKetanaMethod = method)
            complexZmanimCalendar(stockholm, midsummer, settings).minchaKetana(settings)
        }
        PlagHaminchaMethod.entries.forEach { method ->
            val settings = defaults.copy(plagHaminchaMethod = method)
            complexZmanimCalendar(stockholm, midsummer, settings).plagHamincha(settings)
        }
    }

    private companion object {
        const val HourMillis = 60L * 60L * 1000L
    }
}
