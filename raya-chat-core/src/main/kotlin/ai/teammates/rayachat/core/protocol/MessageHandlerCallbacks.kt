package ai.teammates.rayachat.core.protocol

import ai.teammates.rayachat.core.models.CommandData
import ai.teammates.rayachat.core.models.SessionCloseInfo
import ai.teammates.rayachat.core.models.TypeMessage

/**
 * Callbacks from the message handler to the client.
 * Each method corresponds to a server message type's required action.
 */
interface MessageHandlerCallbacks {
    /** STEP: Bot is thinking. Show typing indicator. */
    fun onLoading(isLoading: Boolean, status: String?)

    /** CHUNK: Partial streaming text. Append to current message. */
    fun onChunk(text: String)

    /** RESPONSE: Complete bot message. Add to message list. */
    fun onChatMessage(message: TypeMessage)

    /** RESPONSE: Clear streaming buffer after adding final message. */
    fun onClearCurrentMessage()

    /** RESPONSE: Session ID assigned by server. Save to storage. */
    fun onSessionUpdate(sessionId: String)

    /** RESPONSE: Server returned attachment URLs. Update storage only. */
    fun onAttachments(attachments: List<String>, type: String)

    /** PRESETS: Show suggestion buttons. */
    fun onPresets(presets: List<String>)

    /** COMMAND: Server requires user interaction. */
    fun onCommand(commandData: CommandData)

    /** ERROR: Sanitized error message from server. */
    fun onError(message: String)

    /** ESCALATION: Show "Connect with human representative" button. */
    fun onEscalation()

    /** INFO: Show info text with hourglass. */
    fun onInfo(text: String?)

    /** INFO: Hide escalation button (human agent connected). */
    fun onHideEscalation()

    /** AGENT_ACTIVITY: Add system message. */
    fun onSystemMessage(message: TypeMessage)

    /** auto_close: Session closed by server. */
    fun onAutoClose(closeInfo: SessionCloseInfo)
}
