package ai.teammates.rayachat.ui.components.commands

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.teammates.rayachat.core.Constants
import ai.teammates.rayachat.ui.theme.IconDefault
import ai.teammates.rayachat.ui.theme.LocalRayaTheme
import ai.teammates.rayachat.ui.theme.RayaTypography

private data class RatingItem(val value: Int, val label: String, val labelAr: String, val color: Color)

private val RATINGS = listOf(
    RatingItem(1, "Very Bad", "سيء جدًا", Color(0xFFEF4444)),
    RatingItem(2, "Bad", "سيء", Color(0xFFF97316)),
    RatingItem(3, "Okay", "مقبول", Color(0xFFEAB308)),
    RatingItem(4, "Good", "جيد", Color(0xFF84CC16)),
    RatingItem(5, "Excellent", "ممتاز", Color(0xFF22C55E)),
)

/** Rate conversation — 5 face icons in bot bubble. */
@Composable
fun RatingUI(
    message: String,
    options: List<Any>,
    botIcon: String?,
    locale: String = "en",
    onRate: (Int) -> Unit,
) {
    val theme = LocalRayaTheme.current
    val avatarUrl = botIcon?.ifBlank { null }
    var selected by remember { mutableIntStateOf(0) }
    val isAr = locale.startsWith("ar")
    val filteredRatings = RATINGS.filter { r -> options.any { it.toString().toIntOrNull() == r.value } }

    BotBubbleWrapper(avatarUrl = avatarUrl, theme = theme) {
        if (message.isNotBlank()) {
            Text(message, style = RayaTypography.body.copy(fontStyle = FontStyle.Italic), color = theme.botBubbleForeground)
            Spacer(Modifier.height(12.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            filteredRatings.forEach { item ->
                val isSelected = selected == item.value
                val iconColor = if (isSelected) item.color else IconDefault

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            selected = item.value
                            onRate(item.value)
                        }
                        .background(
                            if (isSelected) item.color.copy(alpha = 0.1f) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                ) {
                    // Simple face icon drawn with Canvas
                    FaceIcon(color = iconColor, mouthType = item.value)

                    if (isSelected) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (isAr) item.labelAr else item.label,
                            fontSize = 9.sp,
                            color = item.color,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FaceIcon(color: Color, mouthType: Int) {
    val strokeWidth = 1.5f
    Box(
        modifier = Modifier.size(32.dp).drawBehind {
            val cx = size.width / 2
            val cy = size.height / 2
            val r = size.width / 2 - 2

            // Circle
            drawCircle(color = color, radius = r, style = Stroke(width = strokeWidth.dp.toPx()))

            // Eyes
            drawCircle(color = color, radius = 1.5.dp.toPx(), center = Offset(cx - 4.dp.toPx(), cy - 2.dp.toPx()))
            drawCircle(color = color, radius = 1.5.dp.toPx(), center = Offset(cx + 4.dp.toPx(), cy - 2.dp.toPx()))

            // Mouth varies by rating
            val mouthY = cy + 4.dp.toPx()
            when (mouthType) {
                1, 2 -> { // Frown
                    drawArc(color = color, startAngle = 200f, sweepAngle = 140f,
                        useCenter = false, style = Stroke(width = strokeWidth.dp.toPx(), cap = StrokeCap.Round),
                        topLeft = Offset(cx - 5.dp.toPx(), mouthY - 2.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(10.dp.toPx(), 6.dp.toPx()))
                }
                3 -> { // Straight
                    drawLine(color = color, start = Offset(cx - 4.dp.toPx(), mouthY),
                        end = Offset(cx + 4.dp.toPx(), mouthY), strokeWidth = strokeWidth.dp.toPx(), cap = StrokeCap.Round)
                }
                4, 5 -> { // Smile
                    drawArc(color = color, startAngle = 20f, sweepAngle = 140f,
                        useCenter = false, style = Stroke(width = strokeWidth.dp.toPx(), cap = StrokeCap.Round),
                        topLeft = Offset(cx - 5.dp.toPx(), mouthY - 4.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(10.dp.toPx(), 6.dp.toPx()))
                }
            }
        }
    )
}
