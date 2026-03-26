package ai.teammates.rayachat.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Raw inbound WebSocket message before routing by type. */
@Serializable
data class ChatMessage(
    val type: String = "",
    val text: String? = null,
    val content: String? = null,
    val data: JsonElement? = null,
    val presets: List<PresetItem>? = null,
    val options: List<JsonElement>? = null,
    val message: String? = null,
    val optional: Boolean = false,
)

@Serializable
data class PresetItem(
    val title: String = "",
)

/** Parsed from ChatMessage.data when type == "response". */
@Serializable
data class ChatResponseData(
    val id: String = "",
    @SerialName("chat_session_id") val chatSessionId: String = "",
    val sender: Int = 2,
    val content: String = "",
    @SerialName("created_at") val createdAt: Long = 0L,
    val attachments: List<String> = emptyList(),
    @SerialName("audio_urls") val audioUrls: String = "",
)
