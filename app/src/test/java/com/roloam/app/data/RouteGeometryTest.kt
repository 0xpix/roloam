package com.roloam.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteGeometryTest {
    @Test fun geoJsonLongitudeLatitudeIsDecodedCorrectly() {
        val route = OsrmRoutes(listOf(OsrmRoute(OsrmGeometry(listOf(
            listOf(8.6724, 49.3988), listOf(7.7521, 48.5734)
        )))))
        val decoded = decodeRouteGeometry(route)
        assertEquals(2, decoded.size)
        assertEquals(49.3988, decoded.first().lat, 0.00001)
        assertEquals(8.6724, decoded.first().lon, 0.00001)
    }

    @Test fun emptyAndInvalidCoordinatesFailSafely() {
        assertTrue(decodeRouteGeometry(null).isEmpty())
        val invalid = OsrmRoutes(listOf(OsrmRoute(OsrmGeometry(
            listOf(listOf(999.0, 999.0), listOf(8.6), listOf(8.7, 49.4))
        ))))
        assertEquals(1, decodeRouteGeometry(invalid).size)
    }
}
