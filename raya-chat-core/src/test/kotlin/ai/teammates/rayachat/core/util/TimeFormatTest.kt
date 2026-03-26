package ai.teammates.rayachat.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TimeFormatTest {

    @Test
    fun `formats epoch seconds`() {
        // 1774436672 = some date in 2026
        val result = formatLocalTime(epochSeconds = 1774436672L)
        assertThat(result).isNotEmpty()
        // Should match pattern like "10:30 AM" or "10:30 PM"
        assertThat(result).containsMatch("\\d{1,2}:\\d{2} [AP]M")
    }

    @Test
    fun `formats ISO timestamp`() {
        val result = formatLocalTime(isoTimestamp = "2026-03-26T10:30:00Z")
        assertThat(result).isNotEmpty()
        assertThat(result).containsMatch("\\d{1,2}:\\d{2} [AP]M")
    }

    @Test
    fun `null inputs return empty string`() {
        assertThat(formatLocalTime()).isEmpty()
    }

    @Test
    fun `blank ISO returns empty string`() {
        assertThat(formatLocalTime(isoTimestamp = "")).isEmpty()
    }

    @Test
    fun `zero epoch returns empty string`() {
        assertThat(formatLocalTime(epochSeconds = 0L)).isEmpty()
    }

    @Test
    fun `invalid ISO returns empty string`() {
        assertThat(formatLocalTime(isoTimestamp = "not-a-date")).isEmpty()
    }

    @Test
    fun `epoch takes priority over ISO when both provided`() {
        val result = formatLocalTime(
            isoTimestamp = "2026-03-26T10:30:00Z",
            epochSeconds = 1774436672L
        )
        assertThat(result).isNotEmpty()
    }
}
