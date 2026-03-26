package ai.teammates.rayachat.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ErrorSanitizerTest {

    @Test
    fun `null input returns default message`() {
        assertThat(sanitizeErrorMessage(null))
            .isEqualTo("An unexpected error occurred. Please try again.")
    }

    @Test
    fun `blank input returns default message`() {
        assertThat(sanitizeErrorMessage("   "))
            .isEqualTo("An unexpected error occurred. Please try again.")
    }

    @Test
    fun `strips HTML tags`() {
        assertThat(sanitizeErrorMessage("<b>Error</b> occurred"))
            .isEqualTo("Error occurred")
    }

    @Test
    fun `strips script tags`() {
        assertThat(sanitizeErrorMessage("<script>alert('xss')</script>Connection failed"))
            .isEqualTo("alert('xss')Connection failed")
    }

    @Test
    fun `strips javascript protocol`() {
        assertThat(sanitizeErrorMessage("javascript:alert(1)"))
            .isEqualTo("alert(1)")
    }

    @Test
    fun `strips event handlers`() {
        assertThat(sanitizeErrorMessage("Error onclick=steal()"))
            .isEqualTo("Error steal()")
    }

    @Test
    fun `replaces sensitive patterns with default`() {
        assertThat(sanitizeErrorMessage("Invalid api_key provided"))
            .isEqualTo("An unexpected error occurred. Please try again.")
        assertThat(sanitizeErrorMessage("Wrong password for user"))
            .isEqualTo("An unexpected error occurred. Please try again.")
        assertThat(sanitizeErrorMessage("Internal Server Error"))
            .isEqualTo("An unexpected error occurred. Please try again.")
    }

    @Test
    fun `caps message at 200 characters`() {
        val longMessage = "A".repeat(300)
        val result = sanitizeErrorMessage(longMessage)
        assertThat(result.length).isEqualTo(203) // 200 + "..."
        assertThat(result).endsWith("...")
    }

    @Test
    fun `clean message passes through`() {
        assertThat(sanitizeErrorMessage("Connection timed out"))
            .isEqualTo("Connection timed out")
    }

    @Test
    fun `trims whitespace`() {
        assertThat(sanitizeErrorMessage("  Connection lost  "))
            .isEqualTo("Connection lost")
    }
}
