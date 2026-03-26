package ai.teammates.rayachat.core.storage

import androidx.room.TypeConverter
import ai.teammates.rayachat.core.models.Attachment
import ai.teammates.rayachat.core.models.AudioData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room TypeConverters for complex fields in [ai.teammates.rayachat.core.models.TypeMessage].
 * Converts List<Attachment> and AudioData to/from JSON strings for SQLite storage.
 */
class Converters {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @TypeConverter
    fun attachmentsToJson(attachments: List<Attachment>?): String? {
        if (attachments == null) return null
        return json.encodeToString(attachments)
    }

    @TypeConverter
    fun jsonToAttachments(value: String?): List<Attachment>? {
        if (value == null) return null
        return try {
            json.decodeFromString<List<Attachment>>(value)
        } catch (_: Exception) {
            null
        }
    }

    @TypeConverter
    fun audioDataToJson(audio: AudioData?): String? {
        if (audio == null) return null
        return json.encodeToString(audio)
    }

    @TypeConverter
    fun jsonToAudioData(value: String?): AudioData? {
        if (value == null) return null
        return try {
            json.decodeFromString<AudioData>(value)
        } catch (_: Exception) {
            null
        }
    }
}
