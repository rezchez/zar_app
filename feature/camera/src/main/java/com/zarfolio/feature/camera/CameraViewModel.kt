package com.zarfolio.feature.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zarfolio.core.network.ApiClient
import com.zarfolio.core.network.EtiquetteDto
import com.zarfolio.core.network.ServerConnectionRepository
import com.zarfolio.core.network.ZarfolioApi
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CameraUiState(
    val query: String = "",
    val products: List<EtiquetteDto> = emptyList(),
    val selectedProduct: EtiquetteDto? = null,
    val loading: Boolean = false,
    val uploading: Boolean = false,
    val message: String? = null,
    val imageUploaded: Boolean = false,
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val servers: ServerConnectionRepository,
    private val apiClient: ApiClient,
    private val photoCapture: ProductPhotoCapture,
) : ViewModel() {
    private val _state = MutableStateFlow(CameraUiState())
    val state = _state.asStateFlow()

    fun updateQuery(query: String) { _state.value = _state.value.copy(query = query, message = null) }

    fun searchProducts() {
        val query = _state.value.query.trim()
        if (query.isBlank()) {
            _state.value = _state.value.copy(message = "بارکد یا نام محصول را وارد کنید")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, message = null, selectedProduct = null, imageUploaded = false)
            runCatching {
                val server = servers.active() ?: error("ابتدا نشانی سرور را تنظیم کنید")
                val response = apiClient.retrofitFor(server).create(ZarfolioApi::class.java)
                    .searchEtiquettes(query = query, status = "available")
                if (!response.isSuccessful) error(if (response.code() == 401) "ابتدا وارد حساب شوید" else "جست‌وجوی محصول انجام نشد (${response.code()})")
                response.body()?.items.orEmpty()
            }.onSuccess { items ->
                _state.value = _state.value.copy(loading = false, products = items, message = if (items.isEmpty()) "محصول موجودی با این مشخصات پیدا نشد" else null)
            }.onFailure { error -> _state.value = _state.value.copy(loading = false, message = error.message ?: "ارتباط با سرور برقرار نشد") }
        }
    }

    fun select(product: EtiquetteDto) { _state.value = _state.value.copy(selectedProduct = product, message = null, imageUploaded = false) }

    fun upload(photo: File) {
        val product = _state.value.selectedProduct ?: run {
            _state.value = _state.value.copy(message = "ابتدا محصول را انتخاب کنید")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(uploading = true, message = null, imageUploaded = false)
            val result = photoCapture.upload(product.id, photo)
            photo.delete()
            result
                .onSuccess { _state.value = _state.value.copy(uploading = false, imageUploaded = true, message = "تصویر برای ${product.barcode} ذخیره شد") }
                .onFailure { _state.value = _state.value.copy(uploading = false, message = it.message ?: "ذخیرهٔ تصویر ناموفق بود") }
        }
    }
}
