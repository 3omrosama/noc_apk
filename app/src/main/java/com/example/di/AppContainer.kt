package com.example.di

import android.content.Context
import com.example.data.api.ApiClientFactory
import com.example.data.api.NocWebSocketClient
import com.example.data.repository.InfraRepository
import com.example.data.security.SecureSessionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class AppContainer(context: Context) {

    val sessionManager: SecureSessionManager = SecureSessionManager(context.applicationContext)

    val apiClientFactory: ApiClientFactory = ApiClientFactory(sessionManager)

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val webSocketClient: NocWebSocketClient = NocWebSocketClient(sessionManager, moshi)

    val repository: InfraRepository = InfraRepository(
        sessionManager = sessionManager,
        apiClientFactory = apiClientFactory,
        webSocketClient = webSocketClient
    )

    companion object {
        @Volatile
        private var instance: AppContainer? = null

        fun getInstance(context: Context): AppContainer {
            return instance ?: synchronized(this) {
                instance ?: AppContainer(context).also { instance = it }
            }
        }
    }
}
