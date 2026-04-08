package ai.teammates.rayachat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.teammates.rayachat.core.RayaChatConfig
import ai.teammates.rayachat.core.models.TypeMessage
import ai.teammates.rayachat.core.models.ViewMode
import ai.teammates.rayachat.ui.adapters.AudioRecorderAdapter
import ai.teammates.rayachat.ui.adapters.ImagePickerAdapter
import ai.teammates.rayachat.ui.components.commands.EndChatModal
import ai.teammates.rayachat.ui.screens.ChatScreen
import ai.teammates.rayachat.ui.screens.FormScreen
import ai.teammates.rayachat.ui.screens.IntroScreen
import ai.teammates.rayachat.ui.theme.LocalRayaTheme
import ai.teammates.rayachat.ui.theme.RayaChatTheme

/**
 * Mode 1: Full packaged chat widget as a @Composable.
 *
 * Includes IntroScreen, FormScreen, ChatScreen, and EndChatModal.
 * Handles the complete INTRO → FORM → CHAT flow.
 *
 * Usage:
 * ```kotlin
 * RayaChatWidget(token = "your-bot-token")
 * ```
 *
 * @param token Bot token from Teammates.ai dashboard.
 * @param locale Language — "en" or "ar". Default: "en".
 * @param imagePickerAdapter Optional adapter for image selection.
 * @param audioRecorderAdapter Optional adapter for voice recording.
 * @param onSessionStart Called when WebSocket session connects.
 * @param onSessionEnd Called when session ends, with the session ID and full message history.
 * @param onError Called on errors.
 * @param onClose Called when user closes the widget.
 */
@Composable
fun RayaChatWidget(
    token: String,
    locale: String = "en",
    imagePickerAdapter: ImagePickerAdapter? = null,
    audioRecorderAdapter: AudioRecorderAdapter? = null,
    onSessionStart: ((String) -> Unit)? = null,
    onSessionEnd: ((sessionId: String, messages: List<TypeMessage>) -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
    onClose: (() -> Unit)? = null,
) {
    val context = LocalContext.current

    val config = remember(token, locale) {
        RayaChatConfig(
            token = token,
            locale = locale,
            onSessionStart = onSessionStart,
            onSessionEnd = onSessionEnd,
            onError = onError,
            onClose = onClose,
        )
    }

    // ViewModel survives configuration changes (rotation, dark mode toggle, etc.)
    // ViewModelProvider stores it in ViewModelStore — same instance returned after recreation
    val viewModel: RayaChatViewModel = viewModel(
        factory = RayaChatViewModelFactory(context.applicationContext, config)
    )

    val botConfig by viewModel.botConfig.collectAsState()
    val configLoading by viewModel.configLoading.collectAsState()

    // Get status bar height (compatible with API 24+)
    val view = LocalView.current
    val statusBarHeight = remember {
        try {
            val resourceId = view.context.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                val heightPx = view.context.resources.getDimensionPixelSize(resourceId)
                (heightPx / view.context.resources.displayMetrics.density).toInt()
            } else 24 // fallback
        } catch (_: Exception) { 24 }
    }

    // Show loading while fetching config
    if (configLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Wrap in theme
    RayaChatTheme(botConfig = botConfig, locale = locale) {
        RayaChatContent(
            viewModel = viewModel,
            botConfig = botConfig,
            imagePickerAdapter = imagePickerAdapter,
            audioRecorderAdapter = audioRecorderAdapter,
            statusBarHeight = statusBarHeight,
            locale = locale,
        )
    }
}

@Composable
private fun RayaChatContent(
    viewModel: RayaChatViewModel,
    botConfig: ai.teammates.rayachat.core.models.BotConfigProps,
    imagePickerAdapter: ImagePickerAdapter?,
    audioRecorderAdapter: AudioRecorderAdapter?,
    statusBarHeight: Int,
    locale: String,
) {
    val theme = LocalRayaTheme.current
    val viewMode by viewModel.viewMode.collectAsState()
    val showEndChatModal by viewModel.showEndChatModal.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        when (viewMode) {
            ViewMode.INTRO -> {
                val sessionCloseInfo by viewModel.sessionCloseInfo.collectAsState()
                IntroScreen(
                    botConfig = botConfig,
                    sessionCloseInfo = sessionCloseInfo,
                    statusBarHeight = statusBarHeight,
                    onStartChat = viewModel::startChat,
                )
            }

            ViewMode.FORM -> {
                FormScreen(
                    botConfig = botConfig,
                    statusBarHeight = statusBarHeight,
                    onSubmit = viewModel::submitForm,
                    onBack = viewModel::goBackToIntro,
                )
            }

            ViewMode.CHAT -> {
                val messages by viewModel.messages.collectAsState()
                val currentMessage by viewModel.currentMessage.collectAsState()
                val loading by viewModel.loading.collectAsState()
                val status by viewModel.status.collectAsState()
                val info by viewModel.info.collectAsState()
                val commandData by viewModel.commandData.collectAsState()
                val presets by viewModel.presets.collectAsState()
                val isConnected by viewModel.isConnected.collectAsState()
                val showHumanAgentBtn by viewModel.showHumanAgentBtn.collectAsState()

                ChatScreen(
                    messages = messages,
                    currentMessage = currentMessage,
                    loading = loading,
                    status = status,
                    info = info,
                    commandData = commandData,
                    presets = presets,
                    botConfig = botConfig,
                    isConnected = isConnected,
                    showHumanAgentBtn = showHumanAgentBtn,
                    imagePickerAdapter = imagePickerAdapter,
                    audioRecorderAdapter = audioRecorderAdapter,
                    statusBarHeight = statusBarHeight,
                    onSendMessage = viewModel::sendMessage,
                    onSendImages = viewModel::sendImages,
                    onSendAudio = viewModel::sendAudio,
                    onSendPreset = viewModel::sendPreset,
                    onSendCommandResponse = viewModel::sendCommandResponse,
                    onEndSession = viewModel::endSessionFromCommand,
                    onClose = viewModel::closeChat,
                )

                // End chat confirmation modal
                EndChatModal(
                    visible = showEndChatModal,
                    locale = locale,
                    onCancel = viewModel::cancelEndChat,
                    onEndSession = viewModel::confirmEndSession,
                )
            }
        }
    }
}
