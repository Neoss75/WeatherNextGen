package com.neos.weatherservice

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import java.text.Collator

/** A launchable app on the device. */
data class AppEntry(val label: String, val packageName: String, val icon: Drawable? = null)

object AppList {

    /**
     * Known iGO packages first (in [known] order), then everything else alphabetically
     * by label, ignoring case and using the device language's ordering.
     */
    fun sort(apps: List<AppEntry>, known: List<String>, collator: Collator = Collator.getInstance()): List<AppEntry> {
        val rank = known.withIndex().associate { (i, p) -> p to i }
        val unique = apps.distinctBy { it.packageName }
        val (igo, other) = unique.partition { it.packageName in rank }
        return igo.sortedBy { rank.getValue(it.packageName) } +
            other.sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    /** Apps that have a launcher icon, except this one. Slow: call it off the main thread. */
    fun load(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION") // the flags overload needs API 33
        val activities = pm.queryIntentActivities(launcher, 0)
        val apps = activities
            .filter { it.activityInfo.packageName != context.packageName }
            .map { ri ->
                AppEntry(
                    label = ri.loadLabel(pm).toString(),
                    packageName = ri.activityInfo.packageName,
                    icon = ri.loadIcon(pm),
                )
            }
        val known = context.resources.getStringArray(R.array.navigator_packages).toList()
        return sort(apps, known)
    }

    /** "iGO Primo (com.nng.igo.primong)", or null if the package is not installed. */
    /** The app's name, or null if it is not installed. */
    fun label(context: Context, packageName: String): String? = try {
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    /** "Label (package)", or null if it is not installed. */
    fun describe(context: Context, packageName: String): String? =
        label(context, packageName)?.let { "$it ($packageName)" }
}
