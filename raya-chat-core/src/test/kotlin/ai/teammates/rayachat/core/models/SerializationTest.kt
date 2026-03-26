package ai.teammates.rayachat.core.models

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

class SerializationTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    @Test
    fun `BotConfigProps deserializes from server JSON`() {
        val serverJson = """
        {
            "id": "abc-123",
            "agent_id": "def-456",
            "theme": "dark",
            "chatbox_initial_msg": "Hi there 👋",
            "chatbox_placeholder": "Ask me anything...",
            "enable_voice_note": false,
            "enable_image_upload": true,
            "enable_realtime_voice_call": false,
            "enable_user_form": true,
            "enable_user_email": true,
            "enable_user_phone": false,
            "preset_options": ["Help", "Pricing"],
            "chatbox_gradient_color": "#FF5733",
            "chatbox_chat_icon": "https://example.com/icon.png",
            "chatbox_btn_icon": null,
            "chatbox_system_heading": "Welcome!",
            "chatbox_system_paragraph": "How can we help?"
        }
        """.trimIndent()

        val config = json.decodeFromString<BotConfigProps>(serverJson)

        assertThat(config.id).isEqualTo("abc-123")
        assertThat(config.theme).isEqualTo("dark")
        assertThat(config.chatboxInitialMsg).isEqualTo("Hi there 👋")
        assertThat(config.enableVoiceNote).isFalse()
        assertThat(config.enableImageUpload).isTrue()
        assertThat(config.presetOptions).containsExactly("Help", "Pricing")
        assertThat(config.chatboxGradientColor).isEqualTo("#FF5733")
        assertThat(config.chatboxBtnIcon).isNull()
    }

    @Test
    fun `BotConfigProps uses defaults for missing fields`() {
        val config = json.decodeFromString<BotConfigProps>("{}")

        assertThat(config.theme).isEqualTo("light")
        assertThat(config.chatboxGradientColor).isEqualTo("#0047AF")
        assertThat(config.enableUserForm).isTrue()
        assertThat(config.presetOptions).isEmpty()
    }

    @Test
    fun `ResponseData deserializes correctly`() {
        val responseJson = """
        {
            "id": "msg-1",
            "chat_session_id": "session-1",
            "sender": 2,
            "content": "Hello!",
            "created_at": 1774436672,
            "attachments": ["https://s3.example.com/img1.jpg", "https://s3.example.com/img2.jpg"],
            "audio_urls": "",
            "chart_data": ""
        }
        """.trimIndent()

        val data = json.decodeFromString<ResponseData>(responseJson)

        assertThat(data.id).isEqualTo("msg-1")
        assertThat(data.chatSessionId).isEqualTo("session-1")
        assertThat(data.sender).isEqualTo(2)
        assertThat(data.content).isEqualTo("Hello!")
        assertThat(data.createdAt).isEqualTo(1774436672L)
        assertThat(data.attachments).hasSize(2)
    }

    @Test
    fun `DeviceMetadata serializes correctly`() {
        val metadata = DeviceMetadata(
            platform = "android",
            osVersion = "14",
            deviceFamily = "Android",
            sdkVersion = "0.1.0",
            locale = "en",
            timezone = "Asia/Dubai"
        )

        val serialized = json.encodeToString(DeviceMetadata.serializer(), metadata)

        assertThat(serialized).contains("\"platform\":\"android\"")
        assertThat(serialized).contains("\"os_version\":\"14\"")
        assertThat(serialized).contains("\"sdk_version\":\"0.1.0\"")
    }

    @Test
    fun `UserInfo round-trips`() {
        val user = UserInfo(fullName = "John Doe", email = "john@test.com", phone = "+1234567890")
        val serialized = json.encodeToString(UserInfo.serializer(), user)
        val deserialized = json.decodeFromString<UserInfo>(serialized)

        assertThat(deserialized).isEqualTo(user)
    }

    @Test
    fun `Attachment round-trips`() {
        val attachment = Attachment(id = "att-1", url = "https://s3.example.com/img.jpg", type = "image", name = "photo.jpg")
        val serialized = json.encodeToString(Attachment.serializer(), attachment)
        val deserialized = json.decodeFromString<Attachment>(serialized)

        assertThat(deserialized).isEqualTo(attachment)
    }

    @Test
    fun `AudioData round-trips`() {
        val audio = AudioData(type = "remote", audioUrls = "https://s3.example.com/audio.mp3")
        val serialized = json.encodeToString(AudioData.serializer(), audio)
        val deserialized = json.decodeFromString<AudioData>(serialized)

        assertThat(deserialized).isEqualTo(audio)
    }

    @Test
    fun `ChatMessage deserializes STEP`() {
        val stepJson = """{"type": "step", "text": "Searching..."}"""
        val msg = json.decodeFromString<ChatMessage>(stepJson)

        assertThat(msg.type).isEqualTo("step")
        assertThat(msg.text).isEqualTo("Searching...")
    }

    @Test
    fun `ChatMessage deserializes CHUNK`() {
        val chunkJson = """{"type": "chunk", "text": "partial text"}"""
        val msg = json.decodeFromString<ChatMessage>(chunkJson)

        assertThat(msg.type).isEqualTo("chunk")
        assertThat(msg.text).isEqualTo("partial text")
    }

    @Test
    fun `ChatMessage deserializes COMMAND`() {
        val commandJson = """
        {
            "type": "command",
            "content": "rate_conversation",
            "options": [1, 2, 3, 4, 5],
            "message": "How would you rate?",
            "optional": false
        }
        """.trimIndent()

        val msg = json.decodeFromString<ChatMessage>(commandJson)

        assertThat(msg.type).isEqualTo("command")
        assertThat(msg.content).isEqualTo("rate_conversation")
        assertThat(msg.options).hasSize(5)
        assertThat(msg.message).isEqualTo("How would you rate?")
        assertThat(msg.optional).isFalse()
    }

    @Test
    fun `ChatMessage deserializes PRESETS`() {
        val presetsJson = """
        {
            "type": "presets",
            "presets": [
                {"title": "Ask about products"},
                {"title": "Track order"}
            ]
        }
        """.trimIndent()

        val msg = json.decodeFromString<ChatMessage>(presetsJson)

        assertThat(msg.type).isEqualTo("presets")
        assertThat(msg.presets).hasSize(2)
        assertThat(msg.presets!![0].title).isEqualTo("Ask about products")
    }

    @Test
    fun `MessageType fromValue works for all types`() {
        assertThat(MessageType.fromValue("step")).isEqualTo(MessageType.STEP)
        assertThat(MessageType.fromValue("chunk")).isEqualTo(MessageType.CHUNK)
        assertThat(MessageType.fromValue("response")).isEqualTo(MessageType.RESPONSE)
        assertThat(MessageType.fromValue("presets")).isEqualTo(MessageType.PRESETS)
        assertThat(MessageType.fromValue("command")).isEqualTo(MessageType.COMMAND)
        assertThat(MessageType.fromValue("error")).isEqualTo(MessageType.ERROR)
        assertThat(MessageType.fromValue("human-rep-escalation")).isEqualTo(MessageType.ESCALATION)
        assertThat(MessageType.fromValue("info")).isEqualTo(MessageType.INFO)
        assertThat(MessageType.fromValue("agent_activity")).isEqualTo(MessageType.AGENT_ACTIVITY)
        assertThat(MessageType.fromValue("message")).isEqualTo(MessageType.MESSAGE)
    }

    @Test
    fun `MessageType fromValue returns null for unknown type`() {
        assertThat(MessageType.fromValue("unknown")).isNull()
        assertThat(MessageType.fromValue("")).isNull()
    }
}
