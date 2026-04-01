package ai.teammates.rayachat.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.teammates.rayachat.core.Constants
import ai.teammates.rayachat.core.models.ImagePayload
import ai.teammates.rayachat.core.models.ImageAsset
import ai.teammates.rayachat.ui.adapters.ImagePickerAdapter
import ai.teammates.rayachat.ui.components.common.RayaIcons
import ai.teammates.rayachat.ui.components.common.Strings
import ai.teammates.rayachat.ui.components.media.ImagePickerPreview
import ai.teammates.rayachat.ui.theme.IconDefault
import ai.teammates.rayachat.ui.theme.LocalRayaTheme
import ai.teammates.rayachat.ui.theme.RayaTypography
import kotlinx.coroutines.launch

/**
 * Message composer matching the web widget UI.
 * Card with subtle border, textarea above, action buttons below.
 */
@Composable
fun MessageComposer(
    placeholder: String? = null,
    enableVoiceNote: Boolean = true,
    enableImageUpload: Boolean = true,
    imagePickerAdapter: ImagePickerAdapter? = null,
    hasAudioAdapter: Boolean = false,
    disabled: Boolean = false,
    locale: String = "en",
    onSendMessage: (String) -> Unit,
    onSendImages: ((List<ImagePayload>, String) -> Unit)? = null,
    onMicPress: (() -> Unit)? = null,
) {
    val theme = LocalRayaTheme.current
    val scope = rememberCoroutineScope()
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var isFocused by remember { mutableStateOf(false) }
    var selectedImages by remember { mutableStateOf<List<ImageAsset>>(emptyList()) }

    val hasText = textFieldValue.text.isNotBlank()
    val hasImages = selectedImages.isNotEmpty()
    // Users can always type and select images — only sending is blocked during generation
    val canSend = (hasText || hasImages) && !disabled
    val actualPlaceholder = placeholder ?: Strings.get("type_message", locale)

    val borderColor = if (isFocused) theme.composerBorderFocused else theme.composerBorder
    val iconColor = IconDefault // Always full opacity — never disabled visually

    fun handleSend() {
        if (!canSend) return
        if (hasImages) {
            val payloads = selectedImages.map { img ->
                ImagePayload(name = img.name, type = img.type, base64 = img.base64, uri = img.uri)
            }
            onSendImages?.invoke(payloads, textFieldValue.text.trim())
            selectedImages = emptyList()
            textFieldValue = TextFieldValue("")
        } else if (hasText) {
            onSendMessage(textFieldValue.text.trim())
            textFieldValue = TextFieldValue("")
        }
    }

    fun handlePickImages() {
        if (disabled || imagePickerAdapter == null) return
        scope.launch {
            try {
                val remaining = Constants.MAX_IMAGES_PER_MESSAGE - selectedImages.size
                if (remaining <= 0) return@launch
                val picked = imagePickerAdapter.pickImages(remaining)
                if (picked.isNotEmpty()) {
                    selectedImages = (selectedImages + picked).take(Constants.MAX_IMAGES_PER_MESSAGE)
                }
            } catch (_: Exception) {}
        }
    }

    // Outer container with opaque background
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.background)
    ) {
        // Image preview — above the card
        if (hasImages) {
            ImagePickerPreview(
                images = selectedImages,
                onRemove = { index ->
                    selectedImages = selectedImages.filterIndexed { i, _ -> i != index }
                },
            )
        }

        // ── Card ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = if (hasImages) 2.dp else 8.dp, bottom = 10.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(theme.composerBg)
                .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            // ── Textarea ──
            BasicTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                enabled = true, // Always editable — user can type while generation is ongoing
                textStyle = RayaTypography.body.copy(
                    color = theme.foreground,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                ),
                cursorBrush = SolidColor(theme.gradientColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 36.dp, max = 140.dp)
                    .onFocusChanged { isFocused = it.isFocused },
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.padding(vertical = 2.dp)) {
                        if (textFieldValue.text.isEmpty()) {
                            Text(
                                actualPlaceholder,
                                fontSize = 15.sp,
                                color = theme.mutedForeground,
                                lineHeight = 22.sp,
                            )
                        }
                        innerTextField()
                    }
                },
            )

            Spacer(Modifier.height(8.dp))

            // ── Button row ──
            // Cache icons — only rebuilt when color changes, not on every recomposition
            val smileIcon = remember(iconColor) { RayaIcons.smile(iconColor) }
            val paperclipIcon = remember(iconColor) { RayaIcons.paperclip(iconColor) }
            val micIcon = remember(iconColor) { RayaIcons.mic(iconColor) }
            val sendIconColor = if (theme.isDark) Color.White else Color(0xFF3F3F46)
            val sendIcon = remember(sendIconColor) { RayaIcons.send(sendIconColor) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left: action icons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Emoji
                    ComposerIconButton(
                        icon = smileIcon,
                        contentDescription = "Emoji",
                        tint = iconColor,
                        onClick = { },
                    )

                    // Paperclip — hidden if no adapter
                    if (enableImageUpload && imagePickerAdapter != null) {
                        ComposerIconButton(
                            icon = paperclipIcon,
                            contentDescription = "Attach",
                            tint = iconColor,
                            onClick = ::handlePickImages,
                        )
                    }

                    // Mic — hidden if no adapter
                    if (enableVoiceNote && hasAudioAdapter) {
                        ComposerIconButton(
                            icon = micIcon,
                            contentDescription = "Record",
                            tint = iconColor,
                            onClick = { onMicPress?.invoke() },
                        )
                    }
                }

                // Right: Send button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(theme.sendBtnBg)
                        .clickable(onClick = ::handleSend),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = sendIcon,
                        contentDescription = "Send",
                        tint = sendIconColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/** Small icon button for the composer — consistent 36dp touch target, 22dp icon. */
@Composable
private fun ComposerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}
