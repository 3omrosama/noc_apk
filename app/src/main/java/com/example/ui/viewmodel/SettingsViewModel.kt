package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.data.model.User
import com.example.data.repository.InfraRepository
import com.example.data.security.SecureSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val serverUrl: String = "",
    val currentUser: User? = null,
    val isDemoMode: Boolean = false,
    val appVersion: String = "1.0.0 (Build 1)"
)

class SettingsViewModel(
    private val sessionManager: SecureSessionManager,
    private val repository: InfraRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            serverUrl = sessionManager.getServerUrl() ?: "Not configured",
            currentUser = sessionManager.getUser()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun logout(onComplete: () -> Unit) {
        repository.clearCache()
        sessionManager.clearSession()
        onComplete()
    }

    fun changeServer(onComplete: () -> Unit) {
        repository.clearCache()
        sessionManager.clearAll()
        onComplete()
    }
}
