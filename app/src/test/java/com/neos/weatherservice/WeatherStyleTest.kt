package com.neos.weatherservice

import com.neos.weatherservice.ui.Scene
import com.neos.weatherservice.ui.WeatherStyle
import com.neos.weatherservice.weather.Condition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherStyleTest {

    @Test
    fun everyConditionHasSceneAndPalette() {
        for (c in Condition.entries) for (day in listOf(true, false)) {
            WeatherStyle.scene(c, day)
            val p = WeatherStyle.palette(c, day)
            assertTrue("$c opaque", (p.top ushr 24) == 0xFF && (p.bottom ushr 24) == 0xFF)
        }
    }

    @Test
    fun sunByDayMoonByNight() {
        assertEquals(Scene.Celestial.SUN, WeatherStyle.scene(Condition.CLEAR, true).celestial)
        assertEquals(Scene.Celestial.MOON, WeatherStyle.scene(Condition.CLEAR, false).celestial)
        assertEquals(0, WeatherStyle.scene(Condition.CLEAR, true).clouds)
        assertEquals(1, WeatherStyle.scene(Condition.PARTLY_CLOUDY, false).clouds)
        assertNotEquals(WeatherStyle.palette(Condition.CLEAR, true), WeatherStyle.palette(Condition.CLEAR, false))
    }

    @Test
    fun precipitation() {
        assertEquals(Scene.Precipitation.RAIN, WeatherStyle.scene(Condition.DRIZZLE, true).precipitation)
        assertEquals(Scene.Precipitation.SNOW, WeatherStyle.scene(Condition.SNOW_SHOWERS, true).precipitation)
        assertTrue(WeatherStyle.scene(Condition.THUNDERSTORM_HAIL, false).lightning)
        assertTrue(WeatherStyle.scene(Condition.FOG, true).fog)
    }

    @Test
    fun compass() {
        assertEquals(0, WeatherStyle.compassIndex(0))
        assertEquals(0, WeatherStyle.compassIndex(22))
        assertEquals(1, WeatherStyle.compassIndex(23))
        assertEquals(2, WeatherStyle.compassIndex(90))
        assertEquals(4, WeatherStyle.compassIndex(180))
        assertEquals(7, WeatherStyle.compassIndex(315))
        assertEquals(0, WeatherStyle.compassIndex(350))
        assertEquals(0, WeatherStyle.compassIndex(360))
    }

    @Test
    fun arrowShowsWhereWindBlows() {
        assertEquals(180f, WeatherStyle.arrowRotation(0)) // north wind blows south
        assertEquals(90f, WeatherStyle.arrowRotation(270)) // west wind blows east
        assertEquals(0f, WeatherStyle.arrowRotation(180))
    }
}
