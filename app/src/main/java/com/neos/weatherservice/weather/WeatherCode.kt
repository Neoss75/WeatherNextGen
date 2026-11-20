package com.neos.weatherservice.weather

/** Weather condition groups the app can describe and draw. */
enum class Condition {
    CLEAR, MAINLY_CLEAR, PARTLY_CLOUDY, OVERCAST, FOG,
    DRIZZLE, FREEZING_DRIZZLE, RAIN, FREEZING_RAIN, SNOW, SNOW_GRAINS,
    RAIN_SHOWERS, SNOW_SHOWERS, THUNDERSTORM, THUNDERSTORM_HAIL, UNKNOWN,
}

/**
 * Mapping of a WMO weather code (as returned by Open-Meteo) to what the iGO skin expects.
 *
 * The iGO skin was built for OpenWeatherMap, so [owmId] and [owmIcon] keep the OWM
 * condition ids and icon names ("01d", "10n", ...) that weather.txt has always contained.
 */
data class WeatherCode(val condition: Condition, val owmId: Int, val owmIcon: String) {

    /** Icon name with day/night suffix, e.g. "10d". */
    fun icon(isDay: Boolean): String = owmIcon + if (isDay) "d" else "n"

    companion object {
        private val table: Map<Int, WeatherCode> = buildMap {
            fun put(codes: IntArray, c: Condition, id: Int, icon: String) =
                codes.forEach { put(it, WeatherCode(c, id, icon)) }

            put(intArrayOf(0), Condition.CLEAR, 800, "01")
            put(intArrayOf(1), Condition.MAINLY_CLEAR, 801, "02")
            put(intArrayOf(2), Condition.PARTLY_CLOUDY, 802, "03")
            put(intArrayOf(3), Condition.OVERCAST, 804, "04")
            put(intArrayOf(45, 48), Condition.FOG, 741, "50")
            put(intArrayOf(51), Condition.DRIZZLE, 300, "09")
            put(intArrayOf(53), Condition.DRIZZLE, 301, "09")
            put(intArrayOf(55), Condition.DRIZZLE, 302, "09")
            put(intArrayOf(56, 57), Condition.FREEZING_DRIZZLE, 511, "13")
            put(intArrayOf(61), Condition.RAIN, 500, "10")
            put(intArrayOf(63), Condition.RAIN, 501, "10")
            put(intArrayOf(65), Condition.RAIN, 502, "10")
            put(intArrayOf(66, 67), Condition.FREEZING_RAIN, 511, "13")
            put(intArrayOf(71), Condition.SNOW, 600, "13")
            put(intArrayOf(73), Condition.SNOW, 601, "13")
            put(intArrayOf(75), Condition.SNOW, 602, "13")
            put(intArrayOf(77), Condition.SNOW_GRAINS, 600, "13")
            put(intArrayOf(80), Condition.RAIN_SHOWERS, 520, "09")
            put(intArrayOf(81), Condition.RAIN_SHOWERS, 521, "09")
            put(intArrayOf(82), Condition.RAIN_SHOWERS, 522, "09")
            put(intArrayOf(85), Condition.SNOW_SHOWERS, 620, "13")
            put(intArrayOf(86), Condition.SNOW_SHOWERS, 621, "13")
            put(intArrayOf(95), Condition.THUNDERSTORM, 211, "11")
            put(intArrayOf(96), Condition.THUNDERSTORM_HAIL, 201, "11")
            put(intArrayOf(99), Condition.THUNDERSTORM_HAIL, 202, "11")
        }

        private val unknown = WeatherCode(Condition.UNKNOWN, 804, "04")

        fun fromWmo(code: Int): WeatherCode = table[code] ?: unknown
    }
}
