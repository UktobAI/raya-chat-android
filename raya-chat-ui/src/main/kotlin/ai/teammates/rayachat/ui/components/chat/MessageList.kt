package ai.teammates.rayachat.ui.components.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.teammates.rayachat.core.models.TypeMessage

/**
 * Scrollable message list with auto-scroll, streaming message, and footer content.
 * Uses LazyColumn (Compose equivalent of FlatList).
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

    // Track scroll position
    val isAtBottom by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem == null || lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 2
        }
    }

    LaunchedEffect(isAtBottom) {
        userScrolledUp.value = !isAtBottom
    }

    // Auto-scroll when messages change
    LaunchedEffect(displayData.size, currentMessage) {
        if (!userScrolledUp.value && displayData.isNotEmpty()) {
            listState.animateScrollToItem(displayData.size - 1)
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
                // Scroll in a coroutine
                // The LaunchedEffect above will handle scrolling
            },
        )
    }
}
