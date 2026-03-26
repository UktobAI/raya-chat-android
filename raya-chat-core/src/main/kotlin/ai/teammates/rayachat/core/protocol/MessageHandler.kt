package ai.teammates.rayachat.core.protocol

import ai.teammates.rayachat.core.Constants
import ai.teammates.rayachat.core.models.*
import ai.teammates.rayachat.core.util.sanitizeErrorMessage
import kotlinx.serialization.json.*

/**
 * Routes incoming WebSocket messages by type to the appropriate callback.
 * Handles all 10 inbound message types defined in NATIVE_SDK_SPEC.md.
 */
class MessageHandler(
    private val callbacks: MessageHandlerCallbacks,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Parse and route an incoming WebSocket text message.
     * Invalid JSON is silently discarded (per spec: edge case handling).
     */
    fun handle(rawText: String) {
        val parsed = try {
            json.decodeFromString<ChatMessage>(rawText)
        } catch (_: Exception) {
            return // Invalid JSON — silently discard
        }

        val type = MessageType.fromValue(parsed.type) ?: return

        when (type) {
            MessageType.STEP -> handleStep(parsed)
            MessageType.CHUNK -> handleChunk(parsed)
            MessageType.RESPONSE -> handleResponse(parsed)
            MessageType.PRESETS -> handlePresets(parsed)
            MessageType.COMMAND -> handleCommand(parsed)
            MessageType.ERROR -> handleError(parsed)
            MessageType.ESCALATION -> handleEscalation()
            MessageType.INFO -> handleInfo(parsed)
            MessageType.AGENT_ACTIVITY -> handleAgentActivity(parsed)
            MessageType.MESSAGE -> handleDirectMessage(parsed)
        }
    }

    // ── STEP ──

    private fun handleStep(msg: ChatMessage) {
        callbacks.onLoading(true, msg.text)
    }

    // ── CHUNK ──

    private fun handleChunk(msg: ChatMessage) {
        val text = msg.text ?: return
        callbacks.onLoading(true, null)
        callbacks.onChunk(text)
    }

    // ── RESPONSE ──

    private fun handleResponse(msg: ChatMessage) {
        val dataElement = msg.data ?: return

        val responseData = try {
            json.decodeFromJsonElement<ChatResponseData>(dataElement)
        } catch (_: Exception) {
            return
        }

        // 1. Save session ID
        if (responseData.chatSessionId.isNotBlank()) {
            callbacks.onSessionUpdate(responseData.chatSessionId)
        }

        // 2. Handle attachments — storage only (CRITICAL: don't replace UI state)
        if (responseData.attachments.isNotEmpty()) {
            callbacks.onAttachments(responseData.attachments, "image")
        }
        if (responseData.audioUrls.isNotBlank()) {
            callbacks.onAttachments(listOf(responseData.audioUrls), "audio")
        }

        // 3. Add bot message
        val message = TypeMessage(
            id = responseData.id.ifBlank { "response-${System.currentTimeMillis()}" },
            sender = responseData.sender,
            type = 1,
            content = responseData.content,
            createdAt = responseData.createdAt.toString(),
        )
        callbacks.onChatMessage(message)

        // 4. Clear streaming buffer
        callbacks.onClearCurrentMessage()

        // 5. Set loading false
        callbacks.onLoading(false, null)
    }

    // ── PRESETS ──

    private fun handlePresets(msg: ChatMessage) {
        val titles = msg.presets?.map { it.title }?.filter { it.isNotBlank() } ?: emptyList()
        callbacks.onPresets(titles)
    }

    // ── COMMAND ──

    private fun handleCommand(msg: ChatMessage) {
        val commandContent = msg.content ?: return

        // auto_close is special — no UI, just close
        if (commandContent == "auto_close") {
            callbacks.onPresets(emptyList()) // Clear presets
            callbacks.onAutoClose(
                SessionCloseInfo(
                    reason = CloseReason.AUTO_CLOSE,
                    message = msg.message ?: "Session closed due to inactivity"
                )
            )
            return
        }

        // Validate command type
        if (commandContent !in Constants.VALID_COMMANDS) return

        // Clear presets on command
        callbacks.onPresets(emptyList())

        val options = msg.options?.map { element ->
            when (element) {
                is JsonPrimitive -> {
                    when {
                        element.isString -> element.content
                        element.intOrNull != null -> element.int
                        else -> element.content
                    }
                }
                else -> element.toString()
            }
        } ?: emptyList()

        callbacks.onCommand(
            CommandData(
                content = commandContent,
                options = options,
                message = msg.message ?: "",
                optional = msg.optional,
            )
        )
    }

    // ── ERROR ──

    private fun handleError(msg: ChatMessage) {
        val sanitized = sanitizeErrorMessage(msg.text)
        callbacks.onError(sanitized)
    }

    // ── ESCALATION ──

    private fun handleEscalation() {
        callbacks.onEscalation()
    }

    // ── INFO ──

    private fun handleInfo(msg: ChatMessage) {
        val text = msg.text
        callbacks.onInfo(text)

        // If info mentions "human agent" — hide escalation button
        if (text != null && text.lowercase().contains("human agent")) {
            callbacks.onHideEscalation()
        }
    }

    // ── AGENT_ACTIVITY ──

    private fun handleAgentActivity(msg: ChatMessage) {
        val content = msg.text ?: return
        val systemMsg = TypeMessage(
            id = "system-${System.currentTimeMillis()}",
            sender = 0,
            type = 4,
            content = content,
            createdAt = System.currentTimeMillis().div(1000).toString(),
        )
        callbacks.onSystemMessage(systemMsg)
    }

    // ── MESSAGE (direct) ──

    private fun handleDirectMessage(msg: ChatMessage) {
        val content = msg.content ?: return
        val botMsg = TypeMessage(
            id = "direct-${System.currentTimeMillis()}",
            sender = 2,
            type = 1,
            content = content,
            createdAt = System.currentTimeMillis().div(1000).toString(),
        )
        callbacks.onChatMessage(botMsg)
        callbacks.onLoading(false, null)
    }
}
