package ai.teammates.rayachat.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AudioData(
    val type: String = "",
    @SerialName("audio_urls") val audioUrls: String = "",
)
