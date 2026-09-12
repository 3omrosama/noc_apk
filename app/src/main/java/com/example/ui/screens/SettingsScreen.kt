package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onChangeServer: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showChangeServerDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = NocBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column {
                Text(
                    text = "NOC SETTINGS",
                    color = NocCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Configuration & Identity",
                    color = NocTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // User Identity Card
            Card(
                colors = CardDefaults.cardColors(containerColor = NocSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NocSurfaceHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "OPERATOR SESSION",
                        color = NocCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NocSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = NocCyan, modifier = Modifier.size(24.dp))
                            }
                            Column {
                                Text(
                                    text = state.currentUser?.username ?: "Authenticated Operator",
                                    color = NocTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Role: ${state.currentUser?.role?.uppercase() ?: "OPERATOR"}",
                                    color = NocTextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Surface(
                            color = NocGreenBg.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NocGreen.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "ACTIVE",
                                color = NocGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // NOC Server Card
            Card(
                colors = CardDefaults.cardColors(containerColor = NocSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NocSurfaceHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CONNECTED NOC SERVER",
                        color = NocCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = state.serverUrl,
                        color = NocTextPrimary,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showChangeServerDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NocSurfaceHighlight, contentColor = NocCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("change_server_settings_button")
                    ) {
                        Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SWITCH NOC SERVER", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Security & App Info
            Card(
                colors = CardDefaults.cardColors(containerColor = NocSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NocSurfaceHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "SYSTEM & COMPLIANCE",
                        color = NocCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Keystore Encryption", color = NocTextSecondary, fontSize = 12.sp)
                        Text("AES-256 GCM", color = NocGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TLS Validation", color = NocTextSecondary, fontSize = 12.sp)
                        Text("Strict System CA", color = NocGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("App Version", color = NocTextSecondary, fontSize = 12.sp)
                        Text(state.appVersion, color = NocTextTertiary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // Sign out button
            Button(
                onClick = { showLogoutDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = NocRedBg.copy(alpha = 0.8f), contentColor = NocRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("logout_button")
            ) {
                Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("LOGOUT OPERATOR SESSION", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }

        // Logout Confirmation Dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                containerColor = NocSurface,
                title = {
                    Text("Logout Session", color = NocTextPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text("Are you sure you want to end your NOC operator session? Local session tokens will be securely cleared.", color = NocTextSecondary)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            viewModel.logout(onLogout)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NocRed, contentColor = NocTextPrimary)
                    ) {
                        Text("LOGOUT", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("CANCEL", color = NocTextSecondary, fontFamily = FontFamily.Monospace)
                    }
                }
            )
        }

        // Change Server Confirmation Dialog
        if (showChangeServerDialog) {
            AlertDialog(
                onDismissRequest = { showChangeServerDialog = false },
                containerColor = NocSurface,
                title = {
                    Text("Switch NOC Server", color = NocTextPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text("Switching to another NOC server will clear cached telemetry and require authentication with the new server.", color = NocTextSecondary)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showChangeServerDialog = false
                            viewModel.changeServer(onChangeServer)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NocCyan, contentColor = NocBackground)
                    ) {
                        Text("PROCEED", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showChangeServerDialog = false }) {
                        Text("CANCEL", color = NocTextSecondary, fontFamily = FontFamily.Monospace)
                    }
                }
            )
        }
    }
}
