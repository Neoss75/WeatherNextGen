package com.neos.weatherservice;




import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


class WeatherHttpClient {


	String getWeatherData(String location) {
		HttpURLConnection con = null ;
		InputStream is = null;

		String LOG_TAG = "WeaServ";
		try {
			URL url = new URL(location);
			con = (HttpURLConnection) url.openConnection();
//			con = (HttpURLConnection) (new URL(urlStr)).openConnection();
			con.setRequestMethod("GET");
			con.setReadTimeout(15000);
//			con.setDoInput(true);
//			con.setDoOutput(true);
			con.connect();
			Log.d(LOG_TAG, location);

			// Let's read the response
			StringBuilder buffer = new StringBuilder();
			is = con.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			while ((line = br.readLine()) != null)
				buffer.append(line).append("\r\n");
			is.close();
			con.disconnect();
			Log.d(LOG_TAG, buffer.toString());
			return buffer.toString();
		}
		catch(Throwable t) {
			Log.d(LOG_TAG, t.toString());
		}
		finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch(Throwable t) {Log.e(LOG_TAG, t.toString());}
			try {
				if (con != null) {
					con.disconnect();
				}
			} catch(Throwable t) {Log.e(LOG_TAG, t.toString());}
		}

		return null;
				
	}
}
