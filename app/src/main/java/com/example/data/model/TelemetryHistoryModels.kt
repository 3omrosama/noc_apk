package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class TelemetryTimeRange(val queryValue: String, val label: String) {
    ONE_HOUR("1h", "1 Hour"),
    SIX_HOURS("6h", "6 Hours"),
    TWENTY_FOUR_HOURS("24h", "24 Hours"),
    SEVEN_DAYS("7d", "7 Days")
}

@JsonClass(generateAdapter = false)
data class TelemetryHistoryPoint(
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "cpu") val cpu: Double? = null,
    @Json(name = "memory") val memory: Double? = null,
    @Json(name = "storage") val storage: Double? = null,
    @Json(name = "networkRx") val networkRx: Double? = null,
    @Json(name = "networkTx") val networkTx: Double? = null
)

@JsonClass(generateAdapter = true)
data class MonitoringMetricsResponse(
    @Json(name = "range") val range: String? = null,
    @Json(name = "data") val data: List<TelemetryHistoryPoint> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TelemetryHistoryResponse(
    @Json(name = "range") val range: String = "1h",
    @Json(name = "points") val points: List<TelemetryHistoryPoint> = emptyList()
)
