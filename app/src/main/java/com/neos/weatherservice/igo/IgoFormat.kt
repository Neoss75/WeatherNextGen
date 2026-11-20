package com.neos.weatherservice.igo

import com.neos.weatherservice.weather.CurrentWeather
import com.neos.weatherservice.weather.Units
import kotlin.math.roundToInt

/** Text formats of the files exchanged with the iGO skin (all files are UTF-16). */
object IgoFormat {

    const val WEATHER_FILE = "weather.txt"
    const val DESTINATION_WEATHER_FILE = "weather_fin.txt"
    const val DESTINATION_COORDS_FILE = "coordinates_fin.txt"

    fun temperature(w: CurrentWeather): String =
        w.temperature.roundToInt().toString() + if (w.units == Units.METRIC) "°C" else "°F"

    /** weather.txt: `temperature;icon.svg;description;owmId`, e.g. `-3°C;13n.svg;Snow;601`. */
    fun currentLine(w: CurrentWeather, description: String): String =
        listOf(temperature(w), "${w.icon}.svg", clean(description), w.code.owmId.toString())
            .joinToString(";")

    /** weather_fin.txt: `temperature;icon.svg;place`. */
    fun destinationLine(w: CurrentWeather, place: String): String =
        listOf(temperature(w), "${w.icon}.svg", clean(place)).joinToString(";")

    private val number = Regex("""[-+]?\d+(?:\.\d+)?""")

    /**
     * Parses coordinates_fin.txt written by the skin: latitude and longitude separated by ';'.
     * Returns null when no destination is set ("0;0", empty or unreadable).
     */
    fun parseCoordinates(text: String): Pair<Double, Double>? {
        val parts = text.trim().split(';')
        if (parts.size < 2) return null
        val lat = number.find(parts[0])?.value?.toDoubleOrNull() ?: return null
        val lon = number.find(parts[1])?.value?.toDoubleOrNull() ?: return null
        if (lat == 0.0 && lon == 0.0) return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return lat to lon
    }

    /** The skin splits on ';', so it must not appear inside a field. */
    private fun clean(s: String) = s.replace(';', ',').replace('\n', ' ').trim()
}
