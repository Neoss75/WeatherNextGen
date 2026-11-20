package com.neos.weatherservice

import com.neos.weatherservice.igo.IgoFormat
import com.neos.weatherservice.weather.Condition
import com.neos.weatherservice.weather.OpenMeteo
import com.neos.weatherservice.weather.Units
import com.neos.weatherservice.weather.WeatherCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherLogicTest {

    private val sample = """
        {"latitude":55.75,"longitude":37.62,
         "current":{"time":"2026-11-20T18:00","interval":900,"temperature_2m":-2.6,
           "relative_humidity_2m":87,"surface_pressure":1001.3,"wind_speed_10m":4.2,
           "wind_direction_10m":250,"weather_code":73,"is_day":0}}
    """.trimIndent()

    @Test fun `parses Open-Meteo current weather`() {
        val w = OpenMeteo.parse(sample, Units.METRIC)
        assertEquals(-2.6, w.temperature, 0.001)
        assertEquals(87, w.humidity)
        assertEquals(751, w.pressureMmHg)
        assertEquals(Condition.SNOW, w.code.condition)
        assertEquals("13n", w.icon)
    }

    @Test fun `weather txt line keeps the old skin format`() {
        val w = OpenMeteo.parse(sample, Units.METRIC)
        assertEquals("-3°C;13n.svg;Snow;601", IgoFormat.currentLine(w, "Snow"))
    }

    @Test fun `destination line and separator escaping`() {
        val w = OpenMeteo.parse(sample, Units.IMPERIAL)
        assertEquals("-3°F;13n.svg;Moscow, RU", IgoFormat.destinationLine(w, "Moscow; RU"))
    }

    @Test fun `WMO codes map to OWM ids and icons`() {
        assertEquals(800, WeatherCode.fromWmo(0).owmId)
        assertEquals("01d", WeatherCode.fromWmo(0).icon(true))
        assertEquals("10n", WeatherCode.fromWmo(63).icon(false))
        assertEquals(211, WeatherCode.fromWmo(95).owmId)
        assertEquals(Condition.UNKNOWN, WeatherCode.fromWmo(42).condition)
    }

    @Test fun `url uses dot decimals whatever the locale`() {
        val default = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale("ru", "RU"))
            val url = OpenMeteo.buildUrl(55.7558, -37.6173, Units.IMPERIAL)
            assertTrue(url, url.contains("latitude=55.75580&longitude=-37.61730"))
            assertTrue(url.startsWith("https://"))
            assertTrue(url.contains("temperature_unit=fahrenheit&wind_speed_unit=mph"))
        } finally {
            java.util.Locale.setDefault(default)
        }
    }

    @Test fun `parses destination coordinates`() {
        assertEquals(55.7558 to 37.6173, IgoFormat.parseCoordinates("55.7558;37.6173"))
        // Signs are kept (the old code cut off the first character).
        assertEquals(-33.8688 to -151.2093, IgoFormat.parseCoordinates(" -33.8688;-151.2093\r\n"))
        assertEquals(48.85 to 2.35, IgoFormat.parseCoordinates("+48.85;+2.35"))
        assertNull(IgoFormat.parseCoordinates("0;0"))
        assertNull(IgoFormat.parseCoordinates(""))
        assertNull(IgoFormat.parseCoordinates("garbage"))
        assertNull(IgoFormat.parseCoordinates("123;456"))
    }
}
