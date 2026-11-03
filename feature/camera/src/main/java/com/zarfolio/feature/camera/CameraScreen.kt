package com.zarfolio.feature.camera

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zarfolio.core.designsystem.components.BrandTopBar
import com.zarfolio.core.designsystem.components.PageTitle
import com.zarfolio.core.designsystem.components.ZarfolioCard
import java.io.File

@Composable
fun CameraScreen(viewModel: CameraViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var photoFile by remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val file = photoFile
        if (captured && file != null && file.exists() && file.length() > 0) viewModel.upload(file)
        else if (!captured) file?.delete()
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        BrandTopBar(); Spacer(Modifier.height(18.dp)); PageTitle("عکاسی محصول", "محصول را با بارکد انتخاب کنید و عکس اصلی را ثبت کنید")
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::updateQuery,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("بارکد یا نام محصول") },
            )
            Button(onClick = viewModel::searchProducts, enabled = !state.loading, modifier = Modifier.height(56.dp)) {
                if (state.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Rounded.Search, "جست‌وجو")
            }
        }
        state.selectedProduct?.let { selected ->
            Spacer(Modifier.height(12.dp))
            ZarfolioCard(Modifier.fillMaxWidth()) {
                Text("محصول انتخاب‌شده", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(selected.title, style = MaterialTheme.typography.titleMedium)
                Text("بارکد: ${selected.barcode}  •  وزن خالص: ${selected.netWeight} گرم", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        runCatching {
                            val directory = File(context.cacheDir, "product-photos").apply { mkdirs() }
                            val file = File.createTempFile("zarfolio_", ".jpg", directory)
                            photoFile = file
                            val uri = FileProvider.getUriForFile(context, "com.zarfolio.app.fileprovider", file)
                            cameraLauncher.launch(uri)
                        }.onFailure {
                            photoFile?.delete(); photoFile = null
                        }
                    },
                    enabled = !state.uploading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.uploading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Rounded.PhotoCamera, "عکاسی")
                    Spacer(Modifier.width(8.dp)); Text(if (state.uploading) "در حال ذخیرهٔ تصویر" else "گرفتن و ذخیرهٔ عکس")
                }
            }
        }
        state.message?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = if (state.imageUploaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        if (state.products.isNotEmpty()) {
            Spacer(Modifier.height(14.dp)); Text("نتایج جست‌وجو", style = MaterialTheme.typography.titleMedium)
            LazyColumn(Modifier.weight(1f)) {
                items(state.products, key = { it.id }) { product ->
                    ZarfolioCard(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.select(product) }) {
                        Text(product.title, style = MaterialTheme.typography.titleMedium)
                        Text("${product.barcode}  •  ${product.netWeight} گرم", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
            Text("عکس فقط پس از اتصال و ورود ذخیره می‌شود؛ فایل پیش از ارسال در حافظهٔ موقت برنامه قرار می‌گیرد.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
        }
    }
}
