package com.neos.weatherservice;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.neos.weatherservice.model.Weather;
import com.splunk.mint.Mint;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;


@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class MainActivity extends Activity {
	private static final String[] INITIAL_PERMS={
			Manifest.permission.ACCESS_FINE_LOCATION,
			Manifest.permission.ACCESS_NETWORK_STATE,
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.READ_PHONE_STATE
	};

	private static final int INITIAL_REQUEST=1337;
	private static final int LOCATION_REQUEST=INITIAL_REQUEST+1;
	private static final int NETWORK_REQUEST=INITIAL_REQUEST+2;
	private static final int READ_REQUEST=INITIAL_REQUEST+3;
	private static final int WRITE_REQUEST=INITIAL_REQUEST+4;
	private static final int PHONESTATE_REQUEST=INITIAL_REQUEST+5;
	private TextView cityText;
	private TextView condDescr;
	private TextView temp;
	private TextView press;
	private TextView windSpeed;
	private TextView windDeg;
	private TextView hum;
	private TextView LatLon;
	private TextView ServStatus;
	private ImageView img;
	private ImageView logoImg;
	private Boolean ExitMyApp;
	private Boolean choiceServer;
	private Boolean changeId;
	private LocationManager lm;
	private SharedPreferences mSettings;
	private final String APP_PREFERENCES;
	private static final String APP_PREFERENCES_PATH = "PathIgoAvic";
	private String PackName;
	private String UnitsMetric;
	private String LANG;
	private String LANG_WU;
	private String UNITS;
	private String UnitSpeed;
	private String str_data;
	private String str_dataWU;
	private final String LOG_TAG = "WeaServ";
//  wu 31f019ff6b50250d - Neosss


	private final LocationListener locationListener_my;

	public MainActivity() {
		APP_PREFERENCES = "settings_app";
		locationListener_my = new LocationListener() {
            @Override
            public void onLocationChanged(final Location location) {
                ProviderLocationEnable();
            }

            @Override
            public void onProviderDisabled(String provider) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
        };
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Mint.initAndStartSession(this.getApplication(), "f2b60c99");

		mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
		cityText = (TextView) findViewById(R.id.cityText);
		condDescr = (TextView) findViewById(R.id.condDescr);
		temp = (TextView) findViewById(R.id.temp);
		hum = (TextView) findViewById(R.id.hum);
		press = (TextView) findViewById(R.id.press);
		windSpeed = (TextView) findViewById(R.id.windSpeed);
		windDeg = (TextView) findViewById(R.id.windDeg);
		LatLon = (TextView) findViewById(R.id.LatLon);
		ServStatus = (TextView) findViewById(R.id.textServiceStatus);
		img = (ImageView) findViewById(R.id.imageView);
		logoImg = (ImageView) findViewById(R.id.imageViewLogo);
		lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		PackageInfo pInfo = null;
		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		} catch (PackageManager.NameNotFoundException e) {
			Log.d(LOG_TAG, e.toString());
		}
		assert pInfo != null;
		String versionName = pInfo.versionName;
		String Text = ("Version: " + versionName);
		TextView textView = (TextView) findViewById(R.id.textVersin);
		textView.setText(Text);
		Log.d(LOG_TAG, "SDK: " + Build.VERSION.SDK_INT);
		Log.d(LOG_TAG, Text);

//		Picasso.with(this)
				//Загружаем изображение через его адрес URL:
//				.load("http://icons.wxug.com/graphics/wu2/logo_130x80.png")
				//Настраиваем изображение, которое отобразится в случае ошибки при загрузке:
//				.error(R.drawable.ic_launcher)
				//Через команду placeholder мы задаем изображение, которое будет
				//отображаться до тех пор, пока не загрузятся необходимые изображения:
//				.placeholder(R.drawable.ic_launcher)
				//Выставляем размер изображения:
//				.resize(250,250)
				//Показываем его в объекте ImageView:
//				.into(logoImg);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (!canAccessLocation()) requestPermissions(INITIAL_PERMS, INITIAL_REQUEST);
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

		switch(requestCode) {
			case LOCATION_REQUEST:
				if (!canAccessLocation()) {
					doLocationThing();
				}
				break;
			case NETWORK_REQUEST:
				if (!canAccessNetwork()) {
					doLocationThing();
				}
				break;
			case READ_REQUEST:
				if (!canAccessRead()) {
					doLocationThing();
				}
				break;
			case WRITE_REQUEST:
				if (!canAccessWrite()) {
					doLocationThing();
				}
				break;
			case PHONESTATE_REQUEST:
				if (!canAccessPhone()) {
					doLocationThing();
				}
				break;
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	private boolean canAccessLocation() {
		return(hasPermission(Manifest.permission.ACCESS_FINE_LOCATION));
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	private boolean canAccessNetwork() {
		return(hasPermission(Manifest.permission.ACCESS_NETWORK_STATE));
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	private boolean canAccessRead() {
		return(hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE));
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	private boolean canAccessWrite() {
		return(hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE));
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	private boolean canAccessPhone() {
		return(hasPermission(Manifest.permission.READ_PHONE_STATE));
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	private boolean hasPermission(String perm) {
		return(PackageManager.PERMISSION_GRANTED==checkSelfPermission(perm));
	}

	private void doLocationThing() {
		Toast.makeText(this, "Ok", Toast.LENGTH_SHORT).show();
	}

	public void onResume() {
		super.onResume();
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			return;
		}
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
			return;
		}
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			return;
		}
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
			return;
		}
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 180000, 10, locationListener_my);
		lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 180000, 10, locationListener_my);
		InitPref();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			return;
		}
		lm.removeUpdates(locationListener_my);
	}

	private void InitPref() {
		ExitMyApp = mSettings.getBoolean("CloseProg", false);
		PackName = mSettings.getString("PackName", "");
		LANG = ("&lang=" + mSettings.getString("LangName", "en"));
		LANG_WU = ("lang:" + mSettings.getString("LangNameWU", "RU"));
		UNITS = ("&units=" + mSettings.getString("UnitName", "metric"));
		Boolean serviceBool = mSettings.getBoolean("ServiceStatus", false);
		choiceServer = mSettings.getBoolean("Serv", true);
		changeId = mSettings.getBoolean("IDChange", true);
		if (serviceBool) {
			ServStatus.setText(R.string.text_run);
		} else {
			ServStatus.setText(R.string.text_stop);
		}
		String name;
		if (choiceServer) {
			name = "logo/OWM_logo.png";
		} else {
			name = "logo/logo_WA.png";
		}
		InputStream logo = null;
		try {
			logo = getAssets().open(name);
		} catch (IOException e) {
			Log.e(LOG_TAG, e.toString());
		}
		logoImg.setImageBitmap(BitmapFactory.decodeStream(logo));
		try {
			if (logo != null) {
				logo.close();
			}
		} catch (IOException e) {
			Log.e(LOG_TAG, e.toString());
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		Intent intent = new Intent(MainActivity.this, Activity_Settings.class);
		Intent intentAbout = new Intent(MainActivity.this, AboutActivity.class);

		switch (id) {
			case R.id.action_settings:
				startActivity(intent);
				return true;
			case R.id.action_about:
				startActivity(intentAbout);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null;
	}


	private void ProviderLocationEnable() {
		if (lm != null) {
			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				return;
			}
			Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			Log.d(LOG_TAG, "GPS: " + location);
			if (location == null) {
				location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
				Log.d(LOG_TAG, "GSM: " + location);
			}
			if (location == null) {
				Toast.makeText(getApplicationContext(),
						"Нет текущих координат", Toast.LENGTH_LONG).show();
				Log.d(LOG_TAG, "Not coordinates");
			} else {
					if (!isNetworkAvailable()) {
						Toast.makeText(getApplicationContext(),
								R.string.text_toast_no_internet, Toast.LENGTH_LONG).show();
						Log.d(LOG_TAG, "Not internet");
					} else {
						double lat = location.getLatitude();
						double lon = location.getLongitude();
						String idServWU;
						String APP_ID;
						if (changeId) {
							idServWU = "3eaa8257b8640ccf";
							APP_ID ="&APPID=472a5137fc1cf4943d9976f218ff60b0"; //my
						} else {
							idServWU = "31f019ff6b50250d";
							APP_ID ="&APPID=e3d037868e766182f3fc1f4507fcceff"; //semenof_se
						}

						String concatLocation = ("http://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon);
						String conLocationWU = ("http://api.wunderground.com/api/" + idServWU + "/conditions/" + LANG_WU + "/q/" + lat + "," + lon + ".json");
						String latLon = ("lat=" + lat + " " + "lon=" + lon);
						LatLon.setText(latLon);
						String params = (concatLocation + LANG + UNITS + APP_ID);
						if (choiceServer) {
							JSONWeatherTask task = new JSONWeatherTask();
							task.execute(params);
							readAssetFileData();
						} else {
							JSONWeatherTask task = new JSONWeatherTask();
							task.execute(conLocationWU);
							readAssetFileDataWU();
							changeWUid();
						}
						Log.d(LOG_TAG, params);
					}
			}
		}
	}


	private class JSONWeatherTask extends AsyncTask<String, Void, Weather> {

		@Override
		protected Weather doInBackground(String... params) {
			String data;
			Weather weather = new Weather();
			data = ((new WeatherHttpClient()).getWeatherData(params[0]));

				try {
					if (choiceServer) {
						assert data != null;
						if (data.contains("Error")) {
							data = str_data;
							Log.d(LOG_TAG, "Error 404:");
						}
						weather = JSONWeatherParser.getWeather(data);
					} else {
						assert data != null;
						if (data.contains("Error")) {
							readAssetFileDataWU();
							data = str_dataWU;
						}
							weather = JSONWeatherParserWU.getWeather(data);
					}
				} catch (JSONException e) {
					Log.e(LOG_TAG, e.toString());
				}

			return weather;
		}

		@Override
		protected void onPostExecute(Weather weather) {
			super.onPostExecute(weather);
			String Hum;
			String descr;

			InputStream is = null;
			if (mSettings.getInt("Unit", 1) == 0) {
				UnitsMetric = "°K";
				UnitSpeed = " m/s";
			} else if (mSettings.getInt("Unit", 1) == 1){
				UnitsMetric = "°C";
				UnitSpeed = " " + getString(R.string.text_wind_units);
			} else if (mSettings.getInt("Unit", 1) == 2){
				UnitsMetric = "°F";
				UnitSpeed = " m/h";
			}
			String unitsMetricWU;
			if (mSettings.getInt("UnitWU", 1) == 0) {
				unitsMetricWU = "°C";
			} else {
				unitsMetricWU = "°F";
			}

			double pres = (weather.currentCondition.getPressure() * 0.75006375541921);
			int presInt = (int) pres;
			String city = (weather.location.getCity() + ", " + weather.location.getCountry());
			String Pre = ("" + presInt + " " + getString(R.string.text_pressure_units));
			String Speed = ("" + weather.wind.getSpeed() + UnitSpeed);
			String Deg = (" " + weather.wind.getDeg() + "°");
			String ico = ("" + weather.currentCondition.getIcon() + ".png");
			String icon;
			String Temp;
			String iconUrl;
			if (choiceServer) {
				Temp = ("" + Math.round(weather.temperature.getTemp()) + UnitsMetric);
				Hum =("" + weather.currentCondition.getHumidity() + "%");
				descr = (weather.currentCondition.getCondition() + "(" + weather.currentCondition.getDescr() + ")");
				icon = ("icon_owm/" + ico);
			} else {
				if (mSettings.getInt("UnitWU", 0) == 0) {
					Temp = ("" + Math.round(weather.temperature.getTemp()) + unitsMetricWU);
				} else {
					Temp = ("" + Math.round(weather.temperature.getTempF()) + unitsMetricWU);
				}
				Hum =("" + weather.currentCondition.getHumidity());
				String str = getResources().getString(R.string.text_type);
				descr = (str + " " + weather.currentCondition.getDescr());
				iconUrl = (weather.currentCondition.getIconUrl());
				if (iconUrl.contains("nt_")) {
					icon = ("icon_wu/nt_" + ico);
				} else {
					icon = ("icon_wu/" + ico);
				}
			}
			try {
				is = getAssets().open(icon);
			} catch (IOException e) {
				Log.e(LOG_TAG, e.toString());
			}

			cityText.setText(city);
			condDescr.setText(descr);
			temp.setText(Temp);
			hum.setText(Hum);
			press.setText(Pre);
			windSpeed.setText(Speed);
			windDeg.setText(Deg);
			img.setImageBitmap(BitmapFactory.decodeStream(is));
			try {
				if (is != null) {
					is.close();
				}
			} catch (IOException e) {
				Log.e(LOG_TAG, e.toString());
			}
		}
	}

	public void onClickStart(View view) {
		if (mSettings.contains(APP_PREFERENCES_PATH)) {
			startService(new Intent(this, WeatherServiceiGO.class));
			savePreferences("ServiceStatus", true);
			if (ExitMyApp) {
				Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage(PackName);
				Log.d(LOG_TAG, "Settings name app: " + PackName);
				if (LaunchIntent == null) {
					Log.d(LOG_TAG, "PackageName not founded");
					Toast.makeText(getApplicationContext(),
							R.string.text_toast_no_app,
							Toast.LENGTH_SHORT).show();
				} else {
					Log.d(LOG_TAG, "PackageName founded, start app.");
					startActivity(LaunchIntent);
					finish();
				}
			}
		} else {
			Log.d(LOG_TAG, "PackageName not save");
			Toast.makeText(getApplicationContext(),
					R.string.text_toast_no_app,
					Toast.LENGTH_SHORT).show();
		}
	}

	public void onClickStop(View view) {
		stopService(new Intent(this, WeatherServiceiGO.class));
		savePreferences("ServiceStatus", false);
	}

	private void savePreferences(String key, Boolean value) {
		mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = mSettings.edit();
		editor.putBoolean(key, value);
		editor.apply();
		InitPref();
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private void readAssetFileData() {
		String str = "data/data.xml";
		byte[] buffer = null;
		InputStream is;
		try {
			is = getAssets().open(str);
			int size = is.available();
			buffer = new byte[size];
			is.read(buffer);
			is.close();
		} catch (IOException e) {
			Log.e(LOG_TAG, e.toString());
		}
		assert buffer != null;
		str_data = new String(buffer);
	}

	private void readAssetFileDataWU() {
		String str = "data/dataWU.xml";
		byte[] buffer = null;
		InputStream is;
		try {
			is = getAssets().open(str);
			int size = is.available();
			buffer = new byte[size];
			is.read(buffer);
			is.close();
		} catch (IOException e) {
			Log.e(LOG_TAG, e.toString());
		}
		assert buffer != null;
		str_dataWU = new String(buffer);
	}

	private void changeWUid() {
		if (changeId) {
			savePreferences("IDChange", false);
		} else {
			savePreferences("IDChange", true);
		}
	}
}