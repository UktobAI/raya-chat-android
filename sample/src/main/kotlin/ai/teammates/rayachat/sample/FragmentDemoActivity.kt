package ai.teammates.rayachat.sample

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import ai.teammates.rayachat.ui.RayaChatFragment

/** Mode 2 demo — Fragment in XML layout. For apps using XML views. */
class FragmentDemoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ai.teammates.rayachat.sample.R.layout.activity_fragment_demo)

        if (savedInstanceState == null) {
            val fragment = RayaChatFragment.newInstance(SAMPLE_TOKEN, "en").apply {
                onSessionStart = { id -> Log.d("Mode2", "Session started: $id") }
                onSessionEnd = { sid, msgs -> Log.d("Mode2", "Session ended: $sid (${msgs.size} messages)") }
                onError = { err -> Log.w("Mode2", "Error: $err") }
                onClose = {
                    Log.d("Mode2", "Chat closed")
                    finish()
                }
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.chat_container, fragment)
                .commit()
        }
    }
}
