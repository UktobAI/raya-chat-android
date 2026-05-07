package ai.teammates.rayachat.ui.components.media

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import ai.teammates.rayachat.core.Constants
import ai.teammates.rayachat.core.adapters.AudioRecorderAdapter
import ai.teammates.rayachat.core.adapters.AudioResult
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.log10

/**
 * SDK-shipped audio recorder. Records WAV PCM 16 kHz mono 16-bit — the format the server's
 * transcription pipeline expects. Uses [AudioRecord] (raw PCM frames) and writes a 44-byte
 * RIFF/WAV header on stop. Cannot use `MediaRecorder` because it produces M4A/AAC, which
 * the server rejects.
 *
 * The recording file is written to [Context.cacheDir] and base64-encoded when
 * [stopRecording] returns. Orphaned files from prior crashes are purged on construction.
 *
 * To enable: declare `<uses-permission android:name="android.permission.RECORD_AUDIO"/>`
 * in the host app manifest and grant it at runtime. [makeIfAvailable] returns null if the
 * permission isn't declared — the SDK auto-hides the mic button in that case.
 */
class DefaultAudioRecorderAdapter private constructor(
    private val context: Context,
) : AudioRecorderAdapter {

    companion object {
        private const val TAG = "RayaChat.Recorder"

        // PCM s16le mono 16 kHz — match iOS settings exactly.
        private const val SAMPLE_RATE = 16_000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BITS_PER_SAMPLE = 16
        private const val NUM_CHANNELS: Short = 1

        private const val RECORDING_FILE_PREFIX = "raya-recording-"
        private const val RECORDING_FILE_SUFFIX = ".wav"

        /**
         * Returns a usable adapter or null if the host app didn't declare RECORD_AUDIO
         * in its merged manifest. The mic button is hidden by the SDK when this returns null.
         */
        fun makeIfAvailable(context: Context): DefaultAudioRecorderAdapter? {
            val pm = context.packageManager
            val declared = try {
                val info = pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
                info.requestedPermissions?.contains(Manifest.permission.RECORD_AUDIO) == true
            } catch (e: Exception) {
                Log.w(TAG, "Could not read host manifest permissions: ${e.message}")
                false
            }
            if (!declared) {
                Log.w(
                    TAG,
                    "RECORD_AUDIO not declared in AndroidManifest — voice notes disabled. " +
                        "Add <uses-permission android:name=\"android.permission.RECORD_AUDIO\"/> " +
                        "to your app manifest to enable.",
                )
                return null
            }
            return DefaultAudioRecorderAdapter(context.applicationContext).also { it.purgeOrphans() }
        }
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val isRecording = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)
    private var outputFile: File? = null
    private var fileOutput: RandomAccessFile? = null
    private var totalPcmBytes: Long = 0
    @Volatile private var lastPeakNormalized: Float = 0f
    private var maxDurationJob: Job? = null
    private val timerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override suspend fun startRecording() = withContext(Dispatchers.IO) {
        if (!isRecording.compareAndSet(false, true)) {
            Log.w(TAG, "startRecording called while already recording — ignored")
            return@withContext
        }

        // 1. Runtime permission check — manifest declaration alone isn't enough.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            isRecording.set(false)
            throw SecurityException("RECORD_AUDIO permission not granted at runtime")
        }

        // 2. Take audio focus (transient-may-duck — lets us record while music is playing).
        AudioFocusCoordinator.onTransientLoss = { isPaused.set(true) }
        AudioFocusCoordinator.enterRecording(context)

        // 3. Initialize AudioRecord.
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBuf <= 0) {
            cleanupOnError()
            throw IllegalStateException("AudioRecord.getMinBufferSize returned $minBuf — device unsupported")
        }
        val bufSize = (minBuf * 2).coerceAtLeast(4096)

        @SuppressLint("MissingPermission")
        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufSize,
        )
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            cleanupOnError()
            throw IllegalStateException("AudioRecord failed to initialize")
        }
        audioRecord = rec

        // 4. Open WAV file in cache, reserve placeholder for the 44-byte header.
        val file = File(context.cacheDir, "$RECORDING_FILE_PREFIX${UUID.randomUUID()}$RECORDING_FILE_SUFFIX")
        outputFile = file
        fileOutput = try {
            RandomAccessFile(file, "rw").apply {
                setLength(0)
                write(ByteArray(44))
            }
        } catch (e: IOException) {
            rec.release()
            cleanupOnError()
            throw IOException("Could not open recording file: ${e.message}", e)
        }
        totalPcmBytes = 0

        // 5. Start recording and reading thread.
        try {
            rec.startRecording()
        } catch (e: Exception) {
            cleanupOnError()
            throw IllegalStateException("AudioRecord.startRecording failed: ${e.message}", e)
        }
        isPaused.set(false)
        recordingThread = Thread { readLoop(bufSize) }.apply {
            name = "RayaChatAudioReader"
            isDaemon = true
            start()
        }

        // 6. Auto-stop at max duration.
        maxDurationJob = timerScope.launch {
            delay(Constants.MAX_AUDIO_DURATION_SECONDS * 1000L)
            try {
                stopRecording()
            } catch (_: Exception) {
                // Stop already in progress or already stopped — ignore.
            }
        }
    }

    private fun readLoop(bufSize: Int) {
        val buf = ByteArray(bufSize)
        val raf = fileOutput ?: return
        val rec = audioRecord ?: return
        var diagCounter = 0
        while (isRecording.get()) {
            if (isPaused.get()) {
                Thread.sleep(50)
                continue
            }
            val read = rec.read(buf, 0, buf.size)
            if (read <= 0) {
                Log.w(TAG, "AudioRecord.read returned $read — mic may be unavailable")
                continue
            }

            // Compute peak amplitude across this buffer (s16le, mono).
            var peak = 0
            var i = 0
            while (i + 1 < read) {
                val sample = (buf[i].toInt() and 0xFF) or ((buf[i + 1].toInt() and 0xFF) shl 8)
                val s = sample.toShort().toInt() // sign-extend
                val mag = if (s == Short.MIN_VALUE.toInt()) Short.MAX_VALUE.toInt() else abs(s)
                if (mag > peak) peak = mag
                i += 2
            }
            // Map to dB then linear-in-dB with -50 dB floor (mirror iOS).
            val peakDb = if (peak > 0) 20.0 * log10(peak / 32768.0) else -100.0
            val minDb = -50.0
            val normalized = ((peakDb - minDb) / -minDb).coerceIn(0.0, 1.0)

            // Verbose-only diagnostic: raw peak + normalized every ~1s so support can verify
            // mic capture. Doesn't show in default logcat — `adb logcat -v time *:V` to see.
            if (diagCounter++ % 10 == 0) {
                Log.v(TAG, "read=$read peakRaw=$peak peakDb=${"%.1f".format(peakDb)} normalized=${"%.3f".format(normalized)}")
            }
            lastPeakNormalized = normalized.toFloat()

            try {
                synchronized(raf) {
                    raf.write(buf, 0, read)
                    totalPcmBytes += read
                }
            } catch (e: IOException) {
                Log.e(TAG, "Write failed: ${e.message}")
                isRecording.set(false)
                break
            }
        }
    }

    override suspend fun stopRecording(): AudioResult = withContext(Dispatchers.IO) {
        if (!isRecording.compareAndSet(true, false)) {
            throw IllegalStateException("stopRecording called when not recording")
        }
        maxDurationJob?.cancel(); maxDurationJob = null
        recordingThread?.join(500); recordingThread = null
        audioRecord?.apply {
            try { stop() } catch (_: Exception) {}
            release()
        }
        audioRecord = null

        val raf = fileOutput ?: throw IllegalStateException("File output missing")
        val file = outputFile ?: throw IllegalStateException("Output file missing")

        // Backfill the WAV header now that totalPcmBytes is known.
        try {
            synchronized(raf) {
                raf.seek(0)
                raf.write(buildWavHeader(totalPcmBytes))
                raf.fd.sync()
            }
        } finally {
            try { raf.close() } catch (_: Exception) {}
            fileOutput = null
        }
        AudioFocusCoordinator.onTransientLoss = null
        AudioFocusCoordinator.exit(context)

        val totalSize = file.length()
        Log.v(TAG, "stopRecording: file=${file.name} bytes=$totalSize pcmBytes=$totalPcmBytes")
        if (totalSize < Constants.MIN_AUDIO_PAYLOAD_BYTES) {
            file.delete()
            outputFile = null
            throw IllegalStateException("Recording too short (${totalSize}B < ${Constants.MIN_AUDIO_PAYLOAD_BYTES}B)")
        }
        if (totalSize > Constants.MAX_AUDIO_PAYLOAD_BYTES) {
            file.delete()
            outputFile = null
            throw IllegalStateException("Recording too large (${totalSize}B > ${Constants.MAX_AUDIO_PAYLOAD_BYTES}B)")
        }

        val bytes = file.readBytes()
        val raw = Base64.encodeToString(bytes, Base64.NO_WRAP)
        // Keep file around for preview playback; cleanup() deletes it.
        AudioResult(
            uri = "data:audio/wav;base64,$raw",
            base64 = raw,
        )
    }

    override suspend fun pauseRecording() {
        isPaused.set(true)
    }

    override suspend fun resumeRecording() {
        isPaused.set(false)
    }

    override suspend fun getAmplitude(): Float = lastPeakNormalized

    override suspend fun cleanup() = withContext(Dispatchers.IO) {
        maxDurationJob?.cancel(); maxDurationJob = null
        if (isRecording.get()) {
            isRecording.set(false)
            recordingThread?.join(500); recordingThread = null
            audioRecord?.apply {
                try { stop() } catch (_: Exception) {}
                release()
            }
            audioRecord = null
            try { fileOutput?.close() } catch (_: Exception) {}
            fileOutput = null
        }
        outputFile?.takeIf { it.exists() }?.delete()
        outputFile = null
        AudioFocusCoordinator.onTransientLoss = null
        AudioFocusCoordinator.exit(context)
        Unit
    }

    private fun cleanupOnError() {
        isRecording.set(false)
        try { fileOutput?.close() } catch (_: Exception) {}
        outputFile?.takeIf { it.exists() }?.delete()
        outputFile = null
        fileOutput = null
        audioRecord = null
        AudioFocusCoordinator.onTransientLoss = null
        AudioFocusCoordinator.exit(context)
    }

    /** Drops orphaned recordings left over from a force-killed app. */
    private fun purgeOrphans() {
        try {
            context.cacheDir.listFiles { f ->
                f.name.startsWith(RECORDING_FILE_PREFIX) && f.name.endsWith(RECORDING_FILE_SUFFIX)
            }?.forEach { it.delete() }
        } catch (_: Exception) {
            // Best-effort cleanup.
        }
    }

    /**
     * Build a 44-byte little-endian RIFF/WAV header for the given PCM payload size.
     * Critical: never use `DataOutputStream.writeInt/Short` here — those write big-endian.
     * Spec: http://soundfile.sapp.org/doc/WaveFormat/
     */
    private fun buildWavHeader(pcmBytes: Long): ByteArray {
        val byteRate = SAMPLE_RATE * NUM_CHANNELS * BITS_PER_SAMPLE / 8
        val blockAlign: Short = (NUM_CHANNELS * BITS_PER_SAMPLE / 8).toShort()
        val totalDataLen = pcmBytes + 36
        return ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(totalDataLen.toInt())
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)                     // Subchunk1Size for PCM
            putShort(1)                    // AudioFormat = PCM
            putShort(NUM_CHANNELS)
            putInt(SAMPLE_RATE)
            putInt(byteRate)
            putShort(blockAlign)
            putShort(BITS_PER_SAMPLE.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(pcmBytes.toInt())
        }.array()
    }
}
