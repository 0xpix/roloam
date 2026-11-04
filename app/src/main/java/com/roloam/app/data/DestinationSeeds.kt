package com.roloam.app.data

import com.roloam.app.model.GeoPoint

/**
 * Small, offline discovery index. These are real city-centre coordinates, not
 * verified attractions or journeys. The trip itself still requires live data.
 * Using it avoids a large Overpass search on every roll near these places.
 */
object DestinationSeeds {
    private fun city(name: String, lat: Double, lon: Double, population: Long? = null) =
        CityRaw(name, "Germany", GeoPoint(lat, lon), population)

    private val cities = listOf(
        city("Heidelberg", 49.3988, 8.6724, 162_000),
        city("Mannheim", 49.4875, 8.4660, 315_000),
        city("Karlsruhe", 49.0069, 8.4037, 308_000),
        city("Stuttgart", 48.7758, 9.1829, 635_000),
        city("Freiburg", 47.9990, 7.8421, 236_000),
        city("Tübingen", 48.5216, 9.0576, 92_000),
        city("Reutlingen", 48.4914, 9.2043, 116_000),
        city("Esslingen", 48.7407, 9.3107, 94_000),
        city("Ludwigsburg", 48.8973, 9.1916, 94_000),
        city("Pforzheim", 48.8913, 8.7047, 127_000),
        city("Baden-Baden", 48.7606, 8.2398, 56_000),
        city("Offenburg", 48.4735, 7.9445, 61_000),
        city("Bretten", 49.0369, 8.7075, 29_000),
        city("Bruchsal", 49.1243, 8.5980, 46_000),
        city("Speyer", 49.3173, 8.4412, 51_000),
        city("Worms", 49.6341, 8.3507, 84_000),
        city("Mainz", 49.9929, 8.2473, 220_000),
        city("Frankfurt", 50.1109, 8.6821, 775_000),
        city("Wiesbaden", 50.0782, 8.2398, 283_000),
        city("Darmstadt", 49.8728, 8.6512, 162_000),
        city("Aschaffenburg", 49.9767, 9.1467, 71_000),
        city("Würzburg", 49.7913, 9.9534, 130_000),
        city("Rothenburg ob der Tauber", 49.3789, 10.1870, 11_000),
        city("Heilbronn", 49.1427, 9.2109, 128_000),
        city("Schwäbisch Hall", 49.1122, 9.7378, 41_000),
        city("Ulm", 48.4011, 9.9876, 130_000),
        city("Aalen", 48.8377, 10.0933, 68_000),
        city("Göppingen", 48.7029, 9.6528, 59_000),
        city("Nuremberg", 49.4521, 11.0767, 526_000),
        city("Bamberg", 49.8988, 10.9028, 79_000),
        city("Regensburg", 49.0134, 12.1016, 157_000),
        city("Augsburg", 48.3705, 10.8978, 303_000),
        city("Munich", 48.1374, 11.5755, 1_490_000),
        city("Kempten", 47.7267, 10.3139, 70_000),
        city("Friedrichshafen", 47.6519, 9.4780, 62_000),
        city("Konstanz", 47.6779, 9.1732, 85_000),
        city("Lindau", 47.5461, 9.6843, 25_000),
        city("Ravensburg", 47.7819, 9.6106, 51_000),
        city("Trier", 49.7499, 6.6371, 112_000),
        city("Koblenz", 50.3569, 7.5889, 115_000),
        city("Bonn", 50.7374, 7.0982, 338_000),
        city("Cologne", 50.9375, 6.9603, 1_087_000),
        city("Düsseldorf", 51.2277, 6.7735, 629_000),
        city("Dortmund", 51.5136, 7.4653, 593_000),
        city("Essen", 51.4566, 7.0123, 584_000),
        city("Aachen", 50.7753, 6.0839, 252_000),
        city("Kassel", 51.3127, 9.4797, 204_000),
        city("Fulda", 50.5558, 9.6808, 69_000),
        city("Marburg", 50.8022, 8.7668, 77_000),
        city("Gießen", 50.5841, 8.6784, 94_000),
        city("Göttingen", 51.5413, 9.9158, 120_000),
        city("Erfurt", 50.9848, 11.0299, 214_000),
        city("Weimar", 50.9795, 11.3235, 65_000),
        city("Jena", 50.9271, 11.5892, 111_000),
        city("Leipzig", 51.3397, 12.3731, 616_000),
        city("Dresden", 51.0504, 13.7373, 563_000),
        city("Hannover", 52.3759, 9.7320, 545_000),
        city("Braunschweig", 52.2689, 10.5268, 252_000),
        city("Hamburg", 53.5511, 9.9937, 1_900_000),
        city("Bremen", 53.0793, 8.8017, 569_000),
        city("Berlin", 52.5200, 13.4050, 3_780_000),
        city("Potsdam", 52.3906, 13.0645, 187_000),
        city("Rostock", 54.0924, 12.0991, 209_000),
        CityRaw("Strasbourg", "France", GeoPoint(48.5734, 7.7521), 291_000),
        CityRaw("Colmar", "France", GeoPoint(48.0794, 7.3585), 67_000),
        CityRaw("Mulhouse", "France", GeoPoint(47.7508, 7.3359), 108_000),
        CityRaw("Basel", "Switzerland", GeoPoint(47.5596, 7.5886), 177_000),
        CityRaw("Zurich", "Switzerland", GeoPoint(47.3769, 8.5417), 430_000),
        CityRaw("Lucerne", "Switzerland", GeoPoint(47.0502, 8.3093), 82_000),
        CityRaw("Luxembourg", "Luxembourg", GeoPoint(49.6116, 6.1319), 136_000)
    )

    fun nearby(origin: GeoPoint, radiusKm: Int): List<CityRaw> =
        cities.filter { haversine(origin, it.point) <= radiusKm.toDouble() }
}
