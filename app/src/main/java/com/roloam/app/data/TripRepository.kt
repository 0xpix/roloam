package com.roloam.app.data

import android.content.Context
import com.roloam.app.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.random.Random

class TripRepository(context: Context) {
    private val network = Network(context)
    private val source = LiveDataSource(network)
    private val trainSource = TrainTimetableSource(network)
    private val overviewSource = DestinationOverviewSource(network)

    suspend fun overview(destination: Destination): DestinationOverview? = overviewSource.fetch(destination)
    private val history = VisitedPlaces(context)
    private val random = Random.Default
    private val discoveryPrefs = context.getSharedPreferences("roloam_discovery", Context.MODE_PRIVATE)
    private val recentDestinations = ArrayDeque(
        discoveryPrefs.getString("recent_destinations", "").orEmpty()
            .split("|").filter { it.isNotBlank() }
    )
    private var cityCache: CachedCities? = null

    /**
     * The roll button does no networking, routing, POI lookup or verification.
     * It only chooses a real, geolocated name from the offline index.
     */
    fun pickDestination(origin: Origin, prefs: TripPreferences): Destination {
        val previousLat = discoveryPrefs.getString("origin_lat", null)?.toDoubleOrNull()
        val previousLon = discoveryPrefs.getString("origin_lon", null)?.toDoubleOrNull()
        if (previousLat == null || previousLon == null ||
            haversine(GeoPoint(previousLat, previousLon), origin.point) > 50.0) {
            recentDestinations.clear()
            discoveryPrefs.edit()
                .putString("origin_lat", origin.point.lat.toString())
                .putString("origin_lon", origin.point.lon.toString())
                .remove("recent_destinations").apply()
        }
        val days = when (prefs.duration) {
            DurationChoice.ONE -> 1
            DurationChoice.TWO, DurationChoice.AUTO -> 2
            DurationChoice.THREE -> 3
        }
        val options = DestinationSeeds.nearby(origin.point, radiusKm(days, prefs.transport))
            .filter { haversine(origin.point, it.point) > minimumDistance(prefs.transport) }
        if (options.isEmpty()) {
            error("No offline destinations in this range yet. Try a longer trip or another transport mode.")
        }
        val pick = DestinationSeeds.pick(options, recentDestinations, random)
            ?: error("No destinations available in the selected range")
        recentDestinations.remove(pick.name.lowercase())
        recentDestinations.addFirst(pick.name.lowercase())
        while (recentDestinations.size > 10) recentDestinations.removeLast()
        discoveryPrefs.edit().putString("recent_destinations", recentDestinations.joinToString("|")).apply()
        return Destination(
            name = pick.name, country = pick.country, point = pick.point,
            population = pick.population, distanceKm = haversine(origin.point, pick.point),
            routeEstimated = true, wikipediaTitle = pick.wikipediaTitle
        )
    }

    /**
     * Called ONLY when the user approves the already selected destination.
     * Missing data results in incomplete, clearly labeled details; it never
     * silently selects a different city or erases their choice.
     */
    suspend fun loadDetails(origin: Origin, prefs: TripPreferences, picked: Destination): TripPlan = coroutineScope {
        val days = when (prefs.duration) {
            DurationChoice.ONE -> 1
            DurationChoice.TWO, DurationChoice.AUTO -> 2
            DurationChoice.THREE -> 3
        }
        val startDate = nextSaturday(LocalDate.now())
        val distanceKm = haversine(origin.point, picked.point)
        val destination = if (prefs.transport == TransportMode.TRAIN) {
            val verified = bestEffort<com.roloam.app.model.TrainConnection?>(null) {
                withTimeout(10_000L) { trainSource.connection(origin.point, picked, startDate) }
            }
            picked.copy(
                travelMinutes = verified?.durationMinutes ?: 0,
                distanceKm = distanceKm, routeEstimated = verified == null,
                trainConnection = verified
            )
        } else {
            val mode = when (prefs.transport) {
                TransportMode.BIKE -> "bike"
                TransportMode.WALK -> "walk"
                else -> "car"
            }
            val route = bestEffort<List<RouteMetric?>>(emptyList()) {
                withTimeout(6_000L) { source.routeTable(origin.point, listOf(picked.point), mode) }
            }.firstOrNull()
            val speed = when (prefs.transport) {
                TransportMode.BIKE -> 18.0
                TransportMode.WALK -> 4.7
                else -> 78.0
            }
            picked.copy(
                travelMinutes = route?.minutes ?: ((distanceKm * 1.25 / speed) * 60).roundToInt(),
                distanceKm = route?.distanceKm ?: distanceKm * 1.25,
                routeEstimated = route == null
            )
        }

        val places = async {
            bestEffort(emptyList<Place>()) {
                withTimeout(12_000L) {
                    val nearby = source.attractions(picked.point, prefs.style.name)
                    TripDataQuality.distinctPlaces(if (nearby.size >= days * 2) nearby else
                        nearby + bestEffort(emptyList()) { source.attractions(picked.point, prefs.style.name, 40) })
                }
            }
        }
        val stays = async {
            if (days == 1) emptyList() else bestEffort(emptyList<Stay>()) {
                withTimeout(12_000L) {
                    val nearby = source.stays(picked.point)
                    TripDataQuality.distinctStays(if (nearby.isNotEmpty()) nearby else
                        nearby + bestEffort(emptyList()) { source.stays(picked.point, 70) })
                }
            }
        }
        val weather = async {
            bestEffort(emptyList<WeatherDay>()) { withTimeout(4_000L) { source.weather(picked.point) } }
        }
        val rankedPlaces = rankPlaces(places.await().filterNot { history.contains(it) }, picked.point, prefs)
        val rankedStays = stays.await()
        val provisionalItinerary = buildItinerary(rankedPlaces, destination, days, startDate, weather.await())
        // We cannot put clock times on a train day with no verified arrival.
        val itinerary = if (prefs.transport == TransportMode.TRAIN && destination.trainConnection == null) {
            provisionalItinerary.map { day -> day.copy(stops = day.stops.map { stop ->
                stop.copy(time = "—", whyNow = "Suggested stop; verify the train journey before deciding when to visit.")
            }) }
        } else provisionalItinerary
        val stayOptions = rankStays(rankedStays, prefs, itinerary, picked.point)
        TripPlan(origin.label, origin.point, destination, days, startDate,
            stayOptions.firstOrNull(), stayOptions.drop(1).take(4), itinerary,
            usedLiveData = rankedPlaces.isNotEmpty() || rankedStays.isNotEmpty())
    }

    suspend fun roll(
        origin: Origin,
        prefs: TripPreferences,
        onPreview: (Destination) -> Unit = {}
    ): TripPlan = coroutineScope {
        val lastLat = discoveryPrefs.getString("origin_lat", null)?.toDoubleOrNull()
        val lastLon = discoveryPrefs.getString("origin_lon", null)?.toDoubleOrNull()
        if (lastLat == null || lastLon == null ||
            haversine(GeoPoint(lastLat, lastLon), origin.point) > 50.0) {
            recentDestinations.clear()
            discoveryPrefs.edit()
                .putString("origin_lat", origin.point.lat.toString())
                .putString("origin_lon", origin.point.lon.toString())
                .remove("recent_destinations")
                .apply()
        }
        val days = when (prefs.duration) {
            DurationChoice.ONE -> 1
            DurationChoice.TWO -> 2
            DurationChoice.THREE -> 3
            DurationChoice.AUTO -> 2
        }
        val radius = radiusKm(days, prefs.transport)
        // Always start with the tiny offline city index. Overpass is only needed
        // where that index lacks nearby choices (e.g. a walk from a rural town).
        val seeded = DestinationSeeds.nearby(origin.point, radius)
        val firstSeed = seeded
            .filter { haversine(origin.point, it.point) > minimumDistance(prefs.transport) }
            .filterNot { recentDestinations.contains(it.name.lowercase()) }
            .sortedBy { abs(haversine(origin.point, it.point) - targetDistance(days, prefs.transport)) }
            .take(8).randomOrNull(random)
        firstSeed?.let { onPreview(Destination(it.name, it.country, it.point, it.population, routeEstimated = true)) }

        val cached = cityCache?.takeIf { it.radiusKm == radius && haversine(it.origin, origin.point) < 5.0 }?.cities
        val liveCities = cached ?: if (seeded.size >= 4) emptyList() else {
            bestEffort(emptyList()) {
                withTimeout(6_000L) { source.cities(origin.point, radius) }
            }.also { if (it.isNotEmpty()) cityCache = CachedCities(origin.point, radius, it) }
        }
        val rawCities = (seeded + liveCities).distinctBy { it.name.lowercase() }

        val prelim = rawCities
            .filter { haversine(origin.point, it.point) > minimumDistance(prefs.transport) }
            .sortedBy { abs(haversine(origin.point, it.point) - targetDistance(days, prefs.transport)) }
            .take(20)

        if (prelim.isEmpty()) error("No nearby destinations found. Check your connection or increase trip duration.")

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
            // Two independent requests at most, with a hard deadline. A failed
            // router yields *labeled estimates*, never a stalled discovery UI.
            prelim.chunked(10).map { batch ->
                async {
                    bestEffort(List<RouteMetric?>(batch.size) { null }) {
                        withTimeout(6_000L) { source.routeTable(origin.point, batch.map { it.point }, mode) }
                    }
                }
            }.awaitAll().flatten()
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
                metric?.minutes ?: ((fallbackKm * 1.25 / speed) * 60).roundToInt(),
                if ((metric?.distanceKm ?: 0.0) > 0) metric!!.distanceKm else fallbackKm * 1.25,
                routeEstimated = metric == null || prefs.transport == TransportMode.TRAIN,
                wikipediaTitle = city.wikipediaTitle
            )
        }.filter { validTravel(it.travelMinutes, days, prefs.transport) }

        val pool = candidates.sortedByDescending { score(it, days, prefs) }.take(20)
        if (pool.isEmpty()) {
            error("No destinations match the travel-time range. Try another duration or transport mode.")
        }

        // Prefer fresh trips, but verify real attractions AND an overnight stay before
        // promising a destination to the user. No placeholder city-centre itineraries.
        val freshPool = pool.filterNot { recentDestinations.contains(it.name.lowercase()) }
        val allowed = freshPool.ifEmpty {
            pool.filterNot { it.name.lowercase() == recentDestinations.firstOrNull() }
                .ifEmpty { pool }
        }
        val remaining = allowed.toMutableList()
        val trials = buildList {
            // Try the destination already displayed first, if it passed the travel filter.
            remaining.firstOrNull { it.name == firstSeed?.name }?.let {
                add(it)
                remaining.remove(it)
            }
            repeat(minOf(4 - size, remaining.size)) {
                val chosen = weightedPick(remaining)
                add(chosen)
                remaining.remove(chosen)
            }
        }
        trials.firstOrNull()?.let(onPreview)
        val startDate = nextSaturday(LocalDate.now())
        suspend fun evaluate(candidate: Destination): TrialResult = coroutineScope {
            // Never show an estimated train duration as a real connection.
            val chosen = if (prefs.transport == TransportMode.TRAIN) {
                val scheduled = try {
                    withTimeout(10_000L) { trainSource.connection(origin.point, candidate, startDate) }
                } catch (error: TimeoutCancellationException) {
                    return@coroutineScope TrialResult(timetableError = true)
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    return@coroutineScope TrialResult(timetableError = true)
                } ?: return@coroutineScope TrialResult()
                if (!validTravel(scheduled.durationMinutes, days, prefs.transport)) return@coroutineScope TrialResult()
                candidate.copy(
                    travelMinutes = scheduled.durationMinutes, routeEstimated = false,
                    trainConnection = scheduled, distanceKm = haversine(origin.point, candidate.point)
                )
            } else candidate

            val placesDeferred = async {
                bestEffort(emptyList()) {
                    withTimeout(12_000L) {
                        val nearby = source.attractions(chosen.point, prefs.style.name)
                        val expanded = if (nearby.size >= days * 2) nearby else
                            nearby + bestEffort(emptyList()) { source.attractions(chosen.point, prefs.style.name, 40) }
                        TripDataQuality.distinctPlaces(expanded)
                    }
                }
            }
            val staysDeferred = async {
                if (days == 1) emptyList() else bestEffort(emptyList()) {
                    withTimeout(12_000L) {
                        val nearby = source.stays(chosen.point)
                        TripDataQuality.distinctStays(if (nearby.isNotEmpty()) nearby else
                            nearby + bestEffort(emptyList()) { source.stays(chosen.point, 70) })
                    }
                }
            }
            val weatherDeferred = async {
                bestEffort(emptyList()) { withTimeout(4_000L) { source.weather(chosen.point) } }
            }
            val places = placesDeferred.await()
            val rawStays = staysDeferred.await()
            val ranked = rankPlaces(places.filterNot { history.contains(it) }, chosen.point, prefs)
            if (ranked.size < days * 2 || (days > 1 && rawStays.isEmpty())) {
                weatherDeferred.cancel()
                return@coroutineScope TrialResult(trainFound = chosen.trainConnection != null)
            }
            val itinerary = buildItinerary(ranked, chosen, days, startDate, weatherDeferred.await())
            val stays = rankStays(rawStays, prefs, itinerary, chosen.point)
            val plan = TripPlan(origin.label, origin.point, chosen, days, startDate,
                stays.firstOrNull(), stays.drop(1).take(4), itinerary)
            TrialResult(plan.takeIf { TripDataQuality.problems(it).isEmpty() },
                trainFound = chosen.trainConnection != null)
        }

        var timetableErrors = 0
        var feasibleTrainConnections = 0
        // Two trials at a time so an unresponsive provider cannot hold every
        // other destination hostage. A single roll is limited by the ViewModel.
        for (batch in trials.chunked(2)) {
            val results = batch.map { candidate ->
                async { evaluate(candidate) }
            }.awaitAll()
            timetableErrors += results.count { it.timetableError }
            feasibleTrainConnections += results.count { it.trainFound }
            val plan = results.firstNotNullOfOrNull { it.plan }
            if (plan != null) {
                recentDestinations.remove(plan.destination.name.lowercase())
                recentDestinations.addFirst(plan.destination.name.lowercase())
                while (recentDestinations.size > 10) recentDestinations.removeLast()
                discoveryPrefs.edit().putString("recent_destinations", recentDestinations.joinToString("|")).apply()
                return@coroutineScope plan
            }
            // Indicate that the first preview did not pass verification.
            trials.getOrNull(trials.indexOf(batch.last()) + 1)?.let(onPreview)
        }

        if (prefs.transport == TransportMode.TRAIN) {
            when {
                feasibleTrainConnections > 0 -> error(
                    "Train connections were found, but the destinations lacked enough mapped places or stays. " +
                        "Try another duration or style."
                )
                timetableErrors > 0 -> error(
                    "The train timetable service did not respond for some destinations. " +
                        "Please try again shortly, or check DB Navigator."
                )
                else -> error(
                    "No train journey within your travel-time limit was found for this date. " +
                        "Try a longer duration or another transport mode."
                )
            }
        }
        error("Could not verify enough real places and accommodation for this trip. " +
            "Try again, change transport, or check your connection.")
    }

    private data class TrialResult(
        val plan: TripPlan? = null,
        val timetableError: Boolean = false,
        val trainFound: Boolean = false
    )

    private suspend fun <T> bestEffort(default: T, action: suspend () -> T): T = try {
        action()
    } catch (error: CancellationException) {
        // A local timeout is an expected provider failure; a cancelled roll
        // must still stop all of its in-flight requests immediately.
        if (error is TimeoutCancellationException) default else throw error
    } catch (_: Exception) {
        default
    }

    /**
     * Replace one suggested stop in-place without discarding the accepted trip.
     * Only unused/unvisited live POIs are eligible. Never invent a replacement.
     */
    suspend fun replaceStop(plan: TripPlan, target: TripStop, prefs: TripPreferences): TripPlan? {
        val dayIndex = plan.itinerary.indexOfFirst { day ->
            day.stops.any { it.place == target.place }
        }
        if (dayIndex < 0) return null
        val stopIndex = plan.itinerary[dayIndex].stops.indexOfFirst { it.place == target.place }
        if (stopIndex < 0) return null

        val alreadyPlanned = plan.allStops().map { PlaceIdentity.key(it.place) }.toSet()
        val nearby = runCatching {
            source.attractions(plan.destination.point, prefs.style.name, 24)
        }.getOrDefault(emptyList())
        val candidates = if (nearby.size > 8) nearby else {
            nearby + runCatching {
                source.attractions(plan.destination.point, prefs.style.name, 45)
            }.getOrDefault(emptyList())
        }
        val next = rankPlaces(
            candidates.filter { p ->
                PlaceIdentity.key(p) !in alreadyPlanned && !history.contains(p)
            }, plan.destination.point, prefs
        ).firstOrNull { p ->
            openingState(
                p.openingHours, plan.itinerary[dayIndex].date,
                runCatching { LocalTime.parse(target.time) }.getOrDefault(LocalTime.NOON)
            ) != OpeningState.CLOSED
        } ?: return null

        val revised = plan.itinerary.toMutableList()
        val day = revised[dayIndex]
        val stops = day.stops.toMutableList()
        stops[stopIndex] = target.copy(
            place = next,
            whyNow = "Fresh alternative near " + plan.destination.name + ". Check opening hours before visiting."
        )
        revised[dayIndex] = day.copy(stops = stops)
        return plan.copy(itinerary = revised)
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
            if (cat in listOf("museum", "gallery", "attraction", "viewpoint",
                    "castle", "fort", "ruins", "palace", "waterfall")) q += 1.4
            if (prefs.style == TripStyle.NATURE &&
                cat in listOf("viewpoint", "park", "garden", "nature_reserve", "peak", "waterfall")) q += 1.2
            if (prefs.style == TripStyle.CITY &&
                cat in listOf("museum", "gallery", "castle", "palace", "attraction")) q += 1.0
            q -= haversine(center, p.point) / 18.0
            return q
        }
        val close = input.filter { haversine(center, it.point) < 16.0 }
        val usable = if (close.size >= 3) close else input.filter { haversine(center, it.point) < 40.0 }
        return TripDataQuality.distinctPlaces(usable)
            .sortedByDescending(::quality)
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
                destination.trainConnection?.let {
                    runCatching { java.time.OffsetDateTime.parse(it.arrivalIso).toLocalTime() }
                        .getOrNull()?.plusMinutes(30)
                } ?: LocalTime.of(8, 0).plusMinutes(destination.travelMinutes.toLong())
            } else LocalTime.of(9, 0)
            // Reserve stops for later days instead of exhausting the list on day one.
            val count = minOf(if (days == 1) 5 else 4,
                (chosen.size / (days - day)).coerceAtLeast(1))
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

}
