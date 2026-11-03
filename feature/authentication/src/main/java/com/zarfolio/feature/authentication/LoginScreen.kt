package com.zarfolio.feature.authentication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.zarfolio.core.model.ServerConfiguration
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zarfolio.core.designsystem.components.*

private fun normalizeOtpDigits(value: String): String = buildString {
    value.forEach { char ->
        val digit = when (char) {
            in '۰'..'۹' -> char.code - '۰'.code
            in '٠'..'٩' -> char.code - '٠'.code
            else -> char.digitToIntOrNull()
        }
        if (digit != null && digit in 0..9 && length < 6) append(('0'.code + digit).toChar())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    servers: List<ServerConfiguration> = emptyList(),
    onSaveConnection: (Boolean, String) -> Unit = { _, _ -> },
    onSelectConnection: (String) -> Unit = {},
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel(),
) {
    var loginId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var localUrl by remember(servers) { mutableStateOf(servers.firstOrNull { it.id == "login-connection-local" }?.baseUrl.orEmpty()) }
    var serverUrl by remember(servers) { mutableStateOf(servers.firstOrNull { it.id == "login-connection-server" }?.baseUrl.orEmpty()) }
    var localMode by remember(servers) { mutableStateOf(servers.firstOrNull { it.isActive }?.id != "login-connection-server") }
    var connectionSheetOpen by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.authenticated) { if (state.authenticated) onSuccess() }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            TextButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null); Text("بازگشت") }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onToggleTheme) { Icon(if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, "تغییر حالت شب و روز") }
        }
        BrandTopBar(); Spacer(Modifier.height(28.dp))
        PageTitle("ورود امن", if (state.mfaRequired) "مرحلهٔ دوم تأیید هویت" else "با حساب زر فولیو وارد شوید")
        Spacer(Modifier.height(18.dp))
        ZarfolioCard(Modifier.fillMaxWidth()) {
            if (!state.mfaRequired) {
                Text("ایمیل یا شماره همراه", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = loginId, onValueChange = { loginId = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Person, null) }, placeholder = { Text("ایمیل یا ۰۹۱۲۱۲۳۴۵۶۷") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                Spacer(Modifier.height(12.dp))
                Text("رمز عبور", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Lock, null) }, visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
            } else {
                Text(state.message ?: "کد تأیید دومرحله‌ای را وارد کنید", style = MaterialTheme.typography.bodyMedium)
                if (state.authenticatorAvailable && state.emailOtpAvailable) {
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.mfaMethod == "totp",
                            onClick = { viewModel.selectMfaMethod("totp") },
                            label = { Text("رمزساز") },
                            modifier = Modifier.weight(1f),
                        )
                        FilterChip(
                            selected = state.mfaMethod == "email",
                            onClick = { viewModel.selectMfaMethod("email") },
                            label = { Text("کد ایمیل") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = normalizeOtpDigits(it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (state.mfaMethod == "totp") "کد ۶ رقمی برنامهٔ رمزساز" else "کد ۶ رقمی ارسال‌شده به ایمیل") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.login(loginId, password, code) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(if (state.mfaRequired) "تأیید و ورود" else "ورود")
            }
            if (!state.mfaRequired) state.message?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
        }
        Spacer(Modifier.height(16.dp))
        val activeConnection = servers.firstOrNull { it.isActive }
        OutlinedButton(
            onClick = { keyboardController?.hide(); connectionSheetOpen = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.CloudSync, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "اتصال: ${activeConnection?.displayName ?: if (localMode) "محلی" else "سرور"}${activeConnection?.baseUrl?.let { "  ·  $it" }.orEmpty()}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text("توکن نشست به‌صورت رمز‌شده با Android Keystore روی دستگاه نگهداری می‌شود.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (connectionSheetOpen) {
        ModalBottomSheet(onDismissRequest = { connectionSheetOpen = false }) {
            val activeModeId = if (localMode) "login-connection-local" else "login-connection-server"
            val savedConnection = servers.firstOrNull { it.id == activeModeId }
            val connectionUrl = if (localMode) localUrl else serverUrl
            Column(
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text("نوع ارتباط با سرور", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(selected = localMode, onClick = { localMode = true }, label = { Text("محلی") }, modifier = Modifier.weight(1f))
                    FilterChip(selected = !localMode, onClick = { localMode = false }, label = { Text("سرور") }, modifier = Modifier.weight(1f))
                }
                Text(if (localMode) "نشانی وب‌اپ در شبکهٔ محلی" else "نشانی IP یا دامنهٔ وب‌اپ", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                    OutlinedTextField(
                        value = connectionUrl,
                        onValueChange = { if (localMode) localUrl = it else serverUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(if (localMode) "http://192.168.1.20:3000" else "https://zar.example.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { onSaveConnection(localMode, connectionUrl); connectionSheetOpen = false },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = connectionUrl.isNotBlank(),
                ) { Text("ذخیره و استفاده از این اتصال") }
                if (savedConnection != null && !savedConnection.isActive) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onSelectConnection(savedConnection.id); connectionSheetOpen = false },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("استفاده از نشانی ذخیره‌شده") }
                }
                Spacer(Modifier.height(8.dp))
                Text("اتصال محلی برای شبکهٔ داخلی است؛ برای سرور اینترنتی از HTTPS استفاده کنید.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
