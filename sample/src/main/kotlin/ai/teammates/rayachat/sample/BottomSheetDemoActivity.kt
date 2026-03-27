package ai.teammates.rayachat.sample

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.teammates.rayachat.ui.RayaChatBottomSheet

/** Mode 3 demo — BottomSheet. Chat slides up from bottom. */
class BottomSheetDemoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                BottomSheetDemoScreen(onOpenChat = ::openChat)
            }
        }
    }

    private fun openChat() {
        val sheet = RayaChatBottomSheet.show(
            fragmentManager = supportFragmentManager,
            token = SAMPLE_TOKEN,
            locale = "en",
        )
        sheet.onSessionStart = { id -> Log.d("Mode3", "Session started: $id") }
        sheet.onError = { err -> Log.w("Mode3", "Error: $err") }
    }
}

@Composable
private fun BottomSheetDemoScreen(onOpenChat: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14))
            .statusBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Your App Screen", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text("The chat will slide up as a bottom sheet", fontSize = 14.sp, color = Color(0xFF71717A))
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onOpenChat,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
            ) {
                Text("Open Support Chat", color = Color.Black, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
