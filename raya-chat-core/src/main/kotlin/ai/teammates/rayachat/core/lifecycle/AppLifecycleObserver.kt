package ai.teammates.rayachat.core.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Observes app foreground/background transitions via ProcessLifecycleOwner.
 * Heartbeat stops on background, restarts on foreground.
 */
class AppLifecycleObserver(
    private val onForeground: (backgroundDurationMs: Long) -> Unit,
    private val onBackground: () -> Unit,
) : DefaultLifecycleObserver {

    private var backgroundedAt: Long = 0L
    private var isInForeground = true

    val isForeground: Boolean get() = isInForeground

    fun register() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun unregister() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        // App came to foreground
        val wasBackground = !isInForeground
        isInForeground = true

        if (wasBackground && backgroundedAt > 0) {
            val duration = System.currentTimeMillis() - backgroundedAt
            backgroundedAt = 0L
            onForeground(duration)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        // App went to background
        isInForeground = false
        backgroundedAt = System.currentTimeMillis()
        onBackground()
    }
}
