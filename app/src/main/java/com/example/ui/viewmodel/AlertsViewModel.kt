package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Alert
import com.example.data.model.AlertSeverity
import com.example.data.model.AlertStatus
import com.example.data.repository.InfraRepository
import com.example.data.repository.Resource
import com.example.data.security.SecureSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AlertsUiState(
    val alerts: Resource<List<Alert>> = Resource.Loading,
    val selectedSeverity: AlertSeverity? = null,
    val selectedStatus: AlertStatus? = null,
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
    val operatingAlertId: String? = null,
    val snackbarMessage: String? = null,
    val canManageAlerts: Boolean = false
)

class AlertsViewModel(
    private val repository: InfraRepository,
    private val sessionManager: SecureSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AlertsUiState(
            canManageAlerts = sessionManager.getUser()?.userRole?.canManageAlerts == true
        )
    )
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.alerts.collect { alertsResource ->
                _uiState.value = _uiState.value.copy(alerts = alertsResource)
            }
        }
        refresh()
    }

    fun refresh() {
        val userCanAct = sessionManager.getUser()?.userRole?.canManageAlerts == true
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRefreshing = true,
                canManageAlerts = userCanAct
            )
            repository.refreshAlerts(
                severity = _uiState.value.selectedSeverity?.name?.lowercase(),
                status = _uiState.value.selectedStatus?.name?.lowercase()
            )
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    fun setSeverityFilter(severity: AlertSeverity?) {
        _uiState.value = _uiState.value.copy(selectedSeverity = severity)
        refresh()
    }

    fun setStatusFilter(status: AlertStatus?) {
        _uiState.value = _uiState.value.copy(selectedStatus = status)
        refresh()
    }

    fun onSearchQueryChanged(q: String) {
        _uiState.value = _uiState.value.copy(searchQuery = q)
    }

    fun acknowledgeAlert(alert: Alert) {
        if (!_uiState.value.canManageAlerts) {
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "Permission denied: Viewer role cannot acknowledge alerts"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(operatingAlertId = alert.id)
            val result = repository.acknowledgeAlert(alert.id)
            _uiState.value = when (result) {
                is Resource.Success -> _uiState.value.copy(
                    operatingAlertId = null,
                    snackbarMessage = "Alert acknowledged: ${alert.title}"
                )
                is Resource.Error -> _uiState.value.copy(
                    operatingAlertId = null,
                    snackbarMessage = "Failed to acknowledge: ${result.message}"
                )
                is Resource.Loading -> _uiState.value
            }
        }
    }

    fun resolveAlert(alert: Alert) {
        if (!_uiState.value.canManageAlerts) {
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "Permission denied: Viewer role cannot resolve alerts"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(operatingAlertId = alert.id)
            val result = repository.resolveAlert(alert.id)
            _uiState.value = when (result) {
                is Resource.Success -> _uiState.value.copy(
                    operatingAlertId = null,
                    snackbarMessage = "Alert marked resolved: ${alert.title}"
                )
                is Resource.Error -> _uiState.value.copy(
                    operatingAlertId = null,
                    snackbarMessage = "Failed to resolve: ${result.message}"
                )
                is Resource.Loading -> _uiState.value
            }
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
