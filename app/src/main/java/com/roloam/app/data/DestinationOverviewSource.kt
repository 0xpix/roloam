package com.roloam.app.data

import com.roloam.app.model.Destination
import com.roloam.app.model.DestinationOverview
import com.roloam.app.model.GeoPoint
import java.net.URLEncoder

/** MediaWiki Action API data; formatversion=2 returns a typed pages array. */
data class WikipediaSummaryResponse(val query: WikipediaSummaryQuery? = null)
data class WikipediaSummaryQuery(val pages: List<WikipediaSummaryPage> = emptyList())
data class WikipediaSummaryPage(
    val title: String? = null,
    val extract: String? = null,
    val coordinates: List<WikipediaCoordinate> = emptyList()
)
data class WikipediaCoordinate(val lat: Double, val lon: Double)

/**
 * Accept an encyclopedia overview only when the article has coordinates near
 * the actual mapped destination. This avoids similarly named towns and
 * disambiguation pages. Wikipedia text is never generated or guessed.
 */
object DestinationOverviewValidator {
    fun verify(destination: Destination, page: WikipediaSummaryPage?): DestinationOverview? {
        if (page == null || !TripDataQuality.validPoint(destination.point)) return null
        val pagePoint = page.coordinates.firstOrNull()?.let { GeoPoint(it.lat, it.lon) }
            ?.takeIf(TripDataQuality::validPoint) ?: return null
        if (haversine(destination.point, pagePoint) > 25.0) return null
        val title = page.title?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val summary = page.extract?.replace(Regex("\\s+"), " ")?.trim()
            ?.takeIf { it.length in 40..2000 } ?: return null
        if (summary.contains("may refer to:", ignoreCase = true)) return null
        val articleUrl = "https://en.wikipedia.org/wiki/" +
            URLEncoder.encode(title.replace(' ', '_'), "UTF-8").replace("+", "%20")
        return DestinationOverview(summary, title, articleUrl)
    }
}

/** Optional enrichment for Reveal; never used to decide whether a trip is valid. */
class DestinationOverviewSource(private val network: Network) {
    private val adapter = network.moshi.adapter(WikipediaSummaryResponse::class.java)

    suspend fun fetch(destination: Destination): DestinationOverview? {
        val title = destination.wikipediaTitle
            ?.takeIf { it.isNotBlank() } ?: destination.name
        val url = "https://en.wikipedia.org/w/api.php?" +
            "action=query&format=json&formatversion=2&prop=extracts%7Ccoordinates" +
            "&exintro=1&explaintext=1&exchars=550&redirects=1&colimit=1&titles=" +
            URLEncoder.encode(title, "UTF-8")
        val response = adapter.fromJson(network.get(url)) ?: return null
        return response.query?.pages.orEmpty()
            .firstNotNullOfOrNull { DestinationOverviewValidator.verify(destination, it) }
    }
}
