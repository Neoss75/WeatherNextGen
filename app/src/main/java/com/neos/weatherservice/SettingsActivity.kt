package com.neos.weatherservice

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.EditText
import android.widget.FrameLayout
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.neos.weatherservice.igo.IgoFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

    private companion object {
        const val TAG = "Settings"
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        private lateinit var prefs: Prefs

        private val pickFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) onFolderPicked(uri)
        }

        private val storagePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            if (ok) showFoundFolders()
            else Toast.makeText(requireContext(), R.string.toast_storage_denied, Toast.LENGTH_LONG).show()
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            prefs = Prefs(requireContext())
            findPreference<Preference>("help")?.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), HelpActivity::class.java))
                true
            }
            findPreference<Preference>("folder_picker")?.apply {
                summary = pickerSummary()
                setOnPreferenceClickListener {
                    startActivity(Intent(requireContext(), HelpActivity::class.java))
                    true
                }
            }

            findPreference<Preference>(Prefs.KEY_FOLDER)?.apply {
                summary = folderSummary(prefs.igoFolder)
                setOnPreferenceClickListener {
                    chooseFolder()
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

        /** Whether the system folder picker exists: many head units ship without it. */
        private fun pickerSummary(): String {
            val pkg = IgoFolder.pickerPackage(requireContext())
            return when {
                pkg != null -> getString(R.string.pref_picker_yes, pkg)
                Build.VERSION.SDK_INT >= 30 -> getString(R.string.pref_picker_no_way)
                else -> getString(R.string.pref_picker_list)
            }
        }

        /** The system folder picker, or our own list where a head unit has none. */
        private fun chooseFolder() {
            if (IgoFolder.pickerPackage(requireContext()) == null) return chooseFolderWithoutPicker()
            try {
                pickFolder.launch(prefs.igoFolder?.takeIf { it.scheme != "file" })
            } catch (e: ActivityNotFoundException) {
                Log.w(TAG, "No system folder picker", e)
                chooseFolderWithoutPicker()
            }
        }

        private fun chooseFolderWithoutPicker() {
            val context = requireContext()
            when {
                Build.VERSION.SDK_INT >= 30 ->
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.pref_folder)
                        .setMessage(R.string.folder_no_picker)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED -> storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                else -> showFoundFolders()
            }
        }

        /** iGO folders found on the internal storage, memory cards and USB drives, plus "enter the path". */
        private fun showFoundFolders() {
            val context = requireContext()
            lifecycleScope.launch {
                val found = withContext(Dispatchers.IO) { IgoFolder.findOnStorage(context) }
                val items = found.map { it.absolutePath } + getString(R.string.folder_enter_path)
                MaterialAlertDialogBuilder(context)
                    .setTitle(if (found.isEmpty()) R.string.folder_none_found else R.string.folder_found_title)
                    .setItems(items.toTypedArray()) { _, which ->
                        if (which < found.size) onFolderPicked(Uri.fromFile(found[which])) else enterPath()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }

        private fun enterPath() {
            val context = requireContext()
            @Suppress("DEPRECATION")
            val start = prefs.igoFolder?.takeIf { it.scheme == "file" }?.path
                ?: (Environment.getExternalStorageDirectory().absolutePath + "/")
            val input = EditText(context).apply {
                setSingleLine()
                setText(start)
                setSelection(start.length)
            }
            val pad = (20 * resources.displayMetrics.density).toInt()
            val box = FrameLayout(context).apply {
                setPadding(pad, pad / 2, pad, 0)
                addView(input)
            }
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.folder_path_title)
                .setMessage(R.string.folder_path_hint)
                .setView(box)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val path = input.text.toString().trim().trimEnd('/')
                    if (path.isNotEmpty()) onFolderPicked(Uri.fromFile(File(path)))
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        private fun onFolderPicked(uri: Uri) {
            val context = requireContext()
            if (uri.scheme != "file") {
                // Keep access to the folder after a reboot. Some head units hand out folders
                // that cannot be kept; the folder then works until the next reboot.
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                } catch (e: SecurityException) {
                    Log.w(TAG, "Cannot keep access to $uri", e)
                }
            }
            // A broken storage provider on a head unit must not take the settings down with it.
            val ok = try {
                IgoFolder(context, uri).isValid()
            } catch (e: RuntimeException) {
                Log.w(TAG, "Cannot read $uri", e)
                false
            }
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
            val (volume, path) = Prefs.treePath(uri) ?: return uri.path ?: uri.toString()
            return if (volume == "primary") "/$path" else "$volume/$path"
        }
    }
}
