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
import com.example.data.model.Alert
import com.example.data.model.AlertSeverity
import com.example.data.model.AlertStatus
import com.example.ui.components.SeverityBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.AlertsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }

    androidx.compose.runtime.LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        containerColor = NocBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            text = "ALERT CENTER",
                            color = NocCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Incidents & Security",
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
                            .testTag("refresh_alerts_button")
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
                    placeholder = { Text("Search alerts by title or source...", fontSize = 13.sp) },
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
                        .testTag("alerts_search_input")
                )
            }

            // Severity Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AlertFilterChip(
                        label = "All Severity",
                        isSelected = state.selectedSeverity == null,
                        onClick = { viewModel.setSeverityFilter(null) }
                    )
                    AlertFilterChip(
                        label = "Critical",
                        isSelected = state.selectedSeverity == AlertSeverity.CRITICAL,
                        onClick = { viewModel.setSeverityFilter(AlertSeverity.CRITICAL) },
                        badgeColor = NocRed
                    )
                    AlertFilterChip(
                        label = "Warning",
                        isSelected = state.selectedSeverity == AlertSeverity.WARNING,
                        onClick = { viewModel.setSeverityFilter(AlertSeverity.WARNING) },
                        badgeColor = NocAmber
                    )
                    AlertFilterChip(
                        label = "Info",
                        isSelected = state.selectedSeverity == AlertSeverity.INFO,
                        onClick = { viewModel.setSeverityFilter(AlertSeverity.INFO) },
                        badgeColor = NocBlue
                    )
                }
            }

            val alerts = state.alerts.dataOrNull ?: emptyList()
            val query = state.searchQuery.trim().lowercase()

            val filteredAlerts = alerts.filter { alert ->
                val matchesSeverity = state.selectedSeverity == null || alert.alertSeverity == state.selectedSeverity
                val matchesStatus = state.selectedStatus == null || alert.alertStatus == state.selectedStatus
                val matchesQuery = query.isEmpty() ||
                        alert.title.lowercase().contains(query) ||
                        alert.message.lowercase().contains(query) ||
                        alert.effectiveSource.lowercase().contains(query)
                matchesSeverity && matchesStatus && matchesQuery
            }

            if (filteredAlerts.isEmpty()) {
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
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = NocGreen, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "No active infrastructure alerts", color = NocTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(text = "All systems reporting operational telemetry", color = NocTextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(filteredAlerts, key = { it.id }) { alert ->
                    AlertCard(
                        alert = alert,
                        isOperating = state.operatingAlertId == alert.id,
                        canManageAlerts = state.canManageAlerts,
                        onAcknowledge = { viewModel.acknowledgeAlert(alert) },
                        onResolve = { viewModel.resolveAlert(alert) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertCard(
    alert: Alert,
    isOperating: Boolean,
    canManageAlerts: Boolean,
    onAcknowledge: () -> Unit,
    onResolve: () -> Unit
) {
    val borderColor = when (alert.alertSeverity) {
        AlertSeverity.CRITICAL -> NocRed.copy(alpha = 0.8f)
        AlertSeverity.WARNING -> NocAmber.copy(alpha = 0.6f)
        AlertSeverity.INFO -> NocSurfaceHighlight
    }

    val cardBg = if (alert.alertSeverity == AlertSeverity.CRITICAL) {
        NocRedBg.copy(alpha = 0.2f)
    } else {
        NocSurface
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth().testTag("alert_card_${alert.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SeverityBadge(severity = alert.alertSeverity)
                    Text(
                        text = alert.alertStatus.displayName.uppercase(),
                        color = NocTextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                Text(
                    text = timeFormat.format(Date(alert.effectiveTimestamp)),
                    color = NocTextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = alert.title,
                color = NocTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = alert.message,
                color = NocTextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Source: ${alert.effectiveSource}",
                color = NocCyan,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )

            // RBAC Action Buttons
            if (canManageAlerts && alert.alertStatus != AlertStatus.RESOLVED) {
                Divider(color = NocSurfaceHighlight, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isOperating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = NocCyan)
                    } else {
                        if (alert.alertStatus == AlertStatus.ACTIVE) {
                            TextButton(
                                onClick = onAcknowledge,
                                modifier = Modifier.testTag("ack_alert_button_${alert.id}")
                            ) {
                                Text("ACKNOWLEDGE", color = NocCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onResolve,
                            colors = ButtonDefaults.buttonColors(containerColor = NocGreenBg, contentColor = NocGreen),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("resolve_alert_button_${alert.id}")
                        ) {
                            Text("RESOLVE", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    badgeColor: Color? = null
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) NocCyan else NocSurfaceVariant,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NocCyan else NocSurfaceHighlight)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            if (badgeColor != null) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(if (isSelected) NocBackground else badgeColor)
                )
            }
            Text(
                text = label,
                color = if (isSelected) NocBackground else NocTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
