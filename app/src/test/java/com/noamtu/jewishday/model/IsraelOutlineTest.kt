// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import java.time.ZoneId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Israel vs. diaspora decides how many days of Yom Tov the app shows, which parsha schedule it
 * follows, and what it asks Hebcal for — so the geographic test behind it is worth pinning.
 *
 * The rectangle this replaced reached Metula in the north and the Golan in the east, and in doing
 * so also covered Tyre in southern Lebanon and Irbid in northwestern Jordan. Those two are here as
 * named regressions.
 */
class IsraelOutlineTest {

    private fun at(latitude: Double, longitude: Double) = JewishLocation(
        name = "Test",
        latitude = latitude,
        longitude = longitude,
        elevationMeters = 0.0,
        zoneId = ZoneId.of("UTC"),
    )

    @Test
    fun israeliCitiesUseTheIsraelCalendar() {
        val inside = mapOf(
            "Jerusalem" to (31.7780 to 35.2354),
            "Tel Aviv" to (32.0853 to 34.7818),
            "Bnei Brak" to (32.0807 to 34.8338),
            "Haifa" to (32.7940 to 34.9896),
            "Beer Sheva" to (31.2530 to 34.7915),
            "Eilat" to (29.5577 to 34.9519),
            "Metula" to (33.2790 to 35.5786),
            "Tzfat" to (32.9646 to 35.4960),
            "Katzrin (Golan)" to (32.9917 to 35.6897),
            "Hebron (Judea)" to (31.5326 to 35.0998),
            "Ariel (Samaria)" to (32.1056 to 35.1878),
            "Ein Gedi (Dead Sea)" to (31.4617 to 35.3889),
        )
        inside.forEach { (name, coordinates) ->
            val (latitude, longitude) = coordinates
            assertTrue(name, at(latitude, longitude).isInIsrael)
        }
    }

    @Test
    fun neighbouringCountriesUseTheDiasporaCalendar() {
        val outside = mapOf(
            // The two the old bounding box got wrong.
            "Tyre, Lebanon" to (33.2705 to 35.2038),
            "Irbid, Jordan" to (32.5556 to 35.8500),
            // And the ones it already excluded, kept so a wider outline can't quietly swallow them.
            "Sidon, Lebanon" to (33.5571 to 35.3729),
            "Beirut, Lebanon" to (33.8938 to 35.5018),
            "Amman, Jordan" to (31.9539 to 35.9106),
            "Aqaba, Jordan" to (29.5321 to 35.0063),
            "Damascus, Syria" to (33.5138 to 36.2765),
            "Cairo, Egypt" to (30.0444 to 31.2357),
            "Nicosia, Cyprus" to (35.1856 to 33.3823),
        )
        outside.forEach { (name, coordinates) ->
            val (latitude, longitude) = coordinates
            assertFalse(name, at(latitude, longitude).isInIsrael)
        }
    }

    @Test
    fun distantAndDegenerateCoordinatesAreDiaspora() {
        assertFalse(at(40.7128, -74.0060).isInIsrael) // New York
        assertFalse(at(-37.8136, 144.9631).isInIsrael) // Melbourne
        assertFalse(at(0.0, 0.0).isInIsrael)
        assertFalse(at(Double.NaN, 35.2).isInIsrael)
        assertFalse(at(31.7, Double.NaN).isInIsrael)
    }
}
