package com.roloam.app.data

/** A POI is useful for a trip only when its OSM tags identify a visitor destination. */
object AttractionQuality {
    private val tourism = setOf("attraction", "museum", "gallery", "viewpoint", "zoo", "theme_park")
    private val historic = setOf("castle", "fort", "ruins", "archaeological_site", "city_gate", "citywalls", "palace")
    private val leisure = setOf("park", "garden", "nature_reserve")
    private val nature = setOf("peak", "waterfall", "beach", "cave_entrance")
    private val exclusions = setOf("memorial", "tomb", "grave", "milestone", "wayside_cross", "wayside_shrine")

    fun isUseful(tags: Map<String, String>, name: String): Boolean {
        if (name.isBlank() || name.length < 3) return false
        if (tags["historic"] in exclusions || tags["tourism"] in exclusions) return false
        if (tags.containsKey("memorial") || tags.containsKey("cemetery") || tags["landuse"] == "cemetery")
            return false
        if (name.contains(" geb. ", ignoreCase = true) ||
            name.contains("gravestone", ignoreCase = true) ||
            name.contains("grabstein", ignoreCase = true)) return false
        return tags["tourism"] in tourism || tags["historic"] in historic ||
            tags["leisure"] in leisure || tags["natural"] in nature
    }
}
