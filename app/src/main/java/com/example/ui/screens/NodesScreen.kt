package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CasaOsHost
import com.example.data.model.ConnectionStatus
import com.example.data.model.EsxiHost
import com.example.ui.components.ConnectionBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.NodesViewModel

@Composable
fun NodesScreen(
    viewModel: NodesViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = NocBackground,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "INFRASTRUCTURE NODES",
                            color = NocCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Hosts & Hypervisors",
                            color = NocTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = viewModel::refresh,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NocSurfaceVariant)
                            .border(1.dp, NocSurfaceHighlight, RoundedCornerShape(8.dp))
                            .testTag("refresh_nodes_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = NocCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = { Text("Search by hostname, IP, or provider...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = NocTextSecondary, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = NocTextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NocCyan,
                        unfocusedBorderColor = NocSurfaceHighlight,
                        focusedContainerColor = NocSurfaceVariant,
                        unfocusedContainerColor = NocSurfaceVariant,
                        focusedTextColor = NocTextPrimary,
                        unfocusedTextColor = NocTextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("nodes_search_input")
                )
            }

            // Status Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        label = "All Nodes",
                        isSelected = state.selectedFilter == null,
                        onClick = { viewModel.setFilter(null) }
                    )
                    FilterChip(
                        label = "Online",
                        isSelected = state.selectedFilter == ConnectionStatus.ONLINE,
                        onClick = { viewModel.setFilter(ConnectionStatus.ONLINE) }
                    )
                    FilterChip(
                        label = "Degraded",
                        isSelected = state.selectedFilter == ConnectionStatus.DEGRADED,
                        onClick = { viewModel.setFilter(ConnectionStatus.DEGRADED) }
                    )
                    FilterChip(
                        label = "Offline",
                        isSelected = state.selectedFilter == ConnectionStatus.OFFLINE,
                        onClick = { viewModel.setFilter(ConnectionStatus.OFFLINE) }
                    )
                }
            }

            val nodesResponse = state.nodes.dataOrNull
            val query = state.searchQuery.trim().lowercase()

            val esxiList = (nodesResponse?.esxiHosts ?: emptyList()).filter { host ->
                val matchesFilter = state.selectedFilter == null || host.connectionStatus == state.selectedFilter
                val matchesQuery = query.isEmpty() ||
                        host.displayName.lowercase().contains(query) ||
                        (host.effectiveIp?.lowercase()?.contains(query) == true) ||
                        (host.connectionName?.lowercase()?.contains(query) == true)
                matchesFilter && matchesQuery
            }

            val casaOsList = (nodesResponse?.casaOsHosts ?: emptyList()).filter { host ->
                val matchesFilter = state.selectedFilter == null || host.connectionStatus == state.selectedFilter
                val matchesQuery = query.isEmpty() ||
                        host.displayName.lowercase().contains(query) ||
                        (host.effectiveEndpoint?.lowercase()?.contains(query) == true)
                matchesFilter && matchesQuery
            }

            // ESXi Section
            if (esxiList.isNotEmpty()) {
                item {
                    Text(
                        text = "VMWARE ESXI HOSTS (${esxiList.size})",
                        color = NocCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(esxiList, key = { "esxi_${it.id}" }) { host ->
                    EsxiHostCard(host = host)
                }
            }

            // CasaOS Section
            if (casaOsList.isNotEmpty()) {
                item {
                    Text(
                        text = "CASAOS SERVERS (${casaOsList.size})",
                        color = NocBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(casaOsList, key = { "casaos_${it.id}" }) { host ->
                    CasaOsHostCard(host = host)
                }
            }

            // Empty state
            if (esxiList.isEmpty() && casaOsList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(NocSurfaceVariant, RoundedCornerShape(12.dp))
                            .border(1.dp, NocSurfaceHighlight, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.Dns, contentDescription = null, tint = NocTextDisabled, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "No matching infrastructure nodes found", color = NocTextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EsxiHostCard(host: EsxiHost) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NocSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NocSurfaceHighlight),
        modifier = Modifier.fillMaxWidth().testTag("esxi_card_${host.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = NocCyan, modifier = Modifier.size(20.dp))
                    Text(
                        text = host.displayName,
                        color = NocTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                ConnectionBadge(status = host.connectionStatus)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "IP: ${host.effectiveIp ?: "—"}",
                    color = NocTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Ver: ${host.version ?: "—"}",
                    color = NocTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "VMs: ${host.vmCount ?: 0}",
                    color = NocCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Resource bars (CPU, Memory, Storage)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniResourceMeter(
                    label = "CPU",
                    percent = host.effectiveCpuUsagePercent,
                    color = NocCyan,
                    modifier = Modifier.weight(1f)
                )
                MiniResourceMeter(
                    label = "RAM",
                    percent = host.effectiveMemoryUsagePercent,
                    color = NocBlue,
                    modifier = Modifier.weight(1f)
                )
                MiniResourceMeter(
                    label = "DISK",
                    percent = host.effectiveStorageUsagePercent,
                    color = NocIndigo,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Uptime: ${host.formattedUptime}",
                    color = NocTextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (host.latencyMs != null) {
                    Text(
                        text = "Latency: ${host.latencyMs}ms",
                        color = if (host.latencyMs > 100) NocAmber else NocGreen,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun CasaOsHostCard(host: CasaOsHost) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NocSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NocSurfaceHighlight),
        modifier = Modifier.fillMaxWidth().testTag("casaos_card_${host.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.CloudQueue, contentDescription = null, tint = NocBlue, modifier = Modifier.size(20.dp))
                    Text(
                        text = host.displayName,
                        color = NocTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                ConnectionBadge(status = host.connectionStatus)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = host.effectiveEndpoint ?: "—",
                color = NocTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniResourceMeter(
                    label = "CPU",
                    percent = host.effectiveCpuUsagePercent,
                    color = NocCyan,
                    modifier = Modifier.weight(1f)
                )
                MiniResourceMeter(
                    label = "RAM",
                    percent = host.effectiveMemoryUsagePercent,
                    color = NocBlue,
                    modifier = Modifier.weight(1f)
                )
                MiniResourceMeter(
                    label = "DISK",
                    percent = host.effectiveStorageUsagePercent,
                    color = NocIndigo,
                    modifier = Modifier.weight(1f)
                )
            }

            if (host.runningAppsCount != null || host.apps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Containers / Apps: ${host.runningAppsCount ?: host.apps.size} Running",
                        color = NocTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniResourceMeter(
    label: String,
    percent: Double?,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = NocTextTertiary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(
                text = if (percent != null) "${String.format("%.0f", percent)}%" else "—",
                color = NocTextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        val progress = percent?.let { (it / 100.0).coerceIn(0.0, 1.0).toFloat() } ?: 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = if (percent != null && percent >= 85) NocRed else color,
            trackColor = NocSurfaceVariant
        )
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) NocCyan else NocSurfaceVariant,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NocCyan else NocSurfaceHighlight)
    ) {
        Text(
            text = label,
            color = if (isSelected) NocBackground else NocTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
