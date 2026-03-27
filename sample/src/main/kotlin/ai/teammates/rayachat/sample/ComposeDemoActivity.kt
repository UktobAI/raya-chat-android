package ai.teammates.rayachat.sample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ai.teammates.rayachat.ui.RayaChatWidget

/** Mode 1 demo — Compose Widget. Simplest integration. */
class ComposeDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RayaChatWidget(
                token = SAMPLE_TOKEN,
                locale = "en",
                onSessionStart = { id -> Log.d("Mode1", "Session started: $id") },
                onSessionEnd = { Log.d("Mode1", "Session ended") },
                onError = { err -> Log.w("Mode1", "Error: $err") },
                onClose = {
                    Log.d("Mode1", "Chat closed")
                    finish()
                },
            )
        }
    }
}
