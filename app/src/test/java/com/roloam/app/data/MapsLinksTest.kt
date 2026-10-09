package com.roloam.app.data

import com.roloam.app.model.Destination
import com.roloam.app.model.GeoPoint
import com.roloam.app.model.TransportMode
import org.junit.Assert.assertTrue
import org.junit.Test

class MapsLinksTest {
    @Test fun directionsIncludesTravelModeAndBothCoordinates() {
        val url = MapsLinks.directions(GeoPoint(49.4, 8.7), GeoPoint(48.5, 7.7), TransportMode.BIKE)
        assertTrue(url.startsWith("https://www.google.com/maps/dir/?api=1"))
        assertTrue(url.contains("travelmode=bicycling"))
        assertTrue(url.contains("49.4%2C8.7"))
        assertTrue(url.contains("48.5%2C7.7"))
    }

    @Test fun staySearchUsesDestinationInsteadOfPhoneLocation() {
        val url = MapsLinks.searchStays(Destination("Strasbourg", point = GeoPoint(48.57, 7.75)), true)
        assertTrue(url.contains("Strasbourg"))
        assertTrue(url.contains("camping"))
        assertTrue(url.contains("48.57"))
    }
}
