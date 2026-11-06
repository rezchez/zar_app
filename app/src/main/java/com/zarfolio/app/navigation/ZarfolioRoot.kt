package com.zarfolio.app.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
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
import com.zarfolio.feature.dashboard.AccountSettingsScreen
import com.zarfolio.feature.dashboard.AppSettingsScreen
import com.zarfolio.feature.products.ProductSearchScreen
import com.zarfolio.feature.products.RecentProductsScreen
import com.zarfolio.feature.products.AllEtiquettesScreen
import com.zarfolio.feature.products.PriceInquiryScreen

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
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    ZarfolioMark(30)
                    Text("زرفولیو-اتیکت و قیمت", style = MaterialTheme.typography.titleMedium)
                }
            },
            actions = { IconButton(onClick = onToggleTheme) { Icon(if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, "تغییر حالت شب و روز") } },
        )
    }, bottomBar = {
        if (showBar) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            val tabs = listOf(
                Triple(AppDestinations.HOME, "خانه", Icons.Rounded.Home),
                Triple(AppDestinations.PRODUCTS, "جستجو", Icons.Rounded.Search),
                Triple(AppDestinations.RECENT, "آخرین‌ها", Icons.Rounded.Inventory2),
                Triple(AppDestinations.SETTINGS, "تنظیمات", Icons.Rounded.Settings),
                Triple(AppDestinations.PROFILE, "پروفایل", Icons.Rounded.Settings),
            )
            tabs.forEach { (destination, label, icon) ->
                NavigationBarItem(selected = route == destination, onClick = {
                    nav.navigate(destination) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
                }, icon = {
                    if (destination == AppDestinations.PROFILE) ProfileAvatar(userName)
                    else Icon(icon, contentDescription = label)
                }, label = { Text(label) })
            }
        }
    }) { padding ->
        NavHost(navController = nav, startDestination = AppDestinations.HOME, modifier = Modifier.padding(padding)) {
            composable(AppDestinations.HOME) { DashboardScreen(onNavigate = { nav.navigate(it) }) }
            composable(AppDestinations.PROFILE) { AccountSettingsScreen(userName, onOpenSettings = { nav.navigate(AppDestinations.SETTINGS) }) }
            composable(AppDestinations.SETTINGS) { AppSettingsScreen(servers.firstOrNull { it.isActive }, onConnectionSettings = { nav.navigate(AppDestinations.CONNECTION) }, isDarkTheme = isDarkTheme, onToggleTheme = onToggleTheme) }
            composable(AppDestinations.PRICE_INQUIRY) { PriceInquiryScreen() }
            composable(AppDestinations.PRODUCTS) { ProductSearchScreen() }
            composable(AppDestinations.CAMERA) { CameraScreen() }
            composable(AppDestinations.RECENT) { RecentProductsScreen() }
            composable(AppDestinations.WITHOUT_PHOTO) { RecentProductsScreen(onlyWithoutPhoto = true) }
            composable(AppDestinations.ALL_ETIQUETTES) { AllEtiquettesScreen() }
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

@Composable
private fun ProfileAvatar(userName: String?) {
    val initial = userName?.trim()?.firstOrNull()?.uppercaseChar()?.toString().orEmpty()
    Box(
        Modifier.size(27.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (initial.isNotEmpty()) {
            Text(initial, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp, textAlign = TextAlign.Center)
        } else {
            Icon(Icons.Rounded.AccountCircle, contentDescription = "پروفایل", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
        }
    }
}
