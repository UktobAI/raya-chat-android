package ai.teammates.rayachat.core.models

/** Parsed inbound WebSocket message with type already determined. */
data class WebSocketIncomingMessage(
    val type: MessageType,
    val text: String? = null,
    val content: String? = null,
    val responseData: ChatResponseData? = null,
    val presets: List<String>? = null,
    val commandData: CommandData? = null,
)
