package ai.teammates.rayachat.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.teammates.rayachat.core.models.ImagePayload
import ai.teammates.rayachat.core.RayaChatClient
import ai.teammates.rayachat.core.RayaChatConfig
import ai.teammates.rayachat.core.models.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel bridging [RayaChatClient] (core) to the packaged UI.
 * Manages the INTRO → FORM → CHAT view state machine.
 */
internal class RayaChatViewModel(
    context: Context,
    config: RayaChatConfig,
) : ViewModel() {

    val client = RayaChatClient(context, config)
    private val locale = config.locale ?: "en"

    // ── Bot config ──
    private val _botConfig = MutableStateFlow(BotConfigProps())
    val botConfig: StateFlow<BotConfigProps> = _botConfig.asStateFlow()

    private val _configLoading = MutableStateFlow(true)
    val configLoading: StateFlow<Boolean> = _configLoading.asStateFlow()

    // ── View state ──
    private val _viewMode = MutableStateFlow(ViewMode.INTRO)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _showEndChatModal = MutableStateFlow(false)
    val showEndChatModal: StateFlow<Boolean> = _showEndChatModal.asStateFlow()

    // ── Delegate state from client ──
    val messages: StateFlow<List<TypeMessage>> get() = client.messages
    val currentMessage: StateFlow<String> get() = client.currentMessage
    val connectionStatus: StateFlow<ConnectionStatus> get() = client.connectionStatus
    val isConnected: StateFlow<Boolean> get() = client.isConnected
    val isOnline: StateFlow<Boolean> get() = client.isOnline
    val loading: StateFlow<Boolean> get() = client.loading
    val status: StateFlow<String?> get() = client.status
    val info: StateFlow<String?> get() = client.info
    val commandData: StateFlow<CommandData?> get() = client.commandData
    val presets: StateFlow<List<String>> get() = client.presets
    val showHumanAgentBtn: StateFlow<Boolean> get() = client.showHumanAgentBtn
    val sessionCloseInfo: StateFlow<SessionCloseInfo?> get() = client.sessionCloseInfo

    init {
        // Fetch bot config on creation
        viewModelScope.launch {
            try {
                val config = client.fetchBotConfig()
                _botConfig.value = config
            } catch (_: Exception) {
                // Use defaults
            } finally {
                _configLoading.value = false
            }
        }
    }

    // ── Actions ──

    fun startChat() {
        client.clearSessionCloseInfo()
        val config = _botConfig.value
        if (config.enableUserForm) {
            _viewMode.value = ViewMode.FORM
        } else {
            // Skip form — connect with empty user info
            viewModelScope.launch {
                client.connect(UserInfo("", "", ""), config)
                _viewMode.value = ViewMode.CHAT
            }
        }
    }

    fun submitForm(userInfo: UserInfo) {
        viewModelScope.launch {
            client.connect(userInfo, _botConfig.value)
            _viewMode.value = ViewMode.CHAT
        }
    }

    fun goBackToIntro() {
        _viewMode.value = ViewMode.INTRO
    }

    fun closeChat() {
        if (_viewMode.value == ViewMode.CHAT) {
            _showEndChatModal.value = true
        } else {
            _viewMode.value = ViewMode.INTRO
        }
    }

    fun cancelEndChat() {
        _showEndChatModal.value = false
    }

    fun confirmEndSession() {
        viewModelScope.launch {
            _showEndChatModal.value = false
            client.endSession()
            _viewMode.value = ViewMode.INTRO
        }
    }

    fun endSessionFromCommand() {
        viewModelScope.launch {
            client.endSession()
            _viewMode.value = ViewMode.INTRO
        }
    }

    fun sendMessage(text: String) = client.sendMessage(text)
    fun sendImages(images: List<ImagePayload>, caption: String = "") = client.sendImages(images, caption)
    fun sendAudio(base64: String) = client.sendAudio(base64)
    fun sendPreset(text: String) = client.sendPreset(text)
    fun sendCommandResponse(command: String, response: Any) = client.sendCommandResponse(command, response)
    fun clearSessionCloseInfo() = client.clearSessionCloseInfo()

    override fun onCleared() {
        super.onCleared()
        client.destroy()
    }
}
