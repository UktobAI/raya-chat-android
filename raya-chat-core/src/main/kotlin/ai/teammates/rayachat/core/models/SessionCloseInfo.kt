package ai.teammates.rayachat.core.models

/** Information about a session that was closed by the server (e.g., auto_close due to inactivity). */
data class SessionCloseInfo(
    val reason: CloseReason,
    val message: String,
)
