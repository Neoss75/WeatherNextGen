package com.neos.weatherservice;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.neos.weatherservice.model.Weather;
import com.splunk.mint.Mint;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("ALL")
public class WeatherServiceiGO extends Service {
    public WeatherServiceiGO() {
    }

    private SharedPreferences mSettings;
    private static String APP_PREFERENCES = "settings_app";
    private static String APP_PREFERENCES_PATH = "PathIgoAvic";
    private NotificationManager nm;
    private LocationManager lm;
    private Boolean ExitMyApp;
    private Boolean SoundPlay;
    private Boolean choiceServer;
    private Boolean changeId;
    private Boolean commonProfileValue;
    private Boolean wasScreenOn = true;
    private Boolean closeService;
    private String basePath;
    private String PathSD = "/save/profiles/01/userlists/weather.txt";
    private String PathSD2 = "/save/profiles/02/userlists/weather.txt";
    private String PathSD3 = "/save/profiles/03/userlists/weather.txt";
    private String PathSD4 = "/save/profiles/04/userlists/weather.txt";
    private String PackName;
    private String LANG;
    private String LANG_WU;
    private String UNITS;
    private String UNITS_WU;
    private String UnitsMetric;
    private String UnitsMetricWU;
    private String CytiText;
    private String WeaTextNotif;
    private String str_data;
    private String str_dataWU;
    private String idServWU;
    private String lat;
    private String lon;
    private String ChoiceCoord;
    private final String LOG_TAG = "WeaServ";
    private final String NotTop = "Weather service";
    private final String NotBot = "Info update";
    private Timer timer;
    private Timer newtimer;
    private Timer timerfin;
    private TimerTask runs;
    private TimerTask task;
    private TimerTask scanfin;
    private Bitmap bmp;
    private BroadcastReceiver mReceiver;
    private final String APP_ID ="&APPID=472a5137fc1cf4943d9976f218ff60b0"; //my
//	private APP_ID ="&APPID=e3d037868e766182f3fc1f4507fcceff"; //semenof_se
//  private String APP_ID ="&APPID=319b7bccd867a372138c588d2481d24e"; //Alex_33
//	private String APP_ID ="&APPID=cd5e2fcee16ab4341fa663cc66a296b4"; //Yoburger
//    private String APP_ID ="&APPID=af167389cb0a2e22cbc36dd37f0e70dc"; //SamDell



    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Mint.initAndStartSession(this.getApplication(), "f2b60c99");
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);
        InitSettings();
        runServWea();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        timer.cancel();
        newtimer.cancel();
        timerfin.cancel();
        runs.cancel();
        task.cancel();
        scanfin.cancel();
        String status = "";
        saveText(status);
        saveTextFin(status);
        savePreferences("ServiceStatus", false);
        nm.cancel(1);
        unregisterReceiver(mReceiver);
    }

    public String VerInfoLog() {
        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG, e.toString());
        }
        assert pInfo != null;
        String versionName = pInfo.versionName;
        String Text = ("Version: " + versionName);
        return Text;
    }

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
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

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }


    public void runServWea() {
        Log.d(LOG_TAG, "SDK: " + Build.VERSION.SDK_INT);
        Log.d(LOG_TAG, VerInfoLog());
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        sendNotif(NotTop, NotBot);
        OnStartTimer();
        RunTimer();
        RunScanFinish();
    }

    public void OnStartTimer() {
        Integer TIME_UPD = mSettings.getInt("TimeUpdate", 1800000);
        timer = new Timer();
        task = new MyTask();
        timer.schedule(task, 10000, TIME_UPD);
    }

    public void RunTimer() {
        newtimer = new Timer();
        runs = new MyRun();
        newtimer.schedule(runs, 20000, 10000);
    }

    public void RunScanFinish() {
        timerfin = new Timer();
        scanfin = new TaskScanFin();
        timerfin.schedule(scanfin, 5000, 30000);
    }

    public class MyRun extends TimerTask {
        public void run() {
            RunStop();
        }
    }

    public class MyTask extends TimerTask {
        public void run() {
            if (lm != null) {
                Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                Log.d(LOG_TAG, "GPS_PROVIDER: " + location);
                if (location == null) {
                    location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    Log.d(LOG_TAG, "NETWORK_PROVIDER: " + location);
                }
                if (location == null) {
                    Log.d(LOG_TAG, "Location: " + location);
                } else {
                    if (location != null) {
                        if (!isNetworkAvailable()) {
                            Log.d(LOG_TAG, "isNetworkAvailable " + isNetworkAvailable());
                        } else {
                            double lat = location.getLatitude();
                            double lon = location.getLongitude();
                            if (changeId) {
                                idServWU = "3eaa8257b8640ccf";
                            } else {
                                idServWU = "31f019ff6b50250d";
                            }
                            String params = ("http://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon + LANG + UNITS + APP_ID);
                            String paramsWU = ("http://api.wunderground.com/api/" + idServWU + "/conditions/" + LANG_WU + "/q/" + lat + "," + lon + ".json");
                            if (choiceServer) {
                                JSONWeatherTask task = new JSONWeatherTask();
                                task.execute(params);
                                readAssetFileData();
                            } else {
                                JSONWeatherTask task = new JSONWeatherTask();
                                task.execute(paramsWU);
                                readAssetFileDataWU();
                                changeWUid();
                            }
                            lm.removeUpdates(locationListener);
                            Log.d(LOG_TAG, params);
                        }
                    }
                }
            }
        }
    }

    private class JSONWeatherTask extends AsyncTask<String, Void, Weather> {

        @Override
        protected Weather doInBackground(String... params) {
            Weather weather = new Weather();
            String data = ((new WeatherHttpClient()).getWeatherData(params[0]));

            try {
                if (choiceServer) {
                    if (data == null | data.contains("Error")) {
                        data = str_data;
                        Log.d(LOG_TAG, "Error 404:");
                    }
                    weather = JSONWeatherParser.getWeather(data);
                } else {
                    if (data == null | data.contains("Error")) {
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

            if (mSettings.getInt("Unit", 1) == 0) {
                UnitsMetric = "°K";
            } else if (mSettings.getInt("Unit", 1) == 1){
                UnitsMetric = "°C";
            } else if (mSettings.getInt("Unit", 1) == 2){
                UnitsMetric = "°F";
            }
            if (mSettings.getInt("UnitWU", 0) == 0) {
                UnitsMetricWU = "°C";
            } else {
                UnitsMetricWU = "°F";
            }
            String DescrText = ("" + weather.currentCondition.getDescr());
            String TempText;
            String icon = null;
            String IconText = null;
            String png = null;
            String WeaID = ("" + 200);

            if (choiceServer) {
                TempText = ("" + Math.round(weather.temperature.getTemp()) + UnitsMetric);
                IconText = ("" + weather.currentCondition.getIcon() + ".svg");
                WeaID = ("" + weather.currentCondition.getWeatherId());
                png = ("" + weather.currentCondition.getIcon() + ".png");
                icon = ("icon_owm/" + png);
            } else {
                if (mSettings.getInt("UnitWU", 0) == 0) {
                    TempText = ("" + Math.round(weather.temperature.getTemp()) + UnitsMetricWU);
                } else {
                    TempText = ("" + Math.round(weather.temperature.getTempF()) + UnitsMetricWU);
                }
                String iconUrl = (weather.currentCondition.getIconUrl());
                if (iconUrl.contains("nt_")) {
                    png = ("nt_" + weather.currentCondition.getIcon() + ".png");
                    IconText = ("nt_" + weather.currentCondition.getIcon() + ".svg");
                } else {
                    png = ("" + weather.currentCondition.getIcon() + ".png");
                    IconText = ("" + weather.currentCondition.getIcon() + ".svg");
                }
                icon = ("icon_wu/" + png);
                if (png.equals("mostlycloudy.png") | png.equals("mostlysunny.png") | png.equals("partlycloudy.png")
                        | png.equals("partlysunny.png") | png.equals("nt_mostlycloudy.png") | png.equals("nt_mostlysunny.png")
                        | png.equals("nt_partlycloudy.png") | png.equals("nt_partlysunny.png")) {
                    WeaID = ("" + 801);
                } else if (png.equals("chanceflurries.png") | png.equals("chancesnow.png") | png.equals("flurries.png")
                        | png.equals("snow.png") | png.equals("nt_chanceflurries.png") | png.equals("nt_chancesnow.png")
                        | png.equals("nt_flurries.png") | png.equals("nt_snow.png")) {
                    WeaID = ("" + 701);
                } else if (png.equals("cloudy.png") | png.equals("nt_cloudy.png")) {
                    WeaID = ("" + 501);
                } else if (png.equals("chancerain.png") | png.equals("chancesleet.png") | png.equals("rain.png") | png.equals("sleet.png")
                        | png.equals("nt_chancerain.png") | png.equals("nt_chancesleet.png") | png.equals("nt_rain.png")
                        | png.equals("nt_sleet.png")) {
                    WeaID = ("" + 301);
                } else if (png.equals("clear.png") | png.equals("sunny.png") | png.equals("nt_clear.png") | png.equals("nt_sunny.png")) {
                    WeaID = ("" + 800);
                } else if (png.equals("chancetstorms.png") | png.equals("tstorms.png")
                        | png.equals("nt_chancetstorms.png") | png.equals("nt_tstorms.png")) {
                    WeaID = ("" + 201);
                } else if (png.equals("fog.png") | png.equals("hazy.png") | png.equals("nt_fog.png") | png.equals("nt_hazy.png")) {
                    WeaID = ("" + 901);
                }
            }

            InputStream is = null;
            try {
                is = getAssets().open(icon);
            } catch (IOException e) {
                Log.e(LOG_TAG, e.toString());
            }
            bmp = BitmapFactory.decodeStream(is);
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, e.toString());
            }

            CytiText = (weather.location.getCity() + ", " + weather.location.getCountry());
            String saveWeather = (TempText + ";" + IconText + ";" + DescrText + ";" + WeaID); // + "\n" + CytiText);
            WeaTextNotif = (TempText + ", " + DescrText);
            saveText(saveWeather);
            Log.d(LOG_TAG, String.valueOf(saveWeather));
        }
    }

    public void saveText(String str) {
        String strEmpty = "";
        ScanFin();
        if (!str.equals(strEmpty)){
            sendNotif(WeaTextNotif, CytiText);
            Log.d(LOG_TAG, "Notification: " + WeaTextNotif + CytiText);
            if (SoundPlay) {
                soundPlayUpd();
            }
        }

        File userlists = new File(basePath + "/save/profiles/01/userlists");
        if (userlists.isDirectory() && userlists.exists() && !commonProfileValue) {
            try {
                FileOutputStream fos = new FileOutputStream(basePath + PathSD);
                Log.d(LOG_TAG, "Write: " + basePath + PathSD);
                Writer out = new OutputStreamWriter(fos, "UTF-16");
                out.write(str);
                out.flush();
                out.close();

            } catch (IOException ex) {
                Log.e(LOG_TAG, ex.toString());
            }
        } else {
            File userlists1 = new File(basePath + "/save/profiles/01/userlists");
            if (userlists1.isDirectory() && userlists1.exists()) {
                try {
                    FileOutputStream fost = new FileOutputStream(basePath + PathSD);
                    Log.d(LOG_TAG, "Write: " + basePath + PathSD);
                    Writer outs = new OutputStreamWriter(fost, "UTF-16");
                    outs.write(str);
                    outs.flush();
                    outs.close();

                } catch (IOException ex) {
                    Log.e(LOG_TAG, ex.toString());
                }
            }
            File userlists2 = new File(basePath + "/save/profiles/02/userlists");
            if (userlists2.isDirectory() && userlists2.exists()) {
                try {
                    FileOutputStream fjg = new FileOutputStream(basePath + PathSD2);
                    Log.d(LOG_TAG, "Write: " + basePath + PathSD2);
                    Writer ots = new OutputStreamWriter(fjg, "UTF-16");
                    ots.write(str);
                    ots.flush();
                    ots.close();

                } catch (IOException ex) {
                    Log.e(LOG_TAG, ex.toString());
                }
            }
            File userlists3 = new File(basePath + "/save/profiles/03/userlists");
            if (userlists3.isDirectory() && userlists3.exists()) {
                try {
                    FileOutputStream fjgs = new FileOutputStream(basePath + PathSD3);
                    Log.d(LOG_TAG, "Write: " + basePath + PathSD3);
                    Writer otst = new OutputStreamWriter(fjgs, "UTF-16");
                    otst.write(str);
                    otst.flush();
                    otst.close();

                } catch (IOException ex) {
                    Log.e(LOG_TAG, ex.toString());
                }
            }
            File userlists4 = new File(basePath + "/save/profiles/04/userlists");
            if (userlists4.isDirectory() && userlists4.exists()) {
                try {
                    FileOutputStream fjgr = new FileOutputStream(basePath + PathSD4);
                    Log.d(LOG_TAG, "Write: " + basePath + PathSD4);
                    Writer otsr = new OutputStreamWriter(fjgr, "UTF-16");
                    otsr.write(str);
                    otsr.flush();
                    otsr.close();

                } catch (IOException ex) {
                    Log.e(LOG_TAG, ex.toString());
                }
            }
        }
    }

    public void InitSettings() {
        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        basePath = mSettings.getString(APP_PREFERENCES_PATH, "");
        choiceServer = mSettings.getBoolean("Serv", true);
        commonProfileValue = mSettings.getBoolean("CommonProfiles", false);
        savePreferences("ServiceStatus", true);
        PackName = mSettings.getString("PackName", "");
        ExitMyApp = mSettings.getBoolean("CloseProg", false);
        LANG = ("&lang=" + mSettings.getString("LangName", "en"));
        LANG_WU = ("lang:" + mSettings.getString("LangNameWU", "RU"));
        UNITS = ("&units=" + mSettings.getString("UnitName", "metric"));
        SoundPlay = mSettings.getBoolean("PlayOnOff", false);
        changeId = mSettings.getBoolean("IDChange", true);
        closeService = mSettings.getBoolean("CloseService", true);
    }

    public void sendNotif(String top, String bot) {

        Context context = getApplicationContext();

        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context,
                0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

//        Resources res = context.getResources();
        Notification.Builder builder = new Notification.Builder(context);

        builder.setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_launcher)
                        // большая картинка
                .setLargeIcon(bmp)
                        //.setTicker(res.getString(R.string.warning)) // текст в строке состояния
                .setTicker("Run service")
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                        //.setContentTitle(res.getString(R.string.notifytitle)) // Заголовок уведомления
                .setContentTitle(top)
                        //.setContentText(res.getString(R.string.notifytext))
                .setContentText(bot); // Текст уведомления
//        builder.setDefaults(Notification.DEFAULT_ALL);
        int defaults = 0;
        boolean lights = false;
        boolean sound = false;
        boolean vibrate = false;
        if (lights) {
            defaults = defaults | Notification.DEFAULT_LIGHTS;
        }
        if (sound) {
            defaults = defaults | Notification.DEFAULT_SOUND;
        }
        if (vibrate) {
            defaults = defaults | Notification.DEFAULT_VIBRATE;
        }
        builder.setDefaults(defaults);
        if (Build.VERSION.SDK_INT < 16) {
            Notification notification = builder.getNotification(); // до API 16
            nm = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            notification.flags = notification.flags | Notification.FLAG_ONGOING_EVENT;
            nm.notify(1, notification);
        } else {
            Notification notification = builder.build(); // выше API 16
            nm = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            notification.flags = notification.flags | Notification.FLAG_ONGOING_EVENT;
            nm.notify(1, notification);
        }
    }

    private void savePreferences(String key, Boolean value) {
        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public void RunStop() {
        if (ExitMyApp) {
            if (closeService) {
                if (ScreenReceiver.wasScreenOn) {
                    Log.d(LOG_TAG, "Service over");
                    stopSelf();
                }
            } else {
                if (isAppRunning_Api(PackName)) {
                    Log.d(LOG_TAG, "Service over ");
                    stopSelf();
                }
            }
        }
    }

    private boolean isAppRunning_Api(String processName) {
        if (Build.VERSION.SDK_INT < 20) {
            Log.d(LOG_TAG, "SDK < 20");
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            ActivityManager.RunningTaskInfo foregroundTaskInfo = am.getRunningTasks(1).get(0);
            String foregroundTaskPackageName = foregroundTaskInfo.topActivity.getPackageName();
            Log.d(LOG_TAG, "Run: " + foregroundTaskPackageName);
            if (foregroundTaskPackageName.equals(processName))
                return false;
        } else {
            Log.d(LOG_TAG, "SDK > 20");
            ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);
            for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
                Log.d(LOG_TAG, "Process: " + runningServiceInfo.service.getClassName());
                if (runningServiceInfo.service.getClassName().equals(processName)) {
                    Log.d(LOG_TAG, "Process runing:" + runningServiceInfo.service.getClassName());
                    return false;
                }
            }
        }
        return true;
    }

    public class TaskScanFin extends TimerTask {
        public void run() {
            ScanFin();
        }
    }

    public void ScanFin() {
        String fileName = "/coordinates_fin.txt";
        String fileName2 = "/weather_fin.txt";
        String pathToFile = "/save/profiles/01/userlists";
        File file = new File(basePath + pathToFile + fileName);
        File file2 = new File(basePath + pathToFile + fileName2);

        if (file.isFile() & file.length() != 0) {
            InputStream inputStream = null;
            BufferedReader coord = null;
            try {
                inputStream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                coord = new BufferedReader(new InputStreamReader(inputStream, "UTF-16"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            String line = null;
            StringBuilder text = new StringBuilder();
            try {
                while ((line = coord.readLine()) != null) {
                    text.append(line);
                    String textCoord = text.toString();
                    if (textCoord == null || textCoord.equals("0;0")) {
                        saveTextFin("");
                    } else {
                        Log.e(LOG_TAG, textCoord);
                        String[] latlon = textCoord.split(";");
                        lat = latlon[0].substring(1, 9);
                        lon = latlon[1].substring(1, 9);
                        if (!textCoord.equals(ChoiceCoord)) {
                            ChoiceCoord = textCoord;
                            RunWeatherFin();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void RunWeatherFin() {
        if (lat != null) {
            JSONWeatherTaskFin task = new JSONWeatherTaskFin();
            if (changeId) {
                idServWU = "3eaa8257b8640ccf";
            } else {
                idServWU = "31f019ff6b50250d";
            }
            String paramsWU = ("http://api.wunderground.com/api/" + idServWU + "/conditions/" + LANG_WU + "/q/" + lat + "," + lon + ".json");
            task.execute(paramsWU);
        }
    }

    private class JSONWeatherTaskFin extends AsyncTask<String, Void, Weather> {

        @Override
        protected Weather doInBackground(String... params) {
            if (isCancelled()) return null;
            Weather weather = new Weather();
            String data = ((new WeatherHttpClient()).getWeatherData(params[0]));
            try {
                if (data == null | data.contains("Error")) {
                    data = str_dataWU;
                }
                    weather = JSONWeatherParserWU.getWeather(data);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.toString());
            }
            return weather;
        }

        protected void onPostExecute(Weather weather) {
            super.onPostExecute(weather);

            if (mSettings.getInt("Unit", 1) == 0) {
                UnitsMetric = "°K";
            } else if (mSettings.getInt("Unit", 1) == 1){
                UnitsMetric = "°C";
            } else if (mSettings.getInt("Unit", 1) == 2){
                UnitsMetric = "°F";
            }
            String unitsMetricWU;
            if (mSettings.getInt("UnitWU", 0) == 0) {
                unitsMetricWU = "°C";
            } else {
                unitsMetricWU = "°F";
            }
            String DescrText = ("" + weather.currentCondition.getDescr());
            String TempText;
            String IconText;
            String png;
            String WeaID = ("" + 200);

                if (mSettings.getInt("UnitWU", 0) == 0) {
                    TempText = ("" + Math.round(weather.temperature.getTemp()) + unitsMetricWU);
                } else {
                    TempText = ("" + Math.round(weather.temperature.getTempF()) + unitsMetricWU);
                }
                String iconUrl = (weather.currentCondition.getIconUrl());
                if (iconUrl.contains("nt_")) {
                    png = ("nt_" + weather.currentCondition.getIcon() + ".png");
                    IconText = ("nt_" + weather.currentCondition.getIcon() + ".svg");
                } else {
                    png = ("" + weather.currentCondition.getIcon() + ".png");
                    IconText = ("" + weather.currentCondition.getIcon() + ".svg");
                }
                if (png.equals("mostlycloudy.png") | png.equals("mostlysunny.png") | png.equals("partlycloudy.png")
                        | png.equals("partlysunny.png") | png.equals("nt_mostlycloudy.png") | png.equals("nt_mostlysunny.png")
                        | png.equals("nt_partlycloudy.png") | png.equals("nt_partlysunny.png")) {
                    WeaID = ("" + 801);
                } else if (png.equals("chanceflurries.png") | png.equals("chancesnow.png") | png.equals("flurries.png")
                        | png.equals("snow.png") | png.equals("nt_chanceflurries.png") | png.equals("nt_chancesnow.png")
                        | png.equals("nt_flurries.png") | png.equals("nt_snow.png")) {
                    WeaID = ("" + 701);
                } else if (png.equals("cloudy.png") | png.equals("nt_cloudy.png")) {
                    WeaID = ("" + 501);
                } else if (png.equals("chancerain.png") | png.equals("chancesleet.png") | png.equals("rain.png") | png.equals("sleet.png")
                        | png.equals("nt_chancerain.png") | png.equals("nt_chancesleet.png") | png.equals("nt_rain.png")
                        | png.equals("nt_sleet.png")) {
                    WeaID = ("" + 301);
                } else if (png.equals("clear.png") | png.equals("sunny.png") | png.equals("nt_clear.png") | png.equals("nt_sunny.png")) {
                    WeaID = ("" + 800);
                } else if (png.equals("chancetstorms.png") | png.equals("tstorms.png")
                        | png.equals("nt_chancetstorms.png") | png.equals("nt_tstorms.png")) {
                    WeaID = ("" + 201);
                } else if (png.equals("fog.png") | png.equals("hazy.png") | png.equals("nt_fog.png") | png.equals("nt_hazy.png")) {
                    WeaID = ("" + 901);
					}

            String cytiText = (weather.location.getCity() + ", " + weather.location.getCountry());
            String saveWeatherFin = (TempText + ";" + IconText + ";"  + cytiText);
            saveTextFin(saveWeatherFin);
            Log.d(LOG_TAG, String.valueOf(saveWeatherFin));

        }
    }

    public void saveTextFin(String str) {
        File userlists = new File(basePath + "/save/profiles/01/userlists");
        String pathSD = "/save/profiles/01/userlists/weather_fin.txt";
        if (userlists.isDirectory() && userlists.exists() && !commonProfileValue) {
            try {
                FileOutputStream fos = new FileOutputStream(basePath + pathSD);
                Writer out = new OutputStreamWriter(fos, "UTF-16");
                out.write(str);
                out.flush();
                out.close();

            } catch (IOException ex) {
                Log.e(LOG_TAG, ex.toString());
            }
        } else {
            File userlists1 = new File(basePath + "/save/profiles/01/userlists");
            if (userlists1.isDirectory() && userlists1.exists()) {
                try {
                    FileOutputStream fost = new FileOutputStream(basePath + pathSD);
                    Writer outs = new OutputStreamWriter(fost, "UTF-16");
                    outs.write(str);
                    outs.flush();
                    outs.close();

                } catch (IOException ex) {
                    Log.e(LOG_TAG, ex.toString());
                }
            }
            File userlists2 = new File(basePath + "/save/profiles/02/userlists");
            if (userlists2.isDirectory() && userlists2.exists()) {
                try {
                    String pathSD2 = "/save/profiles/02/userlists/weather_fin.txt";
                    FileOutputStream fjg = new FileOutputStream(basePath + pathSD2);
                    Writer ots = new OutputStreamWriter(fjg, "UTF-16");
                    ots.write(str);
                    ots.flush();
                    ots.close();

                } catch (IOException ex) {
                    Log.e(LOG_TAG, ex.toString());
                }
            }
            File userlists3 = new File(basePath + "/save/profiles/03/userlists");
            if (userlists3.isDirectory() && userlists3.exists()) {
                try {
                    String pathSD3 = "/save/profiles/03/userlists/weather_fin.txt";
                    FileOutputStream fjgs = new FileOutputStream(basePath + pathSD3);
                    Writer otst = new OutputStreamWriter(fjgs, "UTF-16");
                    otst.write(str);
                    otst.flush();
                    otst.close();

                } catch (IOException ex) {
                    Log.e(LOG_TAG, ex.toString());
                }
            }
            File userlists4 = new File(basePath + "/save/profiles/04/userlists");
            if (userlists4.isDirectory() && userlists4.exists()) {
                try {
                    String pathSD4 = "/save/profiles/04/userlists/weather_fin.txt";
                    FileOutputStream fjgr = new FileOutputStream(basePath + pathSD4);
                    Writer otsr = new OutputStreamWriter(fjgr, "UTF-16");
                    otsr.write(str);
                    otsr.flush();
                    otsr.close();

                } catch (IOException ex) {
                    Log.e(LOG_TAG, ex.toString());
                }
            }
        }
    }


    public void soundPlayUpd() {
        MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.ding);
        mediaPlayer.start();
    }

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
        str_data = new String(buffer);
        Log.d(LOG_TAG, "New data: " + str_data);
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
        str_dataWU = new String(buffer);
//        Log.d(LOG_TAG, "New data: " + str_dataWU);
    }

    private void changeWUid() {
        if (changeId) {
            savePreferences("IDChange", false);
        } else {
            savePreferences("IDChange", true);
        }
    }
}
