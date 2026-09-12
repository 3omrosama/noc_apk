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
import com.example.data.model.VirtualMachine
import com.example.data.model.VmActionType
import com.example.data.model.VmPowerState
import com.example.ui.components.PowerStateBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.VmsViewModel

@Composable
fun VmsScreen(
    viewModel: VmsViewModel,
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
                            text = "VIRTUAL MACHINES",
                            color = NocCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "VM Lifecycle & Telemetry",
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
                            .testTag("refresh_vms_button")
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
                    placeholder = { Text("Search by VM name, IP, or host...", fontSize = 13.sp) },
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
                        .testTag("vms_search_input")
                )
            }

            // Power Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PowerFilterChip(
                        label = "All VMs",
                        isSelected = state.selectedPowerFilter == null,
                        onClick = { viewModel.setPowerFilter(null) }
                    )
                    PowerFilterChip(
                        label = "Running",
                        isSelected = state.selectedPowerFilter == VmPowerState.RUNNING,
                        onClick = { viewModel.setPowerFilter(VmPowerState.RUNNING) }
                    )
                    PowerFilterChip(
                        label = "Stopped",
                        isSelected = state.selectedPowerFilter == VmPowerState.STOPPED,
                        onClick = { viewModel.setPowerFilter(VmPowerState.STOPPED) }
                    )
                    PowerFilterChip(
                        label = "Suspended",
                        isSelected = state.selectedPowerFilter == VmPowerState.SUSPENDED,
                        onClick = { viewModel.setPowerFilter(VmPowerState.SUSPENDED) }
                    )
                }
            }

            val vms = state.vms.dataOrNull ?: emptyList()
            val query = state.searchQuery.trim().lowercase()

            val filteredVms = vms.filter { vm ->
                val matchesFilter = state.selectedPowerFilter == null || vm.state == state.selectedPowerFilter
                val matchesQuery = query.isEmpty() ||
                        vm.name.lowercase().contains(query) ||
                        (vm.ipAddress?.lowercase()?.contains(query) == true) ||
                        vm.effectiveHost.lowercase().contains(query)
                matchesFilter && matchesQuery
            }

            if (filteredVms.isEmpty()) {
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
                            Icon(imageVector = Icons.Default.Computer, contentDescription = null, tint = NocTextDisabled, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "No matching virtual machines found", color = NocTextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(filteredVms, key = { it.id }) { vm ->
                    VmCard(
                        vm = vm,
                        canExecuteActions = state.canExecuteActions,
                        onRequestAction = { action -> viewModel.requestVmAction(vm, action) }
                    )
                }
            }
        }

        // Action Confirmation Dialog
        val activeDialog = state.activeActionDialog
        if (activeDialog != null) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissActionDialog() },
                containerColor = NocSurface,
                titleContentColor = NocTextPrimary,
                textContentColor = NocTextSecondary,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (activeDialog.action.isDestructive) Icons.Default.Warning else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (activeDialog.action.isDestructive) NocAmber else NocCyan
                        )
                        Text(
                            text = "Confirm ${activeDialog.action.displayName}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Target: ${activeDialog.vm.name}",
                            color = NocCyan,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                        Text(
                            text = activeDialog.action.confirmationMessage,
                            color = NocTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )

                        if (activeDialog.error != null) {
                            Surface(
                                color = NocRedBg.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NocRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = activeDialog.error,
                                    color = NocRed,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmVmAction() },
                        enabled = !activeDialog.isExecuting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeDialog.action.isDestructive) NocRed else NocCyan,
                            contentColor = if (activeDialog.action.isDestructive) NocTextPrimary else NocBackground
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("confirm_action_button")
                    ) {
                        if (activeDialog.isExecuting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = NocTextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("EXECUTING...", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        } else {
                            Text(
                                text = "CONFIRM",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissActionDialog() },
                        enabled = !activeDialog.isExecuting,
                        modifier = Modifier.testTag("cancel_action_button")
                    ) {
                        Text("CANCEL", color = NocTextSecondary, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
            )
        }
    }
}

@Composable
private fun VmCard(
    vm: VirtualMachine,
    canExecuteActions: Boolean,
    onRequestAction: (VmActionType) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NocSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NocSurfaceHighlight),
        modifier = Modifier.fillMaxWidth().testTag("vm_card_${vm.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Computer, contentDescription = null, tint = NocCyan, modifier = Modifier.size(20.dp))
                    Text(
                        text = vm.name,
                        color = NocTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                PowerStateBadge(state = vm.state)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Host: ${vm.effectiveHost}",
                    color = NocTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "IP: ${vm.ipAddress ?: "—"}",
                    color = NocTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Telemetry Gauges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("CPU", color = NocTextTertiary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            text = if (vm.cpuUsage != null) "${String.format("%.1f", vm.cpuUsage)}%" else "—",
                            color = NocTextSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    val cpuProg = vm.cpuUsage?.let { (it / 100.0).coerceIn(0.0, 1.0).toFloat() } ?: 0f
                    LinearProgressIndicator(
                        progress = { cpuProg },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = NocCyan,
                        trackColor = NocSurfaceVariant
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("MEM", color = NocTextTertiary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            text = if (vm.memoryUsagePercent != null) "${String.format("%.1f", vm.memoryUsagePercent)}%" else "—",
                            color = NocTextSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    val memProg = vm.memoryUsagePercent?.let { (it / 100.0).coerceIn(0.0, 1.0).toFloat() } ?: 0f
                    LinearProgressIndicator(
                        progress = { memProg },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = NocBlue,
                        trackColor = NocSurfaceVariant
                    )
                }
            }

            // Lifecycle Action Buttons (RBAC Protected)
            if (canExecuteActions) {
                Divider(color = NocSurfaceHighlight, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val actions = vm.availableActions()
                    actions.forEach { action ->
                        VmActionButton(
                            action = action,
                            onClick = { onRequestAction(action) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VmActionButton(
    action: VmActionType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bgColor, contentColor) = when {
        action == VmActionType.POWER_ON -> Pair(NocGreenBg, NocGreen)
        action == VmActionType.POWER_OFF -> Pair(NocRedBg, NocRed)
        action == VmActionType.RESET || action == VmActionType.RESTART -> Pair(NocSurfaceVariant, NocAmber)
        else -> Pair(NocSurfaceVariant, NocTextSecondary)
    }

    Surface(
        onClick = onClick,
        color = bgColor.copy(alpha = 0.7f),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, contentColor.copy(alpha = 0.4f)),
        modifier = modifier.height(32.dp).testTag("action_${action.actionKey}")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = action.displayName.uppercase(),
                color = contentColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun PowerFilterChip(
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
