package com.zarfolio.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zarfolio.core.common.ApiResult
import com.zarfolio.core.model.Product
import com.zarfolio.core.model.ProductSearchQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

@HiltViewModel
class ProductSearchViewModel @Inject constructor(private val repository: ProductRepository) : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products = _products.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    fun search(text: String) {
        viewModelScope.launch {
            _loading.value = true
            when (val result = repository.search(ProductSearchQuery(text))) {
                is ApiResult.Success -> { _products.value = result.value; _message.value = null }
                is ApiResult.Failure -> { _products.value = emptyList(); _message.value = result.message }
            }
            _loading.value = false
        }
    }
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
