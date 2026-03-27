package ai.teammates.rayachat.core

/** SDK-wide constants matching the React Native SDK v0.1.0. */
object Constants {
    const val SDK_VERSION = "0.1.0"
    const val DEFAULT_ENDPOINT = "api.dev.workforce.uktob.ai" // TODO: Change to api.workforce.uktob.ai before publishing
    const val INTEGRATION_TYPE = "widget"
    const val DEFAULT_BOT_AVATAR = "https://app.teammates.ai/api/assets/images/New_raya_agent.png"
    const val ASSET_BASE_URL = "https://app.teammates.ai/api/assets"

    // WebSocket
    const val HEARTBEAT_INTERVAL_MS = 25_000L
    const val HEARTBEAT_TIMEOUT_MS = 60_000L
    const val MAX_RECONNECT_ATTEMPTS = 100
    const val MAX_RECONNECT_DELAY_MS = 30_000L
    const val BASE_RECONNECT_DELAY_MS = 1_000L

    // WebSocket close codes
    const val WS_CLOSE_NORMAL = 1000
    const val WS_CLOSE_GOING_AWAY = 1001
    const val WS_CLOSE_ABNORMAL = 1006

    // Storage
    const val MAX_MESSAGES_IN_MEMORY = 500
    const val PREFS_NAME = "raya_chat_prefs"
    const val KEY_SESSION_ID = "session_id"
    const val KEY_USER_INFO = "user_info"
    const val DB_NAME = "raya_chat_db"

    // Chat
    const val MAX_IMAGES_PER_MESSAGE = 5
    const val MAX_FEEDBACK_LENGTH = 200
    const val FEEDBACK_COUNTDOWN_SECONDS = 3
    const val STALE_STATE_THRESHOLD_MS = 60_000L

    // Valid command types from server
    val VALID_COMMANDS = setOf(
        "end_session",
        "rate_conversation",
        "submit_feedback",
        "feedback_received",
        "auto_close"
    )
}
