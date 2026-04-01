package ai.teammates.rayachat.ui.components.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ai.teammates.rayachat.ui.components.common.RayaIcons
import ai.teammates.rayachat.ui.theme.LocalRayaTheme

@Composable
fun ScrollToBottomButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalRayaTheme.current
    val bgColor = if (theme.isDark) Color(0xFF27272A) else Color.White
    val borderColor = if (theme.isDark) Color(0xFF3F3F46) else Color(0xFFD4D4D8)
    val iconColor = if (theme.isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)
    val arrowIcon = remember(iconColor) { RayaIcons.arrowDown(iconColor) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(bgColor)
                .border(0.5.dp, borderColor, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = arrowIcon,
                contentDescription = "Scroll to bottom",
                tint = iconColor,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
