package ai.teammates.rayachat.core.api

import ai.teammates.rayachat.core.Constants
import ai.teammates.rayachat.core.models.BotConfigProps
import ai.teammates.rayachat.core.models.UserInfo
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class ApiClientTest {

    private lateinit var server: MockWebServer

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `constructWebSocketUrl includes all required params`() {
        val client = ApiClient(token = "test-token", locale = "en")

        val url = client.constructWebSocketUrl(
            sessionId = "session-123",
            userInfo = UserInfo(fullName = "John Doe", email = "john@test.com", phone = "+1234567890")
        )

        assertThat(url).startsWith("wss://${Constants.DEFAULT_ENDPOINT}/v1/enhanced-chat/ws/stream")
        assertThat(url).contains("integration_type=widget")
        assertThat(url).contains("token=test-token")
        assertThat(url).contains("chat_session_id=session-123")
        assertThat(url).contains("agent_id=null")
        // user_name should be URL-encoded
        assertThat(url).contains("user_name=John")
        assertThat(url).contains("email=john%40test.com")
        assertThat(url).contains("phone=%2B1234567890")
    }

    @Test
    fun `constructWebSocketUrl with empty session id`() {
        val client = ApiClient(token = "test-token")

        val url = client.constructWebSocketUrl(
            sessionId = "",
            userInfo = UserInfo()
        )

        assertThat(url).contains("chat_session_id=")
        assertThat(url).doesNotContain("chat_session_id=null")
    }

    @Test
    fun `constructWebSocketUrl URL-encodes data param`() {
        val client = ApiClient(token = "test-token", locale = "en")

        val url = client.constructWebSocketUrl(
            sessionId = "",
            userInfo = UserInfo()
        )

        // data param should be URL-encoded JSON — no raw { or }
        val dataParam = url.substringAfter("data=")
        assertThat(dataParam).doesNotContain("{")
        assertThat(dataParam).doesNotContain("}")
        // Should contain URL-encoded equivalents
        assertThat(dataParam).contains("%7B") // { encoded
    }

    @Test
    fun `constructWebSocketUrl handles Arabic names`() {
        val client = ApiClient(token = "test-token", locale = "ar")

        val url = client.constructWebSocketUrl(
            sessionId = "",
            userInfo = UserInfo(fullName = "محمد", email = "m@test.com", phone = "")
        )

        // Arabic name should be URL-encoded, not raw
        assertThat(url).doesNotContain("محمد")
        assertThat(url).contains("user_name=%D9%85") // UTF-8 encoded Arabic
    }
}
