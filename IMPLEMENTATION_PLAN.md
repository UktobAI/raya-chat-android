# Raya Chat Android SDK — Final Implementation Plan

Build a native Android SDK (`raya-chat-android`) in Kotlin + Jetpack Compose that achieves 100% feature parity with the React Native SDK (`@teammates-ai/raya-chat-react-native` v0.1.0).

Reference: [NATIVE_SDK_SPEC.md](./NATIVE_SDK_SPEC.md) defines all protocol details, message formats, and UI behavior.

---

## Artifacts

Two Maven Central artifacts:

| Artifact | Purpose | Size | Who installs |
|----------|---------|------|-------------|
| `ai.teammates:raya-chat-core` | Headless engine — WebSocket, API, models, storage | ~500KB | Mode 4 (Headless) developers |
| `ai.teammates:raya-chat-ui` | Packaged UI — Compose screens + Fragment/BottomSheet wrappers | ~1.5MB (includes core) | Mode 1, 2, 3 developers |

## Integration Modes

| Mode | Entry Point | Technology | Target Apps |
|------|-------------|-----------|-------------|
| 1. Compose Widget | `RayaChatWidget(token = "...")` | Jetpack Compose | Modern Kotlin apps |
| 2. Fragment | `RayaChatFragment.newInstance("...")` | Fragment + ComposeView bridge | XML layout apps (Kotlin + Java) |
| 3. BottomSheet | `RayaChatBottomSheet.show(...)` | BottomSheetDialogFragment | Any app wanting chat as overlay |
| 4. Headless | `RayaChatClient(context, config)` + StateFlow | Kotlin Coroutines | Custom UI / any architecture |

---

## Storage Architecture

Two separate storage mechanisms for two different data types:

| Data | Storage | Why |
|------|---------|-----|
| Session ID | EncryptedSharedPreferences | Sensitive, tiny, key-value. AES-256 encrypted. |
| User info (name, email, phone) | EncryptedSharedPreferences | Sensitive, tiny, key-value. AES-256 encrypted. |
| Messages (up to 500) | Room Database | Structured, large, needs O(1) insert + SQL trim + reactive Flow |

### Why Room for Messages (not SharedPreferences)

The RN SDK stores messages as one giant JSON string in AsyncStorage. Every message add requires: read all → parse → append → stringify → write all. With 500 messages this is O(n) on every operation.

Room solves this:
- **Insert**: `INSERT INTO messages VALUES (...)` — O(1), adds one row
- **Trim**: `DELETE WHERE id NOT IN (SELECT id ORDER BY created_at DESC LIMIT 500)` — SQL handles it
- **Read**: `SELECT * FROM messages ORDER BY created_at ASC` — returns `Flow<List<TypeMessage>>` (reactive, auto-updates UI)
- **Clear**: `DELETE FROM messages` — instant

### EncryptedSharedPreferences Fallback

Some older/rooted devices throw exceptions with EncryptedSharedPreferences. The implementation MUST fall back to regular SharedPreferences if encryption fails, with a logged warning. Never crash.

---

## Phase 1: Project Scaffolding + Models + Constants

**Days 1-2 · Goal: Buildable multi-module Gradle project with all data models and utilities.**

### Root Files

```
raya-chat-android/
├── settings.gradle.kts              # Module declarations (core, ui, sample)
├── build.gradle.kts                 # Common config (Kotlin, Android versions)
├── gradle.properties                # Maven publish config, Android settings
├── gradle/
│   └── libs.versions.toml           # Version catalog (single source for ALL deps)
├── .gitignore
├── .editorconfig
├── README.md
├── NATIVE_SDK_SPEC.md               # Already exists
├── IMPLEMENTATION_PLAN.md           # This file
└── LICENSE
```

### gradle/libs.versions.toml — Complete Dependency Catalog

```toml
[versions]
kotlin = "2.1.20"
agp = "8.7.0"
compose-bom = "2024.12.01"
compose-compiler = "1.5.15"
okhttp = "4.12.0"
room = "2.6.1"
coroutines = "1.8.0"
coil = "2.7.0"
markwon = "4.6.2"
serialization = "1.7.0"
lifecycle = "2.8.0"
security-crypto = "1.1.0-alpha06"

# Testing
junit = "4.13.2"
mockk = "1.13.10"
turbine = "1.1.0"
truth = "1.4.2"
mockwebserver = "4.12.0"

[libraries]
# Core
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
lifecycle-process = { module = "androidx.lifecycle:lifecycle-process", version.ref = "lifecycle" }
security-crypto = { module = "androidx.security:security-crypto", version.ref = "security-crypto" }

# UI
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-foundation = { module = "androidx.compose.foundation:foundation" }
compose-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-tooling = { module = "androidx.compose.ui:ui-tooling" }
coil-compose = { module = "io.coil-kt:coil-compose", version.ref = "coil" }
markwon-core = { module = "io.noties.markwon:core", version.ref = "markwon" }

# Testing
junit = { module = "junit:junit", version.ref = "junit" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
truth = { module = "com.google.truth:truth", version.ref = "truth" }
coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
mockwebserver = { module = "com.squareup.okhttp3:mockwebserver", version.ref = "mockwebserver" }
compose-test = { module = "androidx.compose.ui:ui-test-junit4" }
```

### Core Module Files

```
raya-chat-core/
├── build.gradle.kts
├── consumer-rules.pro               # ProGuard rules shipped in AAR
├── src/main/
│   ├── AndroidManifest.xml           # <uses-permission android:name="android.permission.INTERNET" />
│   └── kotlin/ai/teammates/rayachat/core/
│       ├── RayaChatConfig.kt         # PUBLIC — config data class (token, locale, callbacks)
│       ├── Constants.kt              # SDK_VERSION, DEFAULT_ENDPOINT, timeouts, limits
│       │
│       ├── models/
│       │   ├── TypeMessage.kt        # Message model (Room @Entity)
│       │   ├── ChatMessage.kt        # Server response message
│       │   ├── Attachment.kt         # Image/file attachment
│       │   ├── AudioData.kt          # Audio message data
│       │   ├── ResponseData.kt       # RESPONSE payload
│       │   ├── BotConfigProps.kt     # Bot configuration from API
│       │   ├── CommandData.kt        # Server command
│       │   ├── SessionCloseInfo.kt   # Auto-close info
│       │   ├── UserInfo.kt           # User form data
│       │   ├── DeviceMetadata.kt     # Device info for WS URL
│       │   ├── ImageAsset.kt         # Image for upload
│       │   ├── WebSocketIncomingMessage.kt  # Raw parsed message
│       │   └── Enums.kt             # MessageType, ConnectionStatus, ViewMode
│       │
│       └── util/
│           ├── ColorUtils.kt         # isDarkColor, getContrastColor
│           ├── ErrorSanitizer.kt     # XSS strip, sensitive pattern replacement
│           ├── Validation.kt         # validateEmail, validatePhone
│           ├── TimeFormat.kt         # formatLocalTime
│           └── TextDirection.kt      # isRTLLocale, isRTLText
│
└── src/test/kotlin/ai/teammates/rayachat/core/
    ├── models/
    │   └── SerializationTest.kt      # All models serialize/deserialize correctly
    └── util/
        ├── ColorUtilsTest.kt
        ├── ErrorSanitizerTest.kt
        ├── ValidationTest.kt
        ├── TimeFormatTest.kt
        └── TextDirectionTest.kt
```

### Key Design Decisions — Phase 1

**RayaChatConfig is a top-level class, not inside models/:**
It's the first thing developers interact with. Placing it at `core/RayaChatConfig.kt` makes imports clean:
```kotlin
import ai.teammates.rayachat.core.RayaChatConfig
```

**TypeMessage is both a data class AND a Room @Entity:**
Annotated with `@Entity(tableName = "messages")` so Room can store it directly. Complex fields (attachments, audio) use `@TypeConverters` to serialize to JSON strings within the table.

**Kotlinx Serialization, not Gson:**
Type-safe, no reflection (faster startup on Android), compile-time verification.

### Verification

- `./gradlew :raya-chat-core:build` passes
- `./gradlew :raya-chat-core:test` passes (all util tests)
- All 13 model classes serialize/deserialize round-trip

---

## Phase 2: Core Protocol Layer

**Days 3-6 · Goal: Complete headless SDK — `RayaChatClient` with StateFlow state + actions. Mode 4 (Headless) works end-to-end.**

### Files

```
raya-chat-core/src/main/kotlin/ai/teammates/rayachat/core/
├── RayaChatClient.kt                 # PUBLIC API — main entry point
│
├── websocket/
│   ├── WebSocketManager.kt           # OkHttp WebSocket lifecycle
│   └── MessageQueue.kt              # Queue messages during CONNECTING
│
├── api/
│   └── ApiClient.kt                 # Bot config fetch + WS URL construction
│
├── protocol/
│   ├── MessageHandler.kt            # Route all 10 inbound message types
│   └── MessageHandlerCallbacks.kt   # Callback interface
│
├── storage/
│   ├── PreferenceStorage.kt         # EncryptedSharedPreferences (session + user)
│   ├── ChatDatabase.kt              # Room database definition
│   ├── MessageDao.kt                # Room DAO (insert, getAll, trim, deleteAll)
│   ├── MessageEntity.kt             # Room entity mapping (if different from TypeMessage)
│   └── Converters.kt                # Room TypeConverters (List<Attachment> → JSON)
│
├── lifecycle/
│   └── AppLifecycleObserver.kt      # ProcessLifecycleOwner
│
└── network/
    └── NetworkMonitor.kt            # ConnectivityManager → StateFlow<Boolean>
```

### RayaChatClient — Public API

```kotlin
class RayaChatClient(
    context: Context,
    config: RayaChatConfig
) {
    // ── State (StateFlow — always has current value, thread-safe) ──
    val messages: StateFlow<List<TypeMessage>>
    val currentMessage: StateFlow<String>           // streaming text
    val connectionStatus: StateFlow<ConnectionStatus>
    val isConnected: StateFlow<Boolean>
    val isOnline: StateFlow<Boolean>                // device network status
    val loading: StateFlow<Boolean>
    val status: StateFlow<String?>                  // "Searching...", "Thinking..."
    val info: StateFlow<String?>                    // "Waiting for human agent..."
    val commandData: StateFlow<CommandData?>
    val presets: StateFlow<List<String>>
    val showHumanAgentBtn: StateFlow<Boolean>
    val sessionCloseInfo: StateFlow<SessionCloseInfo?>

    // ── Actions ──
    suspend fun connect(userInfo: UserInfo, botConfig: BotConfigProps? = null)
    fun sendMessage(text: String)
    fun sendImages(images: List<ImagePayload>, caption: String = "")
    fun sendAudio(base64: String)
    fun sendPreset(text: String)
    fun sendCommandResponse(command: String, response: Any)
    fun clearSessionCloseInfo()                      // reset after auto_close
    suspend fun endSession()
    fun destroy()                                    // cleanup on Activity/Fragment destroy
}
```

**No Builder pattern.** Single constructor with `RayaChatConfig` data class. Simple, matches RN SDK's simplicity:
```kotlin
val client = RayaChatClient(
    context = applicationContext,
    config = RayaChatConfig(
        token = "your-bot-token",
        locale = "en",
        onSessionStart = { id -> Log.d("Chat", "Session: $id") }
    )
)
```

### WebSocketManager — OkHttp Implementation

Port of the RN SDK's `websocket-manager.ts` with Android-specific handling:

| Feature | RN SDK | Android SDK |
|---------|--------|-------------|
| WebSocket | `new WebSocket(url)` | `OkHttpClient.newWebSocket(request, listener)` |
| Heartbeat timer | `setInterval` / `setTimeout` | `CoroutineScope.launch { delay(25_000) }` |
| Thread safety | Single JS thread | `Dispatchers.Main` for state, OkHttp thread for WS callbacks |
| Message queue | Array in memory | `MessageQueue` class with `ConcurrentLinkedQueue` |
| Destroyed flag | `this.destroyed = false` | `AtomicBoolean` |
| Pong filter | `if (data === 'pong')` | Same check in `onMessage` callback |

**Critical:** OkHttp WebSocket callbacks (`onMessage`, `onClose`, `onFailure`) come on OkHttp's dispatcher thread. All state updates MUST be marshaled to main thread via `withContext(Dispatchers.Main)` or `MutableStateFlow` (which is thread-safe).

### Storage — Room + EncryptedSharedPreferences

```kotlin
// ChatDatabase.kt
@Database(entities = [MessageEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}

// MessageDao.kt
@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY created_at_millis ASC")
    fun observeAll(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE id NOT IN (SELECT id FROM messages ORDER BY created_at_millis DESC LIMIT 500)")
    suspend fun trimToLatest500()

    @Query("DELETE FROM messages")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun count(): Int
}

// PreferenceStorage.kt
class PreferenceStorage(context: Context) {
    private val prefs: SharedPreferences = try {
        EncryptedSharedPreferences.create(
            context, "raya_chat_prefs", masterKey,
            PrefKeyEncryptionScheme.AES256_SIV,
            PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback to regular SharedPreferences on older/rooted devices
        Log.w("RayaChat", "EncryptedSharedPreferences failed, using unencrypted", e)
        context.getSharedPreferences("raya_chat_prefs", Context.MODE_PRIVATE)
    }

    fun getSessionId(): String = prefs.getString("session_id", "") ?: ""
    fun setSessionId(id: String) = prefs.edit().putString("session_id", id).apply()
    fun getUserInfo(): UserInfo? = /* deserialize from JSON */
    fun setUserInfo(user: UserInfo) = /* serialize to JSON */
    fun clearAll() = prefs.edit().clear().apply()
}
```

### Android-Specific Concerns

1. **URL encoding:** `data` query param MUST use `URLEncoder.encode(json, "UTF-8")`. OkHttp crashes with raw JSON `{}` in query params (Android issue we already fixed in RN SDK).

2. **EncryptedSharedPreferences fallback:** `try/catch` with fallback to regular SharedPreferences. Never crash.

3. **Heartbeat uses coroutine delay:**
   ```kotlin
   private fun startHeartbeat() {
       heartbeatJob = scope.launch {
           while (isActive) {
               delay(HEARTBEAT_INTERVAL)
               if (isConnected) send("ping")
           }
       }
   }
   ```

4. **OkHttp dispatcher → Main thread:** WebSocket callbacks come on OkHttp's thread pool. Marshal to main:
   ```kotlin
   override fun onMessage(webSocket: WebSocket, text: String) {
       scope.launch(Dispatchers.Main) {
           handleMessage(text)  // updates StateFlow on main thread
       }
   }
   ```

5. **Room runs on IO thread:** All DAO operations are `suspend` functions called from `Dispatchers.IO`.

### Tests

```
src/test/kotlin/ai/teammates/rayachat/core/
├── websocket/
│   ├── WebSocketManagerTest.kt       # MockWebServer: connect, heartbeat ping/pong,
│   │                                 #   reconnect on close, message queue flush,
│   │                                 #   destroyed flag prevents callbacks
│   └── MessageQueueTest.kt          # Queue, flush, flush-on-broken-connection
│
├── api/
│   ├── ApiClientTest.kt             # MockWebServer: bot config fetch, defaults on failure
│   └── UrlBuilderTest.kt            # URL encoding (special chars, Arabic, empty fields)
│
├── protocol/
│   └── MessageHandlerTest.kt        # All 10 message types with JSON fixtures
│
├── storage/
│   ├── PreferenceStorageTest.kt     # In-memory SharedPreferences mock
│   ├── MessageDaoTest.kt            # Room in-memory DB (instrumented)
│   └── ConvertersTest.kt           # TypeConverter round-trips
│
├── lifecycle/
│   └── AppLifecycleObserverTest.kt  # Foreground/background transitions
│
├── network/
│   └── NetworkMonitorTest.kt        # Online/offline state changes
│
└── integration/
    └── ChatSessionIntegrationTest.kt # connect → send → STEP+CHUNK+RESPONSE → verify state
```

### Verification

- `./gradlew :raya-chat-core:build` passes
- `./gradlew :raya-chat-core:test` passes (all unit tests)
- Headless demo works: connect → send "Hello" → receive response → end session
- MockWebServer integration test passes full flow

---

## Phase 3: Theme + Compose Components

**Days 7-9 · Goal: All reusable UI building blocks — no screen orchestration yet.**

### Files

```
raya-chat-ui/
├── build.gradle.kts
├── consumer-rules.pro               # ProGuard rules for Compose + Markwon
├── src/main/
│   ├── AndroidManifest.xml
│   └── kotlin/ai/teammates/rayachat/ui/
│       │
│       ├── theme/
│       │   ├── ColorTokens.kt        # Light/dark color tokens (exact hex from RN SDK)
│       │   ├── RayaTheme.kt          # Theme data class (colors, typography, isRTL, gradientColor, etc.)
│       │   ├── RayaChatTheme.kt      # CompositionLocalProvider wrapper
│       │   ├── Typography.kt         # TextStyle tokens (heading, body, caption, small)
│       │   └── RtlUtils.kt           # Compose LayoutDirection utilities
│       │
│       ├── components/
│       │   ├── chat/
│       │   │   ├── MessageBubble.kt       # User (gradient, right) + bot (gray, left + avatar) + system
│       │   │   ├── MessageList.kt         # LazyColumn, auto-scroll, streaming msg, footer content
│       │   │   ├── MessageComposer.kt     # TextField + emoji/image/mic/send, disabled during commands
│       │   │   ├── TypingIndicator.kt     # 3 animated dots in bot bubble (Compose animation)
│       │   │   ├── PresetButtons.kt       # FlowRow pills, right-aligned, wrapping
│       │   │   ├── ScrollToBottomButton.kt # Animated FAB, appears when scrolled up
│       │   │   └── MarkdownText.kt        # Markwon via AndroidView(TextView)
│       │   │
│       │   ├── commands/
│       │   │   ├── RatingUI.kt            # 5 face icons (Canvas drawn) in bot bubble with tooltip
│       │   │   ├── FeedbackInput.kt       # Textarea + Skip/Submit in bot bubble
│       │   │   ├── EndSessionUI.kt        # Pill buttons in bot bubble
│       │   │   ├── CountdownClose.kt      # Circular Canvas countdown in bot bubble
│       │   │   └── EndChatModal.kt        # Full-screen confirmation overlay
│       │   │
│       │   ├── media/
│       │   │   ├── ImageViewer.kt         # Full-screen Dialog with Coil AsyncImage
│       │   │   ├── ImagePickerPreview.kt  # Dynamic-width Row with X buttons
│       │   │   ├── AudioRecorderUI.kt     # Waveform + controls (adapter pattern)
│       │   │   └── AudioPlayerUI.kt       # Waveform progress bar (adapter pattern)
│       │   │
│       │   └── common/
│       │       ├── Header.kt              # Gradient background, bot icon, back/close buttons
│       │       ├── Icons.kt               # All 16 SVG icons as ImageVector (lucide ports)
│       │       ├── Strings.kt             # EN/AR localized string map (no Android resources)
│       │       └── Toast.kt              # Snackbar-based notifications for errors/status
│       │
│       └── adapters/
│           ├── ImagePickerAdapter.kt      # Interface: pickImages(maxCount) → List<ImageAsset>
│           ├── AudioRecorderAdapter.kt    # Interface: start/stop/pause/resume/getAmplitude/cleanup
│           └── AudioPlayerAdapter.kt      # Interface: load/play/pause/seekTo/getPosition/cleanup
│
└── src/test/kotlin/ai/teammates/rayachat/ui/
    ├── theme/
    │   └── ColorTokensTest.kt        # Light/dark tokens match RN SDK values
    └── components/
        ├── MessageBubbleTest.kt       # Compose preview + assertion tests
        ├── PresetButtonsTest.kt       # Wrapping, click callback
        └── MessageComposerTest.kt     # Enabled/disabled states, send callback
```

### Key Android-Specific UI Decisions

**MarkdownText — Markwon via AndroidView:**
Compose has no native markdown renderer. Markwon is the standard Android markdown library but works with `TextView`. We bridge it:
```kotlin
@Composable
fun MarkdownText(content: String, textColor: Color) {
    AndroidView(
        factory = { context ->
            val markwon = Markwon.builder(context).build()
            TextView(context).also { markwon.setMarkdown(it, content) }
        },
        update = { textView -> markwon.setMarkdown(textView, content) }
    )
}
```

**TypingIndicator — Compose `animateFloatAsState`:**
RN SDK uses `Animated.Value` with `translateY`. Compose equivalent:
```kotlin
val offset1 by animateFloatAsState(targetValue = if (active) -6f else 0f, animationSpec = tween(250))
```

**Icons — ImageVector (not SVG files):**
All 16 icons ported as Compose `ImageVector` builders. This is the standard for Compose — no SVG library needed:
```kotlin
val SendIcon = ImageVector.Builder(defaultWidth = 20.dp, defaultHeight = 20.dp, viewportWidth = 20f, viewportHeight = 20f)
    .path(fill = SolidColor(Color.Black)) {
        moveTo(15.44f, 1.68f)
        // ... same path data as RN SDK
    }.build()
```

**Adapter button visibility — same as RN SDK:**
Image/mic buttons hidden (not disabled) when no adapter provided. Emoji + send always visible.

**Toast — Snackbar:**
Android's native equivalent of the RN Toast. Uses `SnackbarHostState` in Compose:
```kotlin
val snackbarHostState = remember { SnackbarHostState() }
LaunchedEffect(error) {
    error?.let { snackbarHostState.showSnackbar(it) }
}
```

### Verification

- `./gradlew :raya-chat-ui:build` passes
- All components have `@Preview` functions viewable in Android Studio
- Compose UI tests pass for interactive components
- Icons match RN SDK visually

---

## Phase 4: Screen Orchestration + Integration Modes

**Days 10-12 · Goal: Wire everything into 4 integration modes. Full feature parity.**

### Files

```
raya-chat-ui/src/main/kotlin/ai/teammates/rayachat/ui/
├── RayaChatViewModel.kt          # Holds RayaChatClient + BotConfig, manages ViewMode
│
├── screens/
│   ├── IntroScreen.kt            # Full port (gradient header, card, footer)
│   ├── FormScreen.kt             # Full port (floating labels, validation)
│   └── ChatScreen.kt             # Full port (header + message list + footer + composer)
│
├── RayaChatWidget.kt             # Mode 1: @Composable entry point
├── RayaChatFragment.kt           # Mode 2: Fragment + ComposeView bridge
├── RayaChatBottomSheet.kt        # Mode 3: BottomSheetDialogFragment
└── RayaChatActivity.kt           # Optional full-screen Activity wrapper
```

### RayaChatViewModel

Bridges `RayaChatClient` (core) to UI. Manages the view state machine:

```kotlin
class RayaChatViewModel(
    private val client: RayaChatClient,
    private val botConfigFlow: StateFlow<BotConfigProps>
) : ViewModel() {

    val viewMode = MutableStateFlow(ViewMode.INTRO)
    val showEndChatModal = MutableStateFlow(false)

    // Delegate all state from client
    val messages get() = client.messages
    val currentMessage get() = client.currentMessage
    val loading get() = client.loading
    // ... etc

    fun startChat() {
        if (botConfig.enable_user_form) {
            viewMode.value = ViewMode.FORM
        } else {
            viewModelScope.launch {
                client.connect(UserInfo("", "", ""), botConfig)
                viewMode.value = ViewMode.CHAT
            }
        }
    }

    fun submitForm(userInfo: UserInfo) {
        viewModelScope.launch {
            client.connect(userInfo, botConfig)
            viewMode.value = ViewMode.CHAT
        }
    }

    fun closeChat() {
        Keyboard.dismiss()  // via LocalSoftwareKeyboardController
        if (viewMode.value == ViewMode.CHAT) {
            showEndChatModal.value = true
        } else {
            viewMode.value = ViewMode.INTRO
        }
    }

    fun confirmEndSession() {
        viewModelScope.launch {
            showEndChatModal.value = false
            client.endSession()
            viewMode.value = ViewMode.INTRO
        }
    }
}
```

### ChatScreen — Keyboard Handling

Android Compose handles keyboard differently from RN's `KeyboardAvoidingView`:

```kotlin
@Composable
fun ChatScreen(viewModel: RayaChatViewModel) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),      // ← handles keyboard avoidance
        topBar = { Header(...) },
        bottomBar = {
            MessageComposer(
                // ...
                onDismissKeyboard = { keyboardController?.hide() }
            )
        }
    ) { paddingValues ->
        MessageList(
            modifier = Modifier.padding(paddingValues),
            // ...
        )
    }
}
```

`imePadding()` is the Compose equivalent of `KeyboardAvoidingView` — it automatically adjusts layout when the software keyboard appears. No manual height calculation needed.

Keyboard dismissal on end chat modal: `keyboardController?.hide()` called in `closeChat()`.

### Mode 1: Compose Widget

```kotlin
@Composable
fun RayaChatWidget(
    token: String,
    locale: String = "en",
    imagePickerAdapter: ImagePickerAdapter? = null,
    audioRecorderAdapter: AudioRecorderAdapter? = null,
    onSessionStart: ((String) -> Unit)? = null,
    onSessionEnd: (() -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
    onClose: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val client = remember {
        RayaChatClient(context, RayaChatConfig(token, locale, onSessionStart, onSessionEnd, onError, onClose))
    }
    val viewModel = remember { RayaChatViewModel(client, ...) }

    DisposableEffect(Unit) {
        onDispose { client.destroy() }
    }

    RayaChatTheme(botConfig = viewModel.botConfig) {
        RayaChatContent(viewModel, imagePickerAdapter, audioRecorderAdapter)
    }
}
```

### Mode 2: Fragment (XML Apps)

```kotlin
class RayaChatFragment : Fragment() {
    companion object {
        fun newInstance(token: String, locale: String = "en"): RayaChatFragment {
            return RayaChatFragment().apply {
                arguments = bundleOf("token" to token, "locale" to locale)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                RayaChatWidget(
                    token = arguments?.getString("token") ?: "",
                    locale = arguments?.getString("locale") ?: "en"
                )
            }
        }
    }
}
```

The Fragment is a thin wrapper (~20 lines). All real work happens in the Composable. This makes the SDK available to ALL Android apps regardless of UI toolkit.

### Mode 3: BottomSheet

```kotlin
class RayaChatBottomSheet : BottomSheetDialogFragment() {
    companion object {
        fun show(fragmentManager: FragmentManager, token: String, locale: String = "en") {
            RayaChatBottomSheet().apply {
                arguments = bundleOf("token" to token, "locale" to locale)
            }.show(fragmentManager, "raya_chat_sheet")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                RayaChatWidget(
                    token = arguments?.getString("token") ?: "",
                    locale = arguments?.getString("locale") ?: "en",
                    onClose = { dismiss() }
                )
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            (this as BottomSheetDialog).behavior.apply {
                state = BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
            }
        }
    }
}
```

### Tests

```
src/test/kotlin/ai/teammates/rayachat/ui/
├── RayaChatViewModelTest.kt      # State transitions: INTRO→FORM→CHAT→END
├── screens/
│   ├── IntroScreenTest.kt        # Compose UI test: heading, button click
│   ├── FormScreenTest.kt         # Validation, submit callback
│   └── ChatScreenTest.kt         # Keyboard handling, command states

src/androidTest/kotlin/ai/teammates/rayachat/ui/
├── RayaChatWidgetTest.kt         # Integration: launch → full flow
├── RayaChatFragmentTest.kt       # Fragment inflation + Compose rendering
└── RayaChatBottomSheetTest.kt    # Show/dismiss lifecycle
```

### Verification

- All 4 modes launch without crash
- Full flow: Intro → Form → Chat → Send → Receive → Commands → End Session
- Keyboard avoidance works on ChatScreen
- End chat modal dismisses keyboard
- BottomSheet drag-to-dismiss works
- Fragment works in both Kotlin and Java Activities

---

## Phase 5: Sample App + Polish + Distribution

**Days 13-14 · Goal: Production-ready with demo app and Maven Central publishing.**

### Sample App Files

```
sample/
├── build.gradle.kts
├── src/main/
│   ├── AndroidManifest.xml
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml          # 4 demo buttons
│   │   │   └── activity_fragment_demo.xml # FrameLayout for Fragment mode
│   │   └── values/
│   │       └── strings.xml
│   │       └── themes.xml
│   └── kotlin/ai/teammates/rayachat/sample/
│       ├── MainActivity.kt                # Navigation to 4 demos
│       ├── ComposeDemoActivity.kt         # Mode 1: RayaChatWidget
│       ├── FragmentDemoActivity.kt        # Mode 2: RayaChatFragment in XML layout
│       ├── BottomSheetDemoActivity.kt     # Mode 3: RayaChatBottomSheet
│       ├── HeadlessDemoActivity.kt        # Mode 4: Custom UI with RayaChatClient
│       ├── adapters/
│       │   ├── SampleImagePickerAdapter.kt  # ActivityResultContracts.PickMultipleVisualMedia
│       │   └── SampleAudioRecorderAdapter.kt # MediaRecorder wrapper
│       └── headless/
│           └── CustomChatScreen.kt        # Custom Compose UI for headless demo
```

### Distribution Files

```
raya-chat-core/
├── consumer-rules.pro
│   # -keep class ai.teammates.rayachat.core.models.** { *; }
│   # -keep class ai.teammates.rayachat.core.RayaChatConfig { *; }
│   # -keep class ai.teammates.rayachat.core.RayaChatClient { *; }
│   # -keepclassmembers class * implements okhttp3.WebSocketListener { *; }

raya-chat-ui/
├── consumer-rules.pro
│   # -keep class ai.teammates.rayachat.ui.RayaChatWidget* { *; }
│   # -keep class ai.teammates.rayachat.ui.RayaChatFragment { *; }
│   # -keep class ai.teammates.rayachat.ui.RayaChatBottomSheet { *; }
│   # -keep class ai.teammates.rayachat.ui.RayaChatActivity { *; }
```

These consumer-rules.pro files ship INSIDE the AAR. Consumer apps don't need to configure ProGuard — the rules apply automatically.

### CI/CD

```
.github/workflows/
├── ci.yml                    # On every push: build + unit test + lint
│   steps:
│     - ./gradlew :raya-chat-core:test
│     - ./gradlew :raya-chat-ui:test
│     - ./gradlew :raya-chat-core:build
│     - ./gradlew :raya-chat-ui:build
│     - ./gradlew :sample:assembleDebug
│
└── publish.yml               # On tag (v*): publish to Maven Central
    steps:
      - ./gradlew :raya-chat-core:publishReleasePublicationToMavenCentralRepository
      - ./gradlew :raya-chat-ui:publishReleasePublicationToMavenCentralRepository
```

### Documentation

- KDoc on ALL public APIs (RayaChatClient, RayaChatConfig, RayaChatWidget, adapters)
- README.md with: Quick Start, 4 integration mode examples, configuration table, adapter docs, architecture diagram, troubleshooting

### Tests — Final QA

| Test | What | Expected |
|------|------|----------|
| Full flow (all 4 modes) | Intro → Form → Chat → Send → Receive → End | Works |
| Image upload | Pick images → preview → send → visible in chat after response | Images persist |
| Commands | end_session → rate → feedback → countdown → intro | Full cycle |
| Session resume | Chat → kill app → reopen → start chat | Messages restored |
| RTL | Set locale="ar" | Layout mirrors, Arabic text |
| Dark mode | Toggle system dark mode | Colors match spec |
| Background/foreground | Chat → Home → Return | Connection alive |
| Network drop | Airplane mode → disable → resume | Auto-reconnect |
| ProGuard | Build release APK → run | No crashes |
| Maven local | `./gradlew publishToMavenLocal` → test app imports | Works |

### Verification

- `./gradlew build` passes (all modules)
- `./gradlew test` passes (all unit tests)
- Sample app runs all 4 modes on emulator
- Release APK works (ProGuard doesn't break anything)
- `./gradlew publishToMavenLocal` succeeds
- Fresh project can import from Maven local and display the chat

---

## Complete File Inventory

| Module | Kotlin Files | Test Files | Resource Files | Total |
|--------|-------------|------------|---------------|-------|
| raya-chat-core | 22 | 15 | 2 | 39 |
| raya-chat-ui | 28 | 10 | 2 | 40 |
| sample | 8 | 0 | 5 | 13 |
| root (config) | 0 | 0 | 8 | 8 |
| **Total** | **58** | **25** | **17** | **100** |

## Timeline

```
Day 1-2:   Phase 1 — Models, constants, utilities, Gradle project
Day 3-6:   Phase 2 — Core protocol (WebSocket, API, storage, RayaChatClient)
Day 7-9:   Phase 3 — Theme + all Compose components
Day 10-12: Phase 4 — Screen orchestration + 4 integration modes
Day 13-14: Phase 5 — Sample app, polish, ProGuard, Maven publishing

Total: 14 days sequential
```

## Verification Checkpoints

After EACH phase:

1. `./gradlew build` passes
2. `./gradlew test` passes
3. No compiler warnings (treat warnings as errors)
4. All public APIs have KDoc
5. `./gradlew :sample:installDebug` runs on emulator (Phase 4+)
6. Full flow works end-to-end (Phase 4+)
