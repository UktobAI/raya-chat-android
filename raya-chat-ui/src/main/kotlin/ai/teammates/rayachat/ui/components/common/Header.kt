package ai.teammates.rayachat.ui.components.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import androidx.compose.runtime.remember
import ai.teammates.rayachat.core.Constants
import ai.teammates.rayachat.ui.theme.LocalRayaTheme

/**
 * Gradient header with bot icon and action buttons.
 * Used on Form and Chat screens.
 */
@Composable
fun Header(
    botIcon: String?,
    showBackButton: Boolean = false,
    showCloseButton: Boolean = true,
    showBotIcon: Boolean = true,
    onBack: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
) {
    val theme = LocalRayaTheme.current
    val iconColor = theme.gradientForeground
    val avatarUrl = botIcon?.ifBlank { null }  // null = don't show avatar

    // Cache icons — only rebuilt when color changes
    val chevronIcon = remember(iconColor) { RayaIcons.chevronLeft(iconColor, 2f) }
    val closeIcon = remember(iconColor) { RayaIcons.close(iconColor, 2f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.gradientColor)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left: back button + bot icon
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showBackButton && onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = chevronIcon,
                            contentDescription = "Back",
                            tint = iconColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                // Bot icon with white container — hidden if no icon URL
                if (showBotIcon && avatarUrl != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(avatarUrl),
                            contentDescription = "Bot",
                            modifier = Modifier.size(30.dp).clip(CircleShape),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }

            // Right: close button
            if (showCloseButton && onClose != null) {
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = closeIcon,
                        contentDescription = "Close",
                        tint = iconColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
