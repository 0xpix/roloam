package com.roloam.app.data

import android.content.Context
import com.roloam.app.model.TripPlan
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.LocalDate

/** An accepted trip is stored only on the device; no account or cloud sync needed. */
class TripStore(context: Context) {
    private val prefs = context.getSharedPreferences("roloam_saved_trip", Context.MODE_PRIVATE)

    fun save(trip: TripPlan) {
        prefs.edit().putString("accepted_trip_json", TripCodec.encode(trip)).apply()
    }

    fun load(): TripPlan? = prefs.getString("accepted_trip_json", null)?.let { json ->
        runCatching { TripCodec.decode(json) }.getOrNull()
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

    fun encode(value: TripPlan): String = adapter.toJson(value)
    fun decode(json: String): TripPlan = adapter.fromJson(json)
        ?: throw IllegalArgumentException("Trip data was empty")
}
