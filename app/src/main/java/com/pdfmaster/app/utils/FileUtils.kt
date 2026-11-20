package com.pdfmaster.app.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object FileUtils {

    fun copyUriToCache(context: Context, uri: Uri, name: String = "input_${System.currentTimeMillis()}.pdf"): File {
        val file = File(context.cacheDir, name)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Cannot open URI")
        return file
    }

    fun saveToDownloads(context: Context, file: File, displayName: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { output ->
                    FileInputStream(file).use { input -> input.copyTo(output) }
                }
            }
            uri
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            val dest = File(dir, displayName)
            FileInputStream(file).use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
            Uri.fromFile(dest)
        }
    }

    fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
        }
    }
}
