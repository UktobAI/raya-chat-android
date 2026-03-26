package ai.teammates.rayachat.core.util

/**
 * Determines if a hex color is "dark" using the luminance formula.
 * Used to decide foreground text color on gradient backgrounds.
 *
 * Formula: luminance = (0.299 * R + 0.587 * G + 0.114 * B) / 255
 * If luminance < 0.5 → dark color → use white foreground.
 */
fun isDarkColor(hex: String): Boolean {
    val color = parseHexColor(hex) ?: return true
    val r = (color shr 16) and 0xFF
    val g = (color shr 8) and 0xFF
    val b = color and 0xFF
    val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    return luminance < 0.5
}

/**
 * Returns the appropriate foreground color for text placed on the given gradient.
 * White (#FFFFFF) for dark gradients, dark (#1A1A1A) for light gradients.
 */
fun getContrastColor(hex: String): String =
    if (isDarkColor(hex)) "#FFFFFF" else "#1A1A1A"

/** Parse a hex color string (#RGB, #RRGGBB, or RRGGBB) into an Int, or null if invalid. */
internal fun parseHexColor(hex: String): Int? {
    val cleaned = hex.removePrefix("#")
    return when (cleaned.length) {
        3 -> {
            val r = cleaned[0].digitToIntOrNull(16) ?: return null
            val g = cleaned[1].digitToIntOrNull(16) ?: return null
            val b = cleaned[2].digitToIntOrNull(16) ?: return null
            (r * 17 shl 16) or (g * 17 shl 8) or (b * 17)
        }
        6 -> cleaned.toLongOrNull(16)?.toInt()
        else -> null
    }
}
