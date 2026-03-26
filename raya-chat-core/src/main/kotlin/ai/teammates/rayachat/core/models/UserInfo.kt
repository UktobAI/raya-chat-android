package ai.teammates.rayachat.core.models

import kotlinx.serialization.Serializable

/** User information collected from the form screen. */
@Serializable
data class UserInfo(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
)
