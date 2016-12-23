package com.neos.weatherservice;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

public class ActivityRunService extends Activity {

    private static final String APP_PREFERENCES = "settings_app";
    private static final String APP_PREFERENCES_PATH = "PathIgoAvic";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Transparent);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_service);
        SharedPreferences mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        String LOG_TAG = "WeaServ";
        if (mSettings.contains(APP_PREFERENCES_PATH)) {
            String packName = mSettings.getString("PackName", "");
            Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage(packName);
            Log.d(LOG_TAG, "Settings name app: " + packName);
            if (LaunchIntent == null) {
                Log.d(LOG_TAG, "PackageName not founded");
                Toast.makeText(getApplicationContext(),
                        R.string.text_toast_no_app,
                        Toast.LENGTH_SHORT).show();
            } else {
                Log.d(LOG_TAG, "PackageName founded, start app.");
                startActivity(LaunchIntent);
                startService(new Intent(this, WeatherServiceiGO.class));
            }
        } else {
            Log.d(LOG_TAG, "PackageName not save");
            Toast.makeText(getApplicationContext(),
                    R.string.text_toast_no_app,
                    Toast.LENGTH_SHORT).show();
        }
        finish();
    }

}
