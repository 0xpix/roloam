package com.roloam.app.data

import android.content.Context
import com.roloam.app.model.GeoPoint
import com.roloam.app.model.Place
import com.roloam.app.model.Stay
import com.roloam.app.model.WeatherDay
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.math.*

class Network(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .cache(okhttp3.Cache(File(context.cacheDir, "http"), 20L * 1024 * 1024))
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(14, TimeUnit.SECONDS)
        .callTimeout(18, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    suspend fun get(url: String): String = request(Request.Builder().url(url).get().build())

    suspend fun postForm(url: String, key: String, value: String): String {
        val body = FormBody.Builder().add(key, value).build()
        return request(Request.Builder().url(url).post(body).build())
    }

    private suspend fun request(request: Request): String = withContext(Dispatchers.IO) {
        var last: Throwable? = null
        repeat(2) { attempt ->
            try {
                val req = request.newBuilder()
                    .header("User-Agent", "Roloam/0.1 Android; contact via github.com/0xpix/roloam")
                    .header("Accept", "application/json")
                    .build()
                client.newCall(req).execute().use { response ->
                    if (!response.isSuccessful) error("HTTP " + response.code)
                    return@withContext response.body?.string() ?: error("Empty response")
                }
            } catch (t: Throwable) {
                last = t
                if (attempt == 0) delay(450)
            }
        }
        throw last ?: IllegalStateException("Network request failed")
    }
}

data class OverpassResponse(val elements: List<OverpassElement> = emptyList())
data class OverpassCenter(val lat: Double, val lon: Double)
data class OverpassElement(
    val id: Long,
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OverpassCenter? = null,
    val tags: Map<String, String>? = null
) {
    fun point(): GeoPoint? {
        val la = lat ?: center?.lat
        val lo = lon ?: center?.lon
        return if (la != null && lo != null) GeoPoint(la, lo) else null
    }
}

data class OsrmTable(
    val durations: List<List<Double?>>? = null,
    val distances: List<List<Double?>>? = null
)

data class OpenMeteo(val daily: OpenMeteoDaily? = null)
data class OpenMeteoDaily(
    val time: List<String> = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int> = emptyList(),
    @Json(name = "temperature_2m_max") val max: List<Double?> = emptyList(),
    @Json(name = "temperature_2m_min") val min: List<Double?> = emptyList(),
    @Json(name = "precipitation_probability_max") val precipitation: List<Int?> = emptyList(),
    val sunrise: List<String?> = emptyList(),
    val sunset: List<String?> = emptyList()
)

class LiveDataSource(private val network: Network) {
    private val overpassAdapter = network.moshi.adapter(OverpassResponse::class.java)
    private val tableAdapter = network.moshi.adapter(OsrmTable::class.java)
    private val weatherAdapter = network.moshi.adapter(OpenMeteo::class.java)

    suspend fun cities(origin: GeoPoint, radiusKm: Int): List<CityRaw> {
        val meters = radiusKm.coerceAtMost(650) * 1000
        val q = """
            [out:json][timeout:12];
            (
              nwr["place"="city"](around:$meters,${origin.lat},${origin.lon});
              nwr["place"="town"]["population"](around:$meters,${origin.lat},${origin.lon});
            );
            out center tags 180;
        """.trimIndent()
        val json = network.postForm("https://overpass-api.de/api/interpreter", "data", q)
        return overpassAdapter.fromJson(json)?.elements.orEmpty().mapNotNull { e ->
            val p = e.point() ?: return@mapNotNull null
            val tags = e.tags ?: return@mapNotNull null
            val name = tags["name"] ?: return@mapNotNull null
            val population = tags["population"]?.filter(Char::isDigit)?.toLongOrNull()
            val country = tags["addr:country"] ?: tags["is_in:country"]
            CityRaw(name, country, p, population)
        }.distinctBy { it.name.lowercase() }
    }

    suspend fun routeTable(origin: GeoPoint, points: List<GeoPoint>, mode: String): List<RouteMetric?> {
        if (points.isEmpty()) return emptyList()
        val base = when (mode) {
            "bike" -> "https://routing.openstreetmap.de/routed-bike"
            "walk" -> "https://routing.openstreetmap.de/routed-foot"
            else -> "https://router.project-osrm.org"
        }
        val coords = (listOf(origin) + points).joinToString(";") { p -> p.lon.toString() + "," + p.lat }
        val url = base + "/table/v1/driving/" + coords + "?sources=0&annotations=duration,distance"
        val parsed = tableAdapter.fromJson(network.get(url))
        val durs = parsed?.durations?.firstOrNull().orEmpty().drop(1)
        val dists = parsed?.distances?.firstOrNull().orEmpty().drop(1)
        return points.indices.map { i ->
            val sec = durs.getOrNull(i)
            val meters = dists.getOrNull(i)
            if (sec == null) null else RouteMetric((sec / 60.0).roundToInt(), (meters ?: 0.0) / 1000.0)
        }
    }

    suspend fun attractions(center: GeoPoint, style: String): List<Place> {
        val q = """
            [out:json][timeout:12];
            (
              nwr["tourism"~"attraction|museum|gallery|viewpoint"](around:15000,${center.lat},${center.lon});
              nwr["historic"]["name"](around:15000,${center.lat},${center.lon});
              nwr["leisure"~"park|garden"]["name"](around:15000,${center.lat},${center.lon});
            );
            out center tags 120;
        """.trimIndent()
        val response = overpassAdapter.fromJson(
            network.postForm("https://overpass-api.de/api/interpreter", "data", q)
        )
        return response?.elements.orEmpty().mapNotNull { e ->
            val p = e.point() ?: return@mapNotNull null
            val tags = e.tags ?: return@mapNotNull null
            val name = tags["name"] ?: return@mapNotNull null
            val category = tags["tourism"] ?: tags["historic"]?.let { "historic" }
                ?: tags["leisure"] ?: "place"
            Place(
                id = e.id,
                name = name,
                point = p,
                category = category,
                openingHours = tags["opening_hours"],
                website = tags["website"] ?: tags["contact:website"],
                description = tags["description"]
            )
        }.distinctBy { it.name.lowercase() }
    }

    suspend fun stays(center: GeoPoint): List<Stay> {
        val q = """
            [out:json][timeout:12];
            (
              nwr["tourism"~"camp_site|caravan_site|hostel|hotel|guest_house"](around:25000,${center.lat},${center.lon});
            );
            out center tags 90;
        """.trimIndent()
        val response = overpassAdapter.fromJson(
            network.postForm("https://overpass-api.de/api/interpreter", "data", q)
        )
        return response?.elements.orEmpty().mapNotNull { e ->
            val p = e.point() ?: return@mapNotNull null
            val tags = e.tags ?: return@mapNotNull null
            val name = tags["name"] ?: return@mapNotNull null
            Stay(
                id = e.id,
                name = name,
                point = p,
                category = tags["tourism"] ?: "stay",
                website = tags["website"] ?: tags["contact:website"],
                phone = tags["phone"] ?: tags["contact:phone"],
                openingHours = tags["opening_hours"],
                distanceFromCenterKm = haversine(center, p)
            )
        }.distinctBy { it.name.lowercase() }
    }

    suspend fun weather(center: GeoPoint): List<WeatherDay> {
        val params = "latitude=${center.lat}&longitude=${center.lon}" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset" +
            "&timezone=auto&forecast_days=16"
        val data = weatherAdapter.fromJson(network.get("https://api.open-meteo.com/v1/forecast?" + params))
            ?.daily ?: return emptyList()
        return data.time.indices.mapNotNull { i ->
            runCatching {
                WeatherDay(
                    LocalDate.parse(data.time[i]),
                    data.weatherCode.getOrElse(i) { -1 },
                    data.max.getOrNull(i),
                    data.min.getOrNull(i),
                    data.precipitation.getOrNull(i),
                    data.sunrise.getOrNull(i),
                    data.sunset.getOrNull(i)
                )
            }.getOrNull()
        }
    }
}

data class CityRaw(val name: String, val country: String?, val point: GeoPoint, val population: Long?)
data class RouteMetric(val minutes: Int, val distanceKm: Double)

fun haversine(a: GeoPoint, b: GeoPoint): Double {
    val r = 6371.0
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLon = Math.toRadians(b.lon - a.lon)
    val x = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLon / 2).pow(2)
    return 2 * r * asin(sqrt(x))
}
