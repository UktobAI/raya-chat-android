package ai.teammates.rayachat.ui.components.commands

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.teammates.rayachat.core.Constants
import ai.teammates.rayachat.ui.components.common.Strings
import ai.teammates.rayachat.ui.theme.LocalRayaTheme
import ai.teammates.rayachat.ui.theme.RayaTypography
import kotlinx.coroutines.delay

/** Feedback received — circular SVG countdown in bot bubble. */
@Composable
fun CountdownClose(
    message: String,
    botIcon: String?,
    locale: String = "en",
    onComplete: () -> Unit,
) {
    val theme = LocalRayaTheme.current
    val avatarUrl = botIcon?.ifBlank { null }
    var count by remember { mutableIntStateOf(Constants.FEEDBACK_COUNTDOWN_SECONDS) }

    LaunchedEffect(Unit) {
        while (count > 0) {
            delay(1000)
            count--
        }
        onComplete()
    }

    val progress = count.toFloat() / Constants.FEEDBACK_COUNTDOWN_SECONDS
    val trackColor = theme.border
    val progressColor = theme.gradientColor

    BotBubbleWrapper(avatarUrl = avatarUrl, theme = theme) {
        if (message.isNotBlank()) {
            Text(message, style = RayaTypography.body, color = theme.botBubbleForeground)
            Spacer(Modifier.height(12.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Circular countdown
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(32.dp)) {
                Canvas(modifier = Modifier.size(32.dp)) {
                    val strokeWidth = 3.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    // Track
                    drawCircle(color = trackColor, radius = radius, style = Stroke(width = strokeWidth))

                    // Progress arc
                    val sweepAngle = progress * 360f
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                    )
                }
                Text("$count", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = theme.botBubbleForeground)
            }

            Text(Strings.get("ending_session", locale), style = RayaTypography.caption, color = theme.mutedForeground)
        }
    }
}
