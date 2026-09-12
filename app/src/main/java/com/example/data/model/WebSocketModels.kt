package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class WsConnectionStatus(val label: String) {
    LIVE("LIVE"),
    RECONNECTING("RECONNECTING"),
    OFFLINE("OFFLINE")
}

sealed class WsEvent {
    data class TelemetryUpdate(
        val cpu: MetricValue?,
        val memory: MetricValue?,
        val storage: MetricValue?,
        val network: MetricValue?,
        val timestamp: Long
    ) : WsEvent()

    data class AlertUpdate(val alert: Alert) : WsEvent()

    data class ConnectionStatusUpdate(
        val connectionId: String,
        val status: ConnectionStatus,
        val latencyMs: Long?
    ) : WsEvent()

    data class VmUpdate(
        val vmId: String,
        val powerState: VmPowerState,
        val cpuUsage: Double?,
        val memoryUsageBytes: Long?,
        val lastUpdateTime: Long
    ) : WsEvent()

    data class RawMessage(val type: String, val payload: String) : WsEvent()
}

@JsonClass(generateAdapter = true)
data class WsIncomingMessage(
    @Json(name = "type") val type: String? = null,
    @Json(name = "event") val event: String? = null,
    @Json(name = "action") val action: String? = null,
    @Json(name = "timestamp") val timestamp: Long? = null
)
