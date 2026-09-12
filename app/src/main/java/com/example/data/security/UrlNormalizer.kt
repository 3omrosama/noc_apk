package com.example.data.security

import java.net.URI

object UrlNormalizer {

    /**
     * Normalizes a user-entered NOC server URL.
     *
     * Examples:
     * - "https://noc.example.com" -> "https://noc.example.com"
     * - "https://noc.it.nds-technology.com/" -> "https://noc.it.nds-technology.com"
     * - "http://172.16.0.12:3000" -> "http://172.16.0.12:3000"
     * - "192.168.1.50:3000" -> "http://192.168.1.50:3000"
     * - "noc.example.com" -> "https://noc.example.com"
     * - "10.0.0.1" -> "http://10.0.0.1"
     */
    fun normalize(input: String): String {
        var trimmed = input.trim()
        if (trimmed.isEmpty()) return ""

        // Remove trailing slashes
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length - 1)
        }

        val hasScheme = trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)

        val result = if (hasScheme) {
            trimmed
        } else {
            // Determine scheme based on IP address or port
            val isIpAddress = isIpv4(trimmed)
            val hasCustomPort = trimmed.contains(":") && !trimmed.endsWith(":443")
            if (isIpAddress || hasCustomPort) {
                "http://$trimmed"
            } else {
                "https://$trimmed"
            }
        }

        return result
    }

    fun isValidUrl(url: String): Boolean {
        if (url.isBlank()) return false
        return try {
            val normalized = normalize(url)
            val uri = URI.create(normalized)
            val scheme = uri.scheme
            val host = uri.host
            (scheme == "http" || scheme == "https") && !host.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }

    fun toRetrofitBaseUrl(url: String): String {
        val normalized = normalize(url)
        return if (normalized.endsWith("/")) normalized else "$normalized/"
    }

    fun toWebSocketUrl(url: String, path: String = "ws"): String {
        val normalized = normalize(url)
        val wsScheme = if (normalized.startsWith("https://", ignoreCase = true)) "wss://" else "ws://"
        val hostAndPort = normalized.removePrefix("https://").removePrefix("http://")
        val cleanPath = path.removePrefix("/")
        return "$wsScheme$hostAndPort/$cleanPath"
    }

    private fun isIpv4(input: String): Boolean {
        val hostPart = if (input.contains(":")) input.substringBefore(":") else input
        val parts = hostPart.split(".")
        if (parts.size != 4) return false
        return parts.all { part ->
            part.toIntOrNull()?.let { it in 0..255 } == true
        }
    }
}
