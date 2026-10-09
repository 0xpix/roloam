package com.roloam.app.data

import android.content.Context
import com.roloam.app.model.TripPlan
import com.roloam.app.model.TripPreferences
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.LocalDate

/** An accepted trip is stored only on the device; no account or cloud sync needed. */
class TripStore(context: Context) {
    private val prefs = context.getSharedPreferences("roloam_saved_trip", Context.MODE_PRIVATE)

    fun save(trip: TripPlan, preferences: TripPreferences) {
        prefs.edit()
            .putString("accepted_trip_json", TripCodec.encode(trip))
            .putString("accepted_preferences_json", TripCodec.encodePreferences(preferences))
            .apply()
    }

    fun load(): TripPlan? = prefs.getString("accepted_trip_json", null)?.let { json ->
        runCatching { TripCodec.decode(json) }.getOrNull()
    }

    fun loadPreferences(): TripPreferences? =
        prefs.getString("accepted_preferences_json", null)?.let { json ->
            runCatching { TripCodec.decodePreferences(json) }.getOrNull()
        }
}

class LocalDateJsonAdapter {
    @ToJson fun toJson(date: LocalDate): String = date.toString()
    @FromJson fun fromJson(text: String): LocalDate = LocalDate.parse(text)
}

/** Pure JVM codec to cover Android persistence with real unit tests. */
object TripCodec {
    private val moshi = Moshi.Builder()
        .add(LocalDateJsonAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(TripPlan::class.java)
    private val preferencesAdapter = moshi.adapter(TripPreferences::class.java)

    fun encodePreferences(value: TripPreferences): String = preferencesAdapter.toJson(value)
    fun decodePreferences(json: String): TripPreferences = preferencesAdapter.fromJson(json)
        ?: throw IllegalArgumentException("Preferences data was empty")

    fun encode(value: TripPlan): String = adapter.toJson(value)
    fun decode(json: String): TripPlan = adapter.fromJson(json)
        ?: throw IllegalArgumentException("Trip data was empty")
}
