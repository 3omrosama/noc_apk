package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class VmPowerState {
    @Json(name = "RUNNING") RUNNING,
    @Json(name = "STOPPED") STOPPED,
    @Json(name = "SUSPENDED") SUSPENDED,
    @Json(name = "UNKNOWN") UNKNOWN;

    val displayName: String
        get() = when (this) {
            RUNNING -> "RUNNING"
            STOPPED -> "STOPPED"
            SUSPENDED -> "SUSPENDED"
            UNKNOWN -> "UNKNOWN"
        }

    companion object {
        fun fromString(state: String?): VmPowerState {
            return when (state?.uppercase()) {
                "POWEREDON", "POWERED_ON", "RUNNING", "UP", "ONLINE" -> RUNNING
                "POWEREDOFF", "POWERED_OFF", "STOPPED", "DOWN", "OFF" -> STOPPED
                "SUSPENDED", "PAUSED" -> SUSPENDED
                else -> UNKNOWN
            }
        }
    }
}

enum class VmActionType(val actionKey: String, val displayName: String, val isDestructive: Boolean) {
    POWER_ON("powerOn", "Power On", false),
    POWER_OFF("powerOff", "Power Off", true),
    RESTART("restart", "Restart", true),
    RESET("reset", "Reset", true),
    SUSPEND("suspend", "Suspend", true);

    val confirmationMessage: String
        get() = when (this) {
            POWER_OFF -> "Are you sure you want to forcibly power off this virtual machine? Unsaved data may be lost."
            RESTART -> "Are you sure you want to restart this virtual machine?"
            RESET -> "Are you sure you want to reset this virtual machine immediately?"
            SUSPEND -> "Are you sure you want to suspend this virtual machine?"
            POWER_ON -> "Power on this virtual machine?"
        }
}

@JsonClass(generateAdapter = true)
data class VmTelemetry(
    @Json(name = "cpuPercent") val cpuPercent: Double? = null,
    @Json(name = "memoryPercent") val memoryPercent: Double? = null,
    @Json(name = "diskReadBytesSec") val diskReadBytesSec: Long? = null,
    @Json(name = "diskWriteBytesSec") val diskWriteBytesSec: Long? = null,
    @Json(name = "netRxBytesSec") val netRxBytesSec: Long? = null,
    @Json(name = "netTxBytesSec") val netTxBytesSec: Long? = null
)

@JsonClass(generateAdapter = true)
data class VirtualMachine(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "powerState") val powerState: String = "UNKNOWN",
    @Json(name = "cpuUsage") val cpuUsage: Double? = null,
    @Json(name = "cpuUsagePct") val cpuUsagePct: Double? = null,
    @Json(name = "cpuCores") val cpuCores: Int? = null,
    @Json(name = "memoryUsageBytes") val memoryUsageBytes: Long? = null,
    @Json(name = "memoryTotalBytes") val memoryTotalBytes: Long? = null,
    @Json(name = "memoryUsagePct") val memoryUsagePct: Double? = null,
    @Json(name = "ipAddress") val ipAddress: String? = null,
    @Json(name = "host") val host: String? = null,
    @Json(name = "hostName") val hostName: String? = null,
    @Json(name = "connectionId") val connectionId: String? = null,
    @Json(name = "connectionName") val connectionName: String? = null,
    @Json(name = "guestOs") val guestOs: String? = null,
    @Json(name = "uptimeSeconds") val uptimeSeconds: Long? = null,
    @Json(name = "latestTelemetry") val latestTelemetry: VmTelemetry? = null,
    @Json(name = "lastUpdateTime") val lastUpdateTime: Long? = null
) {
    val state: VmPowerState
        get() = VmPowerState.fromString(powerState)

    val effectiveHost: String
        get() = hostName ?: host ?: "—"

    val effectiveConnection: String
        get() = connectionName ?: connectionId ?: "—"

    val effectiveCpuUsage: Double?
        get() = cpuUsage ?: cpuUsagePct

    val memoryUsagePercent: Double?
        get() = memoryUsagePct ?: if (memoryUsageBytes != null && memoryTotalBytes != null && memoryTotalBytes > 0) {
            (memoryUsageBytes.toDouble() / memoryTotalBytes.toDouble()) * 100.0
        } else null

    fun availableActions(): List<VmActionType> {
        return when (state) {
            VmPowerState.RUNNING -> listOf(
                VmActionType.POWER_OFF,
                VmActionType.RESTART,
                VmActionType.RESET,
                VmActionType.SUSPEND
            )
            VmPowerState.STOPPED -> listOf(
                VmActionType.POWER_ON
            )
            VmPowerState.SUSPENDED -> listOf(
                VmActionType.POWER_ON,
                VmActionType.POWER_OFF
            )
            VmPowerState.UNKNOWN -> listOf(
                VmActionType.POWER_ON,
                VmActionType.POWER_OFF,
                VmActionType.RESET
            )
        }
    }
}

@JsonClass(generateAdapter = true)
data class VmActionRequest(
    @Json(name = "action") val action: String,
    @Json(name = "reason") val reason: String? = null
)

@JsonClass(generateAdapter = true)
data class VmActionResult(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String = "Action completed",
    @Json(name = "taskId") val taskId: String? = null
)
