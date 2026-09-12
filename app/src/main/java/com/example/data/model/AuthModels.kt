package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class UserRole {
    @Json(name = "admin") ADMIN,
    @Json(name = "operator") OPERATOR,
    @Json(name = "viewer") VIEWER;

    val canPerformVmActions: Boolean
        get() = this == ADMIN || this == OPERATOR

    val canManageAlerts: Boolean
        get() = this == ADMIN || this == OPERATOR

    companion object {
        fun fromString(role: String?): UserRole {
            return when (role?.lowercase()) {
                "admin", "administrator" -> ADMIN
                "operator" -> OPERATOR
                else -> VIEWER
            }
        }
    }
}

@JsonClass(generateAdapter = true)
data class User(
    @Json(name = "id") val id: String? = null,
    @Json(name = "username") val username: String,
    @Json(name = "role") val role: String? = "viewer",
    @Json(name = "name") val name: String? = null,
    @Json(name = "email") val email: String? = null
) {
    val userRole: UserRole
        get() = UserRole.fromString(role)
}

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "token") val token: String? = null,
    @Json(name = "accessToken") val accessToken: String? = null,
    @Json(name = "refreshToken") val refreshToken: String? = null,
    @Json(name = "user") val user: User? = null,
    @Json(name = "demoMode") val demoMode: Boolean = false,
    @Json(name = "message") val message: String? = null
) {
    val effectiveToken: String?
        get() = token ?: accessToken
}

@JsonClass(generateAdapter = true)
data class HealthResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "version") val version: String? = null,
    @Json(name = "uptime") val uptime: Long? = null,
    @Json(name = "demoMode") val demoMode: Boolean = false,
    @Json(name = "serverTime") val serverTime: Long? = null,
    @Json(name = "service") val service: String? = null
) {
    val isHealthy: Boolean
        get() = status.equals("ok", ignoreCase = true) || 
                status.equals("healthy", ignoreCase = true) || 
                status.equals("running", ignoreCase = true) ||
                service != null
}
