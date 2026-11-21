package com.neos.weatherservice

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.neos.weatherservice.igo.IgoFolder
import com.neos.weatherservice.igo.IgoFormat
import com.neos.weatherservice.weather.CurrentWeather
import com.neos.weatherservice.weather.OpenMeteo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

/**
 * Foreground service that periodically downloads the weather for the current position and
 * writes it into the iGO profile (weather.txt), and does the same for the route destination
 * that the skin publishes in coordinates_fin.txt (weather_fin.txt).
 */
class WeatherService : Service() {

    data class Status(
        val running: Boolean = false,
        val weather: CurrentWeather? = null,
        val place: String = "",
        val location: Location? = null,
        val updatedAt: Long = 0L,
        val error: String? = null,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val refreshRequests = Channel<Unit>(Channel.CONFLATED)
    private lateinit var prefs: Prefs
    private lateinit var locationManager: LocationManager
    private var started = false
    private var lastLocation: Location? = null
    private var lastDestination: Pair<Double, Double>? = null

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val first = lastLocation == null
            lastLocation = location
            if (first) refreshRequests.trySend(Unit)
        }

        // Must be overridden: these have no default implementation before Android 11.
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) = Unit
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF && prefs.stopOnScreenOff) {
                Log.d(TAG, "Screen off, stopping")
                stopSelf()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_REFRESH) {
            refreshRequests.trySend(Unit)
            return START_STICKY
        }
        if (!hasLocationPermission(this)) {
            Log.w(TAG, "No location permission")
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, buildNotification(getString(R.string.notif_starting), null),
                if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
            )
        } catch (e: RuntimeException) {
            // Android 12+ refuses to start a foreground service from the background.
            Log.w(TAG, "Cannot start in foreground", e)
            stopSelf()
            return START_NOT_STICKY
        }
        if (!started) {
            started = true
            _status.value = Status(running = true)
            startLocationUpdates()
            ContextCompat.registerReceiver(
                this, screenReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            scope.launch { currentWeatherLoop() }
            scope.launch { destinationLoop() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        if (started) {
            locationManager.removeUpdates(locationListener)
            unregisterReceiver(screenReceiver)
            // Clear the files so the skin does not show stale weather after the service stops.
            val folder = folder()
            val all = prefs.allProfiles
            Thread {
                folder?.write(IgoFormat.WEATHER_FILE, "", all)
                folder?.write(IgoFormat.DESTINATION_WEATHER_FILE, "", all)
            }.start()
        }
        _status.value = Status(running = false)
        super.onDestroy()
    }

    // ---------------------------------------------------------------- location

    private fun startLocationUpdates() {
        for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            try {
                if (provider !in locationManager.allProviders) continue
                locationManager.getLastKnownLocation(provider)?.let { loc ->
                    if (lastLocation == null || loc.time > lastLocation!!.time) lastLocation = loc
                }
                locationManager.requestLocationUpdates(
                    provider, LOCATION_MIN_TIME_MS, LOCATION_MIN_DISTANCE_M, locationListener, Looper.getMainLooper(),
                )
            } catch (e: SecurityException) {
                Log.w(TAG, "Location permission revoked", e)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Provider $provider unavailable", e)
            }
        }
    }

    // ---------------------------------------------------------------- current weather

    private suspend fun currentWeatherLoop() {
        while (scope.isActive) {
            updateCurrentWeather()
            // Wait for the interval, or less if someone asks for a refresh.
            withTimeoutOrNull(prefs.intervalMs) { refreshRequests.receive() }
        }
    }

    private suspend fun updateCurrentWeather() {
        val loc = lastLocation
        if (loc == null) {
            publishError(getString(R.string.status_no_location))
            return
        }
        val units = prefs.units
        val weather = try {
            withContext(Dispatchers.IO) { OpenMeteo.fetch(loc.latitude, loc.longitude, units) }
        } catch (e: IOException) {
            Log.w(TAG, "Weather download failed", e)
            publishError(getString(R.string.status_no_internet))
            return
        }
        val description = WeatherText.description(this, weather.code.condition)
        val place = withContext(Dispatchers.IO) { WeatherText.place(this@WeatherService, loc.latitude, loc.longitude) }
        val line = IgoFormat.currentLine(weather, description)
        val folder = folder()
        val written = withContext(Dispatchers.IO) { folder?.write(IgoFormat.WEATHER_FILE, line, prefs.allProfiles) ?: 0 }
        Log.d(TAG, "weather.txt <- $line ($written files)")
        val error = if (written > 0) null else {
            when (val p = withContext(Dispatchers.IO) { folder?.problem() }) {
                is IgoFolder.Problem.Missing -> getString(R.string.status_folder_missing, p.path)
                else -> getString(R.string.status_no_folder) // no folder chosen, or access lost
            }
        }

        _status.value = Status(
            running = true, weather = weather, place = place, location = loc,
            updatedAt = System.currentTimeMillis(),
            error = error,
        )
        val title = "${IgoFormat.temperature(weather)}, $description"
        NotificationManagerCompat.from(this).notifySafely(buildNotification(title, place, weather))
        if (prefs.playSound) playSound()
    }

    private fun publishError(message: String) {
        _status.value = _status.value.copy(running = true, error = message)
    }

    // ---------------------------------------------------------------- destination weather

    private suspend fun destinationLoop() {
        while (scope.isActive) {
            updateDestinationWeather()
            delay(DESTINATION_POLL_MS)
        }
    }

    private suspend fun updateDestinationWeather() {
        val folder = folder() ?: return
        val text = withContext(Dispatchers.IO) { folder.read(IgoFormat.DESTINATION_COORDS_FILE) }
        val dest = text?.let(IgoFormat::parseCoordinates)
        if (dest == lastDestination) return
        if (dest == null) {
            withContext(Dispatchers.IO) { folder.write(IgoFormat.DESTINATION_WEATHER_FILE, "", prefs.allProfiles) }
            lastDestination = null
            return
        }
        try {
            val weather = withContext(Dispatchers.IO) { OpenMeteo.fetch(dest.first, dest.second, prefs.units) }
            val place = withContext(Dispatchers.IO) { WeatherText.place(this@WeatherService, dest.first, dest.second) }
            val line = IgoFormat.destinationLine(weather, place)
            withContext(Dispatchers.IO) { folder.write(IgoFormat.DESTINATION_WEATHER_FILE, line, prefs.allProfiles) }
            Log.d(TAG, "weather_fin.txt <- $line")
            lastDestination = dest // only after success, so a failure is retried on the next poll
        } catch (e: IOException) {
            Log.w(TAG, "Destination weather failed", e)
        }
    }

    // ---------------------------------------------------------------- helpers

    private fun folder(): IgoFolder? = prefs.igoFolder?.let { IgoFolder(this, it) }

    private fun playSound() {
        MediaPlayer.create(this, R.raw.ding)?.apply {
            setOnCompletionListener { it.release() }
            start()
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID, getString(R.string.notif_channel), NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String?, weather: CurrentWeather? = null) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_weather)
            .setContentTitle(title)
            .setContentText(text)
            .setLargeIcon(weather?.let { WeatherText.icon(this, it) })
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .build()

    private fun NotificationManagerCompat.notifySafely(n: android.app.Notification) {
        try {
            notify(NOTIFICATION_ID, n)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS denied on Android 13+; the service keeps working.
        }
    }

    companion object {
        private const val TAG = "WeatherService"
        private const val CHANNEL_ID = "weather"
        private const val NOTIFICATION_ID = 1
        private const val LOCATION_MIN_TIME_MS = 60_000L
        private const val LOCATION_MIN_DISTANCE_M = 200f
        private const val DESTINATION_POLL_MS = 30_000L
        private const val ACTION_REFRESH = "com.neos.weatherservice.REFRESH"

        private val _status = MutableStateFlow(Status())
        val status: StateFlow<Status> = _status.asStateFlow()

        fun hasLocationPermission(context: Context) =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        fun start(context: Context) =
            ContextCompat.startForegroundService(context, Intent(context, WeatherService::class.java))

        fun stop(context: Context) = context.stopService(Intent(context, WeatherService::class.java))

        fun refresh(context: Context) {
            if (_status.value.running) {
                context.startService(Intent(context, WeatherService::class.java).setAction(ACTION_REFRESH))
            }
        }

        /** Launches the selected iGO package. Returns false if it is not installed. */
        fun launchNavigator(context: Context, prefs: Prefs): Boolean {
            val pkg = prefs.navigatorPackage ?: return false
            val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return true
        }
    }
}
