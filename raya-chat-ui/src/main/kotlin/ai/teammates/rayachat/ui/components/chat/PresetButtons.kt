@file:OptIn(ExperimentalLayoutApi::class)

package ai.teammates.rayachat.ui.components.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.teammates.rayachat.ui.theme.LocalRayaTheme
import ai.teammates.rayachat.ui.theme.RayaTypography

/** Wrapping pill-shaped suggestion buttons, right-aligned. */
@Composable
fun PresetButtons(
    presets: List<String>,
    onPress: (String) -> Unit,
) {
    if (presets.isEmpty()) return

    val theme = LocalRayaTheme.current

    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = Int.MAX_VALUE,
    ) {
        presets.forEach { text ->
            OutlinedButton(
                onClick = { onPress(text) },
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, theme.presetBorder),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = theme.presetBg),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text(text = text, style = RayaTypography.buttonSmall, color = theme.presetText)
            }
        }
    }
}
