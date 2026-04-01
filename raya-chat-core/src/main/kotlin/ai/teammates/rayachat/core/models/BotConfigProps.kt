package ai.teammates.rayachat.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Bot configuration fetched from the API. All fields have defaults for resilience. */
@Serializable
data class BotConfigProps(
    val id: String? = null,
    @SerialName("agent_id") val agentId: String? = null,
    val theme: String? = "light",
    @SerialName("chatbox_initial_msg") val chatboxInitialMsg: String? = "Hi there \uD83D\uDC4B How can I help?",
    @SerialName("chatbox_placeholder") val chatboxPlaceholder: String? = "Type your message...",
    @SerialName("enable_voice_note") val enableVoiceNote: Boolean = true,
    @SerialName("enable_image_upload") val enableImageUpload: Boolean = true,
    @SerialName("enable_realtime_voice_call") val enableRealtimeVoiceCall: Boolean = false,
    @SerialName("enable_user_form") val enableUserForm: Boolean = true,
    @SerialName("enable_user_email") val enableUserEmail: Boolean = true,
    @SerialName("enable_user_phone") val enableUserPhone: Boolean = false,
    @SerialName("preset_options") val presetOptions: List<String>? = emptyList(),
    @SerialName("chatbox_gradient_color") val chatboxGradientColor: String? = "#0047AF",
    @SerialName("chatbox_chat_icon") val chatboxChatIcon: String? = null,
    @SerialName("chatbox_btn_icon") val chatboxBtnIcon: String? = null,
    @SerialName("chatbox_header_icon") val chatboxHeaderIcon: String? = null,
    @SerialName("chatbox_system_heading") val chatboxSystemHeading: String? = "Hi there Raya is ready to help \u2728",
    @SerialName("chatbox_system_paragraph") val chatboxSystemParagraph: String? = "Ask any question \u2014 Raya is fast and friendly.",
)
