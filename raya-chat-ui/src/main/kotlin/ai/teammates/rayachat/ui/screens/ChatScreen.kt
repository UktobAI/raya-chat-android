package ai.teammates.rayachat.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import ai.teammates.rayachat.core.Constants
import ai.teammates.rayachat.core.adapters.AudioPlayerAdapter
import ai.teammates.rayachat.core.adapters.AudioResult
import ai.teammates.rayachat.core.models.ImagePayload
import ai.teammates.rayachat.core.models.*
import ai.teammates.rayachat.ui.adapters.AudioRecorderAdapter
import ai.teammates.rayachat.ui.adapters.ImagePickerAdapter
import ai.teammates.rayachat.ui.components.chat.*
import ai.teammates.rayachat.ui.components.commands.*
import ai.teammates.rayachat.ui.components.common.Header
import ai.teammates.rayachat.ui.components.common.Strings
import ai.teammates.rayachat.ui.components.media.AudioPreviewUI
import ai.teammates.rayachat.ui.components.media.AudioRecorderUI
import ai.teammates.rayachat.ui.components.media.ImageViewer
import ai.teammates.rayachat.ui.theme.LocalRayaTheme
import ai.teammates.rayachat.ui.theme.RayaTypography

@Composable
internal fun ChatScreen(
    messages: List<TypeMessage>,
    currentMessage: String,
    loading: Boolean,
    status: String?,
    info: String?,
    commandData: CommandData?,
    presets: List<String>,
    botConfig: BotConfigProps,
    isConnected: Boolean,
    showHumanAgentBtn: Boolean,
    imagePickerAdapter: ImagePickerAdapter?,
    audioRecorderAdapter: AudioRecorderAdapter?,
    audioPlayerAdapter: AudioPlayerAdapter? = null,
    onSendMessage: (String) -> Unit,
    onSendImages: (List<ImagePayload>, String) -> Unit,
    onSendAudio: (String) -> Unit,
    onSendPreset: (String) -> Unit,
    onSendCommandResponse: (String, Any) -> Unit,
    onEndSession: () -> Unit,
    onClose: () -> Unit,
) {
    val theme = LocalRayaTheme.current
    val locale = theme.locale
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val chatIcon = botConfig.chatboxChatIcon  // null if not configured — components hide avatar when null
    val scope = rememberCoroutineScope()

    // Full-screen image viewer state
    var fullScreenImage by remember { mutableStateOf<String?>(null) }

    // Voice-note flow: None → Recording → Preview → (Send | Cancel) → None
    var audioFlow by remember { mutableStateOf<AudioFlow>(AudioFlow.None) }

    // Dismiss keyboard — hide only, do NOT clearFocus().
    // clearFocus() causes BringIntoView on re-focus which doubles IME padding on API 35+.
    fun dismissKeyboard() {
        keyboardController?.hide()
    }

    // Wrap callbacks to dismiss keyboard on user actions
    val onSendMessageWithDismiss: (String) -> Unit = { text ->
        dismissKeyboard()
        onSendMessage(text)
    }
    val onSendPresetWithDismiss: (String) -> Unit = { text ->
        dismissKeyboard()
        onSendPreset(text)
    }
    val onSendImagesWithDismiss: (List<ImagePayload>, String) -> Unit = { images, caption ->
        dismissKeyboard()
        onSendImages(images, caption)
    }

    // Footer content extracted as a remembered lambda that only changes when relevant state changes.
    // This prevents MessageList from recomposing on unrelated state changes (e.g., keystrokes).
    val footerContent: @Composable () -> Unit = remember(
        loading, currentMessage.isEmpty(), info, showHumanAgentBtn, commandData, presets, messages.size
    ) {
        @Composable {
            ChatFooter(
                loading = loading,
                isStreaming = currentMessage.isNotEmpty(),
                info = info,
                showHumanAgentBtn = showHumanAgentBtn,
                commandData = commandData,
                presets = presets,
                messagesSize = messages.size,
                botConfig = botConfig,
                chatIcon = chatIcon,
                locale = locale,
                theme = theme,
                onSendMessage = onSendMessageWithDismiss,
                onSendPreset = onSendPresetWithDismiss,
                onSendCommandResponse = onSendCommandResponse,
                onEndSession = onEndSession,
            )
        }
    }

    // ── Main layout ──
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
        // Header
        Header(
            botIcon = chatIcon,
            showCloseButton = true,
            onClose = {
                dismissKeyboard()
                onClose()
            },
        )

        // Footer change signal — triggers auto-scroll when presets/typing/commands change
        val footerSignal = remember(loading, presets.size, commandData, info, showHumanAgentBtn) {
            java.util.Objects.hash(loading, presets.size, commandData, info, showHumanAgentBtn)
        }

        // Message list — tap on empty area to dismiss keyboard.
        // Uses pointerInput instead of clickable to avoid stealing focus from the text field.
        // Stealing focus triggers BringIntoView on re-focus, which causes keyboard padding issues.
        Box(modifier = Modifier.weight(1f).pointerInput(Unit) {
            detectTapGestures { dismissKeyboard() }
        }) {
            MessageList(
                messages = messages,
                currentMessage = currentMessage,
                botIcon = chatIcon,
                onImagePress = { uri -> fullScreenImage = uri },
                footerContent = footerContent,
                footerChangeSignal = footerSignal,
            )
        }

        // Full-screen image viewer
        ImageViewer(imageUri = fullScreenImage, onClose = { fullScreenImage = null })

        // Bottom slot: composer / recorder / preview, depending on audioFlow.
        when (val flow = audioFlow) {
            AudioFlow.None -> MessageComposer(
                placeholder = if (commandData != null) Strings.get("select_option", locale) else botConfig.chatboxPlaceholder,
                enableVoiceNote = botConfig.enableVoiceNote,
                enableImageUpload = botConfig.enableImageUpload,
                imagePickerAdapter = imagePickerAdapter,
                hasAudioAdapter = audioRecorderAdapter != null,
                disabled = commandData != null || (loading && currentMessage.isEmpty()),
                locale = locale,
                onSendMessage = onSendMessageWithDismiss,
                onSendImages = onSendImagesWithDismiss,
                onMicPress = audioRecorderAdapter?.let {
                    {
                        dismissKeyboard()
                        audioFlow = AudioFlow.Recording
                    }
                },
            )

            AudioFlow.Recording -> audioRecorderAdapter?.let { rec ->
                AudioRecorderUI(
                    adapter = rec,
                    onComplete = { result, amps ->
                        audioFlow = AudioFlow.Preview(result, amps)
                    },
                    onCancel = { audioFlow = AudioFlow.None },
                )
            } ?: run { audioFlow = AudioFlow.None }

            is AudioFlow.Preview -> AudioPreviewUI(
                audioResult = flow.result,
                amplitudes = flow.amps,
                audioPlayerAdapter = audioPlayerAdapter,
                onSend = {
                    val payload = flow.result.base64?.takeIf { it.isNotEmpty() }
                        ?: flow.result.uri // fallback — sendAudio handles data URIs too
                    onSendAudio(payload)
                    audioFlow = AudioFlow.None
                },
                onCancel = { audioFlow = AudioFlow.None },
            )
        }

    }
}

/** Voice-note FSM: composer ↔ recorder ↔ preview. */
private sealed class AudioFlow {
    object None : AudioFlow()
    object Recording : AudioFlow()
    data class Preview(val result: AudioResult, val amps: FloatArray) : AudioFlow() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Preview) return false
            return result == other.result && amps.contentEquals(other.amps)
        }
        override fun hashCode(): Int = 31 * result.hashCode() + amps.contentHashCode()
    }
}

/** Extracted footer — stable parameters prevent unnecessary recompositions of MessageList. */
@Composable
private fun ChatFooter(
    loading: Boolean,
    isStreaming: Boolean,
    info: String?,
    showHumanAgentBtn: Boolean,
    commandData: CommandData?,
    presets: List<String>,
    messagesSize: Int,
    botConfig: BotConfigProps,
    chatIcon: String?,
    locale: String,
    theme: ai.teammates.rayachat.ui.theme.RayaTheme,
    onSendMessage: (String) -> Unit,
    onSendPreset: (String) -> Unit,
    onSendCommandResponse: (String, Any) -> Unit,
    onEndSession: () -> Unit,
) {
    // Typing indicator
    if (loading && !isStreaming && info == null) {
        TypingIndicator(botIcon = chatIcon)
    }

    // Info display (hourglass + text)
    if (info != null) {
        Row(
            modifier = Modifier.padding(horizontal = 48.dp).padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(
                painter = rememberAsyncImagePainter("${Constants.ASSET_BASE_URL}/animations/hourGlassAnimation.gif"),
                contentDescription = null,
                modifier = Modifier.size(30.dp),
            )
            Text(info, style = RayaTypography.caption, color = theme.foreground)
        }
    }

    // Escalation button
    if (showHumanAgentBtn) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Box(
                modifier = Modifier
                    .border(1.5.dp, theme.gradientColor, RoundedCornerShape(8.dp))
                    .clickable { onSendMessage("/human_agent") }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                Text(
                    Strings.get("connect_human", locale),
                    fontSize = 12.sp,
                    color = theme.gradientColor,
                )
            }
        }
    }

    // Command UI
    if (commandData != null) {
        when (commandData.content) {
            "rate_conversation" -> RatingUI(
                message = commandData.message, options = commandData.options,
                botIcon = chatIcon, locale = locale,
                onRate = { rating -> onSendCommandResponse("rate_conversation", rating) },
            )
            "submit_feedback" -> FeedbackInput(
                message = commandData.message, optional = commandData.optional,
                botIcon = chatIcon, locale = locale,
                onSubmit = { text -> onSendCommandResponse("submit_feedback", text) },
            )
            "feedback_received" -> CountdownClose(
                message = commandData.message, botIcon = chatIcon,
                locale = locale, onComplete = onEndSession,
            )
            "end_session" -> EndSessionUI(
                message = commandData.message, options = commandData.options,
                botIcon = chatIcon,
                onSelect = { option -> onSendCommandResponse("end_session", option) },
            )
        }
    }

    // Dynamic presets
    if (presets.isNotEmpty() && commandData == null) {
        PresetButtons(presets = presets, onPress = onSendPreset)
    }

    // Static presets from config (only on first message)
    if (!botConfig.presetOptions.isNullOrEmpty() && messagesSize == 1 && commandData == null) {
        PresetButtons(presets = botConfig.presetOptions.orEmpty(), onPress = onSendPreset)
    }
}
