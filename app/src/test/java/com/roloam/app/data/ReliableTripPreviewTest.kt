package com.roloam.app.data

import com.roloam.app.model.GeoPoint
import com.roloam.app.model.Destination
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ReliableTripPreviewTest {
    private val saturday = LocalDate.of(2026, 10, 17)

    @Test fun parsesScheduledThreeHourTrainJourneyWithTransfer() {
        val json = """
            {"journeys":[{"legs":[
                {"departure":"2026-10-17T08:00:00+02:00",
                 "arrival":"2026-10-17T09:00:00+02:00",
                 "line":{"mode":"train","product":"regional"}},
                {"departure":"2026-10-17T09:10:00+02:00",
                 "arrival":"2026-10-17T11:00:00+02:00",
                 "line":{"mode":"train","product":"regional"}}
            ]}]}
        """.trimIndent()
        val adapter = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            .adapter(DbJourneyResponse::class.java)
        val journey = adapter.fromJson(json)!!.journeys.first()
        val verified = TrainJourneyParser.verify(journey, saturday)!!
        assertEquals(180, verified.durationMinutes)
        assertEquals(1, verified.transfers)
        assertEquals("DB timetable", verified.provider)
    }

    @Test fun busOnlyJourneyIsNotRepresentedAsTrain() {
        val bus = DbJourney(listOf(
            DbJourneyLeg("2026-10-17T08:00:00+02:00",
                arrival="2026-10-17T11:00:00+02:00",
                line=DbJourneyLine("bus", "bus"))
        ))
        assertNull(TrainJourneyParser.verify(bus, saturday))
    }

    @Test fun rejectsWrongDateAndBadTimes() {
        val valid = DbJourney(listOf(DbJourneyLeg(
            "2026-10-17T08:00:00+02:00",
            arrival="2026-10-17T11:00:00+02:00",
            line=DbJourneyLine("train","nationalExpress")
        )))
        assertNull(TrainJourneyParser.verify(valid, saturday.plusDays(1)))
        val invalid = valid.copy(legs=listOf(valid.legs.first().copy(
            arrival="2026-10-17T07:30:00+02:00"
        )))
        assertNull(TrainJourneyParser.verify(invalid, saturday))
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
