package ai.teammates.rayachat.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import ai.teammates.rayachat.core.models.TypeMessage
import ai.teammates.rayachat.ui.adapters.AudioRecorderAdapter
import ai.teammates.rayachat.ui.adapters.ImagePickerAdapter

/**
 * Mode 2: Fragment wrapper for XML layout apps.
 *
 * Wraps [RayaChatWidget] in a Fragment that can be placed inside any XML layout's
 * FrameLayout or FragmentContainerView. Internally uses ComposeView to render
 * Jetpack Compose — the host app doesn't need to know about Compose.
 *
 * Usage:
 * ```kotlin
 * supportFragmentManager.beginTransaction()
 *     .replace(R.id.container, RayaChatFragment.newInstance("your-bot-token"))
 *     .commit()
 * ```
 */
class RayaChatFragment : Fragment() {

    /** Optional adapters — set these before adding the fragment. */
    var imagePickerAdapter: ImagePickerAdapter? = null
    var audioRecorderAdapter: AudioRecorderAdapter? = null

    /** Optional callbacks — set these before adding the fragment. */
    var onSessionStart: ((String) -> Unit)? = null
    var onSessionEnd: ((sessionId: String, messages: List<TypeMessage>) -> Unit)? = null
    var onMessageUpdate: ((sessionId: String, message: TypeMessage) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onClose: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val token = arguments?.getString(ARG_TOKEN) ?: ""
        val locale = arguments?.getString(ARG_LOCALE) ?: "en"

        return ComposeView(requireContext()).apply {
            setContent {
                RayaChatWidget(
                    token = token,
                    locale = locale,
                    imagePickerAdapter = imagePickerAdapter,
                    audioRecorderAdapter = audioRecorderAdapter,
                    onSessionStart = onSessionStart,
                    onSessionEnd = onSessionEnd,
                    onMessageUpdate = onMessageUpdate,
                    onError = onError,
                    onClose = onClose,
                )
            }
        }
    }

    companion object {
        private const val ARG_TOKEN = "token"
        private const val ARG_LOCALE = "locale"

        /**
         * Create a new RayaChatFragment instance.
         *
         * @param token Bot token from Teammates.ai dashboard.
         * @param locale Language — "en" or "ar". Default: "en".
         */
        @JvmStatic
        fun newInstance(token: String, locale: String = "en"): RayaChatFragment {
            return RayaChatFragment().apply {
                arguments = bundleOf(ARG_TOKEN to token, ARG_LOCALE to locale)
            }
        }
    }
}
