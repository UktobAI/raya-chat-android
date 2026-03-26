package ai.teammates.rayachat.core.models

/** Image selected by the user for upload. */
data class ImageAsset(
    val uri: String,
    val name: String,
    val type: String,
    val base64: String = "",
)
