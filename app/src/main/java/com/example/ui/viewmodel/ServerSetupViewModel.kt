package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.ApiClientFactory
import com.example.data.api.ConnectionTestStatus
import com.example.data.security.SecureSessionManager
import com.example.data.security.UrlNormalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ServerSetupUiState(
    val rawUrl: String = "",
    val normalizedUrl: String = "",
    val isTesting: Boolean = false,
    val testStatus: ConnectionTestStatus? = null,
    val canProceed: Boolean = false,
    val errorMessage: String? = null
)

class ServerSetupViewModel(
    private val sessionManager: SecureSessionManager,
    private val apiClientFactory: ApiClientFactory
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ServerSetupUiState(
            rawUrl = sessionManager.getServerUrl() ?: ""
        )
    )
    val uiState: StateFlow<ServerSetupUiState> = _uiState.asStateFlow()

    fun onUrlChanged(newUrl: String) {
        val normalized = UrlNormalizer.normalize(newUrl)
        _uiState.value = _uiState.value.copy(
            rawUrl = newUrl,
            normalizedUrl = normalized,
            testStatus = null,
            canProceed = false,
            errorMessage = null
        )
    }

    fun selectPresetUrl(preset: String) {
        onUrlChanged(preset)
    }

    fun testConnection() {
        val url = _uiState.value.rawUrl.trim()
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please enter a NOC Server URL"
            )
            return
        }

        if (!UrlNormalizer.isValidUrl(url)) {
            _uiState.value = _uiState.value.copy(
                testStatus = ConnectionTestStatus.INVALID_URL,
                canProceed = false,
                errorMessage = "Invalid URL format. Please check scheme or hostname."
            )
            return
        }

        val normalized = UrlNormalizer.normalize(url)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isTesting = true,
                testStatus = ConnectionTestStatus.CONNECTING,
                errorMessage = null
            )

            val status = apiClientFactory.testConnection(normalized)

            _uiState.value = _uiState.value.copy(
                isTesting = false,
                normalizedUrl = normalized,
                testStatus = status,
                canProceed = status.isSuccess,
                errorMessage = if (!status.isSuccess) status.displayName else null
            )
        }
    }

    fun saveServerAndProceed(onSuccess: () -> Unit) {
        val normalized = _uiState.value.normalizedUrl.ifEmpty {
            UrlNormalizer.normalize(_uiState.value.rawUrl)
        }
        if (normalized.isNotBlank()) {
            sessionManager.saveServerUrl(normalized)
            onSuccess()
        }
    }
}
