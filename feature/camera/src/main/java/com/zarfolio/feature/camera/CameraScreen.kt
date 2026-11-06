package com.zarfolio.feature.camera

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zarfolio.core.designsystem.components.BrandTopBar
import com.zarfolio.core.designsystem.components.PageTitle
import com.zarfolio.core.designsystem.components.ZarfolioCard
import com.zarfolio.core.network.decodeOrientedPhoto
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(viewModel: CameraViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var photoFile by remember { mutableStateOf<File?>(null) }
    var replaceImageName by remember { mutableStateOf<String?>(null) }
    var cropFile by remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val file = photoFile
        if (captured && file != null && file.exists() && file.length() > 0) cropFile = file
        else { file?.delete(); replaceImageName = null }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) {
            replaceImageName = null
            return@rememberLauncherForActivityResult
        }
        runCatching {
            val directory = File(context.cacheDir, "product-photos").apply { mkdirs() }
            val file = File.createTempFile("zarfolio_gallery_", ".jpg", directory)
            context.contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(file).use { output -> input.copyTo(output) } }
                ?: error("خواندن عکس انتخاب‌شده ممکن نشد")
            check(file.length() > 0L) { "فایل عکس خالی است" }
            cropFile = file
        }.onFailure { replaceImageName = null; viewModel.showMessage(it.message ?: "خواندن عکس ناموفق بود") }
    }

    fun launchCamera(replace: String? = null) {
        replaceImageName = replace
        runCatching {
            val directory = File(context.cacheDir, "product-photos").apply { mkdirs() }
            val file = File.createTempFile("zarfolio_", ".jpg", directory)
            photoFile = file
            cameraLauncher.launch(FileProvider.getUriForFile(context, "com.zarfolio.app.fileprovider", file))
        }.onFailure { photoFile?.delete(); photoFile = null; replaceImageName = null }
    }

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = { viewModel.searchProducts(refresh = true) },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                BrandTopBar()
                Spacer(Modifier.height(4.dp))
                PageTitle("آلبوم عکس اتیکت", "اتیکت را جست‌وجو کنید، عکس‌ها را مدیریت کنید و عکس اصلی را ستاره‌دار کنید")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    OutlinedTextField(value = state.query, onValueChange = viewModel::updateQuery, modifier = Modifier.weight(1f), singleLine = true, label = { Text("بارکد یا نام محصول") })
                    Button(onClick = { viewModel.searchProducts() }, enabled = !state.loading && !state.refreshing, modifier = Modifier.height(56.dp)) {
                        if (state.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Rounded.Search, "جست‌وجو")
                    }
                }
            }
            if (state.recentSearches.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("جست‌وجوهای اخیر", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = viewModel::clearRecentSearches) { Icon(Icons.Rounded.Delete, null, Modifier.size(18.dp)); Text("پاک‌کردن") }
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.recentSearches.forEach { query -> AssistChip(onClick = { viewModel.loadRecentSearch(query) }, label = { Text(query) }) }
                    }
                }
            }
            state.message?.let { message ->
                item { Text(message, color = if (state.imageUploaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            }
            state.selectedProduct?.let { selected ->
                val additionsInQueue = state.pendingPhotos.count { it.replaceName == null }
                val occupiedSlots = selected.images.size + additionsInQueue
                item {
                    ZarfolioCard(Modifier.fillMaxWidth()) {
                        Text("اتیکت انتخاب‌شده", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(selected.title, style = MaterialTheme.typography.titleMedium)
                        Text("بارکد: ${selected.barcode}  •  وزن خالص: ${selected.netWeight} گرم", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("آلبوم عکس (${selected.images.size} از ۵)", style = MaterialTheme.typography.titleSmall)
                        if (selected.images.isEmpty()) Text("هنوز عکسی برای این اتیکت ثبت نشده است.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                            items(selected.images.size, key = { selected.id + ":" + selected.images[it] }) { index ->
                                val name = selected.images[index]
                                val imageKey = "${selected.id}:$name"
                                val bytes = state.imageBytes[imageKey]
                                val bitmap = remember(bytes) { bytes?.let { decodeOrientedPhoto(it) } }
                                DisposableEffect(bitmap) { onDispose { bitmap?.recycle() } }
                                Card(Modifier.width(280.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                    Box(Modifier.fillMaxWidth().height(190.dp)) {
                                        if (bitmap != null) Image(bitmap.asImageBitmap(), contentDescription = "تصویر ${index + 1}", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                                        else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            if (imageKey in state.loadingImages) CircularProgressIndicator(Modifier.size(28.dp)) else Text(state.imageErrors[imageKey] ?: "تصویر بارگیری نشد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        val isPrimary = index == 0
                                        FilledIconButton(onClick = { viewModel.makePrimary(name) }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp), enabled = !state.uploading) {
                                            Icon(if (isPrimary) Icons.Rounded.Star else Icons.Rounded.StarBorder, if (isPrimary) "عکس اصلی" else "انتخاب به‌عنوان عکس اصلی")
                                        }
                                        if (isPrimary) Surface(Modifier.align(Alignment.TopStart).padding(8.dp), color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small) {
                                            Text("عکس اصلی", Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("تصویر ${index + 1}", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                                        IconButton(onClick = { launchCamera(name) }, enabled = !state.uploading) { Icon(Icons.Rounded.Edit, "جایگزینی عکس") }
                                        IconButton(onClick = { viewModel.deleteImage(name) }, enabled = !state.uploading) { Icon(Icons.Rounded.Delete, "حذف عکس", tint = MaterialTheme.colorScheme.error) }
                                    }
                                }
                            }
                        }
                        if (state.pendingPhotos.isNotEmpty()) {
                            Text("عکس‌های آمادهٔ ذخیره", style = MaterialTheme.typography.titleSmall)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                                items(state.pendingPhotos, key = { it.id }) { draft ->
                                    val bitmap = remember(draft.id, draft.file.lastModified()) { decodeOrientedPhoto(draft.file) }
                                    DisposableEffect(bitmap) { onDispose { bitmap?.recycle() } }
                                    Card(Modifier.width(190.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                        Box(Modifier.fillMaxWidth().height(130.dp)) {
                                            bitmap?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                                            IconButton(onClick = { viewModel.removePendingPhoto(draft.id) }, modifier = Modifier.align(Alignment.TopEnd), enabled = !state.uploading) {
                                                Icon(Icons.Rounded.Delete, "حذف از صف", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                        Text(if (draft.replaceName == null) "عکس جدید" else "جایگزینی عکس", Modifier.padding(8.dp), style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                            Text("تا انتخاب دکمهٔ ذخیره، عکس‌ها ارسال نمی‌شوند.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = viewModel::savePendingPhotos, enabled = !state.uploading, modifier = Modifier.fillMaxWidth()) {
                                if (state.uploading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                else Icon(Icons.Rounded.Collections, null)
                                Spacer(Modifier.width(8.dp)); Text(if (state.uploading) "در حال ذخیره…" else "ذخیره عکس‌ها (${state.pendingPhotos.size})")
                            }
                        }
                        if (occupiedSlots < 5) {
                            Spacer(Modifier.height(6.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { launchCamera() }, enabled = !state.uploading, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.AddAPhoto, null); Spacer(Modifier.width(6.dp)); Text("دوربین (${occupiedSlots + 1}/۵)")
                                }
                                OutlinedButton(onClick = { replaceImageName = null; galleryLauncher.launch("image/*") }, enabled = !state.uploading, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.Collections, null); Spacer(Modifier.width(6.dp)); Text("انتخاب از گالری")
                                }
                            }
                        }
                    }
                }
            }
            if (state.products.isNotEmpty()) {
                item { Text("نتایج جست‌وجو", style = MaterialTheme.typography.titleMedium) }
                items(state.products, key = { it.id }) { product ->
                    ZarfolioCard(Modifier.fillMaxWidth().clickable { viewModel.select(product) }) {
                        Text(product.title, style = MaterialTheme.typography.titleMedium)
                        Text("${product.barcode}  •  ${product.netWeight} گرم  •  ${photoCount(product)} عکس", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else if (state.selectedProduct == null) {
                item { Text("جست‌وجوهای عکس در همین دستگاه ذخیره می‌شوند. برای تازه‌سازی صفحه، از بالای فهرست به پایین بکشید.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }

    cropFile?.let { original ->
        ProductPhotoCropDialog(
            file = original,
            onDismiss = { original.delete(); cropFile = null; replaceImageName = null },
            onSave = { cropped ->
                cropFile = null
                viewModel.queuePhoto(cropped, replaceImageName)
                replaceImageName = null
            },
        )
    }
}

@Composable
fun ProductPhotoCropDialog(file: File, onDismiss: () -> Unit, onSave: (File) -> Unit) {
    val context = LocalContext.current
    val original = remember(file) { decodeOrientedPhoto(file) }
    DisposableEffect(original) { onDispose { original?.recycle() } }
    var cropBox by remember(file, original?.width, original?.height) {
        mutableStateOf(original?.let(::initialCropBox) ?: CropBox(.08f, .08f, .92f, .92f))
    }
    var dragMode by remember(file) { mutableStateOf(CropDragMode.None) }
    var dragStart by remember(file) { mutableStateOf(Offset.Zero) }
    var dragBox by remember(file) { mutableStateOf(cropBox) }
    val cropHandleColor = MaterialTheme.colorScheme.primary
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("برش عکس") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 580.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (original != null) {
                    Text("کادر را روی عکس بکشید؛ داخل کادر جابه‌جا و گوشه‌ها را برای تغییر اندازه بکشید.", style = MaterialTheme.typography.bodySmall)
                    Canvas(
                        Modifier.fillMaxWidth().height(340.dp)
                            .pointerInput(original.width, original.height) {
                                detectDragGestures(
                                    onDragStart = { point ->
                                        val bounds = fittedImageBounds(size.width.toFloat(), size.height.toFloat(), original.width, original.height)
                                        val mapped = pointToImage(point, bounds)
                                        dragStart = mapped
                                        dragBox = cropBox
                                        dragMode = when {
                                            nearCropCorner(mapped, cropBox, .055f) != CropDragMode.None -> nearCropCorner(mapped, cropBox, .055f)
                                            cropBox.contains(mapped) -> CropDragMode.Move
                                            else -> CropDragMode.Create
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val bounds = fittedImageBounds(size.width.toFloat(), size.height.toFloat(), original.width, original.height)
                                        val point = pointToImage(change.position, bounds)
                                        val dx = point.x - dragStart.x
                                        val dy = point.y - dragStart.y
                                        cropBox = when (dragMode) {
                                            CropDragMode.Move -> dragBox.translated(dx, dy)
                                            CropDragMode.TopLeft -> dragBox.resize(left = dragBox.left + dx, top = dragBox.top + dy)
                                            CropDragMode.TopRight -> dragBox.resize(right = dragBox.right + dx, top = dragBox.top + dy)
                                            CropDragMode.BottomLeft -> dragBox.resize(left = dragBox.left + dx, bottom = dragBox.bottom + dy)
                                            CropDragMode.BottomRight -> dragBox.resize(right = dragBox.right + dx, bottom = dragBox.bottom + dy)
                                            CropDragMode.Create -> CropBox(dragStart.x, dragStart.y, point.x, point.y).normalized()
                                            CropDragMode.None -> cropBox
                                        }
                                    },
                                    onDragEnd = { dragMode = CropDragMode.None },
                                    onDragCancel = { dragMode = CropDragMode.None },
                                )
                            },
                    ) {
                        val bounds = fittedImageBounds(size.width, size.height, original.width, original.height)
                        drawImage(
                            image = original.asImageBitmap(),
                            dstOffset = androidx.compose.ui.unit.IntOffset(bounds.left.toInt(), bounds.top.toInt()),
                            dstSize = androidx.compose.ui.unit.IntSize(bounds.width.toInt(), bounds.height.toInt()),
                        )
                        val rect = cropBox.toCanvasRect(bounds)
                        val shade = Color.Black.copy(alpha = .56f)
                        drawRect(shade, Offset(bounds.left, bounds.top), Size(bounds.width, (rect.top - bounds.top).coerceAtLeast(0f)))
                        drawRect(shade, Offset(bounds.left, rect.bottom), Size(bounds.width, (bounds.bottom - rect.bottom).coerceAtLeast(0f)))
                        drawRect(shade, Offset(bounds.left, rect.top), Size((rect.left - bounds.left).coerceAtLeast(0f), rect.height))
                        drawRect(shade, Offset(rect.right, rect.top), Size((bounds.right - rect.right).coerceAtLeast(0f), rect.height))
                        drawRect(Color.White, Offset(rect.left, rect.top), Size(rect.width, rect.height), style = Stroke(width = 2.dp.toPx()))
                        listOf(Offset(rect.left, rect.top), Offset(rect.right, rect.top), Offset(rect.left, rect.bottom), Offset(rect.right, rect.bottom)).forEach { corner ->
                            drawCircle(Color.White, radius = 7.dp.toPx(), center = corner)
                            drawCircle(cropHandleColor, radius = 4.dp.toPx(), center = corner)
                        }
                    }
                } else {
                    Text("خواندن عکس ممکن نشد", color = MaterialTheme.colorScheme.error)
                }
                Text("خروجی متناسب با کادر انتخاب‌شده ذخیره می‌شود.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(onClick = {
                val result = runCatching {
                    val bitmap = original ?: error("خواندن عکس ممکن نشد")
                    val left = (cropBox.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
                    val top = (cropBox.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
                    val right = (cropBox.right * bitmap.width).toInt().coerceIn(left + 1, bitmap.width)
                    val bottom = (cropBox.bottom * bitmap.height).toInt().coerceIn(top + 1, bitmap.height)
                    val cropped = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
                    val out = File.createTempFile("zarfolio_cropped_", ".jpg", File(context.cacheDir, "product-photos").apply { mkdirs() })
                    FileOutputStream(out).use { stream -> cropped.compress(Bitmap.CompressFormat.JPEG, 95, stream) }
                    if (cropped !== bitmap) cropped.recycle()
                    file.delete()
                    out
                }
                result.onSuccess(onSave)
            }, enabled = original != null) { Text("افزودن به صف") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

private data class CropBox(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun contains(point: Offset) = point.x in left..right && point.y in top..bottom
    fun normalized(): CropBox {
        val x1 = left.coerceIn(0f, 1f); val x2 = right.coerceIn(0f, 1f)
        val y1 = top.coerceIn(0f, 1f); val y2 = bottom.coerceIn(0f, 1f)
        return CropBox(minOf(x1, x2), minOf(y1, y2), maxOf(x1, x2), maxOf(y1, y2)).withMinimumSize()
    }
    fun withMinimumSize(): CropBox {
        val minSize = .08f
        val l = left.coerceIn(0f, 1f - minSize); val t = top.coerceIn(0f, 1f - minSize)
        return CropBox(l, t, right.coerceIn(l + minSize, 1f), bottom.coerceIn(t + minSize, 1f))
    }
    fun translated(dx: Float, dy: Float) = CropBox(left + dx, top + dy, right + dx, bottom + dy).let {
        val x = dx.coerceIn(-left, 1f - right); val y = dy.coerceIn(-top, 1f - bottom)
        CropBox(left + x, top + y, right + x, bottom + y)
    }
    fun resize(left: Float = this.left, top: Float = this.top, right: Float = this.right, bottom: Float = this.bottom): CropBox {
        val minSize = .08f
        return CropBox(left.coerceIn(0f, right - minSize), top.coerceIn(0f, bottom - minSize), right.coerceIn(left + minSize, 1f), bottom.coerceIn(top + minSize, 1f))
    }
    fun toCanvasRect(bounds: ImageBounds) = ImageBounds(
        bounds.left + left * bounds.width, bounds.top + top * bounds.height,
        bounds.left + right * bounds.width, bounds.top + bottom * bounds.height,
    )
}

private data class ImageBounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = right - left
    val height get() = bottom - top
}

private enum class CropDragMode { None, Move, Create, TopLeft, TopRight, BottomLeft, BottomRight }

private fun initialCropBox(bitmap: Bitmap): CropBox {
    val side = minOf(bitmap.width, bitmap.height) * .84f
    val width = side / bitmap.width; val height = side / bitmap.height
    return CropBox((1f - width) / 2f, (1f - height) / 2f, (1f + width) / 2f, (1f + height) / 2f)
}

private fun fittedImageBounds(canvasWidth: Float, canvasHeight: Float, imageWidth: Int, imageHeight: Int): ImageBounds {
    val scale = minOf(canvasWidth / imageWidth, canvasHeight / imageHeight)
    val width = imageWidth * scale; val height = imageHeight * scale
    val left = (canvasWidth - width) / 2f; val top = (canvasHeight - height) / 2f
    return ImageBounds(left, top, left + width, top + height)
}

private fun pointToImage(point: Offset, bounds: ImageBounds) = Offset(
    ((point.x - bounds.left) / bounds.width).coerceIn(0f, 1f),
    ((point.y - bounds.top) / bounds.height).coerceIn(0f, 1f),
)

private fun nearCropCorner(point: Offset, box: CropBox, threshold: Float): CropDragMode = listOf(
    CropDragMode.TopLeft to Offset(box.left, box.top), CropDragMode.TopRight to Offset(box.right, box.top),
    CropDragMode.BottomLeft to Offset(box.left, box.bottom), CropDragMode.BottomRight to Offset(box.right, box.bottom),
).firstOrNull { (_, corner) -> (point - corner).getDistance() <= threshold }?.first ?: CropDragMode.None

private fun photoCount(product: com.zarfolio.core.network.EtiquetteDto): Int =
    (product.images.ifEmpty { listOfNotNull(product.image) }).distinct().size
