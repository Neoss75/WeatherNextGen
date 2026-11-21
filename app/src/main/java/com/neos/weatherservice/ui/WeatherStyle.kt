package com.neos.weatherservice.ui

import com.neos.weatherservice.weather.Condition

/** What the animated weather scene draws. */
data class Scene(
    val celestial: Celestial,
    /** 0 — no clouds, 1 — a cloud in front of the sun or moon, 2 — overcast. */
    val clouds: Int,
    val precipitation: Precipitation = Precipitation.NONE,
    /** 0..1: drizzle is light, showers are heavy. */
    val intensity: Float = 1f,
    val lightning: Boolean = false,
    val fog: Boolean = false,
    /** Dark grey clouds instead of white ones. */
    val darkClouds: Boolean = false,
) {
    enum class Celestial { NONE, SUN, MOON }
    enum class Precipitation { NONE, RAIN, SNOW }
}

/** Background gradient of the main screen, top to bottom, as ARGB colors. */
data class Palette(val top: Int, val bottom: Int)

/** Maps weather to the look of the main screen. Pure Kotlin so it can be unit-tested. */
object WeatherStyle {

    /** Shown before the first weather update. */
    val idleScene = Scene(Scene.Celestial.SUN, clouds = 1)
    val idlePalette = Palette(0xFF1565C0.toInt(), 0xFF42A5F5.toInt())

    fun scene(condition: Condition, isDay: Boolean): Scene {
        val body = if (isDay) Scene.Celestial.SUN else Scene.Celestial.MOON
        return when (condition) {
            Condition.CLEAR -> Scene(body, clouds = 0)
            Condition.MAINLY_CLEAR, Condition.PARTLY_CLOUDY -> Scene(body, clouds = 1)
            Condition.OVERCAST, Condition.UNKNOWN -> Scene(Scene.Celestial.NONE, clouds = 2)
            Condition.FOG -> Scene(Scene.Celestial.NONE, clouds = 1, fog = true)
            Condition.DRIZZLE, Condition.FREEZING_DRIZZLE ->
                Scene(Scene.Celestial.NONE, 2, Scene.Precipitation.RAIN, intensity = 0.5f)
            Condition.RAIN, Condition.FREEZING_RAIN ->
                Scene(Scene.Celestial.NONE, 2, Scene.Precipitation.RAIN, intensity = 0.8f)
            Condition.RAIN_SHOWERS ->
                Scene(Scene.Celestial.NONE, 2, Scene.Precipitation.RAIN, intensity = 1f, darkClouds = true)
            Condition.SNOW_GRAINS ->
                Scene(Scene.Celestial.NONE, 2, Scene.Precipitation.SNOW, intensity = 0.5f)
            Condition.SNOW -> Scene(Scene.Celestial.NONE, 2, Scene.Precipitation.SNOW, intensity = 0.8f)
            Condition.SNOW_SHOWERS -> Scene(Scene.Celestial.NONE, 2, Scene.Precipitation.SNOW, intensity = 1f)
            Condition.THUNDERSTORM, Condition.THUNDERSTORM_HAIL -> Scene(
                Scene.Celestial.NONE, 2, Scene.Precipitation.RAIN,
                intensity = 1f, lightning = true, darkClouds = true,
            )
        }
    }

    fun palette(condition: Condition, isDay: Boolean): Palette = when (condition) {
        Condition.CLEAR ->
            if (isDay) Palette(0xFF2F80ED, 0xFF56CCF2) else Palette(0xFF141E30, 0xFF243B55)
        Condition.MAINLY_CLEAR, Condition.PARTLY_CLOUDY ->
            if (isDay) Palette(0xFF4A86C8, 0xFF8EC5FC) else Palette(0xFF1F2A44, 0xFF3A4A6B)
        Condition.OVERCAST, Condition.UNKNOWN ->
            if (isDay) Palette(0xFF5B6B7F, 0xFF97A6B7) else Palette(0xFF2B323C, 0xFF4A5563)
        Condition.FOG ->
            if (isDay) Palette(0xFF6F7E8E, 0xFFAAB5C1) else Palette(0xFF373F49, 0xFF58616C)
        Condition.DRIZZLE, Condition.FREEZING_DRIZZLE, Condition.RAIN, Condition.FREEZING_RAIN,
        Condition.RAIN_SHOWERS ->
            if (isDay) Palette(0xFF3E5269, 0xFF6F8BA4) else Palette(0xFF1C2633, 0xFF34465A)
        Condition.SNOW, Condition.SNOW_GRAINS, Condition.SNOW_SHOWERS ->
            if (isDay) Palette(0xFF5F7FA8, 0xFFA9BFDB) else Palette(0xFF2A3550, 0xFF56688A)
        Condition.THUNDERSTORM, Condition.THUNDERSTORM_HAIL ->
            if (isDay) Palette(0xFF2E3138, 0xFF50555E) else Palette(0xFF15161A, 0xFF2D3036)
    }

    /**
     * Index into an 8-point compass (N, NE, E, SE, S, SW, W, NW) for a meteorological
     * wind direction in degrees — the direction the wind blows from.
     */
    fun compassIndex(degrees: Int): Int = Math.floorMod(Math.round(degrees / 45.0).toInt(), 8)

    /** Rotation of an up-pointing arrow that shows where the wind blows to. */
    fun arrowRotation(degrees: Int): Float = Math.floorMod(degrees + 180, 360).toFloat()

    private fun Palette(top: Long, bottom: Long) = Palette(top.toInt(), bottom.toInt())
}
