package io.github.xlopec.opengl.edu

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val MimeTypePng = "image/png"

suspend fun Context.saveBitmap(
    bitmap: Bitmap,
    filename: String,
    format: Bitmap.CompressFormat,
    mimeType: String,
    @IntRange(from = 0L, to = 100L) quality: Int = 100,
): Uri {
    require(quality in 0..100) { "invalid quality $quality" }
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        saveBitmapOnPreQ(bitmap, filename, format, quality)
    } else {
        saveBitmapOnQ(bitmap, format, quality, mimeType, filename)
    }
}

private suspend fun saveBitmapOnPreQ(
    bitmap: Bitmap,
    filename: String,
    format: Bitmap.CompressFormat,
    @IntRange(from = 0L, to = 100L) quality: Int,
): Uri = withContext(Dispatchers.IO) {
    val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), filename)

    try {
        FileOutputStream(file).use { fos ->
            check(bitmap.compress(format, quality, fos)) {
                "Couldn't save bitmap to $file"
            }
        }
    } catch (e: IOException) {
        file.delete()
        throw e
    }

    Uri.fromFile(file)
}

@RequiresApi(Build.VERSION_CODES.Q)
private suspend fun Context.saveBitmapOnQ(
    bitmap: Bitmap,
    format: Bitmap.CompressFormat,
    @IntRange(from = 0L, to = 100L) quality: Int,
    mimeType: String,
    displayName: String,
): Uri = withContext(Dispatchers.IO) {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
    }

    var uri: Uri? = null

    try {
        uri = requireNotNull(contentResolver.insert(Media.EXTERNAL_CONTENT_URI, values)) {
            "Failed to create new MediaStore record"
        }

        (contentResolver.openOutputStream(uri) ?: error("Failed to open output stream")).use { fos ->
            check(bitmap.compress(format, quality, fos)) {
                "Couldn't save bitmap $displayName to pictures dir"
            }
        }
    } catch (e: IOException) {
        uri?.let { orphanUri ->
            // Don't leave an orphan entry in the MediaStore
            contentResolver.delete(orphanUri, null, null)
        }
        throw e
    }

    uri
}