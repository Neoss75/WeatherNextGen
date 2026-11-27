package com.neos.weatherservice

import android.content.Context
import android.content.SharedPreferences
import android.annotation.SuppressLint
import android.net.Uri
import android.provider.DocumentsContract
import androidx.preference.PreferenceManager
import com.neos.weatherservice.weather.Units

/** Typed access to the settings edited on the settings screen (keys match res/xml/preferences.xml). */
class Prefs(context: Context) {

    private val sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    var igoFolder: Uri?
        get() = sp.getString(KEY_FOLDER, null)?.let(Uri::parse)
        set(value) = sp.edit().putString(KEY_FOLDER, value?.toString()).apply()

    val allProfiles: Boolean get() = sp.getBoolean(KEY_ALL_PROFILES, false)

    val units: Units
        get() = if (sp.getString(KEY_UNITS, "metric") == "imperial") Units.IMPERIAL else Units.METRIC

    /** Update interval in milliseconds. */
    val intervalMs: Long
        get() = (sp.getString(KEY_INTERVAL, "30")?.toLongOrNull() ?: 30L).coerceAtLeast(10L) * 60_000L

    val playSound: Boolean get() = sp.getBoolean(KEY_SOUND, false)
    val stopOnScreenOff: Boolean get() = sp.getBoolean(KEY_STOP_SCREEN_OFF, false)
    val launchNavigator: Boolean get() = sp.getBoolean(KEY_LAUNCH, false)
    var navigatorPackage: String?
        get() = sp.getString(KEY_PACKAGE, null)
        set(value) = sp.edit().putString(KEY_PACKAGE, value).apply()

    companion object {
        /**
         * Storage volume and path of a folder picked with the system folder picker:
         * "primary:iGO_avic" -> ("primary", "iGO_avic"), "1234-ABCD:iGO" -> ("1234-ABCD", "iGO").
         */
        fun treePath(uri: Uri): Pair<String, String>? = if (uri.scheme == "file") filePath(uri.path) else try {
            val id = DocumentsContract.getTreeDocumentId(uri)
            id.substringBefore(':') to id.substringAfter(':', "")
        } catch (e: IllegalArgumentException) {
            null
        }

        /** The same for a plain path: "/storage/emulated/0/iGO" -> ("primary", "iGO"). */
        @SuppressLint("SdCardPath") // recognises paths typed by the user
        private fun filePath(path: String?): Pair<String, String>? {
            val p = path?.trimEnd('/') ?: return null
            for (internal in listOf("/storage/emulated/0", "/sdcard", "/mnt/sdcard")) {
                if (p == internal || p.startsWith("$internal/")) return "primary" to p.removePrefix(internal).trim('/')
            }
            if (p.startsWith("/storage/")) {
                val rest = p.removePrefix("/storage/")
                return rest.substringBefore('/') to rest.substringAfter('/', "")
            }
            return null
        }

        const val KEY_FOLDER = "igo_folder"
        const val KEY_ALL_PROFILES = "all_profiles"
        const val KEY_UNITS = "units"
        const val KEY_INTERVAL = "interval_min"
        const val KEY_SOUND = "play_sound"
        const val KEY_STOP_SCREEN_OFF = "stop_on_screen_off"
        const val KEY_LAUNCH = "launch_navigator"
        const val KEY_PACKAGE = "navigator_package"
        const val KEY_ANIMATIONS = "animations"
    }
}
