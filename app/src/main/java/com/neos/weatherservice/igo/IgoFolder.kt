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

    /** Why nothing can be written, for the status line; null when the folder looks fine. */
    sealed class Problem {
        /** The folder permission was lost (e.g. after reinstalling) or the folder is gone. */
        object NoAccess : Problem()
        /** A required folder is missing, e.g. "save/profiles/01". */
        data class Missing(val path: String) : Problem()
    }

    fun problem(): Problem? {
        val r = root?.takeIf { it.exists() && it.canWrite() } ?: return Problem.NoAccess
        var dir: DocumentFile = r
        var path = ""
        for (name in listOf("save", "profiles", "01")) {
            path = if (path.isEmpty()) name else "$path/$name"
            dir = dir.findFile(name)?.takeIf { it.isDirectory } ?: return Problem.Missing(path)
        }
        return null
    }

    /**
     * `userlists` folders of the profiles 01..04 that exist. With [create], a profile that has
     * no `userlists` folder yet gets one (iGO creates it only once a user list is saved).
     */
    private fun userlists(allProfiles: Boolean, create: Boolean = false): List<DocumentFile> {
        val profiles = profiles() ?: return emptyList()
        val names = if (allProfiles) listOf("01", "02", "03", "04") else listOf("01")
        return names.mapNotNull { name ->
            val profile = profiles.findFile(name)?.takeIf { it.isDirectory } ?: return@mapNotNull null
            profile.findFile(USERLISTS)?.takeIf { it.isDirectory }
                ?: if (create) createDir(profile) else null
        }
    }

    private fun createDir(parent: DocumentFile): DocumentFile? = try {
        parent.createDirectory(USERLISTS).also { Log.i(TAG, "Created ${parent.name}/$USERLISTS: ${it != null}") }
    } catch (e: SecurityException) {
        Log.w(TAG, "Cannot create $USERLISTS", e)
        null
    }

    /** Writes [text] as UTF-16 into [fileName] of profile 01, or of every profile. Returns files written. */
    fun write(fileName: String, text: String, allProfiles: Boolean): Int {
        var written = 0
        for (dir in userlists(allProfiles, create = true)) {
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
        private const val USERLISTS = "userlists"
    }
}
