// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
    ) = comprehensiveZmanimCalendar(location, date, settings)

    private val sunrise = requireNotNull(calendar().sunrise(SunriseMethod.SeaLevel))
    private val sunset = requireNotNull(calendar().sunset(SunsetMethod.SeaLevel))

    /** Anything a Jerusalem summer day can legitimately produce, and nothing beyond it. */
    private val dayStart = sunrise.minus(Duration.ofHours(6))
    private val dayEnd = sunset.plus(Duration.ofHours(6))

    private fun assertWithinTheDay(label: String, time: Instant?) {
        assertNotNull("$label resolved to null in Jerusalem", time)
        assertTrue(
            "$label produced $time, outside $dayStart..$dayEnd",
            !time!!.isBefore(dayStart) && !time.isAfter(dayEnd),
        )
    }

    @Test
    fun everyAlotHashacharOptionResolves() {
        AlotHashacharMethod.entries.forEach { method ->
            val time = calendar(settings = defaults.copy(alotHashacharMethod = method))
                .alotHashachar(defaults.copy(alotHashacharMethod = method))
            assertWithinTheDay("Alot $method", time)
            assertTrue("Alot $method is not before sunrise", time!!.isBefore(sunrise))
        }
    }

    @Test
    fun everyMisheyakirOptionResolves() {
        MisheyakirMethod.entries.forEach { method ->
            val settings = defaults.copy(misheyakirMethod = method)
            val time = calendar(settings = settings).misheyakir(settings)
            assertWithinTheDay("Misheyakir $method", time)
            assertTrue("Misheyakir $method is not before sunrise", time!!.isBefore(sunrise))
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
            assertTrue("Tzeit $method is not after sunset", time!!.isAfter(sunset))
        }
        MotzeiShabbatMethod.entries.forEach { method ->
            val settings = defaults.copy(motzeiShabbatMethod = method)
            val time = calendar(settings = settings).motzeiShabbat(settings)
            assertWithinTheDay("Motzei $method", time)
            assertTrue("Motzei $method is not after sunset", time!!.isAfter(sunset))
        }
        RabbeinuTamMethod.entries.forEach { method ->
            val settings = defaults.copy(rabbeinuTamMethod = method)
            val time = calendar(settings = settings).rabbeinuTam(settings)
            assertWithinTheDay("Rabbeinu Tam $method", time)
            assertTrue("Rabbeinu Tam $method is not after sunset", time!!.isAfter(sunset))
        }
    }

    @Test
    fun everyChametzOptionResolvesInOrder() {
        // On Erev Pesach itself, the only day these rows are shown and the only day KosherJava gives
        // its own chametz deadlines at all (it returns null for them on any other date).
        val erevPesach = LocalDate.of(2026, 4, 1)
        val sunrise = requireNotNull(comprehensiveZmanimCalendar(defaultJerusalemLocation, erevPesach, defaults).seaLevelSunrise)
        val sunset = requireNotNull(comprehensiveZmanimCalendar(defaultJerusalemLocation, erevPesach, defaults).seaLevelSunset)
        ChametzMethod.entries.forEach { method ->
            val settings = defaults.copy(chametzMethod = method)
            val (eating, burning) = comprehensiveZmanimCalendar(defaultJerusalemLocation, erevPesach, settings).chametzTimes(settings)
            assertNotNull("Sof zman achilat chametz $method resolved to null", eating)
            assertNotNull("Sof zman biur chametz $method resolved to null", burning)
            assertTrue("$method: eating deadline must fall between sunrise and sunset", eating!! in sunrise..sunset)
            assertTrue("$method: burning deadline must fall between sunrise and sunset", burning!! in sunrise..sunset)
            assertTrue("$method: eating deadline must precede burning", eating.isBefore(burning))
        }
    }

    @Test
    fun everyCandleLightingOptionIsAWholeNumberOfMinutesBeforeSunset() {
        CandleLightingMethod.entries.forEach { method ->
            val settings = defaults.copy(candleLightingMethod = method)
            val lighting = requireNotNull(comprehensiveZmanimCalendar(defaultJerusalemLocation, date, settings).candleLighting(settings)?.time) {
                "Candle lighting $method resolved to null"
            }
            val minutesBefore = Duration.between(lighting, sunset).toMinutes()
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
            assertTrue("Custom alot $unit is not before sunrise", time!!.isBefore(sunrise))

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
            assertTrue("Custom tzeit $unit is not after sunset", tzeit!!.isAfter(sunset))
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
                .isBefore(requireNotNull(calendar(settings = shallow).alotHashachar(shallow))),
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
            comprehensiveZmanimCalendar(stockholm, midsummer, settings).alotHashachar(settings)
        }
        MisheyakirMethod.entries.forEach { method ->
            val settings = defaults.copy(misheyakirMethod = method)
            comprehensiveZmanimCalendar(stockholm, midsummer, settings).misheyakir(settings)
        }
        SofZmanShemaMethod.entries.forEach { method ->
            comprehensiveZmanimCalendar(stockholm, midsummer, defaults).sofZmanShema(method, defaults)
        }
        SofZmanTefillahMethod.entries.forEach { method ->
            comprehensiveZmanimCalendar(stockholm, midsummer, defaults).sofZmanTefillah(method, defaults)
        }
        TzeitHakochavimMethod.entries.forEach { method ->
            val settings = defaults.copy(tzeitHakochavimMethod = method)
            comprehensiveZmanimCalendar(stockholm, midsummer, settings).tzeit(settings)
        }
        RabbeinuTamMethod.entries.forEach { method ->
            val settings = defaults.copy(rabbeinuTamMethod = method)
            comprehensiveZmanimCalendar(stockholm, midsummer, settings).rabbeinuTam(settings)
        }
        ChametzMethod.entries.forEach { method ->
            val settings = defaults.copy(chametzMethod = method)
            comprehensiveZmanimCalendar(stockholm, midsummer, settings).chametzTimes(settings)
        }
        MinchaGedolaMethod.entries.forEach { method ->
            val settings = defaults.copy(minchaGedolaMethod = method)
            comprehensiveZmanimCalendar(stockholm, midsummer, settings).minchaGedola(settings)
        }
        MinchaKetanaMethod.entries.forEach { method ->
            val settings = defaults.copy(minchaKetanaMethod = method)
            comprehensiveZmanimCalendar(stockholm, midsummer, settings).minchaKetana(settings)
        }
        PlagHaminchaMethod.entries.forEach { method ->
            val settings = defaults.copy(plagHaminchaMethod = method)
            comprehensiveZmanimCalendar(stockholm, midsummer, settings).plagHamincha(settings)
        }
    }
}
