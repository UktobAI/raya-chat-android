package ai.teammates.rayachat.core.protocol

import ai.teammates.rayachat.core.models.*
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class MessageHandlerTest {

    private lateinit var handler: MessageHandler
    private lateinit var recorder: CallbackRecorder

    @Before
    fun setup() {
        recorder = CallbackRecorder()
        handler = MessageHandler(recorder)
    }

    // ── STEP ──

    @Test
    fun `STEP sets loading true with status text`() {
        handler.handle("""{"type": "step", "text": "Searching..."}""")

        assertThat(recorder.loadingCalls).containsExactly(Pair(true, "Searching..."))
    }

    // ── CHUNK ──

    @Test
    fun `CHUNK appends text to current message`() {
        handler.handle("""{"type": "chunk", "text": "Hello "}""")
        handler.handle("""{"type": "chunk", "text": "world"}""")

        assertThat(recorder.chunks).containsExactly("Hello ", "world").inOrder()
    }

    @Test
    fun `CHUNK with null text is ignored`() {
        handler.handle("""{"type": "chunk"}""")

        assertThat(recorder.chunks).isEmpty()
    }

    // ── RESPONSE ──

    @Test
    fun `RESPONSE adds bot message and clears streaming`() {
        handler.handle("""
        {
            "type": "response",
            "data": {
                "id": "msg-1",
                "chat_session_id": "session-1",
                "sender": 2,
                "content": "Hello!",
                "created_at": 1774436672,
                "attachments": [],
                "audio_urls": ""
            }
        }
        """)

        assertThat(recorder.chatMessages).hasSize(1)
        assertThat(recorder.chatMessages[0].content).isEqualTo("Hello!")
        assertThat(recorder.chatMessages[0].sender).isEqualTo(2)
        assertThat(recorder.sessionUpdates).containsExactly("session-1")
        assertThat(recorder.clearCurrentMessageCount).isEqualTo(1)
        assertThat(recorder.loadingCalls.last()).isEqualTo(Pair(false, null))
    }

    @Test
    fun `RESPONSE with attachments updates storage only`() {
        handler.handle("""
        {
            "type": "response",
            "data": {
                "id": "msg-1",
                "chat_session_id": "session-1",
                "sender": 2,
                "content": "Here are your images",
                "created_at": 1774436672,
                "attachments": ["https://s3.example.com/img1.jpg", "https://s3.example.com/img2.jpg"],
                "audio_urls": ""
            }
        }
        """)

        assertThat(recorder.attachmentCalls).hasSize(1)
        assertThat(recorder.attachmentCalls[0].first).hasSize(2)
        assertThat(recorder.attachmentCalls[0].second).isEqualTo("image")
    }

    // ── PRESETS ──

    @Test
    fun `PRESETS extracts titles`() {
        handler.handle("""
        {
            "type": "presets",
            "presets": [
                {"title": "Help"},
                {"title": "Pricing"},
                {"title": ""}
            ]
        }
        """)

        assertThat(recorder.presetCalls.last()).containsExactly("Help", "Pricing")
    }

    // ── COMMAND ──

    @Test
    fun `COMMAND rate_conversation creates command data`() {
        handler.handle("""
        {
            "type": "command",
            "content": "rate_conversation",
            "options": [1, 2, 3, 4, 5],
            "message": "How would you rate?",
            "optional": false
        }
        """)

        assertThat(recorder.commands).hasSize(1)
        assertThat(recorder.commands[0].content).isEqualTo("rate_conversation")
        assertThat(recorder.commands[0].message).isEqualTo("How would you rate?")
        assertThat(recorder.commands[0].options).hasSize(5)
    }

    @Test
    fun `COMMAND end_session creates command data`() {
        handler.handle("""
        {
            "type": "command",
            "content": "end_session",
            "options": ["Yes", "No"],
            "message": "End chat?",
            "optional": false
        }
        """)

        assertThat(recorder.commands).hasSize(1)
        assertThat(recorder.commands[0].content).isEqualTo("end_session")
    }

    @Test
    fun `COMMAND auto_close triggers auto close`() {
        handler.handle("""
        {
            "type": "command",
            "content": "auto_close",
            "message": "Session closed due to inactivity"
        }
        """)

        assertThat(recorder.commands).isEmpty() // auto_close doesn't create command data
        assertThat(recorder.autoCloseInfo).isNotNull()
        assertThat(recorder.autoCloseInfo!!.reason).isEqualTo(CloseReason.AUTO_CLOSE)
        // Presets should be cleared
        assertThat(recorder.presetCalls.last()).isEmpty()
    }

    @Test
    fun `COMMAND clears presets`() {
        handler.handle("""
        {
            "type": "command",
            "content": "submit_feedback",
            "message": "Leave feedback",
            "optional": true
        }
        """)

        assertThat(recorder.presetCalls).isNotEmpty()
        assertThat(recorder.presetCalls.last()).isEmpty()
    }

    @Test
    fun `COMMAND with unknown type is ignored`() {
        handler.handle("""
        {
            "type": "command",
            "content": "unknown_command",
            "message": "test"
        }
        """)

        assertThat(recorder.commands).isEmpty()
    }

    // ── ERROR ──

    @Test
    fun `ERROR sanitizes and fires callback`() {
        handler.handle("""{"type": "error", "text": "<script>alert('xss')</script>Connection failed"}""")

        assertThat(recorder.errors).hasSize(1)
        assertThat(recorder.errors[0]).doesNotContain("<script>")
    }

    // ── ESCALATION ──

    @Test
    fun `ESCALATION fires callback`() {
        handler.handle("""{"type": "human-rep-escalation"}""")

        assertThat(recorder.escalationCount).isEqualTo(1)
    }

    // ── INFO ──

    @Test
    fun `INFO sets info text`() {
        handler.handle("""{"type": "info", "text": "Waiting for agent..."}""")

        assertThat(recorder.infoTexts).containsExactly("Waiting for agent...")
    }

    @Test
    fun `INFO with human agent hides escalation`() {
        handler.handle("""{"type": "info", "text": "Connecting to human agent..."}""")

        assertThat(recorder.hideEscalationCount).isEqualTo(1)
    }

    // ── AGENT_ACTIVITY ──

    @Test
    fun `AGENT_ACTIVITY adds system message`() {
        handler.handle("""{"type": "agent_activity", "text": "Agent joined"}""")

        assertThat(recorder.systemMessages).hasSize(1)
        assertThat(recorder.systemMessages[0].content).isEqualTo("Agent joined")
        assertThat(recorder.systemMessages[0].type).isEqualTo(4)
    }

    // ── MESSAGE ──

    @Test
    fun `MESSAGE adds bot message directly`() {
        handler.handle("""{"type": "message", "content": "Direct message"}""")

        assertThat(recorder.chatMessages).hasSize(1)
        assertThat(recorder.chatMessages[0].content).isEqualTo("Direct message")
        assertThat(recorder.chatMessages[0].sender).isEqualTo(2)
    }

    // ── Edge cases ──

    @Test
    fun `invalid JSON is silently discarded`() {
        handler.handle("not json at all")
        handler.handle("{incomplete")
        handler.handle("")

        assertThat(recorder.chatMessages).isEmpty()
        assertThat(recorder.errors).isEmpty()
    }

    @Test
    fun `unknown message type is silently discarded`() {
        handler.handle("""{"type": "unknown_type", "text": "test"}""")

        assertThat(recorder.chatMessages).isEmpty()
    }

    // ── Test helper ──

    private class CallbackRecorder : MessageHandlerCallbacks {
        val loadingCalls = mutableListOf<Pair<Boolean, String?>>()
        val chunks = mutableListOf<String>()
        val chatMessages = mutableListOf<TypeMessage>()
        var clearCurrentMessageCount = 0
        val sessionUpdates = mutableListOf<String>()
        val attachmentCalls = mutableListOf<Pair<List<String>, String>>()
        val presetCalls = mutableListOf<List<String>>()
        val commands = mutableListOf<CommandData>()
        val errors = mutableListOf<String>()
        var escalationCount = 0
        val infoTexts = mutableListOf<String?>()
        var hideEscalationCount = 0
        val systemMessages = mutableListOf<TypeMessage>()
        var autoCloseInfo: SessionCloseInfo? = null

        override fun onLoading(isLoading: Boolean, status: String?) { loadingCalls.add(Pair(isLoading, status)) }
        override fun onChunk(text: String) { chunks.add(text) }
        override fun onChatMessage(message: TypeMessage) { chatMessages.add(message) }
        override fun onClearCurrentMessage() { clearCurrentMessageCount++ }
        override fun onSessionUpdate(sessionId: String) { sessionUpdates.add(sessionId) }
        override fun onAttachments(attachments: List<String>, type: String) { attachmentCalls.add(Pair(attachments, type)) }
        override fun onPresets(presets: List<String>) { presetCalls.add(presets) }
        override fun onCommand(commandData: CommandData) { commands.add(commandData) }
        override fun onError(message: String) { errors.add(message) }
        override fun onEscalation() { escalationCount++ }
        override fun onInfo(text: String?) { infoTexts.add(text) }
        override fun onHideEscalation() { hideEscalationCount++ }
        override fun onSystemMessage(message: TypeMessage) { systemMessages.add(message) }
        override fun onAutoClose(closeInfo: SessionCloseInfo) { autoCloseInfo = closeInfo }
    }
}
