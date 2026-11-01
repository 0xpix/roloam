package com.roloam.app.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ReliableTripPreviewTest {
    private val saturday = LocalDate.of(2026, 10, 17)

    @Test fun parsesTransitousThreeHourJourneyWithCorrectBerlinLocalTimes() {
        val json = """
            {"itineraries":[{"startTime":"2026-10-17T06:00:00Z",
              "endTime":"2026-10-17T09:00:00Z","duration":10800,
              "transfers":1,"legs":[
                {"mode":"WALK"}, {"mode":"REGIONAL_RAIL"},
                {"mode":"WALK"}, {"mode":"HIGHSPEED_RAIL"}, {"mode":"WALK"}
              ]}]}
        """.trimIndent()
        val adapter = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            .adapter(TransitousPlanResponse::class.java)
        val journey = adapter.fromJson(json)!!.itineraries.first()
        val verified = TrainJourneyParser.verify(journey, saturday)!!
        assertEquals(180, verified.durationMinutes)
        assertEquals(1, verified.transfers)
        assertEquals("Transitous timetable", verified.provider)
        assertEquals("08:00", verified.departureIso.substring(11, 16))
        assertEquals("11:00", verified.arrivalIso.substring(11, 16))
    }

    @Test fun busOnlyJourneyIsNotRepresentedAsTrain() {
        val bus = TransitousItinerary(
            "2026-10-17T06:00:00Z", "2026-10-17T09:00:00Z",
            legs = listOf(TransitousLeg("WALK"), TransitousLeg("BUS"), TransitousLeg("TRAM"))
        )
        assertNull(TrainJourneyParser.verify(bus, saturday))
    }

    @Test fun rejectsWrongDateBadTimesAndCancelledLeg() {
        val valid = TransitousItinerary(
            "2026-10-17T06:00:00Z", "2026-10-17T09:00:00Z",
            legs = listOf(TransitousLeg("REGIONAL_RAIL"))
        )
        assertNull(TrainJourneyParser.verify(valid, saturday.plusDays(1)))
        val invalid = valid.copy(endTime="2026-10-17T05:30:00Z")
        assertNull(TrainJourneyParser.verify(invalid, saturday))
        assertNull(TrainJourneyParser.verify(valid.copy(
            legs = listOf(TransitousLeg("REGIONAL_RAIL", cancelled = true))
        ), saturday))
    }

    @Test fun handlesWinterTimeAndMidnightUtcWithoutShiftingTravelDate() {
        val winter = LocalDate.of(2026, 11, 7)
        val valid = TransitousItinerary(
            "2026-11-06T23:30:00Z", "2026-11-07T01:30:00Z",
            transfers = 0, legs = listOf(TransitousLeg("SUBURBAN"))
        )
        val verified = TrainJourneyParser.verify(valid, winter)!!
        assertEquals(120, verified.durationMinutes)
        assertEquals("00:30", verified.departureIso.substring(11, 16))
        assertEquals("02:30", verified.arrivalIso.substring(11, 16))
    }

    @Test fun filtersNamedGravesAndMilestonesButKeepsTouristSites() {
        assertFalse(AttractionQuality.isUseful(mapOf("historic" to "memorial"), "V.E. Becker"))
        assertFalse(AttractionQuality.isUseful(mapOf("historic" to "milestone"), "Berliner Meilenstein"))
        assertFalse(AttractionQuality.isUseful(mapOf("tourism" to "attraction"), "Klara Schloss, geb. Gruenebaum"))
        assertTrue(AttractionQuality.isUseful(mapOf("historic" to "castle"), "Würzburg Fortress"))
        assertTrue(AttractionQuality.isUseful(mapOf("tourism" to "museum"), "Mainfränkisches Museum"))
        assertTrue(AttractionQuality.isUseful(mapOf("natural" to "waterfall"), "River Waterfall"))
        assertFalse(AttractionQuality.isUseful(mapOf("historic" to "yes"), "Random old building"))
    }
}
