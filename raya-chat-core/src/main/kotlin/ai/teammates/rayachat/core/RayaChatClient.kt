package ai.teammates.rayachat.core

import android.content.Context
import ai.teammates.rayachat.core.api.ApiClient
import ai.teammates.rayachat.core.lifecycle.AppLifecycleObserver
import ai.teammates.rayachat.core.models.*
import ai.teammates.rayachat.core.network.NetworkMonitor
import ai.teammates.rayachat.core.protocol.MessageHandler
import ai.teammates.rayachat.core.protocol.MessageHandlerCallbacks
import ai.teammates.rayachat.core.storage.ChatDatabase
import ai.teammates.rayachat.core.storage.PreferenceStorage
import ai.teammates.rayachat.core.websocket.WebSocketManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json

/**
 * Main entry point for the Raya Chat SDK (headless mode).
 *
 * Manages WebSocket connection, message state, persistence, and all chat logic.
 * Exposes all state as [StateFlow] and all actions as functions.
 *
 * Usage:
 * ```kotlin
 * val client = RayaChatClient(context, RayaChatConfig(token = "..."))
 * client.connect(UserInfo("John", "john@test.com", ""))
 *
 * lifecycleScope.launch {
 *     client.messages.collect { msgs -> /* render */ }
 * }
 *
 * client.sendMessage("Hello")
 * ```
 */
class RayaChatClient(
    context: Context,
    private val config: RayaChatConfig,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // ── Internal components ──
    private val apiClient = ApiClient(config.token, config.locale ?: "en")
    private val prefStorage = PreferenceStorage(appContext)
    private val database = ChatDatabase.getInstance(appContext)
    private val messageDao = database.messageDao()
    private val networkMonitor = NetworkMonitor(appContext)

    private var wsManager: WebSocketManager? = null
    private var messageHandler: MessageHandler? = null
    private var lifecycleObserver: AppLifecycleObserver? = null
    private var sessionId: String = ""
    private var currentUserInfo: UserInfo = UserInfo()

    /** Current session ID — empty before first server response. */
    private val _currentSessionId = MutableStateFlow("")
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    // ── Public State ──

    private val _messages = MutableStateFlow<List<TypeMessage>>(emptyList())
    val messages: StateFlow<List<TypeMessage>> = _messages.asStateFlow()

    private val _currentMessage = MutableStateFlow("")
    val currentMessage: StateFlow<String> = _currentMessage.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    private val _info = MutableStateFlow<String?>(null)
    val info: StateFlow<String?> = _info.asStateFlow()

    private val _commandData = MutableStateFlow<CommandData?>(null)
    val commandData: StateFlow<CommandData?> = _commandData.asStateFlow()

    private val _presets = MutableStateFlow<List<String>>(emptyList())
    val presets: StateFlow<List<String>> = _presets.asStateFlow()

    private val _showHumanAgentBtn = MutableStateFlow(false)
    val showHumanAgentBtn: StateFlow<Boolean> = _showHumanAgentBtn.asStateFlow()

    private val _sessionCloseInfo = MutableStateFlow<SessionCloseInfo?>(null)
    val sessionCloseInfo: StateFlow<SessionCloseInfo?> = _sessionCloseInfo.asStateFlow()

    // ── Actions ──

    /**
     * Connect to the chat server.
     * Restores session from storage if available, otherwise starts fresh.
     *
     * @param userInfo User information from the form (or empty for anonymous).
     * @param botConfig Bot configuration — pass this so the initial bot message appears.
     */
    suspend fun connect(userInfo: UserInfo, botConfig: BotConfigProps? = null) {
        try {
        connectInternal(userInfo, botConfig)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "connect() failed: ${e.message}")
            config.onError?.invoke("Connection failed: ${e.message}")
        }
    }

    private suspend fun connectInternal(userInfo: UserInfo, botConfig: BotConfigProps?) {
        // Store userInfo for URL reconstruction on reconnect
        currentUserInfo = userInfo

        // Restore session ID + messages + save user info — ALL on IO thread
        // EncryptedSharedPreferences does AES crypto synchronously, so must be off Main
        val (restoredSessionId, storedMessages) = withContext(Dispatchers.IO) {
            val sid = prefStorage.getSessionId()
            val msgs = messageDao.getAll()
            prefStorage.setUserInfo(userInfo)
            Pair(sid, msgs)
        }
        sessionId = restoredSessionId
        _currentSessionId.value = restoredSessionId
        _messages.value = storedMessages

        // Add initial bot message if no stored messages
        if (storedMessages.isEmpty() && botConfig != null) {
            val initialMsg = botConfig.chatboxInitialMsg
            if (initialMsg.isNotBlank()) {
                val welcomeMsg = TypeMessage(
                    id = "initial-${System.currentTimeMillis()}",
                    sender = 2,
                    type = 1,
                    content = initialMsg,
                    createdAt = (System.currentTimeMillis() / 1000).toString(),
                )
                _messages.value = listOf(welcomeMsg)
                withContext(Dispatchers.IO) {
                    messageDao.insert(welcomeMsg)
                }
            }
        }

        // Construct WebSocket URL
        val url = apiClient.constructWebSocketUrl(sessionId, userInfo)

        // Create message handler
        messageHandler = MessageHandler(createMessageCallbacks())

        // Create WebSocket manager
        wsManager = WebSocketManager(
            url = url,
            callbacks = createWebSocketCallbacks(),
            scope = scope,
        )

        // Start network monitor
        networkMonitor.start()

        // Start lifecycle observer
        lifecycleObserver = AppLifecycleObserver(
            onForeground = { duration ->
                if (duration > Constants.STALE_STATE_THRESHOLD_MS) {
                    // Clear stale streaming state
                    _currentMessage.value = ""
                    _loading.value = false
                    _status.value = null
                }
                wsManager?.setAppActive(true)
            },
            onBackground = {
                wsManager?.setAppActive(false)
            }
        )
        lifecycleObserver?.register()

        // Connect
        wsManager?.connect()
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        _presets.value = emptyList()

        val msg = TypeMessage(
            id = "local-${System.currentTimeMillis()}-${randomSuffix()}",
            sender = 1,
            type = 1,
            content = text.trim(),
            createdAt = (System.currentTimeMillis() / 1000).toString(),
        )

        addMessageToState(msg)

        val payload = json.encodeToString(
            OutboundMessage.serializer(),
            OutboundMessage(content = text.trim(), images = emptyList())
        )

        val sent = wsManager?.send(payload) ?: false
        if (!sent) {
            config.onError?.invoke("Message queued — reconnecting...")
        }
    }

    fun sendImages(images: List<ImagePayload>, caption: String = "") {
        _presets.value = emptyList()

        val ts = System.currentTimeMillis()

        // Attachment JSON for local display — uses URIs (small), runs on Main (fast)
        val attachmentsJson = json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(Attachment.serializer()),
            images.mapIndexed { idx, img ->
                Attachment(
                    id = "local-att-$ts-$idx",
                    url = img.uri.ifBlank { img.base64 },
                    type = "image",
                    name = img.name,
                )
            }
        )

        val msg = TypeMessage(
            id = "local-img-$ts-${randomSuffix()}",
            sender = 1,
            type = 3,
            content = caption,
            createdAt = (ts / 1000).toString(),
            attachmentsJson = attachmentsJson,
        )

        // Add to UI state immediately (Main thread) — user sees images instantly
        addMessageToState(msg)

        // Heavy JSON serialization (base64 payloads) on background thread to avoid UI jank
        scope.launch(Dispatchers.Default) {
            val payload = json.encodeToString(
                OutboundMessage.serializer(),
                OutboundMessage(
                    content = caption,
                    images = images.map {
                        OutboundImage(name = it.name, type = it.type, data = it.base64)
                    }
                )
            )

            // Send on Main (WebSocket send is fast — just queues bytes)
            withContext(Dispatchers.Main) {
                val sent = wsManager?.send(payload) ?: false
                if (!sent) {
                    config.onError?.invoke("Message queued — reconnecting...")
                }
            }
        }
    }

    fun sendAudio(base64: String) {
        _presets.value = emptyList()

        val msg = TypeMessage(
            id = "local-audio-${System.currentTimeMillis()}",
            sender = 1,
            type = 2,
            content = "",
            createdAt = (System.currentTimeMillis() / 1000).toString(),
            audioJson = json.encodeToString(AudioData.serializer(), AudioData(type = "local", audioUrls = base64)),
        )

        addMessageToState(msg)
        wsManager?.send(base64)
    }

    fun sendPreset(text: String) {
        _presets.value = emptyList()
        sendMessage(text)
    }

    fun sendCommandResponse(command: String, response: Any) {
        _commandData.value = null

        val payload = json.encodeToString(
            OutboundCommandResponse.serializer(),
            OutboundCommandResponse(
                type = "command_response",
                command = command,
                response = response.toString(),
            )
        )
        wsManager?.send(payload)
    }

    fun clearSessionCloseInfo() {
        _sessionCloseInfo.value = null
    }

    suspend fun endSession() {
        wsManager?.destroy()
        wsManager = null

        lifecycleObserver?.unregister()
        lifecycleObserver = null

        networkMonitor.stop()

        sessionId = ""
        _currentSessionId.value = ""
        currentUserInfo = UserInfo()

        // Clear all state
        _messages.value = emptyList()
        _currentMessage.value = ""
        _loading.value = false
        _status.value = null
        _info.value = null
        _commandData.value = null
        _presets.value = emptyList()
        _showHumanAgentBtn.value = false
        _isConnected.value = false
        _connectionStatus.value = ConnectionStatus.DISCONNECTED

        // Clear storage
        withContext(Dispatchers.IO) {
            try {
                messageDao.deleteAll()
                prefStorage.clearAll()
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Failed to clear storage", e)
            }
        }

        config.onSessionEnd?.invoke()
    }

    fun destroy() {
        wsManager?.destroy()
        wsManager = null
        lifecycleObserver?.unregister()
        lifecycleObserver = null
        networkMonitor.stop()
        scope.cancel()
    }

    /**
     * Fetch bot configuration from the server.
     * Returns defaults on failure.
     */
    suspend fun fetchBotConfig(): BotConfigProps {
        return apiClient.fetchBotConfig()
    }

    // ── Private: Message state management ──

    private fun addMessageToState(msg: TypeMessage) {
        val updated = _messages.value + msg
        val trimmed = if (updated.size > Constants.MAX_MESSAGES_IN_MEMORY) {
            updated.drop(updated.size - Constants.MAX_MESSAGES_IN_MEMORY)
        } else {
            updated
        }
        _messages.value = trimmed

        // Persist in background
        scope.launch(Dispatchers.IO) {
            try {
                messageDao.insert(msg)
                if (messageDao.count() > Constants.MAX_MESSAGES_IN_MEMORY) {
                    messageDao.trimToLatest(Constants.MAX_MESSAGES_IN_MEMORY)
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Failed to persist message", e)
            }
        }
    }

    // ── Private: Callback factories ──

    private fun createWebSocketCallbacks() = object : WebSocketManager.WebSocketCallbacks {
        override fun onOpen() {
            _isConnected.value = true
            config.onSessionStart?.invoke(sessionId)
        }

        override fun onMessage(text: String) {
            messageHandler?.handle(text)
        }

        override fun onClose(code: Int, reason: String) {
            _isConnected.value = false
        }

        override fun onError(error: String) {
            config.onError?.invoke(error)
        }

        override fun onStatusChange(status: ConnectionStatus) {
            _connectionStatus.value = status
            _isConnected.value = (status == ConnectionStatus.CONNECTED)
        }
    }

    private fun createMessageCallbacks() = object : MessageHandlerCallbacks {
        override fun onLoading(isLoading: Boolean, status: String?) {
            _loading.value = isLoading
            if (status != null) _status.value = status
            if (!isLoading) _status.value = null
        }

        override fun onChunk(text: String) {
            _currentMessage.value += text
        }

        override fun onChatMessage(message: TypeMessage) {
            addMessageToState(message)
        }

        override fun onClearCurrentMessage() {
            _currentMessage.value = ""
        }

        override fun onSessionUpdate(sessionId: String) {
            this@RayaChatClient.sessionId = sessionId
            _currentSessionId.value = sessionId
            // Persist on IO — EncryptedSharedPreferences encrypts synchronously
            scope.launch(Dispatchers.IO) {
                prefStorage.setSessionId(sessionId)
            }
            // Update WebSocket URL so reconnection uses the correct session ID
            val newUrl = apiClient.constructWebSocketUrl(sessionId, currentUserInfo)
            wsManager?.updateUrl(newUrl)
            android.util.Log.d(TAG, "Session ID updated: $sessionId — WS URL refreshed")
        }

        override fun onAttachments(attachments: List<String>, type: String) {
            // Update STORAGE only — not UI state (per NATIVE_SDK_SPEC.md)
            scope.launch(Dispatchers.IO) {
                try {
                    val lastMsg = messageDao.getLastMessage() ?: return@launch

                    if (type == "image" && attachments.isNotEmpty()) {
                        val atts = attachments.map { url ->
                            Attachment(id = "", url = url, type = "image", name = "")
                        }
                        val updated = lastMsg.copy(
                            attachmentsJson = json.encodeToString(
                                kotlinx.serialization.builtins.ListSerializer(Attachment.serializer()),
                                atts
                            )
                        )
                        messageDao.insert(updated)
                    } else if (type == "audio" && attachments.isNotEmpty()) {
                        val updated = lastMsg.copy(
                            audioJson = json.encodeToString(
                                AudioData.serializer(),
                                AudioData(type = "remote", audioUrls = attachments.first())
                            )
                        )
                        messageDao.insert(updated)
                    }
                } catch (_: Exception) {
                    // Storage update failure is non-critical
                }
            }
        }

        override fun onPresets(presets: List<String>) {
            _presets.value = presets
        }

        override fun onCommand(commandData: CommandData) {
            _commandData.value = commandData
        }

        override fun onError(message: String) {
            config.onError?.invoke(message)
        }

        override fun onEscalation() {
            _showHumanAgentBtn.value = true
        }

        override fun onInfo(text: String?) {
            _info.value = text
        }

        override fun onHideEscalation() {
            _showHumanAgentBtn.value = false
        }

        override fun onSystemMessage(message: TypeMessage) {
            addMessageToState(message)
        }

        override fun onAutoClose(closeInfo: SessionCloseInfo) {
            _sessionCloseInfo.value = closeInfo
            _presets.value = emptyList()
            _currentMessage.value = ""
            _loading.value = false
            _status.value = null
            _info.value = null
            _commandData.value = null
            wsManager?.preventReconnect()
        }
    }

    private fun randomSuffix(): String =
        java.util.UUID.randomUUID().toString().take(8)

    companion object {
        private const val TAG = "RayaChatClient"
    }
}

// Outbound models moved to ai.teammates.rayachat.core.models.OutboundModels
