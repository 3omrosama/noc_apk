package com.example

import com.example.data.api.InfraManagerApi
import com.example.data.api.MoshiProvider
import com.example.data.model.*
import com.squareup.moshi.JsonDataException
import org.junit.Assert.*
import org.junit.Test
import retrofit2.http.GET
import retrofit2.http.POST

class ApiCompatibilityTest {

    private val moshi = MoshiProvider.createMoshi()

    @Test
    fun `verify no obsolete endpoints exist on InfraManagerApi`() {
        val methods = InfraManagerApi::class.java.declaredMethods
        val obsoleteEndpoints = listOf(
            "api/overview",
            "api/connections",
            "api/nodes",
            "api/vms",
            "api/telemetry/history"
        )

        for (method in methods) {
            val getAnnotation = method.getAnnotation(GET::class.java)
            if (getAnnotation != null) {
                assertFalse(
                    "Method ${method.name} must not use obsolete endpoint ${getAnnotation.value}",
                    obsoleteEndpoints.contains(getAnnotation.value)
                )
            }
            val postAnnotation = method.getAnnotation(POST::class.java)
            if (postAnnotation != null) {
                assertFalse(
                    "Method ${method.name} must not use obsolete endpoint ${postAnnotation.value}",
                    postAnnotation.value == "api/vms/{id}/action"
                )
            }
        }
    }

    @Test
    fun `verify required backend endpoints exist on InfraManagerApi`() {
        val methods = InfraManagerApi::class.java.declaredMethods
        val getPaths = methods.mapNotNull { it.getAnnotation(GET::class.java)?.value }
        val postPaths = methods.mapNotNull { it.getAnnotation(POST::class.java)?.value }

        assertTrue("api/dashboard must exist", getPaths.contains("api/dashboard"))
        assertTrue("api/infrastructure must exist", getPaths.contains("api/infrastructure"))
        assertTrue("api/esxi/all-hosts must exist", getPaths.contains("api/esxi/all-hosts"))
        assertTrue("api/esxi/all-vms must exist", getPaths.contains("api/esxi/all-vms"))
        assertTrue("api/casaos/all-servers must exist", getPaths.contains("api/casaos/all-servers"))
        assertTrue("api/casaos/all-apps must exist", getPaths.contains("api/casaos/all-apps"))
        assertTrue("api/docker/all-containers must exist", getPaths.contains("api/docker/all-containers"))
        assertTrue(
            "api/esxi/{connectionId}/vms/{vmId}/action must exist",
            postPaths.contains("api/esxi/{connectionId}/vms/{vmId}/action")
        )
    }

    @Test
    fun `parse real backend dashboard response accurately`() {
        val json = """
        {
          "healthScore": 95,
          "nodes": {
            "total": 2,
            "online": 2,
            "offline": 0,
            "degraded": 0
          },
          "vms": {
            "total": 5,
            "running": 4,
            "stopped": 1,
            "suspended": 0
          },
          "containers": {
            "total": 12,
            "running": 11,
            "stopped": 1
          },
          "metrics": {
            "cpuUtilizationPct": 28.4,
            "cpuCoresTotal": 16,
            "memoryUtilizationPct": 54.2,
            "memoryBytesUsed": 34896609280,
            "memoryBytesTotal": 64424509440,
            "storageUtilizationPct": 41.8,
            "storageBytesUsed": 894567890123,
            "storageBytesTotal": 2147483648000,
            "networkTrafficRxKbps": 1240.5,
            "networkTrafficTxKbps": 890.2
          },
          "historicalMetrics": [
            {
              "timestamp": 1788772800000,
              "cpu": 25.1,
              "memory": 52.0,
              "storage": 41.8,
              "networkRxKbps": 1100.0,
              "networkTxKbps": 800.0
            }
          ],
          "activeAlerts": [],
          "recentEvents": [],
          "recentAuditLogs": [],
          "hasLiveInfrastructure": true,
          "isDemoMode": false
        }
        """.trimIndent()

        val adapter = moshi.adapter(DashboardResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertEquals(95, response!!.healthScore)
        assertEquals(2, response.nodes?.total)
        assertEquals(2, response.nodes?.online)
        assertEquals(5, response.vms?.total)
        assertEquals(4, response.vms?.running)
        assertEquals(28.4, response.metrics?.cpuUtilizationPct!!, 0.01)
        assertEquals(54.2, response.metrics?.memoryUtilizationPct!!, 0.01)
        assertEquals(41.8, response.metrics?.storageUtilizationPct!!, 0.01)
        assertEquals(1, response.historicalMetrics.size)
        assertEquals(25.1, response.historicalMetrics[0].cpu!!, 0.01)

        val overview = response.toInfrastructureOverview()
        assertEquals(2, overview.totalConnections)
        assertEquals(2, overview.onlineConnections)
        assertEquals(5, overview.totalVms)
        assertEquals(4, overview.runningVms)
        assertEquals("28.4%", overview.cpu?.displayString)
        assertEquals("54.2%", overview.memory?.displayString)
        assertEquals("41.8%", overview.storage?.displayString)
    }

    @Test
    fun `parse dashboard with null telemetry preserving null and real zero`() {
        val json = """
        {
          "healthScore": null,
          "nodes": { "total": 0, "online": 0, "offline": 0, "degraded": 0 },
          "vms": { "total": 0, "running": 0, "stopped": 0, "suspended": 0 },
          "containers": { "total": 0, "running": 0, "stopped": 0 },
          "metrics": {
            "cpuUtilizationPct": 0.0,
            "memoryUtilizationPct": null,
            "storageUtilizationPct": null
          },
          "historicalMetrics": [],
          "activeAlerts": [],
          "hasLiveInfrastructure": false,
          "isDemoMode": false
        }
        """.trimIndent()

        val adapter = moshi.adapter(DashboardResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        val overview = response!!.toInfrastructureOverview()

        // Real zero must be preserved as 0.0%
        assertNotNull(overview.cpu)
        assertTrue(overview.cpu!!.isAvailable)
        assertEquals("0.0%", overview.cpu!!.displayString)

        // Null metric must be preserved as null (not converted to 0)
        assertNull(overview.memory)
        assertNull(overview.storage)
    }

    @Test
    fun `parse real backend infrastructure connections`() {
        val json = """
        [
          {
            "id": "conn-esxi-01",
            "name": "ESXi-DC1-PROD-01",
            "type": "ESXI",
            "host": "192.168.1.50",
            "port": 443,
            "useHttps": true,
            "skipSslVerify": true,
            "status": "ONLINE",
            "pollIntervalSec": 30,
            "lastCheckedAt": "2026-09-07T09:20:00.000Z",
            "isDemo": false
          },
          {
            "id": "conn-casaos-01",
            "name": "CasaOS-Gateway-01",
            "type": "CASAOS",
            "host": "192.168.1.60",
            "port": 80,
            "useHttps": false,
            "skipSslVerify": false,
            "status": "ONLINE",
            "pollIntervalSec": 30,
            "lastCheckedAt": "2026-09-07T09:20:00.000Z",
            "isDemo": false
          }
        ]
        """.trimIndent()

        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, InfraConnection::class.java)
        val adapter = moshi.adapter<List<InfraConnection>>(type)
        val list = adapter.fromJson(json)

        assertNotNull(list)
        assertEquals(2, list!!.size)
        assertEquals("ESXi-DC1-PROD-01", list[0].name)
        assertEquals(ConnectionType.ESXI, list[0].connectionType)
        assertEquals(ConnectionStatus.ONLINE, list[0].connectionStatus)
        assertEquals("CasaOS-Gateway-01", list[1].name)
        assertEquals(ConnectionType.CASAOS, list[1].connectionType)
    }

    @Test
    fun `parse real backend ESXi all-vms response`() {
        val json = """
        [
          {
            "id": "vm-101",
            "name": "dc1-k8s-master",
            "powerState": "RUNNING",
            "cpuUsagePct": 14.5,
            "memoryUsagePct": 62.1,
            "ipAddress": "192.168.1.110",
            "connectionId": "conn-esxi-01"
          },
          {
            "id": "vm-102",
            "name": "dc1-gitlab-runner",
            "powerState": "STOPPED",
            "cpuUsagePct": null,
            "memoryUsagePct": null,
            "ipAddress": null,
            "connectionId": "conn-esxi-01"
          }
        ]
        """.trimIndent()

        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, VirtualMachine::class.java)
        val adapter = moshi.adapter<List<VirtualMachine>>(type)
        val list = adapter.fromJson(json)

        assertNotNull(list)
        assertEquals(2, list!!.size)
        assertEquals("dc1-k8s-master", list[0].name)
        assertEquals(VmPowerState.RUNNING, list[0].state)
        assertEquals(14.5, list[0].effectiveCpuUsage!!, 0.01)
        assertEquals(62.1, list[0].memoryUsagePercent!!, 0.01)
        assertEquals(VmPowerState.STOPPED, list[1].state)
        assertNull(list[1].effectiveCpuUsage)
    }

    @Test
    fun `verify ISO-8601 dashboard timestamp deserialization`() {
        val json = """
        {
          "timestamp": "2026-09-07T13:59:57.441Z",
          "cpu": 83.9,
          "memory": 95.1,
          "storage": 71.9,
          "networkRxKbps": 0.0,
          "networkTxKbps": 0.0
        }
        """.trimIndent()

        val adapter = moshi.adapter(DashboardHistoricalMetric::class.java)
        val metric = adapter.fromJson(json)

        assertNotNull(metric)
        assertEquals(1788789597441L, metric!!.timestamp)
        assertEquals(83.9, metric.cpu!!, 0.01)
        assertEquals(95.1, metric.memory!!, 0.01)
        assertEquals(71.9, metric.storage!!, 0.01)
    }

    @Test
    fun `verify ISO-8601 telemetry timestamp deserialization`() {
        val json = """
        {
          "timestamp": "2026-09-07T13:08:56.916Z",
          "cpu": 3.6,
          "memory": 33.5,
          "storage": 3.9,
          "networkRx": 12418570.89,
          "networkTx": 840863.61
        }
        """.trimIndent()

        val adapter = moshi.adapter(TelemetryHistoryPoint::class.java)
        val point = adapter.fromJson(json)

        assertNotNull(point)
        // 2026-09-07T13:08:56.916Z -> Instant -> epoch millis
        val expectedEpoch = java.time.Instant.parse("2026-09-07T13:08:56.916Z").toEpochMilli()
        assertEquals(expectedEpoch, point!!.timestamp)
        assertEquals(3.6, point.cpu!!, 0.01)
        assertEquals(33.5, point.memory!!, 0.01)
    }

    @Test
    fun `verify numeric epoch millisecond timestamp deserialization`() {
        val json = """
        {
          "timestamp": 1788772800000,
          "cpu": 45.2,
          "memory": 68.4
        }
        """.trimIndent()

        val adapter = moshi.adapter(DashboardHistoricalMetric::class.java)
        val metric = adapter.fromJson(json)

        assertNotNull(metric)
        assertEquals(1788772800000L, metric!!.timestamp)
        assertEquals(45.2, metric.cpu!!, 0.01)
    }

    @Test
    fun `verify invalid timestamp throws JsonDataException`() {
        val json = """
        {
          "timestamp": "not-a-valid-timestamp-at-all",
          "cpu": 10.0
        }
        """.trimIndent()

        val adapter = moshi.adapter(DashboardHistoricalMetric::class.java)
        try {
            adapter.fromJson(json)
            fail("Expected JsonDataException for malformed timestamp")
        } catch (e: JsonDataException) {
            assertTrue(e.message!!.contains("Cannot parse timestamp"))
        }
    }

    @Test
    fun `verify backend warning field maps to effectiveDegraded`() {
        val jsonWithWarning = """
        {
          "total": 5,
          "online": 4,
          "offline": 0,
          "warning": 1
        }
        """.trimIndent()

        val adapter = moshi.adapter(DashboardNodesSummary::class.java)
        val summary1 = adapter.fromJson(jsonWithWarning)
        assertNotNull(summary1)
        assertEquals(5, summary1!!.total)
        assertEquals(4, summary1.online)
        assertEquals(1, summary1.warning)
        assertEquals(1, summary1.effectiveDegraded)

        val jsonWithDegraded = """
        {
          "total": 3,
          "online": 2,
          "offline": 0,
          "degraded": 1
        }
        """.trimIndent()

        val summary2 = adapter.fromJson(jsonWithDegraded)
        assertNotNull(summary2)
        assertEquals(1, summary2!!.effectiveDegraded)
    }

    @Test
    fun `verify full real backend dashboard payload deserialization`() {
        val realPayload = """
        {
          "hasLiveInfrastructure": true,
          "nodes": {
            "total": 2,
            "online": 2,
            "offline": 0,
            "warning": 0
          },
          "vms": {
            "total": 12,
            "running": 11,
            "stopped": 1,
            "suspended": 0
          },
          "containers": {
            "total": 5,
            "running": 4,
            "stopped": 1
          },
          "metrics": {
            "cpuUtilizationPct": 88.2,
            "memoryUtilizationPct": 94.1,
            "storageUtilizationPct": 71.9,
            "networkTrafficRxKbps": 0,
            "networkTrafficTxKbps": 0,
            "cpuCoresTotal": 20,
            "memoryBytesTotal": 137184247808,
            "memoryBytesUsed": 129090377187,
            "storageBytesTotal": 9023726288896,
            "storageBytesUsed": 6490437976064
          },
          "historicalMetrics": [
            {
              "id": "tel-conn-esxi-mtqy57j1-mtrb52wx",
              "connectionId": "conn-esxi-mtqy57j1",
              "hostId": "host-conn-esxi-mtqy57j1-ha-host",
              "timestamp": "2026-09-07T13:59:57.441Z",
              "cpu": 83.9,
              "cpuCoresTotal": 20,
              "memory": 95.1,
              "memoryBytesUsed": 130462219665,
              "memoryBytesTotal": 137184247808,
              "storage": 71.9,
              "storageBytesUsed": 6490437976064,
              "storageBytesTotal": 9023726288896,
              "networkRxKbps": 0,
              "networkTxKbps": 0,
              "uptimeSeconds": 4958540,
              "latencyMs": 175
            }
          ],
          "activeAlerts": [
            {
              "id": "alert-1788767783409-c089",
              "connectionId": "conn-esxi-mtqy57j1",
              "title": "High Memory Alert: ESXI",
              "message": "Memory utilization of ESXI reached 94.1% (Threshold: 92%)",
              "severity": "CRITICAL",
              "status": "ACTIVE",
              "source": "ESXI",
              "resourceType": "ESXI",
              "valueObserved": 94.1,
              "threshold": 92,
              "createdAt": "2026-09-07T07:56:23.409Z",
              "updatedAt": "2026-09-07T14:06:57.794Z"
            }
          ],
          "recentEvents": [],
          "recentAuditLogs": [],
          "isDemoMode": false,
          "healthScore": 80,
          "lastUpdated": "2026-09-07T14:07:14.530Z"
        }
        """.trimIndent()

        val adapter = moshi.adapter(DashboardResponse::class.java)
        val response = adapter.fromJson(realPayload)

        assertNotNull(response)
        assertEquals(80, response!!.healthScore)
        assertEquals(2, response.nodes?.total)
        assertEquals(2, response.nodes?.online)
        assertEquals(0, response.nodes?.warning)
        assertEquals(0, response.nodes?.effectiveDegraded)
        assertEquals(12, response.vms?.total)
        assertEquals(11, response.vms?.running)
        assertEquals(1, response.vms?.stopped)

        // Metrics
        assertEquals(88.2, response.metrics?.cpuUtilizationPct!!, 0.01)
        assertEquals(94.1, response.metrics?.memoryUtilizationPct!!, 0.01)
        assertEquals(71.9, response.metrics?.storageUtilizationPct!!, 0.01)

        // Historical metrics
        assertEquals(1, response.historicalMetrics.size)
        assertEquals(1788789597441L, response.historicalMetrics[0].timestamp)
        assertEquals(83.9, response.historicalMetrics[0].cpu!!, 0.01)

        // Active alerts
        assertEquals(1, response.activeAlerts.size)
        val alert = response.activeAlerts[0]
        assertEquals("alert-1788767783409-c089", alert.id)
        assertEquals("CRITICAL", alert.severity)
        assertEquals("ACTIVE", alert.status)
        assertEquals("2026-09-07T07:56:23.409Z", alert.createdAt)
        assertEquals(java.time.Instant.parse("2026-09-07T07:56:23.409Z").toEpochMilli(), alert.effectiveTimestamp)

        // Overview mapping
        val overview = response.toInfrastructureOverview()
        assertEquals(2, overview.totalConnections)
        assertEquals(2, overview.onlineConnections)
        assertEquals(12, overview.totalVms)
        assertEquals(11, overview.runningVms)
        assertEquals(1, overview.activeAlerts)
        assertEquals(1, overview.criticalAlerts)
        assertEquals("88.2%", overview.cpu?.displayString)
        assertEquals("94.1%", overview.memory?.displayString)
        assertEquals("71.9%", overview.storage?.displayString)
    }
}
