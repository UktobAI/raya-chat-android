# Changelog

All notable changes to the Raya Chat Android SDK are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.2] — Unreleased

### Added
- **Voice notes — works out of the box.** Declare `RECORD_AUDIO` in your host manifest and the SDK handles record / preview / send / playback with no adapter required. iOS-parity feature.
  - `DefaultAudioRecorderAdapter` — `AudioRecord`-based PCM 16 kHz mono 16-bit recorder, manual RIFF/WAV header writer, `makeIfAvailable()` permission gating, orphan-file cleanup on init.
  - `DefaultAudioPlayerAdapter` — `MediaPlayer`-based playback for `data:` / `https://` / `file://` / absolute path URIs, WAV-PCM amplitude scanner for in-bubble waveform.
  - `AudioFocusCoordinator` — centralized `AudioFocusRequest` / deprecated-fallback handling. Recorder pauses on transient focus loss (phone call, voice assistant) and stays paused — user must explicitly resume (matches iOS UX).
  - `AudioRecorderUI` — live recording overlay with scrolling waveform (40 bars, 10 Hz sampling), monospaced HH:MM:SS timer, pause/resume, stop.
  - `AudioPreviewUI` — post-stop preview overlay with static waveform + playback progress, cancel/play/send.
  - `AudioPlayerUI` — in-bubble player with waveform, dashed-line backdrop, played/unplayed bar coloring, end-of-playback fallback (200 ms tolerance + 3 stationary 100 ms ticks for OEM `OnCompletionListener` flakiness).
- New `audioPlayerAdapter: AudioPlayerAdapter? = null` parameter on `RayaChatWidget`, `RayaChatFragment`, `RayaChatBottomSheet` (additive — existing callers unchanged).
- `AudioPlayerAdapter.getAmplitudes(sampleCount: Int): FloatArray?` default method (additive — UI degrades to dashed-only when `null` is returned).
- `Constants.MAX_AUDIO_DURATION_SECONDS = 180`, `MAX_AUDIO_PAYLOAD_BYTES = 10 MB`, `MIN_AUDIO_PAYLOAD_BYTES = 4 KB`.

### Fixed
- **Voice notes were silently dropped server-side.** `RayaChatClient.sendAudio` was sending base64 as a text WebSocket frame; the server routes binary frames to its transcription pipeline but parses text frames as JSON commands, so audio was silently discarded. Now sent as a proper binary frame via the new `WebSocketManager.sendBinary`.
- Audio bubbles not rendering — `MessageBubble` had no `type == 2` branch, so audio messages were invisible. Now renders a flat-row `AudioPlayerUI` with optional bot avatar.

### Changed
- `Constants.SDK_VERSION` bumped to `"0.1.2"`.

### Notes
- WAV PCM 16 kHz mono is non-negotiable. The server's transcription pipeline only accepts WAV — `MediaRecorder` MPEG-4/AAC output is rejected.
- This release matches iOS SDK v0.1.2 voice-note feature parity.

---

## [0.1.1] — 2026-05-05

### Changed
- WebSocket URL path: `/v1/conversations/ws/start` → `/v1/enhanced-chat/ws/stream`. Aligns with React Native SDK and web widget.
- `RayaChatClient.onAttachments` now preserves the original `id` and `name` of each image attachment when merging in remote URLs (was overwriting both with empty strings). Brings `onMessageUpdate` / `onSessionEnd` payloads to RN-SDK parity.

### Added
- JitPack publishing setup for `raya-chat-core` and `raya-chat-ui` modules.
- Apache-2.0 LICENSE.

---

## [0.1.0] — 2026-04-XX

Initial public release. Native Kotlin + Jetpack Compose SDK for embedding the Raya AI chat widget in Android apps.

- Four integration modes: Compose Widget, Fragment, BottomSheet, Headless.
- WebSocket streaming with auto-reconnect (exponential backoff, 100 retry attempts).
- Image attachments (up to 5 per message) and audio recording via adapter interfaces.
- Markdown rendering, RTL/Arabic, session persistence (Room + EncryptedSharedPreferences).
- Background/foreground lifecycle handling.
- `onSessionStart` / `onSessionEnd` / `onMessageUpdate` / `onError` / `onClose` callbacks.
