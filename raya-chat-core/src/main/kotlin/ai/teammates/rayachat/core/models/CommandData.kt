package ai.teammates.rayachat.core.models

/** Active server command that requires user interaction. */
data class CommandData(
    val content: String,
    val options: List<Any> = emptyList(),
    val message: String = "",
    val optional: Boolean = false,
)
