package com.roloam.app.data

import com.roloam.app.model.GeoPoint
import com.roloam.app.model.Place
import com.roloam.app.model.Stay
import com.roloam.app.model.TripPlan
import java.util.Locale

/**
 * Pure validation and normalization at the provider boundary.
 * No invented pins, travel metrics or booking information may enter a verified trip.
 */
object TripDataQuality {
    fun validPoint(point: GeoPoint): Boolean =
        point.lat.isFinite() && point.lon.isFinite() &&
            point.lat in -90.0..90.0 && point.lon in -180.0..180.0

    fun validRoute(metric: RouteMetric?, straightLineKm: Double): Boolean =
        metric != null && metric.minutes > 0 && metric.minutes <= 24 * 60 &&
            metric.distanceKm.isFinite() && metric.distanceKm > 0.0 &&
            straightLineKm.isFinite() && straightLineKm >= 0.0 &&
            metric.distanceKm + 0.5 >= straightLineKm * 0.90

    fun website(raw: String?): String? {
        val value = raw?.trim()?.takeIf { it.length in 8..2048 } ?: return null
        return value.takeIf {
            (it.startsWith("https://", true) || it.startsWith("http://", true)) &&
                runCatching { java.net.URI(it).host?.isNotBlank() == true }.getOrDefault(false)
        }
    }

    fun distinctPlaces(input: List<Place>): List<Place> =
        input.filter { validPoint(it.point) && it.name.isNotBlank() }
            .distinctBy { it.sourceRef ?: fallbackKey(it.name, it.point) }

    fun distinctStays(input: List<Stay>): List<Stay> =
        input.filter { validPoint(it.point) && it.name.isNotBlank() }
            .distinctBy { it.sourceRef ?: fallbackKey(it.name, it.point) }

    private fun fallbackKey(name: String, point: GeoPoint) =
        name.trim().lowercase(Locale.ROOT) + "|" +
            String.format(Locale.ROOT, "%.3f,%.3f", point.lat, point.lon)

    /**
     * Only yield a completed trip when its real POIs cover every day.
     * Accommodation is a real mapped object for overnight trips, never a fabricated hotel.
     * Weather is optional, since public forecasts may be unavailable.
     */
    fun problems(plan: TripPlan): List<String> = buildList {
        if (!validPoint(plan.origin) || !validPoint(plan.destination.point)) add("invalid trip coordinates")
        if (plan.days !in 1..3 || plan.itinerary.size != plan.days) add("incomplete day coverage")
        if (plan.days > 1 && (plan.stay == null || !validPoint(plan.stay.point))) add("no verified accommodation")
        if (plan.itinerary.any { it.stops.isEmpty() }) add("a day has no mapped activities")
        if (plan.itinerary.sumOf { it.stops.size } < plan.days * 2) add("too few verified activities")
        val stops = plan.itinerary.flatMap { it.stops }
        if (stops.any { !validPoint(it.place.point) || it.place.id < 0L || it.place.name.isBlank() })
            add("invalid or placeholder stop")
        if (stops.map { it.place.sourceRef ?: fallbackKey(it.place.name, it.place.point) }.distinct().size != stops.size)
            add("duplicate stops")
    }
}
