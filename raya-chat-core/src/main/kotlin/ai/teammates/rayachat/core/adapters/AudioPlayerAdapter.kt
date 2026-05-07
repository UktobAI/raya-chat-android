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

    /**
     * Peak amplitude (0..1) for each of [sampleCount] time-buckets across the loaded audio,
     * used by the in-bubble waveform UI. Returns null if amplitude data cannot be extracted
     * (e.g. non-WAV codec, load not yet complete, or [sampleCount] <= 0). The UI degrades
     * gracefully to a dashed line when null is returned.
     */
    suspend fun getAmplitudes(sampleCount: Int): FloatArray? = null
}
