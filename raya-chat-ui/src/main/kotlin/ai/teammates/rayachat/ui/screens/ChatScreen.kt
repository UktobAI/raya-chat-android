package ai.teammates.rayachat.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import ai.teammates.rayachat.core.Constants
import ai.teammates.rayachat.core.ImagePayload
import ai.teammates.rayachat.core.models.*
import ai.teammates.rayachat.ui.adapters.AudioRecorderAdapter
import ai.teammates.rayachat.ui.adapters.ImagePickerAdapter
import ai.teammates.rayachat.ui.components.chat.*
import ai.teammates.rayachat.ui.components.commands.*
import ai.teammates.rayachat.ui.components.common.Header
import ai.teammates.rayachat.ui.components.common.Strings
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
    statusBarHeight: Int = 0,
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
    val chatIcon = botConfig.chatboxChatIcon

    // Full-screen image viewer state
    var fullScreenImage by remember { mutableStateOf<String?>(null) }

    // ── Command UI ──
    @Composable
    fun CommandUI() {
        val cmd = commandData ?: return
        when (cmd.content) {
            "rate_conversation" -> RatingUI(
                message = cmd.message,
                options = cmd.options,
                botIcon = chatIcon,
                locale = locale,
                onRate = { rating -> onSendCommandResponse("rate_conversation", rating) },
            )
            "submit_feedback" -> FeedbackInput(
                message = cmd.message,
                optional = cmd.optional,
                botIcon = chatIcon,
                locale = locale,
                onSubmit = { text -> onSendCommandResponse("submit_feedback", text) },
            )
            "feedback_received" -> CountdownClose(
                message = cmd.message,
                botIcon = chatIcon,
                locale = locale,
                onComplete = onEndSession,
            )
            "end_session" -> EndSessionUI(
                message = cmd.message,
                options = cmd.options,
                botIcon = chatIcon,
                onSelect = { option -> onSendCommandResponse("end_session", option) },
            )
        }
    }

    // ── Footer content (scrolls with messages) ──
    val footerContent: @Composable () -> Unit = {
        // Typing indicator
        if (loading && currentMessage.isEmpty() && info == null) {
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 0.dp).padding(bottom = 20.dp),
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
        CommandUI()

        // Dynamic presets
        if (presets.isNotEmpty() && commandData == null) {
            PresetButtons(presets = presets, onPress = onSendPreset)
        }

        // Static presets from config (only on first message)
        if (!botConfig.presetOptions.isNullOrEmpty() && messages.size == 1 && commandData == null) {
            PresetButtons(presets = botConfig.presetOptions, onPress = onSendPreset)
        }
    }

    // ── Main layout ──
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .imePadding() // keyboard avoidance
    ) {
        // Header
        Header(
            botIcon = chatIcon,
            statusBarHeight = statusBarHeight,
            showCloseButton = true,
            onClose = {
                keyboardController?.hide()
                onClose()
            },
        )

        // Message list
        Box(modifier = Modifier.weight(1f)) {
            MessageList(
                messages = messages,
                currentMessage = currentMessage,
                botIcon = chatIcon,
                onImagePress = { uri -> fullScreenImage = uri },
                footerContent = footerContent,
            )
        }

        // Full-screen image viewer
        ImageViewer(imageUri = fullScreenImage, onClose = { fullScreenImage = null })

        // Composer — always rendered, disabled during commands
        MessageComposer(
            placeholder = if (commandData != null) Strings.get("select_option", locale) else botConfig.chatboxPlaceholder,
            enableVoiceNote = botConfig.enableVoiceNote,
            enableImageUpload = botConfig.enableImageUpload,
            hasImageAdapter = imagePickerAdapter != null,
            hasAudioAdapter = audioRecorderAdapter != null,
            disabled = commandData != null || (loading && currentMessage.isEmpty()),
            locale = locale,
            onSendMessage = onSendMessage,
            onImagePress = null, // TODO: Wire image picker adapter
            onMicPress = null, // TODO: Wire audio recorder adapter
        )
    }
}
