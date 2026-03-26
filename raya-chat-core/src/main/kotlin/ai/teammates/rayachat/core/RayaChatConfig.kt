package ai.teammates.rayachat.core

/**
 * Configuration for the Raya Chat SDK.
 *
 * Only [token] is required. All other fields have sensible defaults.
 * The API endpoint is hardcoded to the production server and cannot be changed.
 *
 * @param token Bot token from the Teammates.ai dashboard.
 * @param locale Language — `"en"` (English) or `"ar"` (Arabic/RTL). Default: `"en"`.
 * @param onSessionStart Called when the WebSocket session connects, with the session ID.
 * @param onSessionEnd Called when the session ends.
 * @param onError Called on connection or send errors with an error message.
 * @param onClose Called when the user closes the chat widget.
 */
data class RayaChatConfig(
    val token: String,
    val locale: String = "en",
    val onSessionStart: ((sessionId: String) -> Unit)? = null,
    val onSessionEnd: (() -> Unit)? = null,
    val onError: ((error: String) -> Unit)? = null,
    val onClose: (() -> Unit)? = null,
)
