package ai.teammates.rayachat.core.websocket

import android.util.Log
import ai.teammates.rayachat.core.Constants
import ai.teammates.rayachat.core.models.ConnectionStatus
import kotlinx.coroutines.*
import okhttp3.*
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "RayaChat.WS"

/**
 * Production-grade WebSocket manager built on OkHttp.
 *
 * Features:
 * - Heartbeat ping/pong (25s interval, 60s timeout)
 * - Exponential backoff reconnection (max 100 attempts, 30s cap)
 * - Message queue during CONNECTING state
 * - App foreground/background awareness
 * - Thread-safe destroyed flag
 */
class WebSocketManager(
    private var url: String,
    private val callbacks: WebSocketCallbacks,
    private val scope: CoroutineScope,
) {
    interface WebSocketCallbacks {
        fun onOpen()
        fun onMessage(text: String)
        fun onClose(code: Int, reason: String)
        fun onError(error: String)
        fun onStatusChange(status: ConnectionStatus)
    }

    private val client = OkHttpClient.Builder()
        .retryOnConnectionFailure(false)
        .build()

    @Volatile private var webSocket: WebSocket? = null
    private val destroyed = AtomicBoolean(false)
    @Volatile private var manualClose = false
    @Volatile private var shouldNotReconnect = false
    @Volatile private var reconnectAttempts = 0
    @Volatile private var isAppActive = true

    private var heartbeatJob: Job? = null
    private var heartbeatTimeoutJob: Job? = null
    private var reconnectJob: Job? = null

    private val messageQueue = MessageQueue()

    private var _status: ConnectionStatus = ConnectionStatus.DISCONNECTED
    val status: ConnectionStatus get() = _status
    val isConnected: Boolean get() = webSocket != null && _status == ConnectionStatus.CONNECTED

    // ── Connect ──

    fun connect() {
        if (destroyed.get()) return
        // Prevent duplicate connections
        if (_status == ConnectionStatus.CONNECTED || _status == ConnectionStatus.CONNECTING) return

        manualClose = false
        shouldNotReconnect = false
        updateStatus(ConnectionStatus.CONNECTING)

        try {
            Log.d(TAG, "→ CONNECT: ${url.substringBefore("&token=").take(100)}...[token redacted]")
            val request = Request.Builder().url(url).build()
            webSocket = client.newWebSocket(request, createListener())
        } catch (e: Exception) {
            callbacks.onError(e.message ?: "WebSocket connection failed")
            scheduleReconnect()
        }
    }

    // ── Send ──

    fun send(data: String): Boolean {
        if (destroyed.get()) return false

        // Log outbound payload (truncate large base64)
        val logData = if (data.length > 500) data.take(500) + "...[${data.length} chars total]" else data
        Log.d(TAG, "→ SEND (${data.length} chars): $logData")

        if (_status == ConnectionStatus.CONNECTED) {
            return try {
                val result = webSocket?.send(data) ?: false
                Log.d(TAG, "→ SEND result: $result")
                result
            } catch (e: Exception) {
                Log.e(TAG, "→ SEND error: ${e.message}")
                false
            }
        }

        // Queue if connecting
        if (_status == ConnectionStatus.CONNECTING) {
            messageQueue.enqueue(data)
            return false
        }

        return false
    }

    // ── Close ──

    fun close(code: Int = Constants.WS_CLOSE_NORMAL, reason: String = "Manual close") {
        manualClose = true
        stopHeartbeat()
        cancelReconnect()
        messageQueue.clear()

        val ws = webSocket
        webSocket = null // Null ref BEFORE closing to prevent callbacks on dead socket

        ws?.let {
            try {
                it.close(code, reason)
            } catch (_: Exception) {
                // Ignore close errors
            }
        }

        updateStatus(ConnectionStatus.DISCONNECTED)
    }

    // ── Lifecycle ──

    fun preventReconnect() {
        shouldNotReconnect = true
        cancelReconnect()
    }

    fun setAppActive(active: Boolean) {
        if (destroyed.get()) return

        isAppActive = active

        if (!active) {
            stopHeartbeat()
            return
        }

        // App returning to foreground
        if (webSocket == null || _status != ConnectionStatus.CONNECTED) {
            forceReconnect()
        } else {
            startHeartbeat()
            // Verify connection with a ping
            try {
                webSocket?.send("ping")
            } catch (_: Exception) {
                forceReconnect()
            }
        }
    }

    fun updateUrl(newUrl: String) {
        url = newUrl
    }

    fun destroy() {
        destroyed.set(true)
        close()
        stopHeartbeat()
        cancelReconnect()
        messageQueue.clear()
    }

    // ── Private: WebSocket Listener ──

    private fun createListener() = object : WebSocketListener() {
        override fun onOpen(ws: WebSocket, response: Response) {
            if (destroyed.get()) return
            Log.d(TAG, "← OPEN: ${response.code} ${response.message}")
            scope.launch(Dispatchers.Main) {
                reconnectAttempts = 0
                updateStatus(ConnectionStatus.CONNECTED)
                startHeartbeat()
                flushQueue()
                callbacks.onOpen()
            }
        }

        override fun onMessage(ws: WebSocket, text: String) {
            if (destroyed.get()) return
            // Log inbound (truncate long messages)
            val logText = if (text.length > 300) text.take(300) + "...[${text.length} chars]" else text
            Log.d(TAG, "← RECV (${text.length} chars): $logText")

            scope.launch(Dispatchers.Main) {
                if (text == "pong") {
                    resetHeartbeatTimeout()
                    return@launch
                }
                callbacks.onMessage(text)
            }
        }

        override fun onClosing(ws: WebSocket, code: Int, reason: String) {
            ws.close(code, reason)
        }

        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
            if (destroyed.get()) return
            Log.d(TAG, "← CLOSED: code=$code reason=$reason")
            scope.launch(Dispatchers.Main) {
                stopHeartbeat()
                callbacks.onClose(code, reason)
                if (!manualClose && !shouldNotReconnect && shouldReconnect(code)) {
                    scheduleReconnect()
                } else {
                    updateStatus(ConnectionStatus.DISCONNECTED)
                }
            }
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            if (destroyed.get()) return
            Log.e(TAG, "← FAILURE: ${t.message}, response=${response?.code}")
            scope.launch(Dispatchers.Main) {
                stopHeartbeat()
                callbacks.onError(t.message ?: "WebSocket failure")
                if (!manualClose && !shouldNotReconnect) {
                    scheduleReconnect()
                } else {
                    updateStatus(ConnectionStatus.DISCONNECTED)
                }
            }
        }
    }

    // ── Private: Heartbeat ──

    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatJob = scope.launch {
            while (isActive && !destroyed.get()) {
                delay(Constants.HEARTBEAT_INTERVAL_MS)
                if (_status == ConnectionStatus.CONNECTED && isAppActive) {
                    try {
                        webSocket?.send("ping")
                        startHeartbeatTimeout()
                    } catch (_: Exception) {
                        forceReconnect()
                        break
                    }
                }
            }
        }
    }

    private fun startHeartbeatTimeout() {
        heartbeatTimeoutJob?.cancel()
        heartbeatTimeoutJob = scope.launch {
            delay(Constants.HEARTBEAT_TIMEOUT_MS)
            if (destroyed.get()) return@launch
            // No pong received — connection is dead
            stopHeartbeat()
            webSocket?.let {
                try {
                    it.close(Constants.WS_CLOSE_ABNORMAL, "Heartbeat timeout")
                } catch (_: Exception) {
                    // Ignore
                }
            }
        }
    }

    private fun resetHeartbeatTimeout() {
        heartbeatTimeoutJob?.cancel()
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        heartbeatTimeoutJob?.cancel()
        heartbeatTimeoutJob = null
    }

    // ── Private: Reconnection ──

    private fun scheduleReconnect() {
        if (destroyed.get() || manualClose || shouldNotReconnect || !isAppActive) {
            updateStatus(ConnectionStatus.DISCONNECTED)
            return
        }

        if (reconnectAttempts >= Constants.MAX_RECONNECT_ATTEMPTS) {
            callbacks.onError("Connection lost after ${Constants.MAX_RECONNECT_ATTEMPTS} attempts")
            updateStatus(ConnectionStatus.DISCONNECTED)
            return
        }

        updateStatus(ConnectionStatus.RECONNECTING)

        val delay = calculateReconnectDelay(reconnectAttempts)

        reconnectJob = scope.launch {
            delay(delay)
            if (destroyed.get()) return@launch
            reconnectAttempts++
            // Null out old socket before reconnecting
            webSocket = null
            _status = ConnectionStatus.DISCONNECTED // Reset so connect() doesn't short-circuit
            connect()
        }
    }

    private fun calculateReconnectDelay(attempt: Int): Long {
        val exponential = Constants.BASE_RECONNECT_DELAY_MS * (1L shl attempt.coerceAtMost(20))
        val jitter = (Math.random() * 1000).toLong()
        return (exponential + jitter).coerceAtMost(Constants.MAX_RECONNECT_DELAY_MS)
    }

    private fun cancelReconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
    }

    private fun forceReconnect() {
        if (destroyed.get()) return
        cancelReconnect()
        stopHeartbeat()

        val ws = webSocket
        webSocket = null
        ws?.let {
            try { it.close(Constants.WS_CLOSE_NORMAL, "Force reconnect") } catch (_: Exception) {}
        }

        reconnectAttempts = 0
        _status = ConnectionStatus.DISCONNECTED
        connect()
    }

    private fun shouldReconnect(code: Int): Boolean = when (code) {
        Constants.WS_CLOSE_NORMAL, Constants.WS_CLOSE_GOING_AWAY -> false
        else -> true
    }

    // ── Private: Queue ──

    private fun flushQueue() {
        messageQueue.flush { msg ->
            try {
                webSocket?.send(msg) ?: false
            } catch (_: Exception) {
                false
            }
        }
    }

    // ── Private: Status ──

    private fun updateStatus(status: ConnectionStatus) {
        if (destroyed.get()) return
        _status = status
        callbacks.onStatusChange(status)
    }
}
