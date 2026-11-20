package com.neos.weatherservice

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.neos.weatherservice.igo.IgoFolder
import com.neos.weatherservice.igo.IgoFormat
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var statusText: TextView
    private lateinit var warningText: TextView
    private lateinit var weatherBlock: View
    private lateinit var icon: ImageView
    private lateinit var temp: TextView
    private lateinit var description: TextView
    private lateinit var details: TextView

    /** Called after the permission dialog; starts the service if location was granted. */
    private val permissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (WeatherService.hasLocationPermission(this)) startService() else
                Toast.makeText(this, R.string.toast_need_location, Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)
        statusText = findViewById(R.id.status)
        warningText = findViewById(R.id.warning)
        weatherBlock = findViewById(R.id.weather_block)
        icon = findViewById(R.id.icon)
        temp = findViewById(R.id.temp)
        description = findViewById(R.id.description)
        details = findViewById(R.id.details)

        findViewById<Button>(R.id.button_start).setOnClickListener { onStartClicked() }
        findViewById<Button>(R.id.button_stop).setOnClickListener { WeatherService.stop(this) }
        warningText.setOnClickListener { openSettings() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                WeatherService.status.collect(::render)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        warningText.visibility = if (folderOk()) View.GONE else View.VISIBLE
    }

    private fun folderOk(): Boolean = prefs.igoFolder?.let { IgoFolder(this, it).isValid() } == true

    private fun onStartClicked() {
        if (!folderOk()) {
            Toast.makeText(this, R.string.toast_choose_folder, Toast.LENGTH_LONG).show()
            openSettings()
            return
        }
        if (!WeatherService.hasLocationPermission(this)) {
            val perms = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
            )
            if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
            permissionRequest.launch(perms.toTypedArray())
            return
        }
        startService()
    }

    private fun startService() {
        WeatherService.start(this)
        if (prefs.launchNavigator) {
            if (WeatherService.launchNavigator(this, prefs)) finish()
            else Toast.makeText(this, R.string.toast_no_app, Toast.LENGTH_SHORT).show()
        }
    }

    private fun render(s: WeatherService.Status) {
        statusText.setText(if (s.running) R.string.status_running else R.string.status_stopped)
        val w = s.weather
        if (w == null) {
            weatherBlock.visibility = View.GONE
            if (s.error != null) statusText.append("\n" + s.error)
            return
        }
        weatherBlock.visibility = View.VISIBLE
        icon.setImageBitmap(WeatherText.icon(this, w))
        temp.text = IgoFormat.temperature(w)
        description.text = listOf(WeatherText.description(this, w.code.condition), s.place)
            .filter { it.isNotEmpty() }.joinToString("\n")
        val lines = mutableListOf(
            getString(R.string.humidity, w.humidity),
            getString(R.string.pressure, w.pressureMmHg, Math.round(w.pressureHpa).toInt()),
            WeatherText.wind(this, w),
        )
        s.location?.let { lines += String.format(Locale.US, "%.5f, %.5f", it.latitude, it.longitude) }
        if (s.updatedAt > 0) {
            lines += getString(R.string.updated_at, DateFormat.getTimeFormat(this).format(Date(s.updatedAt)))
        }
        s.error?.let { lines += it }
        details.text = lines.joinToString("\n")
    }

    private fun openSettings() = startActivity(Intent(this, SettingsActivity::class.java))

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_refresh -> { WeatherService.refresh(this); true }
        R.id.action_settings -> { openSettings(); true }
        R.id.action_about -> {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME)
                .setMessage(R.string.about_text)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
