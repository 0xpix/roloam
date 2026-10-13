package com.roloam.app.data

import com.roloam.app.model.Destination
import com.roloam.app.model.GeoPoint
import com.roloam.app.model.TrainConnection
import java.net.URLEncoder
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Lightweight subset of Germany's public timetable API (v6.db.transport.rest). */
data class DbJourneyResponse(val journeys: List<DbJourney> = emptyList())
data class DbJourney(val legs: List<DbJourneyLeg> = emptyList())
data class DbJourneyLeg(
    val departure: String? = null,
    val plannedDeparture: String? = null,
    val arrival: String? = null,
    val plannedArrival: String? = null,
    val line: DbJourneyLine? = null
)
data class DbJourneyLine(val mode: String? = null, val product: String? = null)

/** Only accept schedule-backed durations. Never infer a rail duration from straight-line distance. */
object TrainJourneyParser {
    private val trainProducts = setOf(
        "nationalExpress", "national", "regionalExpress", "regionalExp",
        "regional", "suburban"
    )

    fun verify(journey: DbJourney, date: LocalDate): TrainConnection? {
        val legs = journey.legs
        if (legs.isEmpty()) return null
        val hasTrain = legs.any { it.line?.mode == "train" || it.line?.product in trainProducts }
        if (!hasTrain) return null

        val departure = parse(legs.first().departure ?: legs.first().plannedDeparture) ?: return null
        val arrival = parse(legs.last().arrival ?: legs.last().plannedArrival) ?: return null
        if (departure.toLocalDate() != date) return null
        val duration = Duration.between(departure.toInstant(), arrival.toInstant()).toMinutes()
        if (duration !in 15L..900L) return null
        val trainLegs = legs.count { it.line?.mode == "train" || it.line?.product in trainProducts }
        return TrainConnection(
            durationMinutes = duration.toInt(),
            departureIso = departure.toString(),
            arrivalIso = arrival.toString(),
            transfers = (trainLegs - 1).coerceAtLeast(0),
            provider = "DB timetable"
        )
    }

    private fun parse(raw: String?): OffsetDateTime? =
        raw?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
}

/** Best-effort live DB journey lookup for a specific Saturday and city-centre endpoints. */
class TrainTimetableSource(private val network: Network) {
    private val adapter = network.moshi.adapter(DbJourneyResponse::class.java)
    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")

    suspend fun connection(
        origin: GeoPoint,
        originName: String,
        destination: Destination,
        departureDate: LocalDate
    ): TrainConnection? {
        if (!TripDataQuality.validPoint(origin) || !TripDataQuality.validPoint(destination.point)) return null
        val departAt = ZonedDateTime.of(
            departureDate, LocalTime.of(8, 0), ZoneId.of("Europe/Berlin")
        ).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val to = destination.point
        // Officially documented address/coordinate request form, avoiding
        // potentially ambiguous matches from text-only station searches.
        val url = "https://v6.db.transport.rest/journeys?" +
            "from.latitude=${origin.lat}&from.longitude=${origin.lon}&from.address=${enc(originName)}" +
            "&to.latitude=${to.lat}&to.longitude=${to.lon}&to.address=${enc(destination.name)}" +
            "&departure=${enc(departAt)}&results=4&polylines=false&stopovers=false&remarks=false"
        val response = adapter.fromJson(network.get(url)) ?: return null
        return response.journeys.mapNotNull { TrainJourneyParser.verify(it, departureDate) }
            .minByOrNull { it.durationMinutes }
    }
}
