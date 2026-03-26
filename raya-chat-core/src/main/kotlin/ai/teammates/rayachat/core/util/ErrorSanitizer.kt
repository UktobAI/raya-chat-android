package ai.teammates.rayachat.core.util

private val HTML_TAG_REGEX = Regex("<[^>]*>")
private val JS_PROTOCOL_REGEX = Regex("javascript:", RegexOption.IGNORE_CASE)
private val EVENT_HANDLER_REGEX = Regex("on\\w+=", RegexOption.IGNORE_CASE)
private const val MAX_ERROR_LENGTH = 200
private const val DEFAULT_ERROR = "An unexpected error occurred. Please try again."

private val SENSITIVE_PATTERNS = listOf(
    "api_key", "password", "token", "secret",
    "internal server error", "stack trace",
    "exception", "traceback",
)

/**
 * Sanitizes server error messages before display.
 * Strips HTML, JavaScript, event handlers, and sensitive information.
 */
fun sanitizeErrorMessage(rawMessage: String?): String {
    if (rawMessage.isNullOrBlank()) return DEFAULT_ERROR

    var cleaned = rawMessage
        .let { HTML_TAG_REGEX.replace(it, "") }
        .let { JS_PROTOCOL_REGEX.replace(it, "") }
        .let { EVENT_HANDLER_REGEX.replace(it, "") }
        .trim()

    // Check for sensitive patterns
    val lowerCleaned = cleaned.lowercase()
    if (SENSITIVE_PATTERNS.any { lowerCleaned.contains(it) }) {
        return DEFAULT_ERROR
    }

    // Cap length
    if (cleaned.length > MAX_ERROR_LENGTH) {
        cleaned = cleaned.take(MAX_ERROR_LENGTH) + "..."
    }

    return cleaned.ifBlank { DEFAULT_ERROR }
}
