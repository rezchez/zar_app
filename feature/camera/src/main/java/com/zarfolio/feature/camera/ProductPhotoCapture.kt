package com.zarfolio.feature.camera

import com.zarfolio.core.network.ApiClient
import com.zarfolio.core.network.ServerConnectionRepository
import com.zarfolio.core.network.ZarfolioApi
import com.zarfolio.core.network.PrimaryEtiquetteImageRequest
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

interface ProductPhotoCapture {
    suspend fun upload(productId: String, photo: File, replaceName: String? = null): Result<String>
}

@Singleton
class ProductPhotoCaptureImpl @Inject constructor(
    private val servers: ServerConnectionRepository,
    private val apiClient: ApiClient,
) : ProductPhotoCapture {
    override suspend fun upload(productId: String, photo: File, replaceName: String?): Result<String> = runCatching {
        require(productId.isNotBlank() && photo.isFile && photo.length() > 0) { "محصول یا فایل تصویر معتبر نیست" }
        val server = servers.active() ?: throw IOException("ابتدا نشانی سرور را تنظیم کنید")
        val part = MultipartBody.Part.createFormData(
            "image",
            "product-${productId.take(12)}.jpg",
            photo.asRequestBody("image/jpeg".toMediaType()),
        )
        val replacePart = replaceName?.toRequestBody("text/plain".toMediaType())
        val response = apiClient.retrofitFor(server).create(ZarfolioApi::class.java).uploadEtiquetteImage(productId, part, replacePart)
        if (!response.isSuccessful) {
            val message = when (response.code()) {
                401 -> "نشست شما منقضی شده است؛ دوباره وارد شوید"
                403 -> "حساب شما مجوز تغییر تصویر این اتیکت را ندارد"
                404 -> "اتیکت در سرور پیدا نشد"
                else -> "بارگذاری تصویر ناموفق بود (${response.code()})"
            }
            throw IOException(message)
        }
        if (response.body()?.item?.id != productId) throw IOException("سرور تصویر را برای اتیکت انتخاب‌شده تأیید نکرد")
        "/api/etiquettes/$productId/image"
    }
}

@dagger.Module
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
abstract class CameraModule {
    @dagger.Binds
    @javax.inject.Singleton
    abstract fun bindProductPhotoCapture(impl: ProductPhotoCaptureImpl): ProductPhotoCapture
}
