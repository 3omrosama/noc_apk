package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InfrastructureOverview
import com.example.data.model.TelemetryTimeRange
import com.example.data.model.WsConnectionStatus
import com.example.data.repository.Resource
import com.example.ui.components.HistoricalTelemetryCanvas
import com.example.ui.components.NocStatusPill
import com.example.ui.theme.*
import com.example.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToNodes: () -> Unit,
    onNavigateToVms: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val overview = state.overview.dataOrNull

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
            // Geometric Balance Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "INFRAMANAGER MOBILE",
                            color = NocCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Pulsing live status dot
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val glowAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "dotPulse"
                            )

                            val dotColor = when (state.wsStatus) {
                                WsConnectionStatus.LIVE -> NocGreen
                                WsConnectionStatus.RECONNECTING -> NocAmber
                                WsConnectionStatus.OFFLINE -> NocGray
                            }

                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(dotColor.copy(alpha = glowAlpha))
                                    .border(1.dp, dotColor, CircleShape)
                            )

                            Text(
                                text = "NOC Dashboard",
                                color = NocTextPrimary,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.3).sp
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "v1.0.4-stable",
                            color = NocTextTertiary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = state.serverUrl.replace("https://", "").replace("http://", "").ifEmpty { "172.16.0.12:3000" },
                            color = NocCyanLight,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Quick Search Bar
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = { Text("Filter metrics & telemetry...", fontSize = 12.sp, color = NocTextTertiary) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = NocTextTertiary, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = NocTextTertiary, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NocCyan,
                        unfocusedBorderColor = NocSurfaceBorder,
                        focusedContainerColor = NocSurface,
                        unfocusedContainerColor = NocSurface,
                        focusedTextColor = NocTextPrimary,
                        unfocusedTextColor = NocTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dashboard_search_input")
                )
            }

            // 3-Column Geometric Stat Grid (Nodes, VMs, Alerts)
            item {
                val criticalCount = overview?.criticalAlerts ?: 0
                val activeAlertCount = overview?.activeAlerts ?: 0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Nodes Stat Card
                    GeometricStatCard(
                        label = "NODES",
                        value = "${overview?.totalConnections ?: 0}",
                        subtext = "↑ ${overview?.onlineConnections ?: 0} Online",
                        subtextColor = NocGreen,
                        borderColor = NocSurfaceBorder,
                        onClick = onNavigateToNodes,
                        modifier = Modifier.weight(1f).testTag("summary_nodes_item")
                    )

                    // VMs Stat Card
                    GeometricStatCard(
                        label = "VMS",
                        value = "${overview?.totalVms ?: 0}",
                        subtext = "${overview?.runningVms ?: 0} Running",
                        subtextColor = NocTextSecondary,
                        borderColor = NocSurfaceBorder,
                        onClick = onNavigateToVms,
                        modifier = Modifier.weight(1f).testTag("summary_vms_item")
                    )

                    // Alerts Stat Card
                    GeometricStatCard(
                        label = "ALERTS",
                        value = String.format("%02d", activeAlertCount),
                        subtext = if (criticalCount > 0) "$criticalCount Critical" else "Operational",
                        subtextColor = if (criticalCount > 0) NocRed else NocGreen,
                        valueColor = if (criticalCount > 0) NocRed else NocTextPrimary,
                        borderColor = if (criticalCount > 0) NocRedBorder else NocSurfaceBorder,
                        onClick = onNavigateToAlerts,
                        modifier = Modifier.weight(1f).testTag("summary_alerts_item")
                    )
                }
            }

            // Primary Host Cluster / Telemetry Array Card (Geometric Balance 24dp card)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NocSurface),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NocSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = "Primary Host Cluster",
                                    color = NocTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "ESXi-DC1-PROD-01 • Hybrid NOC",
                                    color = NocTextTertiary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Surface(
                                color = NocGreenBg,
                                shape = RoundedCornerShape(100.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NocGreen.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "STABLE",
                                    color = NocGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // CPU Utilization Bar
                        GeometricProgressBar(
                            label = "CPU UTILIZATION",
                            valueText = overview?.cpu?.displayString ?: "0.0%",
                            percent = overview?.cpu?.value ?: 0.0,
                            fillColor = NocCyan,
                            glowColor = NocCyan.copy(alpha = 0.3f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Memory Pool Bar
                        GeometricProgressBar(
                            label = "MEMORY POOL",
                            valueText = overview?.memory?.displayString ?: "0.0%",
                            percent = overview?.memory?.value ?: 0.0,
                            fillColor = NocIndigo,
                            glowColor = NocIndigo.copy(alpha = 0.3f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Storage Array Bar
                        val storageVal = overview?.storage?.value ?: 0.0
                        val isStorageCritical = storageVal >= 85.0
                        GeometricProgressBar(
                            label = "STORAGE ARRAY",
                            valueText = if (isStorageCritical) "${overview?.storage?.displayString} CRITICAL" else overview?.storage?.displayString ?: "0.0%",
                            percent = storageVal,
                            fillColor = if (isStorageCritical) NocRed else NocCyanLight,
                            glowColor = if (isStorageCritical) NocRed.copy(alpha = 0.3f) else Color.Transparent,
                            valueColor = if (isStorageCritical) NocRed else NocTextPrimary
                        )
                    }
                }
            }

            // Active Critical Alert Banner (Geometric Balance Style)
            val criticalAlertCount = overview?.criticalAlerts ?: 0
            if (criticalAlertCount > 0) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ACTIVE CRITICAL ALERTS",
                                color = NocTextTertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "View All",
                                color = NocCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable { onNavigateToAlerts() }
                                    .testTag("view_all_alerts_text")
                            )
                        }

                        Surface(
                            onClick = onNavigateToAlerts,
                            color = NocRedBg,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NocRedBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("critical_alert_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(NocRed.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(NocRed)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "High Infrastructure Latency / Load",
                                        color = NocTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "ESXi-Cluster • $criticalAlertCount incident(s) requiring acknowledgment",
                                        color = NocTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                Surface(
                                    color = NocSurfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = NocTextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Historical Telemetry Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TELEMETRY TRENDS",
                            color = NocTextTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )

                        // Time Range Selector
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TelemetryTimeRange.entries.forEach { range ->
                                Surface(
                                    onClick = { viewModel.selectTimeRange(range) },
                                    color = if (state.selectedTimeRange == range) NocCyan else NocSurfaceVariant,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (state.selectedTimeRange == range) NocCyan else NocSurfaceBorder
                                    )
                                ) {
                                    Text(
                                        text = range.queryValue,
                                        color = if (state.selectedTimeRange == range) NocBackground else NocTextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    val historyPoints = state.historyResponse.dataOrNull?.points ?: emptyList()
                    HistoricalTelemetryCanvas(
                        points = historyPoints,
                        modifier = Modifier.testTag("historical_telemetry_canvas")
                    )
                }
            }
        }
    }
}

@Composable
private fun GeometricStatCard(
    label: String,
    value: String,
    subtext: String,
    subtextColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    valueColor: Color = NocTextPrimary
) {
    Surface(
        onClick = onClick,
        color = NocSurface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                color = NocTextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtext,
                color = subtextColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun GeometricProgressBar(
    label: String,
    valueText: String,
    percent: Double,
    fillColor: Color,
    glowColor: Color = Color.Transparent,
    valueColor: Color = NocTextPrimary
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = NocTextTertiary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = valueText,
                color = valueColor,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        val progress = (percent / 100.0).coerceIn(0.0, 1.0).toFloat()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(NocGrayBg)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceAtLeast(0.02f))
                    .clip(CircleShape)
                    .background(fillColor)
            )
        }
    }
}
