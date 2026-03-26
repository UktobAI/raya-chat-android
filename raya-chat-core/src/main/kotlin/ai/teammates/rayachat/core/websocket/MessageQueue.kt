package ai.teammates.rayachat.core.websocket

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Thread-safe queue for messages sent while the WebSocket is in CONNECTING state.
 * Messages are flushed in order on successful connection.
 */
class MessageQueue {

    private val queue = ConcurrentLinkedQueue<String>()

    val size: Int get() = queue.size
    val isEmpty: Boolean get() = queue.isEmpty()

    fun enqueue(message: String) {
        queue.add(message)
    }

    /**
     * Flush all queued messages using the provided sender.
     * Returns the number of messages successfully sent.
     * If sending fails, remaining messages stay in the queue.
     */
    fun flush(sender: (String) -> Boolean): Int {
        var sentCount = 0
        while (queue.isNotEmpty()) {
            val message = queue.peek() ?: break
            if (sender(message)) {
                queue.poll() // Remove only if sent successfully
                sentCount++
            } else {
                break // Connection broke during flush — stop
            }
        }
        return sentCount
    }

    fun clear() {
        queue.clear()
    }
}
