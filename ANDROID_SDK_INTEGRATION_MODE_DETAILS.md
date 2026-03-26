# 4 Integration Modes — Deep Explanation

---

## Mode 1: Packaged UI — Jetpack Compose

```kotlin
// 3 lines — full chat widget
RayaChatWidget(
    token = "your-bot-token",
    locale = "en"
)
```

**Technology:** Jetpack Compose (Android's modern declarative UI toolkit)

**What the developer gets:** Complete chat widget — intro screen, form, chat, commands, image upload, typing indicators — identical to what `<RayaChat>` provides in the RN SDK. Zero custom UI code.

**Why Compose:**
- Google's recommended UI framework since 2021 — all new Android apps use it
- Declarative paradigm matches React's mental model (state → UI)
- Our theme system (light/dark, gradient colors, RTL) maps naturally to Compose's MaterialTheme + CompositionLocal
- Compose previews let developers see the widget in Android Studio without running the app
- 60%+ of new Android projects use Compose as of 2026

**Who uses this:** Any modern Android app built with Compose. Most common integration path — just drop in the Composable.

**Under the hood:**
```
RayaChatWidget (Composable)
  └── RayaChatTheme (CompositionLocal — colors, typography, RTL)
       └── RayaChatViewModel (manages ChatSession + BotConfig)
            ├── IntroScreen (Composable)
            ├── FormScreen (Composable)
            └── ChatScreen (Composable)
                 ├── MessageList (LazyColumn)
                 ├── MessageComposer (TextField + action buttons)
                 └── CommandUI (Rating, Feedback, Countdown)
```

---

## Mode 2: Packaged UI — Fragment (XML Apps)

```kotlin
// For apps using XML layouts + FragmentManager
supportFragmentManager.beginTransaction()
    .replace(R.id.container, RayaChatFragment.newInstance("your-bot-token"))
    .commit()
```

**Technology:** Android Fragment + ComposeView bridge

**What the developer gets:** Same complete chat widget, but wrapped in a Fragment that can be placed inside any XML layout's FrameLayout or FragmentContainerView.

**Why Fragment:**
- Millions of production apps still use XML layouts + Fragments (not Compose)
- Enterprise apps, banking apps, government apps — they can't rewrite their UI to Compose just to add a chat widget
- Fragment is the universal container that works in ALL Android apps regardless of UI toolkit
- The Fragment internally hosts a ComposeView that renders the Compose UI — developers don't know or care that Compose is inside

**Who uses this:** Apps built with XML layouts, Java-only apps, legacy codebases, apps using Navigation Component with Fragments.

**Under the hood:**
```
RayaChatFragment (Fragment)
  └── ComposeView (bridge — renders Compose inside Fragment)
       └── RayaChatWidget (same Composable as Mode 1)
```

The Fragment is a thin wrapper — maybe 30 lines of code. All the real work happens in Compose. But it makes the SDK accessible to 100% of Android apps, not just Compose apps.

**XML layout usage:**
```xml
<!-- activity_main.xml -->
<FrameLayout
    android:id="@+id/chat_container"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

---

## Mode 3: Packaged UI — BottomSheet

```kotlin
// One-liner — presents as a draggable bottom sheet
RayaChatBottomSheet.show(
    fragmentManager = supportFragmentManager,
    token = "your-bot-token"
)
```

**Technology:** BottomSheetDialogFragment (Material Design component)

**What the developer gets:** The chat widget slides up from the bottom of the screen as a draggable sheet — same as how Google Maps shows location details, or how Uber shows ride options. User can swipe down to dismiss.

**Why BottomSheet:**
- Most natural mobile pattern for "secondary content" like chat support
- Matches the RN SDK's `<RayaChatModal>` but with Android-native behavior (drag to dismiss, peek/expand states)
- Users don't leave their current screen — the sheet overlays on top
- Material Design standard — users already understand the interaction pattern
- Works with both Compose and XML apps (it's a DialogFragment)

**Who uses this:** Apps that want chat as an overlay (not a full screen). E-commerce apps, delivery apps, fintech apps — "tap Help → sheet slides up → chat → swipe down → back to app."

**Under the hood:**
```
RayaChatBottomSheet (BottomSheetDialogFragment)
  ├── Initial state: EXPANDED (full height)
  ├── Drag behavior: EXPANDED ↔ COLLAPSED ↔ HIDDEN
  └── Content:
       └── ComposeView
            └── RayaChatWidget (same Composable)
```

**Sheet states:**
- `HIDDEN` → user dismissed (swipe down or close button)
- `COLLAPSED` → shows header only (peek)
- `EXPANDED` → full chat visible (default)

---

## Mode 4: Headless (Custom UI)

```kotlin
val client = RayaChatClient(
    context = applicationContext,
    config = RayaChatConfig(token = "your-bot-token")
)
client.connect(userInfo = UserInfo("John", "john@test.com", ""), botConfig = botConfig)

// Observe state via Kotlin Flow
lifecycleScope.launch {
    client.messages.collect { messages -> /* render your own UI */ }
}

// Actions
client.sendMessage("Hello")
client.sendImages(images)
client.endSession()
```

**Technology:** Kotlin Coroutines + StateFlow (reactive state streams)

**What the developer gets:** All chat logic (WebSocket, heartbeat, reconnection, message parsing, persistence, commands) with zero UI. They build every pixel themselves. Same as `useRayaChat()` in the RN SDK.

**Why Kotlin Flow (not LiveData, not RxJava):**

| Option | Why NOT | Why Flow WINS |
|--------|---------|---------------|
| LiveData | Android-only, no backpressure, limited operators | Flow works with any coroutine scope |
| RxJava | Heavy dependency (2MB+), complex API, Java-centric | Flow is Kotlin-native, lightweight, built-in |
| Callback interface | No composition, no lifecycle awareness | Flow auto-cancels with coroutine scope |
| StateFlow specifically | — | Hot stream, always has a value, thread-safe, lifecycle-aware with `collectAsState()` in Compose |

StateFlow is the Kotlin equivalent of React's `useState` — it holds the current value and emits to all collectors when it changes. Perfect match for our state model:

```kotlin
class RayaChatClient(context: Context, config: RayaChatConfig) {
    // State (StateFlow — always has current value)
    val messages: StateFlow<List<TypeMessage>>
    val currentMessage: StateFlow<String>
    val connectionStatus: StateFlow<ConnectionStatus>
    val isConnected: StateFlow<Boolean>
    val isOnline: StateFlow<Boolean>              // device network status
    val loading: StateFlow<Boolean>
    val status: StateFlow<String?>
    val info: StateFlow<String?>
    val commandData: StateFlow<CommandData?>
    val presets: StateFlow<List<String>>
    val showHumanAgentBtn: StateFlow<Boolean>
    val sessionCloseInfo: StateFlow<SessionCloseInfo?>

    // Actions (suspend functions for async, regular for sync)
    suspend fun connect(userInfo: UserInfo, botConfig: BotConfigProps? = null)
    fun sendMessage(text: String)
    fun sendImages(images: List<ImagePayload>, caption: String = "")
    fun sendAudio(base64: String)
    fun sendPreset(text: String)
    fun sendCommandResponse(command: String, response: Any)
    fun clearSessionCloseInfo()                   // reset after auto_close
    suspend fun endSession()
    fun destroy()                                 // cleanup on Activity/Fragment destroy
}
```

**Why `connect()` takes `botConfig`:** The initial bot message (`chatbox_initial_msg`) is injected locally — the server doesn't send it via WebSocket. You must pass `botConfig` (fetched via `fetchBotConfig()` or the `useBotConfig` equivalent) to `connect()` for the welcome message to appear.

**Why `destroy()` exists:** Different from `endSession()`. `endSession()` is a user action that clears storage and resets to intro. `destroy()` is a lifecycle cleanup that releases WebSocket, coroutines, and observers when the host Activity/Fragment is destroyed — without clearing persisted data.

**Who uses this:** Apps with custom design systems, apps embedding chat inside existing screens, apps that need the chat data for analytics/logging, apps building completely different UIs (like a chat bubble floating on every screen).

**Compose usage (headless + custom UI):**
```kotlin
@Composable
fun MyCustomChat(client: RayaChatClient) {
    val messages by client.messages.collectAsState()
    val loading by client.loading.collectAsState()

    // Your own LazyColumn, your own bubbles, your own theme
    LazyColumn {
        items(messages) { msg ->
            MyCustomBubble(msg)
        }
    }
}
```

**XML/Java usage (headless):**
```kotlin
// In Activity or ViewModel
lifecycleScope.launch {
    client.messages.collect { messages ->
        recyclerViewAdapter.submitList(messages)
    }
}
```

---

## Why All 4 Modes Share One Core

```
┌─────────────────────────────────────────────────┐
│                  raya-chat-core                  │
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
│  → Kotlin Coroutines + StateFlow                 │
│  → OkHttp WebSocket                              │
│  → Zero UI dependencies                          │
└──────────────┬──────────────┬────────────────────┘
               │              │
    ┌──────────▼──────┐  ┌───▼──────────────────┐
    │  raya-chat-ui   │  │  Developer's own UI   │
    │                 │  │                       │
    │  Mode 1: Widget │  │  Mode 4: Headless     │
    │  Mode 2: Fragment│  │  (Compose or XML)     │
    │  Mode 3: Sheet  │  │                       │
    │                 │  │  client.messages       │
    │  Jetpack Compose│  │  client.sendMessage()  │
    └─────────────────┘  └───────────────────────┘
```

One WebSocket implementation. One message parser. One reconnection strategy. One persistence layer. Four ways to present it. Same as how the RN SDK has one `useChatSession` hook powering both `<RayaChat>` and `useRayaChat()`.

---

## Adapter Interfaces

The SDK uses pluggable adapters for native device features. Buttons are **hidden** (not disabled) when no adapter is provided.

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
    suspend fun stopRecording(): AudioResult  // { uri: String, base64: String? }
    suspend fun pauseRecording()
    suspend fun resumeRecording()
    suspend fun getAmplitude(): Float          // 0f..1f for waveform
    suspend fun cleanup()
}
```

### AudioPlayerAdapter
```kotlin
interface AudioPlayerAdapter {
    suspend fun loadAudio(uri: String): AudioInfo  // { durationMs: Long }
    suspend fun play()
    suspend fun pause()
    suspend fun seekTo(positionMs: Long)
    suspend fun getPosition(): Long
    suspend fun cleanup()
}
```

**Button visibility based on adapters provided:**

| Adapters provided | Buttons shown |
|-------------------|---------------|
| None | Emoji + Send only |
| `imagePickerAdapter` only | Emoji + Paperclip + Send |
| `audioRecorderAdapter` only | Emoji + Mic + Send |
| Both adapters | Emoji + Paperclip + Mic + Send |
