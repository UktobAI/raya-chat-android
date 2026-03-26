package ai.teammates.rayachat.core.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import ai.teammates.rayachat.core.Constants
import ai.teammates.rayachat.core.models.UserInfo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Key-value storage for session ID and user info.
 * Uses EncryptedSharedPreferences (AES-256) with fallback to regular SharedPreferences
 * if encryption fails (older/rooted devices).
 */
class PreferenceStorage(context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            Constants.PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.w(TAG, "EncryptedSharedPreferences failed, using unencrypted fallback", e)
        context.applicationContext.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ── Session ID ──

    fun getSessionId(): String =
        prefs.getString(Constants.KEY_SESSION_ID, "") ?: ""

    fun setSessionId(id: String) {
        prefs.edit().putString(Constants.KEY_SESSION_ID, id).apply()
    }

    // ── User Info ──

    fun getUserInfo(): UserInfo? {
        val raw = prefs.getString(Constants.KEY_USER_INFO, null) ?: return null
        return try {
            json.decodeFromString<UserInfo>(raw)
        } catch (_: Exception) {
            null
        }
    }

    fun setUserInfo(user: UserInfo) {
        prefs.edit().putString(Constants.KEY_USER_INFO, json.encodeToString(user)).apply()
    }

    // ── Clear ──

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val TAG = "RayaChat.Prefs"
    }
}
