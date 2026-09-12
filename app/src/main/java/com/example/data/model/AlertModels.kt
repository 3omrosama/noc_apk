package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class AlertSeverity {
    @Json(name = "critical") CRITICAL,
    @Json(name = "warning") WARNING,
    @Json(name = "info") INFO;

    val displayName: String
        get() = when (this) {
            CRITICAL -> "Critical"
            WARNING -> "Warning"
            INFO -> "Info"
        }

    companion object {
        fun fromString(s: String?): AlertSeverity {
            return when (s?.lowercase()) {
                "critical", "error", "fatal" -> CRITICAL
                "warning", "warn" -> WARNING
                else -> INFO
            }
        }
    }
}

enum class AlertStatus {
    @Json(name = "active") ACTIVE,
    @Json(name = "acknowledged") ACKNOWLEDGED,
    @Json(name = "resolved") RESOLVED;

    val displayName: String
        get() = when (this) {
            ACTIVE -> "Active"
            ACKNOWLEDGED -> "Acknowledged"
            RESOLVED -> "Resolved"
        }

    companion object {
        fun fromString(s: String?): AlertStatus {
            return when (s?.lowercase()) {
                "acknowledged", "ack" -> ACKNOWLEDGED
                "resolved", "closed" -> RESOLVED
                else -> ACTIVE
            }
        }
    }
}

@JsonClass(generateAdapter = true)
data class Alert(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "message") val message: String,
    @Json(name = "severity") val severity: String = "info",
    @Json(name = "status") val status: String = "active",
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @Json(name = "source") val source: String? = null,
    @Json(name = "connectionId") val connectionId: String? = null,
    @Json(name = "connectionName") val connectionName: String? = null,
    @Json(name = "acknowledgedBy") val acknowledgedBy: String? = null,
    @Json(name = "acknowledgedAt") val acknowledgedAt: Long? = null,
    @Json(name = "resolvedBy") val resolvedBy: String? = null,
    @Json(name = "resolvedAt") val resolvedAt: Long? = null,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "updatedAt") val updatedAt: String? = null
) {
    val alertSeverity: AlertSeverity
        get() = AlertSeverity.fromString(severity)

    val alertStatus: AlertStatus
        get() = AlertStatus.fromString(status)

    val effectiveSource: String
        get() = connectionName ?: source ?: "System"

    val effectiveTimestamp: Long
        get() {
            if (createdAt != null) {
                try {
                    return java.time.Instant.parse(createdAt).toEpochMilli()
                } catch (_: Exception) {}
            }
            return timestamp
        }
}

@JsonClass(generateAdapter = true)
data class AlertActionResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String = "Updated",
    @Json(name = "alert") val alert: Alert? = null
)

@JsonClass(generateAdapter = true)
data class AlertRule(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String,
    @Json(name = "metric") val metric: String,
    @Json(name = "condition") val condition: String = "gt",
    @Json(name = "threshold") val threshold: Double,
    @Json(name = "durationSec") val durationSec: Int = 60,
    @Json(name = "severity") val severity: String = "CRITICAL"
)
