package ai.teammates.rayachat.ui.components.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import ai.teammates.rayachat.core.adapters.AudioInfo
import ai.teammates.rayachat.core.adapters.AudioPlayerAdapter
import ai.teammates.rayachat.core.network.HttpClientProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import kotlin.math.abs

/**
 * SDK-shipped audio player. Plays back local data URIs (post-record preview), local file
 * paths, and remote `https://` URLs (post-attachment-swap from the server). Uses
 * [MediaPlayer] for simplicity; ExoPlayer overkill for short voice notes.
 *
 * [getAmplitudes] scans the underlying WAV PCM for peak-per-bucket amplitudes to drive
 * the in-bubble waveform visualization. Returns null for non-WAV codecs (the UI degrades
 * to a dashed-line-only view).
 *
 * Each audio bubble owns its own player instance — playing one auto-pauses the others
 * via the OS audio-focus mechanism.
 */
class DefaultAudioPlayerAdapter(
    private val context: Context,
) : AudioPlayerAdapter {

    companion object {
        private const val TAG = "RayaChat.Player"
        private const val PLAYBACK_FILE_PREFIX = "raya-play-"
        private const val PLAYBACK_FILE_SUFFIX = ".wav"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var cachedFile: File? = null

    override suspend fun loadAudio(uri: String): AudioInfo = withContext(Dispatchers.IO) {
        cleanup()

        val file: File = when {
            uri.startsWith("data:") -> {
                val raw = uri.substringAfter(',', "")
                val bytes = Base64.decode(raw, Base64.DEFAULT)
                File(context.cacheDir, "$PLAYBACK_FILE_PREFIX${UUID.randomUUID()}$PLAYBACK_FILE_SUFFIX")
                    .apply { writeBytes(bytes) }
            }
            uri.startsWith("http://") || uri.startsWith("https://") -> downloadToCache(uri)
            uri.startsWith("file://") -> File(java.net.URI(uri))
            else -> File(uri)
        }
        cachedFile = file

        val mp = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            setDataSource(file.absolutePath)
            prepare()
        }
        mediaPlayer = mp
        AudioInfo(durationMs = mp.duration.toLong())
    }

    override suspend fun play() = withContext(Dispatchers.IO) {
        AudioFocusCoordinator.enterPlayback(context)
        try { mediaPlayer?.start() } catch (e: Exception) { Log.w(TAG, "play failed: ${e.message}") }
        Unit
    }

    override suspend fun pause() = withContext(Dispatchers.IO) {
        try { mediaPlayer?.pause() } catch (_: Exception) {}
        Unit
    }

    override suspend fun seekTo(positionMs: Long) = withContext(Dispatchers.IO) {
        try { mediaPlayer?.seekTo(positionMs.toInt()) } catch (_: Exception) {}
        Unit
    }

    override suspend fun getPosition(): Long = withContext(Dispatchers.IO) {
        try { mediaPlayer?.currentPosition?.toLong() ?: 0L } catch (_: Exception) { 0L }
    }

    override suspend fun cleanup() = withContext(Dispatchers.IO) {
        mediaPlayer?.apply {
            try { stop() } catch (_: Exception) {}
            try { release() } catch (_: Exception) {}
        }
        mediaPlayer = null
        cachedFile?.takeIf { it.name.startsWith(PLAYBACK_FILE_PREFIX) }?.delete()
        cachedFile = null
        AudioFocusCoordinator.exit(context)
        Unit
    }

    /**
     * Read the WAV PCM directly to compute peak-per-bucket amplitudes. Returns null
     * when the source isn't WAV (server may transcode to MP3/AAC) or when the file
     * isn't ready — UI falls back to a dashed line in those cases.
     */
    override suspend fun getAmplitudes(sampleCount: Int): FloatArray? = withContext(Dispatchers.IO) {
        if (sampleCount <= 0) return@withContext null
        val file = cachedFile ?: return@withContext null
        if (!file.exists() || file.length() <= 44) return@withContext null

        runCatching {
            FileInputStream(file).use { input ->
                val header = ByteArray(44)
                if (input.read(header) != 44) return@withContext null
                if (String(header, 0, 4, Charsets.US_ASCII) != "RIFF" ||
                    String(header, 8, 4, Charsets.US_ASCII) != "WAVE"
                ) {
                    // Not a WAV file — caller will fall back to dashed-line UI.
                    return@withContext null
                }

                val pcmLength = file.length() - 44
                val totalSamples = (pcmLength / 2).toInt()
                if (totalSamples <= 0) return@withContext null
                val bucketSize = (totalSamples / sampleCount).coerceAtLeast(1)
                val out = FloatArray(sampleCount)
                val buf = ByteArray(bucketSize * 2)
                var i = 0
                while (i < sampleCount) {
                    val read = input.read(buf)
                    if (read <= 0) break
                    var peak = 0
                    var j = 0
                    while (j + 1 < read) {
                        val sample = (buf[j].toInt() and 0xFF) or ((buf[j + 1].toInt() and 0xFF) shl 8)
                        val s = sample.toShort().toInt()
                        val mag = if (s == Short.MIN_VALUE.toInt()) Short.MAX_VALUE.toInt() else abs(s)
                        if (mag > peak) peak = mag
                        j += 2
                    }
                    out[i] = (peak / 32768f).coerceIn(0f, 1f)
                    i++
                }
                out
            }
        }.getOrNull()
    }

    /** Downloads a remote audio URL into [Context.cacheDir] using the SDK's shared OkHttp client. */
    private fun downloadToCache(url: String): File {
        val response = HttpClientProvider.client.newCall(Request.Builder().url(url).build()).execute()
        if (!response.isSuccessful) {
            response.close()
            throw java.io.IOException("Download failed: HTTP ${response.code}")
        }
        val ext = url.substringAfterLast('.', "wav").substringBefore('?').take(4)
        val file = File(context.cacheDir, "$PLAYBACK_FILE_PREFIX${UUID.randomUUID()}.$ext")
        response.body?.byteStream()?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        response.close()
        return file
    }
}
