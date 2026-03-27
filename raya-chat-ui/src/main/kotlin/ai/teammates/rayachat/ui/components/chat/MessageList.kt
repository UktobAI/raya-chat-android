package ai.teammates.rayachat.ui.components.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.teammates.rayachat.core.models.TypeMessage
import kotlinx.coroutines.launch

@Composable
fun MessageList(
    messages: List<TypeMessage>,
    currentMessage: String,
    botIcon: String?,
    onImagePress: ((String) -> Unit)? = null,
    footerContent: @Composable (() -> Unit)? = null,
    footerChangeSignal: Int = 0,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var userScrolledUp by remember { mutableStateOf(false) }

    // Build display data
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

    val hasFooter = footerContent != null
    val totalItemCount = displayData.size + if (hasFooter) 1 else 0
    val isStreaming = currentMessage.isNotEmpty()

    // Detect user scroll — when user drags up, disable auto-scroll
    val firstVisibleIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    var prevFirstVisible by remember { mutableIntStateOf(0) }

    LaunchedEffect(listState.isScrollInProgress, firstVisibleIndex) {
        if (listState.isScrollInProgress) {
            // User is actively scrolling — check direction
            if (firstVisibleIndex < prevFirstVisible) {
                userScrolledUp = true
            }
            // If user scrolled to the very end, re-enable
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            if (lastVisible != null && lastVisible.index >= listState.layoutInfo.totalItemsCount - 1) {
                userScrolledUp = false
            }
        }
        prevFirstVisible = firstVisibleIndex
    }

    // Auto-scroll on new messages — animated (smooth, one-time event)
    LaunchedEffect(displayData.size) {
        if (!userScrolledUp && totalItemCount > 0) {
            listState.animateScrollToItem(totalItemCount - 1, scrollOffset = Int.MAX_VALUE)
        }
    }

    // Footer changes (presets, commands, typing) + streaming — instant scroll (no animation)
    // Using instant scroll avoids stutter from repeated animated scrolls during
    // command UI rendering, streaming chunks, and preset appearance.
    LaunchedEffect(currentMessage, footerChangeSignal) {
        if (!userScrolledUp && totalItemCount > 0) {
            listState.scrollToItem(totalItemCount - 1, scrollOffset = Int.MAX_VALUE)
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

            if (footerContent != null) {
                item(key = "__footer__") {
                    footerContent()
                }
            }
        }

        // Scroll-to-bottom FAB — bottom-right
        ScrollToBottomButton(
            visible = userScrolledUp,
            onClick = {
                userScrolledUp = false
                scope.launch {
                    if (totalItemCount > 0) {
                        listState.animateScrollToItem(totalItemCount - 1, scrollOffset = Int.MAX_VALUE)
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
        )
    }
}
