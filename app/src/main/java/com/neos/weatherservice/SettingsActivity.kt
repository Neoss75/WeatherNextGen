package com.neos.weatherservice

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.neos.weatherservice.igo.IgoFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        private lateinit var prefs: Prefs

        private val pickFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) onFolderPicked(uri)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            prefs = Prefs(requireContext())

            findPreference<Preference>(Prefs.KEY_FOLDER)?.apply {
                summary = folderSummary(prefs.igoFolder)
                setOnPreferenceClickListener {
                    pickFolder.launch(prefs.igoFolder)
                    true
                }
            }
            findPreference<Preference>(Prefs.KEY_PACKAGE)?.apply {
                summary = packageSummary()
                setOnPreferenceClickListener {
                    pickApp()
                    true
                }
            }
        }

        private fun packageSummary(): String {
            val pkg = prefs.navigatorPackage ?: return getString(R.string.pref_package_not_set)
            return AppList.describe(requireContext(), pkg) ?: getString(R.string.pref_package_missing, pkg)
        }

        /** Shows the launchable apps on the device, iGO builds first, and stores the chosen package. */
        private fun pickApp() {
            val context = requireContext()
            val loading = MaterialAlertDialogBuilder(context)
                .setTitle(R.string.pref_package)
                .setMessage(R.string.apps_loading)
                .show()
            lifecycleScope.launch {
                val apps = try {
                    withContext(Dispatchers.IO) { AppList.load(context) }
                } finally {
                    loading.dismiss()
                }
                val current = prefs.navigatorPackage
                val adapter = object : ArrayAdapter<AppEntry>(context, R.layout.item_app, apps) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = convertView ?: layoutInflater.inflate(R.layout.item_app, parent, false)
                        val app = getItem(position)!!
                        view.findViewById<ImageView>(R.id.app_icon).setImageDrawable(app.icon)
                        view.findViewById<TextView>(R.id.app_label).text =
                            if (app.packageName == current) getString(R.string.app_selected, app.label) else app.label
                        view.findViewById<TextView>(R.id.app_package).text = app.packageName
                        return view
                    }
                }
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.pref_package)
                    .setAdapter(adapter) { _, which ->
                        prefs.navigatorPackage = apps[which].packageName
                        findPreference<Preference>(Prefs.KEY_PACKAGE)?.summary = packageSummary()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }

        private fun onFolderPicked(uri: Uri) {
            val context = requireContext()
            // Keep access to the folder after a reboot.
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            val ok = IgoFolder(context, uri).isValid()
            Toast.makeText(context, if (ok) R.string.toast_folder_ok else R.string.toast_folder_bad, Toast.LENGTH_LONG)
                .show()
            if (ok) {
                prefs.igoFolder = uri
                findPreference<Preference>(Prefs.KEY_FOLDER)?.summary = folderSummary(uri)
            }
        }

        /** "primary:iGO_avic" -> "iGO_avic" (internal storage) or "1234-ABCD:iGO" -> "SD 1234-ABCD/iGO". */
        private fun folderSummary(uri: Uri?): String {
            if (uri == null) return getString(R.string.pref_folder_not_set)
            val (volume, path) = Prefs.treePath(uri) ?: return uri.toString()
            return if (volume == "primary") "/$path" else "$volume/$path"
        }
    }
}
