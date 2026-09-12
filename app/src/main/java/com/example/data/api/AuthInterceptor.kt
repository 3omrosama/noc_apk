package com.example.data.api

import com.example.data.security.SecureSessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val sessionManager: SecureSessionManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        // Skip auth header for login and health check endpoints
        val isAuthExempt = path.endsWith("api/auth/login") ||
                path.endsWith("api/health") ||
                path.endsWith("health")

        val token = sessionManager.getToken()
        val request = if (!isAuthExempt && !token.isNullOrBlank()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .header("User-Agent", "InfraManager-Mobile/1.0 (Android NOC)")
                .build()
        } else {
            originalRequest.newBuilder()
                .header("Accept", "application/json")
                .header("User-Agent", "InfraManager-Mobile/1.0 (Android NOC)")
                .build()
        }

        val response = chain.proceed(request)

        // If the session became invalid or expired on an authenticated route
        if (response.code == 401 && !isAuthExempt) {
            sessionManager.onSessionExpired()
        }

        return response
    }
}
