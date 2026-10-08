package com.roloam.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.roloam.app.UiState
import com.roloam.app.model.allStops
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint as OsmPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

private enum class MapMode(val label: String) {
    TODAY("Today"),
    STAY("Stay"),
    FULL("Full trip")
}

@Composable
fun MapScreen(state: UiState, back:()->Unit) = Page {
    val trip = state.trip ?: return@Page
    var mode by remember { mutableStateOf(MapMode.TODAY) }

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        RoloamWordmark()
        Text("ROUTE", fontSize = 10.sp, color = RoloamMuted, letterSpacing = 1.4.sp)
    }

    Spacer(Modifier.height(10.dp))
    Text("‹", fontSize=28.sp, modifier=Modifier.clickable{back()})
    BigTitle("Route.")
    Text(
        "From ${trip.originLabel} to ${trip.destination.name}.",
        color = RoloamMuted
    )
    Spacer(Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MapMode.entries.forEach { option ->
            val selected = mode == option
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clickable { mode = option },
                shape = RoundedCornerShape(13.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                border = if (selected) null else BorderStroke(1.dp, RoloamMuted.copy(alpha=.30f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        option.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(10.dp))

    val context = LocalContext.current
    val allStops = remember(trip) { trip.allStops() }

    val pointData = remember(trip, mode) {
        when (mode) {
            MapMode.TODAY -> {
                val firstDay = trip.itinerary.firstOrNull()?.stops.orEmpty()
                buildList {
                    add(trip.origin to trip.originLabel)
                    firstDay.forEachIndexed { index, stop ->
                        add(stop.place.point to "${index + 1}. ${stop.place.name}")
                    }
                }
            }
            MapMode.STAY -> {
                buildList {
                    trip.stay?.let { stay ->
                        val previous = trip.itinerary.firstOrNull()?.stops?.lastOrNull()
                        previous?.let { add(it.place.point to it.place.name) }
                        add(stay.point to stay.name)
                        val next = trip.itinerary.getOrNull(1)?.stops?.firstOrNull()
                        next?.let { add(it.place.point to it.place.name) }
                    }
                    if (isEmpty()) add(trip.destination.point to trip.destination.name)
                }
            }
            MapMode.FULL -> {
                buildList {
                    add(trip.origin to trip.originLabel)
                    allStops.forEachIndexed { index, stop ->
                        add(stop.place.point to "${index + 1}. ${stop.place.name}")
                    }
                    trip.stay?.let { add(it.point to "Stay · ${it.name}") }
                }
            }
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        factory = {
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                minZoomLevel = 3.0
            }
        },
        update = { map ->
            map.overlays.clear()

            val osm = pointData.map { (point, _) -> OsmPoint(point.lat, point.lon) }
            if (osm.size > 1) {
                val line = Polyline().apply {
                    setPoints(osm)
                    outlinePaint.color = android.graphics.Color.rgb(44, 41, 31)
                    outlinePaint.strokeWidth = 5f
                }
                map.overlays.add(line)
            }

            pointData.forEach { (point, label) ->
                val marker = Marker(map).apply {
                    position = OsmPoint(point.lat, point.lon)
                    title = label
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                map.overlays.add(marker)
            }

            if (osm.isNotEmpty()) {
                if (osm.size == 1) {
                    map.controller.setCenter(osm.first())
                    map.controller.setZoom(13.0)
                } else {
                    val box = org.osmdroid.util.BoundingBox.fromGeoPoints(osm)
                    map.zoomToBoundingBox(box, true, 72)
                }
            }
            map.invalidate()
        }
    )

    Text(
        "OpenStreetMap · route order, not turn-by-turn navigation.",
        fontSize = 10.sp,
        color = RoloamMuted,
        modifier = Modifier.padding(top=8.dp)
    )
}
