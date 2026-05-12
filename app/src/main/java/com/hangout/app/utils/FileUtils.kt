package com.hangout.app.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Resolves a content:// URI to an absolute file path the OS can read directly.
 *
 * Strategy:
 *  1. Try the MediaStore DATA column (fast path — works for gallery picks on most devices).
 *  2. If that returns null (scoped storage, Downloads, Drive, etc.) copy the stream
 *     to a temp file in the app's cache dir and return that path instead.
 *
 * The returned path is always readable by File(path) and can be passed directly
 * to OkHttp's RequestBody.asRequestBody().
 */
fun getRealPathFromUri(context: Context, uri: Uri): String? {
    // ── 1. Fast path: MediaStore DATA column ──────────────────────────────
    if (uri.scheme == "content") {
        val dataPath = queryMediaStoreData(context, uri)
        if (!dataPath.isNullOrBlank()) return dataPath
    }

    // ── 2. file:// URI — already a real path ──────────────────────────────
    if (uri.scheme == "file") return uri.path

    // ── 3. Fallback: copy stream to cache file ────────────────────────────
    return copyUriToCache(context, uri)
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun queryMediaStoreData(context: Context, uri: Uri): String? {
    val projection = arrayOf(MediaStore.Images.Media.DATA)
    var cursor: Cursor? = null
    return try {
        cursor = context.contentResolver.query(uri, projection, null, null, null)
        cursor?.let {
            val col = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            if (it.moveToFirst()) it.getString(col) else null
        }
    } catch (_: Exception) {
        null
    } finally {
        cursor?.close()
    }
}

/**
 * Copies the content behind [uri] into a uniquely-named temp file under
 * [Context.cacheDir]/hangout_picks/ and returns its absolute path.
 * Returns null if the stream cannot be opened or the copy fails.
 */
fun copyUriToCache(context: Context, uri: Uri): String? {
    return try {
        val pickDir = File(context.cacheDir, "hangout_picks").apply { mkdirs() }
        val fileName = resolveFileName(context, uri) ?: "upload_${System.currentTimeMillis()}.jpg"
        val dest = File(pickDir, fileName)

        val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return null
        FileOutputStream(dest).use { out ->
            inputStream.use { it.copyTo(out) }
        }
        dest.absolutePath
    } catch (_: Exception) {
        null
    }
}

/** Reads the display name from the content resolver (used for the cache file name). */
private fun resolveFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) name = it.getString(idx)
        }
    }
    return name?.takeIf { it.isNotBlank() }
}

/**
 * Clears all files previously copied to the hangout_picks cache directory.
 * Call this from onDestroy or after an upload to avoid accumulating stale images.
 */
fun clearPickCache(context: Context) {
    try {
        File(context.cacheDir, "hangout_picks").deleteRecursively()
    } catch (_: Exception) { /* non-fatal */ }
}