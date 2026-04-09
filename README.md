# Raya Chat Android SDK

Native Android SDK for embedding the Raya AI chat widget in Android apps. Built with Kotlin + Jetpack Compose.

Works with **Jetpack Compose**, **XML layout + Fragment**, **BottomSheet**, and **headless (custom UI)** apps.

---

## Table of Contents

- [Installation](#installation)
- [Which Mode Should I Use?](#which-mode-should-i-use)
- [Quick Start](#quick-start)
  - [Mode 1: Compose Widget](#mode-1-compose-widget)
  - [Mode 2: Fragment](#mode-2-fragment-xml-layout-apps)
  - [Mode 3: BottomSheet](#mode-3-bottomsheet-overlay)
  - [Mode 4: Headless](#mode-4-headless-custom-ui)
- [Configuration](#configuration)
- [Callbacks](#callbacks)
- [What Packaged UI Handles for You](#what-packaged-ui-handles-for-you)
- [Adapters](#adapters)
  - [Image Picker](#imagepickeradapter)
  - [Audio Recorder](#audiorecorderadapter)
- [Features](#features)
- [Theming](#theming)
- [RTL / Arabic Support](#rtl--arabic-support)
- [Headless Mode — Full Guide](#headless-mode--full-guide)
  - [All State](#all-state-stateflow)
  - [All Actions](#all-actions)
  - [Lifecycle: endSession vs destroy](#lifecycle-endsession-vs-destroy)
  - [Handling Streaming Messages](#handling-streaming-messages)
  - [Handling Commands](#handling-commands)
- [Exporting Session Data (onSessionEnd)](#exporting-session-data-onsessionend)
- [Session Persistence](#session-persistence)
- [Background / Foreground Behavior](#background--foreground-behavior)
- [Keeping Chat Alive Across Tabs](#keeping-chat-alive-across-tabs)
- [ProGuard / R8](#proguard--r8)
- [Architecture](#architecture)
- [Requirements](#requirements)
- [Sample App](#sample-app)
- [Troubleshooting](#troubleshooting)

---

## Installation

### Packaged UI (includes all screens)

```gradle
dependencies {
    implementation "ai.teammates:raya-chat-ui:0.1.0"
}
```

This includes `raya-chat-core` automatically. You get all 3 packaged UI modes (Widget, Fragment, BottomSheet).

### Headless only (no UI — just the chat engine)

```gradle
dependencies {
    implementation "ai.teammates:raya-chat-core:0.1.0"
}
```

Smaller footprint (~500KB). You build your own UI. All state + actions available via Kotlin `StateFlow`.

### Permissions

The SDK declares `android.permission.INTERNET` in its own manifest, which merges automatically into your app. You do **not** need to add it yourself.

---

## Which Mode Should I Use?

| Mode | Best for | You build | You get for free | Dependency |
|------|----------|-----------|------------------|------------|
| **1. Compose Widget** | Modern Kotlin/Compose apps | Nothing — drop-in | Full UI: intro, form, chat, commands, end session | `raya-chat-ui` |
| **2. Fragment** | XML layout apps, Java apps, Navigation Component | Nothing — drop-in | Same as Mode 1, inside a Fragment | `raya-chat-ui` |
| **3. BottomSheet** | Any app wanting chat as an overlay | Nothing — one-liner | Same as Mode 1, as a draggable sheet | `raya-chat-ui` |
| **4. Headless** | Apps that need fully custom chat UI | Entire UI from scratch | WebSocket, reconnection, storage, state management | `raya-chat-core` |

**Rule of thumb:** Start with Mode 1 (Compose) or Mode 2 (Fragment). Only use Mode 4 if you need a completely custom design that doesn't match the built-in screens.

---

## Quick Start

### Mode 1: Compose Widget

Drop-in Composable. Full chat widget with intro, form, and chat screens.

```kotlin
import ai.teammates.rayachat.ui.RayaChatWidget

@Composable
fun SupportScreen() {
    RayaChatWidget(
        token = "your-bot-token",
        locale = "en",
        onSessionStart = { sessionId -> Log.d("Chat", "Session: $sessionId") },
        onSessionEnd = { sessionId, messages ->
            Log.d("Chat", "Session $sessionId ended with ${messages.size} messages")
        },
        onError = { err -> Log.w("Chat", "Error: $err") },
        onClose = { /* navigate back or finish activity */ },
    )
}
```

That's it. The widget handles everything: fetching bot config, showing the intro screen, user form, chat, commands, end session, and reconnection.

### Mode 2: Fragment (XML layout apps)

For apps using XML layouts, Java, or Navigation Component. The Fragment wraps Compose internally — your app does **not** need Compose dependencies.

```kotlin
import ai.teammates.rayachat.ui.RayaChatFragment

// In your Activity's onCreate()
val fragment = RayaChatFragment.newInstance("your-bot-token", "en").apply {
    onSessionStart = { sessionId -> Log.d("Chat", "Session: $sessionId") }
    onSessionEnd = { sessionId, messages ->
        Log.d("Chat", "Session $sessionId ended with ${messages.size} messages")
    }
    onError = { err -> Log.w("Chat", "Error: $err") }
    onClose = { finish() }
}

supportFragmentManager.beginTransaction()
    .replace(R.id.container, fragment)
    .commit()
```

```xml
<!-- activity_support.xml -->
<FrameLayout
    android:id="@+id/container"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

> **Java apps:** `RayaChatFragment.newInstance()` is annotated with `@JvmStatic`, so it works from Java: `RayaChatFragment.newInstance("token", "en")`.

### Mode 3: BottomSheet (overlay)

Chat slides up from the bottom as a draggable sheet. Works with both Compose and XML apps.

```kotlin
import ai.teammates.rayachat.ui.RayaChatBottomSheet

// Create the sheet with callbacks configured BEFORE showing
val sheet = RayaChatBottomSheet().apply {
    arguments = bundleOf("token" to "your-bot-token", "locale" to "en")
    onSessionStart = { sessionId -> Log.d("Chat", "Session: $sessionId") }
    onSessionEnd = { sessionId, messages ->
        Log.d("Chat", "Session $sessionId ended with ${messages.size} messages")
    }
    onError = { err -> Log.w("Chat", "Error: $err") }
}
sheet.show(supportFragmentManager, "raya_chat")

// Or use the convenience method (callbacks can be set on the returned instance)
// val sheet = RayaChatBottomSheet.show(supportFragmentManager, "your-bot-token")
```

> **Requires `AppCompatActivity`** (or `FragmentActivity`), not `ComponentActivity`, because `BottomSheetDialogFragment` needs `AppCompat`. If your Activity extends `ComponentActivity`, switch to `AppCompatActivity`.

### Mode 4: Headless (custom UI)

Full control — all chat logic with zero pre-built UI. Build your own screens with Kotlin StateFlow.

```kotlin
import ai.teammates.rayachat.core.RayaChatClient
import ai.teammates.rayachat.core.RayaChatConfig
import ai.teammates.rayachat.core.models.UserInfo

// 1. Create client
val client = RayaChatClient(
    context = applicationContext,
    config = RayaChatConfig(
        token = "your-bot-token",
        locale = "en",
        onSessionStart = { sessionId -> Log.d("Chat", "Session: $sessionId") },
        onSessionEnd = { sessionId, messages ->
            Log.d("Chat", "Session $sessionId ended with ${messages.size} messages")
        },
        onError = { err -> Log.w("Chat", "Error: $err") },
    )
)

// 2. Fetch bot config + connect
lifecycleScope.launch {
    val botConfig = client.fetchBotConfig()
    client.connect(UserInfo("John", "john@test.com", ""), botConfig)
}

// 3. Observe state — all reactive via StateFlow
lifecycleScope.launch {
    client.messages.collect { messages -> /* update your message list UI */ }
}
lifecycleScope.launch {
    client.currentMessage.collect { text -> /* show streaming bot response */ }
}
lifecycleScope.launch {
    client.loading.collect { isLoading -> /* show/hide typing indicator */ }
}

// 4. Send messages when the user taps send
client.sendMessage("Hello")

// 5. Clean up in onDestroy
override fun onDestroy() {
    super.onDestroy()
    client.destroy()  // releases WebSocket, cancels coroutines — does NOT clear storage
}
```

> **Full headless guide:** See [Headless Mode — Full Guide](#headless-mode--full-guide) below for all state fields, actions, command handling, and streaming.

---

## Configuration

All 4 modes accept the same configuration parameters. In packaged UI modes (1-3), pass them as function parameters. In headless mode (4), pass them via `RayaChatConfig`.

| Param | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `token` | `String` | **Yes** | — | Bot token from [Teammates.ai](https://teammates.ai) dashboard |
| `locale` | `String` | No | `"en"` | `"en"` (English) or `"ar"` (Arabic with RTL layout) |
| `imagePickerAdapter` | `ImagePickerAdapter` | No | `null` | Adapter for image selection. Image button hidden if not provided. |
| `audioRecorderAdapter` | `AudioRecorderAdapter` | No | `null` | Adapter for voice recording. Mic button hidden if not provided. |
| `onSessionStart` | `(sessionId: String) -> Unit` | No | `null` | Called when WebSocket connects successfully |
| `onSessionEnd` | `(sessionId: String, messages: List<TypeMessage>) -> Unit` | No | `null` | Called when session ends — receives session ID and full message history |
| `onError` | `(error: String) -> Unit` | No | `null` | Called on connection or send errors |
| `onClose` | `() -> Unit` | No | `null` | Called when user taps the close (X) button |

### Bot Configuration (from API)

These are configured on the [Teammates.ai](https://teammates.ai) dashboard and fetched automatically by the SDK. You do **not** set these in code.

| Property | Controls |
|----------|----------|
| `theme` | `"light"` or `"dark"` mode |
| `chatbox_gradient_color` | Primary/brand color (header, buttons, user bubbles) |
| `chatbox_chat_icon` | Bot avatar image URL |
| `chatbox_system_heading` | Intro screen heading text |
| `chatbox_system_paragraph` | Intro screen subtitle |
| `chatbox_initial_msg` | First bot message when chat starts |
| `chatbox_placeholder` | Message input placeholder text |
| `enable_user_form` | Show/skip user info form before chat |
| `enable_user_email` | Show email field in the form |
| `enable_user_phone` | Show phone field in the form |
| `enable_voice_note` | Enable microphone button (still requires adapter) |
| `enable_image_upload` | Enable image upload button (still requires adapter) |
| `preset_options` | Static suggestion buttons shown on first message |

---

## Callbacks

All callbacks are optional. They fire in all modes (packaged UI and headless).

### `onSessionStart(sessionId: String)`

Fires when the WebSocket connection is established. The `sessionId` is assigned by the server and identifies this conversation. Use it for logging, analytics, or linking to your backend.

```kotlin
onSessionStart = { sessionId ->
    analytics.track("chat_started", mapOf("session_id" to sessionId))
}
```

### `onSessionEnd(sessionId: String, messages: List<TypeMessage>)`

Fires when the session ends. Receives the session ID and **complete message history** captured before state is cleared. This fires regardless of how the session ends:

- User confirms "End Session" in the modal
- Server sends `feedback_received` → countdown finishes → session ends
- Server sends `auto_close` (inactivity timeout)
- Developer calls `client.endSession()` in headless mode

```kotlin
onSessionEnd = { sessionId, messages ->
    // Export transcript, send to your API, log analytics, etc.
    Log.d("Chat", "Session $sessionId: ${messages.size} messages")
}
```

See [Exporting Session Data](#exporting-session-data-onsessionend) for a full example.

### `onError(error: String)`

Fires on connection failures, send failures, or server errors. Error messages are sanitized (HTML stripped, sensitive data redacted, max 200 chars).

```kotlin
onError = { error ->
    // Show a toast, log to Crashlytics, etc.
    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
}
```

### `onClose()`

Fires when the user taps the close (X) button in the header. In packaged UI modes, use this to navigate back or finish the Activity. In BottomSheet mode, the sheet dismisses automatically — `onClose` is not needed.

```kotlin
onClose = { finish() }  // or navController.popBackStack()
```

---

## What Packaged UI Handles for You

When you use Mode 1, 2, or 3, the SDK handles all of the following automatically. You do **not** need to build or manage any of this:

| Feature | Details |
|---------|---------|
| **Intro screen** | Bot avatar, heading, subtitle, "Start a chat" button, privacy note, powered-by footer |
| **User form** | Full name (required), email (optional), phone (optional), floating labels, validation, error messages |
| **Chat screen** | Message list, typing indicator, streaming responses, markdown rendering, timestamps |
| **Message composer** | Text input, emoji (system keyboard), image button, mic button, send button |
| **Bot config fetch** | Fetches theme, colors, form settings, initial message from API on launch |
| **Theme** | Light/dark mode, gradient colors, contrast text — all from bot config |
| **Commands** | Rating (5 faces), feedback (textarea), end session (yes/no), countdown timer, auto-close |
| **End chat modal** | Confirmation dialog with cancel/end buttons, keyboard dismissal |
| **Image handling** | Preview grid with remove buttons, full-screen viewer on tap |
| **Audio handling** | Recording waveform, playback bar (requires adapters) |
| **Preset buttons** | Dynamic suggestion pills from server + static presets from config |
| **Escalation** | "Connect with human representative" button when server triggers it |
| **Keyboard avoidance** | Chat input stays above keyboard automatically (`imePadding()`) |
| **Auto-scroll** | Scrolls to latest message; floating "scroll to bottom" button when scrolled up |
| **RTL** | Full Arabic layout, mirrored bubbles, translated strings (when `locale = "ar"`) |
| **Reconnection** | Exponential backoff, message queue, session resume — all invisible to the user |
| **Persistence** | Messages and session ID survive app restart and process death |
| **Configuration changes** | Rotation, dark mode toggle — ViewModel survives, no state loss |

---

## Adapters

The SDK uses **pluggable adapters** for native device features (camera, microphone). This keeps the SDK dependency-free — your app provides the native bridge.

If you don't provide an adapter, the corresponding button is **hidden** (not disabled):

| Adapters provided | Buttons shown in composer |
|-------------------|--------------------------|
| None | Emoji + Send only |
| `imagePickerAdapter` only | Emoji + Image + Send |
| `audioRecorderAdapter` only | Emoji + Mic + Send |
| Both adapters | Emoji + Image + Mic + Send |

### ImagePickerAdapter

```kotlin
import ai.teammates.rayachat.core.adapters.ImagePickerAdapter
import ai.teammates.rayachat.core.models.ImageAsset

interface ImagePickerAdapter {
    suspend fun pickImages(maxCount: Int): List<ImageAsset>
}
```

`ImageAsset` fields:

| Field | Type | Description |
|-------|------|-------------|
| `uri` | `String` | Local file URI (for display in preview) |
| `name` | `String` | Filename, e.g., `"photo.jpg"` |
| `type` | `String` | MIME type, e.g., `"image/jpeg"` |
| `base64` | `String` | Full data URL: `"data:image/jpeg;base64,/9j/4AAQ..."` |

**Complete example using Android Photo Picker:**

```kotlin
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import ai.teammates.rayachat.core.adapters.ImagePickerAdapter
import ai.teammates.rayachat.core.models.ImageAsset
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.math.max

class MyImagePickerAdapter(private val activity: ComponentActivity) : ImagePickerAdapter {

    private var pendingCallback: ((List<Uri>) -> Unit)? = null

    // MUST be called during Activity creation (before STARTED state)
    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        pendingCallback?.invoke(uris)
        pendingCallback = null
    }

    override suspend fun pickImages(maxCount: Int): List<ImageAsset> {
        if (pendingCallback != null) return emptyList() // already picking

        return suspendCancellableCoroutine { continuation ->
            pendingCallback = { uris ->
                val assets = uris.take(maxCount).mapNotNull { uri -> uriToImageAsset(uri) }
                continuation.resume(assets)
            }
            continuation.invokeOnCancellation { pendingCallback = null }
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    private fun uriToImageAsset(uri: Uri): ImageAsset? {
        return try {
            // Decode and scale down to max 1024px
            val maxDim = 1024
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            activity.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            val sampleSize = max(1, max(options.outWidth, options.outHeight) / maxDim)
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = activity.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return null

            // Scale further if needed
            val larger = max(bitmap.width, bitmap.height)
            val scaled = if (larger > maxDim) {
                val scale = maxDim.toFloat() / larger
                Bitmap.createScaledBitmap(
                    bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true
                ).also { if (it !== bitmap) bitmap.recycle() }
            } else bitmap

            // Compress to JPEG base64
            val stream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 70, stream)
            val base64Str = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            scaled.recycle()

            ImageAsset(
                uri = uri.toString(),
                name = uri.lastPathSegment?.substringAfterLast('/') ?: "image.jpg",
                type = "image/jpeg",
                base64 = "data:image/jpeg;base64,$base64Str",
            )
        } catch (e: Exception) {
            null
        }
    }
}
```

**Usage in your Activity:**

```kotlin
class SupportActivity : ComponentActivity() {

    // IMPORTANT: Create adapter BEFORE super.onCreate() — ActivityResultLauncher
    // must be registered before the Activity reaches STARTED state.
    private val imageAdapter = MyImagePickerAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RayaChatWidget(
                token = "your-bot-token",
                imagePickerAdapter = imageAdapter,
            )
        }
    }
}
```

### AudioRecorderAdapter

```kotlin
import ai.teammates.rayachat.core.adapters.AudioRecorderAdapter
import ai.teammates.rayachat.core.adapters.AudioResult

interface AudioRecorderAdapter {
    suspend fun startRecording()
    suspend fun stopRecording(): AudioResult  // { uri: String, base64: String? }
    suspend fun pauseRecording()
    suspend fun resumeRecording()
    suspend fun getAmplitude(): Float          // 0f..1f for waveform visualization
    suspend fun cleanup()                      // release MediaRecorder resources
}
```

**Example implementation using MediaRecorder:**

```kotlin
import android.media.MediaRecorder
import android.util.Base64
import ai.teammates.rayachat.core.adapters.AudioRecorderAdapter
import ai.teammates.rayachat.core.adapters.AudioResult
import java.io.File

class MyAudioRecorderAdapter(private val cacheDir: File) : AudioRecorderAdapter {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    override suspend fun startRecording() {
        val file = File(cacheDir, "raya_voice_${System.currentTimeMillis()}.m4a")
        outputFile = file

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
    }

    override suspend fun stopRecording(): AudioResult {
        recorder?.apply { stop(); release() }
        recorder = null

        val file = outputFile ?: return AudioResult("", null)
        val bytes = file.readBytes()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return AudioResult(uri = file.toURI().toString(), base64 = base64)
    }

    override suspend fun pauseRecording() { recorder?.pause() }
    override suspend fun resumeRecording() { recorder?.resume() }

    override suspend fun getAmplitude(): Float {
        val maxAmplitude = recorder?.maxAmplitude ?: 0
        return (maxAmplitude / 32767f).coerceIn(0f, 1f)
    }

    override suspend fun cleanup() {
        recorder?.release()
        recorder = null
        outputFile?.delete()
        outputFile = null
    }
}
```

> **Permissions:** Your app must request `android.permission.RECORD_AUDIO` at runtime before using the audio recorder. The SDK does **not** request this permission for you.

---

## Features

### Chat
- Real-time text messaging via WebSocket
- Streaming bot responses (chunks rendered as they arrive)
- Markdown rendering in bot messages (bold, italic, code, links, lists, headings)
- Message persistence across app restarts (Room Database, up to 500 messages)
- Auto-scroll to latest message

### Media
- Image upload (up to 5 per message) with preview grid and remove buttons
- Full-screen image viewer on tap
- Voice note recording with waveform visualization (requires adapter)
- Voice note playback with progress bar

### Interactive
- Preset/suggestion buttons (static from config + dynamic from server)
- Server command system: rating, feedback, end session, countdown, auto-close
- End chat confirmation modal
- Escalation to human agent button
- Emoji via system keyboard

### Connection
- WebSocket heartbeat (ping every 25s, timeout after 60s)
- Automatic reconnection with exponential backoff (max 100 attempts, 30s cap)
- Message queue during reconnection (messages sent while connecting are queued and flushed on open)
- Session ID persistence — reconnection resumes the same conversation
- App background/foreground awareness (stops heartbeat when backgrounded, reconnects on foreground)
- Online/offline detection via `ConnectivityManager`

---

## Theming

The SDK automatically fetches theme settings from your bot configuration. No manual setup needed.

**How it works:**

1. `theme: "light" | "dark"` — controls background, cards, borders, and text colors
2. `chatbox_gradient_color` — controls header, buttons, and user message bubbles
3. Text on the gradient **automatically adapts**: white text on dark gradients, dark text on light gradients

These two settings are independent. A dark theme can have a light gradient color and vice versa.

**Dark mode colors:**

| Element | Color |
|---------|-------|
| Background | `#14161A` |
| Cards/Bubbles | `#2C2D31` |
| Borders | `#3F3F46` |
| Text | `#F1F1F0` |

**Light mode colors:**

| Element | Color |
|---------|-------|
| Background | `#FFFFFF` |
| Bot bubbles | `#F5F5F5` |
| Borders | `#E4E4E7` |
| Text | `#14161A` |

---

## RTL / Arabic Support

Set `locale = "ar"` — the entire UI adapts automatically:

- All text right-aligned
- Message bubbles mirrored (user left, bot right)
- Navigation arrows flipped
- Arabic translations for all built-in strings (form labels, buttons, errors, placeholders)
- Per-message language detection for mixed-language chats
- **No global mutation** — the SDK does NOT change your app's `layoutDirection` or any global Android setting. RTL is scoped entirely within the SDK's views.

---

## Headless Mode — Full Guide

Headless mode gives you the complete chat engine (`RayaChatClient`) with all state exposed as Kotlin `StateFlow`. You build the entire UI yourself — the SDK handles WebSocket, reconnection, persistence, commands, and state management.

### All State (StateFlow)

Collect these in your UI to react to changes:

| State | Type | Description |
|-------|------|-------------|
| `messages` | `StateFlow<List<TypeMessage>>` | Full message history (persisted across app restarts). Image/audio attachments are updated with remote URLs once the server processes them. |
| `currentMessage` | `StateFlow<String>` | Streaming text — grows as chunks arrive, cleared on RESPONSE |
| `connectionStatus` | `StateFlow<ConnectionStatus>` | `CONNECTING`, `CONNECTED`, `DISCONNECTED`, `RECONNECTING` |
| `isConnected` | `StateFlow<Boolean>` | `true` when WebSocket is open and healthy |
| `isOnline` | `StateFlow<Boolean>` | Device has network connectivity |
| `loading` | `StateFlow<Boolean>` | `true` when bot is processing (STEP received, cleared on RESPONSE) |
| `status` | `StateFlow<String?>` | Status text from server: `"Searching..."`, `"Thinking..."`, etc. |
| `info` | `StateFlow<String?>` | Info text: `"Waiting for human agent..."`, etc. |
| `commandData` | `StateFlow<CommandData?>` | Active server command — see [Handling Commands](#handling-commands) |
| `presets` | `StateFlow<List<String>>` | Suggestion button titles from server |
| `showHumanAgentBtn` | `StateFlow<Boolean>` | `true` when escalation to human agent is available |
| `sessionCloseInfo` | `StateFlow<SessionCloseInfo?>` | Non-null when server closed the session (auto_close) |
| `currentSessionId` | `StateFlow<String>` | Server-assigned session ID (empty before first response) |

### All Actions

| Action | Signature | When to call |
|--------|-----------|--------------|
| `connect` | `suspend fun connect(userInfo: UserInfo, botConfig: BotConfigProps?)` | After user submits form (or with empty `UserInfo` to skip form) |
| `fetchBotConfig` | `suspend fun fetchBotConfig(): BotConfigProps` | Before `connect()` — needed for initial bot message |
| `sendMessage` | `fun sendMessage(text: String)` | User taps send button |
| `sendImages` | `fun sendImages(images: List<ImagePayload>, caption: String)` | User sends images |
| `sendAudio` | `fun sendAudio(base64: String)` | User sends voice note |
| `sendPreset` | `fun sendPreset(text: String)` | User taps a suggestion button |
| `sendCommandResponse` | `fun sendCommandResponse(command: String, response: Any)` | User responds to a command (rating, feedback, etc.) |
| `clearSessionCloseInfo` | `fun clearSessionCloseInfo()` | When starting a new chat after auto_close |
| `endSession` | `suspend fun endSession()` | User wants to end the session (clears all storage) |
| `destroy` | `fun destroy()` | Activity/Fragment `onDestroy` (releases resources, keeps storage) |

### Lifecycle: endSession vs destroy

| Method | Clears storage? | Closes WebSocket? | Fires `onSessionEnd`? | When to call |
|--------|----------------|-------------------|----------------------|-------------|
| `endSession()` | **Yes** — deletes messages, session ID, user info | Yes | **Yes** — with session ID + messages | User taps "End Session" |
| `destroy()` | **No** — messages and session persist for resume | Yes | No | Activity/Fragment `onDestroy` |

> `endSession()` snapshots the session ID and messages **before** clearing, then passes both to `onSessionEnd`. This is safe even when triggered by a server command.

### Handling Streaming Messages

The bot sends responses in chunks. Here's the typical sequence:

```
Server: STEP  { text: "Searching..." }     → loading = true, status = "Searching..."
Server: CHUNK { text: "Here are " }         → currentMessage = "Here are "
Server: CHUNK { text: "the results" }       → currentMessage = "Here are the results"
Server: RESPONSE { data: { content: ... } } → message added to messages, currentMessage = "", loading = false
```

In your UI, render `currentMessage` as a temporary bot bubble at the bottom of the list:

```kotlin
@Composable
fun ChatScreen(client: RayaChatClient) {
    val messages by client.messages.collectAsState()
    val streamingText by client.currentMessage.collectAsState()
    val isLoading by client.loading.collectAsState()

    LazyColumn {
        // Render finalized messages
        items(messages) { msg ->
            MessageBubble(msg)
        }

        // Show streaming bot response (temporary — disappears when RESPONSE arrives)
        if (streamingText.isNotEmpty()) {
            item { BotBubble(text = streamingText) }
        }

        // Show typing indicator while waiting for first chunk
        if (isLoading && streamingText.isEmpty()) {
            item { TypingIndicator() }
        }
    }
}
```

### Handling Commands

The server sends commands for interactive UI (rating, feedback, end session). When `commandData` is non-null, you should:

1. Show the appropriate UI (disable the message composer)
2. Collect the user's response
3. Call `sendCommandResponse(command, response)`

```kotlin
val command by client.commandData.collectAsState()

when (command?.type) {
    "rate_conversation" -> {
        // Show 5 rating icons (1-5). command.message = "How would you rate this conversation?"
        // command.options = [1, 2, 3, 4, 5]
        RatingUI(
            message = command!!.message,
            onRate = { rating -> client.sendCommandResponse("rate_conversation", rating) }
        )
    }

    "submit_feedback" -> {
        // Show textarea + Skip/Submit buttons. command.message = "Any feedback?"
        FeedbackUI(
            message = command!!.message,
            optional = command!!.optional,  // true = show Skip button
            onSubmit = { text -> client.sendCommandResponse("submit_feedback", text) },
            onSkip = { client.sendCommandResponse("submit_feedback", "") },
        )
    }

    "end_session" -> {
        // Show Yes/No buttons. command.message = "Do you want to end this session?"
        EndSessionUI(
            message = command!!.message,
            onConfirm = { client.sendCommandResponse("end_session", "yes") },
            onCancel = { client.sendCommandResponse("end_session", "no") },
        )
    }

    "feedback_received" -> {
        // Show countdown (3 seconds), then call endSession()
        // command.message = "Thank you for your feedback!"
        CountdownUI(
            message = command!!.message,
            seconds = 3,
            onComplete = { lifecycleScope.launch { client.endSession() } },
        )
    }

    "auto_close" -> {
        // Session closed by server due to inactivity.
        // sessionCloseInfo is already set — navigate back to intro/start screen.
        // Call clearSessionCloseInfo() when user starts a new chat.
    }
}
```

**Command flow (typical):**

```
end_session → user picks Yes/No
    → if Yes: rate_conversation → user rates 1-5
        → submit_feedback → user types feedback or skips
            → feedback_received → 3s countdown → endSession()
```

---

## Exporting Session Data (onSessionEnd)

The `onSessionEnd` callback receives the session ID and full message history, captured before state is cleared. This works in all modes and fires regardless of how the session ends.

### Packaged UI example

```kotlin
class SupportActivity : AppCompatActivity() {

    private val customerId by lazy { intent.getStringExtra("customer_id")!! }
    private val rideId by lazy { intent.getStringExtra("ride_id")!! }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RayaChatWidget(
                token = "your-bot-token",
                onSessionEnd = { sessionId, messages ->
                    lifecycleScope.launch {
                        api.post("/support/sessions", body = mapOf(
                            "customer_id" to customerId,
                            "ride_id" to rideId,
                            "session_id" to sessionId,
                            "message_count" to messages.size,
                            "transcript" to messages.map { msg ->
                                mapOf(
                                    "sender" to if (msg.sender == 1) "user" else "bot",
                                    "content" to msg.content,
                                    "timestamp" to msg.createdAt,
                                )
                            },
                        ))
                    }
                },
                onClose = { finish() },
            )
        }
    }
}
```

### Headless example

```kotlin
val client = RayaChatClient(
    context = applicationContext,
    config = RayaChatConfig(
        token = "your-bot-token",
        onSessionEnd = { sessionId, messages ->
            // Works even when endSession() is triggered by server commands
            scope.launch { api.post("/support/sessions", ...) }
        },
    )
)
```

### What `onSessionEnd` receives

| Parameter | Type | Description |
|-----------|------|-------------|
| `sessionId` | `String` | The server-assigned session ID (empty string if session never connected) |
| `messages` | `List<TypeMessage>` | Complete message history at the moment the session ended |

Each `TypeMessage` contains:

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Unique message ID |
| `sender` | `Int` | `1` = user, `2` = bot, `3` = system |
| `type` | `Int` | `1` = text, `2` = audio, `3` = image |
| `content` | `String?` | Message text (may contain markdown for bot messages) |
| `createdAt` | `String?` | Unix timestamp in seconds |
| `attachmentsJson` | `String?` | JSON array of image attachments with **remote URLs** (safe to store in your DB) |
| `audioJson` | `String?` | JSON audio data with **remote URL** (safe to store in your DB) |

> **Image/audio URLs are server URLs, not local paths.** When the user sends images or audio, the SDK initially stores local URIs. Once the server processes the upload and responds, the SDK automatically replaces them with permanent remote URLs (e.g., `https://s3.amazonaws.com/...`). By the time `onSessionEnd` fires, all attachments contain remote URLs that can be stored in your database or accessed from any device.

---

## Session Persistence

The SDK persists data so conversations survive app restarts and process death:

| Data | Storage | Encrypted |
|------|---------|-----------|
| Session ID | EncryptedSharedPreferences | Yes (AES-256) |
| Messages (up to 500) | Room Database (SQLite) | No |
| User info | EncryptedSharedPreferences | Yes (AES-256) |

**How it works:**

1. User chats, messages are saved to Room on every send/receive
2. User kills the app or the OS kills the process
3. User reopens the app and taps "Start a chat"
4. SDK reads stored session ID and messages from storage
5. WebSocket connects with the stored session ID — server resumes the conversation
6. Previous messages appear immediately in the chat

**When data is cleared:**

- `endSession()` — clears everything (session ID, messages, user info)
- `destroy()` — does **not** clear storage (data survives for session resume)

> **Encryption fallback:** If `EncryptedSharedPreferences` fails on older or rooted devices, the SDK falls back to regular `SharedPreferences` with a logged warning. It never crashes.

---

## Background / Foreground Behavior

| Duration in background | What happens on return |
|-----------------------|------------------------|
| < 60 seconds | WebSocket likely survived. Returns seamlessly. |
| 1-5 minutes | WebSocket may have died. SDK auto-reconnects with same session. |
| 5+ minutes | WebSocket dead. Stale streaming state cleared. SDK auto-reconnects with saved session ID. |
| Process killed by OS | Everything in memory lost. Room + SharedPrefs survive. User taps "Start Chat" → messages restored. |

The SDK uses `ProcessLifecycleOwner` to detect foreground/background transitions. When backgrounded, it stops the heartbeat to save battery. When foregrounded, it reconnects if needed and clears any stale streaming state.

---

## Keeping Chat Alive Across Tabs

If your app has tab navigation, keep the chat widget mounted — hide it visually instead of removing it from the composition:

```kotlin
// Correct — stays mounted, preserves chat state
Box(modifier = if (activeTab == "support") Modifier else Modifier.size(0.dp)) {
    RayaChatWidget(token = "...")
}

// Wrong — unmounts on tab switch, resets to intro
if (activeTab == "support") {
    RayaChatWidget(token = "...")
}
```

When `RayaChatWidget` leaves the composition, `destroy()` is called automatically (via `DisposableEffect`), which closes the WebSocket. Mounting it again starts a fresh session from the intro screen (though stored messages are restored).

---

## ProGuard / R8

The SDK ships ProGuard rules inside the AAR (`consumer-rules.pro`). These are applied automatically when your app builds with minification enabled. You do **not** need to add any ProGuard rules yourself.

If you encounter issues with a release build, check that you are not stripping the SDK's model classes. The included rules keep:
- All public API classes (`RayaChatClient`, `RayaChatConfig`, `RayaChatWidget`, etc.)
- All model classes in `ai.teammates.rayachat.core.models`
- OkHttp WebSocket listener methods

---

## Architecture

```
┌─────────────────────────────────────────────────┐
│              raya-chat-core (~500KB)             │
│                                                  │
│  WebSocket · Heartbeat · Reconnection · Queue    │
│  Message Parser · Commands · Error Sanitizer     │
│  Bot Config Fetch · Session Storage              │
│  App Lifecycle · Network Detection               │
│                                                  │
│  Storage:                                        │
│  → Room Database (messages)                      │
│  → EncryptedSharedPreferences (session + user)   │
│                                                  │
│  Kotlin Coroutines + StateFlow                   │
│  OkHttp WebSocket (shared singleton)             │
└──────────────┬──────────────┬────────────────────┘
               │              │
    ┌──────────▼──────┐  ┌───▼──────────────────┐
    │  raya-chat-ui   │  │  Developer's own UI   │
    │  (~1.5MB)       │  │                       │
    │                 │  │  Mode 4: Headless     │
    │  Mode 1: Widget │  │  (Compose or XML)     │
    │  Mode 2: Fragment│  │                       │
    │  Mode 3: Sheet  │  │  client.messages      │
    │                 │  │  client.sendMessage()  │
    │  Jetpack Compose│  │                       │
    └─────────────────┘  └───────────────────────┘
```

### Project Structure

```
raya-chat-android/
├── raya-chat-core/                    # Headless engine (no UI)
│   └── src/main/kotlin/.../core/
│       ├── RayaChatClient.kt          # Main entry point
│       ├── RayaChatConfig.kt          # Configuration
│       ├── Constants.kt               # SDK constants
│       ├── models/                    # 12 data classes + enums
│       ├── adapters/                  # 3 adapter interfaces
│       ├── websocket/                 # WebSocket + message queue
│       ├── api/                       # Bot config fetch + URL builder
│       ├── protocol/                  # Message handler (10 types)
│       ├── storage/                   # Room DB + encrypted prefs
│       ├── lifecycle/                 # Background/foreground observer
│       ├── network/                   # Connectivity monitor + HTTP client
│       └── util/                      # Color, validation, time, RTL, error sanitizer
│
├── raya-chat-ui/                      # Packaged UI (Compose)
│   └── src/main/kotlin/.../ui/
│       ├── RayaChatWidget.kt          # Mode 1: @Composable
│       ├── RayaChatFragment.kt        # Mode 2: Fragment
│       ├── RayaChatBottomSheet.kt     # Mode 3: BottomSheet
│       ├── RayaChatViewModel.kt       # State machine (INTRO → FORM → CHAT)
│       ├── screens/                   # IntroScreen, FormScreen, ChatScreen
│       ├── components/chat/           # MessageBubble, Composer, Presets, Typing
│       ├── components/commands/       # Rating, Feedback, EndSession, Countdown
│       ├── components/media/          # ImageViewer, ImagePreview, Audio
│       ├── components/common/         # Header, Icons, Strings, Toast
│       └── theme/                     # Colors, typography, RTL utils
│
└── sample/                            # Demo app (4 integration modes)
```

---

## Requirements

| Requirement | Version |
|-------------|---------|
| Kotlin | 2.1+ |
| Android SDK | API 24+ (Android 7.0) |
| compileSdk | 35 |
| Java | 17 |

### Dependencies (auto-included)

These are bundled with the SDK. You do **not** need to add them to your `build.gradle`:

| Library | Used by | Purpose |
|---------|---------|---------|
| OkHttp 4.12 | `core` | WebSocket + HTTP |
| Room 2.6 | `core` | Message persistence |
| Kotlinx Serialization 1.7 | `core` | JSON parsing |
| AndroidX Security Crypto | `core` | Encrypted SharedPreferences |
| Jetpack Compose (BOM 2024.12) | `ui` | UI framework |
| Coil 2.7 | `ui` | Image loading |
| Markwon 4.6 | `ui` | Markdown rendering |

> If your app already uses a different version of OkHttp or Room, Gradle will resolve to the higher version. This is safe — the SDK uses stable APIs only.

---

## Sample App

The `sample/` module demonstrates all 4 integration modes:

```bash
./gradlew :sample:installDebug
```

> **Note:** The sample app uses a test bot token defined in `sample/.../SampleToken.kt`. Replace it with your own token from the [Teammates.ai](https://teammates.ai) dashboard to test with your bot.

| Demo | Mode | What it shows |
|------|------|--------------|
| Compose Widget | Mode 1 | Full chat widget + image picker adapter |
| Fragment | Mode 2 | Chat in XML FrameLayout |
| BottomSheet | Mode 3 | Chat slides up as overlay |
| Headless | Mode 4 | Fully custom UI with all features |

---

## Troubleshooting

### WebSocket not connecting

1. Verify your `token` is correct (from [Teammates.ai](https://teammates.ai) dashboard)
2. Ensure the device has network access
3. Check Logcat with tag filter `RayaChat.WS` for connection logs
4. Check Logcat with tag filter `RayaChat.API` for bot config fetch errors

### Images not uploading

- Ensure you've provided an `imagePickerAdapter`. Without it, the image button is hidden.
- The adapter must be created **before** `super.onCreate()` — Android requires `ActivityResultLauncher` registration before the Activity reaches `STARTED` state.
- Check that images are being converted to base64 correctly. The `base64` field must include the data URL prefix: `"data:image/jpeg;base64,..."`.

### Voice recording not working

- Ensure you've provided an `audioRecorderAdapter`. Without it, the mic button is hidden.
- Your app must request `RECORD_AUDIO` permission at runtime **before** the user taps the mic button. The SDK does not request this permission.

### Chat resets to intro screen when switching tabs

The widget cleans up when removed from composition. Keep `RayaChatWidget` mounted and hide it visually instead. See [Keeping Chat Alive Across Tabs](#keeping-chat-alive-across-tabs).

### BottomSheet crashes on ComponentActivity

`RayaChatBottomSheet` uses `BottomSheetDialogFragment`, which requires `AppCompatActivity` (or `FragmentActivity`). Change your Activity to extend `AppCompatActivity`.

### Bot config fetch fails

Check Logcat with tag `RayaChat.API`. Common causes:
- Invalid or expired token
- No network connectivity
- Server returns null fields (handled gracefully — SDK uses defaults)

### Keyboard covers the chat input

The SDK uses `Modifier.imePadding()` on the chat screen. If your host Activity has `android:windowSoftInputMode="adjustResize"` or its own `imePadding`, they may conflict. Remove the outer keyboard handling and let the SDK manage it.

### App crashes on rotation

The SDK uses `ViewModelProvider` so the ViewModel survives configuration changes (rotation, dark mode toggle, locale change). If you see a crash on an older build, update to the latest version.

### Messages not persisting after app restart

- Ensure you are **not** calling `endSession()` in `onDestroy`. Use `destroy()` instead — it releases resources without clearing storage.
- Check that Room database is not being cleared by your app's own data-clearing logic.

### Debugging

The SDK logs all significant events to Logcat. Use these tag filters:

| Tag | What it logs |
|-----|-------------|
| `RayaChat.WS` | WebSocket connect, disconnect, send, receive, heartbeat, reconnect |
| `RayaChat.API` | Bot config fetch request/response |
| `RayaChatClient` | Session lifecycle, state changes, errors |
| `RayaChat.Prefs` | Storage read/write, encryption fallback |
| `RayaChat.ImagePicker` | Image processing (sample app adapter) |

---

## License

MIT
