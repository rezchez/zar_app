package com.zarfolio.feature.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ImageNotSupported
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PriceCheck
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zarfolio.core.designsystem.components.*
import com.zarfolio.core.designsystem.theme.Doran
import com.zarfolio.core.designsystem.theme.ZarDanger
import com.zarfolio.core.designsystem.theme.ZarOrange
import com.zarfolio.core.model.Product
import com.zarfolio.core.model.ServerConfiguration
import com.zarfolio.feature.products.ProductSearchViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    searchViewModel: ProductSearchViewModel = hiltViewModel(),
) {
    val products by searchViewModel.products.collectAsStateWithLifecycle()
    val loading by searchViewModel.loading.collectAsStateWithLifecycle()

    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        searchViewModel.loadAll()
    }

    val totalCount = products.size
    val withoutPhotoCount = remember(products) {
        products.count { it.images.isEmpty() }
    }

    val searchResults = remember(products, searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) {
            emptyList()
        } else {
            products.filter { p ->
                p.displayName.contains(q, ignoreCase = true) ||
                    p.barcode?.contains(q, ignoreCase = true) == true ||
                    p.itemCode?.contains(q, ignoreCase = true) == true ||
                    p.reference?.contains(q, ignoreCase = true) == true
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp)
    ) {
        Spacer(Modifier.height(14.dp))

        // Replace "دسترسی سریع" with Expandable Animated Search Bar
        ExpandableSearchBar(
            isExpanded = isSearchExpanded,
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onExpandChange = { isSearchExpanded = it },
            onSearchSubmit = { text ->
                if (text.isNotBlank()) {
                    searchViewModel.search(text)
                    onNavigate("products")
                }
            },
        )

        AnimatedVisibility(
            visible = isSearchExpanded && searchQuery.isNotBlank(),
            enter = expandVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(280)),
            exit = shrinkVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(280)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (searchResults.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "نتیجه‌ای برای «$searchQuery» یافت نشد",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Doran),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = "${searchResults.size} نتیجه پیدا شد:",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = Doran),
                        color = MaterialTheme.colorScheme.primary
                    )
                    searchResults.take(5).forEach { product ->
                        SearchResultItem(product = product) {
                            onNavigate("products")
                        }
                    }
                    if (searchResults.size > 5) {
                        TextButton(
                            onClick = {
                                searchViewModel.search(searchQuery)
                                onNavigate("products")
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                "مشاهده همه (${searchResults.size} مورد)",
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = Doran)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Dashboard Navigation Cards Grid
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DashboardAction(
                    title = "استعلام قیمت",
                    detail = "جست‌وجو و برآورد قیمت",
                    icon = Icons.Rounded.PriceCheck,
                    modifier = Modifier.weight(1f)
                ) { onNavigate("price-inquiry") }

                DashboardAction(
                    title = "افزودن عکس به اتیکت",
                    detail = "ثبت تا ۵ عکس",
                    icon = Icons.Rounded.PhotoCamera,
                    modifier = Modifier.weight(1f)
                ) { onNavigate("camera") }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DashboardAction(
                    title = "اتیکت‌های بدون عکس",
                    detail = "تکمیل آلبوم محصولات",
                    icon = Icons.Rounded.ImageNotSupported,
                    badgeCount = withoutPhotoCount,
                    badgeColor = ZarDanger,
                    modifier = Modifier.weight(1f)
                ) { onNavigate("without-photo") }

                DashboardAction(
                    title = "تنظیمات اتصال",
                    detail = "مدیریت سرورهای نام‌گذاری‌شده",
                    icon = Icons.Rounded.CloudSync,
                    modifier = Modifier.weight(1f)
                ) { onNavigate("connection") }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DashboardAction(
                    title = "همه اتیکت‌ها",
                    detail = "مشاهدهٔ شبکه‌ای عکس و مشخصات",
                    icon = Icons.Rounded.Collections,
                    badgeCount = totalCount,
                    badgeColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                ) { onNavigate("all-etiquettes") }

                DashboardAction(
                    title = "جستجوی اتیکت",
                    detail = "پیدا کردن با نام یا بارکد",
                    icon = Icons.Rounded.Search,
                    modifier = Modifier.weight(1f)
                ) { onNavigate("products") }
            }
        }
    }
}

@Composable
private fun ExpandableSearchBar(
    isExpanded: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onExpandChange: (Boolean) -> Unit,
    onSearchSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val duration = 280
    val easing = FastOutSlowInEasing

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = if (isExpanded) 4.dp else 1.dp,
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 1.dp,
                color = if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isExpanded) 3.dp else 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (!isExpanded) {
                        onExpandChange(true)
                    } else if (query.isNotBlank()) {
                        onSearchSubmit(query)
                    }
                },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "جستجو",
                    tint = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandHorizontally(
                    animationSpec = tween(durationMillis = duration, easing = easing),
                    expandFrom = Alignment.Start
                ) + fadeIn(animationSpec = tween(durationMillis = duration, easing = easing)),
                exit = shrinkHorizontally(
                    animationSpec = tween(durationMillis = duration, easing = easing),
                    shrinkTowards = Alignment.Start
                ) + fadeOut(animationSpec = tween(durationMillis = duration, easing = easing)),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = Doran
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearchSubmit(query) }),
                        decorationBox = { innerTextField ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "جستجوی اتیکت براساس نام، کد یا بارکد…",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Doran),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    )

                    IconButton(
                        onClick = {
                            onQueryChange("")
                            onExpandChange(false)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "بستن جستجو",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !isExpanded,
                enter = fadeIn(animationSpec = tween(durationMillis = duration, easing = easing)),
                exit = fadeOut(animationSpec = tween(durationMillis = duration, easing = easing)),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onExpandChange(true) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "جستجوی اتیکت و محصول…",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Doran),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    product: Product,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.displayName,
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = Doran),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                product.reference?.let {
                    Text(
                        text = "کد: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            product.price?.amountRial?.let { price ->
                if (price > 0) {
                    Text(
                        text = "${NumberFormat.getIntegerInstance(Locale("fa", "IR")).format(price)} ریال",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = Doran),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardAction(
    title: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    badgeCount: Int? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
) {
    val outline = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)

    Card(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 154.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = ZarOrange.copy(alpha = 0.05f))
            .border(1.dp, outline, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            )
                        )
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = Doran),
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Animated Count Badge
            androidx.compose.animation.AnimatedVisibility(
                visible = badgeCount != null && badgeCount > 0,
                enter = scaleIn(animationSpec = tween(280, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(280)),
                exit = scaleOut(animationSpec = tween(280, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(280)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeColor,
                    shadowElevation = 2.dp,
                    tonalElevation = 2.dp,
                ) {
                    Text(
                        text = formatPersianNumber(badgeCount ?: 0),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontFamily = Doran
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun formatPersianNumber(number: Int): String {
    return NumberFormat.getIntegerInstance(Locale("fa", "IR")).format(number)
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
