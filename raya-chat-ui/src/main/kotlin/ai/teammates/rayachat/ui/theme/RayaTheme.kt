package ai.teammates.rayachat.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Theme data class holding all resolved color tokens and configuration.
 * Computed from bot config (theme + gradient color).
 */
data class RayaTheme(
    val isDark: Boolean,
    val isRTL: Boolean,
    val locale: String,
    val gradientColor: Color,
    val gradientForeground: Color,
    val background: Color,
    val foreground: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val borderWarm: Color,
    val muted: Color,
    val mutedForeground: Color,
    val botBubble: Color,
    val botBubbleForeground: Color,
    val systemMessage: Color,
    val systemMessageForeground: Color,
    val introCard: Color,
    val introCardBorder: Color,
    val formCardBg: Color,
    val inputBorder: Color,
    val inputBorderFocused: Color,
    val composerBorder: Color,
    val composerBorderFocused: Color,
    val composerBg: Color,
    val sendBtnBg: Color,
    val presetBorder: Color,
    val presetText: Color,
    val presetBg: Color,
    val footerBg: Color,
    val footerText: Color,
    val onlineBadgeBg: Color,
    val onlineBadgeBorder: Color,
    val onlineDot: Color,
    val onlineText: Color,
    val errorRed: Color,
    val destructive: Color,
) {
    companion object {
        fun create(
            isDark: Boolean,
            isRTL: Boolean = false,
            locale: String = "en",
            gradientColor: Color,
            gradientForeground: Color,
        ): RayaTheme {
            return if (isDark) createDark(isRTL, locale, gradientColor, gradientForeground)
            else createLight(isRTL, locale, gradientColor, gradientForeground)
        }

        private fun createLight(isRTL: Boolean, locale: String, gradientColor: Color, gradientForeground: Color) = RayaTheme(
            isDark = false, isRTL = isRTL, locale = locale, gradientColor = gradientColor, gradientForeground = gradientForeground,
            background = LightColors.background, foreground = LightColors.foreground,
            surface = LightColors.surface, surfaceVariant = LightColors.surfaceVariant,
            border = LightColors.border, borderWarm = LightColors.borderWarm,
            muted = LightColors.muted, mutedForeground = LightColors.mutedForeground,
            botBubble = LightColors.botBubble, botBubbleForeground = LightColors.botBubbleForeground,
            systemMessage = LightColors.systemMessage, systemMessageForeground = LightColors.systemMessageForeground,
            introCard = LightColors.introCard, introCardBorder = LightColors.introCardBorder,
            formCardBg = LightColors.formCardBg, inputBorder = LightColors.inputBorder, inputBorderFocused = LightColors.inputBorderFocused,
            composerBorder = LightColors.composerBorder, composerBorderFocused = LightColors.composerBorderFocused,
            composerBg = LightColors.composerBg, sendBtnBg = LightColors.sendBtnBg,
            presetBorder = LightColors.presetBorder, presetText = LightColors.presetText, presetBg = LightColors.presetBg,
            footerBg = LightColors.footerBg, footerText = LightColors.footerText,
            onlineBadgeBg = LightColors.onlineBadgeBg, onlineBadgeBorder = LightColors.onlineBadgeBorder,
            onlineDot = LightColors.onlineDot, onlineText = LightColors.onlineText,
            errorRed = LightColors.errorRed, destructive = LightColors.destructive,
        )

        private fun createDark(isRTL: Boolean, locale: String, gradientColor: Color, gradientForeground: Color) = RayaTheme(
            isDark = true, isRTL = isRTL, locale = locale, gradientColor = gradientColor, gradientForeground = gradientForeground,
            background = DarkColors.background, foreground = DarkColors.foreground,
            surface = DarkColors.surface, surfaceVariant = DarkColors.surfaceVariant,
            border = DarkColors.border, borderWarm = DarkColors.borderWarm,
            muted = DarkColors.muted, mutedForeground = DarkColors.mutedForeground,
            botBubble = DarkColors.botBubble, botBubbleForeground = DarkColors.botBubbleForeground,
            systemMessage = DarkColors.systemMessage, systemMessageForeground = DarkColors.systemMessageForeground,
            introCard = DarkColors.introCard, introCardBorder = DarkColors.introCardBorder,
            formCardBg = DarkColors.formCardBg, inputBorder = DarkColors.inputBorder, inputBorderFocused = DarkColors.inputBorderFocused,
            composerBorder = DarkColors.composerBorder, composerBorderFocused = DarkColors.composerBorderFocused,
            composerBg = DarkColors.composerBg, sendBtnBg = DarkColors.sendBtnBg,
            presetBorder = DarkColors.presetBorder, presetText = DarkColors.presetText, presetBg = DarkColors.presetBg,
            footerBg = DarkColors.footerBg, footerText = DarkColors.footerText,
            onlineBadgeBg = DarkColors.onlineBadgeBg, onlineBadgeBorder = DarkColors.onlineBadgeBorder,
            onlineDot = DarkColors.onlineDot, onlineText = DarkColors.onlineText,
            errorRed = DarkColors.errorRed, destructive = DarkColors.destructive,
        )
    }
}
