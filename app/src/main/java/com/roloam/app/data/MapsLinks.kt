package com.roloam.app.data

import com.roloam.app.model.Destination
import com.roloam.app.model.GeoPoint
import com.roloam.app.model.TransportMode
import java.net.URLEncoder

/** Key-free Maps URLs open Google Maps on Android or a browser if installed. */
object MapsLinks {
    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
    private fun coordinates(point: GeoPoint): String = point.lat.toString() + "," + point.lon.toString()

    fun directions(origin: GeoPoint, destination: GeoPoint, transport: TransportMode): String {
        val mode = when (transport) {
            TransportMode.CAR -> "driving"
            TransportMode.TRAIN -> "transit"
            TransportMode.BIKE -> "bicycling"
            TransportMode.WALK -> "walking"
        }
        return "https://www.google.com/maps/dir/?api=1&origin=" + encode(coordinates(origin)) +
            "&destination=" + encode(coordinates(destination)) + "&travelmode=" + mode
    }

    fun searchStays(destination: Destination, camping: Boolean): String {
        val kind = if (camping) "camping and campsites" else "hotels hostels and guest houses"
        return search(kind + " near " + destination.name + " " + coordinates(destination.point))
    }

    // Exact mapped coordinates, not a free-text name which Google may geocode
    // to an entirely different hostel or street in another city.
    fun place(name: String, point: GeoPoint): String = search(coordinates(point))

    fun navigateTo(point: GeoPoint, transport: TransportMode): String {
        val mode = when (transport) {
            TransportMode.CAR -> "driving"
            TransportMode.TRAIN -> "transit"
            TransportMode.BIKE -> "bicycling"
            TransportMode.WALK -> "walking"
        }
        return "https://www.google.com/maps/dir/?api=1&destination=" +
            encode(coordinates(point)) + "&travelmode=" + mode
    }

    private fun search(query: String): String =
        "https://www.google.com/maps/search/?api=1&query=" + encode(query)
}
