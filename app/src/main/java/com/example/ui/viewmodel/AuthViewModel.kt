package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.LoginRequest
import com.example.data.model.User
import com.example.data.repository.InfraRepository
import com.example.data.repository.Resource
import com.example.data.security.SecureSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val sessionExpiredMessage: String? = null
)

class AuthViewModel(
    private val repository: InfraRepository,
    private val sessionManager: SecureSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            serverUrl = sessionManager.getServerUrl() ?: ""
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.sessionExpiredEvent.collect {
                _uiState.value = _uiState.value.copy(
                    sessionExpiredMessage = "Session expired. Please log in again.",
                    isLoading = false
                )
            }
        }
    }

    fun onUsernameChanged(u: String) {
        _uiState.value = _uiState.value.copy(username = u, error = null)
    }

    fun onPasswordChanged(p: String) {
        _uiState.value = _uiState.value.copy(password = p, error = null)
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isPasswordVisible = !_uiState.value.isPasswordVisible
        )
    }

    fun clearSessionExpiredNotice() {
        _uiState.value = _uiState.value.copy(sessionExpiredMessage = null)
    }

    fun login(onSuccess: (User) -> Unit) {
        val username = _uiState.value.username.trim()
        val password = _uiState.value.password

        if (username.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Please enter both username and password"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = repository.login(LoginRequest(username = username, password = password))
            when (result) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        error = null
                    )
                    onSuccess(result.data)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is Resource.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }
}
