package com.zarfolio.feature.dashboard

import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zarfolio.core.designsystem.components.*
import com.zarfolio.core.model.ServerConfiguration

@Composable
fun DashboardScreen(userName: String?, onNavigate: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
        BrandTopBar()
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("داشبورد", style = MaterialTheme.typography.headlineMedium)
                Text("${userName?.let { "خوش آمدید، $it" } ?: "به زر فولیو خوش آمدید"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(20.dp))
        ZarfolioCard(Modifier.fillMaxWidth()) {
            Text("شروع کار", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text("پس از ورود، جست‌وجوی بارکد و ثبت عکس محصول به‌صورت مستقیم روی سرور انجام می‌شود.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(18.dp))
        Text("دسترسی سریع", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        DashboardAction("جستجوی محصولات", "جست‌وجو در کاتالوگ سرور", Icons.Rounded.Search) { onNavigate("products") }
        DashboardAction("عکاسی محصولات", "انتخاب اتیکت و ثبت عکس اصلی محصول", Icons.Rounded.PhotoCamera) { onNavigate("camera") }
        DashboardAction("آخرین محصولات", "آخرین اتیکت‌های موجود در سرور", Icons.Rounded.Inventory2) { onNavigate("recent") }
        DashboardAction("تنظیمات اتصال", "مدیریت نشانی سرور", Icons.Rounded.CloudSync) { onNavigate("connection") }
    }
}

@Composable
private fun DashboardAction(title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column { Text(title, style = MaterialTheme.typography.titleMedium); Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
fun ServerConfigurationScreen(onSaved: () -> Unit, viewModel: ServerConfigurationViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val active = state.servers.firstOrNull { it.isActive }
    var name by remember(active?.id) { mutableStateOf(active?.displayName.orEmpty()) }
    var url by remember(active?.id) { mutableStateOf(active?.baseUrl.orEmpty()) }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
        BrandTopBar()
        Spacer(Modifier.height(20.dp))
        PageTitle(if (active == null) "تنظیم اتصال" else "اتصال کسب‌وکار", "نشانی وب‌اپ زر فولیو را وارد کنید")
        Spacer(Modifier.height(16.dp))
        if (state.servers.isNotEmpty()) {
            Text("سرورهای ذخیره‌شده", style = MaterialTheme.typography.titleMedium)
            state.servers.forEach { server -> ServerRow(server, selected = server.isActive, onClick = { viewModel.select(server.id) }) }
            Spacer(Modifier.height(10.dp))
        }
        ZarfolioCard(Modifier.fillMaxWidth()) {
            Text("نام کسب‌وکار", style = MaterialTheme.typography.titleMedium)
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
                OutlinedButton(onClick = { viewModel.test(name, url) }, modifier = Modifier.weight(1f), enabled = !state.checking) {
                    if (state.checking) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp) else Icon(Icons.Rounded.CloudSync, null)
                    Spacer(Modifier.width(7.dp)); Text("آزمون اتصال")
                }
                Button(onClick = { viewModel.save(name, url); onSaved() }, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.AddBusiness, null); Spacer(Modifier.width(7.dp)); Text("ذخیره") }
            }
            state.resultMessage?.let { message -> Spacer(Modifier.height(12.dp)); Text(message, color = if (state.connectionPassed == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        }
        Spacer(Modifier.height(14.dp))
        Text("برای سرور عمومی از HTTPS استفاده کنید. نشانی HTTP فقط برای میزبان‌های خصوصی یا محلی پذیرفته می‌شود.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ServerRow(server: ServerConfiguration, selected: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(Modifier.width(6.dp))
            Column { Text(server.displayName, style = MaterialTheme.typography.titleMedium); Text(server.baseUrl, style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
