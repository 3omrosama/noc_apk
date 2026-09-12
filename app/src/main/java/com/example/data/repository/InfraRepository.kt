package com.example.data.repository

import com.example.data.api.ApiClientFactory
import com.example.data.api.NocWebSocketClient
import com.example.data.model.*
import com.example.data.security.SecureSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class InfraRepository(
    val sessionManager: SecureSessionManager,
    private val apiClientFactory: ApiClientFactory,
    val webSocketClient: NocWebSocketClient
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _overview = MutableStateFlow<Resource<InfrastructureOverview>>(Resource.Loading)
    val overview: StateFlow<Resource<InfrastructureOverview>> = _overview.asStateFlow()

    private val _dashboard = MutableStateFlow<Resource<DashboardResponse>>(Resource.Loading)
    val dashboard: StateFlow<Resource<DashboardResponse>> = _dashboard.asStateFlow()

    private val _connections = MutableStateFlow<Resource<List<InfraConnection>>>(Resource.Loading)
    val connections: StateFlow<Resource<List<InfraConnection>>> = _connections.asStateFlow()

    private val _nodes = MutableStateFlow<Resource<NodesResponse>>(Resource.Loading)
    val nodes: StateFlow<Resource<NodesResponse>> = _nodes.asStateFlow()

    private val _vms = MutableStateFlow<Resource<List<VirtualMachine>>>(Resource.Loading)
    val vms: StateFlow<Resource<List<VirtualMachine>>> = _vms.asStateFlow()

    private val _alerts = MutableStateFlow<Resource<List<Alert>>>(Resource.Loading)
    val alerts: StateFlow<Resource<List<Alert>>> = _alerts.asStateFlow()

    private val _telemetryHistory = MutableStateFlow<List<TelemetryHistoryPoint>>(emptyList())
    val telemetryHistory: StateFlow<List<TelemetryHistoryPoint>> = _telemetryHistory.asStateFlow()

    init {
        // Collect real-time WebSocket events and merge into repository state
        repositoryScope.launch {
            webSocketClient.events.collect { event ->
                handleWsEvent(event)
            }
        }
    }

    private fun handleWsEvent(event: WsEvent) {
        when (event) {
            is WsEvent.TelemetryUpdate -> {
                val current = _overview.value.dataOrNull
                if (current != null) {
                    val updated = current.copy(
                        cpu = event.cpu ?: current.cpu,
                        memory = event.memory ?: current.memory,
                        storage = event.storage ?: current.storage,
                        network = event.network ?: current.network,
                        timestamp = event.timestamp
                    )
                    _overview.value = Resource.Success(updated)
                }
            }
            is WsEvent.AlertUpdate -> {
                val currentList = _alerts.value.dataOrNull ?: emptyList()
                val updatedList = currentList.toMutableList()
                val index = updatedList.indexOfFirst { it.id == event.alert.id }
                if (index != -1) {
                    updatedList[index] = event.alert
                } else {
                    updatedList.add(0, event.alert)
                }
                _alerts.value = Resource.Success(updatedList)
            }
            is WsEvent.ConnectionStatusUpdate -> {
                val currentList = _connections.value.dataOrNull ?: emptyList()
                val updatedList = currentList.map { conn ->
                    if (conn.id == event.connectionId) {
                        conn.copy(
                            status = event.status.name.lowercase(),
                            latencyMs = event.latencyMs ?: conn.latencyMs
                        )
                    } else conn
                }
                _connections.value = Resource.Success(updatedList)
            }
            is WsEvent.VmUpdate -> {
                val currentVms = _vms.value.dataOrNull ?: emptyList()
                val updatedVms = currentVms.map { vm ->
                    if (vm.id == event.vmId) {
                        vm.copy(
                            powerState = event.powerState.name,
                            cpuUsage = event.cpuUsage ?: vm.cpuUsage,
                            memoryUsageBytes = event.memoryUsageBytes ?: vm.memoryUsageBytes,
                            lastUpdateTime = event.lastUpdateTime
                        )
                    } else vm
                }
                _vms.value = Resource.Success(updatedVms)
            }
            is WsEvent.RawMessage -> { /* No-op */ }
        }
    }

    suspend fun login(request: LoginRequest): Resource<User> {
        return try {
            val api = apiClientFactory.getApi()
            val response = api.login(request)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val token = body.effectiveToken
                if (!token.isNullOrBlank()) {
                    val user = body.user ?: User(username = request.username)
                    sessionManager.saveSession(token, body.refreshToken, user)
                    webSocketClient.start()
                    Resource.Success(user)
                } else {
                    Resource.Error(body.message ?: "Invalid credentials")
                }
            } else {
                Resource.Error(mapHttpError(response.code(), response.message()))
            }
        } catch (e: Exception) {
            Resource.Error(mapNetworkException(e), e)
        }
    }

    /**
     * Refreshes Dashboard using GET /api/dashboard.
     * Updates both _dashboard and _overview flows, and populates _telemetryHistory
     * from historicalMetrics.
     */
    suspend fun refreshOverview() {
        try {
            val api = apiClientFactory.getApi()
            val response = api.getDashboard()
            if (response.isSuccessful && response.body() != null) {
                val dash = response.body()!!
                _dashboard.value = Resource.Success(dash)
                _overview.value = Resource.Success(dash.toInfrastructureOverview())

                // Populate telemetry history from dashboard historicalMetrics
                if (dash.historicalMetrics.isNotEmpty()) {
                    _telemetryHistory.value = dash.historicalMetrics.map { it.toTelemetryHistoryPoint() }
                }
            } else {
                val err = mapHttpError(response.code(), response.message())
                _dashboard.value = Resource.Error(err)
                _overview.value = Resource.Error(err)
            }
        } catch (e: Exception) {
            val netErr = mapNetworkException(e)
            _dashboard.value = Resource.Error(netErr, e)
            _overview.value = Resource.Error(netErr, e)
        }
    }

    /**
     * Refreshes registered connections from GET /api/infrastructure.
     */
    suspend fun refreshConnections() {
        try {
            val api = apiClientFactory.getApi()
            val response = api.getInfrastructure()
            if (response.isSuccessful && response.body() != null) {
                _connections.value = Resource.Success(response.body()!!)
            } else {
                _connections.value = Resource.Error(mapHttpError(response.code(), response.message()))
            }
        } catch (e: Exception) {
            _connections.value = Resource.Error(mapNetworkException(e), e)
        }
    }

    /**
     * Refreshes infrastructure nodes by querying both GET /api/esxi/all-hosts
     * and GET /api/casaos/all-servers concurrently.
     * Also falls back to /api/infrastructure to enrich or populate nodes if hosts are empty.
     */
    suspend fun refreshNodes() {
        try {
            val api = apiClientFactory.getApi()
            coroutineScope {
                val esxiDeferred = async { runCatching { api.getEsxiHosts() }.getOrNull() }
                val casaDeferred = async { runCatching { api.getCasaOsServers() }.getOrNull() }
                val infraDeferred = async { runCatching { api.getInfrastructure() }.getOrNull() }

                val esxiRes = esxiDeferred.await()
                val casaRes = casaDeferred.await()
                val infraRes = infraDeferred.await()

                var esxiHosts = if (esxiRes?.isSuccessful == true) esxiRes.body() ?: emptyList() else emptyList()
                var casaOsHosts = if (casaRes?.isSuccessful == true) casaRes.body() ?: emptyList() else emptyList()

                // If specialized endpoints return empty, construct from /api/infrastructure connections
                val infraConns = if (infraRes?.isSuccessful == true) infraRes.body() ?: emptyList() else emptyList()
                if (infraConns.isNotEmpty()) {
                    _connections.value = Resource.Success(infraConns)

                    if (esxiHosts.isEmpty()) {
                        esxiHosts = infraConns.filter { it.connectionType == ConnectionType.ESXI }.map { conn ->
                            EsxiHost(
                                id = conn.id,
                                connectionId = conn.id,
                                connectionName = conn.name,
                                name = conn.name,
                                hostname = conn.name,
                                ip = conn.host,
                                ipAddress = conn.host,
                                status = conn.status,
                                powerState = if (conn.status.equals("ONLINE", ignoreCase = true)) "RUNNING" else "OFFLINE",
                                cpuUsagePercent = conn.cpuUsage,
                                memoryUsagePercent = conn.memoryUsage,
                                storageUsagePercent = conn.storageUsage,
                                latencyMs = conn.latencyMs
                            )
                        }
                    }

                    if (casaOsHosts.isEmpty()) {
                        casaOsHosts = infraConns.filter { it.connectionType == ConnectionType.CASAOS }.map { conn ->
                            CasaOsHost(
                                id = conn.id,
                                connectionId = conn.id,
                                connectionName = conn.name,
                                name = conn.name,
                                hostname = conn.name,
                                endpoint = conn.effectiveEndpoint,
                                ipAddress = conn.host,
                                status = conn.status,
                                cpuUsagePercent = conn.cpuUsage,
                                memoryUsagePercent = conn.memoryUsage,
                                storageUsagePercent = conn.storageUsage
                            )
                        }
                    }
                }

                _nodes.value = Resource.Success(
                    NodesResponse(
                        esxiHosts = esxiHosts,
                        casaOsHosts = casaOsHosts
                    )
                )
            }
        } catch (e: Exception) {
            _nodes.value = Resource.Error(mapNetworkException(e), e)
        }
    }

    /**
     * Refreshes virtual machines using GET /api/esxi/all-vms.
     */
    suspend fun refreshVms() {
        try {
            val api = apiClientFactory.getApi()
            val response = api.getEsxiVms()
            if (response.isSuccessful && response.body() != null) {
                _vms.value = Resource.Success(response.body()!!)
            } else {
                _vms.value = Resource.Error(mapHttpError(response.code(), response.message()))
            }
        } catch (e: Exception) {
            _vms.value = Resource.Error(mapNetworkException(e), e)
        }
    }

    /**
     * Executes VM action using POST /api/esxi/{connectionId}/vms/{vmId}/action.
     */
    suspend fun executeVmAction(
        vmId: String,
        action: VmActionType,
        connectionId: String? = null,
        reason: String? = null
    ): Resource<VmActionResult> {
        val currentUser = sessionManager.getUser()
        if (currentUser?.userRole?.canPerformVmActions != true) {
            return Resource.Error("Permission denied: Viewer role cannot execute VM lifecycle actions")
        }

        // Determine target connectionId
        val targetConnectionId = connectionId
            ?: _vms.value.dataOrNull?.find { it.id == vmId }?.connectionId
            ?: _connections.value.dataOrNull?.firstOrNull { it.connectionType == ConnectionType.ESXI }?.id
            ?: return Resource.Error("Cannot determine host connection for VM $vmId")

        return try {
            val api = apiClientFactory.getApi()
            val response = api.executeVmAction(
                connectionId = targetConnectionId,
                vmId = vmId,
                request = VmActionRequest(action = action.actionKey, reason = reason)
            )
            if (response.isSuccessful && response.body() != null) {
                refreshVms()
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(mapHttpError(response.code(), response.message()))
            }
        } catch (e: Exception) {
            Resource.Error(mapNetworkException(e), e)
        }
    }

    suspend fun refreshAlerts(severity: String? = null, status: String? = null) {
        try {
            val api = apiClientFactory.getApi()
            val response = api.getAlerts(status = status)
            if (response.isSuccessful && response.body() != null) {
                val alerts = response.body()!!
                val filtered = if (severity != null) {
                    alerts.filter { it.severity.equals(severity, ignoreCase = true) }
                } else alerts
                _alerts.value = Resource.Success(filtered)
            } else {
                _alerts.value = Resource.Error(mapHttpError(response.code(), response.message()))
            }
        } catch (e: Exception) {
            _alerts.value = Resource.Error(mapNetworkException(e), e)
        }
    }

    suspend fun acknowledgeAlert(alertId: String): Resource<Unit> {
        val currentUser = sessionManager.getUser()
        if (currentUser?.userRole?.canManageAlerts != true) {
            return Resource.Error("Permission denied: Viewer role cannot acknowledge alerts")
        }
        return try {
            val api = apiClientFactory.getApi()
            val response = api.acknowledgeAlert(alertId)
            if (response.isSuccessful) {
                refreshAlerts()
                Resource.Success(Unit)
            } else {
                Resource.Error(mapHttpError(response.code(), response.message()))
            }
        } catch (e: Exception) {
            Resource.Error(mapNetworkException(e), e)
        }
    }

    suspend fun resolveAlert(alertId: String): Resource<Unit> {
        val currentUser = sessionManager.getUser()
        if (currentUser?.userRole?.canManageAlerts != true) {
            return Resource.Error("Permission denied: Viewer role cannot resolve alerts")
        }
        return try {
            val api = apiClientFactory.getApi()
            val response = api.resolveAlert(alertId)
            if (response.isSuccessful) {
                refreshAlerts()
                Resource.Success(Unit)
            } else {
                Resource.Error(mapHttpError(response.code(), response.message()))
            }
        } catch (e: Exception) {
            Resource.Error(mapNetworkException(e), e)
        }
    }

    /**
     * Telemetry history queries GET /api/monitoring/metrics?range=...
     */
    suspend fun fetchTelemetryHistory(
        range: TelemetryTimeRange,
        target: String? = null
    ): Resource<TelemetryHistoryResponse> {
        return try {
            val api = apiClientFactory.getApi()
            val response = api.getMonitoringMetrics(range = range.queryValue)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val hist = TelemetryHistoryResponse(
                    range = body.range ?: range.queryValue,
                    points = body.data
                )
                if (body.data.isNotEmpty()) {
                    _telemetryHistory.value = body.data
                }
                Resource.Success(hist)
            } else {
                // Fallback to dashboard historicalMetrics if available in memory
                val cached = _telemetryHistory.value
                if (cached.isNotEmpty()) {
                    Resource.Success(TelemetryHistoryResponse(range = range.queryValue, points = cached))
                } else {
                    Resource.Error(mapHttpError(response.code(), response.message()))
                }
            }
        } catch (e: Exception) {
            val cached = _telemetryHistory.value
            if (cached.isNotEmpty()) {
                Resource.Success(TelemetryHistoryResponse(range = range.queryValue, points = cached))
            } else {
                Resource.Error(mapNetworkException(e), e)
            }
        }
    }

    fun clearCache() {
        webSocketClient.stop()
        apiClientFactory.resetClient()
        _overview.value = Resource.Loading
        _dashboard.value = Resource.Loading
        _connections.value = Resource.Loading
        _nodes.value = Resource.Loading
        _vms.value = Resource.Loading
        _alerts.value = Resource.Loading
        _telemetryHistory.value = emptyList()
    }

    companion object {
        fun mapHttpError(code: Int, defaultMessage: String?): String {
            return when (code) {
                400 -> "Malformed API response"
                401 -> "Session expired"
                403 -> "Permission denied"
                404 -> "Unsupported endpoint"
                408 -> "Timeout"
                500, 502, 503, 504 -> "Server error"
                else -> defaultMessage ?: "Error ($code)"
            }
        }

        fun mapNetworkException(e: Exception): String {
            return when (e) {
                is SSLException -> "TLS error"
                is SocketTimeoutException -> "Timeout"
                is UnknownHostException -> "Network unavailable"
                is IOException -> {
                    val msg = e.message?.lowercase() ?: ""
                    if (msg.contains("ssl") || msg.contains("cert")) {
                        "TLS error"
                    } else if (msg.contains("failed to connect") || msg.contains("connection refused")) {
                        "Cannot reach NOC"
                    } else {
                        "Network unavailable"
                    }
                }
                else -> "Server error"
            }
        }
    }
}
