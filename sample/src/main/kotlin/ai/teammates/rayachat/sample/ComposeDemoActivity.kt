package ai.teammates.rayachat.sample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ai.teammates.rayachat.sample.adapters.SampleImagePickerAdapter
import ai.teammates.rayachat.ui.RayaChatWidget

/** Mode 1 demo — Compose Widget with image picker adapter. */
class ComposeDemoActivity : ComponentActivity() {

    // Register BEFORE super.onCreate — ActivityResultLauncher requirement
    private lateinit var imagePickerAdapter: SampleImagePickerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must register before super.onCreate()
        imagePickerAdapter = SampleImagePickerAdapter.create(this)

        super.onCreate(savedInstanceState)
        setContent {
            RayaChatWidget(
                token = SAMPLE_TOKEN,
                locale = "en",
                imagePickerAdapter = imagePickerAdapter,
                onSessionStart = { id -> Log.d("Mode1", "Session started: $id") },
                onSessionEnd = { sid, msgs ->
                    Log.d("Mode1", "═══ SESSION ENDED ═══")
                    Log.d("Mode1", "Session ID: $sid")
                    Log.d("Mode1", "Messages: ${msgs.size}")
                    msgs.forEachIndexed { i, msg ->
                        Log.d("Mode1", "  ── Message #${i + 1} ──")
                        Log.d("Mode1", "  id: ${msg.id}")
                        Log.d("Mode1", "  sender: ${msg.sender} (${when (msg.sender) { 1 -> "USER"; 2 -> "BOT"; else -> "SYSTEM" }})")
                        Log.d("Mode1", "  type: ${msg.type} (${when (msg.type) { 1 -> "text"; 2 -> "audio"; 3 -> "image"; else -> "system" }})")
                        Log.d("Mode1", "  content: ${msg.content}")
                        Log.d("Mode1", "  createdAt: ${msg.createdAt}")
                        Log.d("Mode1", "  attachmentsJson: ${msg.attachmentsJson}")
                        Log.d("Mode1", "  audioJson: ${msg.audioJson}")
                    }
                    Log.d("Mode1", "═════════════════════")
                },
                onError = { err -> Log.w("Mode1", "Error: $err") },
                onClose = {
                    Log.d("Mode1", "Chat closed")
                    finish()
                },
            )
        }
    }
}
