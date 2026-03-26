package ai.teammates.rayachat.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ValidationTest {

    @Test
    fun `valid emails pass`() {
        assertThat(validateEmail("user@example.com")).isTrue()
        assertThat(validateEmail("john.doe+tag@company.co.uk")).isTrue()
        assertThat(validateEmail("test@test.io")).isTrue()
    }

    @Test
    fun `invalid emails fail`() {
        assertThat(validateEmail("")).isFalse()
        assertThat(validateEmail("notanemail")).isFalse()
        assertThat(validateEmail("@example.com")).isFalse()
        assertThat(validateEmail("user@")).isFalse()
        assertThat(validateEmail("user@.com")).isFalse()
        assertThat(validateEmail("   ")).isFalse()
    }

    @Test
    fun `valid phone numbers pass`() {
        assertThat(validatePhone("+1 234 567 8900")).isTrue()
        assertThat(validatePhone("1234567890")).isTrue()
        assertThat(validatePhone("+971-50-123-4567")).isTrue()
        assertThat(validatePhone("(555) 123-4567")).isTrue()
    }

    @Test
    fun `invalid phone numbers fail`() {
        assertThat(validatePhone("")).isFalse()
        assertThat(validatePhone("abc")).isFalse()
        assertThat(validatePhone("123")).isFalse()  // too short
        assertThat(validatePhone("   ")).isFalse()
    }
}
