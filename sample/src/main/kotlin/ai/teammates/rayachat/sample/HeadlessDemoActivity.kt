package ai.teammates.rayachat.sample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ai.teammates.rayachat.core.RayaChatClient
import ai.teammates.rayachat.core.RayaChatConfig
import ai.teammates.rayachat.core.adapters.ImagePickerAdapter
import ai.teammates.rayachat.sample.adapters.SampleImagePickerAdapter
import ai.teammates.rayachat.sample.headless.CustomChatScreen

/** Mode 4 demo — Headless with custom UI + image picker. */
class HeadlessDemoActivity : ComponentActivity() {

    private lateinit var client: RayaChatClient
    private lateinit var imagePickerAdapter: SampleImagePickerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must register before super.onCreate
        imagePickerAdapter = SampleImagePickerAdapter.create(this)

        super.onCreate(savedInstanceState)

        client = RayaChatClient(
            context = applicationContext,
            config = RayaChatConfig(
                token = SAMPLE_TOKEN,
                locale = "en",
                onSessionStart = { id -> Log.d("Mode4", "Session started: $id") },
                onSessionEnd = { sid, msgs -> Log.d("Mode4", "Session ended: $sid (${msgs.size} messages)") },
                onError = { err -> Log.w("Mode4", "Error: $err") },
            ),
        )

        setContent {
            CustomChatScreen(
                client = client,
                imagePickerAdapter = imagePickerAdapter,
                onClose = { finish() },
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        client.destroy()
    }
}
