@file:OptIn(ExperimentalLayoutApi::class)

package ai.teammates.rayachat.sample.headless

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.teammates.rayachat.core.RayaChatClient
import ai.teammates.rayachat.core.Constants
import ai.teammates.rayachat.core.models.*
import ai.teammates.rayachat.core.adapters.ImagePickerAdapter
import kotlinx.serialization.json.Json
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Nocturne Velvet palette ──
private val Midnight = Color(0xFF0C0C10)
private val Onyx = Color(0xFF141418)
private val Slate = Color(0xFF1C1C22)
private val Graphite = Color(0xFF26262E)
private val Amber = Color(0xFFD4A056)
private val AmberDim = Color(0x1AD4A056)
private val AmberGlow = Color(0x33D4A056)
private val Cream = Color(0xFFF5EDE0)
private val CreamMuted = Color(0xB3F5EDE0)
private val Stone = Color(0xFF6B6B78)
private val Rose = Color(0xFFD4566A)
private val Emerald = Color(0xFF4ADE80)

/**
 * Nocturne Velvet — headless chat demo using ALL RayaChatClient features.
 * Luxury hotel concierge aesthetic. Zero SDK UI components.
 */
@Composable
fun CustomChatScreen(
    client: RayaChatClient,
    imagePickerAdapter: ai.teammates.rayachat.core.adapters.ImagePickerAdapter? = null,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var chatStarted by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(TextFieldValue("")) }
    var selectedImages by remember { mutableStateOf<List<ImageAsset>>(emptyList()) }

    // ── All headless state ──
    val messages by client.messages.collectAsState()
    val currentMessage by client.currentMessage.collectAsState()
    val loading by client.loading.collectAsState()
    val status by client.status.collectAsState()
    val info by client.info.collectAsState()
    val isConnected by client.isConnected.collectAsState()
    val isOnline by client.isOnline.collectAsState()
    val presets by client.presets.collectAsState()
    val commandData by client.commandData.collectAsState()
    val showHumanAgentBtn by client.showHumanAgentBtn.collectAsState()
    val sessionCloseInfo by client.sessionCloseInfo.collectAsState()
    val sessionId by client.currentSessionId.collectAsState()

    val listState = rememberLazyListState()
    val isStreaming = currentMessage.isNotEmpty()

    val displayMessages = remember(messages, currentMessage) {
        val list = messages.toMutableList()
        if (currentMessage.isNotEmpty()) {
            list.add(TypeMessage(id = "__stream__", sender = 2, type = 1, content = currentMessage))
        }
        list
    }

    val totalItems = displayMessages.size + 1 // +1 for footer

    // Auto-scroll: instant for streaming, animated for new messages
    LaunchedEffect(displayMessages.size) {
        if (totalItems > 0) listState.animateScrollToItem(totalItems - 1, scrollOffset = Int.MAX_VALUE)
    }
    LaunchedEffect(currentMessage, presets.size, commandData, info, loading) {
        if (totalItems > 0) listState.scrollToItem(totalItems - 1, scrollOffset = Int.MAX_VALUE)
    }

    // ── Welcome screen ──
    if (!chatStarted) {
        WelcomeScreen(
            sessionCloseInfo = sessionCloseInfo,
            onStart = {
                scope.launch {
                    val config = client.fetchBotConfig()
                    client.connect(UserInfo("", "", ""), config)
                    chatStarted = true
                }
            },
            onClearWarning = { client.clearSessionCloseInfo() },
        )
        return
    }

    // ── Chat screen ──
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Midnight)
            .statusBarsPadding()
            .imePadding()
    ) {
        // Header
        ChatHeader(isConnected = isConnected, isOnline = isOnline, sessionId = sessionId, onClose = {
            keyboard?.hide()
            scope.launch {
                client.endSession()
                chatStarted = false
                onClose()
            }
        })

        // Messages + footer
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            items(displayMessages, key = { it.id }) { msg ->
                when (msg.type) {
                    4 -> SystemMessage(msg) // AGENT_ACTIVITY
                    else -> ChatBubble(msg)
                }
            }

            // Footer: typing, info, escalation, commands, presets
            item(key = "__footer__") {
                FooterContent(
                    loading = loading,
                    status = status,
                    info = info,
                    isStreaming = isStreaming,
                    commandData = commandData,
                    presets = presets,
                    showHumanAgentBtn = showHumanAgentBtn,
                    onSendPreset = { client.sendPreset(it) },
                    onSendCommandResponse = { cmd, resp -> client.sendCommandResponse(cmd, resp) },
                    onEscalate = { client.sendMessage("/human_agent") },
                    onEndSession = {
                        scope.launch {
                            client.endSession()
                            chatStarted = false
                        }
                    },
                )
            }
        }

        // Image preview
        if (selectedImages.isNotEmpty()) {
            ImagePreviewRow(
                images = selectedImages,
                onRemove = { index -> selectedImages = selectedImages.filterIndexed { i, _ -> i != index } },
            )
        }

        // Composer
        Composer(
            text = text,
            onTextChange = { text = it },
            hasImages = selectedImages.isNotEmpty(),
            hasImageAdapter = imagePickerAdapter != null,
            onPickImages = {
                imagePickerAdapter?.let { adapter ->
                    scope.launch {
                        try {
                            val remaining = Constants.MAX_IMAGES_PER_MESSAGE - selectedImages.size
                            if (remaining <= 0) return@launch
                            val picked = adapter.pickImages(remaining)
                            if (picked.isNotEmpty()) {
                                selectedImages = (selectedImages + picked).take(Constants.MAX_IMAGES_PER_MESSAGE)
                            }
                        } catch (_: Exception) {}
                    }
                }
            },
            onSend = {
                if (selectedImages.isNotEmpty()) {
                    val payloads = selectedImages.map { img ->
                        ImagePayload(name = img.name, type = img.type, base64 = img.base64, uri = img.uri)
                    }
                    client.sendImages(payloads, text.text.trim())
                    selectedImages = emptyList()
                    text = TextFieldValue("")
                } else if (text.text.isNotBlank()) {
                    client.sendMessage(text.text.trim())
                    text = TextFieldValue("")
                }
            },
        )
    }
}

// ══════════════════════════════════════════════
// WELCOME
// ══════════════════════════════════════════════

@Composable
private fun WelcomeScreen(
    sessionCloseInfo: SessionCloseInfo?,
    onStart: () -> Unit,
    onClearWarning: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Onyx, Midnight, Midnight))
            )
            .statusBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp),
        ) {
            // Amber ring avatar
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .drawBehind {
                        drawCircle(Amber, radius = size.minDimension / 2, style = Stroke(width = 1.5.dp.toPx()))
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("N", fontSize = 28.sp, fontWeight = FontWeight.Thin, color = Amber, letterSpacing = 2.sp)
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "Nocturne Concierge",
                fontSize = 22.sp,
                fontWeight = FontWeight.Light,
                color = Cream,
                letterSpacing = 3.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Headless Mode — All Features",
                fontSize = 11.sp,
                color = Stone,
                letterSpacing = 2.sp,
            )

            // Session close warning
            if (sessionCloseInfo != null) {
                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Rose.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .border(0.5.dp, Rose.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                ) {
                    Text(
                        sessionCloseInfo.message,
                        fontSize = 12.sp,
                        color = Rose,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onClearWarning) {
                    Text("Dismiss", fontSize = 11.sp, color = Stone, letterSpacing = 1.sp)
                }
            }

            Spacer(Modifier.height(36.dp))

            // Start button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Amber)
                    .clickable(onClick = onStart)
                    .padding(horizontal = 36.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Begin Conversation",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Midnight,
                    letterSpacing = 1.5.sp,
                )
            }
        }
    }
}

// ══════════════════════════════════════════════
// HEADER
// ══════════════════════════════════════════════

@Composable
private fun ChatHeader(isConnected: Boolean, isOnline: Boolean, sessionId: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Onyx)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Avatar with status dot
            Box {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Graphite),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("N", fontSize = 15.sp, fontWeight = FontWeight.Thin, color = Amber, letterSpacing = 1.sp)
                }
                // Status dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Onyx)
                        .padding(1.5.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) Emerald else if (isOnline) Amber else Rose)
                )
            }

            Column {
                Text("Nocturne", fontSize = 15.sp, fontWeight = FontWeight.Light, color = Cream, letterSpacing = 1.sp)
                Text(
                    when {
                        !isOnline -> "Offline"
                        isConnected -> "Active"
                        else -> "Reconnecting..."
                    },
                    fontSize = 10.sp,
                    color = if (isConnected) Stone else Rose,
                    letterSpacing = 0.5.sp,
                )
            }
        }

        // End session
        Text(
            "End",
            fontSize = 12.sp,
            color = Rose,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onClose)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }

    // Thin amber divider
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(Amber.copy(alpha = 0.15f)))
}

// ══════════════════════════════════════════════
// MESSAGE BUBBLE
// ══════════════════════════════════════════════

private val attachmentJson = Json { ignoreUnknownKeys = true }

@Composable
private fun ChatBubble(message: TypeMessage) {
    val isUser = message.sender == 1
    val content = message.content
    val hasContent = !content.isNullOrBlank()

    // Parse attachments from JSON
    val attachments = remember(message.attachmentsJson) {
        message.attachmentsJson?.let {
            try { attachmentJson.decodeFromString<List<Attachment>>(it) } catch (_: Exception) { null }
        }
    }
    val hasImages = !attachments.isNullOrEmpty()

    // Skip if no content AND no images
    if (!hasContent && !hasImages) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier.size(24.dp).clip(CircleShape).background(Graphite),
                contentAlignment = Alignment.Center,
            ) {
                Text("N", fontSize = 9.sp, color = Amber, fontWeight = FontWeight.Thin)
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            // Images — above text bubble for user, inside bubble for bot
            if (hasImages && isUser) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                ) {
                    attachments!!.forEach { att ->
                        AsyncImage(
                            model = att.url,
                            contentDescription = "Image",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                if (hasContent) Spacer(Modifier.height(6.dp))
            }

            // Text bubble (or bot images inside bubble)
            if (hasContent || (hasImages && !isUser)) {
                val bubbleShape = if (isUser) {
                    RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
                } else {
                    RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
                }

                Column(
                    modifier = Modifier
                        .background(if (isUser) Amber else Slate, bubbleShape)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    // Bot images inside bubble
                    if (hasImages && !isUser) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            attachments!!.forEach { att ->
                                AsyncImage(
                                    model = att.url,
                                    contentDescription = "Image",
                                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                        if (hasContent) Spacer(Modifier.height(8.dp))
                    }

                    if (hasContent) {
                        Text(
                            text = content!!,
                            fontSize = 14.sp,
                            color = if (isUser) Midnight else Cream,
                            lineHeight = 21.sp,
                        )
                    }
                }
            }

            // Timestamp
            val ts = message.createdAt?.toLongOrNull()
            if (ts != null && ts > 0) {
                Text(
                    ai.teammates.rayachat.core.util.formatLocalTime(epochSeconds = ts),
                    fontSize = 9.sp,
                    color = Stone.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun SystemMessage(message: TypeMessage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            message.content ?: "",
            fontSize = 10.sp,
            color = Stone,
            letterSpacing = 0.5.sp,
            modifier = Modifier
                .background(Graphite, RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

// ══════════════════════════════════════════════
// FOOTER: typing, info, escalation, commands, presets
// ══════════════════════════════════════════════

@Composable
private fun FooterContent(
    loading: Boolean,
    status: String?,
    info: String?,
    isStreaming: Boolean,
    commandData: CommandData?,
    presets: List<String>,
    showHumanAgentBtn: Boolean,
    onSendPreset: (String) -> Unit,
    onSendCommandResponse: (String, Any) -> Unit,
    onEscalate: () -> Unit,
    onEndSession: () -> Unit,
) {
    Column {
        // Typing indicator
        if (loading && !isStreaming && info == null) {
            TypingDots()
        }

        // Status text (Searching..., Thinking...)
        if (status != null && !isStreaming) {
            Text(
                status,
                fontSize = 10.sp,
                color = Amber.copy(alpha = 0.7f),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 32.dp, bottom = 8.dp),
            )
        }

        // Info (hourglass: "Waiting for human agent...")
        if (info != null) {
            Row(
                modifier = Modifier.padding(start = 32.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Animated hourglass
                val rotation by rememberInfiniteTransition(label = "hg").animateFloat(
                    initialValue = 0f, targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "hgr"
                )
                Text("⏳", fontSize = 14.sp, modifier = Modifier.graphicsLayer(rotationZ = rotation))
                Text(info, fontSize = 12.sp, color = CreamMuted, lineHeight = 18.sp)
            }
        }

        // Escalation button
        if (showHumanAgentBtn) {
            Box(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .align(Alignment.End)
                    .border(0.5.dp, Amber.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .clickable(onClick = onEscalate)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text("Connect with a person", fontSize = 11.sp, color = Amber, letterSpacing = 0.5.sp)
            }
        }

        // Command UIs
        commandData?.let { cmd ->
            when (cmd.content) {
                "end_session" -> EndSessionCommand(cmd, onSendCommandResponse)
                "rate_conversation" -> RatingCommand(cmd, onSendCommandResponse)
                "submit_feedback" -> FeedbackCommand(cmd, onSendCommandResponse)
                "feedback_received" -> CountdownCommand(cmd, onEndSession)
            }
        }

        // Presets
        if (presets.isNotEmpty() && commandData == null) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.forEach { preset ->
                    Text(
                        preset,
                        fontSize = 12.sp,
                        color = CreamMuted,
                        letterSpacing = 0.3.sp,
                        modifier = Modifier
                            .border(0.5.dp, Stone.copy(alpha = 0.3f), RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .clickable { onSendPreset(preset) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════
// COMMAND UIs
// ══════════════════════════════════════════════

@Composable
private fun EndSessionCommand(cmd: CommandData, onResponse: (String, Any) -> Unit) {
    CommandBubble(cmd.message) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            cmd.options.forEach { option ->
                Text(
                    option.toString(),
                    fontSize = 12.sp,
                    color = Cream,
                    modifier = Modifier
                        .border(0.5.dp, Stone.copy(alpha = 0.4f), RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .clickable { onResponse("end_session", option) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun RatingCommand(cmd: CommandData, onResponse: (String, Any) -> Unit) {
    var selected by remember { mutableIntStateOf(0) }
    val ratingColors = listOf(Rose, Color(0xFFF97316), Amber, Color(0xFF84CC16), Emerald)

    CommandBubble(cmd.message) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            cmd.options.forEachIndexed { index, option ->
                val value = option.toString().toIntOrNull() ?: (index + 1)
                val isSelected = selected == value
                val color = ratingColors.getOrElse(index) { Amber }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent)
                        .border(if (isSelected) 1.dp else 0.5.dp, if (isSelected) color else Stone.copy(alpha = 0.3f), CircleShape)
                        .clickable { selected = value; onResponse("rate_conversation", value) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$value", fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Light, color = if (isSelected) color else Stone)
                }
            }
        }
    }
}

@Composable
private fun FeedbackCommand(cmd: CommandData, onResponse: (String, Any) -> Unit) {
    var feedback by remember { mutableStateOf("") }

    CommandBubble(cmd.message) {
        BasicTextField(
            value = feedback,
            onValueChange = { if (it.length <= Constants.MAX_FEEDBACK_LENGTH) feedback = it },
            textStyle = TextStyle(color = Cream, fontSize = 13.sp, lineHeight = 20.sp),
            cursorBrush = SolidColor(Amber),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp)
                .border(0.5.dp, Stone.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(12.dp),
            decorationBox = { inner ->
                Box {
                    if (feedback.isEmpty()) Text("Share your thoughts...", fontSize = 13.sp, color = Stone)
                    inner()
                }
            },
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${feedback.length}/${Constants.MAX_FEEDBACK_LENGTH}", fontSize = 10.sp, color = Stone)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (cmd.optional) {
                    Text(
                        "Skip",
                        fontSize = 12.sp, color = Stone,
                        modifier = Modifier
                            .border(0.5.dp, Stone.copy(alpha = 0.3f), RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .clickable { onResponse("submit_feedback", "") }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
                Text(
                    "Submit",
                    fontSize = 12.sp, color = Midnight, fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Amber)
                        .clickable { onResponse("submit_feedback", feedback.trim()) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun CountdownCommand(cmd: CommandData, onComplete: () -> Unit) {
    var count by remember { mutableIntStateOf(Constants.FEEDBACK_COUNTDOWN_SECONDS) }

    LaunchedEffect(Unit) {
        while (count > 0) { delay(1000); count-- }
        onComplete()
    }

    val progress = count.toFloat() / Constants.FEEDBACK_COUNTDOWN_SECONDS

    CommandBubble(cmd.message) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Circular countdown
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
                Canvas(Modifier.size(28.dp)) {
                    val r = (size.minDimension - 2.dp.toPx()) / 2
                    drawCircle(Stone.copy(alpha = 0.2f), r, style = Stroke(2.dp.toPx()))
                    drawArc(Amber, -90f, progress * 360f, false, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
                        topLeft = Offset(center.x - r, center.y - r), size = androidx.compose.ui.geometry.Size(r * 2, r * 2))
                }
                Text("$count", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Cream)
            }
            Text("Closing session...", fontSize = 11.sp, color = Stone, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
private fun CommandBubble(message: String, content: @Composable ColumnScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape).background(Graphite),
            contentAlignment = Alignment.Center,
        ) { Text("N", fontSize = 9.sp, color = Amber, fontWeight = FontWeight.Thin) }
        Spacer(Modifier.width(8.dp))
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(Slate, RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                .padding(14.dp),
            content = {
                if (message.isNotBlank()) {
                    Text(message, fontSize = 13.sp, color = CreamMuted, lineHeight = 20.sp)
                    Spacer(Modifier.height(12.dp))
                }
                content()
            },
        )
    }
}

// ══════════════════════════════════════════════
// TYPING DOTS
// ══════════════════════════════════════════════

@Composable
private fun TypingDots() {
    val transition = rememberInfiniteTransition(label = "dots")
    val offsets = (0..2).map { i ->
        transition.animateFloat(
            initialValue = 0f, targetValue = -5f,
            animationSpec = infiniteRepeatable(
                animation = keyframes { durationMillis = 700; 0f at (i * 120); -5f at (i * 120 + 200); 0f at (i * 120 + 400) },
                repeatMode = RepeatMode.Restart,
            ), label = "d$i"
        )
    }

    Row(modifier = Modifier.padding(start = 32.dp, bottom = 12.dp)) {
        Box(
            modifier = Modifier
                .background(Slate, RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                offsets.forEach { offset ->
                    Box(
                        Modifier
                            .offset(y = offset.value.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Amber.copy(alpha = 0.6f))
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════
// COMPOSER
// ══════════════════════════════════════════════

// ══════════════════════════════════════════════
// IMAGE PREVIEW
// ══════════════════════════════════════════════

@Composable
private fun ImagePreviewRow(images: List<ImageAsset>, onRemove: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Onyx)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        images.forEachIndexed { index, img ->
            Box(modifier = Modifier.size(80.dp)) {
                // Thumbnail
                AsyncImage(
                    model = img.uri,
                    contentDescription = "Selected image",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
                // X button
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .clip(CircleShape)
                        .background(Midnight.copy(alpha = 0.9f))
                        .border(0.5.dp, Stone.copy(alpha = 0.3f), CircleShape)
                        .clickable { onRemove(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("×", fontSize = 12.sp, color = Cream, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════
// COMPOSER
// ══════════════════════════════════════════════

@Composable
private fun Composer(
    text: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    hasImages: Boolean = false,
    hasImageAdapter: Boolean = false,
    onPickImages: () -> Unit = {},
    onSend: () -> Unit,
) {
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(Amber.copy(alpha = 0.1f)))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Onyx)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Paperclip — image picker (only shown when adapter is provided)
        if (hasImageAdapter) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onPickImages),
                contentAlignment = Alignment.Center,
            ) {
                Text("📎", fontSize = 18.sp)
            }
        }

        // Text input
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            textStyle = TextStyle(color = Cream, fontSize = 15.sp, lineHeight = 22.sp),
            cursorBrush = SolidColor(Amber),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 40.dp, max = 140.dp)
                .background(Slate, RoundedCornerShape(22.dp))
                .padding(horizontal = 18.dp, vertical = 10.dp),
            decorationBox = { inner ->
                Box {
                    if (text.text.isEmpty()) {
                        Text("Write something...", fontSize = 15.sp, color = Stone)
                    }
                    inner()
                }
            },
        )

        // Send
        val canSend = text.text.isNotBlank() || hasImages
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (canSend) Amber else Graphite)
                .clickable(onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "↑",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (canSend) Midnight else Stone,
            )
        }
    }
}

