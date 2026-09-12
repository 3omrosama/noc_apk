package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.di.AppContainer
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.*

enum class NocTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD("Overview", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "tab_dashboard"),
    NODES("Nodes", Icons.Filled.Dns, Icons.Outlined.Dns, "tab_nodes"),
    VMS("VMs", Icons.Filled.Computer, Icons.Outlined.Computer, "tab_vms"),
    ALERTS("Alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications, "tab_alerts"),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
}

@Composable
fun NocApp(
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val vmFactory = remember { ViewModelFactory(container) }

    val startDestination = remember {
        val serverUrl = container.sessionManager.getServerUrl()
        val hasSession = container.sessionManager.hasValidSession()
        when {
            serverUrl.isNullOrBlank() -> "welcome"
            !hasSession -> "login"
            else -> "main"
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("welcome") {
            val setupVm: ServerSetupViewModel = viewModel(factory = vmFactory)
            WelcomeScreen(
                viewModel = setupVm,
                onContinueToLogin = {
                    navController.navigate("login") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            val authVm: AuthViewModel = viewModel(factory = vmFactory)
            LoginScreen(
                viewModel = authVm,
                onChangeServer = {
                    navController.navigate("welcome")
                },
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            MainNocContainer(
                container = container,
                vmFactory = vmFactory,
                onChangeServer = {
                    navController.navigate("welcome") {
                        popUpTo("main") { inclusive = true }
                    }
                },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
fun MainNocContainer(
    container: AppContainer,
    vmFactory: ViewModelFactory,
    onChangeServer: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(NocTab.DASHBOARD) }

    val dashboardVm: DashboardViewModel = viewModel(factory = vmFactory)
    val nodesVm: NodesViewModel = viewModel(factory = vmFactory)
    val vmsVm: VmsViewModel = viewModel(factory = vmFactory)
    val alertsVm: AlertsViewModel = viewModel(factory = vmFactory)
    val settingsVm: SettingsViewModel = viewModel(factory = vmFactory)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 600.dp

        if (isTablet) {
            // Adaptive layout for Tablets and Expanded Screens: NavigationRail
            Row(modifier = Modifier.fillMaxSize().background(NocBackground)) {
                NavigationRail(
                    containerColor = NocSurface,
                    contentColor = NocTextSecondary,
                    modifier = Modifier.border(1.dp, NocSurfaceBorder)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    NocTab.entries.forEach { tab ->
                        val selected = currentTab == tab
                        NavigationRailItem(
                            selected = selected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title,
                                    tint = if (selected) NocCyan else NocTextTertiary
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (selected) NocCyan else NocTextTertiary
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                indicatorColor = NocCyan.copy(alpha = 0.15f),
                                selectedIconColor = NocCyan,
                                unselectedIconColor = NocTextTertiary,
                                selectedTextColor = NocCyan,
                                unselectedTextColor = NocTextTertiary
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    TabContent(
                        tab = currentTab,
                        dashboardVm = dashboardVm,
                        nodesVm = nodesVm,
                        vmsVm = vmsVm,
                        alertsVm = alertsVm,
                        settingsVm = settingsVm,
                        onSwitchTab = { currentTab = it },
                        onChangeServer = onChangeServer,
                        onLogout = onLogout
                    )
                }
            }
        } else {
            // Mobile layout: Bottom NavigationBar
            Scaffold(
                containerColor = NocBackground,
                bottomBar = {
                    NavigationBar(
                        containerColor = NocSurface,
                        contentColor = NocTextSecondary,
                        modifier = Modifier.border(1.dp, NocSurfaceBorder)
                    ) {
                        NocTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title,
                                        tint = if (selected) NocCyan else NocTextTertiary
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (selected) NocCyan else NocTextTertiary
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = NocCyan.copy(alpha = 0.15f),
                                    selectedIconColor = NocCyan,
                                    unselectedIconColor = NocTextTertiary,
                                    selectedTextColor = NocCyan,
                                    unselectedTextColor = NocTextTertiary
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    TabContent(
                        tab = currentTab,
                        dashboardVm = dashboardVm,
                        nodesVm = nodesVm,
                        vmsVm = vmsVm,
                        alertsVm = alertsVm,
                        settingsVm = settingsVm,
                        onSwitchTab = { currentTab = it },
                        onChangeServer = onChangeServer,
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}

@Composable
private fun TabContent(
    tab: NocTab,
    dashboardVm: DashboardViewModel,
    nodesVm: NodesViewModel,
    vmsVm: VmsViewModel,
    alertsVm: AlertsViewModel,
    settingsVm: SettingsViewModel,
    onSwitchTab: (NocTab) -> Unit,
    onChangeServer: () -> Unit,
    onLogout: () -> Unit
) {
    when (tab) {
        NocTab.DASHBOARD -> DashboardScreen(
            viewModel = dashboardVm,
            onNavigateToNodes = { onSwitchTab(NocTab.NODES) },
            onNavigateToVms = { onSwitchTab(NocTab.VMS) },
            onNavigateToAlerts = { onSwitchTab(NocTab.ALERTS) }
        )
        NocTab.NODES -> NodesScreen(viewModel = nodesVm)
        NocTab.VMS -> VmsScreen(viewModel = vmsVm)
        NocTab.ALERTS -> AlertsScreen(viewModel = alertsVm)
        NocTab.SETTINGS -> SettingsScreen(
            viewModel = settingsVm,
            onChangeServer = onChangeServer,
            onLogout = onLogout
        )
    }
}
