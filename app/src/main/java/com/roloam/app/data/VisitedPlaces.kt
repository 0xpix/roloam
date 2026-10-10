package com.roloam.app.data

import android.content.Context
import com.roloam.app.model.Place
import java.util.Locale

/**
 * Persistent "already been here" history, shared between trips.
 * Coordinates distinguish similarly named landmarks in different towns;
 * no user location history or remote account is stored.
 */
object PlaceIdentity {
    fun key(place: Place): String =
        place.name.trim().lowercase(Locale.ROOT) + "|" +
            String.format(Locale.ROOT, "%.4f", place.point.lat) + "," +
            String.format(Locale.ROOT, "%.4f", place.point.lon)
}

class VisitedPlaces(context: Context) {
    private val prefs = context.getSharedPreferences("roloam_visited_places", Context.MODE_PRIVATE)

    fun all(): Set<String> = prefs.getStringSet("known", emptySet()).orEmpty().toSet()

    fun contains(place: Place): Boolean = PlaceIdentity.key(place) in all()

    fun mark(place: Place) {
        val updated = (all() + PlaceIdentity.key(place)).takeLast(500).toSet()
        prefs.edit().putStringSet("known", updated).apply()
    }
}
