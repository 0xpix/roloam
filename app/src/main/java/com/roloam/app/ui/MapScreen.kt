package com.roloam.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun MapScreen(state: UiState, back:()->Unit) = Page {
    val trip=state.trip ?: return@Page
    JourneyHeader(state.preferences.transport,false)
    Text("‹",fontSize=28.sp,modifier=Modifier.clickable{back()})
    BigTitle("Route.")
    Text("The order is optimized to reduce backtracking.",color=RoloamMuted)
    Spacer(Modifier.height(12.dp))
    val context=LocalContext.current
    val stops=remember(trip){trip.allStops()}
    val points=remember(trip){ listOf(trip.origin) + stops.map{it.place.point} + listOfNotNull(trip.stay?.point) }
    AndroidView(
        modifier=Modifier.fillMaxWidth().weight(1f),
        factory={
            MapView(context).apply{
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                minZoomLevel=3.0
            }
        },
        update={map->
            map.overlays.clear()
            val osm=points.map{OsmPoint(it.lat,it.lon)}
            val line=Polyline().apply{
                setPoints(osm)
                outlinePaint.color=android.graphics.Color.rgb(44,41,31)
                outlinePaint.strokeWidth=6f
            }
            map.overlays.add(line)
            points.forEachIndexed{i,p->
                val marker=Marker(map).apply{
                    position=OsmPoint(p.lat,p.lon)
                    title=when{
                        i==0->trip.originLabel
                        i-1<stops.size->stops[i-1].place.name
                        else->trip.stay?.name ?: "Stay"
                    }
                    setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM)
                }
                map.overlays.add(marker)
            }
            if(osm.isNotEmpty()){
                val box=org.osmdroid.util.BoundingBox.fromGeoPoints(osm)
                map.zoomToBoundingBox(box,true,72)
            }
            map.invalidate()
        }
    )
    Text("OpenStreetMap · route line shows trip order, not turn-by-turn navigation.",fontSize=10.sp,color=RoloamMuted,modifier=Modifier.padding(top=8.dp))
}
