package ai.teammates.rayachat.ui.components.chat

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.teammates.rayachat.ui.components.common.RayaIcons
import ai.teammates.rayachat.ui.theme.LocalRayaTheme

/** Floating circular button that appears when user scrolls up. */
@Composable
fun ScrollToBottomButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalRayaTheme.current

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            FloatingActionButton(
                onClick = onClick,
                shape = CircleShape,
                containerColor = theme.surface,
                contentColor = theme.foreground,
                modifier = Modifier.padding(16.dp).size(40.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
            ) {
                Icon(
                    imageVector = RayaIcons.arrowDown(theme.foreground),
                    contentDescription = "Scroll to bottom",
                    tint = theme.foreground,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
