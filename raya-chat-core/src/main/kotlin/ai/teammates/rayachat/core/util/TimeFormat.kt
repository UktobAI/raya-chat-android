package ai.teammates.rayachat.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Formats a timestamp for display in message bubbles.
 *
 * @param isoTimestamp ISO-8601 string (e.g., "2026-03-26T10:30:00Z")
 * @param epochSeconds Unix timestamp in seconds (e.g., 1774436672)
 * @return Formatted local time string (e.g., "10:30 AM") or empty string on failure.
 */
fun formatLocalTime(isoTimestamp: String? = null, epochSeconds: Long? = null): String {
    return try {
        val date = when {
            epochSeconds != null && epochSeconds > 0 -> Date(epochSeconds * 1000)
            !isoTimestamp.isNullOrBlank() -> {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                format.timeZone = TimeZone.getTimeZone("UTC")
                format.parse(isoTimestamp.removeSuffix("Z"))
            }
            else -> return ""
        } ?: return ""

        val displayFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        displayFormat.timeZone = TimeZone.getDefault()
        displayFormat.format(date)
    } catch (_: Exception) {
        ""
    }
}
