package com.neos.weatherservice.igo

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.IOException

/**
 * Access to the iGO folder chosen by the user through the system folder picker
 * (Storage Access Framework). Works on every Android version from 5.0, including
 * Android 10+ where direct file paths on shared storage are no longer allowed.
 */
class IgoFolder(private val context: Context, treeUri: Uri) {

    private val root: DocumentFile? = DocumentFile.fromTreeUri(context, treeUri)

    /** True when the chosen folder contains `save/profiles`, i.e. looks like an iGO folder. */
    fun isValid(): Boolean = profiles() != null

    private fun profiles(): DocumentFile? =
        root?.findFile("save")?.findFile("profiles")?.takeIf { it.isDirectory }

    /** `userlists` folders of the profiles 01..04 that exist. */
    private fun userlists(allProfiles: Boolean): List<DocumentFile> {
        val profiles = profiles() ?: return emptyList()
        val names = if (allProfiles) listOf("01", "02", "03", "04") else listOf("01")
        return names.mapNotNull { profiles.findFile(it)?.findFile("userlists")?.takeIf { d -> d.isDirectory } }
    }

    /** Writes [text] as UTF-16 into [fileName] of profile 01, or of every profile. Returns files written. */
    fun write(fileName: String, text: String, allProfiles: Boolean): Int {
        var written = 0
        for (dir in userlists(allProfiles)) {
            try {
                val file = dir.findFile(fileName) ?: dir.createFile("application/octet-stream", fileName)
                    ?: continue
                // "wt" truncates the old content.
                context.contentResolver.openOutputStream(file.uri, "wt")?.use {
                    it.write(text.toByteArray(Charsets.UTF_16))
                    written++
                }
            } catch (e: IOException) {
                Log.w(TAG, "Cannot write $fileName", e)
            } catch (e: SecurityException) {
                Log.w(TAG, "No access to $fileName", e)
            }
        }
        return written
    }

    /** Reads a UTF-16 file from profile 01, or null if it is missing or empty. */
    fun read(fileName: String): String? {
        val file = userlists(false).firstOrNull()?.findFile(fileName) ?: return null
        return try {
            context.contentResolver.openInputStream(file.uri)?.use {
                it.readBytes().toString(Charsets.UTF_16)
            }?.takeIf { it.isNotBlank() }
        } catch (e: IOException) {
            Log.w(TAG, "Cannot read $fileName", e)
            null
        } catch (e: SecurityException) {
            Log.w(TAG, "No access to $fileName", e)
            null
        }
    }

    companion object {
        private const val TAG = "IgoFolder"
    }
}
