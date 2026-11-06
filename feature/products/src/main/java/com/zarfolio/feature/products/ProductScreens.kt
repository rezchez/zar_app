package com.zarfolio.feature.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zarfolio.core.designsystem.components.*
import com.zarfolio.core.model.Product
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale
import java.text.NumberFormat
import androidx.compose.ui.Alignment
import com.zarfolio.core.network.decodeOrientedPhoto
import com.zarfolio.feature.camera.ProductPhotoCropDialog

@Composable
fun ProductSearchScreen(viewModel: ProductSearchViewModel = hiltViewModel()) {
    var query by remember { mutableStateOf("") }
    val message by viewModel.message.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        BrandTopBar(); Spacer(Modifier.height(18.dp)); PageTitle("جستجوی اتیکت", "جست‌وجو با نام، بارکد یا کد کالا")
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("نام یا شناسهٔ محصول") }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search))
            Button(onClick = { viewModel.search(query) }, enabled = !loading, modifier = Modifier.height(56.dp)) { Icon(Icons.Rounded.Search, "جستجو") }
        }
        if (recentSearches.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("جست‌وجوهای اخیر", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = viewModel::clearRecentSearches) { Icon(Icons.Rounded.Delete, "پاک کردن تاریخچهٔ جست‌وجو") }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(recentSearches, key = { it }) { recent -> AssistChip(onClick = { query = recent; viewModel.searchRecent(recent) }, label = { Text(recent) }) }
            }
        }
        if (loading) { Spacer(Modifier.height(20.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (products.isEmpty()) ZarfolioEmptyState("محصولی نمایش داده نمی‌شود", message ?: "نام یا بارکد اتیکت را جست‌وجو کنید.", Modifier.weight(1f))
        else LazyColumn(Modifier.weight(1f)) {
            items(products, key = { it.id }) { product ->
                ZarfolioCard(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(product.displayName, style = MaterialTheme.typography.titleMedium)
                            product.reference?.let { Text("بارکد / کد: $it", style = MaterialTheme.typography.bodyMedium) }
                            Text("وزن ${product.totalWeight ?: 0.0} گرم · عیار ${product.purity ?: 750} · قیمت ${formatRial(product.price?.amountRial ?: 0)} ریال", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { editingProduct = product }) { Icon(Icons.Rounded.Edit, "ویرایش مشخصات اتیکت") }
                    }
                }
            }
        }
    }
    editingProduct?.let { product -> ProductEditDialog(product, onDismiss = { editingProduct = null }, onSave = { viewModel.updateProduct(it); editingProduct = null }) }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RecentProductsScreen(onlyWithoutPhoto: Boolean = false, viewModel: ProductSearchViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val message by viewModel.message.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val photoBytes by viewModel.photoBytes.collectAsStateWithLifecycle()
    val loadingPhotos by viewModel.loadingPhotos.collectAsStateWithLifecycle()
    val photoErrors by viewModel.photoErrors.collectAsStateWithLifecycle()
    val visibleProducts = if (onlyWithoutPhoto) products.filter { it.images.isEmpty() } else products
    var pendingReplace by remember { mutableStateOf<Pair<String, String?>?>(null) }
    var photoFile by remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val file = photoFile
        if (!captured || file == null || !file.exists() || file.length() == 0L || pendingReplace == null) {
            file?.delete()
            photoFile = null
            pendingReplace = null
        }
    }
    fun capture(productId: String, imageName: String?) {
        runCatching {
            val directory = File(context.cacheDir, "product-photos").apply { mkdirs() }
            val file = File.createTempFile("zarfolio_edit_", ".jpg", directory)
            photoFile = file
            pendingReplace = productId to imageName
            cameraLauncher.launch(FileProvider.getUriForFile(context, "com.zarfolio.app.fileprovider", file))
        }.onFailure { photoFile?.delete(); photoFile = null; pendingReplace = null }
    }
    LaunchedEffect(Unit) { viewModel.loadLatest() }
    PullToRefreshBox(isRefreshing = loading, onRefresh = viewModel::loadLatest, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                BrandTopBar(); Spacer(Modifier.height(18.dp)); PageTitle(if (onlyWithoutPhoto) "اتیکت‌های بدون عکس" else "آخرین‌ها", "مشاهدهٔ مشخصات و مدیریت آلبوم حداکثر ۵ عکس")
                message?.let { Text(it, modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.primary) }
            }
            if (visibleProducts.isEmpty()) item {
                ZarfolioEmptyState(
                    if (onlyWithoutPhoto) "همهٔ اتیکت‌ها عکس دارند" else "هنوز داده‌ای دریافت نشده",
                    message ?: "فهرست اتیکت‌های سرور اینجا نمایش داده می‌شود.",
                )
            }
            items(visibleProducts, key = { it.id }) { p ->
                ZarfolioCard(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Text(p.displayName, style = MaterialTheme.typography.titleMedium)
                    p.reference?.let { Text("بارکد / کد کالا: $it", style = MaterialTheme.typography.bodyMedium) }
                    Text("وزن کل: ${p.totalWeight ?: 0.0} گرم  •  وزن خالص: ${p.netWeight ?: 0.0} گرم", style = MaterialTheme.typography.bodyMedium)
                    Text("قیمت: ${p.price?.amountRial ?: 0} ریال  •  وضعیت: ${p.status ?: "نامشخص"}", style = MaterialTheme.typography.bodyMedium)
                    Text("تصویرها: ${p.images.size} از ۵", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    p.images.forEachIndexed { index, image ->
                        val imageName = image.description ?: android.net.Uri.parse(image.uri).getQueryParameter("name")
                        val imageKey = imageName?.let { "${p.id}:$it" }
                        LaunchedEffect(p.id, image.uri, imageName) { viewModel.loadPhoto(p.id, image) }
                        val bytes = imageKey?.let(photoBytes::get)
                        val bitmap = remember(bytes) { bytes?.let { decodeOrientedPhoto(it) } }
                        DisposableEffect(bitmap) { onDispose { bitmap?.recycle() } }
                        if (bitmap != null) {
                            Image(bitmap.asImageBitmap(), "تصویر ${index + 1} از ${p.displayName}", Modifier.fillMaxWidth().height(190.dp), contentScale = ContentScale.Fit)
                        } else {
                            Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
                                val key = imageKey
                                when {
                                    key != null && key in loadingPhotos -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                    key != null && photoErrors[key] != null -> Text(photoErrors.getValue(key), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                    else -> Text("تصویر در دسترس نیست", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text("تصویر ${index + 1}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { capture(p.id, image.description) }) { Icon(Icons.Rounded.PhotoCamera, null); Text("جایگزینی") }
                            IconButton(onClick = { image.description?.let { viewModel.deletePhoto(p.id, it) } }, enabled = image.description != null) { Icon(Icons.Rounded.Delete, "حذف تصویر", tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                    Button(onClick = { capture(p.id, null) }, enabled = p.images.size < 5, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.PhotoCamera, null); Spacer(Modifier.width(6.dp)); Text(if (p.images.size < 5) "افزودن تصویر" else "سقف ۵ تصویر تکمیل است")
                    }
                }
            }
        }
    }
    photoFile?.takeIf { it.exists() && it.length() > 0L }?.let { file ->
        ProductPhotoCropDialog(
            file = file,
            onDismiss = { file.delete(); photoFile = null; pendingReplace = null },
            onSave = { cropped ->
                val target = pendingReplace
                photoFile = null
                pendingReplace = null
                if (target != null) viewModel.replacePhoto(target.first, cropped, target.second) else cropped.delete()
            },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AllEtiquettesScreen(viewModel: ProductSearchViewModel = hiltViewModel()) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val photoBytes by viewModel.photoBytes.collectAsStateWithLifecycle()
    val loadingPhotos by viewModel.loadingPhotos.collectAsStateWithLifecycle()
    val photoErrors by viewModel.photoErrors.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadAll() }

    PullToRefreshBox(isRefreshing = loading, onRefresh = viewModel::loadAll, modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    BrandTopBar()
                    Spacer(Modifier.height(8.dp))
                    PageTitle("همه اتیکت‌ها", "فهرست محصولات همراه با عکس شاخص و مشخصات")
                    message?.let { Text(it, Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            }
            if (products.isEmpty() && !loading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ZarfolioEmptyState("اتیکتی برای نمایش نیست", message ?: "فهرست اتیکت‌ها از سرور دریافت نشد.")
                }
            }
            gridItems(products, key = { it.id }) { product ->
                val primaryImage = product.images.firstOrNull()
                val imageName = primaryImage?.description ?: primaryImage?.uri?.let { android.net.Uri.parse(it).getQueryParameter("name") }
                val imageKey = imageName?.let { "${product.id}:$it" }
                LaunchedEffect(product.id, imageName) {
                    if (primaryImage != null) viewModel.loadPhoto(product.id, primaryImage)
                }
                val bytes = imageKey?.let(photoBytes::get)
                val bitmap = remember(bytes) { bytes?.let(::decodeOrientedPhoto) }
                DisposableEffect(bitmap) { onDispose { bitmap?.recycle() } }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(Modifier.fillMaxWidth().padding(9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                            when {
                                bitmap != null -> Image(bitmap.asImageBitmap(), "عکس شاخص ${product.displayName}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                imageKey == null -> Text("بدون عکس", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                                imageKey != null && imageKey in loadingPhotos -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                imageKey?.let { photoErrors[it] } != null -> Text("عکس در دسترس نیست", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                                else -> Text("عکس در دسترس نیست", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                            }
                            Surface(Modifier.align(Alignment.BottomStart).padding(5.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .9f), shape = MaterialTheme.shapes.small) {
                                Text("${product.images.size} عکس", Modifier.padding(horizontal = 6.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Text(product.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 2, minLines = 2)
                        Text("کد: ${product.barcode ?: product.itemCode ?: "—"}", style = MaterialTheme.typography.bodySmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("وزن خالص ${product.netWeight ?: 0.0} گرم", style = MaterialTheme.typography.bodySmall)
                        Text("عیار ${product.purity ?: 750} · ${product.category ?: "بدون دسته"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${formatRial(product.price?.amountRial ?: 0)} ریال", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                        product.description?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductEditDialog(product: Product, onDismiss: () -> Unit, onSave: (Product) -> Unit) {
    var title by remember(product.id) { mutableStateOf(product.displayName) }
    var barcode by remember(product.id) { mutableStateOf(product.barcode.orEmpty()) }
    var itemCode by remember(product.id) { mutableStateOf(product.itemCode.orEmpty()) }
    var weight by remember(product.id) { mutableStateOf(product.totalWeight?.toString().orEmpty()) }
    var purity by remember(product.id) { mutableStateOf(product.purity?.toString() ?: "750") }
    var wage by remember(product.id) { mutableStateOf(product.wage?.toString() ?: "0") }
    var wageType by remember(product.id) { mutableStateOf(product.wageType ?: "percentage") }
    var notes by remember(product.id) { mutableStateOf(product.description.orEmpty()) }
    var category by remember(product.id) { mutableStateOf(product.category ?: "plain") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ویرایش مشخصات اتیکت") },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("نام محصول") }, singleLine = true)
                OutlinedTextField(barcode, { barcode = it }, label = { Text("بارکد") }, singleLine = true)
                OutlinedTextField(itemCode, { itemCode = it }, label = { Text("کد کالا") }, singleLine = true)
                OutlinedTextField(weight, { weight = it }, label = { Text("وزن کل (گرم)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(purity, { purity = it }, label = { Text("عیار") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(wage, { wage = it }, label = { Text(if (wageType == "percentage") "اجرت (درصد)" else "اجرت (ریال)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                Text("نوع اجرت", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = wageType == "percentage", onClick = { wageType = "percentage" }, label = { Text("درصدی") })
                    FilterChip(selected = wageType == "per_gram", onClick = { wageType = "per_gram" }, label = { Text("هر گرم") })
                    FilterChip(selected = wageType == "fixed", onClick = { wageType = "fixed" }, label = { Text("تومانی") })
                }
                Text("دسته‌بندی اتیکت", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("plain" to "ساده", "gemstone" to "سنگ‌دار", "jewelry" to "جواهر", "base" to "پایه").forEach { (code, label) ->
                        FilterChip(selected = category == code, onClick = { category = code }, label = { Text(label) })
                    }
                }
                OutlinedTextField(notes, { notes = it }, label = { Text("یادداشت") }, minLines = 2)
            }
        },
        confirmButton = { TextButton(onClick = {
            onSave(product.copy(displayName = title.trim(), barcode = barcode.trim(), itemCode = itemCode.trim(), category = category, reference = listOf(barcode.trim(), itemCode.trim()).filter(String::isNotBlank).joinToString(" · "), totalWeight = weight.toDoubleOrNull() ?: product.totalWeight, purity = purity.toIntOrNull() ?: 750, wage = wage.toDoubleOrNull() ?: 0.0, wageType = wageType, description = notes))
        }, enabled = title.isNotBlank()) { Text("ذخیره") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

@Composable
fun PriceInquiryScreen(viewModel: ProductSearchViewModel = hiltViewModel()) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val spotPrice by viewModel.liveGoldPrice.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("همه") }
    var minWeight by remember { mutableStateOf("") }
    var maxWeight by remember { mutableStateOf("") }
    var karat by remember { mutableStateOf("همه") }
    var wageFilter by remember { mutableStateOf("همه") }
    LaunchedEffect(Unit) { viewModel.loadLatest(); viewModel.refreshLiveGoldPrice() }
    val categoryOptions = listOf("همه", "انگشتر", "دستبند", "النگو", "زنجیر", "پلاک", "طلای آبشده")
    val karatOptions = listOf("همه", "750", "740", "705", "875", "900", "995")
    val filtered = products.filter { p ->
        val w = p.netWeight ?: p.totalWeight ?: 0.0
        (query.isBlank() || p.displayName.contains(query, true) || p.reference.orEmpty().contains(query, true)) &&
            (category == "همه" || p.displayName.contains(category, true) || p.category.orEmpty().contains(category, true)) &&
            (minWeight.toDoubleOrNull()?.let { w >= it } != false) &&
            (maxWeight.toDoubleOrNull()?.let { w <= it } != false) &&
            (karat == "همه" || p.purity?.toString() == karat) &&
            (wageFilter == "همه" || if (wageFilter == "amount") p.wageType == "per_gram" || p.wageType == "fixed" else p.wageType == wageFilter)
    }
    Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
        BrandTopBar(); Spacer(Modifier.height(16.dp)); PageTitle("استعلام قیمت", "فیلتر اتیکت‌ها و پیش‌نمایش قیمت مشتری")
        Spacer(Modifier.height(10.dp))
        ZarfolioCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("قیمت زندهٔ هر گرم ۷۵۰", style = MaterialTheme.typography.titleSmall); Text(spotPrice?.let { "${formatRial(it)} ریال" } ?: "قیمت از سرور دریافت نشده", color = if (spotPrice == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
                IconButton(onClick = viewModel::refreshLiveGoldPrice) { Icon(Icons.Rounded.Refresh, "به‌روزرسانی قیمت") }
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text("نام، بارکد یا کد کالا") }, singleLine = true)
        Text("دسته‌بندی", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(categoryOptions) { option -> FilterChip(category == option, { category = option }, label = { Text(option) }) } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(minWeight, { minWeight = it }, modifier = Modifier.weight(1f), label = { Text("حداقل وزن") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            OutlinedTextField(maxWeight, { maxWeight = it }, modifier = Modifier.weight(1f), label = { Text("حداکثر وزن") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
        }
        Text("عیار", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(karatOptions) { option -> FilterChip(karat == option, { karat = option }, label = { Text(option) }) } }
        Text("نوع اجرت", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf("همه", "درصدی", "تومانی")) { option ->
                val code = when (option) { "درصدی" -> "percentage"; "تومانی" -> "amount"; else -> "همه" }
                FilterChip(wageFilter == code, { wageFilter = code }, label = { Text(option) })
            }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Text("${filtered.size} اتیکت", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 8.dp))
        filtered.forEach { product ->
            val estimate = estimateRetail(product, spotPrice)
            ZarfolioCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(product.displayName, style = MaterialTheme.typography.titleMedium)
                Text("${product.reference ?: "بدون بارکد"} · ${product.netWeight ?: product.totalWeight ?: 0.0} گرم · عیار ${product.purity ?: 750}", style = MaterialTheme.typography.bodySmall)
                Text("برآورد فروش: ${formatRial(estimate)} ریال", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun estimateRetail(product: Product, spotPrice: Long?): Long {
    val net = product.netWeight ?: product.totalWeight ?: 0.0
    val raw = if (spotPrice == null) 0.0 else net * (product.purity ?: 750) / 750.0 * spotPrice
    val wage = when (product.wageType) { "percentage" -> raw * (product.wage ?: 0.0) / 100.0; "per_gram" -> net * (product.wage ?: 0.0); "fixed" -> product.wage ?: 0.0; else -> 0.0 }
    val base = raw + wage
    return (base + base * (product.profitPercentage ?: 0.0) / 100.0 + (product.stonePrice ?: 0L) + (product.otherCosts ?: 0L)).toLong().coerceAtLeast(0L)
}

private fun formatRial(value: Long): String = NumberFormat.getIntegerInstance(Locale("fa", "IR")).format(value)
