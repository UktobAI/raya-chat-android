package ai.teammates.rayachat.ui.theme

import androidx.compose.ui.graphics.Color

/** Light mode color tokens — exact hex values from the RN SDK. */
object LightColors {
    val background = Color(0xFFFFFFFF)
    val foreground = Color(0xFF14161A)
    val surface = Color(0xFFFFFFFF)
    val surfaceVariant = Color(0xFFF5F5F5)
    val border = Color(0xFFE4E4E7)        // zinc-200
    val borderWarm = Color(0xFFD4D4D8)    // zinc-300
    val muted = Color(0xFFF4F4F5)         // zinc-100
    val mutedForeground = Color(0xFF71717A) // zinc-500
    val botBubble = Color(0xFFF5F5F5)
    val botBubbleForeground = Color(0xFF14161A)
    val systemMessage = Color(0xFFF5F5F5)
    val systemMessageForeground = Color(0xFF14161A)
    val introCard = Color(0xFFFFFFFF)
    val introCardBorder = Color(0xFFE4E4E7)
    val formCardBg = Color(0xFFFFFFFF)
    val inputBorder = Color(0xFFE4E4E7)
    val inputBorderFocused = Color(0xFF3F3F46)  // zinc-700
    val composerBorder = Color(0xFFF4F4F5)      // zinc-100
    val composerBorderFocused = Color(0xFF3F3F46)
    val composerBg = Color(0xFFFFFFFF)
    val sendBtnBg = Color(0xFFF3F4F6)           // gray-100
    val presetBorder = Color(0xFFE4E4E7)
    val presetText = Color(0xFF71717A)
    val presetBg = Color.Transparent
    val footerBg = Color(0xFFF3F4F6)
    val footerText = Color(0xFF71717A)
    val onlineBadgeBg = Color(0xFFECFDF5)       // emerald-50
    val onlineBadgeBorder = Color(0xFFA7F3D0)   // emerald-200
    val onlineDot = Color(0xFF10B981)            // emerald-500
    val onlineText = Color(0xFF047857)           // emerald-700
    val errorRed = Color(0xFFEF4444)
    val destructive = Color(0xFFDC2626)
}

/** Dark mode color tokens — exact hex values from the RN SDK. */
object DarkColors {
    val background = Color(0xFF14161A)
    val foreground = Color(0xFFFAFAFA)
    val surface = Color(0xFF2C2D31)
    val surfaceVariant = Color(0xFF2C2D31)
    val border = Color(0xFF3F3F46)        // zinc-700
    val borderWarm = Color(0xFF52525B)    // zinc-600
    val muted = Color(0xFF27272A)
    val mutedForeground = Color(0xFFA1A1AA)
    val botBubble = Color(0xFF2C2D31)
    val botBubbleForeground = Color(0xFFF1F1F0)
    val systemMessage = Color(0xFF2C2D31)
    val systemMessageForeground = Color(0xFFF1F1F0)
    val introCard = Color(0xFF1B1C20)
    val introCardBorder = Color(0xFF3F3F46)
    val formCardBg = Color(0xFF2C2D31)
    val inputBorder = Color(0xFF3F3F46)
    val inputBorderFocused = Color(0xFF71717A)  // zinc-500
    val composerBorder = Color(0xFF3F3F46)
    val composerBorderFocused = Color(0xFF71717A)
    val composerBg = Color(0xFF2C2D31)
    val sendBtnBg = Color(0x1AF3F4F6)           // gray-100 at 10%
    val presetBorder = Color(0xFF3F3F46)
    val presetText = Color.White
    val presetBg = Color(0xFF2C2D31)
    val footerBg = Color(0xFF3F3F46)
    val footerText = Color.White
    val onlineBadgeBg = Color(0x2610B981)       // emerald-500 at 15%
    val onlineBadgeBorder = Color(0x4D34D399)   // emerald-400 at 30%
    val onlineDot = Color(0xFF10B981)
    val onlineText = Color(0xFFA7F3D0)          // emerald-200
    val errorRed = Color(0xFFEF4444)
    val destructive = Color(0xFFDC2626)
}

/** Icon default color. */
val IconDefault = Color(0xFFA1A1AA)  // zinc-400
