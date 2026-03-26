package ai.teammates.rayachat.core.websocket

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MessageQueueTest {

    @Test
    fun `enqueue and flush all messages`() {
        val queue = MessageQueue()
        queue.enqueue("msg1")
        queue.enqueue("msg2")
        queue.enqueue("msg3")

        val sent = mutableListOf<String>()
        val count = queue.flush { msg ->
            sent.add(msg)
            true
        }

        assertThat(count).isEqualTo(3)
        assertThat(sent).containsExactly("msg1", "msg2", "msg3").inOrder()
        assertThat(queue.isEmpty).isTrue()
    }

    @Test
    fun `flush stops on send failure and retains remaining`() {
        val queue = MessageQueue()
        queue.enqueue("msg1")
        queue.enqueue("msg2")
        queue.enqueue("msg3")

        var sendCount = 0
        queue.flush { _ ->
            sendCount++
            sendCount <= 1 // Only first message succeeds
        }

        assertThat(queue.size).isEqualTo(2) // msg2 and msg3 remain
    }

    @Test
    fun `flush on empty queue returns 0`() {
        val queue = MessageQueue()
        val count = queue.flush { true }
        assertThat(count).isEqualTo(0)
    }

    @Test
    fun `clear removes all messages`() {
        val queue = MessageQueue()
        queue.enqueue("msg1")
        queue.enqueue("msg2")
        queue.clear()

        assertThat(queue.isEmpty).isTrue()
        assertThat(queue.size).isEqualTo(0)
    }

    @Test
    fun `size tracks correctly`() {
        val queue = MessageQueue()
        assertThat(queue.size).isEqualTo(0)
        queue.enqueue("a")
        assertThat(queue.size).isEqualTo(1)
        queue.enqueue("b")
        assertThat(queue.size).isEqualTo(2)
    }
}
