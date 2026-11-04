package com.roloam.app.data

import com.roloam.app.model.GeoPoint
import org.junit.Assert.*
import org.junit.Test

class DestinationSeedsTest {
    @Test fun germanOriginsHaveImmediateRealCityChoices() {
        val nearHeidelberg = DestinationSeeds.nearby(GeoPoint(49.3988, 8.6724), 220)
        assertTrue(nearHeidelberg.size >= 10)
        assertTrue(nearHeidelberg.any { it.name == "Strasbourg" })

        val nearStuttgart = DestinationSeeds.nearby(GeoPoint(48.7758, 9.1829), 220)
        assertTrue(nearStuttgart.size >= 10)
        assertTrue(nearStuttgart.any { it.name == "Tübingen" })
    }

    @Test fun neverPresentsFarAwaySeedCitiesAsNearby() {
        val walkRadius = DestinationSeeds.nearby(GeoPoint(48.7758, 9.1829), 25)
        assertFalse(walkRadius.any { it.name == "Berlin" })
        assertTrue(walkRadius.all { haversine(GeoPoint(48.7758, 9.1829), it.point) <= 25 })
    }
}
