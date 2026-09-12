package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.ui.theme.*
import com.example.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onChangeServer: () -> Unit,
    onLoginSuccess: (User) -> Unit,
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
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Server target chip
            Surface(
                color = NocSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NocSurfaceHighlight)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = NocCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = state.serverUrl.ifEmpty { "No server configured" },
                        color = NocTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    TextButton(
                        onClick = onChangeServer,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(28.dp).testTag("change_server_button")
                    ) {
                        Text(
                            text = "CHANGE",
                            color = NocCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Auth Card
            Card(
                colors = CardDefaults.cardColors(containerColor = NocSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NocSurfaceHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "OPERATOR AUTHENTICATION",
                        color = NocCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "Sign in with your NOC credentials or Demo Operator account to access infrastructure nodes and telemetry.",
                        color = NocTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
                    )

                    // Session expired banner
                    if (state.sessionExpiredMessage != null) {
                        Surface(
                            color = NocAmberBg.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NocAmber),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = NocAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = state.sessionExpiredMessage ?: "",
                                    color = NocAmber,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Error banner
                    if (state.error != null) {
                        Surface(
                            color = NocRedBg.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NocRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .testTag("auth_error_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = NocRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = state.error ?: "",
                                    color = NocRed,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Username
                    OutlinedTextField(
                        value = state.username,
                        onValueChange = viewModel::onUsernameChanged,
                        label = { Text("Username") },
                        placeholder = { Text("admin / operator") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = NocCyan
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
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
                            .testTag("username_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = viewModel::onPasswordChanged,
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = NocCyan
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = viewModel::togglePasswordVisibility,
                                modifier = Modifier.testTag("password_visibility_toggle")
                            ) {
                                Icon(
                                    imageVector = if (state.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (state.isPasswordVisible) "Hide password" else "Show password",
                                    tint = NocTextSecondary
                                )
                            }
                        },
                        visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { viewModel.login(onLoginSuccess) }
                        ),
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
                            .testTag("password_input")
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Submit button
                    Button(
                        onClick = { viewModel.login(onLoginSuccess) },
                        enabled = !state.isLoading && state.username.isNotBlank() && state.password.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NocCyan,
                            contentColor = NocBackground
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_button")
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = NocBackground,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "AUTHENTICATING...",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        } else {
                            Text(
                                "CONNECT & AUTHENTICATE",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(imageVector = Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
