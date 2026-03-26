package ai.teammates.rayachat.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import ai.teammates.rayachat.ui.components.common.RayaIcons
import ai.teammates.rayachat.ui.components.common.Strings
import ai.teammates.rayachat.ui.theme.IconDefault
import ai.teammates.rayachat.ui.theme.LocalRayaTheme
import ai.teammates.rayachat.ui.theme.RayaTypography

/**
 * Message composer — text input + action buttons.
 * Matches the RN SDK's card-based composer with dynamic button visibility.
 */
@Composable
fun MessageComposer(
    placeholder: String? = null,
    enableVoiceNote: Boolean = true,
    enableImageUpload: Boolean = true,
    hasImageAdapter: Boolean = false,
    hasAudioAdapter: Boolean = false,
    disabled: Boolean = false,
    locale: String = "en",
    onSendMessage: (String) -> Unit,
    onImagePress: (() -> Unit)? = null,
    onMicPress: (() -> Unit)? = null,
) {
    val theme = LocalRayaTheme.current
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var isFocused by remember { mutableStateOf(false) }

    val hasText = textFieldValue.text.isNotBlank()
    val canSend = hasText && !disabled

    val borderColor = if (isFocused) theme.composerBorderFocused else theme.composerBorder
    val actualPlaceholder = placeholder ?: Strings.get("type_message", locale)

    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Card container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(12.dp), clip = false)
                .clip(RoundedCornerShape(12.dp))
                .background(theme.composerBg)
                .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(12.dp),
        ) {
            // Text input
            BasicTextField(
                value = textFieldValue,
                onValueChange = { if (!disabled) textFieldValue = it },
                enabled = !disabled,
                textStyle = RayaTypography.input.copy(color = theme.foreground),
                cursorBrush = SolidColor(theme.foreground),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp, max = 160.dp)
                    .onFocusChanged { isFocused = it.isFocused },
                decorationBox = { innerTextField ->
                    Box {
                        if (textFieldValue.text.isEmpty()) {
                            Text(
                                text = actualPlaceholder,
                                style = RayaTypography.input,
                                color = theme.mutedForeground,
                            )
                        }
                        innerTextField()
                    }
                },
            )

            Spacer(Modifier.height(4.dp))

            // Button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left buttons
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Emoji
                    IconButton(onClick = { /* Focus keyboard */ }, modifier = Modifier.size(28.dp), enabled = !disabled) {
                        Icon(RayaIcons.smile(IconDefault), "Emoji", tint = IconDefault, modifier = Modifier.size(20.dp))
                    }

                    // Image upload — hidden if no adapter
                    if (enableImageUpload && hasImageAdapter) {
                        IconButton(onClick = { onImagePress?.invoke() }, modifier = Modifier.size(28.dp), enabled = !disabled) {
                            Icon(RayaIcons.paperclip(IconDefault), "Attach", tint = IconDefault, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Mic — hidden if no adapter
                    if (enableVoiceNote && hasAudioAdapter) {
                        IconButton(onClick = { onMicPress?.invoke() }, modifier = Modifier.size(28.dp), enabled = !disabled) {
                            Icon(RayaIcons.mic(IconDefault), "Record", tint = IconDefault, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Send button
                IconButton(
                    onClick = {
                        if (canSend) {
                            onSendMessage(textFieldValue.text.trim())
                            textFieldValue = TextFieldValue("")
                        }
                    },
                    enabled = canSend,
                    modifier = Modifier
                        .size(35.dp)
                        .clip(CircleShape)
                        .background(theme.sendBtnBg),
                ) {
                    val sendColor = if (canSend) {
                        if (theme.isDark) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color(0xFF3F3F46)
                    } else IconDefault

                    Icon(RayaIcons.send(sendColor), "Send", tint = sendColor, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
