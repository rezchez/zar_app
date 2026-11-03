package com.zarfolio.feature.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Delete
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zarfolio.core.designsystem.components.*
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun ProductSearchScreen(viewModel: ProductSearchViewModel = hiltViewModel()) {
    var query by remember { mutableStateOf("") }
    val message by viewModel.message.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        BrandTopBar(); Spacer(Modifier.height(18.dp)); PageTitle("جستجوی محصولات", "جست‌وجو از سرور کسب‌وکار")
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("نام یا شناسهٔ محصول") }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search))
            Button(onClick = { viewModel.search(query) }, enabled = !loading, modifier = Modifier.height(56.dp)) { Icon(Icons.Rounded.Search, "جستجو") }
        }
        if (loading) { Spacer(Modifier.height(20.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (products.isEmpty()) ZarfolioEmptyState("محصولی نمایش داده نمی‌شود", message ?: "با مشخص شدن قرارداد API محصولات، نتایج جست‌وجو اینجا نمایش داده می‌شوند.", Modifier.weight(1f))
        else LazyColumn(Modifier.weight(1f)) { items(products, key = { it.id }) { product -> ZarfolioCard(Modifier.fillMaxWidth().padding(vertical = 5.dp)) { Text(product.displayName, style = MaterialTheme.typography.titleMedium); product.reference?.let { Text(it, style = MaterialTheme.typography.bodyMedium) } } } }
    }
}

@Composable
fun RecentProductsScreen(viewModel: ProductSearchViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val message by viewModel.message.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    var pendingReplace by remember { mutableStateOf<Pair<String, String?>?>(null) }
    var photoFile by remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val file = photoFile
        val target = pendingReplace
        if (captured && file != null && file.exists() && file.length() > 0 && target != null) {
            viewModel.replacePhoto(target.first, file, target.second)
        } else file?.delete()
        photoFile = null
        pendingReplace = null
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
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        BrandTopBar(); Spacer(Modifier.height(18.dp)); PageTitle("آخرین‌ها", "مشخصات اتیکت و مدیریت حداکثر ۵ تصویر هر محصول")
        message?.let { Text(it, modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.primary) }
        if (products.isEmpty()) ZarfolioEmptyState("هنوز داده‌ای دریافت نشده", message ?: "پس از مشخص شدن endpoint رسمی، فهرست آخرین محصولات نمایش داده می‌شود.", Modifier.weight(1f))
        else LazyColumn(Modifier.weight(1f)) {
            items(products, key = { it.id }) { p ->
                ZarfolioCard(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Text(p.displayName, style = MaterialTheme.typography.titleMedium)
                    p.reference?.let { Text("بارکد / کد کالا: $it", style = MaterialTheme.typography.bodyMedium) }
                    Text("وزن کل: ${p.totalWeight ?: 0.0} گرم  •  وزن خالص: ${p.netWeight ?: 0.0} گرم", style = MaterialTheme.typography.bodyMedium)
                    Text("قیمت: ${p.price?.amountRial ?: 0} ریال  •  وضعیت: ${p.status ?: "نامشخص"}", style = MaterialTheme.typography.bodyMedium)
                    Text("تصویرها: ${p.images.size} از ۵", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    p.images.forEachIndexed { index, image ->
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
}
