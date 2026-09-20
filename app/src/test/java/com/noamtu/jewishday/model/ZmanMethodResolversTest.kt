// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sanity coverage for the halachic core: the default method of every daily zman resolves
 * to a time in Jerusalem and the day is internally ordered, the custom-degree methods
 * added by hand (rather than via a KosherJava getter) resolve, and the hand-computed
 * MGA 72-zmaniyot chametz times are consistent with each other.
 */
class ZmanMethodResolversTest {
    private val settings = ZmanimCalculationSettings()
    private val calendar = complexZmanimCalendar(
        location = defaultJerusalemLocation,
        date = LocalDate.of(2026, 7, 2),
        settings = settings,
    )

    @Test
    fun defaultMethodsProduceAFullyOrderedDayInJerusalem() {
        val orderedZmanim = listOf(
            "alot" to calendar.alotHashachar(settings),
            "misheyakir" to calendar.misheyakir(settings),
            "sunrise" to calendar.sunrise(settings.sunriseMethod),
            "sof zman shema MGA" to calendar.sofZmanShema(settings.sofZmanShemaMethod, settings),
            "sof zman shema GRA" to calendar.sofZmanShema(settings.sofZmanShemaGraMethod, settings),
            "sof zman tefillah GRA" to calendar.sofZmanTefillah(settings.sofZmanTefillahGraMethod, settings),
            "chatzot" to calendar.chatzot(settings.chatzotMethod),
            "mincha gedola" to calendar.minchaGedola(settings),
            "mincha ketana" to calendar.minchaKetana(settings),
            "plag hamincha" to calendar.plagHamincha(settings),
            "sunset" to calendar.sunset(settings.sunsetMethod),
            "tzeit" to calendar.tzeit(settings),
        )

        orderedZmanim.forEach { (name, time) -> assertNotNull("$name resolved to null", time) }
        orderedZmanim.zipWithNext { (earlierName, earlier), (laterName, later) ->
            assertTrue(
                "$earlierName (${earlier}) should be before $laterName (${later})",
                earlier!!.before(later!!),
            )
        }
    }

    @Test
    fun handRolledDegreeMethodsResolveInJerusalem() {
        val sunset = calendar.sunset(settings.sunsetMethod)!!

        // These use getSunsetOffsetByDegrees directly (no dedicated KosherJava getter), the default
        // among them and any value the user types in alike.
        listOf(
            settings,
            settings.copy(
                tzeitHakochavimMethod = TzeitHakochavimMethod.CustomDegrees,
                tzeitHakochavimCustom = settings.tzeitHakochavimCustom.withValue(CustomZmanUnit.Degrees, 4.42),
            ),
            settings.copy(
                tzeitHakochavimMethod = TzeitHakochavimMethod.CustomDegrees,
                tzeitHakochavimCustom = settings.tzeitHakochavimCustom.withValue(CustomZmanUnit.Degrees, 4.66),
            ),
        ).forEach { candidate ->
            val tzeit = calendar.tzeit(candidate)
            assertNotNull("$candidate resolved to null", tzeit)
            assertTrue("$candidate should be after sunset", tzeit!!.after(sunset))
        }
    }

    /**
     * The custom zmaniyot-minute options are computed here rather than through a KosherJava getter:
     * a 72-zmaniyot day must reproduce the library's own alos72Zmanis, and the chametz deadlines it
     * feeds must come out in order after it.
     */
    @Test
    fun customZmaniyotChametzTimesMatchTheLibrarysOwn72ZmaniyotDawn() {
        val custom = settings.copy(
            chametzMethod = ChametzMethod.CustomZmaniyotMinutes,
            chametzCustom = settings.chametzCustom.withValue(CustomZmanUnit.ZmaniyotMinutes, 72.0),
        )
        val (achilah, biur) = calendar.chametzTimes(custom)

        assertNotNull(achilah)
        assertNotNull(biur)
        assertTrue("eating deadline must precede burning deadline", achilah!!.before(biur!!))
        assertTrue("both must fall after the 72-zmaniyot dawn", achilah.after(calendar.alos72Zmanis))
    }

    /**
     * Four named options were dropped because they were only a degree value, and an install that had
     * one is migrated to that value. That is only honest if the times are identical, so this asserts
     * it against the KosherJava getters the options used to call.
     */
    @Test
    fun theDroppedNamedOptionsAreExactlyTheDegreeValuesTheyMigrateTo() {
        fun dawnAt(degrees: Double) = settings.copy(
            alotHashacharMethod = AlotHashacharMethod.CustomDegrees,
            alotHashacharCustom = settings.alotHashacharCustom.withValue(CustomZmanUnit.Degrees, degrees),
        ).let { calendar.alotHashachar(it) }

        fun nightfallAt(degrees: Double) = settings.copy(
            tzeitHakochavimMethod = TzeitHakochavimMethod.CustomDegrees,
            tzeitHakochavimCustom = settings.tzeitHakochavimCustom.withValue(CustomZmanUnit.Degrees, degrees),
        ).let { calendar.tzeit(it) }

        fun rabbeinuTamAt(degrees: Double) = settings.copy(
            rabbeinuTamMethod = RabbeinuTamMethod.CustomDegrees,
            rabbeinuTamCustom = settings.rabbeinuTamCustom.withValue(CustomZmanUnit.Degrees, degrees),
        ).let { calendar.rabbeinuTam(it) }

        // The Baal Hatanya's dawn and nightfall.
        assertEquals(calendar.alosBaalHatanya, dawnAt(16.9))
        assertEquals(calendar.tzaisBaalHatanya, nightfallAt(6.0))
        // And Rabbeinu Tam's Bein Hashmashot by degrees.
        assertEquals(calendar.bainHashmashosRT13Point24Degrees, rabbeinuTamAt(13.24))
    }

    /**
     * The GRA's day is sunrise to sunset, so the sunrise and sunset the user picked are the ones it is
     * measured between — that choice is the only place elevation is decided, and it has to reach these
     * rows. Ein Gedi is used because a real elevation is needed for the two to differ at all.
     */
    @Test
    fun theGraRowsFollowTheChosenSunriseAndSunset() {
        val mountain = JewishLocation("Tzfat", 32.9646, 35.4960, 900.0, defaultJerusalemLocation.zoneId)
        val date = LocalDate.of(2026, 7, 2)
        fun on(sunriseMethod: SunriseMethod, sunsetMethod: SunsetMethod): List<java.util.Date?> {
            val chosen = settings.copy(sunriseMethod = sunriseMethod, sunsetMethod = sunsetMethod)
            val calendar = complexZmanimCalendar(mountain, date, chosen)
            return listOf(
                calendar.sofZmanShema(chosen.sofZmanShemaGraMethod, chosen),
                calendar.sofZmanTefillah(chosen.sofZmanTefillahGraMethod, chosen),
                calendar.minchaGedola(chosen),
                calendar.minchaKetana(chosen),
                calendar.plagHamincha(chosen),
            )
        }

        val atSeaLevel = on(SunriseMethod.SeaLevel, SunsetMethod.SeaLevel)
        val observed = on(SunriseMethod.ElevationAdjusted, SunsetMethod.ElevationAdjusted)

        atSeaLevel.zip(observed).forEachIndexed { index, (sea, seen) ->
            assertNotNull("row $index resolved to null at sea level", sea)
            assertNotNull("row $index resolved to null observed", seen)
            assertTrue(
                "row $index did not follow the sunrise/sunset choice: $sea vs $seen",
                sea != seen,
            )
        }
    }

    @Test
    fun aCustomZmaniyotDawnIsTheSameTimeTheLibraryCalls72Zmaniyot() {
        val custom = settings.copy(
            alotHashacharMethod = AlotHashacharMethod.CustomZmaniyotMinutes,
            alotHashacharCustom = settings.alotHashacharCustom.withValue(CustomZmanUnit.ZmaniyotMinutes, 72.0),
        )
        // Within a second: the library rounds its own offset through a different intermediate.
        val ours = requireNotNull(calendar.alotHashachar(custom)).time
        val theirs = requireNotNull(calendar.alos72Zmanis).time
        assertTrue("custom 72 zmaniyot was $ours, library said $theirs", Math.abs(ours - theirs) < 1_000)
    }
}