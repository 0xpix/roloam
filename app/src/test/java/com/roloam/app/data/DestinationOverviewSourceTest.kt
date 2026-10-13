package com.roloam.app.data

import com.roloam.app.model.Destination
import com.roloam.app.model.GeoPoint
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test

class DestinationOverviewSourceTest {
    private val destination = Destination(
        name = "Heidelberg",
        point = GeoPoint(49.3988, 8.6724)
    )

    @Test fun acceptsOnlyNearbyEncyclopediaArticles() {
        val page = WikipediaSummaryPage(
            title = "Heidelberg",
            extract = "Heidelberg is a university town in Baden-Württemberg, Germany.",
            coordinates = listOf(WikipediaCoordinate(49.3988, 8.6724))
        )
        val verified = DestinationOverviewValidator.verify(destination, page)
        assertNotNull(verified)
        assertEquals("Heidelberg", verified!!.sourceTitle)
        assertEquals("https://en.wikipedia.org/wiki/Heidelberg", verified.sourceUrl)
        assertTrue(verified.summary.startsWith("Heidelberg"))
    }

    @Test fun rejectsAnotherTownWithTheSameName() {
        val page = WikipediaSummaryPage(
            title = "Heidelberg, South Africa",
            extract = "Heidelberg is a town in South Africa, and not the German city.",
            coordinates = listOf(WikipediaCoordinate(-26.5, 28.4))
        )
        assertNull(DestinationOverviewValidator.verify(destination, page))
    }

    @Test fun rejectsMissingCoordinatesEmptyAndDisambiguationText() {
        assertNull(DestinationOverviewValidator.verify(destination,
            WikipediaSummaryPage("Heidelberg", "This description has no matching coordinates.")))
        assertNull(DestinationOverviewValidator.verify(destination,
            WikipediaSummaryPage("Heidelberg", "Short", listOf(WikipediaCoordinate(49.4, 8.7)))))
        assertNull(DestinationOverviewValidator.verify(destination,
            WikipediaSummaryPage("Heidelberg", "Heidelberg may refer to: several different cities.",
                listOf(WikipediaCoordinate(49.4, 8.7)))))
    }

    @Test fun parsesMediaWikiFormatVersionTwoJson() {
        val response = """
            {
              "query": {
                "pages": [{
                  "pageid": 123,
                  "title": "Heidelberg",
                  "extract": "Heidelberg is a university town in the German state of Baden-Württemberg.",
                  "coordinates": [{"lat":49.3988,"lon":8.6724}]
                }]
              }
            }
        """.trimIndent()
        val adapter = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            .adapter(WikipediaSummaryResponse::class.java)
        val decoded = adapter.fromJson(response)
        assertNotNull(DestinationOverviewValidator.verify(destination,
            decoded?.query?.pages?.firstOrNull()))
    }
}
