package ai.teammates.rayachat.ui.components.media

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.teammates.rayachat.ui.adapters.AudioRecorderAdapter
import ai.teammates.rayachat.ui.adapters.AudioResult
import ai.teammates.rayachat.ui.components.common.RayaIcons
import ai.teammates.rayachat.ui.theme.LocalRayaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

private const val MAX_VISIBLE_BARS = 40
private const val SAMPLE_INTERVAL_MS = 100L

/**
 * Live audio-recording overlay that replaces the message composer while recording.
 *
 * Layout (left to right): Cancel · red dot · monospaced timer · scrolling waveform ·
 * Pause/Resume · Stop. On stop, the captured amplitudes are passed back to the caller
 * to seed the static waveform shown in [AudioPreviewUI].
 *
 * The adapter is started on first composition and cleaned up on disposal regardless of
 * outcome (cancel via X button, system back, or unexpected recomposition).
 */
@Composable
fun AudioRecorderUI(
    adapter: AudioRecorderAdapter,
    onComplete: (AudioResult, FloatArray) -> Unit,
    onCancel: () -> Unit,
) {
    val theme = LocalRayaTheme.current
    val scope = rememberCoroutineScope()
    var isPaused by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    val amplitudes = remember { mutableStateListOf<Float>() }
    var started by remember { mutableStateOf(false) }
    var stopping by remember { mutableStateOf(false) }

    // Start the recorder once. If it throws (permission, init), bail to cancel.
    LaunchedEffect(Unit) {
        try {
            adapter.startRecording()
            started = true
        } catch (_: Exception) {
            onCancel()
            return@LaunchedEffect
        }

        // Amplitude sampling loop — feeds the live waveform.
        launch {
            while (isActive) {
                if (!isPaused) {
                    val amp = runCatching { adapter.getAmplitude() }.getOrDefault(0f)
                    amplitudes.add(amp.coerceIn(0f, 1f))
                }
                delay(SAMPLE_INTERVAL_MS)
            }
        }

        // Elapsed-time loop.
        launch {
            while (isActive) {
                delay(1_000)
                if (!isPaused) elapsedSeconds++
            }
        }
    }

    // Always release the recorder on disposal.
    DisposableEffect(Unit) {
        onDispose {
            scope.launch {
                runCatching { adapter.cleanup() }
            }
        }
    }

    BackHandler(enabled = !stopping) {
        scope.launch {
            runCatching { adapter.cleanup() }
            onCancel()
        }
    }

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
            contentDescription = "Cancel recording",
            tint = Color.Unspecified,
            modifier = Modifier
                .size(28.dp)
                .clickableNoRipple {
                    if (stopping) return@clickableNoRipple
                    scope.launch {
                        runCatching { adapter.cleanup() }
                        onCancel()
                    }
                },
        )

        // Red recording dot — dimmed when paused.
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFFEF4444).copy(alpha = if (isPaused) 0.3f else 1f)),
        )

        // Timer (monospaced, fixed width).
        Text(
            text = formatHms(elapsedSeconds),
            color = theme.foreground,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.widthIn(min = 64.dp),
        )

        // Live waveform — scrolling, suffix-clipped to last MAX_VISIBLE_BARS samples.
        // Snapshot to a stable array so Canvas state-tracking sees a fresh value on each
        // recomposition. Reading `amplitudes.size` here (in the composable body, not the
        // draw scope) anchors the recomposition trigger to the SnapshotStateList.
        val ampSnapshot = remember(amplitudes.size) { amplitudes.toFloatArray() }
        WaveformBars(
            amplitudes = ampSnapshot,
            color = theme.foreground,
            modifier = Modifier
                .weight(1f)
                .height(28.dp),
        )

        // Pause / Resume
        Icon(
            imageVector = if (isPaused) RayaIcons.circlePlay(theme.mutedForeground)
            else RayaIcons.circlePause(theme.mutedForeground),
            contentDescription = if (isPaused) "Resume" else "Pause",
            tint = Color.Unspecified,
            modifier = Modifier
                .size(28.dp)
                .clickableNoRipple {
                    if (stopping) return@clickableNoRipple
                    scope.launch {
                        if (isPaused) {
                            runCatching { adapter.resumeRecording() }
                            isPaused = false
                        } else {
                            runCatching { adapter.pauseRecording() }
                            isPaused = true
                        }
                    }
                },
        )

        // Stop → preview
        Icon(
            imageVector = RayaIcons.circleStop(theme.mutedForeground),
            contentDescription = "Stop recording",
            tint = Color.Unspecified,
            modifier = Modifier
                .size(28.dp)
                .clickableNoRipple {
                    if (stopping || !started) return@clickableNoRipple
                    stopping = true
                    scope.launch {
                        val result = runCatching { adapter.stopRecording() }.getOrNull()
                        if (result == null) {
                            runCatching { adapter.cleanup() }
                            onCancel()
                            return@launch
                        }
                        onComplete(result, amplitudes.toFloatArray())
                    }
                },
        )
    }
}

/**
 * Static waveform bars, drawn right-to-left (newest on the right) up to a max of
 * [MAX_VISIBLE_BARS] bars. Used for the live recording overlay.
 */
@Composable
private fun WaveformBars(
    amplitudes: FloatArray,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val barWidthPx = 2.dp.toPx()
        val gapPx = 2.dp.toPx()
        val pitchPx = barWidthPx + gapPx
        val maxBars = (size.width / pitchPx).toInt().coerceAtMost(MAX_VISIBLE_BARS)
        if (maxBars <= 0) return@Canvas

        val startIdx = if (amplitudes.size > maxBars) amplitudes.size - maxBars else 0
        val cornerRadius = CornerRadius(1.dp.toPx())
        val visibleCount = amplitudes.size - startIdx
        val totalWidth = visibleCount * pitchPx
        var x = size.width - totalWidth
        for (i in startIdx until amplitudes.size) {
            val amp = amplitudes[i].coerceIn(0f, 1f)
            val h = max(3.dp.toPx(), amp * size.height)
            val top = (size.height - h) / 2f
            drawRoundRect(
                color = color,
                topLeft = Offset(x, top),
                size = Size(barWidthPx, h),
                cornerRadius = cornerRadius,
            )
            x += pitchPx
        }
    }
}

internal fun formatHms(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

@Composable
internal fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )
