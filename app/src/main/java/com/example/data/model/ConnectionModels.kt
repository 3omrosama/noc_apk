package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class ConnectionType {
    @Json(name = "esxi") ESXI,
    @Json(name = "casaos") CASAOS,
    @Json(name = "generic") GENERIC;

    val displayName: String
        get() = when (this) {
            ESXI -> "VMware ESXi"
            CASAOS -> "CasaOS"
            GENERIC -> "Generic Host"
        }

    companion object {
        fun fromString(type: String?): ConnectionType {
            return when (type?.lowercase()) {
                "esxi", "vmware", "vsphere" -> ESXI
                "casaos", "casa_os" -> CASAOS
                else -> GENERIC
            }
        }
    }
}

enum class ConnectionStatus {
    @Json(name = "online") ONLINE,
    @Json(name = "offline") OFFLINE,
    @Json(name = "degraded") DEGRADED;

    val displayName: String
        get() = when (this) {
            ONLINE -> "Online"
            OFFLINE -> "Offline"
            DEGRADED -> "Degraded"
        }

    companion object {
        fun fromString(status: String?): ConnectionStatus {
            return when (status?.lowercase()) {
                "online", "healthy", "up", "connected" -> ONLINE
                "degraded", "warning" -> DEGRADED
                else -> OFFLINE
            }
        }
    }
}

@JsonClass(generateAdapter = true)
data class InfraConnection(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "type") val type: String = "generic",
    @Json(name = "host") val host: String? = null,
    @Json(name = "endpoint") val endpoint: String? = null,
    @Json(name = "status") val status: String = "offline",
    @Json(name = "latencyMs") val latencyMs: Long? = null,
    @Json(name = "lastSeen") val lastSeen: Long? = null,
    @Json(name = "version") val version: String? = null,
    @Json(name = "vmCount") val vmCount: Int? = null,
    @Json(name = "appCount") val appCount: Int? = null,
    @Json(name = "cpuUsage") val cpuUsage: Double? = null,
    @Json(name = "memoryUsage") val memoryUsage: Double? = null,
    @Json(name = "storageUsage") val storageUsage: Double? = null
) {
    val connectionType: ConnectionType
        get() = ConnectionType.fromString(type)

    val connectionStatus: ConnectionStatus
        get() = ConnectionStatus.fromString(status)

    val effectiveEndpoint: String
        get() = endpoint ?: host ?: "—"
}
