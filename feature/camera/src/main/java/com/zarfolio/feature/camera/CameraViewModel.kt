package com.zarfolio.feature.camera

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zarfolio.core.network.ApiClient
import com.zarfolio.core.network.EtiquetteDto
import com.zarfolio.core.network.PrimaryEtiquetteImageRequest
import com.zarfolio.core.network.decodeOrientedPhoto
import com.zarfolio.core.network.encodePhotoPreview
import com.zarfolio.core.network.ServerConnectionRepository
import com.zarfolio.core.network.ZarfolioApi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.UUID

data class PendingProductPhoto(
    val id: String = UUID.randomUUID().toString(),
    val file: File,
    val replaceName: String? = null,
)

data class CameraUiState(
    val query: String = "",
    val products: List<EtiquetteDto> = emptyList(),
    val selectedProduct: EtiquetteDto? = null,
    val recentSearches: List<String> = emptyList(),
    val imageBytes: Map<String, ByteArray> = emptyMap(),
    val imageErrors: Map<String, String> = emptyMap(),
    val loadingImages: Set<String> = emptySet(),
    val pendingPhotos: List<PendingProductPhoto> = emptyList(),
    val loading: Boolean = false,
    val uploading: Boolean = false,
    val refreshing: Boolean = false,
    val message: String? = null,
    val imageUploaded: Boolean = false,
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val servers: ServerConnectionRepository,
    private val apiClient: ApiClient,
    private val photoCapture: ProductPhotoCapture,
    @ApplicationContext context: Context,
) : ViewModel() {
    private val preferences = context.getSharedPreferences("zarfolio_camera_search", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(CameraUiState(recentSearches = readRecentSearches()))
    val state = _state.asStateFlow()

    fun updateQuery(query: String) { _state.value = _state.value.copy(query = query, message = null) }

    fun searchProducts(queryOverride: String? = null, refresh: Boolean = false) {
        val query = (queryOverride ?: _state.value.query).trim()
        if (query.isBlank()) {
            _state.value = _state.value.copy(message = "بارکد یا نام محصول را وارد کنید")
            return
        }
        rememberSearch(query)
        viewModelScope.launch {
            val previousSelected = _state.value.selectedProduct.takeIf { refresh }
            if (!refresh) discardPendingPhotos()
            _state.value = if (refresh) {
                _state.value.copy(loading = false, refreshing = true, imageUploaded = false)
            } else {
                _state.value.copy(loading = true, refreshing = false, message = null, selectedProduct = null, imageBytes = emptyMap(), imageErrors = emptyMap(), loadingImages = emptySet(), imageUploaded = false)
            }
            runCatching {
                val response = activeApi().searchEtiquettes(query = query, status = "available")
                if (!response.isSuccessful) error(if (response.code() == 401) "ابتدا وارد حساب شوید" else "جست‌وجوی محصول انجام نشد (${response.code()})")
                response.body()?.items.orEmpty()
            }.onSuccess { items ->
                val refreshedSelected = previousSelected?.let { old -> items.firstOrNull { it.id == old.id }?.let { fresh -> fresh.copy(images = photoNames(fresh)) } }
                val visibleKeys = refreshedSelected?.let { selected -> selected.images.map { imageKey(selected.id, it) }.toSet() }.orEmpty()
                _state.value = _state.value.copy(
                    loading = false,
                    refreshing = false,
                    products = items,
                    selectedProduct = refreshedSelected,
                    imageBytes = _state.value.imageBytes.filterKeys { it in visibleKeys },
                    imageErrors = _state.value.imageErrors.filterKeys { it in visibleKeys },
                    loadingImages = _state.value.loadingImages.filter { it in visibleKeys }.toSet(),
                    message = if (items.isEmpty()) "محصولی با این مشخصات پیدا نشد" else _state.value.message,
                )
                refreshedSelected?.images?.forEach { loadImage(refreshedSelected.id, it) }
            }.onFailure { error -> _state.value = _state.value.copy(loading = false, refreshing = false, message = error.message ?: "ارتباط با سرور برقرار نشد") }
        }
    }

    fun select(product: EtiquetteDto) {
        discardPendingPhotos()
        val selected = product.copy(images = photoNames(product))
        _state.value = _state.value.copy(selectedProduct = selected, message = null, imageUploaded = false, imageBytes = emptyMap(), imageErrors = emptyMap(), loadingImages = emptySet())
        selected.images.forEach { loadImage(selected.id, it) }
    }

    fun loadRecentSearch(query: String) {
        updateQuery(query)
        searchProducts(query)
    }

    fun clearRecentSearches() {
        preferences.edit().remove(RECENT_KEY).apply()
        _state.value = _state.value.copy(recentSearches = emptyList())
    }

    fun showMessage(message: String) {
        _state.value = _state.value.copy(message = message, imageUploaded = false)
    }

    fun queuePhoto(photo: File, replaceName: String? = null) {
        val product = _state.value.selectedProduct ?: run {
            _state.value = _state.value.copy(message = "ابتدا محصول را انتخاب کنید")
            photo.delete(); return
        }
        val pending = _state.value.pendingPhotos
        if (replaceName == null && product.images.size + pending.count { it.replaceName == null } >= 5) {
            photo.delete()
            _state.value = _state.value.copy(message = "هر اتیکت حداکثر می‌تواند ۵ عکس داشته باشد")
            return
        }
        if (replaceName != null && replaceName !in product.images) {
            photo.delete()
            _state.value = _state.value.copy(message = "عکس انتخاب‌شده برای جایگزینی دیگر موجود نیست")
            return
        }
        val remaining = if (replaceName == null) pending else pending.filterNot { draft ->
            if (draft.replaceName == replaceName) { draft.file.delete(); true } else false
        }
        _state.value = _state.value.copy(
            pendingPhotos = remaining + PendingProductPhoto(file = photo, replaceName = replaceName),
            message = null,
            imageUploaded = false,
        )
    }

    fun removePendingPhoto(photoId: String) {
        val photo = _state.value.pendingPhotos.firstOrNull { it.id == photoId } ?: return
        photo.file.delete()
        _state.value = _state.value.copy(pendingPhotos = _state.value.pendingPhotos - photo, message = null)
    }

    fun savePendingPhotos() {
        val product = _state.value.selectedProduct ?: return
        if (_state.value.uploading || _state.value.pendingPhotos.isEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(uploading = true, message = null, imageUploaded = false)
            val queue = _state.value.pendingPhotos.toList()
            var uploaded = 0
            var failure: Throwable? = null
            for (draft in queue) {
                val result = photoCapture.upload(product.id, draft.file, draft.replaceName)
                val error = result.exceptionOrNull()
                if (error != null) {
                    failure = error
                    break
                }
                draft.file.delete()
                uploaded++
                _state.value = _state.value.copy(pendingPhotos = _state.value.pendingPhotos.filterNot { it.id == draft.id })
            }
            val remaining = _state.value.pendingPhotos.size
            val success = failure == null && remaining == 0
            val message = when {
                success -> "$uploaded عکس برای ${product.barcode} ذخیره شد"
                uploaded > 0 -> "$uploaded عکس ذخیره شد؛ ارسال عکس بعدی ناموفق بود: ${failure?.message ?: "خطای نامشخص"}"
                else -> failure?.message ?: "ذخیرهٔ عکس‌ها ناموفق بود"
            }
            _state.value = _state.value.copy(uploading = false, imageUploaded = success, message = message)
            if (uploaded > 0) refreshSelected()
        }
    }

    private fun discardPendingPhotos() {
        _state.value.pendingPhotos.forEach { it.file.delete() }
        _state.value = _state.value.copy(pendingPhotos = emptyList())
    }

    fun makePrimary(imageName: String) {
        val product = _state.value.selectedProduct ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(uploading = true, message = null)
            runCatching {
                val response = activeApi().setPrimaryEtiquetteImage(product.id, PrimaryEtiquetteImageRequest(imageName))
                if (!response.isSuccessful) error(if (response.code() == 403) "مجوز تغییر عکس اصلی را ندارید" else "تغییر عکس اصلی ناموفق بود (${response.code()})")
                response.body()?.item ?: error("سرور تغییر عکس اصلی را تأیید نکرد")
            }.onSuccess { updated ->
                val refreshed = updated.copy(images = photoNames(updated))
                _state.value = _state.value.copy(selectedProduct = refreshed, products = _state.value.products.map { if (it.id == refreshed.id) refreshed else it }, uploading = false, message = "عکس اصلی و تصویر بندانگشتی به‌روز شد")
            }.onFailure { _state.value = _state.value.copy(uploading = false, message = it.message ?: "تغییر عکس اصلی ناموفق بود") }
        }
    }

    fun deleteImage(imageName: String) {
        val product = _state.value.selectedProduct ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(uploading = true, message = null)
            runCatching {
                val response = activeApi().deleteEtiquetteImage(product.id, imageName)
                if (!response.isSuccessful) error("حذف عکس ناموفق بود (${response.code()})")
                response.body()?.item ?: error("سرور حذف عکس را تأیید نکرد")
            }.onSuccess { updated ->
                val refreshed = updated.copy(images = photoNames(updated))
                _state.value = _state.value.copy(selectedProduct = refreshed, products = _state.value.products.map { if (it.id == refreshed.id) refreshed else it }, imageBytes = _state.value.imageBytes - imageKey(product.id, imageName), uploading = false, message = "عکس حذف شد")
                refreshed.images.forEach { loadImage(refreshed.id, it) }
            }.onFailure { _state.value = _state.value.copy(uploading = false, message = it.message ?: "حذف عکس ناموفق بود") }
        }
    }

    private fun loadImage(productId: String, imageName: String) {
        val key = imageKey(productId, imageName)
        if (key in _state.value.imageBytes || key in _state.value.loadingImages) return
        _state.value = _state.value.copy(loadingImages = _state.value.loadingImages + key, imageErrors = _state.value.imageErrors - key)
        viewModelScope.launch {
            val result = runCatching {
                val response = activeApi().getEtiquetteImage(productId, imageName)
                if (!response.isSuccessful) error("دریافت تصویر ناموفق بود (${response.code()})")
                response.body()?.use { body ->
                    body.bytes().also { bytes ->
                        if (bytes.isEmpty()) error("فایل تصویر خالی است")
                    }
                } ?: error("پاسخ تصویر خالی است")
            }
            val rawBytes = result.getOrNull()
            val previewBytes = rawBytes?.let { runCatching {
                val bitmap = decodeOrientedPhoto(it) ?: error("فایل تصویر معتبر نیست")
                try { encodePhotoPreview(bitmap) } finally { bitmap.recycle() }
            }.getOrNull() }
            val stillSelected = _state.value.selectedProduct?.id == productId
            _state.value = _state.value.copy(
                imageBytes = if (previewBytes == null || !stillSelected) _state.value.imageBytes else _state.value.imageBytes + (key to previewBytes),
                imageErrors = if (previewBytes == null) _state.value.imageErrors + (key to (result.exceptionOrNull()?.message ?: "بارگذاری تصویر ناموفق بود")) else _state.value.imageErrors - key,
                loadingImages = _state.value.loadingImages - key,
            )
        }
    }

    private fun refreshSelected() {
        val query = _state.value.query
        searchProducts(query, refresh = true)
    }

    private suspend fun activeApi(): ZarfolioApi {
        val server = servers.active() ?: error("ابتدا نشانی سرور را تنظیم کنید")
        return apiClient.retrofitFor(server).create(ZarfolioApi::class.java)
    }

    private fun rememberSearch(raw: String) {
        val text = raw.trim()
        if (text.isBlank()) return
        val next = (listOf(text) + _state.value.recentSearches.filterNot { it.equals(text, true) }).take(10)
        _state.value = _state.value.copy(query = text, recentSearches = next)
        preferences.edit().putString(RECENT_KEY, JSONArray(next).toString()).apply()
    }

    private fun readRecentSearches(): List<String> = runCatching {
        val array = preferences.getString(RECENT_KEY, null)?.let(::JSONArray) ?: return emptyList()
        (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }.take(10)
    }.getOrDefault(emptyList())

    private fun photoNames(product: EtiquetteDto): List<String> =
        (product.images.ifEmpty { product.imageUrls.ifEmpty { listOfNotNull(product.image) } })
            .mapNotNull(::photoName)
            .distinct()
            .take(5)

    private fun photoName(value: String): String? {
        val candidate = value.trim().takeIf(String::isNotEmpty) ?: return null
        val uri = android.net.Uri.parse(candidate)
        uri.getQueryParameter("name")?.let { return it.takeIf(String::isNotBlank) }
        if (candidate.startsWith("/")) return null
        return candidate.substringAfterLast('/').substringBefore('?').takeIf(String::isNotBlank)
    }

    private fun imageKey(productId: String, imageName: String) = "$productId:$imageName"

    private companion object { const val RECENT_KEY = "recent_queries" }
}
