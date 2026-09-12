package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.VirtualMachine
import com.example.data.model.VmActionType
import com.example.data.model.VmPowerState
import com.example.data.repository.InfraRepository
import com.example.data.repository.Resource
import com.example.data.security.SecureSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VmActionDialogState(
    val vm: VirtualMachine,
    val action: VmActionType,
    val isExecuting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

data class VmsUiState(
    val vms: Resource<List<VirtualMachine>> = Resource.Loading,
    val selectedPowerFilter: VmPowerState? = null,
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
    val activeActionDialog: VmActionDialogState? = null,
    val snackbarMessage: String? = null,
    val canExecuteActions: Boolean = false
)

class VmsViewModel(
    private val repository: InfraRepository,
    private val sessionManager: SecureSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        VmsUiState(
            canExecuteActions = sessionManager.getUser()?.userRole?.canPerformVmActions == true
        )
    )
    val uiState: StateFlow<VmsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.vms.collect { vmsResource ->
                _uiState.value = _uiState.value.copy(vms = vmsResource)
            }
        }
        refresh()
    }

    fun refresh() {
        val userCanAct = sessionManager.getUser()?.userRole?.canPerformVmActions == true
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRefreshing = true,
                canExecuteActions = userCanAct
            )
            repository.refreshVms()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    fun setPowerFilter(filter: VmPowerState?) {
        _uiState.value = _uiState.value.copy(selectedPowerFilter = filter)
    }

    fun onSearchQueryChanged(q: String) {
        _uiState.value = _uiState.value.copy(searchQuery = q)
    }

    fun requestVmAction(vm: VirtualMachine, action: VmActionType) {
        if (!_uiState.value.canExecuteActions) {
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "Permission denied: Viewer role cannot perform VM actions"
            )
            return
        }

        // For all actions, show confirmation dialog to prevent accidental triggers
        _uiState.value = _uiState.value.copy(
            activeActionDialog = VmActionDialogState(vm = vm, action = action)
        )
    }

    fun confirmVmAction() {
        val dialog = _uiState.value.activeActionDialog ?: return
        if (dialog.isExecuting) return // Prevent duplicate submission

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                activeActionDialog = dialog.copy(isExecuting = true, error = null)
            )

            val result = repository.executeVmAction(dialog.vm.id, dialog.action)
            when (result) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        activeActionDialog = null,
                        snackbarMessage = "${dialog.action.displayName} executed on ${dialog.vm.name}: ${result.data.message}"
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        activeActionDialog = dialog.copy(
                            isExecuting = false,
                            error = result.message
                        )
                    )
                }
                is Resource.Loading -> { /* Handled */ }
            }
        }
    }

    fun dismissActionDialog() {
        val dialog = _uiState.value.activeActionDialog
        if (dialog != null && !dialog.isExecuting) {
            _uiState.value = _uiState.value.copy(activeActionDialog = null)
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
