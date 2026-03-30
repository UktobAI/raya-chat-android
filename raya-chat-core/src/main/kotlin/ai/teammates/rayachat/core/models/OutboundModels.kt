package ai.teammates.rayachat.core.models

import kotlinx.serialization.Serializable

/** Outbound text/image message sent via WebSocket. */
@Serializable
data class OutboundMessage(
    val content: String,
    val images: List<OutboundImage>,
)

/** Single image in an outbound message. */
@Serializable
data class OutboundImage(
    val name: String,
    val type: String,
    val data: String,
)

/** Outbound command response (e.g., rating, feedback). */
@Serializable
data class OutboundCommandResponse(
    val type: String,
    val command: String,
    val response: String,
)

/** Payload for sending images — passed by the host app. */
data class ImagePayload(
    val name: String,
    val type: String,
    val base64: String,
    val uri: String = "",
)
