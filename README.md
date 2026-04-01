# Raya Chat Android SDK

Native Android SDK for embedding the Raya AI chat widget in Android apps. Built with Kotlin + Jetpack Compose.

Works with **Jetpack Compose**, **XML layout + Fragment**, **BottomSheet**, and **headless (custom UI)** apps.

---

## Table of Contents

- [Installation](#installation)
- [Quick Start](#quick-start)
  - [Mode 1: Compose Widget](#mode-1-compose-widget)
  - [Mode 2: Fragment](#mode-2-fragment-xml-layout-apps)
  - [Mode 3: BottomSheet](#mode-3-bottomsheet-overlay)
  - [Mode 4: Headless](#mode-4-headless-custom-ui)
- [Configuration](#configuration)
- [Adapters](#adapters)
  - [Image Picker](#imagepickeradapter)
  - [Audio Recorder](#audiorecorderadapter)
- [Features](#features)
- [Theming](#theming)
- [RTL / Arabic Support](#rtl--arabic-support)
- [Architecture](#architecture)
- [Headless Mode — Full Guide](#headless-mode--full-guide)
- [Session Persistence](#session-persistence)
- [Background / Foreground Behavior](#background--foreground-behavior)
- [Keeping Chat Alive Across Tabs](#keeping-chat-alive-across-tabs)
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

---

## Quick Start

### Mode 1: Compose Widget

The simplest integration — 3 lines of code. Full chat widget with intro, form, and chat screens.

```kotlin
import ai.teammates.rayachat.ui.RayaChatWidget

@Composable
fun SupportScreen() {
    RayaChatWidget(
        token = "your-bot-token",
        locale = "en",
        onSessionStart = { id -> Log.d("Chat", "Session: $id") },
        onError = { err -> Log.w("Chat", "Error: $err") },
        onClose = { /* handle close */ },
    )
}
```

### Mode 2: Fragment (XML layout apps)

For apps using XML layouts, Java, or Navigation Component. The Fragment wraps Compose internally — your app doesn't need Compose.

```kotlin
import ai.teammates.rayachat.ui.RayaChatFragment

// In your Activity
val fragment = RayaChatFragment.newInstance("your-bot-token", "en").apply {
    onSessionStart = { id -> Log.d("Chat", "Session: $id") }
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

### Mode 3: BottomSheet (overlay)

Chat slides up from the bottom as a draggable sheet. Works with both Compose and XML apps.

```kotlin
import ai.teammates.rayachat.ui.RayaChatBottomSheet

// One-liner — slides up from bottom
val sheet = RayaChatBottomSheet.show(
    fragmentManager = supportFragmentManager,
    token = "your-bot-token",
    locale = "en",
)
sheet.onSessionStart = { id -> Log.d("Chat", "Session: $id") }
sheet.onError = { err -> Log.w("Chat", "Error: $err") }
```

> **Note:** The BottomSheet requires an `AppCompatActivity` (not `ComponentActivity`) because it uses `BottomSheetDialogFragment`.

### Mode 4: Headless (custom UI)

Full control — all chat logic with zero pre-built UI. Build your own screens with Kotlin Flow.

```kotlin
import ai.teammates.rayachat.core.RayaChatClient
import ai.teammates.rayachat.core.RayaChatConfig
import ai.teammates.rayachat.core.models.UserInfo

// Create client
val client = RayaChatClient(
    context = applicationContext,
    config = RayaChatConfig(
        token = "your-bot-token",
        locale = "en",
        onSessionStart = { id -> Log.d("Chat", "Session: $id") },
        onError = { err -> Log.w("Chat", "Error: $err") },
    )
)

// Fetch bot config + connect
lifecycleScope.launch {
    val botConfig = client.fetchBotConfig()
    client.connect(UserInfo("John", "john@test.com", ""), botConfig)
}

// Observe state — all reactive via StateFlow
lifecycleScope.launch {
    client.messages.collect { messages -> /* render messages */ }
}
lifecycleScope.launch {
    client.loading.collect { isLoading -> /* show/hide typing indicator */ }
}
lifecycleScope.launch {
    client.presets.collect { presets -> /* show suggestion buttons */ }
}

// Actions
client.sendMessage("Hello")
client.sendPreset("Ask about pricing")
client.sendImages(imagePayloads, "Check these out")
client.sendCommandResponse("rate_conversation", 5)
client.endSession()

// Cleanup when done
client.destroy()
```

> **Full headless guide:** See [Headless Mode — Full Guide](#headless-mode--full-guide) below for all state fields, actions, and building blocks.

---

## Configuration

### RayaChatWidget / RayaChatFragment / RayaChatBottomSheet

| Param | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `token` | String | Yes | — | Bot token from Teammates.ai dashboard |
| `locale` | String | No | `"en"` | `"en"` or `"ar"` (Arabic/RTL) |
| `imagePickerAdapter` | ImagePickerAdapter | No | — | Adapter for image selection. Button hidden if not provided. |
| `audioRecorderAdapter` | AudioRecorderAdapter | No | — | Adapter for voice recording. Button hidden if not provided. |
| `onSessionStart` | (String) -> Unit | No | — | Called with session ID when WebSocket connects |
| `onSessionEnd` | () -> Unit | No | — | Called when session ends |
| `onError` | (String) -> Unit | No | — | Called on connection/send errors |
| `onClose` | () -> Unit | No | — | Called when user closes the widget |

### Bot Configuration (from API)

These are configured on the Teammates.ai dashboard and fetched automatically:

| Property | Controls |
|----------|----------|
| `theme` | Light/dark mode |
| `chatbox_gradient_color` | Primary/brand color (header, buttons, user bubbles) |
| `chatbox_chat_icon` | Bot avatar image URL |
| `chatbox_system_heading` | Intro screen heading text |
| `chatbox_system_paragraph` | Intro screen subtitle |
| `chatbox_initial_msg` | First bot message in chat |
| `chatbox_placeholder` | Message input placeholder |
| `enable_user_form` | Show/skip user info form |
| `enable_user_email` | Show email field in form |
| `enable_user_phone` | Show phone field in form |
| `enable_voice_note` | Show microphone button (if adapter provided) |
| `enable_image_upload` | Show image upload button (if adapter provided) |
| `preset_options` | Static suggestion buttons on first message |

---

## Adapters

The SDK uses **pluggable adapters** for native device features. This keeps the SDK dependency-free — your app provides the native bridge.

**Button visibility:**

| Adapters provided | Buttons shown in composer |
|-------------------|--------------------------|
| None | Emoji + Send only |
| `imagePickerAdapter` only | Emoji + Paperclip + Send |
| `audioRecorderAdapter` only | Emoji + Mic + Send |
| Both adapters | Emoji + Paperclip + Mic + Send |

### ImagePickerAdapter

```kotlin
import ai.teammates.rayachat.core.adapters.ImagePickerAdapter
import ai.teammates.rayachat.core.models.ImageAsset

interface ImagePickerAdapter {
    suspend fun pickImages(maxCount: Int): List<ImageAsset>
}

// ImageAsset fields:
data class ImageAsset(
    val uri: String,      // Local file URI
    val name: String,     // Filename (e.g., "photo.jpg")
    val type: String,     // MIME type (e.g., "image/jpeg")
    val base64: String,   // Base64 data URL ("data:image/jpeg;base64,...")
)
```

**Example implementation using Android Photo Picker:**

```kotlin
class MyImagePickerAdapter(activity: ComponentActivity) : ImagePickerAdapter {

    private var callback: ((List<Uri>) -> Unit)? = null

    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris -> callback?.invoke(uris); callback = null }

    override suspend fun pickImages(maxCount: Int): List<ImageAsset> {
        return suspendCancellableCoroutine { continuation ->
            callback = { uris ->
                val assets = uris.take(maxCount).mapNotNull { uri ->
                    // Convert URI to ImageAsset with base64
                    uriToImageAsset(uri)
                }
                continuation.resume(assets)
            }
            launcher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        }
    }
}

// IMPORTANT: Create the adapter BEFORE super.onCreate()
class MyActivity : ComponentActivity() {
    private lateinit var imageAdapter: MyImagePickerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        imageAdapter = MyImagePickerAdapter(this) // before super
        super.onCreate(savedInstanceState)
        setContent {
            RayaChatWidget(
                token = "...",
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
    suspend fun getAmplitude(): Float          // 0f..1f for waveform
    suspend fun cleanup()
}
```

---

## Features

### Chat
- Real-time text messaging via WebSocket
- Streaming bot responses (chunks rendered as they arrive)
- Markdown rendering in bot messages (bold, italic, code, links, lists, headings)
- Message persistence across app restarts (Room Database)
- Auto-scroll to latest message

### Media
- Image upload (up to 5 per message) with preview grid and remove buttons
- Full-screen image viewer on tap
- Voice note recording with waveform (requires adapter)

### Interactive
- Preset/suggestion buttons (static from config + dynamic from server)
- Command system: `end_session`, `rate_conversation`, `submit_feedback`, `feedback_received`, `auto_close`
- End chat confirmation modal
- Escalation to human agent button
- Emoji via system keyboard

### Connection
- WebSocket heartbeat (ping every 25s, timeout after 60s)
- Automatic reconnection with exponential backoff (max 100 attempts, 30s cap)
- Message queue during reconnection (messages sent while connecting are queued and flushed on open)
- Session ID persistence — reconnection resumes the same conversation
- App background/foreground awareness (stops heartbeat when backgrounded, reconnects on foreground)
- Online/offline detection via ConnectivityManager

---

## Theming

The SDK automatically fetches theme settings from your bot configuration. No manual setup needed.

**How it works:**
- `theme: "light" | "dark"` — controls global background, card, border, and text colors
- `chatbox_gradient_color` — controls header, buttons, user message bubbles
- Text on the gradient automatically adapts: white text on dark gradients, dark text on light gradients

**Dark mode colors:**

| Element | Color |
|---------|-------|
| Background | `#14161A` |
| Cards/Bubbles | `#2C2D31` |
| Borders | `#3F3F46` (zinc-700) |
| Text | `#F1F1F0` |

**Light mode colors:**

| Element | Color |
|---------|-------|
| Background | `#FFFFFF` |
| Bot bubbles | `#F5F5F5` |
| Borders | `#E4E4E7` (zinc-200) |
| Text | `#14161A` |

---

## RTL / Arabic Support

Set `locale = "ar"` — the entire UI adapts automatically:

- All text right-aligned
- Message bubbles mirrored (user left, bot right)
- Navigation arrows flipped
- Arabic translations for all built-in strings (form labels, buttons, errors, placeholders)
- Per-message language detection for mixed-language chats
- **No global mutation** — the SDK does NOT call `I18nManager` or affect your host app's layout direction

---

## Headless Mode — Full Guide

### All State (StateFlow)

| State | Type | Description |
|-------|------|-------------|
| `messages` | `StateFlow<List<TypeMessage>>` | Full message history |
| `currentMessage` | `StateFlow<String>` | Streaming text (grows as chunks arrive) |
| `connectionStatus` | `StateFlow<ConnectionStatus>` | `CONNECTING`, `CONNECTED`, `DISCONNECTED`, `RECONNECTING` |
| `isConnected` | `StateFlow<Boolean>` | WebSocket is open |
| `isOnline` | `StateFlow<Boolean>` | Device has network connectivity |
| `loading` | `StateFlow<Boolean>` | Bot is processing (STEP received) |
| `status` | `StateFlow<String?>` | "Searching...", "Thinking..." |
| `info` | `StateFlow<String?>` | "Waiting for human agent..." |
| `commandData` | `StateFlow<CommandData?>` | Active server command (rate, feedback, etc.) |
| `presets` | `StateFlow<List<String>>` | Suggestion buttons from server |
| `showHumanAgentBtn` | `StateFlow<Boolean>` | Escalation available |
| `sessionCloseInfo` | `StateFlow<SessionCloseInfo?>` | Session closed by server (auto_close) |
| `currentSessionId` | `StateFlow<String>` | Current WebSocket session ID |

### All Actions

| Action | Signature | Description |
|--------|-----------|-------------|
| `connect` | `suspend fun connect(userInfo, botConfig?)` | Start WebSocket connection |
| `sendMessage` | `fun sendMessage(text)` | Send text message |
| `sendImages` | `fun sendImages(images, caption)` | Send images with optional caption |
| `sendAudio` | `fun sendAudio(base64)` | Send voice note |
| `sendPreset` | `fun sendPreset(text)` | Send preset as message + clear presets |
| `sendCommandResponse` | `fun sendCommandResponse(cmd, response)` | Respond to server commands |
| `clearSessionCloseInfo` | `fun clearSessionCloseInfo()` | Reset after auto_close warning |
| `endSession` | `suspend fun endSession()` | End session, clear storage, reset state |
| `destroy` | `fun destroy()` | Release all resources (Activity onDestroy) |
| `fetchBotConfig` | `suspend fun fetchBotConfig()` | Fetch bot configuration from API |

### Lifecycle: endSession vs destroy

| Method | Clears storage? | Closes WebSocket? | Resets UI? | When to call |
|--------|----------------|-------------------|-----------|-------------|
| `endSession()` | Yes | Yes | Yes → Intro | User taps "End Session" |
| `destroy()` | No | Yes | N/A | Activity/Fragment `onDestroy` |

---

## Session Persistence

| Data | Storage | Survives |
|------|---------|----------|
| Session ID | EncryptedSharedPreferences (AES-256) | App restart, process death |
| Messages (up to 500) | Room Database (SQLite) | App restart, process death |
| User info | EncryptedSharedPreferences | App restart, process death |

On reconnection, the SDK reads the stored session ID and sends it in the WebSocket URL. The server resumes the same conversation — no messages lost.

> **Encryption fallback:** If `EncryptedSharedPreferences` fails on older/rooted devices, the SDK falls back to regular `SharedPreferences` with a logged warning. It never crashes.

---

## Background / Foreground Behavior

| Duration in background | What happens |
|-----------------------|-------------|
| < 60 seconds | WebSocket likely survives. Returns seamlessly. |
| 1-5 minutes | WebSocket may die. Auto-reconnects on return. |
| 5+ minutes | WebSocket dead. Stale streaming state cleared. Auto-reconnects with saved session ID. |
| Process killed by OS | Everything in memory lost. Room + SharedPrefs survive. User taps "Start Chat" → messages restored. |

---

## Keeping Chat Alive Across Tabs

If your app has tab navigation, keep the chat widget mounted — hide it visually instead of unmounting:

```kotlin
// ✅ Correct — stays mounted, preserves chat state
Box(modifier = Modifier.then(
    if (activeTab == "support") Modifier else Modifier.size(0.dp)
)) {
    RayaChatWidget(token = "...")
}

// ❌ Wrong — unmounts on tab switch, resets to intro
if (activeTab == "support") {
    RayaChatWidget(token = "...")
}
```

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
│       ├── RayaChatViewModel.kt       # State machine (INTRO→FORM→CHAT)
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

### Peer Dependencies (auto-included)

| Library | Purpose |
|---------|---------|
| OkHttp | WebSocket + HTTP |
| Room | Message persistence |
| Kotlinx Serialization | JSON parsing |
| Jetpack Compose | UI (raya-chat-ui only) |
| Coil | Image loading (raya-chat-ui only) |
| Markwon | Markdown rendering (raya-chat-ui only) |

---

## Sample App

The `sample/` module demonstrates all 4 integration modes with a working bot token:

```bash
./gradlew :sample:installDebug
```

| Demo | Mode | What it shows |
|------|------|--------------|
| Compose Widget | Mode 1 | Full chat widget + image picker adapter |
| Fragment | Mode 2 | Chat in XML FrameLayout |
| BottomSheet | Mode 3 | Chat slides up as overlay |
| Headless | Mode 4 | Fully custom UI (Nocturne Velvet design) with all features |

---

## Troubleshooting

### WebSocket not connecting
1. Check that `token` is correct (from Teammates.ai dashboard)
2. Ensure the device has network access
3. Check Logcat: `tag:RayaChat.WS` for connection logs
4. Check Logcat: `tag:RayaChat.API` for bot config fetch

### Images not uploading
Ensure you've provided an `imagePickerAdapter`. Without it, the paperclip button is hidden. The adapter must be created **before** `super.onCreate()` (ActivityResultLauncher registration requirement).

### Chat resets to intro screen when switching tabs
The SDK cleans up when unmounted. Keep `<RayaChatWidget>` mounted — hide it visually instead of removing it. See [Keeping Chat Alive Across Tabs](#keeping-chat-alive-across-tabs).

### BottomSheet crashes on ComponentActivity
`RayaChatBottomSheet` requires `AppCompatActivity` (or `FragmentActivity`), not `ComponentActivity`. Change your Activity to extend `AppCompatActivity`.

### Bot config fetch fails
Check Logcat `tag:RayaChat.API`. Common causes:
- Invalid token
- No network
- Server returns null fields (handled gracefully — uses defaults)

### Keyboard covers the chat input
The SDK uses `imePadding()` on the chat screen. If your host app has its own `KeyboardAvoidingView` or `imePadding`, they may conflict — remove the outer one.

### App crashes on rotation
Fixed in v0.1.0. The SDK uses `ViewModelProvider` so the ViewModel survives configuration changes. If you see this on an older build, update to latest.

---

## License

MIT
