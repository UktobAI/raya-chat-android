package ai.teammates.rayachat.core.adapters

/** Info returned after loading audio. */
data class AudioInfo(val durationMs: Long)

/** Pluggable adapter for audio playback. */
interface AudioPlayerAdapter {
    suspend fun loadAudio(uri: String): AudioInfo
    suspend fun play()
    suspend fun pause()
    suspend fun seekTo(positionMs: Long)
    suspend fun getPosition(): Long
    suspend fun cleanup()
}
