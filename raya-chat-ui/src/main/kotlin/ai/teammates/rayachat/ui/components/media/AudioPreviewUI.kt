package ai.teammates.rayachat.ui.components.media

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ai.teammates.rayachat.core.adapters.AudioPlayerAdapter
import ai.teammates.rayachat.ui.adapters.AudioResult
import ai.teammates.rayachat.ui.components.common.RayaIcons
import ai.teammates.rayachat.ui.theme.LocalRayaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Post-stop preview overlay. Shows the captured recording as a static waveform with
 * playback progress, and gives the user three choices: cancel (discard), play/pause
 * (preview), or send.
 *
 * If [audioPlayerAdapter] is null, a [DefaultAudioPlayerAdapter] is constructed
 * internally — this lets the preview work even when the consumer didn't pass a custom
 * playback adapter to the SDK.
 */
@Composable
fun AudioPreviewUI(
    audioResult: AudioResult,
    amplitudes: FloatArray,
    onSend: (AudioResult) -> Unit,
    onCancel: () -> Unit,
    audioPlayerAdapter: AudioPlayerAdapter? = null,
) {
    val theme = LocalRayaTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val player = remember(audioPlayerAdapter) {
        audioPlayerAdapter ?: DefaultAudioPlayerAdapter(context.applicationContext)
    }

    var durationMs by remember { mutableLongStateOf(0L) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(audioResult.uri) {
        runCatching {
            val info = player.loadAudio(audioResult.uri)
            durationMs = info.durationMs
            loaded = true
        }.onFailure {
            // If preview can't even load, treat it like a cancel — recorder file may be gone.
            onCancel()
        }
    }

    // Position polling while playing.
    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            positionMs = runCatching { player.getPosition() }.getOrDefault(positionMs)
            // Detect end-of-playback: position at duration (with 200ms tolerance).
            if (durationMs > 0 && positionMs >= durationMs - 200) {
                isPlaying = false
                positionMs = durationMs
                runCatching { player.seekTo(0) }
                positionMs = 0
            }
            delay(50)
        }
    }

    DisposableEffect(player) {
        onDispose {
            scope.launch { runCatching { player.cleanup() } }
        }
    }

    BackHandler {
        scope.launch { runCatching { player.cleanup() } }
        onCancel()
    }

    val playedRatio: Float = if (durationMs > 0) {
        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, theme.composerBorder, RoundedCornerShape(14.dp))
            .background(theme.composerBg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Cancel
        Icon(
            imageVector = RayaIcons.circleX(theme.mutedForeground),
            contentDescription = "Cancel",
            tint = Color.Unspecified,
            modifier = Modifier
                .size(28.dp)
                .clickableNoRipple {
                    scope.launch { runCatching { player.cleanup() } }
                    onCancel()
                },
        )

        // Static waveform with progress.
        StaticWaveform(
            amplitudes = amplitudes,
            playedRatio = playedRatio,
            color = theme.foreground,
            modifier = Modifier
                .weight(1f)
                .height(28.dp),
        )

        // Play / Pause
        Icon(
            imageVector = if (isPlaying) RayaIcons.circlePause(theme.mutedForeground)
            else RayaIcons.circlePlay(theme.mutedForeground),
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.Unspecified,
            modifier = Modifier
                .size(28.dp)
                .clickableNoRipple {
                    if (!loaded) return@clickableNoRipple
                    scope.launch {
                        if (isPlaying) {
                            runCatching { player.pause() }
                            isPlaying = false
                        } else {
                            runCatching { player.play() }
                            isPlaying = true
                        }
                    }
                },
        )

        // Send (filled gradient circle)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(theme.gradientColor)
                .clickableNoRipple {
                    scope.launch { runCatching { player.cleanup() } }
                    onSend(audioResult)
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = RayaIcons.send(theme.gradientForeground),
                contentDescription = "Send voice note",
                tint = Color.Unspecified,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * Static waveform: bars are downsampled (peak-per-bucket) to fit the available width.
 * Bars at index < playedRatio are drawn at full opacity; the rest at 40%.
 */
@Composable
private fun StaticWaveform(
    amplitudes: FloatArray,
    playedRatio: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val barWidthPx = 2.dp.toPx()
        val gapPx = 2.dp.toPx()
        val pitchPx = barWidthPx + gapPx
        val barCount = (size.width / pitchPx).toInt().coerceAtLeast(1)
        if (barCount <= 0 || amplitudes.isEmpty()) return@Canvas

        val downsampled = downsample(amplitudes, barCount)
        val cornerRadius = CornerRadius(1.dp.toPx())
        val playedThreshold = playedRatio * barCount

        var x = 0f
        downsampled.forEachIndexed { idx, amp ->
            val h = max(3.dp.toPx(), amp.coerceIn(0f, 1f) * size.height)
            val top = (size.height - h) / 2f
            val isPlayed = idx < playedThreshold
            val drawColor = if (isPlayed) color else color.copy(alpha = 0.4f)
            drawRoundRect(
                color = drawColor,
                topLeft = Offset(x, top),
                size = Size(barWidthPx, h),
                cornerRadius = cornerRadius,
            )
            x += pitchPx
        }
    }
}

/** Peak-per-bucket downsample (matches iOS `downsample(amps, to:)`). */
internal fun downsample(amps: FloatArray, target: Int): FloatArray {
    if (target <= 0 || amps.isEmpty()) return FloatArray(0)
    if (amps.size <= target) return amps
    val out = FloatArray(target)
    val bucket = amps.size.toFloat() / target
    for (i in 0 until target) {
        val start = (i * bucket).toInt()
        val end = ((i + 1) * bucket).toInt().coerceAtMost(amps.size)
        var peak = 0f
        for (j in start until end) if (amps[j] > peak) peak = amps[j]
        out[i] = peak
    }
    return out
}
