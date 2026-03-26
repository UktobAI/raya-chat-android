package ai.teammates.rayachat.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ColorUtilsTest {

    @Test
    fun `dark blue is detected as dark`() {
        assertThat(isDarkColor("#0047AF")).isTrue()
    }

    @Test
    fun `white is detected as light`() {
        assertThat(isDarkColor("#FFFFFF")).isFalse()
    }

    @Test
    fun `black is detected as dark`() {
        assertThat(isDarkColor("#000000")).isTrue()
    }

    @Test
    fun `yellow is detected as light`() {
        assertThat(isDarkColor("#FFD700")).isFalse()
    }

    @Test
    fun `red is detected as dark`() {
        assertThat(isDarkColor("#FF0000")).isTrue()
    }

    @Test
    fun `lime green is detected as light`() {
        assertThat(isDarkColor("#00FF00")).isFalse()
    }

    @Test
    fun `shorthand hex works`() {
        assertThat(isDarkColor("#FFF")).isFalse()
        assertThat(isDarkColor("#000")).isTrue()
    }

    @Test
    fun `without hash prefix works`() {
        assertThat(isDarkColor("0047AF")).isTrue()
    }

    @Test
    fun `invalid hex defaults to dark`() {
        assertThat(isDarkColor("notacolor")).isTrue()
        assertThat(isDarkColor("")).isTrue()
    }

    @Test
    fun `getContrastColor returns white for dark gradient`() {
        assertThat(getContrastColor("#0047AF")).isEqualTo("#FFFFFF")
    }

    @Test
    fun `getContrastColor returns dark for light gradient`() {
        assertThat(getContrastColor("#FFD700")).isEqualTo("#1A1A1A")
    }
}
