@file:OptIn(ExperimentalLayoutApi::class)

package ai.teammates.rayachat.ui.components.commands

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import ai.teammates.rayachat.core.Constants
import ai.teammates.rayachat.ui.theme.LocalRayaTheme
import ai.teammates.rayachat.ui.theme.RayaTypography

/** End session command — pill buttons inside bot bubble. */
@Composable
fun EndSessionUI(
    message: String,
    options: List<Any>,
    botIcon: String?,
    onSelect: (Any) -> Unit,
) {
    val theme = LocalRayaTheme.current
    val avatarUrl = botIcon?.ifBlank { null }

    BotBubbleWrapper(avatarUrl = avatarUrl, theme = theme) {
        if (message.isNotBlank()) {
            Text(message, style = RayaTypography.body, color = theme.botBubbleForeground)
            Spacer(Modifier.height(12.dp))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                OutlinedButton(
                    onClick = { onSelect(option) },
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, theme.borderWarm),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = theme.surface),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(option.toString(), style = RayaTypography.buttonSmall, color = theme.foreground)
                }
            }
        }
    }
}

/** Shared wrapper for command UIs rendered as bot bubbles. */
@Composable
internal fun BotBubbleWrapper(
    avatarUrl: String?,
    theme: ai.teammates.rayachat.ui.theme.RayaTheme,
    content: @Composable ColumnScope.() -> Unit,
) {
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
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(theme.botBubble, RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                .padding(16.dp),
            content = content,
        )
    }
}
