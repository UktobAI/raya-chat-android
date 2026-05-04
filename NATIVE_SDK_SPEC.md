# Native SDK Specification

Complete feature specification for building iOS (Swift) and Android (Kotlin) SDKs that are in full parity with the React Native SDK v0.1.0.

---

## 1. SDK Configuration

### Required Input
| Field | Type | Description |
|-------|------|-------------|
| `token` | String | Bot token from Teammates.ai dashboard |

### Optional Input
| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `locale` | String | `"en"` | `"en"` or `"ar"` |
| `imagePickerAdapter` | ImagePickerAdapter | — | Adapter for image selection (hidden if not provided) |
| `audioRecorderAdapter` | AudioRecorderAdapter | — | Adapter for voice recording (hidden if not provided) |
| `onSessionStart` | Callback | — | Fires with session ID on WS connect |
| `onSessionEnd` | Callback | — | Fires when session ends |
| `onError` | Callback | — | Fires on errors |
| `onClose` | Callback | — | Fires when user taps close |

### Hardcoded
| Value | Description |
|-------|-------------|
| `api.workforce.uktob.ai` | Production API endpoint (not configurable by developers) |
| `widget` | Integration type sent in WS URL |
| `0.1.0` | SDK version |

---

## 1b. Client Lifecycle

### `connect(userInfo, botConfig?)`
Starts the WebSocket connection. `botConfig` is optional but recommended — the initial bot message (`chatbox_initial_msg`) is injected locally, NOT sent by the server. Without `botConfig`, no welcome message appears.

### `endSession()`
User action — clears all storage (session ID, messages, user info), closes WebSocket, resets UI to intro screen. Fires `onSessionEnd` callback.

### `destroy()`
Lifecycle cleanup — releases WebSocket, cancels timers/coroutines, removes observers. Called when the host Activity/Fragment/View is destroyed. Does NOT clear persisted data (messages remain for session resume).

| Method | Clears storage? | Closes WebSocket? | Resets UI? | When to call |
|--------|----------------|-------------------|-----------|-------------|
| `endSession()` | Yes | Yes | Yes → Intro | User taps "End Session" |
| `destroy()` | No | Yes | N/A | Activity/Fragment `onDestroy` |

### `clearSessionCloseInfo()`
Resets the `sessionCloseInfo` state after an `auto_close` command. The intro screen shows a warning ("Session closed due to inactivity") — this clears it when the user starts a new chat.

---

## 1c. Public State

All state exposed as reactive streams (StateFlow on Android, Combine on iOS, React state on RN):

| State | Type | Description |
|-------|------|-------------|
| `messages` | List of TypeMessage | Full message history |
| `currentMessage` | String | Streaming text (grows as chunks arrive) |
| `connectionStatus` | Enum | `connecting`, `connected`, `disconnected`, `reconnecting` |
| `isConnected` | Boolean | WebSocket is open |
| `isOnline` | Boolean | Device has network connectivity |
| `loading` | Boolean | Bot is processing (STEP received) |
| `status` | String? | "Searching...", "Thinking..." |
| `info` | String? | "Waiting for human agent..." |
| `commandData` | CommandData? | Active server command |
| `presets` | List of String | Suggestion buttons |
| `showHumanAgentBtn` | Boolean | Escalation available |
| `sessionCloseInfo` | SessionCloseInfo? | Session closed by server |

---

## 2. API Endpoints

### Bot Config Fetch
```
GET https://api.workforce.uktob.ai/v1/agents/chatbox-config/widget/
Headers: Authorization: Bearer {token}
         Content-Type: application/json
```

**Response** — `BotConfigProps`:
```json
{
  "id": "uuid",
  "agent_id": "uuid",
  "theme": "light" | "dark",
  "chatbox_initial_msg": "Hi there 👋 How can I help?",
  "chatbox_placeholder": "Type your message...",
  "enable_voice_note": true,
  "enable_image_upload": true,
  "enable_realtime_voice_call": false,
  "enable_user_form": true,
  "enable_user_email": true,
  "enable_user_phone": false,
  "preset_options": [],
  "chatbox_gradient_color": "#0047AF",
  "chatbox_chat_icon": "https://...",
  "chatbox_btn_icon": null,
  "chatbox_system_heading": "Hi there Raya is ready to help ✨",
  "chatbox_system_paragraph": "Ask any question . Raya is fast and friendly."
}
```

**On failure:** Use defaults, don't crash.

### Default Bot Config (fallback)
```
theme: "light"
chatbox_initial_msg: "Hi there 👋 How can I help?"
chatbox_placeholder: "Type your message..."
enable_voice_note: true
enable_image_upload: true
enable_realtime_voice_call: false
enable_user_form: true
enable_user_email: true
enable_user_phone: false
preset_options: []
chatbox_gradient_color: "#0047AF"
chatbox_system_heading: "Hi there Raya is ready to help ✨"
chatbox_system_paragraph: "Ask any question — Raya is fast and friendly."
```

---

## 3. WebSocket Protocol

### Connection URL
```
wss://api.workforce.uktob.ai/v1/enhanced-chat/ws/stream
  ?integration_type=widget
  &token={token}
  &agent_id=null
  &chat_session_id={sessionId_or_empty}
  &user_name={urlEncode(fullName)}
  &email={urlEncode(email.toLowerCase())}
  &phone={urlEncode(phone)}
  &data={urlEncode(JSON.stringify(deviceMetadata))}
```

**IMPORTANT:** `data` parameter MUST be URL-encoded. Android's OkHttp WebSocket crashes with raw JSON in query params.

### Device Metadata (JSON in `data` param)
```json
{
  "platform": "ios" | "android",
  "os_version": "17.2",
  "device_family": "iPhone/iPad" | "Android",
  "sdk_version": "0.1.0",
  "locale": "en",
  "timezone": "Asia/Dubai"
}
```

### Session ID Behavior
| Scenario | `chat_session_id` value |
|----------|------------------------|
| First connection | Empty string `""` |
| Resuming session | Stored session ID from previous connection |
| After `endSession()` | Empty string (storage cleared) |

Server assigns session ID in the first `RESPONSE` message's `data.chat_session_id` field.

### Heartbeat
| Parameter | Value |
|-----------|-------|
| Ping message | Plain text `"ping"` |
| Pong response | Plain text `"pong"` |
| Ping interval | 25 seconds |
| Pong timeout | 60 seconds |
| On timeout | Close with code 1006, trigger reconnect |
| On app background | Stop heartbeat |
| On app foreground | Restart heartbeat + verify connection with ping |

### Reconnection
| Parameter | Value |
|-----------|-------|
| Strategy | Exponential backoff with jitter |
| Formula | `min(1000 * 2^attempt + random(0..1000), 30000)` |
| Max attempts | 100 |
| Max delay | 30 seconds |
| Reset counter | On successful connection |
| Stop conditions | Manual close, auto_close command, app backgrounded, max attempts |

### Close Codes
| Code | Meaning | Reconnect? |
|------|---------|-----------|
| 1000 | Normal closure | No |
| 1001 | Going away | No |
| 1006 | Abnormal (heartbeat timeout) | Yes |
| Other | Unexpected | Yes |

### Message Queue
Messages sent while WebSocket is in `CONNECTING` state are queued in memory and flushed in order on successful connection.

---

## 4. Outbound Message Formats

### Text Message
```json
{
  "content": "user message text",
  "images": []
}
```

### Image Message
```json
{
  "content": "optional caption",
  "images": [
    {
      "name": "photo.jpg",
      "type": "image/jpeg",
      "data": "data:image/jpeg;base64,/9j/4AAQ..."
    }
  ]
}
```
- Max 5 images per message
- Base64 includes data URL prefix (`data:image/jpeg;base64,...`)

### Audio Message
```
{raw base64 string — NOT wrapped in JSON}
```

### Command Response
```json
{
  "type": "command_response",
  "command": "rate_conversation",
  "response": 4
}
```

### Human Agent Escalation
```json
{
  "content": "/human_agent",
  "images": []
}
```

---

## 5. Inbound Message Types

### STEP — Bot thinking
```json
{ "type": "step", "text": "Searching..." }
```
**Action:** Show typing indicator, set loading = true.

### CHUNK — Streaming text
```json
{ "type": "chunk", "text": "partial response text" }
```
**Action:** Append to streaming buffer. Multiple chunks concatenate into one response. Clear typing indicator, show streaming bubble.

### RESPONSE — Complete response
```json
{
  "type": "response",
  "data": {
    "id": "uuid",
    "chat_session_id": "uuid",
    "sender": 2,
    "content": "Full response text",
    "created_at": 1774436672,
    "attachments": ["https://s3-url-1", "https://s3-url-2"],
    "audio_urls": ""
  }
}
```
**Action:**
1. Save `chat_session_id` to storage (if new)
2. If `attachments` has URLs → update **storage only** (not UI state) for the last user message
3. Add bot message to message list
4. Clear streaming buffer
5. Set loading = false

**CRITICAL:** Attachment URLs from RESPONSE update **storage only** (for session resume). Do NOT replace local image URIs in the UI — they would disappear.

### PRESETS — Suggestion buttons
```json
{
  "type": "presets",
  "presets": [
    { "title": "Ask about products" },
    { "title": "Track order" }
  ]
}
```
**Action:** Extract titles, show as pill buttons.

### COMMAND — Interactive UI
```json
{
  "type": "command",
  "content": "rate_conversation",
  "options": [1, 2, 3, 4, 5],
  "message": "How would you rate this conversation?",
  "optional": false
}
```

**Command types:**

| `content` | UI | User response |
|-----------|-----|--------------|
| `end_session` | Yes/No buttons | Selected option (string) |
| `rate_conversation` | 5 face icons (red→green) | Rating (1-5) |
| `submit_feedback` | Textarea + Skip/Submit | Feedback text or empty |
| `feedback_received` | Countdown timer (3s) | Auto → endSession() |
| `auto_close` | Return to intro | No response needed |

**`auto_close` special behavior:**
- Set session close info with reason "inactivity"
- Prevent all future reconnections
- Clear all streaming state
- Return to intro screen
- Show warning on intro: "Session closed due to inactivity"

### ERROR
```json
{ "type": "error", "text": "error details" }
```
**Action:** Sanitize text (remove HTML, JS, sensitive info), fire onError callback.

### ESCALATION
```json
{ "type": "human-rep-escalation" }
```
**Action:** Show "Connect with human representative" button.

### INFO
```json
{ "type": "info", "text": "Connecting to human agent..." }
```
**Action:** Show with hourglass animation. If text contains "human agent" → hide escalation button.

### AGENT_ACTIVITY
```json
{ "type": "agent_activity", "text": "Agent joined the conversation" }
```
**Action:** Add as system message (centered, small text, gray background).

### MESSAGE
```json
{ "type": "message", "content": "Direct message text" }
```
**Action:** Add as bot message (no streaming).

---

## 6. UI State Machine

```
┌───────────────────────────────────────────────────────┐
│                                                       │
│  INTRO ──Start Chat──→ FORM ──Submit──→ CHAT          │
│    ↑                     ↑                  │         │
│    └───────Back──────────┘                  │         │
│    ↑                                        │         │
│    └──────End Session / Close───────────────┘         │
│                                                       │
│  CHAT ──Close (X)──→ END CHAT MODAL                  │
│                        ├── Cancel → back to CHAT      │
│                        └── End → endSession → INTRO   │
│                                                       │
└───────────────────────────────────────────────────────┘
```

### Intro Screen
- Bot avatar (from `chatbox_chat_icon` or default)
- "Online now" badge (emerald green)
- Heading (from `chatbox_system_heading`)
- Paragraph (from `chatbox_system_paragraph`)
- White/dark card with:
  - "Start a new conversation and ask me anything"
  - "Start a chat" button (gradient color)
  - Privacy note with shield icon
- "Powered by Teammates.ai" footer (links to teammates.ai)
- Session close warning (if auto_close happened previously)

### Form Screen
- Shared gradient header with back (ChevronLeft) button
- User avatar circle (gradient color)
- "Welcome to our live chat!" text
- Full Name input (required)
- Email input (if `enable_user_email`)
- Phone input (if `enable_user_phone`)
- "Start the chat" button with loading spinner
- Floating labels that animate up when field has value
- Validation on blur + on submit
- Error messages with ⚠ icon

### Chat Screen
- Gradient header with bot icon + close (X) button
- Scrollable message list with:
  - Bot messages (left, gray bubble, avatar, markdown)
  - User messages (right, gradient bubble, no avatar)
  - System messages (centered, small, gray background)
  - User image messages (images above text bubble, right-aligned)
  - Typing indicator (3 bouncing dots in bot bubble)
  - Info display (hourglass + text)
  - Escalation button
  - Command UIs (rendered as bot bubbles)
  - Preset buttons (wrapping pills, right-aligned)
- Message composer (fixed at bottom):
  - Text input (multiline, max 160px height)
  - Emoji button (leftmost)
  - Image upload button (hidden if no adapter)
  - Mic button (hidden if no adapter)
  - Send button (circular, gradient bg)
  - Disabled during commands with "Please select an option above" placeholder
- KeyboardAvoidingView wraps the entire chat screen
- Auto-scroll to bottom on new messages
- Scroll-to-bottom floating button when scrolled up

### End Chat Modal
- Full-screen overlay (theme background)
- MessageSquareX icon in circle
- "End Chat Session" title
- "Do you want to end this chat session?" subtitle
- Cancel button (outlined)
- End Session button (gradient color)

---

## 7. Persistence

### Storage Architecture (per platform)

| Data | RN SDK | Android SDK | iOS SDK |
|------|--------|-------------|---------|
| Session ID | AsyncStorage (key-value) | EncryptedSharedPreferences | Keychain |
| User info | AsyncStorage (key-value) | EncryptedSharedPreferences | Keychain |
| Messages | AsyncStorage (JSON string) | Room Database (SQLite) | Core Data |

**Why native SDKs use database for messages (not key-value):**
The RN SDK stores messages as one giant JSON string — every message add requires read-all → parse → append → stringify → write-all (O(n)). Native SDKs use proper databases for O(1) inserts, SQL trimming, and reactive queries.

### Storage Keys / Tables

| Key/Table | Content | Written when | Read when |
|-----------|---------|-------------|-----------|
| `raya-chat-session-id` | Session ID string | Server assigns via RESPONSE | On `connect()` |
| `messages` table (Room/Core Data) | Individual message rows | After every send/receive | On `connect()` |
| `raya-chat-user` | `{ fullName, email, phone }` | Form submit | Not currently read back |

### Message Trimming
Cap at 500 messages. When exceeded, trim oldest.
- RN: `array.slice(-500)` then write all
- Android: `DELETE FROM messages WHERE id NOT IN (SELECT id ORDER BY created_at DESC LIMIT 500)`
- iOS: Core Data fetch request with `fetchLimit` + delete

### Clear All
On `endSession()`: remove all stored data (session ID, messages, user info).

### Encrypted Storage Fallback (Android)
`EncryptedSharedPreferences` may throw on older/rooted devices. Implementation MUST fall back to regular `SharedPreferences` with a logged warning. Never crash.

---

## 8. Theming

### Two-Tier Color System
1. **`theme`** (`"light"` / `"dark"`) — controls app scaffold (backgrounds, borders, text)
2. **`chatbox_gradient_color`** brightness — controls text on gradient (buttons, user bubbles, header icons)

These are **independent**. A dark theme can have a light gradient and vice versa.

### Gradient Foreground Calculation
```
luminance = (0.299 * R + 0.587 * G + 0.114 * B) / 255
if luminance < 0.5 → foreground = "#FFFFFF" (white text on dark gradient)
else → foreground = "#1A1A1A" (dark text on light gradient)
```

### Dark Mode Colors
| Element | Color |
|---------|-------|
| Background | `#14161A` |
| Surface/Cards | `#2C2D31` |
| Borders | `#3F3F46` |
| Text | `#F1F1F0` |
| Muted text | `#A1A1AA` |
| Bot bubble | `#2C2D31` |
| Intro card | `#1B1C20` |

### Light Mode Colors
| Element | Color |
|---------|-------|
| Background | `#FFFFFF` |
| Bot bubble | `#F5F5F5` |
| Borders | `#E4E4E7` |
| Text | `#14161A` |
| Muted text | `#71717A` |
| Intro card | `#FFFFFF` |

---

## 9. RTL Support (Arabic)

When `locale = "ar"`:
- All text right-aligned
- Flex rows reversed (`row-reverse`)
- Margins/paddings mirrored
- Back arrow flipped
- Arabic translations for all built-in strings
- Per-message text direction detection for mixed-language chats
- **No global RTL mutation** — SDK must not affect host app layout

---

## 10. Error Sanitization

All error messages from server must be sanitized before display:
1. Strip HTML tags (`<[^>]*>`)
2. Strip `javascript:` protocols
3. Strip event handlers (`on\w+=`)
4. Trim whitespace
5. Cap at 200 characters
6. Replace sensitive patterns (`api_key`, `password`, `token`, `secret`, `internal server error`, `stack trace`) with generic message
7. Default: "An unexpected error occurred. Please try again."

---

## 11. Preset Behavior

### Dynamic Presets (from server)
- Set when server sends `PRESETS` message
- Cleared when: user sends any message, clicks a preset, command received, session ends

### Static Presets (from bot config `preset_options`)
- Show only when `messages.length === 1` (initial bot message only)
- Never cleared in state — disappear naturally when more messages added

---

## 12. Edge Cases

| Scenario | Behavior |
|----------|----------|
| App killed during chat | Resume from storage on reopen |
| Network drop mid-message | Message queued, sent on reconnect |
| 60s+ background | Clear stale streaming state on foreground |
| Max reconnect attempts | Fire `onError("Connection lost")`, stop retrying |
| Invalid JSON from server | Silently discard |
| Config fetch fails | Use defaults, proceed to chat |
| Empty token | Show loading indefinitely (no crash) |
| Rapid send button taps | Each message sent independently |
| 500+ messages | Oldest trimmed from memory + storage |

---

## 13. CDN Assets

```
Base: https://app.teammates.ai/api/assets/

Sounds:
  /sound/chats/msg_sent.mp3
  /sound/chats/msg_received.mp3
  /sound/voice/call-connected.mp3
  /sound/voice/call-end.mp3

Images:
  /images/New_raya_agent.png  (default bot avatar)

Animations:
  /animations/hourGlassAnimation.json  (Lottie, for info display)
  /animations/typing-dark.webp         (typing indicator, dark mode)
  /animations/typing-light.webp        (typing indicator, light mode)
```

---

## 14. Adapter Interfaces

The SDK uses pluggable adapters for native device features. Image upload and mic buttons are **hidden** (not disabled) when no adapter is provided. This keeps the SDK dependency-free — the host app provides the native bridge.

### ImagePickerAdapter

```
pickImages(maxCount: Int) → List<ImageAsset>
```

Returns a list of selected images with `uri`, `name`, `type`, `base64` fields. Max 5 images per message.

### AudioRecorderAdapter

```
startRecording()
stopRecording() → { uri: String, base64: String? }
pauseRecording()
resumeRecording()
getAmplitude() → Float (0..1)     // for waveform visualization
cleanup()
```

### AudioPlayerAdapter

```
loadAudio(uri: String) → { durationMs: Long }
play()
pause()
seekTo(positionMs: Long)
getPosition() → Long
cleanup()
```

### Button Visibility

| Adapters provided | Buttons shown in composer |
|-------------------|--------------------------|
| None | Emoji + Send only |
| `imagePickerAdapter` only | Emoji + Paperclip + Send |
| `audioRecorderAdapter` only | Emoji + Mic + Send |
| Both adapters | Emoji + Paperclip + Mic + Send |

---

## 15. Toast / Error Notifications

Connection errors and status changes are shown as brief notifications within the chat UI:

| Trigger | Message | Duration | Dismiss on |
|---------|---------|----------|-----------|
| WebSocket disconnected | "Connection lost" | Until reconnect | WS reconnect |
| Reconnecting | "Reconnecting..." | Until reconnect | WS reconnect |
| Offline (no network) | "You're offline" | Until online | Network recovery |
| Max reconnect attempts | "Connection lost. Please try again." | 5 seconds | Auto |
| Send failed (queued) | "Message queued — reconnecting..." | 3 seconds | Auto |

On Android: use Compose `Snackbar` via `SnackbarHostState`.
On iOS: use a custom slide-down view.
On RN: use the existing `ToastProvider` + `useToast` system (already built but not fully wired).

---

## 16. What's NOT in v0.1.0 (Future)

- LiveKit voice/video calls
- Push notifications
- Offline message queue (partial — queue exists during CONNECTING only)
- File attachments (non-image)
- Message search
- Message editing/deletion
- Read receipts
- Typing indicators (outbound — we only receive, don't send)
- End-to-end encryption

---

## 17. Platform Distribution

| Platform | Package Name | Distribution |
|----------|-------------|-------------|
| React Native | `@teammates-ai/raya-chat-react-native` | npm |
| iOS | `RayaChatSDK` | Swift Package Manager + CocoaPods |
| Android (headless) | `ai.teammates:raya-chat-core` | Maven Central |
| Android (packaged UI) | `ai.teammates:raya-chat-ui` | Maven Central |

**Android dual-module split:** `raya-chat-core` has zero UI dependencies (~500KB). `raya-chat-ui` includes core + Compose + Coil + Markwon (~1.5MB). Headless developers install only core.
