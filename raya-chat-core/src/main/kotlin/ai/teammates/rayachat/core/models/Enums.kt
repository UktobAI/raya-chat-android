package ai.teammates.rayachat.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Inbound WebSocket message types from the server. */
@Serializable
enum class MessageType(val value: String) {
    @SerialName("step") STEP("step"),
    @SerialName("chunk") CHUNK("chunk"),
    @SerialName("response") RESPONSE("response"),
    @SerialName("presets") PRESETS("presets"),
    @SerialName("command") COMMAND("command"),
    @SerialName("error") ERROR("error"),
    @SerialName("human-rep-escalation") ESCALATION("human-rep-escalation"),
    @SerialName("info") INFO("info"),
    @SerialName("agent_activity") AGENT_ACTIVITY("agent_activity"),
    @SerialName("message") MESSAGE("message");

    companion object {
        fun fromValue(value: String): MessageType? =
            entries.find { it.value == value }
    }
}

/** WebSocket connection status. */
enum class ConnectionStatus {
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    RECONNECTING;
}

/** UI view mode / screen state. */
enum class ViewMode {
    INTRO,
    FORM,
    CHAT;
}

/** Reason a session was closed. */
enum class CloseReason {
    USER_ENDED,
    AUTO_CLOSE,
    SERVER_CLOSED;
}
