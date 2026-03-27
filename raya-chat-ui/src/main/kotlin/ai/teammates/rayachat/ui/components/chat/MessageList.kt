package ai.teammates.rayachat.ui.components.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.teammates.rayachat.core.models.TypeMessage
import kotlinx.coroutines.launch

/**
 * Scrollable message list with auto-scroll, streaming message, and footer content.
 *
 * Auto-scroll triggers:
 * 1. New message added (displayData.size changes)
 * 2. Streaming text grows (currentMessage changes)
 * 3. Footer content changes (typing indicator, presets, commands appear)
 *
 * Auto-scroll stops if user manually scrolls up.
 * Resumes when user taps the scroll-to-bottom button.
 */
@Composable
fun MessageList(
    messages: List<TypeMessage>,
    currentMessage: String,
    botIcon: String?,
    onImagePress: ((String) -> Unit)? = null,
    footerContent: @Composable (() -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val userScrolledUp = remember { mutableStateOf(false) }

    // Build display data: messages + streaming message
    val displayData = remember(messages, currentMessage) {
        val items = messages.toMutableList()
        if (currentMessage.isNotEmpty()) {
            items.add(
                TypeMessage(
                    id = "__streaming__",
                    sender = 2,
                    type = 1,
                    content = currentMessage,
                    createdAt = null,
                )
            )
        }
        items
    }

    // Total item count including footer
    val hasFooter = footerContent != null
    val totalItemCount = displayData.size + if (hasFooter) 1 else 0

    // Track if user is near the bottom
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem == null || lastVisibleItem.index >= layoutInfo.totalItemsCount - 2
        }
    }

    // Update userScrolledUp based on scroll position
    LaunchedEffect(isAtBottom) {
        userScrolledUp.value = !isAtBottom
    }

    // Auto-scroll when content changes — messages, streaming, or footer
    // Uses totalItemCount so footer (typing indicator, presets) triggers scroll too
    LaunchedEffect(displayData.size, currentMessage.length, totalItemCount) {
        if (!userScrolledUp.value && totalItemCount > 0) {
            // Scroll to the very last item (footer if present, otherwise last message)
            listState.animateScrollToItem(totalItemCount - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(items = displayData, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    botIcon = botIcon,
                    onImagePress = onImagePress,
                )
            }

            // Footer content — scrolls with messages
            if (footerContent != null) {
                item(key = "__footer__") {
                    footerContent()
                }
            }
        }

        // Scroll to bottom button
        ScrollToBottomButton(
            visible = userScrolledUp.value,
            onClick = {
                userScrolledUp.value = false
                scope.launch {
                    if (totalItemCount > 0) {
                        listState.animateScrollToItem(totalItemCount - 1)
                    }
                }
            },
        )
    }
}
