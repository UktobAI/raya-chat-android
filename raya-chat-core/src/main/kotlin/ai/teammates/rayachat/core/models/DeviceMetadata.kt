package ai.teammates.rayachat.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Device metadata sent in the WebSocket connection URL's `data` parameter. */
@Serializable
data class DeviceMetadata(
    val platform: String = "android",
    @SerialName("os_version") val osVersion: String = "",
    @SerialName("device_family") val deviceFamily: String = "Android",
    @SerialName("sdk_version") val sdkVersion: String = "",
    val locale: String = "en",
    val timezone: String = "",
)
