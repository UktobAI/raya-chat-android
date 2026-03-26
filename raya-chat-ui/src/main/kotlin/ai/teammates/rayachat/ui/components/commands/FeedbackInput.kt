package ai.teammates.rayachat.ui.components.commands

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import ai.teammates.rayachat.core.Constants
import ai.teammates.rayachat.ui.components.common.Strings
import ai.teammates.rayachat.ui.theme.LocalRayaTheme
import ai.teammates.rayachat.ui.theme.RayaTypography

/** Submit feedback — textarea + Skip/Submit in bot bubble. */
@Composable
fun FeedbackInput(
    message: String,
    optional: Boolean = false,
    botIcon: String?,
    locale: String = "en",
    onSubmit: (String) -> Unit,
) {
    val theme = LocalRayaTheme.current
    val avatarUrl = botIcon?.ifBlank { null } ?: Constants.DEFAULT_BOT_AVATAR
    var feedback by remember { mutableStateOf("") }
    val canSubmit = feedback.isNotBlank() || optional

    BotBubbleWrapper(avatarUrl = avatarUrl, theme = theme) {
        if (message.isNotBlank()) {
            Text(message, style = RayaTypography.body, color = theme.botBubbleForeground)
            Spacer(Modifier.height(12.dp))
        }

        // Textarea
        BasicTextField(
            value = feedback,
            onValueChange = { if (it.length <= Constants.MAX_FEEDBACK_LENGTH) feedback = it },
            textStyle = RayaTypography.input.copy(color = theme.foreground),
            cursorBrush = SolidColor(theme.foreground),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp)
                .border(1.dp, theme.inputBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (feedback.isEmpty()) {
                        Text(
                            Strings.get("type_message", locale),
                            style = RayaTypography.input,
                            color = theme.mutedForeground,
                        )
                    }
                    innerTextField()
                }
            },
        )

        Spacer(Modifier.height(10.dp))

        // Bottom row: counter + buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${feedback.length}/${Constants.MAX_FEEDBACK_LENGTH} ${Strings.get("characters", locale)}",
                style = RayaTypography.caption,
                color = theme.mutedForeground,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (optional) {
                    OutlinedButton(
                        onClick = { onSubmit("") },
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, theme.borderWarm),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(Strings.get("skip", locale), style = RayaTypography.buttonSmall, color = theme.foreground)
                    }
                }
                Button(
                    onClick = { onSubmit(feedback.trim()) },
                    enabled = canSubmit,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.gradientColor),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(Strings.get("submit", locale), style = RayaTypography.buttonSmall, color = theme.gradientForeground)
                }
            }
        }
    }
}
