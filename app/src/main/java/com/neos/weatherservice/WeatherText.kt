package com.neos.weatherservice

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.util.Log
import com.neos.weatherservice.ui.WeatherStyle
import com.neos.weatherservice.weather.Condition
import com.neos.weatherservice.weather.CurrentWeather
import com.neos.weatherservice.weather.Units
import java.io.IOException
import java.util.Locale

/** Android-side helpers that turn weather data into text and pictures. */
object WeatherText {

    fun description(context: Context, condition: Condition): String = context.getString(
        when (condition) {
            Condition.CLEAR -> R.string.cond_clear
            Condition.MAINLY_CLEAR -> R.string.cond_mainly_clear
            Condition.PARTLY_CLOUDY -> R.string.cond_partly_cloudy
            Condition.OVERCAST -> R.string.cond_overcast
            Condition.FOG -> R.string.cond_fog
            Condition.DRIZZLE -> R.string.cond_drizzle
            Condition.FREEZING_DRIZZLE -> R.string.cond_freezing_drizzle
            Condition.RAIN -> R.string.cond_rain
            Condition.FREEZING_RAIN -> R.string.cond_freezing_rain
            Condition.SNOW -> R.string.cond_snow
            Condition.SNOW_GRAINS -> R.string.cond_snow_grains
            Condition.RAIN_SHOWERS -> R.string.cond_rain_showers
            Condition.SNOW_SHOWERS -> R.string.cond_snow_showers
            Condition.THUNDERSTORM -> R.string.cond_thunderstorm
            Condition.THUNDERSTORM_HAIL -> R.string.cond_thunderstorm_hail
            Condition.UNKNOWN -> R.string.cond_unknown
        }
    )

    fun windSpeed(context: Context, w: CurrentWeather): String = context.getString(
        if (w.units == Units.METRIC) R.string.value_wind_ms else R.string.value_wind_mph,
        w.windSpeed,
    )

    fun windDirection(context: Context, degrees: Int): String {
        val points = context.resources.getStringArray(R.array.compass_points)
        return context.getString(R.string.wind_direction, points[WeatherStyle.compassIndex(degrees)], degrees)
    }

    fun icon(context: Context, w: CurrentWeather): Bitmap? = try {
        context.assets.open("icon_owm/${w.icon}.png").use(BitmapFactory::decodeStream)
    } catch (e: IOException) {
        null
    }

    /**
     * "City, CC" for the coordinates, or "" when the device has no geocoder
     * (common on car head units without Google services). Blocking call.
     */
    @Suppress("DEPRECATION") // the async variant needs API 33
    fun place(context: Context, lat: Double, lon: Double): String {
        if (!Geocoder.isPresent()) return ""
        return try {
            val a = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lon, 1)?.firstOrNull()
                ?: return ""
            listOfNotNull(a.locality ?: a.subAdminArea ?: a.adminArea, a.countryCode)
                .joinToString(", ")
        } catch (e: IOException) {
            Log.w("WeatherText", "Geocoder failed", e)
            ""
        } catch (e: IllegalArgumentException) {
            ""
        }
    }
}
