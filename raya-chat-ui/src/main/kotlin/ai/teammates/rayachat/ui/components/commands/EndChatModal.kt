package ai.teammates.rayachat.ui.components.commands

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ai.teammates.rayachat.ui.components.common.RayaIcons
import ai.teammates.rayachat.ui.components.common.Strings
import ai.teammates.rayachat.ui.theme.LocalRayaTheme
import ai.teammates.rayachat.ui.theme.RayaTypography

/** Full-screen "End Chat Session" confirmation overlay. */
@Composable
fun EndChatModal(
    visible: Boolean,
    locale: String = "en",
    onCancel: () -> Unit,
    onEndSession: () -> Unit,
) {
    if (!visible) return

    val theme = LocalRayaTheme.current

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.background)
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Icon in circle
                Box(
                    modifier = Modifier
                        .size(75.dp)
                        .background(theme.muted, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = RayaIcons.messageSquareX(theme.mutedForeground),
                        contentDescription = null,
                        tint = theme.mutedForeground,
                        modifier = Modifier.size(32.dp),
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    Strings.get("end_chat_title", locale),
                    style = RayaTypography.subheading,
                    color = theme.foreground,
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    Strings.get("end_chat_subtitle", locale),
                    style = RayaTypography.body,
                    color = theme.mutedForeground,
                )

                Spacer(Modifier.height(32.dp))

                // Buttons — stacked vertically, full width
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.5.dp, theme.border),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Text(Strings.get("cancel", locale), style = RayaTypography.bodyBold, color = theme.foreground)
                    }
                    Button(
                        onClick = onEndSession,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.gradientColor),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Text(Strings.get("end_session", locale), style = RayaTypography.bodyBold, color = theme.gradientForeground)
                    }
                }
            }
        }
    }
}
