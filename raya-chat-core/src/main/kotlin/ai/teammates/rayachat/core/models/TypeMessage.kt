package ai.teammates.rayachat.core.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Chat message — the primary data type stored in Room and held in UI state.
 *
 * Sender: 1 = user, 2 = bot/agent.
 * Type: 1 = text, 2 = audio, 3 = image, 4 = system/agent_activity.
 */
@Entity(tableName = "messages")
data class TypeMessage(
    @PrimaryKey val id: String,
    val sender: Int = 0,
    val type: Int = 1,
    val content: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    /** JSON-serialized List<Attachment>. Room stores as String via TypeConverter. */
    @ColumnInfo(name = "attachments_json") val attachmentsJson: String? = null,
    /** JSON-serialized AudioData. Room stores as String via TypeConverter. */
    @ColumnInfo(name = "audio_json") val audioJson: String? = null,
)
