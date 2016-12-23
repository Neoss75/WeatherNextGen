package com.neos.weatherservice;

import com.neos.weatherservice.model.Location;
import com.neos.weatherservice.model.Weather;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 */

class JSONWeatherParser {

	static Weather getWeather(String data) throws JSONException  {
		Weather weather = new Weather();

		// We create out JSONObject from the data
		JSONObject jObj = new JSONObject(data);
		
		// We start extracting the info
		Location loc = new Location();
		
//		JSONObject coordObj = getObject("coord", jObj);
//		loc.setLatitude(getFloat("lat", coordObj));
//		loc.setLongitude(getFloat("lon", coordObj));

		JSONObject sysObj = getObject("sys", jObj);
		try {
			loc.setCountry(getString("country", sysObj));
		} catch (JSONException e) {
			loc.setCountry("");
		}
//		loc.setSunrise(getInt("sunrise", sysObj));
//		loc.setSunset(getInt("sunset", sysObj));
		try {
			loc.setCity(getString("name", jObj));
		} catch (JSONException e) {
			loc.setCity("");
		}
		weather.location = loc;
		
		// We get weather info (This is an array)
		JSONArray jArr = jObj.getJSONArray("weather");
		JSONObject JSONWeather = jArr.getJSONObject(0);
		try {
			weather.currentCondition.setWeatherId(getInt("id", JSONWeather));
		} catch (JSONException e) {
			weather.currentCondition.setWeatherId(800);
		}
		try {
			weather.currentCondition.setDescr(getString("description", JSONWeather));
		} catch (JSONException e) {
			weather.currentCondition.setDescr("");
		}
			weather.currentCondition.setCondition(getString("main", JSONWeather));
		try {
			weather.currentCondition.setIcon(getString("icon", JSONWeather));
		} catch (JSONException e) {
			weather.currentCondition.setIcon("");
		}

			JSONObject mainObj = getObject("main", jObj);
		try {
			weather.currentCondition.setHumidity(getString("humidity", mainObj));
		} catch (JSONException e) {
			weather.currentCondition.setHumidity("");
		}
		try {
			weather.currentCondition.setPressure(getInt("pressure", mainObj));
		} catch (JSONException e) {
			weather.currentCondition.setPressure(0);
		}
//		weather.temperature.setMaxTemp(getFloat("temp_max", mainObj));
//		weather.temperature.setMinTemp(getFloat("temp_min", mainObj));
		try {
			weather.temperature.setTemp(getFloat("temp", mainObj));
		} catch (JSONException e) {
			weather.temperature.setTemp(0);
		}

			// Wind
			JSONObject wObj = getObject("wind", jObj);
		try {
			weather.wind.setSpeed(getFloat("speed", wObj));
		} catch (JSONException e) {
			weather.wind.setSpeed(0);
		}
		try {
			weather.wind.setDeg(getFloat("deg", wObj));
		} catch (JSONException e) {
			weather.wind.setDeg(0);
		}

		// Clouds
/*		JSONObject cObj = getObject("clouds", jObj);
		try {
			weather.clouds.setPerc(getInt("all", cObj));
		} catch (JSONException e) {
			weather.clouds.setPerc(0);
		}*/
		
		// We download the icon to show
		
		
		return weather;
	}
	
	
	private static JSONObject getObject(String tagName, JSONObject jObj)  throws JSONException {
		return jObj.getJSONObject(tagName);
	}
	
	private static String getString(String tagName, JSONObject jObj) throws JSONException {
		return jObj.getString(tagName);
	}

	private static float  getFloat(String tagName, JSONObject jObj) throws JSONException {
		return (float) jObj.getDouble(tagName);
	}
	
	private static int  getInt(String tagName, JSONObject jObj) throws JSONException {
		return jObj.getInt(tagName);
	}
	
}
