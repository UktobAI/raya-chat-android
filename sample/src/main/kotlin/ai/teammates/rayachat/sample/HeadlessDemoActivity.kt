package ai.teammates.rayachat.sample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ai.teammates.rayachat.core.RayaChatClient
import ai.teammates.rayachat.core.RayaChatConfig
import ai.teammates.rayachat.core.models.UserInfo
import ai.teammates.rayachat.sample.headless.CustomChatScreen

/** Mode 4 demo — Headless with custom UI. Full control. */
class HeadlessDemoActivity : ComponentActivity() {

    private lateinit var client: RayaChatClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        client = RayaChatClient(
            context = applicationContext,
            config = RayaChatConfig(
                token = SAMPLE_TOKEN,
                locale = "en",
                onSessionStart = { id -> Log.d("Mode4", "Session started: $id") },
                onSessionEnd = { Log.d("Mode4", "Session ended") },
                onError = { err -> Log.w("Mode4", "Error: $err") },
            ),
        )

        setContent {
            CustomChatScreen(
                client = client,
                onClose = { finish() },
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        client.destroy()
    }
}
