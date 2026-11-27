package com.neos.weatherservice.igo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.IOException

/**
 * Access to the iGO folder chosen by the user through the system folder picker
 * (Storage Access Framework). Works on every Android version from 5.0, including
 * Android 10+ where direct file paths on shared storage are no longer allowed.
 *
 * Some car head units ship without the system folder picker. There the folder is a plain
 * `file://` path, found by [findOnStorage] or typed in, and accessed with the storage permission.
 */
class IgoFolder(private val context: Context, treeUri: Uri) {

    private val root: DocumentFile? =
        if (treeUri.scheme == "file") treeUri.path?.let { DocumentFile.fromFile(File(it)) }
        else DocumentFile.fromTreeUri(context, treeUri)

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
                val file = dir.findFile(fileName) ?: createFile(dir, fileName) ?: continue
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

    /** A plain path gets the exact name (a raw DocumentFile would add an extension from the type). */
    private fun createFile(dir: DocumentFile, name: String): DocumentFile? {
        val path = dir.uri.path
        if (dir.uri.scheme != "file" || path == null) return dir.createFile("application/octet-stream", name)
        val file = File(path, name)
        return if (file.exists() || file.createNewFile()) DocumentFile.fromFile(file) else null
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

        /** Where to look for iGO folders when there is no system folder picker. */
        @SuppressLint("SdCardPath") // mount points of head units, not the app's own storage
        private val EXTRA_ROOTS = listOf(
            "/mnt/sdcard", "/mnt/sdcard2", "/mnt/extsd", "/mnt/external_sd", "/mnt/usbhost",
            "/mnt/usb_storage", "/sdcard2", "/extsd",
        )

        /** The package of the system folder picker, or null when the device has none. */
        fun pickerPackage(context: Context): String? {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            @Suppress("DEPRECATION") // the flags overload needs API 33
            val info = context.packageManager.resolveActivity(intent, 0) ?: return null
            return info.activityInfo?.packageName
        }

        /** True when [dir] holds `save/profiles`, like an iGO folder. */
        fun looksLikeIgo(dir: File): Boolean = File(dir, "save/profiles").isDirectory

        /** Internal storage, memory cards and USB drives that can be read. */
        fun storageRoots(context: Context): List<File> {
            val roots = mutableListOf<File>()
            @Suppress("DEPRECATION")
            roots += Environment.getExternalStorageDirectory()
            context.getExternalFilesDirs(null).filterNotNull().forEach {
                roots += File(it.absolutePath.substringBefore("/Android/"))
            }
            File("/storage").listFiles()?.filter { it.name != "self" && it.name != "emulated" }?.let { roots += it }
            EXTRA_ROOTS.forEach { roots += File(it) }
            return roots.filter { it.isDirectory && it.canRead() }.distinctBy { canonical(it) }
        }

        /** iGO folders on the storage roots, up to two levels deep (e.g. /storage/sdcard1/Navi/iGO). Slow. */
        fun findOnStorage(context: Context): List<File> {
            val found = mutableListOf<File>()
            fun scan(dir: File, depth: Int) {
                if (looksLikeIgo(dir)) {
                    found += dir
                    return
                }
                if (depth == 0) return
                dir.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") && it.name != "Android" }
                    ?.forEach { scan(it, depth - 1) }
            }
            storageRoots(context).forEach { scan(it, 2) }
            return found.distinctBy { canonical(it) }
        }

        private fun canonical(file: File): String = try {
            file.canonicalPath
        } catch (e: IOException) {
            file.absolutePath
        }
    }
}
