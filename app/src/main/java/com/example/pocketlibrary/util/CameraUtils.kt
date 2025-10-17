// util/CameraUtils.kt
package com.example.pocketlibrary.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * CameraUtils — tiny helper for camera + file handling.
 *
 *  it does:
 * 1) Creates a unique temp image file in your app's private Pictures/ directory.
 * 2) Converts that File to a content:// Uri using FileProvider
 * 3) Cleans up old photos you stored, to avoid filling storage.

 */
object CameraUtils {

    /**
     * Create a unique temp file for a new camera photo.
     * Example file name:
     * - JPEG_20231015_143022_.jpg
     */
    fun createImageFile(context: Context): File {
        // Build a timestamp like 20250210_101530 (yyyyMMdd_HHmmss)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        // Prefix "JPEG_20250210_101530_"
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)

        // Create a zero-byte temp file that we’ll pass to the camera
        return File.createTempFile(
            imageFileName,  // prefix
            ".jpg",         // suffix
            storageDir      // parent directory
        )
    }

    /**
     * Convert a File to a content:// Uri via FileProvider.
     */
    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider", // must match manifest provider "authorities"
            file
        )
    }

    /**
     *  cleanup for old photos you created
     * - Deletes files older than [daysOld] days in the app's Pictures/ directory.
     */
    fun cleanupOldPhotos(context: Context, daysOld: Int = 30) {
        try {
            val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
            storageDir?.listFiles()?.forEach { file ->
                val daysOldMillis = daysOld * 24 * 60 * 60 * 1000L
                val fileAge = System.currentTimeMillis() - file.lastModified()
                if (fileAge > daysOldMillis) {
                    file.delete() // best-effort; no crash if it fails
                }
            }
        } catch (e: Exception) {
            // Silently ignore
        }
    }
}

