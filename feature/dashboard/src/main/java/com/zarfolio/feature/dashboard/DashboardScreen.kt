package com.zarfolio.feature.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddBusiness
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.PriceCheck
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ImageNotSupported
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zarfolio.core.designsystem.components.*
import com.zarfolio.core.model.ServerConfiguration

@Composable
fun DashboardScreen(onNavigate: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        Text("دسترسی سریع", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardAction("استعلام قیمت", "جست‌وجو و برآورد قیمت", Icons.Rounded.PriceCheck, Modifier.weight(1f)) { onNavigate("price-inquiry") }
                DashboardAction("افزودن عکس به اتیکت", "ثبت تا ۵ عکس", Icons.Rounded.PhotoCamera, Modifier.weight(1f)) { onNavigate("camera") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardAction("اتیکت‌های بدون عکس", "تکمیل آلبوم محصولات", Icons.Rounded.ImageNotSupported, Modifier.weight(1f)) { onNavigate("without-photo") }
                DashboardAction("تنظیمات اتصال", "مدیریت سرورهای نام‌گذاری‌شده", Icons.Rounded.CloudSync, Modifier.weight(1f)) { onNavigate("connection") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardAction("همه اتیکت‌ها", "مشاهدهٔ شبکه‌ای عکس و مشخصات", Icons.Rounded.Collections, Modifier.weight(1f)) { onNavigate("all-etiquettes") }
                DashboardAction("جستجوی اتیکت", "پیدا کردن با نام یا بارکد", Icons.Rounded.Search, Modifier.weight(1f)) { onNavigate("products") }
            }
        }
    }
}

@Composable
private fun DashboardAction(title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val outline = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    Card(onClick = onClick, modifier = modifier.heightIn(min = 154.dp).shadow(9.dp, RoundedCornerShape(23.dp)).border(1.dp, outline, RoundedCornerShape(23.dp)), shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface.copy(alpha = .92f), MaterialTheme.colorScheme.primary.copy(alpha = .08f)))).padding(15.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(23.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Start)
                Spacer(Modifier.height(4.dp))
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
        }
    }
}

@Composable
fun AccountSettingsScreen(userName: String?, onOpenSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
        PageTitle("پروفایل کاربری", "اطلاعات حساب شما")
        Spacer(Modifier.height(16.dp))
        ZarfolioCard(Modifier.fillMaxWidth()) {
            Text("پروفایل", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            Text("نام کاربر", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(userName ?: "کاربر واردشده", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("وضعیت حساب", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("ورود فعال", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Settings, null); Spacer(Modifier.width(8.dp)); Text("تنظیمات برنامه") }
    }
}

@Composable
fun AppSettingsScreen(
    activeServer: ServerConfiguration?,
    onConnectionSettings: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
        PageTitle("تنظیمات", "مدیریت برنامه و اتصال")
        Spacer(Modifier.height(16.dp))
        ZarfolioCard(Modifier.fillMaxWidth()) {
            Text("اتصال فعال", style = MaterialTheme.typography.titleMedium)
            Text(activeServer?.displayName ?: "سروری انتخاب نشده", style = MaterialTheme.typography.bodyLarge)
            activeServer?.baseUrl?.let { Text(it, style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onConnectionSettings, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.CloudSync, null); Spacer(Modifier.width(8.dp)); Text("مدیریت پروفایل‌های سرور") }
        }
        Spacer(Modifier.height(12.dp))
        ZarfolioCard(Modifier.fillMaxWidth()) {
            Text("نمایش", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (isDarkTheme) "حالت شب" else "حالت روز", Modifier.weight(1f))
                Switch(checked = isDarkTheme, onCheckedChange = { onToggleTheme() })
            }
        }
    }
}

@Composable
fun ServerConfigurationScreen(onSaved: () -> Unit, viewModel: ServerConfigurationViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val active = state.servers.firstOrNull { it.isActive }
    var editingId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
        BrandTopBar()
        Spacer(Modifier.height(20.dp))
        PageTitle(if (active == null) "تنظیم اتصال" else "اتصال کسب‌وکار", "نشانی وب‌اپ زر فولیو را وارد کنید")
        Spacer(Modifier.height(16.dp))
        if (state.servers.isNotEmpty()) {
            Text("سرورهای ذخیره‌شده", style = MaterialTheme.typography.titleMedium)
            state.servers.forEach { server ->
                ServerRow(
                    server = server,
                    selected = server.isActive,
                    ping = state.pingResults[server.id],
                    onClick = { viewModel.select(server.id) },
                    onEdit = { editingId = server.id; name = server.displayName; url = server.baseUrl },
                    onPing = { viewModel.testServer(server) },
                )
            }
            Spacer(Modifier.height(10.dp))
        }
        ZarfolioCard(Modifier.fillMaxWidth()) {
            Text(if (editingId == null) "افزودن سرور" else "ویرایش پروفایل اتصال", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("هر پروفایل نام، نشانی مستقل و آزمون اتصال جداگانه دارد.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(value = name, onValueChange = { name = it; viewModel.clearMessage() }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("مثلاً فروشگاه زر") })
            Spacer(Modifier.height(12.dp))
            Text("نشانی سرور", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                OutlinedTextField(value = url, onValueChange = { url = it; viewModel.clearMessage() }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("https://server.example.com") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
            }
            Spacer(Modifier.height(7.dp))
            Text("اتصال http فقط برای شبکهٔ خصوصی یا نشانی محلی مجاز است.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { viewModel.saveProfile(editingId, name, url); editingId = null; name = ""; url = "" }, modifier = Modifier.weight(1f)) { Icon(if (editingId == null) Icons.Rounded.Add else Icons.Rounded.Edit, null); Spacer(Modifier.width(7.dp)); Text("ذخیره پروفایل") }
                if (editingId != null) OutlinedButton(onClick = { editingId = null; name = ""; url = "" }, modifier = Modifier.weight(1f)) { Text("لغو ویرایش") }
            }
            state.resultMessage?.let { message -> Spacer(Modifier.height(12.dp)); Text(message, color = if (state.connectionPassed == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        }
        Spacer(Modifier.height(14.dp))
        Text("برای سرور عمومی از HTTPS استفاده کنید. نشانی HTTP فقط برای میزبان‌های خصوصی یا محلی پذیرفته می‌شود.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ServerRow(server: ServerConfiguration, selected: Boolean, ping: ServerPingResult?, onClick: () -> Unit, onEdit: () -> Unit, onPing: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected, onClick = onClick)
                Column(Modifier.weight(1f).clickable(onClick = onClick)) {
                    Text(server.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(server.baseUrl, style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "ویرایش پروفایل") }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPing, enabled = ping?.testing != true) {
                    if (ping?.testing == true) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Icon(if (ping?.error == null && ping?.latencyMs != null) Icons.Rounded.Wifi else Icons.Rounded.WifiOff, null)
                    Spacer(Modifier.width(6.dp)); Text("تست اتصال")
                }
                when {
                    ping?.testing == true -> Text("در حال آزمون…", color = MaterialTheme.colorScheme.tertiary)
                    ping?.latencyMs != null -> Text("آنلاین · ${ping.latencyMs}ms", color = MaterialTheme.colorScheme.primary)
                    ping?.error != null -> Text("آفلاین · ${ping.error}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    selected -> Text("اتصال فعال", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
