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
                onSessionEnd = { sid, msgs -> Log.d("Mode1", "Session ended: $sid (${msgs.size} messages)") },
                onError = { err -> Log.w("Mode1", "Error: $err") },
                onClose = {
                    Log.d("Mode1", "Chat closed")
                    finish()
                },
            )
        }
    }
}
