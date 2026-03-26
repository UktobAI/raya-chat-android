package ai.teammates.rayachat.core.api

import ai.teammates.rayachat.core.Constants
import ai.teammates.rayachat.core.models.BotConfigProps
import ai.teammates.rayachat.core.models.DeviceMetadata
import ai.teammates.rayachat.core.models.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.TimeZone

/**
 * API client for bot config fetch and WebSocket URL construction.
 */
class ApiClient(
    private val token: String,
    private val locale: String = "en",
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val httpClient = OkHttpClient()
    private val endpoint = Constants.DEFAULT_ENDPOINT

    /**
     * Fetches bot configuration from the server.
     * Returns defaults on any failure — never throws.
     */
    suspend fun fetchBotConfig(): BotConfigProps = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://$endpoint/v1/agents/chatbox-config/widget/")
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string()

            if (response.isSuccessful && !body.isNullOrBlank()) {
                json.decodeFromString<BotConfigProps>(body)
            } else {
                BotConfigProps()
            }
        } catch (_: Exception) {
            BotConfigProps()
        }
    }

    /**
     * Constructs the WebSocket connection URL with all required parameters.
     * Matches the RN SDK's URL format exactly.
     *
     * IMPORTANT: The `data` parameter is URL-encoded.
     * OkHttp crashes with raw JSON in query params on Android.
     */
    fun constructWebSocketUrl(
        sessionId: String,
        userInfo: UserInfo,
    ): String {
        val metadata = getDeviceMetadata()
        val metadataJson = json.encodeToString(DeviceMetadata.serializer(), metadata)

        return buildString {
            append("wss://")
            append(endpoint)
            append("/v1/conversations/ws/start")
            append("?integration_type=")
            append(Constants.INTEGRATION_TYPE)
            append("&token=")
            append(token)
            append("&agent_id=null")
            append("&chat_session_id=")
            append(sessionId)
            append("&user_name=")
            append(urlEncode(userInfo.fullName))
            append("&email=")
            append(urlEncode(userInfo.email.lowercase()))
            append("&phone=")
            append(urlEncode(userInfo.phone))
            append("&data=")
            append(urlEncode(metadataJson))
        }
    }

    private fun getDeviceMetadata(): DeviceMetadata {
        return DeviceMetadata(
            platform = "android",
            osVersion = try { android.os.Build.VERSION.RELEASE ?: "unknown" } catch (_: Exception) { "unknown" },
            deviceFamily = "Android",
            sdkVersion = Constants.SDK_VERSION,
            locale = locale,
            timezone = try { TimeZone.getDefault().id } catch (_: Exception) { "UTC" },
        )
    }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, "UTF-8")
}
