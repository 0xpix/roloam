package com.roloam.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roloam.app.UiState
import com.roloam.app.model.*
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun WeatherScreen(state: UiState, back: () -> Unit) = Page {
    val trip = state.trip ?: return@Page
    RoloamSectionBar("Weather")
    Spacer(Modifier.height(12.dp))
    Text("‹", fontSize = 28.sp, modifier = Modifier.clickable { back() })
    BigTitle("Weather.")
    Text("Forecast for " + trip.destination.name + ".", color = RoloamMuted)

    Spacer(Modifier.height(18.dp))
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        trip.itinerary.forEachIndexed { index, day ->
            WeatherDayCard(day, index + 1, index == 0)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun WeatherDayCard(day: TripDay, dayNumber: Int, isFirst: Boolean) {
    val weather = day.weather
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, RoloamMuted.copy(alpha = .20f)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        "DAY " + dayNumber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoloamMuted,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        day.date.format(DateTimeFormatter.ofPattern("EEE · dd MMM")).uppercase(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (weather != null) {
                    val high = weather.maxC?.roundToInt()?.toString() ?: "—"
                    val low = weather.minC?.roundToInt()?.toString() ?: "—"
                    Text(high + "° / " + low + "°", fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }

            if (weather == null) {
                Spacer(Modifier.height(12.dp))
                Text("Forecast unavailable.", color = RoloamMuted, fontSize = 12.sp)
                return@Column
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = RoloamMuted.copy(alpha = .14f))
            Spacer(Modifier.height(12.dp))

            val rain = weather.precipitationProbability?.let { it.toString() + "% rain" } ?: "rain unknown"
            val sunrise = weather.sunrise?.substringAfter("T") ?: "—"
            val sunset = weather.sunset?.substringAfter("T") ?: "—"
            Text(
                rain + "  ·  sunrise " + sunrise + "  ·  sunset " + sunset,
                fontSize = 11.sp,
                color = RoloamMuted
            )

            if (weather.hours.isNotEmpty()) {
                val window = bestDepartureWindow(weather.hours)
                if (isFirst && window != null) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "BEST DEPARTURE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoloamAccent,
                        letterSpacing = 1.1.sp
                    )
                    Text(
                        window.start + "–" + window.end,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(window.reason, fontSize = 11.sp, color = RoloamMuted, lineHeight = 16.sp)
                }

                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    weather.hours
                        .filter { hour ->
                            hour.time.substringBefore(":").toIntOrNull() in listOf(8, 11, 14, 17, 20)
                        }
                        .take(5)
                        .forEach { hour ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(hour.time, fontSize = 10.sp, color = RoloamMuted)
                                Text(
                                    hour.temperatureC?.roundToInt()?.let { it.toString() + "°" } ?: "—",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    hour.precipitationProbability?.let { it.toString() + "%" } ?: "—",
                                    fontSize = 9.sp,
                                    color = RoloamMuted
                                )
                            }
                        }
                }
            }
        }
    }
}

private data class DepartureWindow(val start: String, val end: String, val reason: String)

private fun bestDepartureWindow(hours: List<WeatherHour>): DepartureWindow? {
    val morning = hours.filter { it.time.substringBefore(":").toIntOrNull() in 6..12 }
    if (morning.size < 2) return null

    val best = morning.zipWithNext().minByOrNull { pair ->
        val a = pair.first
        val b = pair.second
        val rain = ((a.precipitationProbability ?: 35) + (b.precipitationProbability ?: 35)) / 2.0
        val wind = ((a.windKph ?: 10.0) + (b.windKph ?: 10.0)) / 2.0
        val temps = listOfNotNull(a.temperatureC, b.temperatureC)
        val coldPenalty = if (temps.isEmpty()) 0.0 else {
            val avg = temps.average()
            if (avg < 5) (5 - avg) * 2 else 0.0
        }
        rain + wind * .65 + coldPenalty
    } ?: return null

    val rainValues = listOfNotNull(best.first.precipitationProbability, best.second.precipitationProbability)
    val windValues = listOfNotNull(best.first.windKph, best.second.windKph)
    val rain = if (rainValues.isEmpty()) null else rainValues.average().roundToInt()
    val wind = if (windValues.isEmpty()) null else windValues.average().roundToInt()

    val reason = buildString {
        append("Lowest weather friction in the morning")
        rain?.let { append(" · ~").append(it).append("% rain") }
        wind?.let { append(" · ~").append(it).append(" km/h wind") }
        append(".")
    }
    return DepartureWindow(best.first.time, best.second.time, reason)
}

@Composable
fun PackingScreen(state: UiState, back: () -> Unit) = Page {
    val trip = state.trip ?: return@Page
    RoloamSectionBar("Packing")
    Spacer(Modifier.height(12.dp))
    Text("‹", fontSize = 28.sp, modifier = Modifier.clickable { back() })
    BigTitle("Pack.")
    Text("Only what this trip actually needs.", color = RoloamMuted)

    val items = remember(trip, state.preferences) { buildPackingList(trip, state.preferences) }
    val checked = remember { mutableStateMapOf<String, Boolean>() }

    Spacer(Modifier.height(18.dp))
    val done = items.count { checked[it.label] == true }
    Text(
        done.toString() + " / " + items.size + " packed",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = if (done == items.size && items.isNotEmpty()) RoloamAccent else RoloamMuted
    )
    Spacer(Modifier.height(10.dp))

    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        items.groupBy { it.group }.forEach { entry ->
            Text(
                entry.key.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = RoloamMuted,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(top = 12.dp, bottom = 5.dp)
            )
            entry.value.forEach { item ->
                PackingRow(
                    item = item,
                    checked = checked[item.label] == true,
                    onToggle = { checked[item.label] = !(checked[item.label] == true) }
                )
            }
        }
    }
}

private data class PackingItem(val group: String, val label: String, val reason: String? = null)

@Composable
private fun PackingRow(item: PackingItem, checked: Boolean, onToggle: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onToggle() },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, RoloamMuted.copy(alpha = .17f)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Column(Modifier.padding(start = 4.dp)) {
                Text(item.label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                item.reason?.let {
                    Text(it, fontSize = 10.sp, color = RoloamMuted, lineHeight = 14.sp)
                }
            }
        }
    }
}

private fun buildPackingList(trip: TripPlan, prefs: TripPreferences): List<PackingItem> {
    val result = mutableListOf(
        PackingItem("Essentials", "Phone + charger"),
        PackingItem("Essentials", "Wallet / ID"),
        PackingItem("Essentials", "Water bottle"),
        PackingItem("Essentials", "Small first-aid kit")
    )

    val forecast = trip.itinerary.mapNotNull { it.weather }
    val maxRain = forecast.mapNotNull { it.precipitationProbability }.maxOrNull() ?: 0
    val minTemp = forecast.mapNotNull { it.minC }.minOrNull()
    val maxTemp = forecast.mapNotNull { it.maxC }.maxOrNull()

    if (maxRain >= 35) {
        result += PackingItem("Weather", "Light rain shell", maxRain.toString() + "% rain risk")
    }
    if (minTemp != null && minTemp < 10) {
        result += PackingItem("Weather", "Warm layer", "Forecast low around " + minTemp.roundToInt() + "°C")
    }
    if (maxTemp != null && maxTemp >= 20) {
        result += PackingItem("Weather", "Sunscreen", "Forecast high around " + maxTemp.roundToInt() + "°C")
    }

    when (prefs.transport) {
        TransportMode.BIKE -> {
            result += PackingItem("Transport", "Helmet")
            result += PackingItem("Transport", "Bike lights")
            result += PackingItem("Transport", "Tube / repair kit")
        }
        TransportMode.WALK -> {
            result += PackingItem("Transport", "Comfortable walking shoes")
            result += PackingItem("Transport", "Compact daypack")
        }
        TransportMode.TRAIN -> {
            result += PackingItem("Transport", "Offline ticket / booking")
            result += PackingItem("Transport", "Power bank")
        }
        TransportMode.CAR -> {
            result += PackingItem("Transport", "Offline map")
            result += PackingItem("Transport", "Car charger")
        }
    }

    if (trip.days > 1) {
        result += PackingItem("Overnight", "Change of clothes × " + trip.days)
        result += PackingItem("Overnight", "Toiletries")
    }

    if (prefs.stay == StayPreference.CAMPING && trip.days > 1) {
        result += listOf(
            PackingItem("Camping", "Tent"),
            PackingItem("Camping", "Sleeping bag"),
            PackingItem("Camping", "Sleeping mat"),
            PackingItem("Camping", "Headlamp"),
            PackingItem("Camping", "Camp towel")
        )
    }
    return result.distinctBy { it.label }
}
