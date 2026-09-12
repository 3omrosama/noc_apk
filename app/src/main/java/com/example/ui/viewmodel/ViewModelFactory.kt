package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.di.AppContainer

class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ServerSetupViewModel::class.java) -> {
                ServerSetupViewModel(container.sessionManager, container.apiClientFactory) as T
            }
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(container.repository, container.sessionManager) as T
            }
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(container.repository) as T
            }
            modelClass.isAssignableFrom(NodesViewModel::class.java) -> {
                NodesViewModel(container.repository) as T
            }
            modelClass.isAssignableFrom(VmsViewModel::class.java) -> {
                VmsViewModel(container.repository, container.sessionManager) as T
            }
            modelClass.isAssignableFrom(AlertsViewModel::class.java) -> {
                AlertsViewModel(container.repository, container.sessionManager) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(container.sessionManager, container.repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
