package com.roloam.app.data

import android.content.Context
import com.roloam.app.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.random.Random

class TripRepository(context: Context) {
    private val source = LiveDataSource(Network(context))
    private val random = Random.Default
    private val recentDestinations = ArrayDeque<String>()
    private var cityCache: CachedCities? = null

    suspend fun roll(origin: Origin, prefs: TripPreferences): TripPlan = coroutineScope {
        val days = when (prefs.duration) {
            DurationChoice.ONE -> 1
            DurationChoice.TWO -> 2
            DurationChoice.THREE -> 3
            DurationChoice.AUTO -> 2
        }
        val radius = radiusKm(days, prefs.transport)
        val cached = cityCache?.takeIf { it.radiusKm == radius && haversine(it.origin, origin.point) < 5.0 }?.cities
        val liveCities = cached ?: runCatching { source.cities(origin.point, radius) }.getOrDefault(emptyList()).also {
            if (it.isNotEmpty()) cityCache = CachedCities(origin.point, radius, it)
        }
        val rawCities = if (liveCities.size >= 4) liveCities else fallbackCities()

        val prelim = rawCities
            .filter { haversine(origin.point, it.point) > minimumDistance(prefs.transport) }
            .sortedBy { abs(haversine(origin.point, it.point) - targetDistance(days, prefs.transport)) }
            .take(22)

        if (prelim.isEmpty()) error("No destinations found nearby. Try a longer trip.")

        val routeMetrics = if (prefs.transport == TransportMode.TRAIN) {
            prelim.map {
                val km = haversine(origin.point, it.point)
                RouteMetric(((km / 75.0) * 60.0 + 25).roundToInt(), km)
            }
        } else {
            val mode = when (prefs.transport) {
                TransportMode.BIKE -> "bike"
                TransportMode.WALK -> "walk"
                else -> "car"
            }
            runCatching { source.routeTable(origin.point, prelim.map { it.point }, mode) }
                .getOrDefault(List(prelim.size) { null })
        }

        val candidates = prelim.mapIndexed { i, city ->
            val metric = routeMetrics.getOrNull(i)
            val fallbackKm = haversine(origin.point, city.point)
            val speed = when (prefs.transport) {
                TransportMode.CAR -> 78.0
                TransportMode.TRAIN -> 75.0
                TransportMode.BIKE -> 18.0
                TransportMode.WALK -> 4.7
            }
            Destination(
                city.name,
                city.country,
                city.point,
                city.population,
                metric?.minutes ?: ((fallbackKm / speed) * 60).roundToInt(),
                if ((metric?.distanceKm ?: 0.0) > 0) metric!!.distanceKm else fallbackKm
            )
        }.filter { validTravel(it.travelMinutes, days, prefs.transport) }

        val pool = (if (candidates.isNotEmpty()) candidates else prelim.map {
            val km = haversine(origin.point, it.point)
            Destination(it.name, it.country, it.point, it.population, ((km / 75) * 60).roundToInt(), km)
        }).sortedByDescending { score(it, days, prefs) }.take(7)

        val freshPool = pool.filterNot { recentDestinations.contains(it.name.lowercase()) }
        val chosen = weightedPick(if (freshPool.size >= 2) freshPool else pool)
        recentDestinations.addFirst(chosen.name.lowercase())
        while (recentDestinations.size > 4) recentDestinations.removeLast()

        val startDate = nextSaturday(LocalDate.now())

        val placesDeferred = async { runCatching { source.attractions(chosen.point, prefs.style.name) }.getOrDefault(emptyList()) }
        val staysDeferred = async {
            if (days == 1) emptyList()
            else runCatching { source.stays(chosen.point) }.getOrDefault(emptyList())
        }
        val weatherDeferred = async { runCatching { source.weather(chosen.point) }.getOrDefault(emptyList()) }

        val places = placesDeferred.await()
        val rawStays = staysDeferred.await()
        val weather = weatherDeferred.await()

        val rankedPlaces = rankPlaces(places, chosen.point, prefs).ifEmpty {
            listOf(Place(-1, chosen.name + " city centre", chosen.point, "city"))
        }

        val itinerary = buildItinerary(rankedPlaces, chosen, days, startDate, weather)
        val stays = rankStays(rawStays, prefs, itinerary, chosen.point)
        TripPlan(
            originLabel = origin.label,
            origin = origin.point,
            destination = chosen,
            days = days,
            startDate = startDate,
            stay = stays.firstOrNull(),
            alternativeStays = stays.drop(1).take(4),
            itinerary = itinerary,
            usedLiveData = liveCities.isNotEmpty() && places.isNotEmpty()
        )
    }

    private fun score(d: Destination, days: Int, prefs: TripPreferences): Double {
        val targetMinutes = targetTravelMinutes(days, prefs.transport)
        val targetKm = targetDistance(days, prefs.transport)

        // A good random trip should feel worth the travel without consuming the whole day.
        val travelFit = 1.0 -
            (abs(d.travelMinutes - targetMinutes).toDouble() / targetMinutes)
                .coerceIn(0.0, 1.0)
        val distanceFit = 1.0 -
            (abs(d.distanceKm - targetKm) / targetKm.coerceAtLeast(1.0))
                .coerceIn(0.0, 1.0)

        // Population is useful as a rough proxy for how dense the choice of things to do is,
        // but we deliberately do not make "largest city wins" the default.
        val cityScale = d.population
            ?.let { (ln(it.toDouble().coerceAtLeast(1_000.0)) - 6.9) / 7.0 }
            ?.coerceIn(0.0, 1.0)
            ?: 0.32

        val styleFit = when (prefs.style) {
            TripStyle.CITY -> cityScale
            TripStyle.NATURE -> 1.0 - cityScale
            TripStyle.BOTH -> 1.0 - abs(cityScale - 0.55)
        }.coerceIn(0.0, 1.0)

        val budgetFit = when (prefs.budget) {
            Budget.CHEAP -> (1.0 - cityScale * 0.72).coerceIn(0.0, 1.0)
            Budget.NORMAL -> 0.72
        }

        // Randomness stays meaningful, but it can no longer rescue a poor travel-time match.
        val discoveryJitter = random.nextDouble(0.0, 0.14)

        return travelFit * 0.46 +
            distanceFit * 0.18 +
            styleFit * 0.20 +
            budgetFit * 0.10 +
            discoveryJitter
    }

    private fun weightedPick(pool: List<Destination>): Destination {
        if (pool.size == 1) return pool.first()
        val weights = pool.indices.map { 1.0 / (it + 1.0) }
        var needle = random.nextDouble() * weights.sum()
        for (i in pool.indices) {
            needle -= weights[i]
            if (needle <= 0) return pool[i]
        }
        return pool.last()
    }

    private fun rankPlaces(input: List<Place>, center: GeoPoint, prefs: TripPreferences): List<Place> {
        fun quality(p: Place): Double {
            val cat = p.category.lowercase()
            var q = 0.0
            if (p.website != null) q += 0.8
            if (p.openingHours != null) q += 0.5
            if (cat in listOf("museum", "gallery", "attraction", "viewpoint", "historic")) q += 1.2
            if (prefs.style == TripStyle.NATURE && cat in listOf("viewpoint", "park", "garden")) q += 1.2
            if (prefs.style == TripStyle.CITY && cat in listOf("museum", "gallery", "historic", "attraction")) q += 1.0
            q -= haversine(center, p.point) / 18.0
            return q
        }
        return input.filter { haversine(center, it.point) < 16.0 }
            .sortedByDescending(::quality)
            .distinctBy { it.name.lowercase() }
            .take(16)
    }

    private fun rankStays(
        input: List<Stay>,
        prefs: TripPreferences,
        itinerary: List<TripDay>,
        destinationCenter: GeoPoint
    ): List<Stay> {
        val overnightTransitions = itinerary.zipWithNext().mapNotNull { (today, tomorrow) ->
            val last = today.stops.lastOrNull()?.place?.point ?: return@mapNotNull null
            val next = tomorrow.stops.firstOrNull()?.place?.point ?: return@mapNotNull null
            last to next
        }

        return input.sortedByDescending { stay ->
            val camp = stay.category == "camp_site" || stay.category == "caravan_site"

            val preference = when {
                prefs.stay == StayPreference.CAMPING && camp -> 6.0
                prefs.stay == StayPreference.CAMPING -> 0.4
                camp -> 1.8
                else -> 1.2
            }

            val routeDetourKm = if (overnightTransitions.isEmpty()) {
                haversine(destinationCenter, stay.point)
            } else {
                overnightTransitions.map { (last, next) ->
                    val viaStay = haversine(last, stay.point) + haversine(stay.point, next)
                    val direct = haversine(last, next)
                    (viaStay - direct).coerceAtLeast(0.0)
                }.average()
            }

            val usefulData = when {
                stay.website != null && stay.openingHours != null -> 0.8
                stay.website != null -> 0.5
                else -> 0.0
            }

            preference +
                usefulData -
                routeDetourKm / 5.0 -
                stay.distanceFromCenterKm / 35.0
        }
    }

    private fun buildItinerary(
        places: List<Place>,
        destination: Destination,
        days: Int,
        startDate: LocalDate,
        weather: List<WeatherDay>
    ): List<TripDay> {
        val chosen = places.take((days * 4).coerceAtMost(12)).toMutableList()
        var previous = destination.point
        return (0 until days).map { day ->
            val date = startDate.plusDays(day.toLong())
            val dayWeather = weather.firstOrNull { it.date == date }
            var clock = if (day == 0) {
                LocalTime.of(8, 0).plusMinutes(destination.travelMinutes.toLong())
            } else LocalTime.of(9, 0)
            val count = if (days == 1) 5 else 4
            val stops = mutableListOf<TripStop>()
            repeat(count) {
                if (chosen.isEmpty()) return@repeat

                var eligible = chosen.filter {
                    openingState(it.openingHours, date, clock) != OpeningState.CLOSED
                }

                // If every known place is still closed, wait a little rather than schedule a
                // museum or attraction before its listed opening time.
                var waits = 0
                while (eligible.isEmpty() && chosen.isNotEmpty() && waits < 6) {
                    clock = clock.plusMinutes(30)
                    waits += 1
                    eligible = chosen.filter {
                        openingState(it.openingHours, date, clock) != OpeningState.CLOSED
                    }
                }

                val next = eligible.minByOrNull { p ->
                    val dist = haversine(previous, p.point)
                    val preferred = preferredHour(p.category, dayWeather)
                    dist * 2.2 + abs(clock.hour + clock.minute / 60.0 - preferred) * 0.8
                } ?: return@repeat

                chosen.remove(next)
                val transitMin = ((haversine(previous, next.point) / 4.5) * 60).roundToInt().coerceIn(5, 45)
                if (stops.isNotEmpty()) clock = clock.plusMinutes(transitMin.toLong())

                // Re-check after transit. If the POI would be closed on arrival, skip it rather
                // than presenting a plan that cannot actually be followed.
                if (openingState(next.openingHours, date, clock) == OpeningState.CLOSED) {
                    return@repeat
                }

                val visit = visitMinutes(next.category)
                stops += TripStop(next, clock.format(DateTimeFormatter.ofPattern("HH:mm")), visit, whyNow(next.category, clock, dayWeather))
                clock = clock.plusMinutes(visit.toLong())
                previous = next.point
            }
            TripDay(date, stops, dayWeather)
        }
    }

    private fun preferredHour(category: String, weather: WeatherDay?): Double {
        val c = category.lowercase()
        val rainy = (weather?.precipitationProbability ?: 0) >= 50
        return when {
            c in listOf("viewpoint", "park", "garden") && !rainy -> 17.0
            c in listOf("museum", "gallery") -> if (rainy) 11.0 else 13.0
            c == "historic" || c == "attraction" -> 10.0
            else -> 14.0
        }
    }

    private fun whyNow(category: String, time: LocalTime, weather: WeatherDay?): String {
        val c = category.lowercase()
        val rainy = (weather?.precipitationProbability ?: 0) >= 50
        return when {
            c in listOf("museum", "gallery") && rainy -> "Indoor stop while rain is more likely."
            c in listOf("viewpoint", "park", "garden") && time.hour >= 16 -> "Placed late for softer light and a relaxed finish."
            (c == "historic" || c == "attraction") && time.hour <= 11 -> "Earlier visit to avoid the busiest part of the day."
            else -> "Placed here to keep the route compact and reduce backtracking."
        }
    }

    private fun visitMinutes(category: String) = when (category.lowercase()) {
        "museum", "gallery" -> 90
        "park", "garden" -> 75
        else -> 60
    }

    private fun radiusKm(days: Int, mode: TransportMode) = when (mode) {
        TransportMode.CAR -> listOf(0, 220, 450, 650)[days]
        TransportMode.TRAIN -> listOf(0, 280, 520, 650)[days]
        TransportMode.BIKE -> listOf(0, 70, 140, 220)[days]
        TransportMode.WALK -> listOf(0, 25, 45, 70)[days]
    }

    private fun targetDistance(days: Int, mode: TransportMode) = when (mode) {
        TransportMode.CAR -> listOf(0.0, 110.0, 250.0, 420.0)[days]
        TransportMode.TRAIN -> listOf(0.0, 140.0, 300.0, 480.0)[days]
        TransportMode.BIKE -> listOf(0.0, 40.0, 90.0, 150.0)[days]
        TransportMode.WALK -> listOf(0.0, 12.0, 25.0, 40.0)[days]
    }

    private fun targetTravelMinutes(days: Int, mode: TransportMode) = when (mode) {
        TransportMode.WALK -> listOf(0, 120, 240, 330)[days]
        TransportMode.BIKE -> listOf(0, 90, 210, 300)[days]
        else -> listOf(0, 80, 165, 260)[days]
    }

    private fun validTravel(minutes: Int, days: Int, mode: TransportMode): Boolean {
        val max = when (mode) {
            TransportMode.CAR, TransportMode.TRAIN -> listOf(0, 150, 300, 420)[days]
            TransportMode.BIKE -> listOf(0, 180, 330, 450)[days]
            TransportMode.WALK -> listOf(0, 210, 360, 480)[days]
        }
        return minutes in 25..max
    }

    private fun minimumDistance(mode: TransportMode) = when (mode) {
        TransportMode.WALK -> 3.0
        TransportMode.BIKE -> 8.0
        else -> 25.0
    }

    private fun nextSaturday(today: LocalDate): LocalDate {
        var d = today
        while (d.dayOfWeek != DayOfWeek.SATURDAY) d = d.plusDays(1)
        return d
    }

    private data class CachedCities(val origin: GeoPoint, val radiusKm: Int, val cities: List<CityRaw>)

    private fun fallbackCities() = listOf(
        CityRaw("Cologne", "Germany", GeoPoint(50.9375, 6.9603), 1_087_000),
        CityRaw("Strasbourg", "France", GeoPoint(48.5734, 7.7521), 291_000),
        CityRaw("Freiburg", "Germany", GeoPoint(47.9990, 7.8421), 236_000),
        CityRaw("Stuttgart", "Germany", GeoPoint(48.7758, 9.1829), 635_000),
        CityRaw("Nuremberg", "Germany", GeoPoint(49.4521, 11.0767), 526_000),
        CityRaw("Würzburg", "Germany", GeoPoint(49.7913, 9.9534), 130_000),
        CityRaw("Trier", "Germany", GeoPoint(49.7499, 6.6371), 112_000),
        CityRaw("Koblenz", "Germany", GeoPoint(50.3569, 7.5889), 115_000),
        CityRaw("Frankfurt", "Germany", GeoPoint(50.1109, 8.6821), 775_000),
        CityRaw("Luxembourg", "Luxembourg", GeoPoint(49.6116, 6.1319), 136_000),
        CityRaw("Basel", "Switzerland", GeoPoint(47.5596, 7.5886), 177_000),
        CityRaw("Baden-Baden", "Germany", GeoPoint(48.7606, 8.2398), 56_000),
        CityRaw("Mainz", "Germany", GeoPoint(49.9929, 8.2473), 220_000),
        CityRaw("Rothenburg ob der Tauber", "Germany", GeoPoint(49.3789, 10.1870), 11_000),
        CityRaw("Speyer", "Germany", GeoPoint(49.3173, 8.4412), 51_000)
    )
}
