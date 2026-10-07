package com.roloam.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roloam.app.Screen
import com.roloam.app.UiState
import com.roloam.app.model.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun Page(content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            content = content
        )
    }
}

@Composable
fun JourneyHeader(mode: TransportMode, active: Boolean = true) {
    val infinite = rememberInfiniteTransition(label = "journey")
    val progress by infinite.animateFloat(
        0.12f, 0.88f,
        infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "progress"
    )
    val p = if (active) progress else .58f
    val bg = MaterialTheme.colorScheme.background
    Canvas(Modifier.fillMaxWidth().height(52.dp)) {
        val y = size.height * .55f
        val path = Path()
        val left = size.width * .05f
        val right = size.width * .95f
        path.moveTo(left, y)
        val segments = 44
        for (i in 1..segments) {
            val f = i / segments.toFloat()
            val x = left + (right - left) * f
            val yy = y + sin(f * 9f * Math.PI).toFloat() * 5.dp.toPx()
            path.lineTo(x, yy)
        }
        drawPath(path, RoloamMuted.copy(alpha=.65f), style=Stroke(width=1.5.dp.toPx(), pathEffect=androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 5.dp.toPx()))))
        drawCircle(RoloamInk, 4.dp.toPx(), Offset(left, y))
        drawCircle(RoloamAccent, 4.dp.toPx(), Offset(right, y), style=Stroke(2.dp.toPx()))
        val x = left + (right-left)*p
        val yy = y + sin(p * 9f * Math.PI).toFloat() * 5.dp.toPx()
        when(mode) {
            TransportMode.CAR -> {
                drawRoundRect(RoloamInk, Offset(x-10.dp.toPx(),yy-6.dp.toPx()), androidx.compose.ui.geometry.Size(20.dp.toPx(),10.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                drawCircle(bg, 2.5.dp.toPx(), Offset(x-6.dp.toPx(), yy+5.dp.toPx()))
                drawCircle(bg, 2.5.dp.toPx(), Offset(x+6.dp.toPx(), yy+5.dp.toPx()))
            }
            TransportMode.TRAIN -> {
                drawRoundRect(RoloamInk, Offset(x-7.dp.toPx(),yy-9.dp.toPx()), androidx.compose.ui.geometry.Size(14.dp.toPx(),18.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                drawLine(bg, Offset(x-4.dp.toPx(),yy-4.dp.toPx()),Offset(x+4.dp.toPx(),yy-4.dp.toPx()),2.dp.toPx())
            }
            TransportMode.BIKE -> {
                drawCircle(RoloamInk, 5.dp.toPx(), Offset(x-7.dp.toPx(),yy+4.dp.toPx()), style=Stroke(1.8.dp.toPx()))
                drawCircle(RoloamInk, 5.dp.toPx(), Offset(x+7.dp.toPx(),yy+4.dp.toPx()), style=Stroke(1.8.dp.toPx()))
                drawLine(RoloamInk, Offset(x-7.dp.toPx(),yy+4.dp.toPx()),Offset(x,yy-4.dp.toPx()),1.8.dp.toPx())
                drawLine(RoloamInk, Offset(x,yy-4.dp.toPx()),Offset(x+7.dp.toPx(),yy+4.dp.toPx()),1.8.dp.toPx())
            }
            TransportMode.WALK -> {
                drawCircle(RoloamInk, 3.dp.toPx(), Offset(x,yy-8.dp.toPx()))
                drawLine(RoloamInk,Offset(x,yy-4.dp.toPx()),Offset(x,yy+4.dp.toPx()),2.dp.toPx(),cap=StrokeCap.Round)
                drawLine(RoloamInk,Offset(x,yy),Offset(x-6.dp.toPx(),yy+6.dp.toPx()),2.dp.toPx(),cap=StrokeCap.Round)
                drawLine(RoloamInk,Offset(x,yy+4.dp.toPx()),Offset(x+6.dp.toPx(),yy+9.dp.toPx()),2.dp.toPx(),cap=StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun DateStamp() {
    Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEE · dd MMM")).uppercase(), fontSize=12.sp, fontWeight=FontWeight.Bold, color=RoloamMuted)
}

@Composable
fun BigTitle(text: String) {
    Text(text, fontSize=36.sp, lineHeight=39.sp, fontWeight=FontWeight.Black, letterSpacing=(-1.3).sp, color=MaterialTheme.colorScheme.onBackground)
}

@Composable
private fun PrimaryButton(text:String, enabled:Boolean=true, onClick:()->Unit) {
    Button(
        onClick=onClick,
        enabled=enabled,
        modifier=Modifier.fillMaxWidth().height(58.dp),
        shape=RoundedCornerShape(18.dp),
        colors=ButtonDefaults.buttonColors(containerColor=RoloamInk)
    ) { Text(text.uppercase() + "  →", fontWeight=FontWeight.Bold, letterSpacing=1.2.sp) }
}

@Composable
fun HomeScreen(state: UiState, open:(Screen)->Unit, roll:()->Unit) = Page {
    JourneyHeader(state.preferences.transport, !state.rolling)
    DateStamp()
    Spacer(Modifier.height(20.dp))
    BigTitle(if (state.rolling) "Rolling..." else LocalDate.now().dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() } + ".")
    Spacer(Modifier.height(18.dp))
    if (state.rolling) {
        Text("Finding somewhere worth going.\nChecking the route, weather and places to stay.", lineHeight=24.sp)
        Spacer(Modifier.weight(1f))
        LinearProgressIndicator(Modifier.fillMaxWidth(), color=RoloamAccent)
        Spacer(Modifier.height(26.dp))
    } else {
        val durationText = when(state.preferences.duration){DurationChoice.ONE->"1 day";DurationChoice.TWO->"2 days";DurationChoice.THREE->"3 days";DurationChoice.AUTO->"a few days"}
        Text("Next free weekend detected.\nWeather decides the rhythm.\nYou could disappear for " + durationText + ".", lineHeight=24.sp)
        Spacer(Modifier.weight(1f))
        state.error?.let {
            Text(it, color=MaterialTheme.colorScheme.error, modifier=Modifier.padding(bottom=12.dp))
        }
        PrimaryButton("Roll a trip", onClick=roll)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            TinyChip("⌖  " + state.origin.label, Modifier.weight(1f)) { }
            TinyChip("⚙  Preferences", Modifier.weight(1f)) { open(Screen.PREFERENCES) }
        }
        if (state.origin.isFallback) {
            Text("Location permission off · using Heidelberg", fontSize=11.sp, color=RoloamMuted, modifier=Modifier.padding(top=10.dp))
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun TinyChip(text:String, modifier:Modifier=Modifier, onClick:()->Unit) {
    OutlinedButton(onClick=onClick, modifier=modifier.height(42.dp), shape=RoundedCornerShape(14.dp), contentPadding=PaddingValues(horizontal=10.dp)) {
        Text(text, fontSize=11.sp, maxLines=1, overflow=TextOverflow.Ellipsis)
    }
}

@Composable
fun PreferencesScreen(initial: TripPreferences, update:(TripPreferences)->Unit, back:()->Unit) = Page {
    var p by remember(initial) { mutableStateOf(initial) }
    JourneyHeader(p.transport)
    Text("‹  Trip preferences.", fontSize=30.sp, fontWeight=FontWeight.Black, modifier=Modifier.clickable{back()})
    Text("Only the things that actually change the trip.", color=RoloamMuted)
    Spacer(Modifier.height(24.dp))
    Choice("DURATION", listOf("1 day","2 days","3 days","Auto"), when(p.duration){DurationChoice.ONE->0;DurationChoice.TWO->1;DurationChoice.THREE->2;DurationChoice.AUTO->3}) {
        p=p.copy(duration=listOf(DurationChoice.ONE,DurationChoice.TWO,DurationChoice.THREE,DurationChoice.AUTO)[it])
    }
    Choice("TRANSPORT", listOf("Car","Train","Bike","Walk"), p.transport.ordinal) { p=p.copy(transport=TransportMode.entries[it]) }
    Choice("BUDGET", listOf("Cheap","Normal"), p.budget.ordinal) { p=p.copy(budget=Budget.entries[it]) }
    Choice("STAY", listOf("Camping","Any"), p.stay.ordinal) { p=p.copy(stay=StayPreference.entries[it]) }
    Choice("STYLE", listOf("Nature","City","Both"), p.style.ordinal) { p=p.copy(style=TripStyle.entries[it]) }
    Spacer(Modifier.weight(1f))
    PrimaryButton("Save") { update(p); back() }
}

@Composable
private fun Choice(label:String, values:List<String>, selected:Int, onSelect:(Int)->Unit) {
    Text(label, fontSize=11.sp, fontWeight=FontWeight.Bold, color=RoloamMuted, modifier=Modifier.padding(top=12.dp,bottom=7.dp))
    Row(Modifier.fillMaxWidth().height(42.dp).border(1.dp,RoloamMuted.copy(alpha=.25f),RoundedCornerShape(14.dp)).padding(3.dp)) {
        values.forEachIndexed { i, value ->
            Surface(
                modifier=Modifier.weight(1f).fillMaxHeight().clickable{onSelect(i)},
                color=if(i==selected) RoloamInk else androidx.compose.ui.graphics.Color.Transparent,
                shape=RoundedCornerShape(11.dp)
            ) {
                Box(contentAlignment=Alignment.Center) {
                    Text(value,fontSize=11.sp,color=if(i==selected) RoloamCream else MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }
}

@Composable
fun RevealScreen(state: UiState, accept:()->Unit, reroll:()->Unit, back:()->Unit) = Page {
    val trip=state.trip ?: return@Page
    JourneyHeader(state.preferences.transport)
    DateStamp()
    Spacer(Modifier.height(10.dp))
    BigTitle("Your trip.")
    Text("One result. No endless browsing.", color=RoloamMuted)
    Spacer(Modifier.height(34.dp))
    BigTitle(trip.destination.name + " — " + trip.days + if(trip.days==1) " day" else " days")
    Spacer(Modifier.height(12.dp))
    val h=trip.destination.travelMinutes/60
    val m=trip.destination.travelMinutes%60
    Text((if(h>0) h.toString()+"h " else "") + m + "m  ·  " + trip.destination.distanceKm.roundToInt() + " km  ·  " + (trip.stay?.let{if(it.category.contains("camp")) "camping found" else "stay found"} ?: "stay search"))
    Spacer(Modifier.weight(1f))
    CityGlyph(trip.destination.name)
    Spacer(Modifier.weight(1f))
    if(!trip.usedLiveData) Text("Live place data was limited; route generation used the offline destination fallback.", fontSize=11.sp, color=RoloamMuted, modifier=Modifier.padding(bottom=12.dp))
    PrimaryButton("Accept this trip", onClick=accept)
    Spacer(Modifier.height(10.dp))
    OutlinedButton(onClick=reroll, modifier=Modifier.fillMaxWidth().height(52.dp), shape=RoundedCornerShape(18.dp)) { Text("REROLL  ↻") }
    Text("‹ back", Modifier.padding(top=14.dp).clickable{back()}, color=RoloamMuted)
}

@Composable
private fun CityGlyph(seed:String) {
    val n = seed.hashCode()
    Canvas(Modifier.fillMaxWidth().height(180.dp)) {
        val base=size.height*.84f
        drawLine(RoloamInk.copy(alpha=.5f),Offset(0f,base),Offset(size.width,base),1.dp.toPx())
        val count=9
        repeat(count){i->
            val w=size.width/count*.62f
            val left=i*size.width/count + size.width/count*.18f
            val frac=((n shr (i%12)) and 7)/7f
            val h=35.dp.toPx()+frac*85.dp.toPx()
            drawRect(RoloamInk.copy(alpha=.8f),Offset(left,base-h),androidx.compose.ui.geometry.Size(w,h),style=Stroke(1.4.dp.toPx()))
            repeat(3){r-> drawCircle(RoloamInk.copy(alpha=.55f),1.2.dp.toPx(),Offset(left+w*.5f,base-h+10.dp.toPx()+r*11.dp.toPx())) }
        }
    }
}

@Composable
fun PlanScreen(state: UiState, open:(Screen)->Unit, select:(TripStop)->Unit, back:()->Unit) = Page {
    val trip=state.trip ?: return@Page
    JourneyHeader(state.preferences.transport, false)
    Row(verticalAlignment=Alignment.CenterVertically) {
        Text("‹",fontSize=28.sp,modifier=Modifier.clickable{back()}.padding(end=10.dp))
        BigTitle("Plan.")
    }
    Text("A compact " + trip.days + "-day plan for " + trip.destination.name + ".", color=RoloamMuted)
    Spacer(Modifier.height(18.dp))
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        trip.itinerary.forEachIndexed { dayIndex, day ->
            Text("DAY " + (dayIndex+1) + " · " + day.date.format(DateTimeFormatter.ofPattern("EEE dd MMM")).uppercase(),fontSize=12.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(vertical=10.dp))
            if(dayIndex==0) TimelineRow("08:00","Depart "+trip.originLabel,travelText(trip.destination.travelMinutes),false) {}
            day.stops.forEach { stop ->
                TimelineRow(stop.time,stop.place.name,"~ "+stop.durationMinutes+" min",true){select(stop)}
            }
            if(dayIndex < trip.days-1 && trip.stay!=null) TimelineRow("19:00","Sleep · "+trip.stay.name,trip.stay.category.replace('_',' '),false){open(Screen.STAY)}
            if(dayIndex==trip.days-1) TimelineRow("16:00","Return to "+trip.originLabel,travelText(trip.destination.travelMinutes),false){}
        }
    }
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        TinyChip("MAP",Modifier.weight(1f)){open(Screen.MAP)}
        TinyChip("STAY",Modifier.weight(1f)){open(Screen.STAY)}
        TinyChip("START",Modifier.weight(1f)){open(Screen.NOW)}
    }
}

private fun travelText(min:Int):String {
    val h=min/60; val m=min%60
    return (if(h>0) h.toString()+"h " else "")+m+"m"
}

@Composable
private fun TimelineRow(time:String,title:String,sub:String,clickable:Boolean,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().clickable(enabled=clickable,onClick=onClick).padding(vertical=8.dp)) {
        Text(time,fontSize=11.sp,color=RoloamMuted,modifier=Modifier.width(55.dp))
        Column(Modifier.padding(start=14.dp)) {
            Text(title,fontWeight=FontWeight.Bold,fontSize=14.sp)
            Text(sub,fontSize=11.sp,color=RoloamMuted)
        }
    }
}

@Composable
fun StayScreen(state: UiState, back:()->Unit) = Page {
    val trip=state.trip ?: return@Page
    JourneyHeader(state.preferences.transport,false)
    Text("‹",fontSize=28.sp,modifier=Modifier.clickable{back()})
    BigTitle("Tonight.")
    Text("Closest useful stays, camping first.",color=RoloamMuted)
    Spacer(Modifier.height(18.dp))
    val all=listOfNotNull(trip.stay)+trip.alternativeStays
    if(all.isEmpty()) {
        Spacer(Modifier.weight(1f))
        Text("No stay was returned by OpenStreetMap nearby.\nThe trip is still valid, but check accommodation before leaving.",lineHeight=24.sp)
        Spacer(Modifier.weight(1f))
    } else {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            all.forEachIndexed { i,s ->
                StayCard(s,i==0)
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun StayCard(stay: Stay,recommended:Boolean) {
    val ctx=LocalContext.current
    Surface(shape=RoundedCornerShape(18.dp),tonalElevation=1.dp,modifier=Modifier.fillMaxWidth().border(1.dp,RoloamMuted.copy(alpha=.18f),RoundedCornerShape(18.dp))) {
        Column(Modifier.padding(16.dp)) {
            if(recommended) Text("RECOMMENDED",fontSize=10.sp,color=RoloamAccent,fontWeight=FontWeight.Bold)
            Text(stay.name,fontSize=18.sp,fontWeight=FontWeight.Black)
            Text(stay.category.replace('_',' ')+" · "+String.format("%.1f",stay.distanceFromCenterKm)+" km from centre",fontSize=12.sp,color=RoloamMuted)
            stay.openingHours?.let{Text("Hours: "+it,fontSize=11.sp,color=RoloamMuted)}
            stay.website?.let { url ->
                OutlinedButton(onClick={runCatching{ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }},modifier=Modifier.padding(top=8.dp)) { Text("CHECK SITE →") }
            }
        }
    }
}

@Composable
fun PlaceScreen(state: UiState, back:()->Unit) = Page {
    val stop=state.selectedStop ?: return@Page
    JourneyHeader(state.preferences.transport,false)
    Text("‹",fontSize=28.sp,modifier=Modifier.clickable{back()})
    BigTitle(stop.place.name + ".")
    CityGlyph(stop.place.name)
    Text("BEST SLOT  ·  "+stop.time+"  ·  ~"+stop.durationMinutes+" MIN",fontSize=11.sp,fontWeight=FontWeight.Bold,color=RoloamMuted)
    Spacer(Modifier.height(20.dp))
    Text("Why now?",fontWeight=FontWeight.Black)
    Text(stop.whyNow,lineHeight=22.sp)
    stop.place.openingHours?.let{
        Spacer(Modifier.height(18.dp)); Text("OSM hours",fontWeight=FontWeight.Bold); Text(it,color=RoloamMuted)
    }
    Spacer(Modifier.weight(1f))
    val ctx=LocalContext.current
    PrimaryButton("Navigate") {
        val uri=Uri.parse("geo:${stop.place.point.lat},${stop.place.point.lon}?q=${stop.place.point.lat},${stop.place.point.lon}("+Uri.encode(stop.place.name)+")")
        runCatching{ctx.startActivity(Intent(Intent.ACTION_VIEW,uri))}
    }
}

@Composable
fun NowScreen(state: UiState, back:()->Unit) = Page {
    val trip=state.trip ?: return@Page
    val next=trip.allStops().firstOrNull()
    JourneyHeader(state.preferences.transport)
    Text("‹",fontSize=28.sp,modifier=Modifier.clickable{back()})
    BigTitle("Now.")
    Text("Only the next thing matters.",color=RoloamMuted)
    Spacer(Modifier.weight(1f))
    if(next!=null){
        Text("NEXT",fontSize=11.sp,fontWeight=FontWeight.Bold,color=RoloamMuted)
        BigTitle(next.place.name)
        Text(next.time+" · "+next.whyNow,color=RoloamMuted)
        Spacer(Modifier.height(20.dp))
        val ctx=LocalContext.current
        PrimaryButton("Go"){
            val uri=Uri.parse("geo:${next.place.point.lat},${next.place.point.lon}?q=${next.place.point.lat},${next.place.point.lon}("+Uri.encode(next.place.name)+")")
            runCatching{ctx.startActivity(Intent(Intent.ACTION_VIEW,uri))}
        }
    }
    Spacer(Modifier.weight(1f))
}
