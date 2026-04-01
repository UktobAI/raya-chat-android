package ai.teammates.rayachat.ui.components.chat

import android.content.Context
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon

/**
 * Shared Markwon instance — ONE per composition tree, not one per MessageBubble.
 * Provided via CompositionLocal from RayaChatTheme or RayaChatWidget.
 *
 * If not provided (headless mode), falls back to creating one per context via remember.
 */
val LocalMarkwon = compositionLocalOf<Markwon?> { null }

/** Create a Markwon singleton for a given Context. Thread-safe. */
object MarkwonProvider {
    @Volatile
    private var instance: Markwon? = null

    fun get(context: Context): Markwon {
        return instance ?: synchronized(this) {
            instance ?: Markwon.builder(context.applicationContext).build().also { instance = it }
        }
    }
}

/**
 * Renders markdown text using Markwon via AndroidView(TextView) bridge.
 * Uses a shared Markwon instance — no per-bubble allocation.
 */
@Composable
fun MarkdownText(
    content: String,
    textColor: Color,
    fontSize: TextUnit = 14.sp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // Use CompositionLocal if provided, otherwise singleton
    val markwon = LocalMarkwon.current ?: remember { MarkwonProvider.get(context) }
    val argbColor = textColor.toArgb()
    val fontSizePx = with(LocalDensity.current) { fontSize.toPx() }

    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(argbColor)
                textSize = fontSizePx / ctx.resources.displayMetrics.scaledDensity
                setLineSpacing(0f, 1.4f)
            }
        },
        update = { textView ->
            textView.setTextColor(argbColor)
            markwon.setMarkdown(textView, content)
        },
        modifier = modifier,
    )
}
