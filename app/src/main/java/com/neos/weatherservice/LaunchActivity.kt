package com.neos.weatherservice

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

/**
 * Second launcher icon ("Weather service"): starts the service and the navigator in one tap.
 * Falls back to the main screen when something still needs to be set up.
 */
class LaunchActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        if (prefs.igoFolder == null || !WeatherService.hasLocationPermission(this)) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            WeatherService.start(this)
            if (!WeatherService.launchNavigator(this, prefs)) {
                Toast.makeText(this, R.string.toast_no_app, Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }
}
