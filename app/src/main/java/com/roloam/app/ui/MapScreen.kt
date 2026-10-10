package com.roloam.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.view.MotionEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.roloam.app.UiState
import com.roloam.app.data.LiveDataSource
import com.roloam.app.data.MapsLinks
import com.roloam.app.data.Network
import com.roloam.app.model.GeoPoint
import com.roloam.app.model.TripStop
import com.roloam.app.model.Stay
import com.roloam.app.model.TransportMode
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint as OsmPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.roundToInt

private class ClippedInteractiveMapView(context: Context) : MapView(context) {
    override fun draw(canvas: Canvas) {
        val save = canvas.save()
        canvas.clipRect(0, 0, width, height)
        try { super.draw(canvas) } finally { canvas.restoreToCount(save) }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_MOVE ->
                parent?.requestDisallowInterceptTouchEvent(true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                parent?.requestDisallowInterceptTouchEvent(false)
        }
        return super.dispatchTouchEvent(event)
    }
}

private enum class PinKind { DESTINATION, STOP, STAY, ORIGIN }
private data class ItineraryPin(
    val point: GeoPoint,
    val title: String,
    val kind: PinKind,
    val badge: String,
    val stop: TripStop? = null,
    val stay: Stay? = null
)

/** Stop pins are custom-drawn at screen density, not the tiny default OSM marker. */
private fun pinDrawable(context: Context, kind: PinKind, badge: String): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val w = (44 * density).roundToInt().coerceAtLeast(44)
    val h = (57 * density).roundToInt().coerceAtLeast(57)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val dark = Color.rgb(30, 52, 39)
    val gold = Color.rgb(180, 133, 54)
    val paper = Color.rgb(250, 247, 232)
    paint.color = if (kind == PinKind.STOP) dark else gold
    canvas.drawCircle(w / 2f, 20 * density, 17 * density, paint)
    val tail = Path().apply {
        moveTo(w / 2f - 8 * density, 31 * density)
        lineTo(w / 2f, 51 * density)
        lineTo(w / 2f + 8 * density, 31 * density)
        close()
    }
    canvas.drawPath(tail, paint)
    paint.color = paper
    paint.textSize = (if (badge.length > 2) 12f else 17f) * density
    paint.typeface = android.graphics.Typeface.create(
        android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD
    )
    paint.textAlign = Paint.Align.CENTER
    val metrics = paint.fontMetrics
    canvas.drawText(badge, w / 2f, 20 * density - (metrics.ascent + metrics.descent) / 2f, paint)
    return BitmapDrawable(context.resources, bitmap)
}

private fun makePins(state: UiState, selection: String): List<ItineraryPin> {
    val trip = state.trip ?: return emptyList()
    return buildList {
        if (selection == "STAY") {
            trip.stay?.let { add(ItineraryPin(it.point, "Stay · " + it.name, PinKind.STAY, "S", stay=it)) }
            trip.alternativeStays.take(4).forEach {
                add(ItineraryPin(it.point, "Alternative · " + it.name, PinKind.STAY, "S", stay=it))
            }
            if (isEmpty()) {
                add(ItineraryPin(trip.destination.point, trip.destination.name, PinKind.DESTINATION, "D"))
            }
        } else {
            if (selection == "ALL") {
                add(ItineraryPin(trip.origin, "Start · " + trip.originLabel, PinKind.ORIGIN, "O"))
            }
            add(ItineraryPin(trip.destination.point, trip.destination.name, PinKind.DESTINATION, "D"))
            trip.itinerary.forEachIndexed { day, itinerary ->
                if (selection == "ALL" || selection == "DAY_$day") {
                    itinerary.stops.forEachIndexed { index, stop ->
                        add(ItineraryPin(
                            stop.place.point,
                            "Day ${day+1}, stop ${index+1} · " + stop.place.name,
                            PinKind.STOP,
                            if (selection == "ALL") "${day+1}.${index+1}" else "${index+1}",
                            stop=stop
                        ))
                    }
                }
            }
            if (selection == "ALL") trip.stay?.let {
                add(ItineraryPin(it.point, "Stay · " + it.name, PinKind.STAY, "S", stay=it))
            }
        }
    }
}

@Composable
fun MapScreen(state: UiState, back: () -> Unit, replaceStop: (TripStop)->Unit) = Page {
    val trip = state.trip ?: return@Page
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var selection by remember(trip.generatedAtEpochMs) { mutableStateOf("DAY_0") }
    var selectedPin by remember(trip.generatedAtEpochMs) { mutableStateOf<ItineraryPin?>(null) }
    var refit by remember(trip.generatedAtEpochMs, selection) { mutableIntStateOf(0) }
    val cameraKey = trip.generatedAtEpochMs.toString() + ":" + selection + ":" + refit
    val pins = remember(trip, selection) { makePins(state, selection) }
    val source = remember(context) { LiveDataSource(Network(context.applicationContext)) }

    val mapView = remember(context, trip.generatedAtEpochMs) {
        ClippedInteractiveMapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setUseDataConnection(true)
            minZoomLevel = 3.0
            maxZoomLevel = 19.0
            controller.setZoom(12.0)
            controller.setCenter(OsmPoint(trip.destination.point.lat, trip.destination.point.lon))
            setBackgroundColor(Color.rgb(30, 31, 28))
            overlayManager.tilesOverlay.setLoadingLineColor(Color.TRANSPARENT)
            clipChildren = true
            clipToPadding = true
        }
    }
    DisposableEffect(mapView, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when(event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    // Full trip routes can exceed the public OSRM waypoint limit; batch with overlap.
    var route by remember(trip.generatedAtEpochMs, selection, state.preferences.transport) {
        mutableStateOf<List<GeoPoint>?>(null)
    }
    LaunchedEffect(trip.generatedAtEpochMs, selection, state.preferences.transport) {
        val mode = when(state.preferences.transport) {
            TransportMode.CAR -> "car"
            TransportMode.BIKE -> "bike"
            TransportMode.WALK -> "walk"
            TransportMode.TRAIN -> "train"
        }
        route = if (mode == "train" || selection == "STAY") emptyList() else {
            val points = pins.filter { it.kind != PinKind.STAY }.map { it.point }
            if(points.size < 2) emptyList() else runCatching {
                val geometry = mutableListOf<GeoPoint>()
                var at = 0
                while (at < points.lastIndex) {
                    val part = points.subList(at, (at + 7).coerceAtMost(points.lastIndex) + 1)
                    val line = source.routeGeometry(part, mode)
                    if(line.isEmpty()) break
                    geometry.addAll(if(geometry.isEmpty()) line else line.drop(1))
                    at += part.size - 1
                }
                geometry
            }.getOrDefault(emptyList())
        }
    }

    RoloamSectionBar("MAP")
    Spacer(Modifier.height(12.dp))
    Text("‹  Route.", fontSize=29.sp, fontWeight=FontWeight.Black,
        modifier=Modifier.clickable { back() })
    Text(trip.destination.name + " · " + trip.days + " days", color=RoloamMuted)
    Spacer(Modifier.height(10.dp))

    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement=Arrangement.spacedBy(7.dp)) {
        trip.itinerary.forEachIndexed { index, _ ->
            MapTab("DAY ${index+1}", selection=="DAY_$index") { selection="DAY_$index" }
        }
        MapTab("ALL", selection=="ALL") { selection="ALL" }
        MapTab("STAYS", selection=="STAY") { selection="STAY" }
    }
    Spacer(Modifier.height(8.dp))
    val count = pins.count { it.kind == PinKind.STOP }
    Text(
        when {
            selection=="STAY" -> "${pins.count { it.kind == PinKind.STAY }} accommodation pins"
            count==0 -> "No attraction stops were retrieved. Showing the destination."
            else -> "$count numbered itinerary stops · tap any pin for its name"
        }, fontSize=11.sp, color=RoloamMuted
    )
    Spacer(Modifier.height(8.dp))

    Box(
        modifier=Modifier.fillMaxWidth().weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, RoloamMuted.copy(alpha=.25f), RoundedCornerShape(16.dp))
    ) {
        AndroidView(
            modifier=Modifier.fillMaxSize().clipToBounds(),
            factory={ mapView },
            update={ map ->
                val previous = map.tag as? Pair<*, *>
                val key = cameraKey to (route?.size ?: -1)
                if(previous != key) {
                    val fit = previous?.first != cameraKey
                    map.tag = key
                    map.overlays.clear()
                    val routePoints = route.orEmpty().map { OsmPoint(it.lat, it.lon) }
                    if(routePoints.size > 1) {
                        map.overlays.add(Polyline().apply {
                            setPoints(routePoints)
                            outlinePaint.color=Color.rgb(35, 82, 62)
                            outlinePaint.strokeWidth=6f * map.resources.displayMetrics.density
                        })
                    }
                    pins.forEach { pin ->
                        map.overlays.add(Marker(map).apply {
                            position=OsmPoint(pin.point.lat,pin.point.lon)
                            title=pin.title
                            icon=pinDrawable(context, pin.kind, pin.badge)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            setOnMarkerClickListener { _, _ ->
                                selectedPin = pin
                                true
                            }
                        })
                    }
                    if(fit) {
                        map.doOnLayout {
                            if((map.tag as? Pair<*, *>)?.first == cameraKey) {
                                val coords = pins.map { OsmPoint(it.point.lat,it.point.lon) }
                                if(coords.size > 1) {
                                    map.zoomToBoundingBox(
                                        org.osmdroid.util.BoundingBox.fromGeoPoints(coords),
                                        false, (64*map.resources.displayMetrics.density).roundToInt()
                                    )
                                } else if(coords.size==1) {
                                    map.controller.setZoom(13.5)
                                    map.controller.setCenter(coords.first())
                                }
                            }
                        }
                    }
                    map.invalidate()
                }
            }
        )
    }
    Spacer(Modifier.height(7.dp))
    // The itinerary is visible even if network map tiles are unavailable.
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement=Arrangement.spacedBy(7.dp)) {
        pins.filter { it.kind == PinKind.STOP || it.kind == PinKind.STAY }.forEach { pin ->
            OutlinedButton(
                onClick={
                    selectedPin = pin
                    mapView.controller.animateTo(OsmPoint(pin.point.lat, pin.point.lon))
                    mapView.controller.setZoom(15.0)
                },
                shape=RoundedCornerShape(12.dp),
                contentPadding=PaddingValues(horizontal=10.dp)
            ) {
                Text(pin.badge + " " + pin.title.substringAfter(" · "), fontSize=10.sp,
                    maxLines=1, overflow=TextOverflow.Ellipsis)
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick={refit++}, modifier=Modifier.weight(1f),
            shape=RoundedCornerShape(13.dp)) { Text("FIT MAP", fontSize=11.sp) }
        Button(
            onClick={
                val url=MapsLinks.directions(trip.origin,trip.destination.point,state.preferences.transport)
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            },
            modifier=Modifier.weight(1.5f),
            shape=RoundedCornerShape(13.dp)
        ) { Text("DIRECTIONS ↗", fontSize=11.sp) }
    }
    state.error?.let {
        Text(it,color=MaterialTheme.colorScheme.error,fontSize=11.sp,
            modifier=Modifier.padding(top=5.dp))
    }
    Text(
        when {
            state.preferences.transport==TransportMode.TRAIN ->
                "Transit: use Google Maps for live connections."
            route == null -> "Loading road and path details…"
            route!!.isEmpty() -> "Pins are available; detailed route geometry could not load."
            else -> "Numbered stops follow the itinerary. Tap to inspect."
        },
        fontSize=10.sp,color=RoloamMuted,
        modifier=Modifier.padding(top=4.dp, bottom=4.dp)
    )
    selectedPin?.let { pin ->
        ModalBottomSheet(
            onDismissRequest = { selectedPin = null },
            containerColor=MaterialTheme.colorScheme.surface
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .padding(horizontal=24.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    when(pin.kind) {
                        PinKind.STOP -> "ITINERARY STOP " + pin.badge
                        PinKind.STAY -> "ACCOMMODATION · MAPPED LOCATION"
                        PinKind.ORIGIN -> "YOUR STARTING POINT"
                        PinKind.DESTINATION -> "DESTINATION"
                    },
                    fontSize=10.sp,color=RoloamAccent,fontWeight=FontWeight.Bold
                )
                Spacer(Modifier.height(9.dp))
                Text(
                    pin.stop?.place?.name ?: pin.stay?.name ?: pin.title,
                    fontWeight=FontWeight.Black,fontSize=21.sp
                )
                Text(
                    pin.stop?.let {
                        it.time + " · " + it.durationMinutes + " min · " + it.place.category.replace('_',' ')
                    } ?: pin.stay?.let {
                        it.category.replace('_',' ') + " · " +
                            String.format(java.util.Locale.ROOT,"%.1f",it.distanceFromCenterKm) +
                            " km from destination"
                    } ?: "Your mapped trip location",
                    fontSize=12.sp,color=RoloamMuted
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "OpenStreetMap pin · " +
                        String.format(java.util.Locale.ROOT,"%.5f, %.5f",pin.point.lat,pin.point.lon),
                    color=RoloamMuted,fontSize=11.sp
                )
                Text(
                    "Google Maps opens these exact coordinates; the listing name or availability may differ.",
                    color=RoloamMuted,fontSize=11.sp,lineHeight=17.sp
                )
                Spacer(Modifier.height(13.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                    OutlinedButton(
                        onClick={
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW,
                                    Uri.parse(MapsLinks.place(pin.title,pin.point))))
                            }
                        },
                        modifier=Modifier.weight(1f)
                    ) {Text("EXACT PIN ↗",fontSize=11.sp)}
                    Button(
                        onClick={
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW,
                                    Uri.parse(MapsLinks.navigateTo(pin.point,state.preferences.transport))))
                            }
                        },
                        modifier=Modifier.weight(1f)
                    ) {Text("DIRECTIONS ↗",fontSize=11.sp)}
                }
                pin.stop?.let { stop ->
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick={ selectedPin=null; replaceStop(stop) },
                        enabled=!state.replacingPlace,
                        modifier=Modifier.fillMaxWidth()
                    ) {
                        Text(if(state.replacingPlace) "SEARCHING…" else "ALREADY VISITED? REPLACE STOP ↻",
                            fontSize=11.sp)
                    }
                }
                Spacer(Modifier.height(22.dp))
            }
        }
    }

}

@Composable
private fun MapTab(label:String, selected:Boolean, onClick:()->Unit) {
    Surface(
        modifier=Modifier.height(36.dp).clickable(onClick=onClick),
        shape=RoundedCornerShape(12.dp),
        color=if(selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor=if(selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        border=if(selected) null else BorderStroke(1.dp, RoloamMuted.copy(alpha=.25f))
    ) {
        Box(Modifier.padding(horizontal=15.dp), contentAlignment=Alignment.Center) {
            Text(label,fontSize=11.sp,fontWeight=FontWeight.Bold)
        }
    }
}
