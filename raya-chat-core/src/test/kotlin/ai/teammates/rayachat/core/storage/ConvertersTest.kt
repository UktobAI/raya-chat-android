package ai.teammates.rayachat.core.storage

import ai.teammates.rayachat.core.models.Attachment
import ai.teammates.rayachat.core.models.AudioData
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `attachments round-trip`() {
        val attachments = listOf(
            Attachment(id = "1", url = "https://example.com/img.jpg", type = "image", name = "photo.jpg"),
            Attachment(id = "2", url = "https://example.com/img2.jpg", type = "image", name = "photo2.jpg"),
        )

        val jsonStr = converters.attachmentsToJson(attachments)
        assertThat(jsonStr).isNotNull()

        val restored = converters.jsonToAttachments(jsonStr)
        assertThat(restored).isEqualTo(attachments)
    }

    @Test
    fun `null attachments return null`() {
        assertThat(converters.attachmentsToJson(null)).isNull()
        assertThat(converters.jsonToAttachments(null)).isNull()
    }

    @Test
    fun `invalid attachments json returns null`() {
        assertThat(converters.jsonToAttachments("not json")).isNull()
    }

    @Test
    fun `empty attachments list round-trips`() {
        val jsonStr = converters.attachmentsToJson(emptyList())
        val restored = converters.jsonToAttachments(jsonStr)
        assertThat(restored).isEmpty()
    }

    @Test
    fun `audio data round-trips`() {
        val audio = AudioData(type = "remote", audioUrls = "https://example.com/audio.mp3")

        val jsonStr = converters.audioDataToJson(audio)
        assertThat(jsonStr).isNotNull()

        val restored = converters.jsonToAudioData(jsonStr)
        assertThat(restored).isEqualTo(audio)
    }

    @Test
    fun `null audio data returns null`() {
        assertThat(converters.audioDataToJson(null)).isNull()
        assertThat(converters.jsonToAudioData(null)).isNull()
    }

    @Test
    fun `invalid audio json returns null`() {
        assertThat(converters.jsonToAudioData("not json")).isNull()
    }
}
