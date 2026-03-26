package ai.teammates.rayachat.core.util

/** Returns true if the locale uses RTL (right-to-left) layout. */
fun isRTLLocale(locale: String): Boolean =
    locale.lowercase().startsWith("ar") ||
    locale.lowercase().startsWith("he") ||
    locale.lowercase().startsWith("fa") ||
    locale.lowercase().startsWith("ur")

// Unicode ranges for RTL scripts
private val RTL_CHAR_REGEX = Regex("[\\u0590-\\u05FF\\u0600-\\u06FF\\u0700-\\u074F\\uFB50-\\uFDFF\\uFE70-\\uFEFF]")

/**
 * Detects if text content is primarily RTL by checking if the first meaningful character
 * is in an RTL Unicode range. Used for per-message text direction in mixed-language chats.
 */
fun isRTLText(text: String): Boolean {
    if (text.isBlank()) return false
    // Find the first letter character
    val firstLetter = text.firstOrNull { it.isLetter() } ?: return false
    return RTL_CHAR_REGEX.containsMatchIn(firstLetter.toString())
}
