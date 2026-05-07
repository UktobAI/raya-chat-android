package ai.teammates.rayachat.ui.components.media

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.teammates.rayachat.core.adapters.AudioPlayerAdapter
import ai.teammates.rayachat.ui.components.common.RayaIcons
import ai.teammates.rayachat.ui.theme.LocalRayaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.sqrt

private const val WAVEFORM_BUCKETS = 60
private const val SILENCE_THRESHOLD = 0.05f
private const val POSITION_TICK_MS = 100L
private const val END_OF_PLAYBACK_TOLERANCE_MS = 200L
private const val STATIONARY_TICKS_TO_END = 3

/**
 * In-bubble audio playback row. Renders a static waveform with a dashed line behind it,
 * played/unplayed coloring, time label, and outlined Play/Pause control.
 *
 * The waveform amplitudes are scanned from the loaded WAV file via
 * [AudioPlayerAdapter.getAmplitudes]. If amplitudes can't be extracted (non-WAV codec
 * or load failure), only the dashed line shows — gracefully degraded.
 *
 * End-of-playback detection uses two complementary heuristics: position within 200ms of
 * duration, OR position stationary for 3 consecutive ticks (~300ms). The latter catches
 * `MediaPlayer.OnCompletionListener` flakiness on some OEMs.
 */
@Composable
fun AudioPlayerUI(
    uri: String,
    adapter: AudioPlayerAdapter,
    modifier: Modifier = Modifier,
    foregroundOverride: Color? = null,
) {
    val theme = LocalRayaTheme.current
    val foreground = foregroundOverride ?: theme.foreground
    val scope = rememberCoroutineScope()

    var isPlaying by remember { mutableStateOf(false) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var loaded by remember { mutableStateOf(false) }
    var amplitudes by remember { mutableStateOf<FloatArray?>(null) }

    // Load audio + scan amplitudes on first composition (or when uri changes due to
    // local→remote URL swap after server transcribes).
    LaunchedEffect(uri) {
        loaded = false
        positionMs = 0
        isPlaying = false
        runCatching {
            val info = adapter.loadAudio(uri)
            durationMs = info.durationMs
            amplitudes = adapter.getAmplitudes(WAVEFORM_BUCKETS)
            loaded = true
        }
    }

    // Tick playback position while playing.
    LaunchedEffect(isPlaying) {
        var lastPos = -1L
        var stationaryCount = 0
        while (isActive && isPlaying) {
            val pos = runCatching { adapter.getPosition() }.getOrDefault(positionMs)
            positionMs = pos

            // 200ms tolerance end-detection.
            if (durationMs > 0 && pos >= durationMs - END_OF_PLAYBACK_TOLERANCE_MS) {
                handleEnd(adapter)
                isPlaying = false
                positionMs = 0
                break
            }
            // Stationary-position fallback for OEMs with flaky OnCompletionListener.
            if (pos == lastPos) {
                stationaryCount++
                if (stationaryCount >= STATIONARY_TICKS_TO_END) {
                    handleEnd(adapter)
                    isPlaying = false
                    positionMs = 0
                    break
                }
            } else {
                stationaryCount = 0
                lastPos = pos
            }
            delay(POSITION_TICK_MS)
        }
    }

    DisposableEffect(adapter) {
        onDispose {
            scope.launch { runCatching { adapter.cleanup() } }
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Time label — positionMs / durationMs.
        Text(
            text = "${formatHms((positionMs / 1000).toInt())} / ${formatHms((durationMs / 1000).toInt())}",
            color = foreground,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )

        // Waveform with dashed background line.
        WaveformWithProgress(
            amplitudes = amplitudes,
            playedRatio = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
            color = foreground,
            dashColor = theme.mutedForeground.copy(alpha = 0.6f),
            modifier = Modifier
                .weight(1f)
                .height(24.dp),
        )

        // Outlined Play / Pause (no circle).
        Icon(
            imageVector = if (isPlaying) RayaIcons.pause(foreground) else RayaIcons.play(foreground),
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.Unspecified,
            modifier = Modifier
                .size(22.dp)
                .clickableNoRipple {
                    if (!loaded) return@clickableNoRipple
                    scope.launch {
                        if (isPlaying) {
                            runCatching { adapter.pause() }
                            isPlaying = false
                        } else {
                            runCatching { adapter.play() }
                            isPlaying = true
                        }
                    }
                },
        )
    }
}

private suspend fun handleEnd(adapter: AudioPlayerAdapter) {
    runCatching {
        adapter.pause()
        adapter.seekTo(0)
    }
}

/**
 * Static waveform overlaying a dashed background line. Bars below [SILENCE_THRESHOLD] are
 * skipped so the dashed line shows through — that's the design from iOS.
 */
@Composable
private fun WaveformWithProgress(
    amplitudes: FloatArray?,
    playedRatio: Float,
    color: Color,
    dashColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        // Always draw the dashed line first; bars go on top.
        val centerY = size.height / 2f
        drawLine(
            color = dashColor,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 4.dp.toPx()), 0f),
        )

        if (amplitudes == null || amplitudes.isEmpty()) return@Canvas

        val barWidthPx = 2.dp.toPx()
        val gapPx = 2.dp.toPx()
        val pitchPx = barWidthPx + gapPx
        val barCount = (size.width / pitchPx).toInt().coerceAtLeast(1)
        val downsampled = downsample(amplitudes, barCount.coerceAtMost(WAVEFORM_BUCKETS))
        val peak = max(SILENCE_THRESHOLD, downsampled.max())
        val maxBarHeightPx = 22.dp.toPx()
        val minBarHeightPx = 4.dp.toPx()
        val cornerRadius = CornerRadius(1.dp.toPx())
        val playedThreshold = playedRatio * downsampled.size

        var x = 0f
        downsampled.forEachIndexed { idx, amp ->
            if (amp < SILENCE_THRESHOLD) {
                x += pitchPx
                return@forEachIndexed
            }
            val h = max(minBarHeightPx, sqrt(amp / peak) * maxBarHeightPx)
            val top = centerY - h / 2f
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

