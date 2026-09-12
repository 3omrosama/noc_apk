package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ConnectionStatus
import com.example.data.model.NodesResponse
import com.example.data.repository.InfraRepository
import com.example.data.repository.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NodesUiState(
    val nodes: Resource<NodesResponse> = Resource.Loading,
    val selectedFilter: ConnectionStatus? = null,
    val searchQuery: String = "",
    val isRefreshing: Boolean = false
)

class NodesViewModel(
    private val repository: InfraRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NodesUiState())
    val uiState: StateFlow<NodesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.nodes.collect { nodesResource ->
                _uiState.value = _uiState.value.copy(nodes = nodesResource)
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            repository.refreshNodes()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    fun setFilter(status: ConnectionStatus?) {
        _uiState.value = _uiState.value.copy(selectedFilter = status)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }
}
