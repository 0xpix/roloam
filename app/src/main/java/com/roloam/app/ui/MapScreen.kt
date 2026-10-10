package com.roloam.app.ui

import android.content.Intent
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.view.MotionEvent
import androidx.core.view.doOnLayout
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.roloam.app.UiState
import com.roloam.app.data.MapsLinks
import com.roloam.app.data.LiveDataSource
import com.roloam.app.data.Network
import com.roloam.app.model.GeoPoint
import com.roloam.app.model.TransportMode
import com.roloam.app.model.allStops
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint as OsmPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * Native hard clipping matters for OSMDroid: tile canvas and marker overlays
 * must never draw across adjacent Compose content, even during pinch-zoom.
 */
private class ClippedInteractiveMapView(context: Context) : MapView(context) {
    override fun draw(canvas: Canvas) {
        val save = canvas.save()
        canvas.clipRect(0, 0, width, height)
        try {
            super.draw(canvas)
        } finally {
            canvas.restoreToCount(save)
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // Keep pan, pinch, and flings within the map instead of passing
        // them to a surrounding page / navigation gesture container.
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_MOVE -> parent?.requestDisallowInterceptTouchEvent(true)
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> parent?.requestDisallowInterceptTouchEvent(false)
        }
        return super.dispatchTouchEvent(event)
    }
}


private enum class MapMode(val label: String) {
    TODAY("Day 1"),
    STAY("Stay"),
    FULL("Full trip")
}

@Composable
fun MapScreen(state: UiState, back: () -> Unit) = Page {
    val trip = state.trip ?: return@Page
    var mode by remember(trip) { mutableStateOf(MapMode.TODAY) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember(context, trip.generatedAtEpochMs) {
        ClippedInteractiveMapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setUseDataConnection(true)
            minZoomLevel = 3.0
            maxZoomLevel = 19.0
            // OSMDroid otherwise starts at (0, 0), often displaying unrelated
            // African map tiles before layout has completed.
            controller.setZoom(11.0)
            controller.setCenter(OsmPoint(trip.destination.point.lat, trip.destination.point.lon))
            setBackgroundColor(Color.rgb(30, 31, 28))
            overlayManager.tilesOverlay.setLoadingLineColor(Color.TRANSPARENT)
            clipChildren = true
            clipToPadding = true
            clipToOutline = true
        }
    }

    // osmdroid owns a tile downloader and cache: pause and detach when leaving the screen.
    DisposableEffect(mapView, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        RoloamWordmark()
        Text("MAP", fontSize = 10.sp, color = RoloamMuted, letterSpacing = 1.4.sp)
    }
    Spacer(Modifier.height(10.dp))
    Text("‹", fontSize = 28.sp, modifier = Modifier.clickable { back() })
    BigTitle("Route.")
    Text("From " + trip.originLabel + " to " + trip.destination.name, color = RoloamMuted)
    Spacer(Modifier.height(12.dp))

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        MapMode.entries.forEach { option ->
            val selected = mode == option
            Surface(
                modifier = Modifier.weight(1f).height(38.dp).clickable { mode = option },
                shape = RoundedCornerShape(13.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                border = if (selected) null else BorderStroke(1.dp, RoloamMuted.copy(alpha = .30f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(option.label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
    Spacer(Modifier.height(10.dp))

    val pointData = remember(trip, mode) {
        buildList {
            when (mode) {
                MapMode.TODAY -> {
                    add(trip.origin to trip.originLabel)
                    add(trip.destination.point to trip.destination.name)
                    trip.itinerary.firstOrNull()?.stops.orEmpty().forEachIndexed { index, stop ->
                        add(stop.place.point to ((index + 1).toString() + ". " + stop.place.name))
                    }
                }
                MapMode.STAY -> {
                    trip.itinerary.firstOrNull()?.stops?.lastOrNull()?.let {
                        add(it.place.point to it.place.name)
                    }
                    trip.stay?.let { add(it.point to ("Stay · " + it.name)) }
                    trip.itinerary.getOrNull(1)?.stops?.firstOrNull()?.let {
                        add(it.place.point to it.place.name)
                    }
                    if (isEmpty()) add(trip.destination.point to trip.destination.name)
                }
                MapMode.FULL -> {
                    add(trip.origin to trip.originLabel)
                    add(trip.destination.point to trip.destination.name)
                    trip.itinerary.forEachIndexed { dayIndex, day ->
                        day.stops.forEachIndexed { index, stop ->
                            add(stop.place.point to ("D" + (dayIndex + 1) + "·" + (index + 1) + " " + stop.place.name))
                        }
                        if (dayIndex < trip.days - 1) trip.stay?.let {
                            add(it.point to ("Stay · " + it.name))
                        }
                    }
                }
            }
        }
    }
    var refitCount by remember(trip.generatedAtEpochMs, mode) { mutableIntStateOf(0) }
    val cameraKey = trip.generatedAtEpochMs.toString() + ":" + mode.name + ":" + refitCount
    val routeSource = remember(context) { LiveDataSource(Network(context.applicationContext)) }
    var routedLine by remember(cameraKey, state.preferences.transport) {
        mutableStateOf<List<GeoPoint>?>(null)
    }
    LaunchedEffect(trip.generatedAtEpochMs, mode, state.preferences.transport) {
        if (state.preferences.transport == TransportMode.TRAIN) {
            routedLine = emptyList()
        } else {
            val requestedMode = when (state.preferences.transport) {
                TransportMode.CAR -> "car"
                TransportMode.BIKE -> "bike"
                TransportMode.WALK -> "walk"
                TransportMode.TRAIN -> "train"
            }
            routedLine = runCatching {
                routeSource.routeGeometry(pointData.map { it.first }, requestedMode)
            }.getOrDefault(emptyList())
        }
    }
    // Fixed native frame + Compose clip: the entire MapView stays contained.
    // In particular, zoom/pan changes only OSMDroid's viewport.
    val mapShape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(mapShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, RoloamMuted.copy(alpha = .22f), mapShape)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize().clipToBounds(),
            factory = { mapView },
            update = { map ->
                val previousRender = map.tag as? Pair<*, *>
                val renderKey = cameraKey to (routedLine?.size ?: -1)
                if (previousRender != renderKey) {
                    val reposition = previousRender?.first != cameraKey
                    map.tag = renderKey
                    map.overlays.clear()
                    val points = pointData.map { (point, _) -> OsmPoint(point.lat, point.lon) }
                    val route = routedLine.orEmpty().map { OsmPoint(it.lat, it.lon) }
                    if (route.size > 1) {
                        map.overlays.add(Polyline().apply {
                            setPoints(route)
                            outlinePaint.color = Color.rgb(80, 110, 91)
                            outlinePaint.strokeWidth = 5f
                        })
                    }
                    pointData.forEach { (point, label) ->
                        map.overlays.add(Marker(map).apply {
                            position = OsmPoint(point.lat, point.lon)
                            title = label
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        })
                    }
                    if (reposition) {
                        // Immediately move off OSMDroid's (0,0) default; then fit
                        // only after a real, non-zero AndroidView layout.
                        val focus = points.getOrNull(1) ?: points.firstOrNull()
                        if (focus != null) {
                            map.controller.setCenter(focus)
                            map.controller.setZoom(11.0)
                        }
                        map.doOnLayout {
                            if ((map.tag as? Pair<*, *>)?.first == cameraKey) {
                                if (points.size == 1) {
                                    map.controller.setCenter(points[0])
                                    map.controller.setZoom(13.0)
                                } else if (points.size > 1 && map.width > 0 && map.height > 0) {
                                    map.zoomToBoundingBox(
                                        org.osmdroid.util.BoundingBox.fromGeoPoints(points),
                                        false,
                                        (48 * map.resources.displayMetrics.density).toInt()
                                    )
                                }
                            }
                            map.invalidate()
                        }
                    }
                    map.invalidate()
                }
            }
        )
    }

    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = { refitCount++ },
        modifier = Modifier.fillMaxWidth().height(38.dp),
        shape = RoundedCornerShape(12.dp)
    ) { Text("FIT TRIP TO MAP", fontSize = 11.sp) }

    Spacer(Modifier.height(10.dp))
    Button(
        onClick = {
            val url = MapsLinks.directions(trip.origin, trip.destination.point, state.preferences.transport)
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text("OPEN GOOGLE MAPS DIRECTIONS →")
    }
    Text(
        when {
            state.preferences.transport == TransportMode.TRAIN ->
                "Train: stops are shown as pins; use Maps for live transit navigation."
            routedLine == null -> "Loading road or path geometry…"
            routedLine!!.size > 1 ->
                "Map shows the routed path and stops. Check Maps for navigation."
            else -> "Routing unavailable: stops shown as pins. Use Maps for directions."
        },
        fontSize = 10.sp,
        color = RoloamMuted,
        modifier = Modifier.padding(top = 7.dp, bottom = 3.dp)
    )
}
