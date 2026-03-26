package ai.teammates.rayachat.ui.components.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ai.teammates.rayachat.ui.adapters.AudioPlayerAdapter
import ai.teammates.rayachat.ui.components.common.RayaIcons
import ai.teammates.rayachat.ui.theme.LocalRayaTheme
import ai.teammates.rayachat.ui.theme.RayaTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Audio playback with progress bar — uses adapter pattern. */
@Composable
fun AudioPlayerUI(
    uri: String,
    adapter: AudioPlayerAdapter,
    modifier: Modifier = Modifier,
) {
    val theme = LocalRayaTheme.current
    val scope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(false) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var loaded by remember { mutableStateOf(false) }

    // Load audio
    LaunchedEffect(uri) {
        try {
            val info = adapter.loadAudio(uri)
            durationMs = info.durationMs
            loaded = true
        } catch (_: Exception) {}
    }

    // Update position while playing
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            positionMs = adapter.getPosition()
            if (positionMs >= durationMs && durationMs > 0) {
                isPlaying = false
                positionMs = 0
            }
            delay(100)
        }
    }

    val progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
    val durationStr = formatDuration(durationMs)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(theme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Play/Pause button
        IconButton(
            onClick = {
                scope.launch {
                    if (isPlaying) { adapter.pause(); isPlaying = false }
                    else { adapter.play(); isPlaying = true }
                }
            },
            enabled = loaded,
            modifier = Modifier.size(30.dp),
        ) {
            val icon = if (isPlaying) RayaIcons.close(theme.foreground) else RayaIcons.mic(theme.foreground)
            Icon(icon, if (isPlaying) "Pause" else "Play", tint = theme.foreground, modifier = Modifier.size(20.dp))
        }

        // Progress bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = theme.gradientColor,
            trackColor = theme.border,
        )

        // Duration
        Text(durationStr, style = RayaTypography.small, color = theme.mutedForeground)
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
