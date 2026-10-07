package com.roloam.app

import com.roloam.app.data.haversine
import com.roloam.app.model.GeoPoint
import org.junit.Assert.assertTrue
import org.junit.Test

class SmokeTest {
    @Test fun heidelbergToCologneDistanceIsPlausible() {
        val km = haversine(GeoPoint(49.3988,8.6724), GeoPoint(50.9375,6.9603))
        assertTrue(km in 200.0..300.0)
    }
}
