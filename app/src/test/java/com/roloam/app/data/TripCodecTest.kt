package com.roloam.app.data

import com.roloam.app.model.*
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripCodecTest {
    @Test fun saveAndRestorePlanWithDateAndStops() {
        val date = LocalDate.of(2026, 10, 10)
        val point = GeoPoint(49.4, 8.7)
        val place = Place(12, "Old Bridge", point, "attraction", website = "https://example.org")
        val stop = TripStop(place, "11:00", 45, "Compact route")
        val plan = TripPlan(
            "Home", point, Destination("Heidelberg", point = point, routeEstimated = true),
            2, date, Stay(42, "Hostel", point, "hostel"),
            emptyList(), listOf(
                TripDay(date, listOf(stop), null),
                TripDay(date.plusDays(1), emptyList(),
                    WeatherDay(date.plusDays(1), 3, 16.0, 7.0, 20, null, null))
            ), generatedAtEpochMs = 101L
        )
        val saved = TripCodec.decode(TripCodec.encode(plan))
        assertEquals(plan, saved)
        assertEquals("2026-10-10", saved.startDate.toString())
        assertEquals("Old Bridge", saved.itinerary.first().stops.first().place.name)
        assertTrue(saved.destination.routeEstimated)
    }

    @Test fun corruptDataIsNotSilentlyAccepted() {
        val error = runCatching { TripCodec.decode("{invalid") }.exceptionOrNull()
        assertTrue(error != null)
    }
}
