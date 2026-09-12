package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.api.ConnectionTestStatus
import com.example.ui.theme.*
import com.example.ui.viewmodel.ServerSetupViewModel

@Composable
fun WelcomeScreen(
    viewModel: ServerSetupViewModel,
    onContinueToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NocBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // NOC Logo / Hero Element
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(NocSurfaceVariant)
                    .border(2.dp, NocCyan.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Dns,
                    contentDescription = "InfraManager NOC",
                    tint = NocCyan,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "InfraManager Mobile",
                color = NocTextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Text(
                text = "IT Infrastructure & NOC Operations",
                color = NocTextSecondary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = NocSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NocSurfaceHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "CONNECT TO NOC SERVER",
                        color = NocCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "Enter your InfraManager server URL. Supports public HTTPS, private LAN IP, and local hostnames with custom ports.",
                        color = NocTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = state.rawUrl,
                        onValueChange = viewModel::onUrlChanged,
                        label = { Text("Server Host / URL") },
                        placeholder = { Text("https://noc.example.com") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lan,
                                contentDescription = null,
                                tint = NocCyan
                            )
                        },
                        trailingIcon = {
                            if (state.rawUrl.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.onUrlChanged("") },
                                    modifier = Modifier.testTag("clear_url_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = NocTextSecondary
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NocCyan,
                            unfocusedBorderColor = NocSurfaceHighlight,
                            focusedTextColor = NocTextPrimary,
                            unfocusedTextColor = NocTextPrimary,
                            focusedLabelColor = NocCyan,
                            unfocusedLabelColor = NocTextSecondary,
                            focusedContainerColor = NocSurfaceVariant,
                            unfocusedContainerColor = NocSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("server_url_input")
                    )

                    if (state.normalizedUrl.isNotBlank() && state.normalizedUrl != state.rawUrl.trim()) {
                        Text(
                            text = "Normalized: ${state.normalizedUrl}",
                            color = NocBlue,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Quick Presets:",
                        color = NocTextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresetChip(
                            label = "Demo NOC",
                            onClick = { viewModel.selectPresetUrl("https://noc.it.nds-technology.com") }
                        )
                        PresetChip(
                            label = "Local IP:3000",
                            onClick = { viewModel.selectPresetUrl("http://172.16.0.12:3000") }
                        )
                        PresetChip(
                            label = "LAN Port",
                            onClick = { viewModel.selectPresetUrl("http://192.168.1.50:3000") }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Connection Test Status Card
                    if (state.testStatus != null || state.isTesting) {
                        TestStatusBanner(
                            status = state.testStatus,
                            isTesting = state.isTesting,
                            errorMessage = state.errorMessage
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Test Connection Button
                    Button(
                        onClick = viewModel::testConnection,
                        enabled = !state.isTesting && state.rawUrl.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NocSurfaceHighlight,
                            contentColor = NocCyan
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("test_connection_button")
                    ) {
                        if (state.isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = NocCyan,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PROBING SERVER...", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        } else {
                            Icon(imageVector = Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("TEST CONNECTION", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Continue Button
                    Button(
                        onClick = { viewModel.saveServerAndProceed(onContinueToLogin) },
                        enabled = state.canProceed || (state.rawUrl.isNotBlank() && !state.isTesting),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NocCyan,
                            contentColor = NocBackground
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("continue_to_login_button")
                    ) {
                        Text(
                            text = "CONTINUE TO LOGIN",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = NocTextTertiary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Encrypted NOC Session with Android Keystore",
                    color = NocTextTertiary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = NocSurfaceVariant,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NocSurfaceHighlight)
    ) {
        Text(
            text = label,
            color = NocTextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun TestStatusBanner(
    status: ConnectionTestStatus?,
    isTesting: Boolean,
    errorMessage: String?
) {
    val (bgColor, borderColor, textColor, icon) = when {
        isTesting -> Quadruple(
            NocSurfaceVariant,
            NocCyan.copy(alpha = 0.5f),
            NocCyan,
            Icons.Default.Sync
        )
        status == ConnectionTestStatus.CONNECTED -> Quadruple(
            NocGreenBg.copy(alpha = 0.7f),
            NocGreen,
            NocGreen,
            Icons.Default.CheckCircle
        )
        status == ConnectionTestStatus.AUTH_REQUIRED -> Quadruple(
            NocGreenBg.copy(alpha = 0.5f),
            NocCyan,
            NocCyan,
            Icons.Default.VpnKey
        )
        else -> Quadruple(
            NocRedBg.copy(alpha = 0.7f),
            NocRed,
            NocRed,
            Icons.Default.ErrorOutline
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = if (isTesting) "Probing server endpoint..." else (status?.displayName ?: "Testing"),
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (!isTesting && errorMessage != null && errorMessage != status?.displayName) {
                    Text(
                        text = errorMessage,
                        color = NocTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
