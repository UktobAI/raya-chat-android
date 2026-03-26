package ai.teammates.rayachat.core.models

import kotlinx.serialization.Serializable

@Serializable
data class Attachment(
    val id: String = "",
    val url: String = "",
    val type: String = "image",
    val name: String = "",
)
