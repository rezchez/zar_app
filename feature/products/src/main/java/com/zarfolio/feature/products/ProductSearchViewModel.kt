package com.zarfolio.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import org.json.JSONArray
import dagger.hilt.android.qualifiers.ApplicationContext
import com.zarfolio.core.common.ApiResult
import com.zarfolio.core.model.Product
import com.zarfolio.core.model.ProductSearchQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import com.zarfolio.core.model.ProductImage

@HiltViewModel
class ProductSearchViewModel @Inject constructor(
    private val repository: ProductRepository,
    @ApplicationContext context: Context,
) : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products = _products.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()
    private val preferences = context.getSharedPreferences("zarfolio_product_search", Context.MODE_PRIVATE)
    private val _recentSearches = MutableStateFlow(readRecentSearches())
    val recentSearches = _recentSearches.asStateFlow()
    private val _liveGoldPrice = MutableStateFlow<Long?>(null)
    val liveGoldPrice = _liveGoldPrice.asStateFlow()
    private val _photoBytes = MutableStateFlow<Map<String, ByteArray>>(emptyMap())
    val photoBytes = _photoBytes.asStateFlow()
    private val _loadingPhotos = MutableStateFlow<Set<String>>(emptySet())
    val loadingPhotos = _loadingPhotos.asStateFlow()
    private val _photoErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val photoErrors = _photoErrors.asStateFlow()

    fun loadPhoto(productId: String, image: ProductImage) {
        val name = image.description ?: android.net.Uri.parse(image.uri).getQueryParameter("name") ?: return
        val key = photoKey(productId, name)
        if (key in _photoBytes.value || key in _loadingPhotos.value) return
        _loadingPhotos.value = _loadingPhotos.value + key
        viewModelScope.launch {
            when (val result = repository.loadPhoto(productId, name)) {
                is ApiResult.Success -> {
                    _photoBytes.value = _photoBytes.value + (key to result.value)
                    _photoErrors.value = _photoErrors.value - key
                }
                is ApiResult.Failure -> _photoErrors.value = _photoErrors.value + (key to result.message.orEmpty().ifBlank { "بارگیری تصویر ناموفق بود" })
            }
            _loadingPhotos.value = _loadingPhotos.value - key
        }
    }

    private fun photoKey(productId: String, imageName: String) = "$productId:$imageName"

    fun search(text: String) {
        rememberSearch(text)
        viewModelScope.launch {
            _loading.value = true
            when (val result = repository.search(ProductSearchQuery(text))) {
                is ApiResult.Success -> { _products.value = result.value; _message.value = null }
                is ApiResult.Failure -> { _products.value = emptyList(); _message.value = result.message }
            }
            _loading.value = false
        }
    }

    fun clearRecentSearches() {
        preferences.edit().remove(RECENT_KEY).apply()
        _recentSearches.value = emptyList()
    }

    fun searchRecent(text: String) = search(text)

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            _loading.value = true
            when (val result = repository.updateProduct(product)) {
                is ApiResult.Success -> {
                    _products.value = _products.value.map { if (it.id == result.value.id) result.value else it }
                    _message.value = "مشخصات اتیکت ذخیره شد"
                }
                is ApiResult.Failure -> _message.value = result.message
            }
            _loading.value = false
        }
    }

    fun refreshLiveGoldPrice() {
        viewModelScope.launch {
            when (val result = repository.liveGoldPrice()) {
                is ApiResult.Success -> { _liveGoldPrice.value = result.value; _message.value = null }
                is ApiResult.Failure -> _message.value = result.message
            }
        }
    }

    private fun rememberSearch(raw: String) {
        val text = raw.trim()
        if (text.isBlank()) return
        val next = (listOf(text) + _recentSearches.value.filterNot { it.equals(text, true) }).take(10)
        _recentSearches.value = next
        preferences.edit().putString(RECENT_KEY, JSONArray(next).toString()).apply()
    }

    private fun readRecentSearches(): List<String> = runCatching {
        val array = preferences.getString(RECENT_KEY, null)?.let(::JSONArray) ?: return emptyList()
        (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }.take(10)
    }.getOrDefault(emptyList())

    private companion object { const val RECENT_KEY = "recent_queries" }
    fun loadLatest() {
        viewModelScope.launch {
            _loading.value = true
            when (val result = repository.latest()) {
                is ApiResult.Success -> { _products.value = result.value; _message.value = null }
                is ApiResult.Failure -> { _products.value = emptyList(); _message.value = result.message }
            }
            _loading.value = false
        }
    }

    fun loadAll() {
        viewModelScope.launch {
            _loading.value = true
            when (val result = repository.all()) {
                is ApiResult.Success -> { _products.value = result.value; _message.value = null }
                is ApiResult.Failure -> { _products.value = emptyList(); _message.value = result.message }
            }
            _loading.value = false
        }
    }

    fun replacePhoto(productId: String, photo: File, replaceName: String?) {
        viewModelScope.launch {
            _loading.value = true
            val result = repository.replacePhoto(productId, photo, replaceName)
            photo.delete()
            when (result) {
                is ApiResult.Success -> { _message.value = "تصویر اتیکت با موفقیت ذخیره شد"; loadLatest() }
                is ApiResult.Failure -> { _message.value = result.message; _loading.value = false }
            }
        }
    }

    fun deletePhoto(productId: String, imageName: String) {
        viewModelScope.launch {
            _loading.value = true
            when (val result = repository.deletePhoto(productId, imageName)) {
                is ApiResult.Success -> { _message.value = "تصویر حذف شد"; loadLatest() }
                is ApiResult.Failure -> { _message.value = result.message; _loading.value = false }
            }
        }
    }
}
