# Raya Chat Android SDK

Native Android SDK for embedding the Raya AI chat widget in Android apps. Built with Kotlin + Jetpack Compose.

Works with **Jetpack Compose**, **XML layout + Fragment**, **BottomSheet**, and **headless (custom UI)** apps.

## Installation

### Packaged UI (includes all screens)

```gradle
dependencies {
    implementation "ai.teammates:raya-chat-ui:0.1.0"
}
```

### Headless only (no UI, just the chat engine)

```gradle
dependencies {
    implementation "ai.teammates:raya-chat-core:0.1.0"
}
```

---

## Quick Start

### Mode 1: Compose Widget (3 lines)

```kotlin
import ai.teammates.rayachat.ui.RayaChatWidget

@Composable
fun SupportScreen() {
    RayaChatWidget(token = "your-bot-token")
}
```

### Mode 2: Fragment (XML layout apps)

```kotlin
import ai.teammates.rayachat.ui.RayaChatFragment

// In your Activity
supportFragmentManager.beginTransaction()
    .replace(R.id.container, RayaChatFragment.newInstance("your-bot-token"))
    .commit()
```

### Mode 3: BottomSheet (overlay)

```kotlin
import ai.teammates.rayachat.ui.RayaChatBottomSheet

// One-liner — slides up from bottom
RayaChatBottomSheet.show(supportFragmentManager, "your-bot-token")
```

### Mode 4: Headless (custom UI)

```kotlin
import ai.teammates.rayachat.core.RayaChatClient
import ai.teammates.rayachat.core.RayaChatConfig

val client = RayaChatClient(
    context = applicationContext,
    config = RayaChatConfig(token = "your-bot-token")
)

// Connect
val botConfig = client.fetchBotConfig()
client.connect(UserInfo("John", "john@test.com", ""), botConfig)

// Observe state via Kotlin Flow
lifecycleScope.launch {
    client.messages.collect { messages -> /* render your own UI */ }
}

// Actions
client.sendMessage("Hello")
client.endSession()
```

---

## Configuration

### RayaChatWidget / RayaChatFragment / RayaChatBottomSheet

| Param | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `token` | String | Yes | — | Bot token from Teammates.ai dashboard |
| `locale` | String | No | `"en"` | `"en"` or `"ar"` (Arabic/RTL) |
| `imagePickerAdapter` | ImagePickerAdapter | No | — | Adapter for image selection |
| `audioRecorderAdapter` | AudioRecorderAdapter | No | — | Adapter for voice recording |
| `onSessionStart` | (String) -> Unit | No | — | Called with session ID on connect |
| `onSessionEnd` | () -> Unit | No | — | Called when session ends |
| `onError` | (String) -> Unit | No | — | Called on errors |
| `onClose` | () -> Unit | No | — | Called when user closes the widget |

---

## Adapters

Image upload and mic buttons are **hidden** when no adapter is provided.

### ImagePickerAdapter

```kotlin
interface ImagePickerAdapter {
    suspend fun pickImages(maxCount: Int): List<ImageAsset>
}
```

### AudioRecorderAdapter

```kotlin
interface AudioRecorderAdapter {
    suspend fun startRecording()
    suspend fun stopRecording(): AudioResult
    suspend fun pauseRecording()
    suspend fun resumeRecording()
    suspend fun getAmplitude(): Float  // 0..1
    suspend fun cleanup()
}
```

---

## Features

- Text chat via WebSocket (send/receive, streaming chunks)
- Markdown rendering in bot messages (Markwon)
- Image uploads (up to 5, base64)
- Voice note recording + playback (adapter pattern)
- Preset/suggestion buttons
- Command system (end_session, rate_conversation, submit_feedback, auto_close)
- Agent activity notifications
- Escalation to human agent
- Session persistence (Room + EncryptedSharedPreferences)
- Reconnection with exponential backoff (max 100 attempts, 30s cap)
- Heartbeat ping/pong (25s/60s)
- Light/dark theme (from bot config)
- RTL support (Arabic)
- Keyboard avoidance (imePadding)
- App background/foreground awareness

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
│  Room Database (messages)                        │
│  EncryptedSharedPreferences (session + user)     │
│                                                  │
│  Kotlin Coroutines + StateFlow                   │
│  OkHttp WebSocket                                │
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

---

## Requirements

| Requirement | Version |
|-------------|---------|
| Kotlin | 2.1+ |
| Android SDK | API 24+ (Android 7.0) |
| compileSdk | 35 |
| Java | 17 |

---

## Sample App

The `sample/` module demonstrates all 4 integration modes:

```bash
./gradlew :sample:installDebug
```

---

## License

MIT
