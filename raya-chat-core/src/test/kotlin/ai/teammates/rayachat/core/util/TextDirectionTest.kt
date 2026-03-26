package ai.teammates.rayachat.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TextDirectionTest {

    @Test
    fun `Arabic locale is RTL`() {
        assertThat(isRTLLocale("ar")).isTrue()
        assertThat(isRTLLocale("ar-SA")).isTrue()
    }

    @Test
    fun `Hebrew locale is RTL`() {
        assertThat(isRTLLocale("he")).isTrue()
    }

    @Test
    fun `English locale is LTR`() {
        assertThat(isRTLLocale("en")).isFalse()
        assertThat(isRTLLocale("en-US")).isFalse()
    }

    @Test
    fun `French locale is LTR`() {
        assertThat(isRTLLocale("fr")).isFalse()
    }

    @Test
    fun `Arabic text is detected as RTL`() {
        assertThat(isRTLText("مرحبا بالعالم")).isTrue()
    }

    @Test
    fun `English text is detected as LTR`() {
        assertThat(isRTLText("Hello World")).isFalse()
    }

    @Test
    fun `Mixed text starting with Arabic is RTL`() {
        assertThat(isRTLText("مرحبا Hello")).isTrue()
    }

    @Test
    fun `Mixed text starting with English is LTR`() {
        assertThat(isRTLText("Hello مرحبا")).isFalse()
    }

    @Test
    fun `Blank text is not RTL`() {
        assertThat(isRTLText("")).isFalse()
        assertThat(isRTLText("   ")).isFalse()
    }

    @Test
    fun `Numbers-only text is not RTL`() {
        assertThat(isRTLText("12345")).isFalse()
    }

    @Test
    fun `Emoji-only text is not RTL`() {
        assertThat(isRTLText("👋😊")).isFalse()
    }
}
