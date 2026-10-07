package com.roloam.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

enum class OpeningState { OPEN, CLOSED, UNKNOWN }

/**
 * Small, deliberately conservative parser for the common OpenStreetMap opening_hours forms
 * Roloam sees for museums, attractions and parks.
 *
 * Complex expressions remain UNKNOWN rather than being guessed.
 */
fun openingState(
    openingHours: String?,
    date: LocalDate,
    time: LocalTime
): OpeningState {
    val raw = openingHours?.trim().orEmpty()
    if (raw.isBlank()) return OpeningState.UNKNOWN

    if (raw.equals("24/7", ignoreCase = true)) return OpeningState.OPEN

    val day = dayCode(date.dayOfWeek)
    var recognizedForDay = false
    var anyOpen = false

    for (segmentRaw in raw.split(";")) {
        val segment = segmentRaw.trim()
        if (segment.isBlank()) continue

        if (segment.contains("sunrise", ignoreCase = true) ||
            segment.contains("sunset", ignoreCase = true) ||
            segment.contains("PH", ignoreCase = true)
        ) {
            continue
        }

        val firstSpace = segment.indexOf(' ')
        val firstToken = if (firstSpace >= 0) segment.substring(0, firstSpace) else segment
        val hasDaySpec = firstToken.any { it.isLetter() } &&
            Regex("""(?i)(Mo|Tu|We|Th|Fr|Sa|Su)""").containsMatchIn(firstToken)

        val appliesToday = if (hasDaySpec) {
            daySpecContains(firstToken, day)
        } else {
            true
        }

        if (!appliesToday) continue

        if (segment.contains(Regex("""(?i)off|closed"""))) {
            recognizedForDay = true
            continue
        }

        val timePart = if (hasDaySpec && firstSpace >= 0) {
            segment.substring(firstSpace + 1)
        } else {
            segment
        }

        val ranges = Regex("""(d{1,2}:d{2})s*-s*(d{1,2}:d{2})""")
            .findAll(timePart)
            .mapNotNull { match ->
                val start = parseTime(match.groupValues[1])
                val end = parseTime(match.groupValues[2])
                if (start != null && end != null) start to end else null
            }
            .toList()

        if (ranges.isEmpty()) continue

        recognizedForDay = true
        if (ranges.any { (start, end) -> within(time, start, end) }) {
            anyOpen = true
        }
    }

    return when {
        anyOpen -> OpeningState.OPEN
        recognizedForDay -> OpeningState.CLOSED
        else -> OpeningState.UNKNOWN
    }
}

private fun within(time: LocalTime, start: LocalTime, end: LocalTime): Boolean {
    return if (end >= start) {
        time >= start && time < end
    } else {
        // Overnight window, e.g. 20:00-02:00.
        time >= start || time < end
    }
}

private fun parseTime(value: String): LocalTime? {
    val pieces = value.split(":")
    if (pieces.size != 2) return null
    val hour = pieces[0].toIntOrNull() ?: return null
    val minute = pieces[1].toIntOrNull() ?: return null
    if (hour !in 0..24 || minute !in 0..59) return null

    // OSM sometimes uses 24:00 as the end of a range.
    if (hour == 24) return LocalTime.MAX
    return LocalTime.of(hour, minute)
}

private fun daySpecContains(spec: String, target: String): Boolean {
    return spec.split(",").any { token ->
        val clean = token.trim()
        if ("-" in clean) {
            val parts = clean.split("-", limit = 2)
            val start = dayIndex(parts.getOrNull(0))
            val end = dayIndex(parts.getOrNull(1))
            val wanted = dayIndex(target)
            if (start == null || end == null || wanted == null) {
                false
            } else if (start <= end) {
                wanted in start..end
            } else {
                wanted >= start || wanted <= end
            }
        } else {
            clean.equals(target, ignoreCase = true)
        }
    }
}

private fun dayIndex(value: String?): Int? = when (value?.take(2)?.lowercase()) {
    "mo" -> 0
    "tu" -> 1
    "we" -> 2
    "th" -> 3
    "fr" -> 4
    "sa" -> 5
    "su" -> 6
    else -> null
}

private fun dayCode(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "Mo"
    DayOfWeek.TUESDAY -> "Tu"
    DayOfWeek.WEDNESDAY -> "We"
    DayOfWeek.THURSDAY -> "Th"
    DayOfWeek.FRIDAY -> "Fr"
    DayOfWeek.SATURDAY -> "Sa"
    DayOfWeek.SUNDAY -> "Su"
}
