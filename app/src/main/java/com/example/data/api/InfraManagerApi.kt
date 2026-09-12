package com.example.data.api

import com.example.data.model.Alert
import com.example.data.model.AlertRule
import com.example.data.model.CasaOsApp
import com.example.data.model.CasaOsHost
import com.example.data.model.DashboardResponse
import com.example.data.model.DockerContainer
import com.example.data.model.EsxiHost
import com.example.data.model.HealthResponse
import com.example.data.model.InfraConnection
import com.example.data.model.LoginRequest
import com.example.data.model.LoginResponse
import com.example.data.model.MonitoringMetricsResponse
import com.example.data.model.User
import com.example.data.model.VirtualMachine
import com.example.data.model.VmActionRequest
import com.example.data.model.VmActionResult
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface InfraManagerApi {

    // Health
    @GET("api/health")
    suspend fun getHealth(): Response<HealthResponse>

    // Authentication
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/auth/me")
    suspend fun getCurrentUser(): Response<User>

    // Dashboard Overview
    @GET("api/dashboard")
    suspend fun getDashboard(): Response<DashboardResponse>

    // Infrastructure Inventory (Registered node connections)
    @GET("api/infrastructure")
    suspend fun getInfrastructure(): Response<List<InfraConnection>>

    // ESXi Hypervisors & Hosts
    @GET("api/esxi/all-hosts")
    suspend fun getEsxiHosts(): Response<List<EsxiHost>>

    // Virtual Machines
    @GET("api/esxi/all-vms")
    suspend fun getEsxiVms(): Response<List<VirtualMachine>>

    // VM Actions on ESXi (Requires connectionId and vmId)
    @POST("api/esxi/{connectionId}/vms/{vmId}/action")
    suspend fun executeVmAction(
        @Path("connectionId") connectionId: String,
        @Path("vmId") vmId: String,
        @Body request: VmActionRequest
    ): Response<VmActionResult>

    // CasaOS Edge Servers
    @GET("api/casaos/all-servers")
    suspend fun getCasaOsServers(): Response<List<CasaOsHost>>

    // CasaOS Applications
    @GET("api/casaos/all-apps")
    suspend fun getCasaOsApps(): Response<List<CasaOsApp>>

    // Docker Containers
    @GET("api/docker/all-containers")
    suspend fun getDockerContainers(): Response<List<DockerContainer>>

    // Incident Center / Alerts
    @GET("api/alerts")
    suspend fun getAlerts(@Query("status") status: String? = null): Response<List<Alert>>

    @POST("api/alerts/{id}/acknowledge")
    suspend fun acknowledgeAlert(@Path("id") alertId: String): Response<Unit>

    @POST("api/alerts/{id}/resolve")
    suspend fun resolveAlert(@Path("id") alertId: String): Response<Unit>

    @GET("api/alerts/rules")
    suspend fun getAlertRules(): Response<List<AlertRule>>

    // Monitoring Metrics
    @GET("api/monitoring/metrics")
    suspend fun getMonitoringMetrics(@Query("range") range: String = "24h"): Response<MonitoringMetricsResponse>
}
