package com.roloam.app.data

import com.roloam.app.model.GeoPoint
import com.roloam.app.model.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PlaceIdentityTest {
    @Test fun capitalizationAndWhitespaceDoNotDuplicateVisits() {
        val p=GeoPoint(49.3988,8.6724)
        assertEquals(
            PlaceIdentity.key(Place(1,"  Old Bridge ",p,"historic")),
            PlaceIdentity.key(Place(2,"old bridge",p,"attraction"))
        )
    }
    @Test fun namesInDifferentCitiesStayDistinct() {
        assertNotEquals(
            PlaceIdentity.key(Place(1,"Museum",GeoPoint(49.3988,8.6724),"museum")),
            PlaceIdentity.key(Place(2,"Museum",GeoPoint(49.4521,11.0767),"museum"))
        )
    }
}
