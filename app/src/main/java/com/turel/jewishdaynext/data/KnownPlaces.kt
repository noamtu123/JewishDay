package com.turel.jewishdaynext.data

import java.time.ZoneId

val offlineKnownPlaces = listOf(
    defaultSavedPlace,
    SavedPlace("new_york", "New York", 40.7128, -74.0060, 10.0, ZoneId.of("America/New_York")),
    SavedPlace("brooklyn", "Brooklyn", 40.6782, -73.9442, 12.0, ZoneId.of("America/New_York")),
    SavedPlace("lakewood", "Lakewood", 40.0821, -74.2097, 20.0, ZoneId.of("America/New_York")),
    SavedPlace("monsey", "Monsey", 41.1112, -74.0685, 166.0, ZoneId.of("America/New_York")),
    SavedPlace("miami", "Miami", 25.7617, -80.1918, 2.0, ZoneId.of("America/New_York")),
    SavedPlace("los_angeles", "Los Angeles", 34.0522, -118.2437, 71.0, ZoneId.of("America/Los_Angeles")),
    SavedPlace("chicago", "Chicago", 41.8781, -87.6298, 181.0, ZoneId.of("America/Chicago")),
    SavedPlace("toronto", "Toronto", 43.6532, -79.3832, 76.0, ZoneId.of("America/Toronto")),
    SavedPlace("montreal", "Montreal", 45.5019, -73.5674, 233.0, ZoneId.of("America/Toronto")),
    SavedPlace("london", "London", 51.5072, -0.1276, 11.0, ZoneId.of("Europe/London")),
    SavedPlace("manchester", "Manchester", 53.4808, -2.2426, 38.0, ZoneId.of("Europe/London")),
    SavedPlace("paris", "Paris", 48.8566, 2.3522, 35.0, ZoneId.of("Europe/Paris")),
    SavedPlace("antwerp", "Antwerp", 51.2194, 4.4025, 8.0, ZoneId.of("Europe/Brussels")),
    SavedPlace("zurich", "Zurich", 47.3769, 8.5417, 408.0, ZoneId.of("Europe/Zurich")),
    SavedPlace("buenos_aires", "Buenos Aires", -34.6037, -58.3816, 25.0, ZoneId.of("America/Argentina/Buenos_Aires")),
    SavedPlace("melbourne", "Melbourne", -37.8136, 144.9631, 31.0, ZoneId.of("Australia/Melbourne")),
    SavedPlace("sydney", "Sydney", -33.8688, 151.2093, 58.0, ZoneId.of("Australia/Sydney")),
    SavedPlace("johannesburg", "Johannesburg", -26.2041, 28.0473, 1753.0, ZoneId.of("Africa/Johannesburg")),
    SavedPlace("tel_aviv", "Tel Aviv", 32.0853, 34.7818, 5.0, ZoneId.of("Asia/Jerusalem")),
    SavedPlace("bnei_brak", "Bnei Brak", 32.0807, 34.8338, 22.0, ZoneId.of("Asia/Jerusalem")),
    SavedPlace("beit_shemesh", "Beit Shemesh", 31.7470, 34.9881, 305.0, ZoneId.of("Asia/Jerusalem")),
    SavedPlace("modiin_illit", "Modiin Illit", 31.9321, 35.0442, 286.0, ZoneId.of("Asia/Jerusalem")),
    SavedPlace("beitar_illit", "Beitar Illit", 31.6976, 35.1110, 749.0, ZoneId.of("Asia/Jerusalem")),
    SavedPlace("safed", "Safed", 32.9658, 35.4983, 900.0, ZoneId.of("Asia/Jerusalem")),
    SavedPlace("tiberias", "Tiberias", 32.7959, 35.5309, -200.0, ZoneId.of("Asia/Jerusalem")),
)
