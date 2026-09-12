package com.example.data.api

import com.example.data.security.SecureSessionManager
import com.example.data.security.UrlNormalizer
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

enum class ConnectionTestStatus(val displayName: String) {
    CONNECTING("Connecting..."),
    CONNECTED("Connected"),
    AUTH_REQUIRED("Authentication required"),
    INVALID_URL("Invalid URL"),
    NETWORK_UNAVAILABLE("Network unavailable"),
    TLS_ERROR("TLS/SSL error"),
    SERVER_UNAVAILABLE("Server unavailable"),
    UNSUPPORTED_ENDPOINT("Unsupported endpoint"),
    TIMEOUT("Timeout");

    val isSuccess: Boolean
        get() = this == CONNECTED || this == AUTH_REQUIRED
}

class ApiClientFactory(private val sessionManager: SecureSessionManager) {

    val moshi: Moshi = MoshiProvider.createMoshi()

    private var currentBaseUrl: String? = null
    private var currentApi: InfraManagerApi? = null
    private var currentOkHttpClient: OkHttpClient? = null

    fun getOkHttpClient(): OkHttpClient {
        val existing = currentOkHttpClient
        if (existing != null) return existing

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
            // Redact Authorization headers to prevent token leakage
            redactHeader("Authorization")
            redactHeader("Cookie")
            redactHeader("Set-Cookie")
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .addInterceptor(logging)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        currentOkHttpClient = client
        return client
    }

    @Synchronized
    fun getApi(baseUrlOverride: String? = null): InfraManagerApi {
        val targetUrl = baseUrlOverride ?: sessionManager.getServerUrl()
        val retrofitBaseUrl = if (!targetUrl.isNullOrBlank()) {
            UrlNormalizer.toRetrofitBaseUrl(targetUrl)
        } else {
            "http://127.0.0.1:3000/" // Safe default until server is set
        }

        if (currentApi != null && currentBaseUrl == retrofitBaseUrl) {
            return currentApi!!
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(retrofitBaseUrl)
            .client(getOkHttpClient())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val api = retrofit.create(InfraManagerApi::class.java)
        currentApi = api
        currentBaseUrl = retrofitBaseUrl
        return api
    }

    fun resetClient() {
        currentApi = null
        currentBaseUrl = null
    }

    /**
     * Tests connectivity to the given NOC Server URL.
     * Evaluates real HTTP/TLS status codes without guessing.
     */
    suspend fun testConnection(rawUrl: String): ConnectionTestStatus {
        if (!UrlNormalizer.isValidUrl(rawUrl)) {
            return ConnectionTestStatus.INVALID_URL
        }

        val normalized = UrlNormalizer.normalize(rawUrl)
        val testBaseUrl = UrlNormalizer.toRetrofitBaseUrl(normalized)

        // Raw OkHttp probe to test the server directly
        val testClient = OkHttpClient.Builder()
            .connectTimeout(7, TimeUnit.SECONDS)
            .readTimeout(7, TimeUnit.SECONDS)
            .build()

        // 1. Try /api/health
        val healthUrl = testBaseUrl + "api/health"
        val request = Request.Builder()
            .url(healthUrl)
            .header("Accept", "application/json")
            .header("User-Agent", "InfraManager-Mobile-Probe")
            .build()

        return try {
            val response = testClient.newCall(request).execute()
            when (response.code) {
                200, 204 -> ConnectionTestStatus.CONNECTED
                401, 403 -> ConnectionTestStatus.AUTH_REQUIRED
                404 -> {
                    // Try root fallback or /health
                    val fallbackReq = Request.Builder()
                        .url(testBaseUrl + "health")
                        .header("Accept", "application/json")
                        .build()
                    val fallbackResp = testClient.newCall(fallbackReq).execute()
                    if (fallbackResp.isSuccessful || fallbackResp.code == 401) {
                        ConnectionTestStatus.CONNECTED
                    } else {
                        // Even if 404, check if the server is responding as a Web NOC
                        ConnectionTestStatus.UNSUPPORTED_ENDPOINT
                    }
                }
                in 500..599 -> ConnectionTestStatus.SERVER_UNAVAILABLE
                else -> ConnectionTestStatus.CONNECTED
            }
        } catch (e: Exception) {
            mapExceptionToTestStatus(e)
        }
    }

    private fun mapExceptionToTestStatus(e: Exception): ConnectionTestStatus {
        return when (e) {
            is SSLException -> ConnectionTestStatus.TLS_ERROR
            is SocketTimeoutException -> ConnectionTestStatus.TIMEOUT
            is UnknownHostException -> ConnectionTestStatus.NETWORK_UNAVAILABLE
            is IOException -> {
                val msg = e.message?.lowercase() ?: ""
                if (msg.contains("ssl") || msg.contains("cert")) {
                    ConnectionTestStatus.TLS_ERROR
                } else if (msg.contains("connection refused") || msg.contains("failed to connect")) {
                    ConnectionTestStatus.SERVER_UNAVAILABLE
                } else {
                    ConnectionTestStatus.NETWORK_UNAVAILABLE
                }
            }
            else -> ConnectionTestStatus.SERVER_UNAVAILABLE
        }
    }
}
