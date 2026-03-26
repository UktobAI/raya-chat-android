package ai.teammates.rayachat.ui.adapters

/** Result from stopping a recording. */
data class AudioResult(val uri: String, val base64: String? = null)

/** Pluggable adapter for audio recording. Mic button hidden if not provided. */
interface AudioRecorderAdapter {
    suspend fun startRecording()
    suspend fun stopRecording(): AudioResult
    suspend fun pauseRecording()
    suspend fun resumeRecording()
    /** Returns amplitude normalized to 0f..1f for waveform visualization. */
    suspend fun getAmplitude(): Float
    suspend fun cleanup()
}
