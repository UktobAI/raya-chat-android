package ai.teammates.rayachat.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ai.teammates.rayachat.core.adapters.AudioPlayerAdapter
import ai.teammates.rayachat.core.models.TypeMessage
import ai.teammates.rayachat.ui.adapters.AudioRecorderAdapter
import ai.teammates.rayachat.ui.adapters.ImagePickerAdapter

/**
 * Mode 3: Bottom sheet presentation.
 *
 * Presents the chat widget as a draggable bottom sheet. Expanded by default.
 * User can swipe down or tap close to dismiss.
 *
 * Usage:
 * ```kotlin
 * RayaChatBottomSheet.show(
 *     fragmentManager = supportFragmentManager,
 *     token = "your-bot-token"
 * )
 * ```
 */
class RayaChatBottomSheet : BottomSheetDialogFragment() {

    var imagePickerAdapter: ImagePickerAdapter? = null
    var audioRecorderAdapter: AudioRecorderAdapter? = null
    var audioPlayerAdapter: AudioPlayerAdapter? = null
    var onSessionStart: ((String) -> Unit)? = null
    var onSessionEnd: ((sessionId: String, messages: List<TypeMessage>) -> Unit)? = null
    var onMessageUpdate: ((sessionId: String, message: TypeMessage) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            (this as? BottomSheetDialog)?.behavior?.apply {
                state = BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
                isDraggable = true
            }
        }
    }

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
                    audioPlayerAdapter = audioPlayerAdapter,
                    onSessionStart = onSessionStart,
                    onSessionEnd = onSessionEnd,
                    onMessageUpdate = onMessageUpdate,
                    onError = onError,
                    onClose = { dismiss() },
                )
            }
        }
    }

    companion object {
        private const val TAG = "RayaChatBottomSheet"
        private const val ARG_TOKEN = "token"
        private const val ARG_LOCALE = "locale"

        /**
         * Show the chat widget as a bottom sheet.
         *
         * @param fragmentManager The host activity's FragmentManager.
         * @param token Bot token from Teammates.ai dashboard.
         * @param locale Language — "en" or "ar". Default: "en".
         * @return The created RayaChatBottomSheet instance (for setting adapters/callbacks).
         */
        @JvmStatic
        fun show(
            fragmentManager: FragmentManager,
            token: String,
            locale: String = "en",
        ): RayaChatBottomSheet {
            val sheet = RayaChatBottomSheet().apply {
                arguments = bundleOf(ARG_TOKEN to token, ARG_LOCALE to locale)
            }
            sheet.show(fragmentManager, TAG)
            return sheet
        }
    }
}
