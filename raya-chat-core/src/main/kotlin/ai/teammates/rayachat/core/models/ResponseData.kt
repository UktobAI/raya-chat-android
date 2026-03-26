package ai.teammates.rayachat.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Payload inside a RESPONSE message from the server. */
@Serializable
data class ResponseData(
    val id: String = "",
    @SerialName("chat_session_id") val chatSessionId: String = "",
    val sender: Int = 2,
    val content: String = "",
    @SerialName("created_at") val createdAt: Long = 0L,
    val attachments: List<String> = emptyList(),
    @SerialName("audio_urls") val audioUrls: String = "",
    @SerialName("chart_data") val chartData: String = "",
)
