package ai.teammates.rayachat.sample.headless

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.teammates.rayachat.core.RayaChatClient
import ai.teammates.rayachat.core.models.BotConfigProps
import ai.teammates.rayachat.core.models.TypeMessage
import ai.teammates.rayachat.core.models.UserInfo
import kotlinx.coroutines.launch

private val Charcoal = Color(0xFF0F0F14)
private val Surface = Color(0xFF1A1A24)
private val Accent = Color(0xFFEF4444)
private val TextPrimary = Color(0xFFF1F1F0)
private val TextMuted = Color(0xFF71717A)
private val UserBubbleBg = Color(0xFFEF4444)
private val BotBubbleBg = Color(0xFF1A1A24)

/**
 * Fully custom chat UI using RayaChatClient (headless).
 * Zero SDK UI components — everything built from scratch.
 */
@Composable
fun CustomChatScreen(client: RayaChatClient, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var chatStarted by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(TextFieldValue("")) }

    val messages by client.messages.collectAsState()
    val currentMessage by client.currentMessage.collectAsState()
    val loading by client.loading.collectAsState()
    val isConnected by client.isConnected.collectAsState()
    val presets by client.presets.collectAsState()

    val listState = rememberLazyListState()

    // Auto-scroll
    LaunchedEffect(messages.size, currentMessage) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // Build display list
    val displayMessages = remember(messages, currentMessage) {
        val list = messages.toMutableList()
        if (currentMessage.isNotEmpty()) {
            list.add(TypeMessage(id = "__stream__", sender = 2, type = 1, content = currentMessage))
        }
        list
    }

    if (!chatStarted) {
        // Welcome screen
        Box(
            modifier = Modifier.fillMaxSize().background(Charcoal).statusBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(60.dp).clip(CircleShape).background(Accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("R", fontSize = 24.sp, fontWeight = FontWeight.Light, color = Color.White)
                }
                Spacer(Modifier.height(16.dp))
                Text("Custom Headless Chat", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text("Built with RayaChatClient — zero SDK UI", fontSize = 12.sp, color = TextMuted)
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = {
                        scope.launch {
                            val config = client.fetchBotConfig()
                            client.connect(UserInfo("", "", ""), config)
                            chatStarted = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    shape = RoundedCornerShape(50),
                ) {
                    Text("Start Chat", fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
        return
    }

    // Chat screen
    Column(
        modifier = Modifier.fillMaxSize().background(Charcoal).statusBarsPadding().imePadding()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().background(Surface).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(Accent), contentAlignment = Alignment.Center) {
                    Text("R", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Light)
                }
                Column {
                    Text("Headless Chat", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Text(if (isConnected) "Online" else "Connecting...", fontSize = 10.sp, color = TextMuted)
                }
            }
            TextButton(onClick = onClose) { Text("Close", color = Accent, fontSize = 12.sp) }
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(8.dp),
        ) {
            items(displayMessages, key = { it.id }) { msg ->
                val isUser = msg.sender == 1
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                ) {
                    Text(
                        text = msg.content ?: "",
                        fontSize = 14.sp,
                        color = if (isUser) Color.White else TextPrimary,
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .background(
                                if (isUser) UserBubbleBg else BotBubbleBg,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            // Loading dots
            if (loading && currentMessage.isEmpty()) {
                item {
                    Text("● ● ●", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                }
            }

            // Presets
            if (presets.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    ) {
                        presets.forEach { preset ->
                            OutlinedButton(
                                onClick = { client.sendPreset(preset) },
                                shape = RoundedCornerShape(50),
                            ) {
                                Text(preset, fontSize = 11.sp, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }

        // Composer
        Row(
            modifier = Modifier.fillMaxWidth().background(Surface).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 14.sp),
                cursorBrush = SolidColor(Accent),
                modifier = Modifier
                    .weight(1f)
                    .background(Charcoal, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                decorationBox = { inner ->
                    Box {
                        if (text.text.isEmpty()) Text("Type a message...", color = TextMuted, fontSize = 14.sp)
                        inner()
                    }
                },
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (text.text.isNotBlank()) {
                        client.sendMessage(text.text.trim())
                        text = TextFieldValue("")
                    }
                },
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Accent),
            ) {
                Text("→", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
