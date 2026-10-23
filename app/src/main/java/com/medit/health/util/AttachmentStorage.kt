package com.medit.health.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object AttachmentStorage {

    fun saveBitmap(context: Context, bitmap: Bitmap): String {
        return try {
            val dir = File(context.filesDir, "prescriptions")
            if (!dir.exists()) dir.mkdirs()

            val fileName = "rx_${UUID.randomUUID().toString().take(8)}.jpg"
            val file = File(dir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            ""
        }
    }

    fun copyUriToInternalStorage(context: Context, uri: Uri): String {
        return try {
            val dir = File(context.filesDir, "prescriptions")
            if (!dir.exists()) dir.mkdirs()

            val extension = context.contentResolver.getType(uri)?.let { mime ->
                when {
                    mime.contains("pdf") -> ".pdf"
                    mime.contains("png") -> ".png"
                    else -> ".jpg"
                }
            } ?: ".jpg"

            val fileName = "doc_${UUID.randomUUID().toString().take(8)}$extension"
            val destFile = File(dir, fileName)

            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            ""
        }
    }
}
