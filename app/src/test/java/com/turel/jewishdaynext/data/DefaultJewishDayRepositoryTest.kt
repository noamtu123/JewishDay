package com.turel.jewishdaynext.data

import com.turel.jewishdaynext.model.defaultJerusalemLocation
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultJewishDayRepositoryTest {
    @Test
    fun getTodayUsesInjectedClockDate() {
        val repository = DefaultJewishDayRepository(
            Clock.fixed(Instant.parse("2024-04-23T10:00:00Z"), ZoneId.of("UTC")),
        )

        assertEquals(LocalDate.of(2024, 4, 23), repository.getToday().gregorianDate)
    }

    @Test
    fun getZmanimUsesInjectedClockDateInLocationZone() {
        val repository = DefaultJewishDayRepository(
            Clock.fixed(Instant.parse("2024-04-22T22:30:00Z"), ZoneId.of("UTC")),
        )

        assertEquals(LocalDate.of(2024, 4, 23), repository.getZmanim(defaultJerusalemLocation).date)
    }
}
