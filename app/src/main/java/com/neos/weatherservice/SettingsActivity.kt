package com.neos.weatherservice

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.neos.weatherservice.igo.IgoFolder

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
            findPreference<ListPreference>(Prefs.KEY_PACKAGE)?.setOnPreferenceChangeListener { _, value ->
                val installed = requireContext().packageManager.getLaunchIntentForPackage(value as String) != null
                if (!installed) Toast.makeText(requireContext(), R.string.toast_no_app, Toast.LENGTH_SHORT).show()
                true
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
            val id = try {
                DocumentsContract.getTreeDocumentId(uri)
            } catch (e: IllegalArgumentException) {
                return uri.toString()
            }
            val volume = id.substringBefore(':')
            val path = id.substringAfter(':', "")
            return if (volume == "primary") "/$path" else "$volume/$path"
        }
    }
}
