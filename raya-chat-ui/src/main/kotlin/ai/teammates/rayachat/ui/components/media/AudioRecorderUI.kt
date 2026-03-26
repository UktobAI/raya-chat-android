package ai.teammates.rayachat.ui.components.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ai.teammates.rayachat.ui.adapters.AudioRecorderAdapter
import ai.teammates.rayachat.ui.adapters.AudioResult
import ai.teammates.rayachat.ui.components.common.RayaIcons
import ai.teammates.rayachat.ui.theme.LocalRayaTheme
import ai.teammates.rayachat.ui.theme.RayaTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Audio recording UI with timer and controls — uses adapter pattern. */
@Composable
fun AudioRecorderUI(
    adapter: AudioRecorderAdapter,
    onComplete: (AudioResult) -> Unit,
    onCancel: () -> Unit,
) {
    val theme = LocalRayaTheme.current
    val scope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }

    // Start recording on mount
    LaunchedEffect(Unit) {
        try {
            adapter.startRecording()
            isRecording = true
        } catch (_: Exception) {
            onCancel()
        }
    }

    // Timer
    LaunchedEffect(isRecording, isPaused) {
        while (isRecording && !isPaused) {
            delay(1000)
            seconds++
        }
    }

    val timeStr = String.format("%02d:%02d", seconds / 60, seconds % 60)

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        // Cancel
        IconButton(onClick = {
            scope.launch {
                isRecording = false
                adapter.cleanup()
                onCancel()
            }
        }) {
            Icon(RayaIcons.close(Color(0xFFA1A1AA)), "Cancel", tint = Color(0xFFA1A1AA), modifier = Modifier.size(24.dp))
        }

        // Timer
        Text(timeStr, style = RayaTypography.body, color = theme.foreground)

        // Pause/Resume
        IconButton(onClick = {
            scope.launch {
                if (isPaused) { adapter.resumeRecording(); isPaused = false }
                else { adapter.pauseRecording(); isPaused = true }
            }
        }) {
            val icon = if (isPaused) RayaIcons.mic(theme.foreground) else RayaIcons.mic(Color(0xFFEF4444))
            Icon(icon, if (isPaused) "Resume" else "Pause", tint = if (isPaused) theme.foreground else Color(0xFFEF4444), modifier = Modifier.size(24.dp))
        }

        // Stop & send
        IconButton(
            onClick = {
                scope.launch {
                    isRecording = false
                    try {
                        val result = adapter.stopRecording()
                        onComplete(result)
                    } catch (_: Exception) {
                        onCancel()
                    }
                }
            },
            modifier = Modifier.size(40.dp).clip(CircleShape).background(theme.gradientColor),
        ) {
            Icon(RayaIcons.send(theme.gradientForeground), "Send", tint = theme.gradientForeground, modifier = Modifier.size(20.dp))
        }
    }
}
