package ai.teammates.rayachat.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ai.teammates.rayachat.core.RayaChatConfig

/**
 * Factory for [RayaChatViewModel] that passes [Context] and [RayaChatConfig].
 * Used with [ViewModelProvider] so the ViewModel survives configuration changes (rotation).
 */
internal class RayaChatViewModelFactory(
    private val context: Context,
    private val config: RayaChatConfig,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RayaChatViewModel::class.java)) {
            return RayaChatViewModel(context.applicationContext, config) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
