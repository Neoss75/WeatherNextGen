package com.neos.weatherservice.weather

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

enum class Units { METRIC, IMPERIAL }

data class CurrentWeather(
    val temperature: Double,
    val humidity: Int,
    /** Surface pressure in hPa. */
    val pressureHpa: Double,
    val windSpeed: Double,
    val windDirection: Int,
    val code: WeatherCode,
    val isDay: Boolean,
    val units: Units,
) {
    val icon: String get() = code.icon(isDay)
    val pressureMmHg: Int get() = Math.round(pressureHpa * 0.750062).toInt()
}

/** Client for the free Open-Meteo forecast API (https://open-meteo.com), no API key needed. */
object OpenMeteo {

    private const val BASE = "https://api.open-meteo.com/v1/forecast"
    private const val TIMEOUT_MS = 15_000

    fun buildUrl(lat: Double, lon: Double, units: Units): String {
        val (temp, wind) = when (units) {
            Units.METRIC -> "celsius" to "ms"
            Units.IMPERIAL -> "fahrenheit" to "mph"
        }
        return String.format(
            Locale.US,
            "%s?latitude=%.5f&longitude=%.5f" +
                "&current=temperature_2m,relative_humidity_2m,surface_pressure," +
                "wind_speed_10m,wind_direction_10m,weather_code,is_day" +
                "&temperature_unit=%s&wind_speed_unit=%s",
            BASE, lat, lon, temp, wind,
        )
    }

    fun parse(json: String, units: Units): CurrentWeather {
        val current = JSONObject(json).getJSONObject("current")
        return CurrentWeather(
            temperature = current.getDouble("temperature_2m"),
            humidity = current.optInt("relative_humidity_2m"),
            pressureHpa = current.optDouble("surface_pressure", 0.0),
            windSpeed = current.optDouble("wind_speed_10m", 0.0),
            windDirection = current.optInt("wind_direction_10m"),
            code = WeatherCode.fromWmo(current.optInt("weather_code", -1)),
            isDay = current.optInt("is_day", 1) == 1,
            units = units,
        )
    }

    /** Blocking network call; run it off the main thread. */
    @Throws(IOException::class)
    fun fetch(lat: Double, lon: Double, units: Units): CurrentWeather {
        val conn = URL(buildUrl(lat, lon, units)).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            val status = conn.responseCode
            if (status != HttpURLConnection.HTTP_OK) throw IOException("HTTP $status")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            return try {
                parse(body, units)
            } catch (e: org.json.JSONException) {
                throw IOException("Bad response", e)
            }
        } finally {
            conn.disconnect()
        }
    }
}
