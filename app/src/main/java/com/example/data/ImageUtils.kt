package com.example.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class GalleryFailureReason {
    INSERT_FAILED,
    STREAM_OPEN_FAILED,
    WRITE_FAILED,
    FINALIZE_FAILED,
    TIMEOUT,
    UNKNOWN
}

sealed class GallerySaveResult {
    data class Success(val uri: Uri) : GallerySaveResult()
    data class Failure(val reason: GalleryFailureReason) : GallerySaveResult()
}

suspend fun savePreparedJpegToGallery(
    context: Context,
    jpegBytes: ByteArray,
    displayName: String
): GallerySaveResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        kotlinx.coroutines.withTimeout(6000L) {
            val filename = "$displayName.jpg"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ScreenPilot")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = try {
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            } catch (e: Exception) {
                Log.e("ImageUtils", "Failed to insert into MediaStore: ${e.message}", e)
                null
            } ?: return@withTimeout GallerySaveResult.Failure(GalleryFailureReason.INSERT_FAILED)

            var outputStreamOpenFailed = false
            try {
                val outputStream = resolver.openOutputStream(uri)
                if (outputStream == null) {
                    outputStreamOpenFailed = true
                    throw java.io.IOException("Output stream was null")
                }
                outputStream.use { os ->
                    os.write(jpegBytes)
                    os.flush()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val updatedRows = resolver.update(
                        uri,
                        ContentValues().apply {
                            put(MediaStore.Images.Media.IS_PENDING, 0)
                        },
                        null,
                        null
                    )
                    if (updatedRows <= 0) {
                        throw java.lang.IllegalStateException("Finalization failed - no rows updated")
                    }
                }
                GallerySaveResult.Success(uri)
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (e: Exception) {
                Log.e("ImageUtils", "Failed to write JPEG bytes to gallery: ${e.message}", e)
                try {
                    resolver.delete(uri, null, null)
                } catch (ex: Exception) {
                    // ignore
                }
                val reason = when {
                    outputStreamOpenFailed -> GalleryFailureReason.STREAM_OPEN_FAILED
                    e is java.lang.IllegalStateException -> GalleryFailureReason.FINALIZE_FAILED
                    else -> GalleryFailureReason.WRITE_FAILED
                }
                GallerySaveResult.Failure(reason)
            }
        }
    } catch (te: kotlinx.coroutines.TimeoutCancellationException) {
        Log.e("ImageUtils", "Gallery save timed out", te)
        GallerySaveResult.Failure(GalleryFailureReason.TIMEOUT)
    } catch (ce: kotlinx.coroutines.CancellationException) {
        throw ce
    } catch (e: Exception) {
        Log.e("ImageUtils", "Gallery save failed with unexpected error", e)
        GallerySaveResult.Failure(GalleryFailureReason.UNKNOWN)
    }
}

// Phase 11: prepareImageForApi (which produced a legacy data:image/jpeg;base64, string)
// has been removed. Production capture uses encodeGeminiInlineImage in ProviderGateway.
