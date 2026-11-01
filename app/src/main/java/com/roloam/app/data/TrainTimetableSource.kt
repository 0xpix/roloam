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

/** Minimal subset of the Transitous MOTIS 2 /api/v6/plan response. */
data class TransitousPlanResponse(val itineraries: List<TransitousItinerary> = emptyList())
data class TransitousItinerary(
    val startTime: String? = null,
    val endTime: String? = null,
    val transfers: Int? = null,
    val legs: List<TransitousLeg> = emptyList()
)
data class TransitousLeg(val mode: String? = null, val cancelled: Boolean = false)

/** Accept only itinerary durations backed by a dated timetable with a real rail leg. */
object TrainJourneyParser {
    private val berlin = ZoneId.of("Europe/Berlin")
    private val railModes = setOf(
        "HIGHSPEED_RAIL", "LONG_DISTANCE", "NIGHT_RAIL", "REGIONAL_FAST_RAIL",
        "REGIONAL_RAIL", "SUBURBAN"
    )

    fun verify(itinerary: TransitousItinerary, date: LocalDate): TrainConnection? {
        if (itinerary.legs.isEmpty() || itinerary.legs.any { it.cancelled }) return null
        val railLegs = itinerary.legs.count { it.mode in railModes }
        if (railLegs == 0) return null // Walking, bus or tram alone is not a train trip.

        val departure = parse(itinerary.startTime)?.atZoneSameInstant(berlin) ?: return null
        val arrival = parse(itinerary.endTime)?.atZoneSameInstant(berlin) ?: return null
        if (departure.toLocalDate() != date) return null
        val minutes = Duration.between(departure.toInstant(), arrival.toInstant()).toMinutes()
        if (minutes !in 15L..900L) return null
        return TrainConnection(
            durationMinutes = minutes.toInt(),
            // MOTIS uses UTC timestamps; keep Berlin-local offsets for the app's clock.
            departureIso = departure.toOffsetDateTime().toString(),
            arrivalIso = arrival.toOffsetDateTime().toString(),
            transfers = (itinerary.transfers ?: (railLegs - 1)).coerceAtLeast(0),
            provider = "Transitous timetable"
        )
    }

    private fun parse(raw: String?): OffsetDateTime? =
        raw?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
}

/** Best-effort outbound routing for a specific date, including first/last-mile walking. */
class TrainTimetableSource(private val network: Network) {
    private val adapter = network.moshi.adapter(TransitousPlanResponse::class.java)
    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")

    suspend fun connection(
        origin: GeoPoint,
        destination: Destination,
        departureDate: LocalDate
    ): TrainConnection? {
        if (!TripDataQuality.validPoint(origin) || !TripDataQuality.validPoint(destination.point)) return null
        val departAt = ZonedDateTime.of(
            departureDate, LocalTime.of(8, 0), ZoneId.of("Europe/Berlin")
        ).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        // The old v6.db.transport.rest proxy is unavailable. MOTIS returns real
        // schedules and journey durations for geographic endpoints, not estimates.
        val url = "https://api.transitous.org/api/v6/plan?" +
            "fromPlace=${origin.lat},${origin.lon}" +
            "&toPlace=${destination.point.lat},${destination.point.lon}" +
            "&time=${enc(departAt)}&numItineraries=4&withFares=false"
        val response = adapter.fromJson(network.get(url)) ?: return null
        return response.itineraries.mapNotNull { TrainJourneyParser.verify(it, departureDate) }
            .minByOrNull { it.durationMinutes }
    }
}
