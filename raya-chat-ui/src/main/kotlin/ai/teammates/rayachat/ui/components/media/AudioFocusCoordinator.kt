package ai.teammates.rayachat.ui.components.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log

/**
 * Centralized audio focus management for recording and playback. Mirror of iOS
 * `AudioSessionCoordinator`. Uses [AudioFocusRequest] on API 26+ and the deprecated
 * [AudioManager.requestAudioFocus] callback API on API 24-25.
 *
 * Recording requests transient focus that may duck other audio (so users can record
 * while music plays at lower volume). Playback requests full focus.
 *
 * The recorder subscribes [onTransientLoss] to pause itself when the system grants
 * focus to a phone call or voice assistant. **Behavior matches iOS: stays paused
 * after the interruption ends — user must explicitly resume.**
 */
internal object AudioFocusCoordinator {

    private const val TAG = "RayaChat.AudioFocus"

    private var request: AudioFocusRequest? = null
    private var listener: AudioManager.OnAudioFocusChangeListener? = null

    /** Set by the active recorder; called when focus is lost transiently. */
    @Volatile
    var onTransientLoss: (() -> Unit)? = null

    fun enterRecording(context: Context): Boolean = enter(
        context,
        usage = AudioAttributes.USAGE_VOICE_COMMUNICATION,
        contentType = AudioAttributes.CONTENT_TYPE_SPEECH,
        focusGain = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
    )

    fun enterPlayback(context: Context): Boolean = enter(
        context,
        usage = AudioAttributes.USAGE_MEDIA,
        contentType = AudioAttributes.CONTENT_TYPE_MUSIC,
        focusGain = AudioManager.AUDIOFOCUS_GAIN,
    )

    fun exit(context: Context) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            request?.let { am.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            listener?.let { am.abandonAudioFocus(it) }
        }
        request = null
        listener = null
    }

    private fun enter(context: Context, usage: Int, contentType: Int, focusGain: Int): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false

        val l = AudioManager.OnAudioFocusChangeListener { change ->
            when (change) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    onTransientLoss?.invoke()
                }
            }
        }
        listener = l

        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(contentType)
                .build()
            val req = AudioFocusRequest.Builder(focusGain)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener(l)
                .build()
            request = req
            am.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(l, AudioManager.STREAM_MUSIC, focusGain) ==
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }

        if (!granted) Log.w(TAG, "Audio focus request denied (focusGain=$focusGain)")
        return granted
    }
}
