package ai.teammates.rayachat.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import ai.teammates.rayachat.core.models.BotConfigProps
import ai.teammates.rayachat.core.util.isDarkColor
import ai.teammates.rayachat.core.util.isRTLLocale
import ai.teammates.rayachat.core.util.parseHexColor

/** CompositionLocal to access the current Raya theme from any composable. */
val LocalRayaTheme = compositionLocalOf<RayaTheme> {
    error("No RayaTheme provided. Wrap your composable in RayaChatTheme { ... }")
}

/**
 * Provides the Raya theme to all child composables.
 *
 * Computes colors from [botConfig] (theme mode + gradient color) and [locale].
 */
@Composable
fun RayaChatTheme(
    botConfig: BotConfigProps,
    locale: String = "en",
    content: @Composable () -> Unit,
) {
    val isDark = botConfig.theme == "dark"
    val isRTL = isRTLLocale(locale)

    val gradientHex = botConfig.chatboxGradientColor?.ifBlank { null } ?: "#0047AF"
    val gradientColor = hexToColor(gradientHex)
    val isGradientDark = isDarkColor(gradientHex)
    val gradientForeground = if (isGradientDark) Color.White else Color(0xFF1A1A1A)

    val theme = RayaTheme.create(
        isDark = isDark,
        isRTL = isRTL,
        locale = locale,
        gradientColor = gradientColor,
        gradientForeground = gradientForeground,
    )

    CompositionLocalProvider(LocalRayaTheme provides theme) {
        content()
    }
}

/** Convert hex string to Compose Color. Falls back to blue. */
internal fun hexToColor(hex: String): Color {
    val parsed = parseHexColor(hex) ?: return Color(0xFF0047AF)
    return Color(0xFF000000.toInt() or parsed)
}
