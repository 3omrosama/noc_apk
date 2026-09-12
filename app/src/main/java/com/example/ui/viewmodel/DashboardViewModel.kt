package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.InfraRepository
import com.example.data.repository.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val overview: Resource<InfrastructureOverview> = Resource.Loading,
    val selectedTimeRange: TelemetryTimeRange = TelemetryTimeRange.ONE_HOUR,
    val historyResponse: Resource<TelemetryHistoryResponse> = Resource.Loading,
    val wsStatus: WsConnectionStatus = WsConnectionStatus.OFFLINE,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val serverUrl: String = ""
)

class DashboardViewModel(
    private val repository: InfraRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            serverUrl = repository.sessionManager.getServerUrl() ?: ""
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        // Collect repository overview flow
        viewModelScope.launch {
            repository.overview.collect { overviewResource ->
                _uiState.value = _uiState.value.copy(overview = overviewResource)
            }
        }

        // Collect WebSocket connection status
        viewModelScope.launch {
            repository.webSocketClient.connectionStatus.collect { status ->
                _uiState.value = _uiState.value.copy(wsStatus = status)
            }
        }

        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            repository.refreshOverview()
            loadHistory(_uiState.value.selectedTimeRange)
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    fun selectTimeRange(range: TelemetryTimeRange) {
        if (_uiState.value.selectedTimeRange == range) return
        _uiState.value = _uiState.value.copy(selectedTimeRange = range)
        loadHistory(range)
    }

    private fun loadHistory(range: TelemetryTimeRange) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(historyResponse = Resource.Loading)
            val result = repository.fetchTelemetryHistory(range)
            _uiState.value = _uiState.value.copy(historyResponse = result)
        }
    }

    fun onSearchQueryChanged(q: String) {
        _uiState.value = _uiState.value.copy(searchQuery = q)
    }
}
