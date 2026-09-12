package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MetricValue(
    @Json(name = "value") val value: Double? = null,
    @Json(name = "unit") val unit: String? = null,
    @Json(name = "trend") val trend: Double? = null,
    @Json(name = "timestamp") val timestamp: Long? = null
) {
    val isAvailable: Boolean
        get() = value != null

    val displayString: String
        get() = when {
            value == null -> "—"
            unit != null -> String.format("%.1f%s", value, unit)
            else -> String.format("%.1f", value)
        }
}

@JsonClass(generateAdapter = true)
data class DashboardNodesSummary(
    @Json(name = "total") val total: Int = 0,
    @Json(name = "online") val online: Int = 0,
    @Json(name = "offline") val offline: Int = 0,
    @Json(name = "degraded") val degraded: Int = 0,
    @Json(name = "warning") val warning: Int = 0
) {
    val effectiveDegraded: Int
        get() = if (warning > 0) warning else degraded
}

@JsonClass(generateAdapter = true)
data class DashboardVmsSummary(
    @Json(name = "total") val total: Int = 0,
    @Json(name = "running") val running: Int = 0,
    @Json(name = "stopped") val stopped: Int = 0,
    @Json(name = "suspended") val suspended: Int = 0
)

@JsonClass(generateAdapter = true)
data class DashboardContainersSummary(
    @Json(name = "total") val total: Int = 0,
    @Json(name = "running") val running: Int = 0,
    @Json(name = "stopped") val stopped: Int = 0
)

@JsonClass(generateAdapter = true)
data class DashboardMetrics(
    @Json(name = "cpuUtilizationPct") val cpuUtilizationPct: Double? = null,
    @Json(name = "cpuCoresTotal") val cpuCoresTotal: Int? = null,
    @Json(name = "memoryUtilizationPct") val memoryUtilizationPct: Double? = null,
    @Json(name = "memoryBytesUsed") val memoryBytesUsed: Long? = null,
    @Json(name = "memoryBytesTotal") val memoryBytesTotal: Long? = null,
    @Json(name = "storageUtilizationPct") val storageUtilizationPct: Double? = null,
    @Json(name = "storageBytesUsed") val storageBytesUsed: Long? = null,
    @Json(name = "storageBytesTotal") val storageBytesTotal: Long? = null,
    @Json(name = "networkTrafficRxKbps") val networkTrafficRxKbps: Double? = null,
    @Json(name = "networkTrafficTxKbps") val networkTrafficTxKbps: Double? = null
)

@JsonClass(generateAdapter = false)
data class DashboardHistoricalMetric(
    @Json(name = "timestamp") val timestamp: Long = 0L,
    @Json(name = "cpu") val cpu: Double? = null,
    @Json(name = "memory") val memory: Double? = null,
    @Json(name = "storage") val storage: Double? = null,
    @Json(name = "networkRxKbps") val networkRxKbps: Double? = null,
    @Json(name = "networkTxKbps") val networkTxKbps: Double? = null
) {
    fun toTelemetryHistoryPoint(): TelemetryHistoryPoint {
        return TelemetryHistoryPoint(
            timestamp = timestamp,
            cpu = cpu,
            memory = memory,
            storage = storage,
            networkRx = networkRxKbps,
            networkTx = networkTxKbps
        )
    }
}

@JsonClass(generateAdapter = true)
data class DashboardResponse(
    @Json(name = "healthScore") val healthScore: Int? = null,
    @Json(name = "nodes") val nodes: DashboardNodesSummary? = null,
    @Json(name = "vms") val vms: DashboardVmsSummary? = null,
    @Json(name = "containers") val containers: DashboardContainersSummary? = null,
    @Json(name = "metrics") val metrics: DashboardMetrics? = null,
    @Json(name = "historicalMetrics") val historicalMetrics: List<DashboardHistoricalMetric> = emptyList(),
    @Json(name = "activeAlerts") val activeAlerts: List<Alert> = emptyList(),
    @Json(name = "recentEvents") val recentEvents: List<Map<String, Any?>> = emptyList(),
    @Json(name = "recentAuditLogs") val recentAuditLogs: List<Map<String, Any?>> = emptyList(),
    @Json(name = "hasLiveInfrastructure") val hasLiveInfrastructure: Boolean = false,
    @Json(name = "isDemoMode") val isDemoMode: Boolean = false
) {
    fun toInfrastructureOverview(): InfrastructureOverview {
        val totalAlerts = activeAlerts.size
        val criticalCount = activeAlerts.count { it.severity.equals("CRITICAL", ignoreCase = true) }

        val cpuVal = metrics?.cpuUtilizationPct
        val memVal = metrics?.memoryUtilizationPct
        val storageVal = metrics?.storageUtilizationPct
        val netVal = metrics?.networkTrafficRxKbps

        return InfrastructureOverview(
            totalConnections = nodes?.total ?: 0,
            onlineConnections = nodes?.online ?: 0,
            offlineConnections = nodes?.offline ?: 0,
            degradedConnections = nodes?.effectiveDegraded ?: 0,
            totalVms = vms?.total ?: 0,
            runningVms = vms?.running ?: 0,
            stoppedVms = vms?.stopped ?: 0,
            suspendedVms = vms?.suspended ?: 0,
            activeAlerts = totalAlerts,
            criticalAlerts = criticalCount,
            cpu = if (cpuVal != null) MetricValue(value = cpuVal, unit = "%") else null,
            memory = if (memVal != null) MetricValue(value = memVal, unit = "%") else null,
            storage = if (storageVal != null) MetricValue(value = storageVal, unit = "%") else null,
            network = if (netVal != null) MetricValue(value = netVal, unit = " kbps") else null,
            isDemo = isDemoMode,
            demoMode = isDemoMode,
            timestamp = System.currentTimeMillis()
        )
    }
}

@JsonClass(generateAdapter = true)
data class InfrastructureOverview(
    @Json(name = "totalConnections") val totalConnections: Int = 0,
    @Json(name = "onlineConnections") val onlineConnections: Int = 0,
    @Json(name = "offlineConnections") val offlineConnections: Int = 0,
    @Json(name = "degradedConnections") val degradedConnections: Int = 0,
    @Json(name = "totalVms") val totalVms: Int = 0,
    @Json(name = "runningVms") val runningVms: Int = 0,
    @Json(name = "stoppedVms") val stoppedVms: Int = 0,
    @Json(name = "suspendedVms") val suspendedVms: Int = 0,
    @Json(name = "activeAlerts") val activeAlerts: Int = 0,
    @Json(name = "criticalAlerts") val criticalAlerts: Int = 0,
    @Json(name = "cpu") val cpu: MetricValue? = null,
    @Json(name = "memory") val memory: MetricValue? = null,
    @Json(name = "storage") val storage: MetricValue? = null,
    @Json(name = "network") val network: MetricValue? = null,
    @Json(name = "isDemo") val isDemo: Boolean = false,
    @Json(name = "demoMode") val demoMode: Boolean = false,
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
) {
    val effectiveIsDemo: Boolean
        get() = isDemo || demoMode

    val isStale: Boolean
        get() = (System.currentTimeMillis() - timestamp) > 60_000
}
