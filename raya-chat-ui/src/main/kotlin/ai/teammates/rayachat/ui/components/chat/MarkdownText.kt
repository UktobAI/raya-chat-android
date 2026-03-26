package ai.teammates.rayachat.ui.components.chat

import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon

/**
 * Renders markdown text using Markwon via AndroidView(TextView) bridge.
 * Supports bold, italic, code, links, lists, headings, blockquotes.
 */
@Composable
fun MarkdownText(
    content: String,
    textColor: Color,
    fontSize: TextUnit = 14.sp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val markwon = remember { Markwon.builder(context).build() }
    val argbColor = textColor.toArgb()
    val fontSizePx = with(androidx.compose.ui.platform.LocalDensity.current) { fontSize.toPx() }

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
