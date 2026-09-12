package com.example.data.api

import com.example.data.model.*
import com.example.data.security.SecureSessionManager
import com.example.data.security.UrlNormalizer
import com.squareup.moshi.Moshi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow

class NocWebSocketClient(
    private val sessionManager: SecureSessionManager,
    private val moshi: Moshi
) {
    private val _connectionStatus = MutableStateFlow(WsConnectionStatus.OFFLINE)
    val connectionStatus: StateFlow<WsConnectionStatus> = _connectionStatus.asStateFlow()

    private val _events = MutableSharedFlow<WsEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<WsEvent> = _events.asSharedFlow()

    private var client: OkHttpClient? = null
    private var webSocket: WebSocket? = null
    private var connectionScope: CoroutineScope? = null
    private var reconnectAttempts = 0
    private var shouldKeepConnected = false

    private val alertAdapter = moshi.adapter(Alert::class.java)

    fun start() {
        shouldKeepConnected = true
        reconnectAttempts = 0
        val baseUrl = sessionManager.getServerUrl() ?: return
        connect(baseUrl)
    }

    fun stop() {
        shouldKeepConnected = false
        connectionScope?.cancel()
        connectionScope = null
        webSocket?.close(1000, "Client stopped")
        webSocket = null
        _connectionStatus.value = WsConnectionStatus.OFFLINE
    }

    private fun connect(baseUrl: String) {
        val wsUrl = UrlNormalizer.toWebSocketUrl(baseUrl, "ws")
        val token = sessionManager.getToken()

        if (client == null) {
            client = OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .pingInterval(20, TimeUnit.SECONDS)
                .build()
        }

        val requestBuilder = Request.Builder().url(wsUrl)
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        connectionScope?.cancel()
        connectionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        webSocket?.cancel()
        _connectionStatus.value = if (reconnectAttempts > 0) WsConnectionStatus.RECONNECTING else WsConnectionStatus.OFFLINE

        webSocket = client?.newWebSocket(requestBuilder.build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempts = 0
                _connectionStatus.value = WsConnectionStatus.LIVE
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseAndEmitMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionStatus.value = WsConnectionStatus.OFFLINE
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionStatus.value = WsConnectionStatus.OFFLINE
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (!shouldKeepConnected) return
        val baseUrl = sessionManager.getServerUrl() ?: return

        connectionScope?.launch {
            _connectionStatus.value = WsConnectionStatus.RECONNECTING
            reconnectAttempts++
            // Bounded exponential backoff: 1s, 2s, 4s, 8s, up to 30s
            val delaySeconds = min(30.0, 2.0.pow(min(reconnectAttempts.toDouble(), 5.0))).toLong()
            delay(delaySeconds * 1000)
            if (shouldKeepConnected) {
                connect(baseUrl)
            }
        }
    }

    private fun parseAndEmitMessage(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            val type = json.optString("type", json.optString("event", ""))

            when (type.lowercase()) {
                "telemetry", "metrics" -> {
                    val dataObj = json.optJSONObject("data") ?: json.optJSONObject("payload")
                    if (dataObj != null) {
                        val cpuVal = if (dataObj.has("cpu")) dataObj.optDouble("cpu") else null
                        val memVal = if (dataObj.has("memory")) dataObj.optDouble("memory") else null
                        val storageVal = if (dataObj.has("storage")) dataObj.optDouble("storage") else null
                        val netVal = if (dataObj.has("network")) dataObj.optDouble("network") else null

                        val event = WsEvent.TelemetryUpdate(
                            cpu = cpuVal?.let { MetricValue(it, "%", 0.0, System.currentTimeMillis()) },
                            memory = memVal?.let { MetricValue(it, "%", 0.0, System.currentTimeMillis()) },
                            storage = storageVal?.let { MetricValue(it, "%", 0.0, System.currentTimeMillis()) },
                            network = netVal?.let { MetricValue(it, "MB/s", 0.0, System.currentTimeMillis()) },
                            timestamp = System.currentTimeMillis()
                        )
                        _events.tryEmit(event)
                    }
                }
                "alert", "alert_created", "alert_updated" -> {
                    val alertJson = json.optJSONObject("data")?.toString() ?: json.optJSONObject("payload")?.toString()
                    if (alertJson != null) {
                        alertAdapter.fromJson(alertJson)?.let {
                            _events.tryEmit(WsEvent.AlertUpdate(it))
                        }
                    }
                }
                "connection", "connection_status" -> {
                    val dataObj = json.optJSONObject("data") ?: json.optJSONObject("payload")
                    if (dataObj != null) {
                        val id = dataObj.optString("id", "")
                        val statusStr = dataObj.optString("status", "offline")
                        val latency = if (dataObj.has("latencyMs")) dataObj.optLong("latencyMs") else null
                        if (id.isNotEmpty()) {
                            _events.tryEmit(
                                WsEvent.ConnectionStatusUpdate(
                                    connectionId = id,
                                    status = ConnectionStatus.fromString(statusStr),
                                    latencyMs = latency
                                )
                            )
                        }
                    }
                }
                "vm", "vm_status", "vm_update" -> {
                    val dataObj = json.optJSONObject("data") ?: json.optJSONObject("payload")
                    if (dataObj != null) {
                        val vmId = dataObj.optString("id", "")
                        val powerStateStr = dataObj.optString("powerState", "UNKNOWN")
                        val cpu = if (dataObj.has("cpuUsage")) dataObj.optDouble("cpuUsage") else null
                        val mem = if (dataObj.has("memoryUsageBytes")) dataObj.optLong("memoryUsageBytes") else null
                        if (vmId.isNotEmpty()) {
                            _events.tryEmit(
                                WsEvent.VmUpdate(
                                    vmId = vmId,
                                    powerState = VmPowerState.fromString(powerStateStr),
                                    cpuUsage = cpu,
                                    memoryUsageBytes = mem,
                                    lastUpdateTime = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
                else -> {
                    _events.tryEmit(WsEvent.RawMessage(type, jsonString))
                }
            }
        } catch (_: Exception) {
            // Ignore malformed individual WebSocket messages without crashing
        }
    }
}
