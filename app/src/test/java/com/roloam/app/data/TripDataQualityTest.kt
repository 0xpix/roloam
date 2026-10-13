package com.roloam.app.data

import com.roloam.app.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TripDataQualityTest {
    private val heidelberg = GeoPoint(49.3988, 8.6724)
    private val start = LocalDate.of(2026, 10, 17)

    private fun plan(): TripPlan {
        val stops = (1..4).map { index ->
            TripStop(
                Place(index.toLong(), "Place $index",
                    GeoPoint(49.3988 + index * 0.001, 8.6724 + index * 0.001),
                    "attraction", sourceRef = "osm:node/$index"),
                "11:00", 45, "Verified"
            )
        }
        return TripPlan(
            originLabel = "Heidelberg",
            origin = heidelberg,
            destination = Destination("Freiburg", point = GeoPoint(47.999, 7.8421)),
            days = 2,
            startDate = start,
            stay = Stay(100, "Real campsite", GeoPoint(48.0, 7.85),
                "camp_site", sourceRef = "osm:way/100"),
            alternativeStays = emptyList(),
            itinerary = listOf(
                TripDay(start, stops.take(2), null),
                TripDay(start.plusDays(1), stops.drop(2), null)
            )
        )
    }

    @Test fun rejectsInvalidCoordinates() {
        assertTrue(TripDataQuality.validPoint(heidelberg))
        assertFalse(TripDataQuality.validPoint(GeoPoint(Double.NaN, 8.0)))
        assertFalse(TripDataQuality.validPoint(GeoPoint(92.0, 8.0)))
        assertFalse(TripDataQuality.validPoint(GeoPoint(50.0, 181.0)))
        assertNull(OverpassElement(2, lat = 95.0, lon = 8.0).point())
    }

    @Test fun requiresPlausibleRealRoutingMetrics() {
        assertTrue(TripDataQuality.validRoute(RouteMetric(90, 100.0), 85.0))
        assertFalse(TripDataQuality.validRoute(RouteMetric(0, 100.0), 85.0))
        assertFalse(TripDataQuality.validRoute(RouteMetric(90, 1.0), 85.0))
        assertFalse(TripDataQuality.validRoute(RouteMetric(90, Double.NaN), 85.0))
        assertFalse(TripDataQuality.validRoute(null, 85.0))
    }

    @Test fun onlyAcceptsWebLinksWithHttpSchemeAndHost() {
        assertEquals("https://example.org/visit",
            TripDataQuality.website(" https://example.org/visit "))
        assertNull(TripDataQuality.website("javascript:alert(1)"))
        assertNull(TripDataQuality.website("https://"))
        assertNull(TripDataQuality.website("example.org"))
    }

    @Test fun keepsSameNamedDifferentMappedPlacesDistinct() {
        val input = listOf(
            Place(1, "Old Town", GeoPoint(49.1, 8.1), "historic", sourceRef = "osm:node/1"),
            Place(2, "Old Town", GeoPoint(48.1, 9.1), "historic", sourceRef = "osm:way/2"),
            Place(1, "Old Town", GeoPoint(49.1, 8.1), "historic", sourceRef = "osm:node/1")
        )
        assertEquals(2, TripDataQuality.distinctPlaces(input).size)
        assertEquals("osm:way/71",
            OverpassElement(71, type = "way", lat = 49.0, lon = 8.0).sourceRef())
    }

    @Test fun verifiedTripRequiresActualActivitiesEveryDayAndAccommodation() {
        val good = plan()
        assertTrue(TripDataQuality.problems(good).isEmpty())
        assertTrue(TripDataQuality.problems(good.copy(stay = null))
            .contains("no verified accommodation"))
        assertTrue(TripDataQuality.problems(good.copy(
            itinerary = good.itinerary.mapIndexed { i, day ->
                if (i == 1) day.copy(stops = emptyList()) else day
            }
        )).isNotEmpty())
        val fake = TripStop(
            Place(-1, "City centre (fallback)", heidelberg, "city"),
            "12:00", 60, "fallback"
        )
        assertTrue(TripDataQuality.problems(good.copy(
            itinerary = listOf(
                good.itinerary[0].copy(stops = listOf(fake) + good.itinerary[0].stops),
                good.itinerary[1]
            )
        )).contains("invalid or placeholder stop"))
    }
}
