package ai.teammates.rayachat.ui.components.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow

/**
 * Snackbar-based toast for connection errors and status messages.
 * Matches the RN SDK's ToastProvider behavior.
 */
@Composable
fun rememberToastState(): SnackbarHostState = remember { SnackbarHostState() }

/**
 * Observe an error flow and show snackbar notifications.
 */
@Composable
fun ObserveErrors(
    errors: Flow<String>,
    snackbarHostState: SnackbarHostState,
) {
    LaunchedEffect(Unit) {
        errors.collect { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
            )
        }
    }
}
