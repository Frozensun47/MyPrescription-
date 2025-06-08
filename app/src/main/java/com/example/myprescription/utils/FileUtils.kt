package com.example.myprescription.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Saves a file from a content URI to the app's internal storage.
 * This is a general-purpose utility function.
 *
 * @param context The application context.
 * @param uri The content URI of the file to save.
 * @param prefix A prefix for the filename (e.g., "profile", "prescription").
 * @return The absolute path of the saved file, or an empty string if it fails.
 */
suspend fun saveFileToInternalStorage(context: Context, uri: Uri, prefix: String = "file"): String {
    return withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val mimeType = context.contentResolver.getType(uri)
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            val fileName = "${prefix}_${System.currentTimeMillis()}" + if (extension != null) ".$extension" else ""
            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            "" // Return empty string on failure
        }
    }
}