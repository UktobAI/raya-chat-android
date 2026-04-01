package ai.teammates.rayachat.ui.components.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import ai.teammates.rayachat.core.Constants
import ai.teammates.rayachat.ui.theme.IconDefault
import ai.teammates.rayachat.ui.theme.LocalRayaTheme

/** 3 animated bouncing dots in a bot bubble with avatar. */
@Composable
fun TypingIndicator(botIcon: String?) {
    val theme = LocalRayaTheme.current
    val avatarUrl = botIcon?.ifBlank { null }
    val dotColor = if (theme.isDark) IconDefault else theme.mutedForeground

    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 800; 0f at 0; -6f at 200; 0f at 400; 0f at 800 },
            repeatMode = RepeatMode.Restart,
        ), label = "dot1"
    )
    val offset2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 800; 0f at 150; -6f at 350; 0f at 550; 0f at 800 },
            repeatMode = RepeatMode.Restart,
        ), label = "dot2"
    )
    val offset3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 800; 0f at 300; -6f at 500; 0f at 700; 0f at 800 },
            repeatMode = RepeatMode.Restart,
        ), label = "dot3"
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 20.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        if (avatarUrl != null) {
            Image(
                painter = rememberAsyncImagePainter(avatarUrl),
                contentDescription = "Bot",
                modifier = Modifier.size(28.dp).clip(CircleShape),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.width(8.dp))
        }
        Row(
            modifier = Modifier
                .background(theme.botBubble, RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BouncingDot(dotColor, offset1)
            BouncingDot(dotColor, offset2)
            BouncingDot(dotColor, offset3)
        }
    }
}

@Composable
private fun BouncingDot(color: androidx.compose.ui.graphics.Color, offsetY: Float) {
    Box(
        modifier = Modifier
            .offset(y = offsetY.dp)
            .size(6.dp)
            .clip(CircleShape)
            .background(color)
    )
}
