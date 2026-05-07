package ai.teammates.rayachat.sample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                // Request mic on first launch so voice-note testing works in all demo modes.
                // SDK auto-hides the mic button if denied; user can re-grant via system settings.
                val context = LocalContext.current
                val micLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { /* result intentionally ignored — SDK handles denied state */ }
                LaunchedEffect(Unit) {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }

                MainScreen(
                    onMode1 = { startActivity(Intent(this, ComposeDemoActivity::class.java)) },
                    onMode2 = { startActivity(Intent(this, FragmentDemoActivity::class.java)) },
                    onMode3 = { startActivity(Intent(this, BottomSheetDemoActivity::class.java)) },
                    onMode4 = { startActivity(Intent(this, HeadlessDemoActivity::class.java)) },
                )
            }
        }
    }
}

@Composable
private fun MainScreen(
    onMode1: () -> Unit,
    onMode2: () -> Unit,
    onMode3: () -> Unit,
    onMode4: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14))
            .padding(24.dp)
            .statusBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Raya Chat SDK",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            "Android Sample App",
            fontSize = 14.sp,
            color = Color(0xFF71717A),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "v0.1.2",
            fontSize = 12.sp,
            color = Color(0xFF3F3F46),
        )

        Spacer(Modifier.height(48.dp))

        DemoButton("Mode 1 — Compose Widget", "RayaChatWidget(token = \"...\")", Color(0xFF6C5CE7), onMode1)
        Spacer(Modifier.height(16.dp))
        DemoButton("Mode 2 — Fragment", "RayaChatFragment.newInstance(\"...\")", Color(0xFF00CEC9), onMode2)
        Spacer(Modifier.height(16.dp))
        DemoButton("Mode 3 — BottomSheet", "RayaChatBottomSheet.show(...)", Color(0xFFF59E0B), onMode3)
        Spacer(Modifier.height(16.dp))
        DemoButton("Mode 4 — Headless", "RayaChatClient(context, config)", Color(0xFFEF4444), onMode4)
    }
}

@Composable
private fun DemoButton(title: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A24)),
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = accent)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, fontSize = 11.sp, color = Color(0xFF71717A), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
    }
}
