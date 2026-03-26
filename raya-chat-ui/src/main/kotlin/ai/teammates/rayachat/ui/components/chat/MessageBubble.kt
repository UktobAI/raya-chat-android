package ai.teammates.rayachat.ui.components.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import ai.teammates.rayachat.core.Constants
import ai.teammates.rayachat.core.models.Attachment
import ai.teammates.rayachat.core.models.TypeMessage
import ai.teammates.rayachat.core.util.formatLocalTime
import ai.teammates.rayachat.ui.theme.LocalRayaTheme
import ai.teammates.rayachat.ui.theme.RayaTypography
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

@Composable
fun MessageBubble(
    message: TypeMessage,
    botIcon: String?,
    onImagePress: ((String) -> Unit)? = null,
) {
    val theme = LocalRayaTheme.current
    val isUser = message.sender == 1
    val isSystem = message.type == 4

    val attachments = remember(message.attachmentsJson) {
        message.attachmentsJson?.let {
            try { json.decodeFromString<List<Attachment>>(it) } catch (_: Exception) { null }
        }
    }
    val hasImages = !attachments.isNullOrEmpty()

    // System message
    if (isSystem) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = message.content ?: "",
                style = RayaTypography.caption,
                color = theme.systemMessageForeground,
                modifier = Modifier
                    .background(theme.systemMessage, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            )
        }
        return
    }

    val avatarUrl = botIcon?.ifBlank { null } ?: Constants.DEFAULT_BOT_AVATAR
    val timestamp = formatLocalTime(
        epochSeconds = message.createdAt?.toLongOrNull()
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 0.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        // User image message — images above text
        if (isUser && hasImages) {
            UserImageSection(attachments!!, message.content, theme, onImagePress, timestamp)
            Spacer(Modifier.height(20.dp))
            return
        }

        // Normal message (bot or user text)
        Row(
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Bot avatar
            if (!isUser) {
                Image(
                    painter = rememberAsyncImagePainter(avatarUrl),
                    contentDescription = "Bot",
                    modifier = Modifier.size(28.dp).clip(CircleShape),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.width(8.dp))
            }

            // Bubble
            val bubbleShape = if (isUser) {
                RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
            } else {
                RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
            }
            val bubbleBg = if (isUser) theme.gradientColor else theme.botBubble
            val textColor = if (isUser) theme.gradientForeground else theme.botBubbleForeground

            Column(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .background(bubbleBg, bubbleShape)
                    .padding(horizontal = 16.dp, vertical = if (isUser) 8.dp else 16.dp),
            ) {
                // Bot images inside bubble
                if (!isUser && hasImages) {
                    BotImageGrid(attachments!!, onImagePress)
                    Spacer(Modifier.height(8.dp))
                }

                if (!message.content.isNullOrBlank()) {
                    if (!isUser) {
                        MarkdownText(
                            content = message.content!!,
                            textColor = textColor,
                            fontSize = 14.sp,
                        )
                    } else {
                        Text(
                            text = message.content!!,
                            style = RayaTypography.body,
                            color = textColor,
                        )
                    }
                }
            }
        }

        // Timestamp
        if (timestamp.isNotBlank()) {
            Text(
                text = timestamp,
                style = RayaTypography.small,
                color = theme.mutedForeground,
                modifier = Modifier.padding(
                    top = 8.dp,
                    start = if (!isUser) 36.dp else 0.dp, // align with bubble, not avatar
                ),
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun UserImageSection(
    attachments: List<Attachment>,
    content: String?,
    theme: ai.teammates.rayachat.ui.theme.RayaTheme,
    onImagePress: ((String) -> Unit)?,
    timestamp: String,
) {
    Column(horizontalAlignment = Alignment.End) {
        // Image grid
        Row(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
        ) {
            attachments.take(Constants.MAX_IMAGES_PER_MESSAGE).forEach { att ->
                Image(
                    painter = rememberAsyncImagePainter(att.url),
                    contentDescription = "Image",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onImagePress?.invoke(att.url) },
                    contentScale = ContentScale.Crop,
                )
            }
        }

        // Text below images
        if (!content.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = content,
                style = RayaTypography.body,
                color = theme.gradientForeground,
                modifier = Modifier
                    .background(theme.gradientColor, RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        if (timestamp.isNotBlank()) {
            Text(
                text = timestamp,
                style = RayaTypography.small,
                color = theme.mutedForeground,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun BotImageGrid(attachments: List<Attachment>, onImagePress: ((String) -> Unit)?) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        attachments.forEach { att ->
            Image(
                painter = rememberAsyncImagePainter(att.url),
                contentDescription = "Image",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onImagePress?.invoke(att.url) },
                contentScale = ContentScale.Crop,
            )
        }
    }
}
