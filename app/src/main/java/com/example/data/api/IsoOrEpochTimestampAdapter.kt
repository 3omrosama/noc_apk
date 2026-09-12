package com.example.data.api

import com.example.data.model.DashboardHistoricalMetric
import com.example.data.model.TelemetryHistoryPoint
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

object IsoOrEpochTimestampParser {
    fun parse(reader: JsonReader): Long {
        return when (val token = reader.peek()) {
            JsonReader.Token.NUMBER -> {
                reader.nextLong()
            }
            JsonReader.Token.STRING -> {
                val raw = reader.nextString().trim()
                if (raw.isEmpty()) {
                    throw JsonDataException("Timestamp string cannot be blank")
                }
                raw.toLongOrNull() ?: try {
                    Instant.parse(raw).toEpochMilli()
                } catch (e: DateTimeParseException) {
                    try {
                        OffsetDateTime.parse(raw).toInstant().toEpochMilli()
                    } catch (e2: Exception) {
                        throw JsonDataException("Cannot parse timestamp '$raw': invalid ISO-8601 or epoch format", e)
                    }
                }
            }
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                throw JsonDataException("Non-null timestamp cannot be null")
            }
            else -> {
                reader.skipValue()
                throw JsonDataException("Expected NUMBER or STRING for timestamp, but found $token")
            }
        }
    }
}

class DashboardHistoricalMetricAdapter : JsonAdapter<DashboardHistoricalMetric>() {
    @FromJson
    override fun fromJson(reader: JsonReader): DashboardHistoricalMetric? {
        if (reader.peek() == JsonReader.Token.NULL) {
            return reader.nextNull()
        }
        var timestamp = 0L
        var cpu: Double? = null
        var memory: Double? = null
        var storage: Double? = null
        var networkRxKbps: Double? = null
        var networkTxKbps: Double? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "timestamp" -> timestamp = IsoOrEpochTimestampParser.parse(reader)
                "cpu" -> cpu = if (reader.peek() == JsonReader.Token.NULL) reader.nextNull() else reader.nextDouble()
                "memory" -> memory = if (reader.peek() == JsonReader.Token.NULL) reader.nextNull() else reader.nextDouble()
                "storage" -> storage = if (reader.peek() == JsonReader.Token.NULL) reader.nextNull() else reader.nextDouble()
                "networkRxKbps" -> networkRxKbps = if (reader.peek() == JsonReader.Token.NULL) reader.nextNull() else reader.nextDouble()
                "networkTxKbps" -> networkTxKbps = if (reader.peek() == JsonReader.Token.NULL) reader.nextNull() else reader.nextDouble()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return DashboardHistoricalMetric(
            timestamp = timestamp,
            cpu = cpu,
            memory = memory,
            storage = storage,
            networkRxKbps = networkRxKbps,
            networkTxKbps = networkTxKbps
        )
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: DashboardHistoricalMetric?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("timestamp").value(value.timestamp)
        if (value.cpu != null) writer.name("cpu").value(value.cpu)
        if (value.memory != null) writer.name("memory").value(value.memory)
        if (value.storage != null) writer.name("storage").value(value.storage)
        if (value.networkRxKbps != null) writer.name("networkRxKbps").value(value.networkRxKbps)
        if (value.networkTxKbps != null) writer.name("networkTxKbps").value(value.networkTxKbps)
        writer.endObject()
    }
}

class TelemetryHistoryPointAdapter : JsonAdapter<TelemetryHistoryPoint>() {
    @FromJson
    override fun fromJson(reader: JsonReader): TelemetryHistoryPoint? {
        if (reader.peek() == JsonReader.Token.NULL) {
            return reader.nextNull()
        }
        var timestamp = 0L
        var cpu: Double? = null
        var memory: Double? = null
        var storage: Double? = null
        var networkRx: Double? = null
        var networkTx: Double? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "timestamp" -> timestamp = IsoOrEpochTimestampParser.parse(reader)
                "cpu" -> cpu = if (reader.peek() == JsonReader.Token.NULL) reader.nextNull() else reader.nextDouble()
                "memory" -> memory = if (reader.peek() == JsonReader.Token.NULL) reader.nextNull() else reader.nextDouble()
                "storage" -> storage = if (reader.peek() == JsonReader.Token.NULL) reader.nextNull() else reader.nextDouble()
                "networkRx" -> networkRx = if (reader.peek() == JsonReader.Token.NULL) reader.nextNull() else reader.nextDouble()
                "networkTx" -> networkTx = if (reader.peek() == JsonReader.Token.NULL) reader.nextNull() else reader.nextDouble()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return TelemetryHistoryPoint(
            timestamp = timestamp,
            cpu = cpu,
            memory = memory,
            storage = storage,
            networkRx = networkRx,
            networkTx = networkTx
        )
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: TelemetryHistoryPoint?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("timestamp").value(value.timestamp)
        if (value.cpu != null) writer.name("cpu").value(value.cpu)
        if (value.memory != null) writer.name("memory").value(value.memory)
        if (value.storage != null) writer.name("storage").value(value.storage)
        if (value.networkRx != null) writer.name("networkRx").value(value.networkRx)
        if (value.networkTx != null) writer.name("networkTx").value(value.networkTx)
        writer.endObject()
    }
}

object MoshiProvider {
    fun createMoshi(): Moshi = Moshi.Builder()
        .add(DashboardHistoricalMetric::class.java, DashboardHistoricalMetricAdapter())
        .add(TelemetryHistoryPoint::class.java, TelemetryHistoryPointAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()
}
