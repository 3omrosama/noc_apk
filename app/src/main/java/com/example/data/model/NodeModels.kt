package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CasaOsApp(
    @Json(name = "id") val id: String,
    @Json(name = "connectionId") val connectionId: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "icon") val icon: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "status") val status: String = "running",
    @Json(name = "port") val port: Int? = null,
    @Json(name = "memoryUsageBytes") val memoryUsageBytes: Long? = null,
    @Json(name = "description") val description: String? = null
) {
    val displayName: String
        get() = title ?: name ?: id
}

@JsonClass(generateAdapter = true)
data class DockerContainer(
    @Json(name = "id") val id: String,
    @Json(name = "connectionId") val connectionId: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "names") val names: List<String> = emptyList(),
    @Json(name = "image") val image: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "status") val status: String = "running",
    @Json(name = "created") val created: Long? = null
)

@JsonClass(generateAdapter = true)
data class EsxiHost(
    @Json(name = "id") val id: String,
    @Json(name = "connectionId") val connectionId: String? = null,
    @Json(name = "connectionName") val connectionName: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "hostname") val hostname: String? = null,
    @Json(name = "ip") val ip: String? = null,
    @Json(name = "ipAddress") val ipAddress: String? = null,
    @Json(name = "version") val version: String? = null,
    @Json(name = "uptimeSeconds") val uptimeSeconds: Long? = null,
    @Json(name = "status") val status: String = "online",
    @Json(name = "powerState") val powerState: String? = null,
    @Json(name = "cpuCores") val cpuCores: Int? = null,
    @Json(name = "cpuMhzTotal") val cpuMhzTotal: Long? = null,
    @Json(name = "cpuUsagePercent") val cpuUsagePercent: Double? = null,
    @Json(name = "cpuUsagePct") val cpuUsagePct: Double? = null,
    @Json(name = "memoryUsedBytes") val memoryUsedBytes: Long? = null,
    @Json(name = "memoryTotalBytes") val memoryTotalBytes: Long? = null,
    @Json(name = "memoryBytesTotal") val memoryBytesTotal: Long? = null,
    @Json(name = "memoryUsagePercent") val memoryUsagePercent: Double? = null,
    @Json(name = "memoryUsagePct") val memoryUsagePct: Double? = null,
    @Json(name = "storageUsedBytes") val storageUsedBytes: Long? = null,
    @Json(name = "storageTotalBytes") val storageTotalBytes: Long? = null,
    @Json(name = "storageUsagePercent") val storageUsagePercent: Double? = null,
    @Json(name = "storageUsagePct") val storageUsagePct: Double? = null,
    @Json(name = "latencyMs") val latencyMs: Long? = null,
    @Json(name = "vmCount") val vmCount: Int? = null
) {
    val displayName: String
        get() = hostname ?: name ?: "ESXi Host"

    val effectiveIp: String?
        get() = ipAddress ?: ip

    val effectiveCpuUsagePercent: Double?
        get() = cpuUsagePercent ?: cpuUsagePct

    val effectiveMemoryUsagePercent: Double?
        get() = memoryUsagePercent ?: memoryUsagePct

    val effectiveStorageUsagePercent: Double?
        get() = storageUsagePercent ?: storageUsagePct

    val connectionStatus: ConnectionStatus
        get() = ConnectionStatus.fromString(powerState ?: status)

    val formattedUptime: String
        get() {
            val s = uptimeSeconds ?: return "—"
            val days = s / 86400
            val hours = (s % 86400) / 3600
            val mins = (s % 3600) / 60
            return if (days > 0) "${days}d ${hours}h" else "${hours}h ${mins}m"
        }
}

@JsonClass(generateAdapter = true)
data class CasaOsHost(
    @Json(name = "id") val id: String,
    @Json(name = "connectionId") val connectionId: String? = null,
    @Json(name = "connectionName") val connectionName: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "hostname") val hostname: String? = null,
    @Json(name = "endpoint") val endpoint: String? = null,
    @Json(name = "ipAddress") val ipAddress: String? = null,
    @Json(name = "version") val version: String? = null,
    @Json(name = "kernelVersion") val kernelVersion: String? = null,
    @Json(name = "status") val status: String = "online",
    @Json(name = "cpuUsagePercent") val cpuUsagePercent: Double? = null,
    @Json(name = "cpuUsagePct") val cpuUsagePct: Double? = null,
    @Json(name = "memoryUsagePercent") val memoryUsagePercent: Double? = null,
    @Json(name = "memoryUsagePct") val memoryUsagePct: Double? = null,
    @Json(name = "storageUsagePercent") val storageUsagePercent: Double? = null,
    @Json(name = "storageUsagePct") val storageUsagePct: Double? = null,
    @Json(name = "runningAppsCount") val runningAppsCount: Int? = null,
    @Json(name = "totalAppsCount") val totalAppsCount: Int? = null,
    @Json(name = "apps") val apps: List<CasaOsApp> = emptyList(),
    @Json(name = "uptimeSeconds") val uptimeSeconds: Long? = null
) {
    val displayName: String
        get() = hostname ?: name ?: "CasaOS Host"

    val effectiveEndpoint: String?
        get() = endpoint ?: ipAddress

    val effectiveCpuUsagePercent: Double?
        get() = cpuUsagePercent ?: cpuUsagePct

    val effectiveMemoryUsagePercent: Double?
        get() = memoryUsagePercent ?: memoryUsagePct

    val effectiveStorageUsagePercent: Double?
        get() = storageUsagePercent ?: storageUsagePct

    val connectionStatus: ConnectionStatus
        get() = ConnectionStatus.fromString(status)
}

@JsonClass(generateAdapter = true)
data class NodesResponse(
    @Json(name = "esxiHosts") val esxiHosts: List<EsxiHost> = emptyList(),
    @Json(name = "casaOsHosts") val casaOsHosts: List<CasaOsHost> = emptyList()
)
