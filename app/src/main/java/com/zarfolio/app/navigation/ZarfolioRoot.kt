package com.zarfolio.app.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.zarfolio.core.designsystem.components.ZarfolioMark
import com.zarfolio.core.designsystem.theme.ZarNavy
import com.zarfolio.feature.authentication.LoginScreen
import com.zarfolio.feature.camera.CameraScreen
import com.zarfolio.feature.dashboard.DashboardScreen
import com.zarfolio.feature.dashboard.ServerConfigurationScreen
import com.zarfolio.feature.dashboard.ServerConfigurationViewModel
import com.zarfolio.feature.products.ProductSearchScreen
import com.zarfolio.feature.products.RecentProductsScreen

@Composable
fun ZarfolioRoot(isDarkTheme: Boolean = false, onToggleTheme: () -> Unit = {}) {
    val serverViewModel: ServerConfigurationViewModel = hiltViewModel()
    val state by serverViewModel.state.collectAsStateWithLifecycle()
    var showServerConfiguration by remember { mutableStateOf(false) }
    when {
        state.loading -> SplashScreen()
        showServerConfiguration -> ServerConfigurationScreen(onSaved = { showServerConfiguration = false })
        !state.authenticated -> LoginScreen(
            onBack = { if (state.servers.any { it.isActive }) showServerConfiguration = true },
            onSuccess = serverViewModel::refreshSession,
            servers = state.servers,
            onSaveConnection = serverViewModel::saveLoginConnection,
            onSelectConnection = serverViewModel::select,
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
        )
        else -> MainNavigation(
            onChangeServer = { showServerConfiguration = true },
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            servers = state.servers,
            onSaveConnection = serverViewModel::saveLoginConnection,
            onSelectConnection = serverViewModel::select,
            userName = state.userName,
        )
    }
}

@Composable
private fun SplashScreen() {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { ZarfolioMark(72); Spacer(Modifier.height(14.dp)); Text("زر فولیو", style = MaterialTheme.typography.headlineMedium); Text("در حال آماده‌سازی فضای کاری", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MainNavigation(
    onChangeServer: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    servers: List<com.zarfolio.core.model.ServerConfiguration>,
    onSaveConnection: (Boolean, String) -> Unit,
    onSelectConnection: (String) -> Unit,
    userName: String?,
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: AppDestinations.HOME
    val showBar = route in AppDestinations.bottomTabs || route == AppDestinations.HOME
    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text("مدیریت اتیکت ها (ویژه اتیکت و قیمت)", style = MaterialTheme.typography.titleMedium) },
            actions = { IconButton(onClick = onToggleTheme) { Icon(if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, "تغییر حالت شب و روز") } },
        )
    }, bottomBar = {
        if (showBar) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            val tabs = listOf(
                Triple(AppDestinations.PRODUCTS, "جستجو", Icons.Rounded.Search),
                Triple(AppDestinations.CAMERA, "عکاسی", Icons.Rounded.PhotoCamera),
                Triple(AppDestinations.RECENT, "آخرین‌ها", Icons.Rounded.Inventory2),
                Triple(AppDestinations.CONNECTION, "اتصال", Icons.Rounded.CloudSync),
            )
            tabs.forEach { (destination, label, icon) ->
                NavigationBarItem(selected = route == destination, onClick = {
                    nav.navigate(destination) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
                }, icon = { Icon(icon, contentDescription = label) }, label = { Text(label) })
            }
        }
    }) { padding ->
        NavHost(navController = nav, startDestination = AppDestinations.HOME, modifier = Modifier.padding(padding)) {
            composable(AppDestinations.HOME) { DashboardScreen(userName = userName, onNavigate = { nav.navigate(it) }) }
            composable(AppDestinations.PRODUCTS) { ProductSearchScreen() }
            composable(AppDestinations.CAMERA) { CameraScreen() }
            composable(AppDestinations.RECENT) { RecentProductsScreen() }
            composable(AppDestinations.CONNECTION) { ServerConfigurationScreen(onSaved = onChangeServer) }
            composable(AppDestinations.LOGIN) {
                LoginScreen(
                    onBack = { nav.popBackStack() },
                    onSuccess = { nav.popBackStack(AppDestinations.LOGIN, inclusive = true) },
                    servers = servers,
                    onSaveConnection = onSaveConnection,
                    onSelectConnection = onSelectConnection,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                )
            }
        }
    }
}
