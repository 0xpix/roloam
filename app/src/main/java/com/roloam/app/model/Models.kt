package com.roloam.app.model

import java.time.LocalDate

data class GeoPoint(val lat: Double, val lon: Double)

enum class TransportMode { CAR, TRAIN, BIKE, WALK }
enum class DurationChoice { ONE, TWO, THREE, AUTO }
enum class Budget { CHEAP, NORMAL }
enum class StayPreference { CAMPING, ANY }
enum class TripStyle { NATURE, CITY, BOTH }

data class TripPreferences(
    val duration: DurationChoice = DurationChoice.TWO,
    val transport: TransportMode = TransportMode.CAR,
    val budget: Budget = Budget.CHEAP,
    val stay: StayPreference = StayPreference.CAMPING,
    val style: TripStyle = TripStyle.BOTH
)

data class Destination(
    val name: String,
    val country: String? = null,
    val point: GeoPoint,
    val population: Long? = null,
    val travelMinutes: Int = 0,
    val distanceKm: Double = 0.0
)

data class Place(
    val id: Long,
    val name: String,
    val point: GeoPoint,
    val category: String,
    val openingHours: String? = null,
    val website: String? = null,
    val description: String? = null
)

data class Stay(
    val id: Long,
    val name: String,
    val point: GeoPoint,
    val category: String,
    val website: String? = null,
    val phone: String? = null,
    val openingHours: String? = null,
    val distanceFromCenterKm: Double = 0.0
)

data class WeatherDay(
    val date: LocalDate,
    val code: Int,
    val maxC: Double?,
    val minC: Double?,
    val precipitationProbability: Int?,
    val sunrise: String?,
    val sunset: String?
)

data class TripStop(
    val place: Place,
    val time: String,
    val durationMinutes: Int,
    val whyNow: String
)

data class TripDay(
    val date: LocalDate,
    val stops: List<TripStop>,
    val weather: WeatherDay?
)

data class TripPlan(
    val originLabel: String,
    val origin: GeoPoint,
    val destination: Destination,
    val days: Int,
    val startDate: LocalDate,
    val stay: Stay?,
    val alternativeStays: List<Stay>,
    val itinerary: List<TripDay>,
    val generatedAtEpochMs: Long = System.currentTimeMillis(),
    val usedLiveData: Boolean = true
)

fun TripPlan.allStops(): List<TripStop> = itinerary.flatMap { it.stops }
