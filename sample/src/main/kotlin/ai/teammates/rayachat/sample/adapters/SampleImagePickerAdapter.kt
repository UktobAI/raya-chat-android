package ai.teammates.rayachat.sample.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import ai.teammates.rayachat.core.models.ImageAsset
import ai.teammates.rayachat.ui.adapters.ImagePickerAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min

private const val TAG = "RayaChat.ImagePicker"
private const val MAX_DIMENSION = 1024 // Max width or height — matches web widget behavior
private const val JPEG_QUALITY = 70    // Compression quality

class SampleImagePickerAdapter private constructor(
    private val context: Context,
) : ImagePickerAdapter {

    private var launcher: ActivityResultLauncher<PickVisualMediaRequest>? = null
    private var pendingCallback: ((List<Uri>) -> Unit)? = null

    fun register(activity: ComponentActivity) {
        launcher = activity.registerForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia()
        ) { uris ->
            pendingCallback?.invoke(uris)
            pendingCallback = null
        }
    }

    override suspend fun pickImages(maxCount: Int): List<ImageAsset> {
        val currentLauncher = launcher ?: return emptyList()

        return suspendCancellableCoroutine { continuation ->
            pendingCallback = { uris ->
                val assets = uris.take(maxCount).mapNotNull { uri ->
                    uriToImageAsset(uri)
                }
                Log.d(TAG, "Picked ${assets.size} images")
                continuation.resume(assets)
            }

            continuation.invokeOnCancellation { pendingCallback = null }

            currentLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    private fun uriToImageAsset(uri: Uri): ImageAsset? {
        return try {
            // Step 1: Decode bitmap dimensions without loading full bitmap
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            Log.d(TAG, "Original image: ${origWidth}x${origHeight}")

            // Step 2: Calculate sample size to reduce memory usage
            val sampleSize = calculateSampleSize(origWidth, origHeight, MAX_DIMENSION)
            Log.d(TAG, "Sample size: $sampleSize")

            // Step 3: Decode with sample size
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return null

            // Step 4: Scale down further if needed
            val scaledBitmap = scaleBitmap(bitmap, MAX_DIMENSION)
            Log.d(TAG, "Scaled image: ${scaledBitmap.width}x${scaledBitmap.height}")

            // Step 5: Compress to JPEG
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
            val bytes = outputStream.toByteArray()
            val base64Str = Base64.encodeToString(bytes, Base64.NO_WRAP)

            Log.d(TAG, "Base64 length: ${base64Str.length} chars (~${bytes.size / 1024}KB)")

            // Recycle bitmaps
            if (scaledBitmap != bitmap) bitmap.recycle()
            scaledBitmap.recycle()

            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "image.jpg"
            val mimeType = "image/jpeg" // Always JPEG after compression

            ImageAsset(
                uri = uri.toString(),
                name = fileName,
                type = mimeType,
                base64 = "data:$mimeType;base64,$base64Str",
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process image: ${e.message}")
            null
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var sampleSize = 1
        val larger = max(width, height)
        if (larger > maxDim) {
            sampleSize = larger / maxDim
        }
        return max(1, sampleSize)
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val larger = max(width, height)

        if (larger <= maxDim) return bitmap

        val scale = maxDim.toFloat() / larger
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    companion object {
        fun create(activity: ComponentActivity): SampleImagePickerAdapter {
            val adapter = SampleImagePickerAdapter(activity.applicationContext)
            adapter.register(activity)
            return adapter
        }
    }
}
