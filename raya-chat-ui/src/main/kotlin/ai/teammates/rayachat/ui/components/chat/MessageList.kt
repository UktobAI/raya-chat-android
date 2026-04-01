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

    val isStreaming = currentMessage.isNotEmpty()
    val hasFooter = footerContent != null

    // Total item count: messages + streaming bubble (if active) + footer
    val totalItemCount = messages.size +
        (if (isStreaming) 1 else 0) +
        (if (hasFooter) 1 else 0)

    // Streaming message — only the TypeMessage object is rebuilt, NOT the whole list
    val streamingMessage = remember(currentMessage) {
        if (currentMessage.isNotEmpty()) {
            TypeMessage(id = "__streaming__", sender = 2, type = 1, content = currentMessage, createdAt = null)
        } else null
    }

    // Detect user scroll
    val firstVisibleIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    var prevFirstVisible by remember { mutableIntStateOf(0) }

    LaunchedEffect(listState.isScrollInProgress, firstVisibleIndex) {
        if (listState.isScrollInProgress) {
            if (firstVisibleIndex < prevFirstVisible) {
                userScrolledUp = true
            }
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            if (lastVisible != null && lastVisible.index >= listState.layoutInfo.totalItemsCount - 1) {
                userScrolledUp = false
            }
        }
        prevFirstVisible = firstVisibleIndex
    }

    // Auto-scroll on new messages — animated
    LaunchedEffect(messages.size) {
        if (!userScrolledUp && totalItemCount > 0) {
            listState.animateScrollToItem(totalItemCount - 1, scrollOffset = Int.MAX_VALUE)
        }
    }

    // Streaming + footer changes — instant scroll
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
            // Persisted messages — list reference only changes when a new message is added (not on streaming)
            items(items = messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    botIcon = botIcon,
                    onImagePress = onImagePress,
                )
            }

            // Streaming message — separate item, only rebuilds when currentMessage changes
            // No list copy needed — just one TypeMessage object recreated per chunk
            if (streamingMessage != null) {
                item(key = "__streaming__") {
                    MessageBubble(
                        message = streamingMessage,
                        botIcon = botIcon,
                        onImagePress = null,
                    )
                }
            }

            // Footer
            if (footerContent != null) {
                item(key = "__footer__") {
                    footerContent()
                }
            }
        }

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
