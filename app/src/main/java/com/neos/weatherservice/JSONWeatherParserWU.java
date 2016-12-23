package com.neos.weatherservice;

import com.neos.weatherservice.model.Location;
import com.neos.weatherservice.model.Weather;

import org.json.JSONException;
import org.json.JSONObject;


class JSONWeatherParserWU {

    static Weather getWeather(String data) throws JSONException {
        Weather weather = new Weather();

        // We create out JSONObject from the data

        JSONObject jObj = new JSONObject(data);
        JSONObject curObs = jObj.getJSONObject("current_observation");



        // We start extracting the info
        Location loc = new Location();

        JSONObject sysObj = curObs.getJSONObject("display_location");
        try {

            loc.setCountry(sysObj.getString("country_iso3166"));
        } catch (JSONException e) {
            loc.setCountry("");
        }
        try {
            loc.setCity(sysObj.getString("city"));
        } catch (JSONException e) {
            loc.setCity("");
        }
        weather.location = loc;

        // We get weather info (This is an array)
        try {
            weather.currentCondition.setWeatherId(curObs.getInt("id"));
        } catch (JSONException e) {
            weather.currentCondition.setWeatherId(800);
        }
        try {
            weather.currentCondition.setDescr(curObs.getString("weather"));
        } catch (JSONException e) {
            weather.currentCondition.setDescr("");
        }
        try {
            weather.currentCondition.setIcon(curObs.getString("icon"));
        } catch (JSONException e) {
            weather.currentCondition.setIcon("");
        }
        try {
            weather.currentCondition.setIconUrl(curObs.getString("icon_url"));
        } catch (JSONException e) {
            weather.currentCondition.setIconUrl("");
        }
        try {
            weather.currentCondition.setHumidity(curObs.getString("relative_humidity"));
        } catch (JSONException e) {
            weather.currentCondition.setHumidity("");
        }
        try {
            weather.currentCondition.setPressure(curObs.getInt("pressure_mb"));
        } catch (JSONException e) {
            weather.currentCondition.setPressure(0);
        }
        try {
            weather.temperature.setTemp(curObs.getInt("temp_c"));
        } catch (JSONException e) {
            weather.temperature.setTemp(0);
        }
        try {
            weather.temperature.setTempF(curObs.getInt("temp_f"));
        } catch (JSONException e) {
            weather.temperature.setTempF(0);
        }

        // Wind
        try {
            weather.wind.setSpeed(curObs.getInt("wind_mph"));
        } catch (JSONException e) {
            weather.wind.setSpeed(0);
        }
        try {
            weather.wind.setDeg(curObs.getInt("wind_degrees"));
        } catch (JSONException e) {
            weather.wind.setDeg(0);
        }

        return weather;
    }

}
