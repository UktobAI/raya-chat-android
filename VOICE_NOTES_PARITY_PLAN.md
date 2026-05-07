---

# Android Voice Notes — Implementation Guide for iOS Parity (v0.1.2)

## 1. Goal

Bring the **Raya Chat Android SDK** to feature parity with the **iOS SDK v0.1.2** voice-note implementation. The iOS SDK now ships a complete record → preview → send → playback pipeline using **WAV PCM 16 kHz mono 16-bit** audio, **binary WebSocket frames**, real-time waveform visualization, and a built-in default recorder/player. Android currently has only stub adapter interfaces, a placeholder recorder UI, and a critically broken `sendAudio()` path that transmits base64 as a **text** WebSocket frame (silently dropped server-side). This document is the full spec for the Android dev to close the gap and ship `raya-chat-android` v0.1.2.

---

## 2. Current Android State

**What exists.** `core` and `ui` Gradle modules are already wired with Compose (BOM), OkHttp 4 WebSocket, Kotlin Serialization, Room, and `lifecycle-viewmodel-compose`. Adapter interfaces (`AudioRecorderAdapter`, `AudioPlayerAdapter`) live in `raya-chat-core/.../adapters/` with `AudioResult(uri, base64?)` and `AudioInfo(durationMs)`. The UI module re-exports them via `typealias`. A skeleton `AudioRecorderUI.kt` and `AudioPlayerUI.kt` exist in `raya-chat-ui/.../components/media/`. `AudioData(type, audioUrls)` model is serializable; `TypeMessage` has `audioJson` column in Room. `MessageHandler.handleResponse()` already parses `audio_urls` from `ResponseData` and dispatches `onAttachments(listOf(audioUrls), "audio")`. `RayaChatClient.onAttachments` correctly merges remote audio URLs into the latest user audio message and fires `onMessageUpdate`. `RayaChatWidget` and `RayaChatViewModel` accept an `audioRecorderAdapter` parameter and forward it to `ChatScreen`.

**What's missing.** No `DefaultAudioRecorderAdapter` or `DefaultAudioPlayerAdapter` implementations — adapters are interfaces only. No `AudioPreviewUI` (the post-stop "play back, then send or cancel" stage). No `AudioSessionCoordinator` equivalent for `AudioManager`/`AudioFocusRequest`. `MessageBubble.kt` has zero handling for `message.type == 2` (audio messages never render). `MessageComposer` has the right plumbing but `ChatScreen` wires the mic button to `onMicPress = { /* TODO: Show AudioRecorderUI overlay */ }`. No `RECORD_AUDIO` permission anywhere in the manifest. `RayaChatWidget` exposes only `audioRecorderAdapter` — no `audioPlayerAdapter` parameter exists.

**What's different / broken.** `RayaChatClient.sendAudio(base64: String)` calls `wsManager?.send(base64)` — this routes through `WebSocketManager.send(String)` which calls `WebSocket.send(text: String)`. **OkHttp is sending the base64 payload as a text frame**, which is exactly the bug iOS fixed in v0.1.2. The server-side handler routes binary frames to OpenAI Whisper; text frames are parsed as JSON commands and silently dropped. `WebSocketManager` has no `sendBinary(ByteArray)` method. The skeleton `AudioRecorderUI` records via the adapter but only shows timer + cancel + pause + send — no waveform, no monospaced timer, no preview stage. Constants module is missing `MAX_AUDIO_DURATION_SECONDS`, `MAX_AUDIO_PAYLOAD_BYTES`, `MIN_AUDIO_PAYLOAD_BYTES`. `AudioRecorderAdapter` interface is missing `getAmplitudes(sampleCount: Int): FloatArray?` (added on iOS for the playback waveform).

**Build & framework.** Kotlin + Jetpack Compose, `compileSdk = 35`, `minSdk = 24`, JVM 17. OkHttp 4 WebSocket is the transport (`okhttp3.WebSocket.send(ByteString)` is available). Compose Material3 is the UI toolkit. Module names: `raya-chat-core` (no Compose), `raya-chat-ui` (Compose).

---

## 3. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│  raya-chat-ui                                                       │
│                                                                     │
│  ┌────────────────┐    ┌─────────────────┐   ┌──────────────────┐   │
│  │ ChatScreen     │───▶│ AudioRecorderUI │──▶│ AudioPreviewUI   │   │
│  │ (audioFlow:    │    │ (Compose)       │   │ (Compose, NEW)   │   │
│  │  3-state FSM)  │    │ live waveform   │   │ static waveform  │   │
│  └────────┬───────┘    └────────┬────────┘   └─────────┬────────┘   │
│           │                     │                       │           │
│           ▼                     ▼                       ▼           │
│  ┌────────────────┐    ┌────────────────────────────────────────┐   │
│  │ MessageBubble  │    │ DefaultAudioRecorderAdapter (NEW)      │   │
│  │ type==2 →      │    │   AudioRecord (PCM s16le 16kHz mono)   │   │
│  │ AudioPlayerUI  │    │   WAV header writer                    │   │
│  └────────┬───────┘    │   peakAmplitudeNormalizedDb()          │   │
│           │            └────────────────────────────────────────┘   │
│           ▼            ┌────────────────────────────────────────┐   │
│  ┌────────────────┐    │ DefaultAudioPlayerAdapter (NEW)        │   │
│  │ AudioPlayerUI  │◀───│   MediaPlayer + URL cache              │   │
│  │ (live timer +  │    │   getAmplitudes(): scan WAV PCM        │   │
│  │  progress bars)│    │   peak-per-bucket                      │   │
│  └────────────────┘    └────────────────────────────────────────┘   │
│                        ┌────────────────────────────────────────┐   │
│                        │ AudioFocusCoordinator (NEW)            │   │
│                        │   AudioManager.requestAudioFocus       │   │
│                        │   transient-pause on phone call/Siri   │   │
│                        └────────────────────────────────────────┘   │
└──────────────────────────┬──────────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────────┐
│  raya-chat-core                                                     │
│                                                                     │
│  RayaChatClient.sendAudio(base64) ──▶ wsManager.sendBinary(bytes)   │
│                                                ▼                    │
│  WebSocketManager.sendBinary(bytes: ByteArray)                      │
│       OkHttp WebSocket.send(ByteString)  ◀── BINARY FRAME (NEW)     │
│                                                                     │
│  MessageHandler.handleResponse() ──▶ onAttachments(urls, "audio")   │
│       (already parses ResponseData.audio_urls)                      │
└─────────────────────────────────────────────────────────────────────┘
```

**Component additions matrix:**

| Component | Module | Status | Action |
|---|---|---|---|
| `WebSocketManager.sendBinary` | core | Missing | Add |
| `RayaChatClient.sendAudio` | core | Broken (text frame) | Rewrite to call `sendBinary` |
| `Constants` audio caps | core | Missing | Add 3 constants |
| `AudioRecorderAdapter.getAmplitudes` | core | Missing | Add to interface |
| `DefaultAudioRecorderAdapter` | ui | Missing | Build (AudioRecord + WAV writer) |
| `DefaultAudioPlayerAdapter` | ui | Missing | Build (MediaPlayer + scanner) |
| `AudioFocusCoordinator` | ui | Missing | Build |
| `AudioRecorderUI` | ui | Stub | Replace with waveform-bearing overlay |
| `AudioPreviewUI` | ui | Missing | Build |
| `AudioPlayerUI` | ui | Stub (LinearProgress) | Replace with waveform + dashed line |
| `MessageBubble` audio branch | ui | Missing | Add `type == 2` rendering |
| `RECORD_AUDIO` permission | ui manifest | Missing | Add (declared, not required) |
| `RayaChatWidget(audioPlayerAdapter)` | ui | Missing | Add optional parameter |
| `ChatScreen` audioFlow FSM | ui | Stub TODO | Wire 3-state machine |

---

## 4. Implementation Plan

### Workstream 1 — Core constants and binary WebSocket transport

**Files to modify:**
- `raya-chat-core/.../Constants.kt`
- `raya-chat-core/.../websocket/WebSocketManager.kt`
- `raya-chat-core/.../RayaChatClient.kt`

**Constants additions:**

```kotlin
// Audio
const val MAX_AUDIO_DURATION_SECONDS = 180
const val MAX_AUDIO_PAYLOAD_BYTES = 10 * 1024 * 1024  // 10 MB
const val MIN_AUDIO_PAYLOAD_BYTES = 4096              // ~125 ms
```

Bump `SDK_VERSION = "0.1.2"`.

**`WebSocketManager.sendBinary`** — OkHttp's `WebSocket` interface has both `send(text: String): Boolean` and `send(bytes: ByteString): Boolean`. The latter sends a binary frame.

```kotlin
import okio.ByteString
import okio.ByteString.Companion.toByteString

fun sendBinary(bytes: ByteArray): Boolean {
    if (destroyed.get()) return false
    Log.d(TAG, "→ SEND BINARY (${bytes.size} bytes)")
    if (_status == ConnectionStatus.CONNECTED) {
        return try {
            webSocket?.send(bytes.toByteString()) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "→ SEND BINARY error: ${e.message}")
            false
        }
    }
    // Do NOT enqueue binary in the text MessageQueue — drop and surface error.
    return false
}
```

**Rewrite `RayaChatClient.sendAudio`:**

```kotlin
fun sendAudio(base64: String) {
    _presets.value = emptyList()
    val ts = System.currentTimeMillis()

    // Normalize: accept raw base64 OR data:audio/wav;base64,...
    val (raw, dataUri) = if (base64.startsWith("data:")) {
        val comma = base64.indexOf(',')
        if (comma < 0) return
        base64.substring(comma + 1) to base64
    } else {
        base64 to "data:audio/wav;base64,$base64"
    }

    val msg = TypeMessage(
        id = "local-audio-$ts-${randomSuffix()}",
        sender = 1,
        type = 2,
        content = "",
        createdAt = (ts / 1000).toString(),
        audioJson = json.encodeToString(
            AudioData.serializer(),
            AudioData(type = "local", audioUrls = dataUri),
        ),
    )
    addMessageToState(msg)

    val bytes = try { android.util.Base64.decode(raw, android.util.Base64.DEFAULT) }
                catch (_: Exception) { config.onError?.invoke("Audio could not be encoded for upload."); return }

    val sent = wsManager?.sendBinary(bytes) ?: false
    if (!sent) config.onError?.invoke("Audio queued — reconnecting...")
}
```

**Edge cases:** invalid base64, send-while-disconnected (no queue for binary — drop and notify), payload > server limits (already filtered by recorder).

> **Why this matters.** The iOS team discovered that base64-as-text-frame was being silently dropped server-side. The server-side handler routes binary frames straight to OpenAI Whisper for transcription; text frames are parsed as JSON commands. Sending audio as text means the user sees their bubble locally but the bot never transcribes anything — silent failure. This is the single most important fix in the workstream.

---

### Workstream 2 — Audio adapter interface refresh

**File to modify:** `raya-chat-core/.../adapters/AudioPlayerAdapter.kt`

Add an optional default-method extension (Kotlin doesn't have proto default impls but `interface` methods can have a body):

```kotlin
interface AudioPlayerAdapter {
    suspend fun loadAudio(uri: String): AudioInfo
    suspend fun play()
    suspend fun pause()
    suspend fun seekTo(positionMs: Long)
    suspend fun getPosition(): Long
    suspend fun cleanup()

    /** Peak amplitude (0..1) for each of `sampleCount` time-buckets across the loaded audio,
     *  for waveform visualization. Returns null if the adapter cannot extract amplitude data. */
    suspend fun getAmplitudes(sampleCount: Int): FloatArray? = null
}
```

This is **additive and backwards-compatible** — existing third-party adapters compile unchanged.

**Edge cases:** `sampleCount <= 0` → return `null`; load not yet complete → `null`; codec doesn't expose PCM → `null` (player UI gracefully falls back to dashed line only).

---

### Workstream 3 — `DefaultAudioRecorderAdapter` (the load-bearing piece)

**File to create:** `raya-chat-ui/.../components/media/DefaultAudioRecorderAdapter.kt`

Android's `MediaRecorder` cannot natively produce WAV. You must use **`AudioRecord`** to read raw PCM frames from the mic and **manually write a 44-byte RIFF/WAV header** before the PCM payload.

**Recording config (mirror iOS settings):**

```kotlin
private const val SAMPLE_RATE = 16_000
private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
private const val BITS_PER_SAMPLE = 16
private const val NUM_CHANNELS: Short = 1
```

**Class skeleton:**

```kotlin
class DefaultAudioRecorderAdapter(
    private val context: Context,
) : AudioRecorderAdapter {

    companion object {
        /** Returns null if the host app didn't declare RECORD_AUDIO. SDK auto-hides mic button. */
        fun makeIfAvailable(context: Context): DefaultAudioRecorderAdapter? {
            val pm = context.packageManager
            val info = pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            val declared = info.requestedPermissions?.contains(Manifest.permission.RECORD_AUDIO) == true
            if (!declared) {
                Log.w("RayaChat",
                    "RECORD_AUDIO not declared in AndroidManifest — voice notes disabled. " +
                    "Add <uses-permission android:name=\"android.permission.RECORD_AUDIO\"/>.")
                return null
            }
            return DefaultAudioRecorderAdapter(context.applicationContext)
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

    override suspend fun startRecording() = withContext(Dispatchers.IO) {
        // 1. Permission check — runtime
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("RECORD_AUDIO permission not granted")
        }

        // 2. Take audio focus (transient — ducks music, lets us record)
        AudioFocusCoordinator.enterRecording(context)

        // 3. Init AudioRecord
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufSize = (minBuf * 2).coerceAtLeast(4096)
        @SuppressLint("MissingPermission")
        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufSize
        )
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            AudioFocusCoordinator.exit(context)
            throw IllegalStateException("AudioRecord failed to initialize")
        }
        audioRecord = rec

        // 4. Open WAV file in cache, reserve 44-byte header
        val cacheDir = context.cacheDir
        val file = File(cacheDir, "raya-recording-${UUID.randomUUID()}.wav")
        outputFile = file
        fileOutput = RandomAccessFile(file, "rw").apply {
            setLength(0)
            write(ByteArray(44)) // placeholder header
        }
        totalPcmBytes = 0

        // 5. Start AudioRecord and reading thread
        rec.startRecording()
        isRecording.set(true)
        isPaused.set(false)
        recordingThread = Thread { readLoop(bufSize) }.apply { isDaemon = true; start() }

        // 6. Max-duration auto-stop
        maxDurationJob = CoroutineScope(Dispatchers.Default).launch {
            delay(Constants.MAX_AUDIO_DURATION_SECONDS * 1000L)
            try { stopRecording() } catch (_: Exception) {}
        }
    }

    private fun readLoop(bufSize: Int) {
        val buf = ByteArray(bufSize)
        val raf = fileOutput ?: return
        val rec = audioRecord ?: return
        while (isRecording.get()) {
            if (isPaused.get()) { Thread.sleep(50); continue }
            val read = rec.read(buf, 0, buf.size)
            if (read <= 0) continue
            // Compute peak amplitude (s16le, mono)
            var peak = 0
            var i = 0
            while (i < read) {
                val sample = (buf[i].toInt() and 0xFF) or (buf[i + 1].toInt() shl 8)
                val s = sample.toShort().toInt() // sign-extend
                val mag = if (s == Short.MIN_VALUE.toInt()) Short.MAX_VALUE.toInt() else abs(s)
                if (mag > peak) peak = mag
                i += 2
            }
            // Map to dB then linear-in-dB (mirror iOS): -50dB floor
            val peakDb = if (peak > 0) 20.0 * log10(peak / 32768.0) else -100.0
            val minDb = -50.0
            val normalized = ((peakDb - minDb) / -minDb).coerceIn(0.0, 1.0)
            lastPeakNormalized = normalized.toFloat()
            // Append to file
            synchronized(raf) {
                raf.write(buf, 0, read)
                totalPcmBytes += read
            }
        }
    }

    override suspend fun stopRecording(): AudioResult = withContext(Dispatchers.IO) {
        maxDurationJob?.cancel(); maxDurationJob = null
        isRecording.set(false)
        recordingThread?.join(500); recordingThread = null
        audioRecord?.apply { stop(); release() }; audioRecord = null

        val raf = fileOutput ?: throw IllegalStateException("not recording")
        val file = outputFile ?: throw IllegalStateException("no output file")

        // Backfill RIFF/WAV header now that totalPcmBytes is known
        synchronized(raf) {
            raf.seek(0)
            raf.write(buildWavHeader(totalPcmBytes))
            raf.fd.sync()
        }
        raf.close(); fileOutput = null
        AudioFocusCoordinator.exit(context)

        val totalSize = file.length()
        if (totalSize < Constants.MIN_AUDIO_PAYLOAD_BYTES) {
            file.delete(); throw IllegalStateException("Recording too short")
        }
        if (totalSize > Constants.MAX_AUDIO_PAYLOAD_BYTES) {
            file.delete(); throw IllegalStateException("Recording too large")
        }

        val bytes = file.readBytes()
        val raw = Base64.encodeToString(bytes, Base64.NO_WRAP)
        val uri = "data:audio/wav;base64,$raw"
        // Keep the file for preview playback — adapter cleanup deletes it later
        AudioResult(uri = uri, base64 = raw)
    }

    override suspend fun pauseRecording() { isPaused.set(true) }
    override suspend fun resumeRecording() { isPaused.set(false) }
    override suspend fun getAmplitude(): Float = lastPeakNormalized
    override suspend fun cleanup() { /* stop, delete file, release focus */ }
}
```

**WAV header writer** (44 bytes, little-endian throughout, **never use Java's `DataOutputStream` — it writes big-endian**):

```kotlin
private fun buildWavHeader(pcmBytes: Long): ByteArray {
    val byteRate = SAMPLE_RATE * NUM_CHANNELS * BITS_PER_SAMPLE / 8
    val blockAlign: Short = (NUM_CHANNELS * BITS_PER_SAMPLE / 8).toShort()
    val totalDataLen = pcmBytes + 36
    return ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
        put("RIFF".toByteArray(Charsets.US_ASCII))
        putInt(totalDataLen.toInt())
        put("WAVE".toByteArray(Charsets.US_ASCII))
        put("fmt ".toByteArray(Charsets.US_ASCII))
        putInt(16)                              // Subchunk1Size for PCM
        putShort(1)                             // AudioFormat = PCM
        putShort(NUM_CHANNELS)
        putInt(SAMPLE_RATE)
        putInt(byteRate)
        putShort(blockAlign)
        putShort(BITS_PER_SAMPLE.toShort())
        put("data".toByteArray(Charsets.US_ASCII))
        putInt(pcmBytes.toInt())
    }.array()
}
```

WAV/RIFF authoritative spec: http://soundfile.sapp.org/doc/WaveFormat/

**Edge cases to handle (mirror iOS):** mic-permission denied → throw `SecurityException`; `AudioRecord.STATE_UNINITIALIZED` → throw + release focus; disk-full on write → catch `IOException`, surface toast; max-duration auto-stop; min-size rejection; max-size rejection; `AudioFocusChange.LOSS_TRANSIENT` (phone call) → pause and stay paused; double-tap mic → `isRecording.compareAndSet` guard; cleanup must release `AudioRecord`, close file, drop focus, delete tmp file; init must purge orphaned `raya-recording-*.wav` from `cacheDir`.

> **Why this matters (WAV format).** The OpenAI Responses API used by the server only accepts `wav` and `mp3` for audio inputs — not `m4a`/`aac`. Android's `MediaRecorder` produces 3GPP/M4A by default; using it would silently break transcription. The trade-off is file size: WAV PCM is ~10× larger than AAC, which is why the cap is 3 minutes and 10 MB. **Do not use `MediaRecorder` for the recording path even if it seems easier — Whisper rejects the output.**

---

### Workstream 4 — `DefaultAudioPlayerAdapter`

**File to create:** `raya-chat-ui/.../components/media/DefaultAudioPlayerAdapter.kt`

Use `MediaPlayer` for playback (simpler than ExoPlayer for short voice notes; covers both local files, `data:` URIs after base64-decode-to-temp-file, and remote `https://` URLs).

```kotlin
class DefaultAudioPlayerAdapter(
    private val context: Context,
) : AudioPlayerAdapter {
    private var mediaPlayer: MediaPlayer? = null
    private var cachedFile: File? = null   // for getAmplitudes scanning

    override suspend fun loadAudio(uri: String): AudioInfo = withContext(Dispatchers.IO) {
        cleanup()
        val file: File = when {
            uri.startsWith("data:") -> {
                val raw = uri.substringAfter(',', "")
                val bytes = Base64.decode(raw, Base64.DEFAULT)
                File.createTempFile("raya-play-", ".wav", context.cacheDir).apply {
                    writeBytes(bytes)
                }
            }
            uri.startsWith("http://") || uri.startsWith("https://") -> {
                downloadToCache(uri) // simple OkHttp GET → cacheDir
            }
            uri.startsWith("file://") -> File(java.net.URI(uri))
            else -> File(uri)
        }
        cachedFile = file
        val mp = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            setDataSource(file.absolutePath)
            prepare()
        }
        mediaPlayer = mp
        AudioInfo(durationMs = mp.duration.toLong())
    }

    override suspend fun play() = withContext(Dispatchers.IO) {
        AudioFocusCoordinator.enterPlayback(context)
        mediaPlayer?.start()
        Unit
    }
    override suspend fun pause() { mediaPlayer?.pause() }
    override suspend fun seekTo(positionMs: Long) { mediaPlayer?.seekTo(positionMs.toInt()) }
    override suspend fun getPosition(): Long = mediaPlayer?.currentPosition?.toLong() ?: 0L

    override suspend fun cleanup() {
        mediaPlayer?.apply { try { stop() } catch (_: Exception) {}; release() }
        mediaPlayer = null
        cachedFile?.takeIf { it.name.startsWith("raya-play-") }?.delete()
        cachedFile = null
        AudioFocusCoordinator.exit(context)
    }

    /** Reads the WAV PCM directly to compute peak-per-bucket amplitudes. */
    override suspend fun getAmplitudes(sampleCount: Int): FloatArray? = withContext(Dispatchers.IO) {
        val file = cachedFile ?: return@withContext null
        if (sampleCount <= 0) return@withContext null
        runCatching {
            FileInputStream(file).use { input ->
                // Skip 44-byte WAV header. (For non-WAV remote URLs, fall back to decoding via MediaExtractor / null.)
                val header = ByteArray(44); input.read(header)
                if (String(header, 0, 4) != "RIFF") return@withContext null
                val pcmLength = file.length() - 44
                val totalSamples = (pcmLength / 2).toInt()
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
                        val s = ((buf[j].toInt() and 0xFF) or (buf[j + 1].toInt() shl 8)).toShort().toInt()
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
}
```

**Edge cases:** non-WAV remote MP3/AAC → fall back to `MediaExtractor`/`MediaCodec` decode loop (or return null and let UI show only the dashed line); concurrent players → each chat audio bubble owns its own adapter instance (mirror iOS `AudioMessageBubble.adapter = .init()`); `MediaPlayer` cannot resume after `release()`; `OnCompletionListener` to flip `isPlaying` to false.

---

### Workstream 5 — `AudioFocusCoordinator`

**File to create:** `raya-chat-ui/.../components/media/AudioFocusCoordinator.kt`

Mirror of iOS `AudioSessionCoordinator`. Use `AudioManager.requestAudioFocus`, `AudioFocusRequest` (API 26+).

```kotlin
internal object AudioFocusCoordinator {
    private var request: AudioFocusRequest? = null
    private var listener: AudioManager.OnAudioFocusChangeListener? = null
    @Volatile var onTransientLoss: (() -> Unit)? = null  // Recorder subscribes here

    fun enterRecording(context: Context) = enter(
        context,
        usage = AudioAttributes.USAGE_VOICE_COMMUNICATION,
        contentType = AudioAttributes.CONTENT_TYPE_SPEECH,
        focusGain = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
    )
    fun enterPlayback(context: Context) = enter(
        context,
        usage = AudioAttributes.USAGE_MEDIA,
        contentType = AudioAttributes.CONTENT_TYPE_SPEECH,
        focusGain = AudioManager.AUDIOFOCUS_GAIN,
    )

    private fun enter(context: Context, usage: Int, contentType: Int, focusGain: Int) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val l = AudioManager.OnAudioFocusChangeListener { change ->
            when (change) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> onTransientLoss?.invoke()
            }
        }
        listener = l
        if (Build.VERSION.SDK_INT >= 26) {
            val attrs = AudioAttributes.Builder().setUsage(usage).setContentType(contentType).build()
            val req = AudioFocusRequest.Builder(focusGain)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener(l)
                .build()
            request = req
            am.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(l, AudioManager.STREAM_MUSIC, focusGain)
        }
    }

    fun exit(context: Context) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= 26) request?.let { am.abandonAudioFocusRequest(it) }
        else listener?.let { @Suppress("DEPRECATION") am.abandonAudioFocus(it) }
        request = null; listener = null
    }
}
```

The recorder registers `onTransientLoss = { isPaused.set(true) }` in `startRecording`. **Match iOS UX exactly: stay paused — never auto-resume after the call ends; user must tap Resume.**

---

### Workstream 6 — Replace `AudioRecorderUI` (live waveform) and add `AudioPreviewUI`

**Files:**
- Replace `raya-chat-ui/.../components/media/AudioRecorderUI.kt`
- Create `raya-chat-ui/.../components/media/AudioPreviewUI.kt`

**Recorder UI** — match iOS layout left → right: **CircleX (cancel) · 8dp red dot · `HH:MM:SS` monospaced timer · live waveform (40 bars max, 2dp wide, 2dp spacing, scrolls left) · CirclePause/Play · CircleStop**. Card has 14dp corner radius, 1dp border, `theme.composerBg` background, 14dp horizontal padding, 10dp vertical padding, 16dp outer horizontal padding.

```kotlin
@Composable
fun AudioRecorderUI(
    adapter: AudioRecorderAdapter,
    onComplete: (AudioResult, FloatArray) -> Unit,
    onCancel: () -> Unit,
) {
    val theme = LocalRayaTheme.current
    val scope = rememberCoroutineScope()
    var isPaused by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    val amplitudes = remember { mutableStateListOf<Float>() }

    LaunchedEffect(Unit) {
        runCatching { adapter.startRecording() }.onFailure { onCancel(); return@LaunchedEffect }
        // Sample loop
        launch {
            while (isActive) {
                if (!isPaused) amplitudes.add(adapter.getAmplitude())
                delay(100)
            }
        }
        // Elapsed timer loop
        launch {
            while (isActive) {
                delay(1000)
                if (!isPaused) elapsedSeconds++
            }
        }
    }
    // ... HStack with CircleX, dot (alpha 1f or 0.3f), Text("%02d:%02d:%02d"...),
    // GeometryReader-equivalent (BoxWithConstraints) drawing last N bars,
    // CirclePauseIcon/CirclePlayIcon, CircleStopIcon → onClick = stopAndPreview
}
```

**Preview UI** is a separate Compose function showing the **whole** recording as a static waveform with per-bar played/unplayed coloring driven by `MediaPlayer.currentPosition / duration`. It owns its own `DefaultAudioPlayerAdapter` to play back the data URI from `AudioResult.uri`. Layout: `[CircleX] [static waveform with progress] [CirclePauseIcon/CirclePlayIcon] [Send filled circle 36dp]`.

Use the iOS `AudioPreviewUI.swift` as the visual reference verbatim — iOS's `downsample(amps, to: count)` peak-per-bucket logic translates directly to Kotlin.

**Edge cases:** cancel during permission prompt (Compose `LaunchedEffect` cancellation handles this — `runCatching` swallows); `BackHandler` to also fire `onCancel`; `DisposableEffect { onDispose { adapter.cleanup() } }`; when `MAX_AUDIO_DURATION_SECONDS` hits, recorder auto-stops → push to preview (don't drop).

---

### Workstream 7 — `MessageBubble` audio rendering + new `AudioPlayerUI`

**Files:**
- Modify `raya-chat-ui/.../components/chat/MessageBubble.kt`
- Replace `raya-chat-ui/.../components/media/AudioPlayerUI.kt`

In `MessageBubble.kt`, before the existing `if (isSystem)` and main bubble branches, add:

```kotlin
if (message.type == 2 && !message.audioJson.isNullOrEmpty()) {
    val audio = remember(message.audioJson) {
        runCatching { json.decodeFromString<AudioData>(message.audioJson!!) }.getOrNull()
    }
    if (audio != null && audio.audioUrls.isNotEmpty()) {
        AudioMessageRow(uri = audio.audioUrls, isUser = isUser, timestamp = timestamp,
                        avatarUrl = avatarUrl, audioPlayerAdapter = audioPlayerAdapter)
        return
    }
}
```

`MessageBubble` now needs an `audioPlayerAdapter: AudioPlayerAdapter?` parameter. If null, host hasn't provided one and `MessageList` should construct a `DefaultAudioPlayerAdapter` per audio bubble (mirror iOS `AudioMessageBubble`).

**New `AudioPlayerUI.kt`** — replace LinearProgressIndicator with the iOS-spec waveform: `[HH:MM:SS / HH:MM:SS]  [waveform with dashed line behind, played/unplayed bars]  [outlined Play/Pause triangle]`. Background dashed line is drawn via `Modifier.drawBehind { drawLine(..., pathEffect = PathEffect.dashPathEffect(...)) }`. Bars use `Canvas { drawRoundRect(...) }` with `silenceThreshold = 0.05f` skipping silent samples (so dashed line shows through). Bar heights: `sqrt(amp / peak) * 22dp`, `peak = max(0.05f, amplitudes.max())`. Played alpha 1.0, unplayed alpha 0.4.

End-of-playback detection (mirror iOS): 200ms tolerance + 3 stationary 100ms ticks fallback (`MediaPlayer.OnCompletionListener` is sometimes flaky on some OEMs).

Each audio bubble owns its own adapter instance. Playing one calls the OS audio focus and the previous adapter's `pause()` is naturally triggered via focus loss → all good.

---

### Workstream 8 — Public API surface and wiring

**Files to modify:**
- `raya-chat-ui/.../RayaChatWidget.kt` — add `audioPlayerAdapter: AudioPlayerAdapter? = null`
- `raya-chat-ui/.../RayaChatViewModel.kt` — pass through
- `raya-chat-ui/.../screens/ChatScreen.kt` — add 3-state `audioFlow` enum, replace TODO

```kotlin
// ChatScreen.kt — replace the inline `MessageComposer` with:
sealed class AudioFlow {
    object None : AudioFlow()
    object Recording : AudioFlow()
    data class Preview(val result: AudioResult, val amps: FloatArray) : AudioFlow()
}

var audioFlow by remember { mutableStateOf<AudioFlow>(AudioFlow.None) }

when (val flow = audioFlow) {
    AudioFlow.Recording -> audioRecorderAdapter?.let { rec ->
        AudioRecorderUI(
            adapter = rec,
            onComplete = { result, amps -> audioFlow = AudioFlow.Preview(result, amps) },
            onCancel = { audioFlow = AudioFlow.None },
        )
    } ?: run { audioFlow = AudioFlow.None }
    is AudioFlow.Preview -> AudioPreviewUI(
        audioResult = flow.result,
        amplitudes = flow.amps,
        onSend = {
            flow.result.base64?.takeIf { it.isNotEmpty() }?.let { onSendAudio(it) }
            audioFlow = AudioFlow.None
        },
        onCancel = { audioFlow = AudioFlow.None },
    )
    AudioFlow.None -> MessageComposer(
        // ...existing wiring...
        onMicPress = { audioFlow = AudioFlow.Recording },
    )
}
```

Wire the `RayaChatWidget` factory to construct a `DefaultAudioRecorderAdapter` if the consumer didn't provide one (and `RECORD_AUDIO` is declared); same for `DefaultAudioPlayerAdapter` (no permission needed for playback).

```kotlin
@Composable
fun RayaChatWidget(
    token: String,
    locale: String = "en",
    imagePickerAdapter: ImagePickerAdapter? = null,
    audioRecorderAdapter: AudioRecorderAdapter? = null,
    audioPlayerAdapter: AudioPlayerAdapter? = null,   // NEW
    /* ...callbacks unchanged... */
) {
    val context = LocalContext.current
    val effectiveRecorder = audioRecorderAdapter
        ?: remember(context) { DefaultAudioRecorderAdapter.makeIfAvailable(context) }
    val effectivePlayer = audioPlayerAdapter
        ?: remember(context) { DefaultAudioPlayerAdapter(context.applicationContext) }
    /* ...pass both to RayaChatContent → ChatScreen → MessageBubble... */
}
```

This is **fully additive** — existing consumers calling `RayaChatWidget(token = "...")` still compile and now get voice notes for free as long as they declared `RECORD_AUDIO`.

---

## 5. Permissions

**Required manifest entry** in `raya-chat-ui/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

Library manifest entries are merged into the host app's manifest by AGP. A consumer that doesn't want voice notes can suppress the permission with `tools:node="remove"`. Alternatively, leave the permission undeclared in the library manifest and require the consumer to declare it themselves; `makeIfAvailable()` already detects this. **Recommendation: do not auto-declare in the library manifest** — match iOS behavior where the host app must opt in by adding `NSMicrophoneUsageDescription`. The Android equivalent is the consumer adding `<uses-permission android:name="android.permission.RECORD_AUDIO"/>`.

**Runtime permission flow (API 23+).** `RECORD_AUDIO` is a dangerous permission. The recorder UI must request it before `AudioRecord.startRecording()`:

```kotlin
@Composable
fun rememberRecordAudioLauncher(onResult: (Boolean) -> Unit) =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onResult)
```

In `AudioRecorderUI`, on first compose check `ContextCompat.checkSelfPermission`. If `PERMISSION_DENIED`, launch `RequestPermission`. If user denies twice (`shouldShowRequestPermissionRationale == false`), show a snack/toast: *"Microphone access is required for voice notes — open Settings to enable."* with an action that opens `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` for `context.packageName`. **Mirror iOS exactly** — never auto-cancel on denial; surface a path to recovery.

**Foreground recording.** No `FOREGROUND_SERVICE_MICROPHONE` is needed because the SDK only records while the app is in the foreground. Backgrounding pauses (via `AudioManager.AUDIOFOCUS_LOSS_TRANSIENT` on Android 14+ or via lifecycle observer on lower APIs). Like iOS, the user must explicitly resume when they return.

**Comparison vs iOS.** iOS requires `NSMicrophoneUsageDescription` (compile-time/Info.plist string) — there's no runtime prompt initially, the OS auto-prompts on first `AVAudioRecorder.record()`. Android requires both a manifest declaration AND an explicit runtime request. Both `makeIfAvailable()` static factories return null when the host app hasn't declared the permission; both SDKs hide the mic button accordingly so the user never hits a hard crash.

---

## 6. Audio file format

**WAV PCM, 16 kHz sample rate, mono, 16-bit signed little-endian.** This is Whisper-native and accepted by the OpenAI Responses API.

| Property | Value | iOS API key | Android equivalent |
|---|---|---|---|
| Format | WAV (RIFF) | `kAudioFormatLinearPCM` | manual header + `AudioRecord` PCM |
| Sample rate | 16,000 Hz | `AVSampleRateKey` | `AudioRecord` `sampleRateInHz = 16000` |
| Channels | 1 (mono) | `AVNumberOfChannelsKey = 1` | `AudioFormat.CHANNEL_IN_MONO` |
| Bit depth | 16-bit | `AVLinearPCMBitDepthKey = 16` | `AudioFormat.ENCODING_PCM_16BIT` |
| Endianness | little-endian | `AVLinearPCMIsBigEndianKey = false` | `ByteOrder.LITTLE_ENDIAN` for header |
| Sample type | signed PCM | `AVLinearPCMIsFloatKey = false` | (default for `ENCODING_PCM_16BIT`) |
| Container ext | `.wav` | — | `.wav` |
| MIME | `audio/wav` | — | `audio/wav` |

The 44-byte WAV header structure is in Workstream 3. **Watch out:** Android's `DataOutputStream.writeInt/Short` writes big-endian — always use `ByteBuffer.allocate(...).order(ByteOrder.LITTLE_ENDIAN)`.

If you'd rather not hand-roll, the small library [skoumalcz/audio-recorder](https://github.com/skoumalcz/audio-recorder) wraps the same `AudioRecord` + WAV-header pattern (Apache-2 licensed). For SDK distribution, however, hand-rolling avoids a transitive dependency and keeps the JAR slim — recommended.

Authoritative WAV spec: http://soundfile.sapp.org/doc/WaveFormat/

> **Why this matters (WAV format, again).** OpenAI Responses API audio input only accepts `wav` or `mp3`. The server streams the binary frame straight to OpenAI without re-encoding. If you ship M4A/AAC, every voice note silently fails to transcribe — bot has no idea what the user said. **Whisper can technically decode many formats but the Responses API rejects them at the schema layer.**

---

## 7. WebSocket binary frame

**Library:** OkHttp 4 (already a dependency). The relevant interface:

```kotlin
interface WebSocket {
    fun send(text: String): Boolean        // text frame (current path)
    fun send(bytes: ByteString): Boolean   // binary frame (NEW path)
}
```

**Before (broken):**

```kotlin
// RayaChatClient.sendAudio
fun sendAudio(base64: String) {
    /* ... */
    wsManager?.send(base64)   // ← TEXT FRAME — server drops as malformed JSON
}
```

**After (correct):**

```kotlin
// WebSocketManager.kt — NEW method
import okio.ByteString.Companion.toByteString
fun sendBinary(bytes: ByteArray): Boolean {
    if (destroyed.get() || _status != ConnectionStatus.CONNECTED) return false
    return try { webSocket?.send(bytes.toByteString()) ?: false }
    catch (e: Exception) { Log.e(TAG, "sendBinary error: ${e.message}"); false }
}

// RayaChatClient.kt
fun sendAudio(base64: String) {
    /* normalize, build TypeMessage, addMessageToState */
    val bytes = Base64.decode(raw, Base64.DEFAULT)
    wsManager?.sendBinary(bytes)
}
```

Verify on the wire with Charles/Wireshark — frame opcode `0x82` (binary FIN) instead of `0x81` (text FIN). OkHttp docs: https://square.github.io/okhttp/4.x/okhttp/okhttp3/-web-socket/send/

> **Why this matters (binary frame routing).** The server's WebSocket handler has two branches: text frames are JSON-parsed and routed to the chat-message pipeline; binary frames are forwarded as-is to OpenAI Whisper. There is no fallback that base64-decodes an incoming text frame as audio. **A text frame containing base64 audio bytes is genuinely indistinguishable from malformed JSON and gets dropped without an error response** — that's why the iOS bug took a while to find. Test by sending a 1-second voice note and watching for a `RESPONSE` message containing the transcript; if it never arrives, you're still on text frames.

---

## 8. `audio_urls` response handling

Already wired correctly on Android — `MessageHandler.handleResponse()` reads `responseData.audioUrls` and dispatches `callbacks.onAttachments(listOf(audioUrls), "audio")`. `RayaChatClient.onAttachments` overload finds the latest `sender == 1, type == 2` message, replaces `audioJson` with `AudioData(type = "remote", audioUrls = remoteUrl)`, fires `config.onMessageUpdate?.invoke(sessionId, updatedMsg)`, and persists.

The **only** related change required: **make sure `MessageBubble` reads `audioJson` and renders the audio waveform** so the message visually appears (currently it's invisible because no rendering branch exists). After Workstream 7, the local data-URI bubble plays immediately after recording, and the bubble silently swaps to the remote URL when the server response arrives — `onMessageUpdate` callback fires once with the remote URL, exactly like iOS.

Pseudocode for the full path (already implemented except where noted):

```
sendAudio(base64) →
  addMessageToState(local TypeMessage, audioJson = {type:"local", audioUrls:"data:audio/wav;base64,..."}) ✓
  pendingAttachmentMessageIds += msg.id   // [NEW — match iOS to defer onMessageUpdate]
  wsManager.sendBinary(decodeBase64(base64)) ✓
  ... (server transcribes via OpenAI)
  RESPONSE arrives with data.audio_urls = "https://s3.../voice.wav" ✓
  MessageHandler dispatches onAttachments([url], "audio") ✓
  RayaChatClient.onAttachments(...) finds latest user audio msg, swaps audioJson ✓
  config.onMessageUpdate?.invoke(sessionId, updatedMsg)  // fires NOW with remote URL ✓
  MessageBubble re-reads audioJson → AudioPlayerUI loads new uri seamlessly  // [NEW after WS7]
```

Add `pendingAttachmentMessageIds` set on Android (currently `addMessageToState` always fires `onMessageUpdate`; it skips only by `sender == 1 && (type == 3 || type == 2)`. That logic already correctly defers user-image and user-audio messages — match the iOS rename for consistency or keep as-is; behavior is equivalent).

> **Why this matters (`audio_urls`).** Without this swap, `onSessionEnd(sessionId, messages)` returns transcripts where every user voice note has `audioUrls = "data:audio/wav;base64,...HUGE..."`. That's hundreds of KB per message, breaks JSON exports for backend storage, and the URLs aren't shareable. With the swap, `onSessionEnd` returns clean S3 URLs ready for backend persistence. **Same pipeline as image attachments — the iOS team didn't invent a new path; they reused the existing image-attachment swap mechanism, which Android already has.**

---

## 9. UI design specs

All visual values mirror the iOS implementation, which mirrors the web widget. Use `LocalRayaTheme.current` for colors.

**Recording overlay (`AudioRecorderUI`)** — replaces the composer card while recording:

| Element | Spec |
|---|---|
| Container shape | RoundedCornerShape(14dp) |
| Container border | 1dp `theme.composerBorder` |
| Container background | `theme.composerBg` |
| Container padding | horizontal 14dp, vertical 10dp |
| Outer horizontal margin | 16dp |
| HStack spacing | 12dp |
| Cancel icon | `CircleXIcon`, 28dp × 28dp, tint `theme.mutedForeground` |
| Recording dot | 8dp circle, fill `Color(0xFFEF4444)`; alpha = isPaused ? 0.3 : 1.0 |
| Timer | `Text(format = "%02d:%02d:%02d")`, monospaced, 13sp, foreground = `theme.foreground`, minWidth 64dp |
| Waveform | weight = 1f, height 28dp, bars 2dp wide × 2dp spaced, color `theme.foreground`, height = `max(3dp, amp * 28dp)`, max 40 bars (suffix-clip), corner radius 1dp |
| Pause/Resume icon | `CirclePauseIcon` or `CirclePlayIcon`, 28dp × 28dp, tint `theme.mutedForeground` |
| Stop icon | `CircleStopIcon`, 28dp × 28dp, tint `theme.mutedForeground` |

**Preview overlay (`AudioPreviewUI`)** — same container chrome as recording overlay:

| Element | Spec |
|---|---|
| Cancel | `CircleXIcon` 28dp |
| Static waveform | weight 1f, height 28dp, downsampled to fit available bar count (peak-per-bucket); played bars at full opacity, unplayed at 30% (`theme.foreground.opacity(0.3)`) |
| Play/Pause | `CirclePlayIcon` or `CirclePauseIcon` 28dp |
| Send | 36dp circle, fill `theme.gradientColor`, contains `SendIcon` 16dp tinted `theme.gradientForeground` |

**In-bubble player (`AudioPlayerUI`)** — renders flat (no bubble bg), `type == 2`:

| Element | Spec |
|---|---|
| Layout | flat row, no background; only avatar (28dp circle) on bot side |
| Time label | `HH:MM:SS / HH:MM:SS`, monospaced 12sp, fixed width, `theme.foreground` |
| Waveform | weight 1f, height 24dp, 60 buckets downsampled |
| Bar width / spacing | 2dp / 2dp |
| Bar height | `max(4dp, sqrt(amp / peak) * 22dp)` where `peak = max(0.05f, amplitudes.max())` |
| Silence threshold | `amp < 0.05` → don't draw the bar (dashed line shows through) |
| Played bar color | `theme.foreground`, alpha 1.0 |
| Unplayed bar color | `theme.foreground`, alpha 0.4 |
| Background dashed line | 1dp stroke, color `theme.mutedForeground.copy(alpha = 0.6f)`, dash `[2dp, 4dp]`, vertically centered |
| Play/Pause icon | `PlayIcon` or `PauseIcon` (outlined triangle/bars, **no circle**), 22dp, `theme.foreground` |

**Icons.** The lucide-react icons used: `CircleX`, `CirclePause`, `CirclePlay`, `CircleStop`, `Play`, `Pause`, plus the existing `Send`. iOS bundles them as PDF assets; for Android, add SVG vector drawables in `raya-chat-ui/src/main/res/drawable/`. Source: https://lucide.dev/icons/ (download SVG, convert to Vector Drawable via Android Studio's *New → Vector Asset → Local file*). Naming convention to match existing `RayaIcons.kt`:

```kotlin
object RayaIcons {
    fun circleX(tint: Color): ImageVector = /* lucide circle-x */
    fun circlePause(tint: Color): ImageVector = /* lucide circle-pause */
    fun circlePlay(tint: Color): ImageVector = /* lucide circle-play */
    fun circleStop(tint: Color): ImageVector = /* lucide circle-stop */
    fun play(tint: Color): ImageVector = /* lucide play */
    fun pause(tint: Color): ImageVector = /* lucide pause */
}
```

Existing `RayaIcons.close` and `RayaIcons.mic` should NOT be reused for record/play UI — the visual specs are different (outlined circles around glyphs).

---

## 10. Edge cases checklist

| # | Case | Expected behavior |
|---|---|---|
| 1 | `RECORD_AUDIO` not declared in manifest | `makeIfAvailable()` returns null; mic button hidden |
| 2 | Permission denied (first request) | Snackbar: "Microphone access required" + "Open Settings" action |
| 3 | Permission denied + don't-ask-again | Same snackbar; settings deep-link to app permissions page |
| 4 | App backgrounded mid-recording | `AUDIOFOCUS_LOSS_TRANSIENT` → adapter pauses |
| 5 | App foregrounded after pause | Stays paused; user must tap Resume (matches iOS) |
| 6 | Phone call / Siri-equivalent | Same `AUDIOFOCUS_LOSS_TRANSIENT` path → pause; stay paused |
| 7 | Disk full on PCM write | `IOException` caught → cleanup + toast "Could not save recording" |
| 8 | Recording exceeds 180 s | Auto-stop fires `stopRecording()` → preview UI shown |
| 9 | File < 4 KB on stop | Throw `fileTooSmall`; show "Recording too short" toast; recorder UI dismissed |
| 10 | File > 10 MB on stop | Throw `fileTooLarge`; show toast; UI dismissed |
| 11 | Tap mic while already recording | State machine guard — no-op (audioFlow already `Recording`) |
| 12 | Cancel during permission prompt | LaunchedEffect cancellation + `runCatching` swallows |
| 13 | `AudioRecord` init fails (`STATE_UNINITIALIZED`) | Throw, release focus, dismiss UI |
| 14 | `AudioFocusRequest` denied | Throw, dismiss UI, toast "Audio busy — try again" |
| 15 | Orphaned `raya-recording-*.wav` in cache | Purge on adapter init |
| 16 | WebSocket disconnected when sending | `sendBinary` returns false → `onError("Audio queued — reconnecting...")` and message stays as local data-URI; user can re-send manually |
| 17 | Multiple audio bubbles on screen | Each owns own player adapter; tapping one steals audio focus (auto-pauses the other) |
| 18 | `MediaPlayer.OnCompletionListener` flaky | Position-stagnation fallback (3 stationary 100ms ticks) — same as iOS |
| 19 | Remote MP3/AAC URL returned by server | `MediaPlayer` plays it; `getAmplitudes` returns null → UI shows dashed line only (no bars) |
| 20 | `data:audio/wav;base64,...` URI | Player decodes to temp file in cache, scans for amplitudes |
| 21 | Process death during recording | Recording lost (no foreground service); next launch purges orphan WAV |
| 22 | Rotation during recording | `RayaChatViewModel` survives; `audioFlow` state held in Compose `rememberSaveable` (use a custom Saver for sealed class) — recording continues |
| 23 | API 23 (minSdk 24 enforced — N/A but) | Use `AudioFocusRequest` only on API 26+, fallback to deprecated `requestAudioFocus(listener, ...)` |
| 24 | Large transcript export via `onSessionEnd` | After workstream 8, audio messages contain remote URLs (never data URIs) — verify by sending 3 voice notes then ending session |

---

## 11. Public API surface (Kotlin signatures)

**Additive only** — every existing call site continues to compile.

```kotlin
// raya-chat-core/.../adapters/AudioPlayerAdapter.kt — interface gains default method
interface AudioPlayerAdapter {
    suspend fun loadAudio(uri: String): AudioInfo
    suspend fun play()
    suspend fun pause()
    suspend fun seekTo(positionMs: Long)
    suspend fun getPosition(): Long
    suspend fun cleanup()
    /** NEW — additive with default returning null */
    suspend fun getAmplitudes(sampleCount: Int): FloatArray? = null
}

// raya-chat-core/.../websocket/WebSocketManager.kt — new method
class WebSocketManager(/* ... */) {
    fun sendBinary(bytes: ByteArray): Boolean   // NEW
}

// raya-chat-core/.../Constants.kt — new fields
object Constants {
    const val MAX_AUDIO_DURATION_SECONDS = 180     // NEW
    const val MAX_AUDIO_PAYLOAD_BYTES = 10 * 1024 * 1024  // NEW
    const val MIN_AUDIO_PAYLOAD_BYTES = 4096       // NEW
    const val SDK_VERSION = "0.1.2"                // BUMP
}

// raya-chat-ui/.../components/media/DefaultAudioRecorderAdapter.kt — NEW
class DefaultAudioRecorderAdapter(context: Context) : AudioRecorderAdapter {
    companion object {
        fun makeIfAvailable(context: Context): DefaultAudioRecorderAdapter?
    }
    override suspend fun startRecording()
    override suspend fun stopRecording(): AudioResult
    override suspend fun pauseRecording()
    override suspend fun resumeRecording()
    override suspend fun getAmplitude(): Float
    override suspend fun cleanup()
}

// raya-chat-ui/.../components/media/DefaultAudioPlayerAdapter.kt — NEW
class DefaultAudioPlayerAdapter(context: Context) : AudioPlayerAdapter {
    override suspend fun loadAudio(uri: String): AudioInfo
    override suspend fun play()
    override suspend fun pause()
    override suspend fun seekTo(positionMs: Long)
    override suspend fun getPosition(): Long
    override suspend fun cleanup()
    override suspend fun getAmplitudes(sampleCount: Int): FloatArray?
}

// raya-chat-ui/.../RayaChatWidget.kt — new optional parameter
@Composable
fun RayaChatWidget(
    token: String,
    locale: String = "en",
    imagePickerAdapter: ImagePickerAdapter? = null,
    audioRecorderAdapter: AudioRecorderAdapter? = null,
    audioPlayerAdapter: AudioPlayerAdapter? = null,    // NEW
    onSessionStart: ((String) -> Unit)? = null,
    onSessionEnd: ((sessionId: String, messages: List<TypeMessage>) -> Unit)? = null,
    onMessageUpdate: ((sessionId: String, message: TypeMessage) -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
    onClose: (() -> Unit)? = null,
)

// raya-chat-ui/.../RayaChatFragment.kt and RayaChatBottomSheet.kt — same additive parameter
```

`RayaChatClient.sendAudio(base64: String)` — signature unchanged; behavior fixed.

---

## 12. Versioning

Bump from `0.1.1` → **`0.1.2`** (minor patch, additive surface). Match iOS exactly. Locations to bump:
- `raya-chat-core/.../Constants.kt`: `SDK_VERSION = "0.1.2"`
- `raya-chat-core/build.gradle.kts`: default `version = "0.1.2"`
- `raya-chat-ui/build.gradle.kts`: default `version = "0.1.2"`
- `README.md` install snippet (jitpack tag `0.1.2`)
- Update CHANGELOG entry

No deprecations. No breaking changes. Existing `RayaChatWidget(token = "...")` callers get voice notes for free as soon as they add `<uses-permission android:name="android.permission.RECORD_AUDIO"/>` to their manifest.

---

## 13. Test plan

**Device matrix.** At minimum: one Pixel emulator API 24 (minSdk floor), one emulator API 30, one physical device API 33+ (for behavioral mic permissions and `AudioFocusRequest`), and one OEM device (Samsung One UI is the most common amplitude/`MediaPlayer` outlier). Test in both light and dark theme, both `en` and `ar` (RTL must mirror waveform direction the same way iOS does — verify).

**Verification list:**

- [ ] Mic button hidden when `RECORD_AUDIO` not declared
- [ ] First tap → permission prompt; deny once → button still works, retry shows prompt
- [ ] Deny twice → snackbar with "Open Settings"; tapping deep-links to app permissions
- [ ] Tap mic → recorder overlay replaces composer; red dot pulses; waveform bars start scrolling
- [ ] Pause → red dot dims to 30%; waveform freezes; timer freezes; tap Resume → all resume
- [ ] Stop → preview overlay shows; waveform spans full width; play button works
- [ ] Preview play → progress paints across bars; finishes at end with stagnation fallback verified
- [ ] Preview cancel → returns to composer, no message added
- [ ] Preview send → bubble appears in chat with local waveform, plays back via data URI
- [ ] Server `RESPONSE` arrives with `audio_urls` → bubble swaps to remote URL silently; `onMessageUpdate` callback fires once with remote URL
- [ ] Two audio bubbles → playing one auto-pauses the other (audio focus)
- [ ] Phone call mid-recording → recorder pauses; after call ends, stays paused
- [ ] Background app mid-recording → pauses; foreground → still paused
- [ ] Rotate device mid-recording → recording continues (verify `audioFlow` saves)
- [ ] Record 0.5 s → "Recording too short" toast; no message added
- [ ] Record 4 min → auto-stops at 3 min; preview shown
- [ ] Force-kill app mid-record → next launch, no orphan files in `cacheDir`
- [ ] Disconnect WiFi mid-record-send → toast "Audio queued — reconnecting"; reconnect; message either auto-retries or stays as local URI (acceptable: user-initiated retry)
- [ ] Network sniff (Charles): verify outbound frame opcode is `0x82` (binary), not `0x81` (text)
- [ ] `onSessionEnd` callback returns transcript where every audio message has `audioUrls = "https://..."` (never `data:`)
- [ ] Mode 1 (`RayaChatWidget`), Mode 2 (`RayaChatFragment`), Mode 3 (`RayaChatBottomSheet`), Mode 4 (headless `RayaChatClient`) — voice notes work in all four
- [ ] No memory leak across 50 sequential record→play→delete cycles (use Android Studio Profiler)

---

## 14. Open questions and assumptions

1. **MP3/AAC remote playback amplitude.** When the server returns a remote URL with a non-WAV codec, `getAmplitudes()` would need to decode via `MediaExtractor`/`MediaCodec` to scan PCM. **Assumption:** server always returns WAV (since that's what the SDK uploads); if the server transcodes, the player UI gracefully falls back to "dashed line only, no bars" — visually still legible. Confirm with server team whether remote audio URLs are guaranteed WAV.

2. **Foreground service for backgrounded recording.** iOS deliberately does not support background recording — neither should Android. **Assumption:** no `FOREGROUND_SERVICE_MICROPHONE` permission needed. If product wants background recording later, that's a separate project (would require Android 14+ `FOREGROUND_SERVICE_MICROPHONE` declaration plus a notification).

3. **Scoped storage.** Cache directory writes (`context.cacheDir`) don't require any Storage Access Framework. The recording file is internal-only, deleted after upload. **Assumption:** no `WRITE_EXTERNAL_STORAGE` needed.

4. **Codec availability on minSdk 24.** `AudioRecord` + PCM 16-bit at 16 kHz is supported on every Android device since API 3. No risk.

5. **OkHttp WebSocket message size limits.** OkHttp does not impose an artificial limit on `WebSocket.send(ByteString)`, but the underlying TCP/HTTP infrastructure may. 10 MB payloads are within typical limits. **Assumption:** server accepts up to 10 MB binary frames (matches iOS limit). Confirm with backend.

6. **`rememberSaveable` for `AudioFlow` sealed class.** `AudioFlow.Preview(result, amps)` contains a `FloatArray` and a custom data class — needs a custom `Saver`. **Assumption:** acceptable to lose preview state on rotation (user must re-record or just send) — this matches iOS behavior. If product wants preview to survive rotation, add a Saver.

7. **Lucide icon licensing.** Lucide is ISC-licensed (https://lucide.dev/license) — fully compatible with Apache-2 SDK distribution. Convert SVG → Vector Drawable manually or via Android Studio.

8. **Default adapter ergonomics.** Should `DefaultAudioRecorderAdapter` be auto-instantiated by `RayaChatWidget`, or always opt-in? **Assumption:** auto-instantiate if `RECORD_AUDIO` is declared (matches iOS). Document clearly so consumers who pass their own adapter aren't confused.

9. **Markwon and Compose interop.** Not relevant to this workstream — flagged only because `MessageBubble` uses Markwon for bot messages and the audio rendering branch must `return` before reaching Markwon.

10. **`MediaRecorder` alternative path.** Some teams prefer `MediaRecorder` with `OutputFormat.MPEG_4` + `AudioEncoder.AAC` for size/battery. **Decision: do not.** WAV requirement is non-negotiable per the OpenAI Responses API constraint. If a future server-side change accepts AAC, this can be revisited.

---

**End of guide.** Length: ~3,400 words. The Android team has everything needed: audit summary, dependency-ordered workstreams, file-by-file change list, exact UI specs, edge-case matrix, public-API diff, and call-out warnings on the two non-obvious gotchas (WAV-not-AAC, binary-not-text frame).
