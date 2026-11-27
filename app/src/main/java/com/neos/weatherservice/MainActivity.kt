package com.neos.weatherservice

import android.Manifest
import android.animation.ArgbEvaluator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neos.weatherservice.igo.IgoFolder
import com.neos.weatherservice.ui.Motion
import com.neos.weatherservice.ui.Palette
import com.neos.weatherservice.ui.Scene
import com.neos.weatherservice.ui.WeatherBackdropView
import com.neos.weatherservice.ui.WeatherSceneView
import com.neos.weatherservice.ui.WeatherStyle
import com.neos.weatherservice.weather.Units
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    /** One of the three glass cards under the temperature. */
    private class Metric(root: View, @DrawableRes iconRes: Int, @StringRes labelRes: Int) {
        val icon: ImageView = root.findViewById(R.id.metric_icon)
        val value: TextView = root.findViewById(R.id.metric_value)
        val sub: TextView = root.findViewById(R.id.metric_sub)

        init {
            icon.setImageResource(iconRes)
            root.findViewById<TextView>(R.id.metric_label).setText(labelRes)
        }
    }

    /** A row of the "Details" card. */
    private class InfoRow(val root: View, @DrawableRes iconRes: Int, @StringRes labelRes: Int) {
        val value: TextView = root.findViewById(R.id.info_value)
        val note: TextView = root.findViewById(R.id.info_note)

        init {
            root.findViewById<ImageView>(R.id.info_icon).setImageResource(iconRes)
            root.findViewById<TextView>(R.id.info_label).setText(labelRes)
        }

        /** Makes the row open [action] on tap and shows a chevron. */
        fun onClick(action: () -> Unit) {
            root.findViewById<View>(R.id.info_chevron).visibility = View.VISIBLE
            root.setOnClickListener { action() }
        }

        fun setNote(text: String?) {
            note.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
            note.text = text
        }
    }

    private lateinit var prefs: Prefs
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var warning: View
    private lateinit var statusDot: View
    private lateinit var statusText: TextView
    private lateinit var hero: View
    private lateinit var scene: WeatherSceneView
    private lateinit var place: TextView
    private lateinit var temp: TextView
    private lateinit var tempUnit: TextView
    private lateinit var description: TextView
    private lateinit var updated: TextView
    private lateinit var metrics: View
    private lateinit var humidity: Metric
    private lateinit var pressure: Metric
    private lateinit var wind: Metric
    private lateinit var info: View
    private lateinit var coordsRow: InfoRow
    private lateinit var navigatorRow: InfoRow
    private lateinit var folderRow: InfoRow
    private var lastLocation: Location? = null
    private lateinit var errorCard: View
    private lateinit var errorText: TextView
    private lateinit var toggle: MaterialButton

    private val background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0, 0))
    /** Colours currently on screen (GradientDrawable.getColors needs API 24); 0 = not set yet. */
    private val shownColors = intArrayOf(0, 0)
    private var palette = WeatherStyle.idlePalette
    private var paletteTween: Motion.Tween? = null
    private var tempTween: Motion.Tween? = null
    private var shownTemp: Int? = null
    private var pulse: Motion.Tween? = null
    private var running: Boolean? = null
    private var weatherShown = false
    private var motion = true
    private lateinit var backdrop: WeatherBackdropView
    private var lastUpdatedAt = 0L
    private val stopRefreshing = Runnable { swipe.isRefreshing = false }

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
        motion = Motion.enabled(this)

        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        window.setBackgroundDrawable(background)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        swipe = findViewById(R.id.swipe)
        warning = findViewById(R.id.warning)
        statusDot = findViewById(R.id.status_dot)
        statusText = findViewById(R.id.status)
        hero = findViewById(R.id.hero)
        backdrop = findViewById(R.id.backdrop)
        scene = findViewById(R.id.scene)
        place = findViewById(R.id.place)
        temp = findViewById(R.id.temp)
        tempUnit = findViewById(R.id.temp_unit)
        description = findViewById(R.id.description)
        updated = findViewById(R.id.updated)
        metrics = findViewById(R.id.metrics)
        humidity = Metric(findViewById(R.id.metric_humidity), R.drawable.ic_humidity, R.string.label_humidity)
        pressure = Metric(findViewById(R.id.metric_pressure), R.drawable.ic_pressure, R.string.label_pressure)
        wind = Metric(findViewById(R.id.metric_wind), R.drawable.ic_wind_arrow, R.string.label_wind)
        info = findViewById(R.id.info)
        coordsRow = InfoRow(findViewById(R.id.info_coords), R.drawable.ic_my_location, R.string.info_coordinates)
        navigatorRow = InfoRow(findViewById(R.id.info_navigator), R.drawable.ic_car, R.string.info_navigator)
        folderRow = InfoRow(findViewById(R.id.info_folder), R.drawable.ic_folder, R.string.info_folder)
        navigatorRow.onClick(::openSettings)
        folderRow.onClick(::openSettings)
        errorCard = findViewById(R.id.error_card)
        errorText = findViewById(R.id.error_text)
        toggle = findViewById(R.id.button_toggle)

        scene.animated = motion
        backdrop.animated = motion
        applyPalette(palette)
        swipe.setProgressBackgroundColorSchemeColor(0xFFFFFFFF.toInt())
        swipe.setOnRefreshListener(::refresh)
        toggle.setOnClickListener {
            if (running == true) WeatherService.stop(this) else onStartClicked()
        }
        warning.setOnClickListener { openSettings() }

        if (savedInstanceState == null) enter(findViewById(R.id.status_pill), hero, info, toggle)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                WeatherService.status.collect(::render)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        warning.visibility = if (folderOk()) View.GONE else View.VISIBLE
        renderSettingsInfo() // the settings may have just changed
        val nowMotion = Motion.enabled(this)
        if (nowMotion != motion) {
            motion = nowMotion
            scene.animated = motion
            backdrop.animated = motion
            setPulsing(running == true)
        }
    }

    override fun onStop() {
        setPulsing(false) // restarted by render() when the screen is shown again
        super.onStop()
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

    /** Pull-to-refresh and the toolbar button. The spinner stops on the next status update. */
    private fun refresh() {
        if (running != true) {
            swipe.isRefreshing = false
            Toast.makeText(this, R.string.toast_start_first, Toast.LENGTH_SHORT).show()
            return
        }
        swipe.isRefreshing = true
        WeatherService.refresh(this)
        swipe.removeCallbacks(stopRefreshing)
        swipe.postDelayed(stopRefreshing, REFRESH_TIMEOUT_MS)
    }

    // ---- Rendering ----

    private fun render(s: WeatherService.Status) {
        if (swipe.isRefreshing) {
            swipe.removeCallbacks(stopRefreshing)
            swipe.isRefreshing = false
        }
        renderStatus(s)
        renderToggle(s.running)

        val w = s.weather
        if (w == null) {
            applyPalette(WeatherStyle.idlePalette)
            showScene(WeatherStyle.idleScene)
            tempTween?.cancel()
            shownTemp = null
            temp.text = getString(R.string.temp_empty)
            tempUnit.text = ""
            setText(description, getString(if (s.running) R.string.notif_starting else R.string.empty_hint))
            place.visibility = View.GONE
            updated.visibility = View.GONE
            metrics.visibility = View.GONE
            weatherShown = false
        } else {
            applyPalette(WeatherStyle.palette(w.code.condition, w.isDay))
            showScene(WeatherStyle.scene(w.code.condition, w.isDay))
            showTemperature(w.temperature.roundToInt())
            tempUnit.text = if (w.units == Units.METRIC) "C" else "F"
            setText(description, WeatherText.description(this, w.code.condition))

            place.visibility = if (s.place.isEmpty()) View.GONE else View.VISIBLE
            setText(place, s.place)
            val info = mutableListOf<String>()
            if (s.updatedAt > 0) {
                info += getString(R.string.updated_at, DateFormat.getTimeFormat(this).format(Date(s.updatedAt)))
            }
            updated.visibility = if (info.isEmpty()) View.GONE else View.VISIBLE
            setText(updated, info.joinToString())

            setText(humidity.value, getString(R.string.value_percent, w.humidity))
            humidity.sub.text = ""
            setText(pressure.value, w.pressureMmHg.toString())
            setText(
                pressure.sub,
                getString(R.string.value_pressure, getString(R.string.unit_mmhg), Math.round(w.pressureHpa).toInt()),
            )
            setText(wind.value, WeatherText.windSpeed(this, w))
            setText(wind.sub, WeatherText.windDirection(this, w.windDirection))
            pointWindArrow(WeatherStyle.arrowRotation(w.windDirection))

            if (weatherShown && s.updatedAt != lastUpdatedAt) {
                popCards(humidity.icon.parent as View, pressure.icon.parent as View, wind.icon.parent as View)
            }
            lastUpdatedAt = s.updatedAt
            if (!weatherShown) {
                metrics.visibility = View.VISIBLE
                enter(humidity.icon.parent as View, pressure.icon.parent as View, wind.icon.parent as View)
                weatherShown = true
            }
        }

        renderCoordinates(s.location)

        errorCard.visibility = if (s.error == null) View.GONE else View.VISIBLE
        s.error?.let { errorText.text = it }
    }

    private fun renderStatus(s: WeatherService.Status) {
        val text: Int
        val dot: Int
        when {
            !s.running -> { text = R.string.status_stopped; dot = R.color.status_stopped }
            s.error != null -> { text = R.string.status_running; dot = R.color.status_problem }
            s.weather == null -> { text = R.string.notif_starting; dot = R.color.status_running }
            else -> { text = R.string.status_running; dot = R.color.status_running }
        }
        statusText.setText(text)
        statusDot.background.mutate().setTint(ContextCompat.getColor(this, dot))
        setPulsing(s.running)
    }

    private fun renderCoordinates(location: Location?) {
        lastLocation = location
        if (location == null) {
            setText(coordsRow.value, getString(R.string.info_coordinates_none))
            coordsRow.setNote(null)
            return
        }
        setText(coordsRow.value, String.format(Locale.US, "%.5f, %.5f", location.latitude, location.longitude))
        coordsRow.setNote(
            if (location.hasAccuracy()) getString(R.string.info_accuracy, location.accuracy.roundToInt()) else null,
        )
    }

    /** Navigator and iGO folder rows; both open the settings on tap. */
    private fun renderSettingsInfo() {
        val pkg = prefs.navigatorPackage
        navigatorRow.value.text = when {
            pkg == null -> getString(R.string.info_not_set)
            else -> AppList.label(this, pkg) ?: getString(R.string.pref_package_missing, pkg)
        }
        navigatorRow.setNote(
            getString(if (prefs.launchNavigator) R.string.info_navigator_autostart else R.string.info_navigator_no_autostart),
        )

        val folder = prefs.igoFolder
        if (folder == null) {
            folderRow.value.text = getString(R.string.info_not_set)
            folderRow.setNote(null)
            return
        }
        folderRow.value.text = Prefs.treePath(folder)?.let { (volume, path) ->
            val storage = if (volume == "primary") getString(R.string.info_internal_storage)
            else getString(R.string.info_sd_card, volume)
            if (path.isEmpty()) storage else "$storage/$path"
        } ?: folder.path ?: folder.toString()
        folderRow.setNote(getString(if (prefs.allProfiles) R.string.info_profiles_all else R.string.info_profile_one))
    }

    /** One button that starts or stops the service; it changes look with a little bounce. */
    private fun renderToggle(isRunning: Boolean) {
        if (running == isRunning) return
        val first = running == null
        running = isRunning
        val update = {
            toggle.setText(if (isRunning) R.string.button_stop else R.string.button_start)
            toggle.setIconResource(if (isRunning) R.drawable.ic_stop else R.drawable.ic_play)
            styleToggle()
        }
        if (first || !motion) {
            update()
            return
        }
        Motion.animate(toggle, 120, scale = 0.92f).then {
            update()
            Motion.animate(toggle, 450, Motion.overshoot(3f), scale = 1f).start()
        }.start()
    }

    /** Start: solid white with the sky colour as text. Stop: glass with white text. */
    private fun styleToggle() {
        val white = 0xFFFFFFFF.toInt()
        val fg = if (running == true) white else palette.top
        val bg = if (running == true) ContextCompat.getColor(this, R.color.glass) else white
        toggle.backgroundTintList = ColorStateList.valueOf(bg)
        toggle.setTextColor(fg)
        toggle.iconTint = ColorStateList.valueOf(fg)
    }

    // ---- Animations ----

    /** Fades and slides views up into place, one after another. */
    private fun enter(vararg views: View) {
        if (!motion) return
        val shift = 40 * resources.displayMetrics.density
        views.forEachIndexed { i, v ->
            v.alpha = 0f
            v.translationY = shift
            Motion.animate(v, 700, Motion.enter, delayMs = 110L * i, alpha = 1f, translationY = 0f).start()
        }
    }

    /** A quick "pop" of the cards when fresh weather arrives. */
    private fun popCards(vararg views: View) {
        if (!motion) return
        views.forEachIndexed { i, v ->
            Motion.animate(v, 140, delayMs = 70L * i, scale = 0.93f).then {
                Motion.animate(v, 500, Motion.overshoot(2.5f), scale = 1f).start()
            }.start()
        }
    }

    /** Smoothly blends the background (and the system bars) into the new weather colours. */
    private fun applyPalette(target: Palette) {
        if (target == palette && paletteTween == null && shownColors[0] != 0) return
        val from = shownColors.copyOf()
        paletteTween?.cancel()
        palette = target
        styleToggle()
        if (!motion || from[0] == 0) {
            setBackgroundColors(target.top, target.bottom)
            return
        }
        val argb = ArgbEvaluator()
        paletteTween = Motion.Tween(1200, Motion.standard) { f ->
            setBackgroundColors(
                argb.evaluate(f, from[0], target.top) as Int,
                argb.evaluate(f, from[1], target.bottom) as Int,
            )
        }.then { paletteTween = null }.start()
    }

    @Suppress("DEPRECATION") // system bar colours still apply: the app opts out of edge-to-edge
    private fun setBackgroundColors(top: Int, bottom: Int) {
        shownColors[0] = top
        shownColors[1] = bottom
        background.colors = intArrayOf(top, bottom)
        window.statusBarColor = top
        window.navigationBarColor = bottom
        swipe.setColorSchemeColors(top)
    }

    /** Swaps the weather picture: the old one shrinks away, the new one pops in. */
    private fun showScene(target: Scene) {
        backdrop.scene = target
        if (scene.scene == target) return
        if (!motion || !scene.isLaidOut) {
            scene.scene = target
            return
        }
        Motion.animate(scene, 220, alpha = 0f, scale = 0.6f).then {
            scene.scene = target
            Motion.animate(scene, 650, Motion.overshoot(2f), alpha = 1f, scale = 1f).start()
        }.start()
    }

    /** Counts the temperature up or down to the new value. */
    private fun showTemperature(value: Int) {
        val from = shownTemp
        shownTemp = value
        tempTween?.cancel()
        if (from == value) return
        if (!motion) {
            temp.text = degrees(value)
            return
        }
        val start = from ?: 0
        tempTween = Motion.Tween(if (from == null) 1100L else 800L, Motion.enter) { f ->
            temp.text = degrees(Math.round(Motion.lerp(start.toFloat(), value.toFloat(), f)))
        }.start()
    }

    private fun degrees(value: Int) = "$value°"

    /** Turns the wind arrow the short way round, with an overshoot like a weather vane. */
    private fun pointWindArrow(rotation: Float) {
        val arrow = wind.icon
        if (!motion) {
            arrow.rotation = rotation
            return
        }
        val current = arrow.rotation
        val delta = ((rotation - current) % 360 + 540) % 360 - 180
        if (delta == 0f) return
        Motion.animate(arrow, 1400, Motion.overshoot(2.2f), rotation = current + delta).start()
    }

    /** Changes text with a quick fade when the screen is already showing something. */
    private fun setText(view: TextView, text: CharSequence) {
        if (view.text.toString() == text.toString()) return
        if (!motion || view.text.isNullOrEmpty() || !view.isShown) {
            view.text = text
            return
        }
        Motion.animate(view, 150, alpha = 0f).then {
            view.text = text
            Motion.animate(view, 300, alpha = 1f).start()
        }.start()
    }

    /** The status dot pulses while the service is running. */
    private fun setPulsing(on: Boolean) {
        if (on && motion && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            if (pulse != null) return
            pulse = Motion.Tween(800, Motion.smooth, loop = true) { f ->
                statusDot.scaleX = 1f + 0.6f * f
                statusDot.scaleY = statusDot.scaleX
                statusDot.alpha = 1f - 0.6f * f
            }.start()
        } else {
            pulse?.cancel()
            pulse = null
            statusDot.scaleX = 1f
            statusDot.scaleY = 1f
            statusDot.alpha = 1f
        }
    }

    // ---- Menu ----

    private fun openSettings() = startActivity(Intent(this, SettingsActivity::class.java))

    private fun openHelp() = startActivity(Intent(this, HelpActivity::class.java))

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_refresh -> { refresh(); true }
        R.id.action_settings -> { openSettings(); true }
        R.id.action_help -> { openHelp(); true }
        else -> super.onOptionsItemSelected(item)
    }

    private companion object {
        const val REFRESH_TIMEOUT_MS = 15_000L
    }
}
