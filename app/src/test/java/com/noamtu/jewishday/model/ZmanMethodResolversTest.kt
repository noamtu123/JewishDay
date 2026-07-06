package com.noamtu.jewishday.model

import java.time.LocalDate
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

        // These use getSunsetOffsetByDegrees directly (no dedicated KosherJava getter).
        listOf(
            TzeitHakochavimMethod.Degrees6Point2,
            TzeitHakochavimMethod.Geonim4Point42,
            TzeitHakochavimMethod.Geonim4Point66,
        ).forEach { method ->
            val tzeit = calendar.tzeit(method)
            assertNotNull("$method resolved to null", tzeit)
            assertTrue("$method should be after sunset", tzeit!!.after(sunset))
        }
    }

    @Test
    fun mga72ZmanisChametzTimesAreOrderedAfterItsDawn() {
        val (achilah, biur) = calendar.chametzTimes(ChametzMethod.Mga72Zmanis)

        assertNotNull(achilah)
        assertNotNull(biur)
        assertTrue("eating deadline must precede burning deadline", achilah!!.before(biur!!))
        assertTrue("both must fall after the 72-zmaniyot dawn", achilah.after(calendar.alos72Zmanis))
    }
}
