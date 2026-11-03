package com.zarfolio.feature.products

import com.zarfolio.core.common.ApiResult
import com.zarfolio.core.model.Product
import com.zarfolio.core.model.ProductImage
import com.zarfolio.core.model.ProductPrice
import com.zarfolio.core.model.ProductSearchQuery
import com.zarfolio.core.network.ApiClient
import com.zarfolio.core.network.EtiquetteDto
import com.zarfolio.core.network.ServerConnectionRepository
import com.zarfolio.core.network.ZarfolioApi
import java.io.IOException
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

interface ProductRepository {
    suspend fun search(query: ProductSearchQuery): ApiResult<List<Product>>
    suspend fun latest(): ApiResult<List<Product>>
    suspend fun replacePhoto(productId: String, photo: File, replaceName: String?): ApiResult<Unit>
    suspend fun deletePhoto(productId: String, imageName: String): ApiResult<Unit>
}

@Singleton
class ContractPendingProductRepository @Inject constructor(
    private val servers: ServerConnectionRepository,
    private val apiClient: ApiClient,
) : ProductRepository {
    override suspend fun search(query: ProductSearchQuery): ApiResult<List<Product>> = request {
        val server = servers.active() ?: throw IOException("ابتدا نشانی سرور را تنظیم کنید")
        apiClient.retrofitFor(server).create(ZarfolioApi::class.java)
            .searchEtiquettes(query.text.trim().ifBlank { query.tag?.trim()?.ifBlank { null } }, "available")
            .let { response ->
                if (!response.isSuccessful) throw IOException(if (response.code() == 401) "ابتدا وارد حساب شوید" else "جست‌وجوی اتیکت انجام نشد (${response.code()})")
                response.body()?.items.orEmpty().map(EtiquetteDto::toProduct)
            }
    }

    override suspend fun latest(): ApiResult<List<Product>> = request {
        val server = servers.active() ?: throw IOException("ابتدا نشانی سرور را تنظیم کنید")
        apiClient.retrofitFor(server).create(ZarfolioApi::class.java)
            .searchEtiquettes(status = "available").let { response ->
                if (!response.isSuccessful) throw IOException(if (response.code() == 401) "ابتدا وارد حساب شوید" else "دریافت اتیکت‌ها انجام نشد (${response.code()})")
                response.body()?.items.orEmpty().map(EtiquetteDto::toProduct)
            }
    }

    override suspend fun replacePhoto(productId: String, photo: File, replaceName: String?): ApiResult<Unit> = requestUnit {
        val api = activeApi()
        val image = MultipartBody.Part.createFormData("image", photo.name, photo.asRequestBody("image/jpeg".toMediaType()))
        val replace = replaceName?.toRequestBody("text/plain".toMediaType())
        val response = api.uploadEtiquetteImage(productId, image, replace)
        if (!response.isSuccessful) throw IOException(if (response.code() == 409) "هر اتیکت حداکثر می‌تواند ۵ عکس داشته باشد" else "ذخیرهٔ عکس ناموفق بود (${response.code()})")
        if (response.body()?.item?.id != productId) throw IOException("سرور اتیکت مقصد تصویر را تأیید نکرد؛ تصویر را بررسی کنید")
    }

    override suspend fun deletePhoto(productId: String, imageName: String): ApiResult<Unit> = requestUnit {
        val response = activeApi().deleteEtiquetteImage(productId, imageName)
        if (!response.isSuccessful) throw IOException("حذف عکس ناموفق بود (${response.code()})")
        if (response.body()?.item?.id != productId) throw IOException("سرور اتیکت مقصد حذف تصویر را تأیید نکرد")
    }

    private suspend fun activeApi(): ZarfolioApi {
        val server = servers.active() ?: throw IOException("ابتدا نشانی سرور را تنظیم کنید")
        return apiClient.retrofitFor(server).create(ZarfolioApi::class.java)
    }

    private suspend fun request(block: suspend () -> List<Product>): ApiResult<List<Product>> = try {
        ApiResult.Success(block())
    } catch (error: Exception) {
        ApiResult.Failure(error, error.message ?: "ارتباط با سرور برقرار نشد")
    }

    private suspend fun requestUnit(block: suspend () -> Unit): ApiResult<Unit> = try {
        block(); ApiResult.Success(Unit)
    } catch (error: Exception) { ApiResult.Failure(error, error.message ?: "ارتباط با سرور برقرار نشد") }
}

private fun EtiquetteDto.toProduct() = Product(
    id = id,
    displayName = title.ifBlank { barcode },
    reference = listOfNotNull(barcode.takeIf(String::isNotBlank), itemCode?.takeIf(String::isNotBlank)).joinToString(" · ").ifBlank { null },
    price = ProductPrice(totalPrice.coerceAtLeast(0L)),
    images = imageUrls.mapIndexed { index, url -> ProductImage(url, images.getOrNull(index)) }
        .ifEmpty { listOfNotNull(imageUrl?.let { ProductImage(it, image) }) },
    totalWeight = totalWeight,
    netWeight = netWeight,
    metal = metal,
    status = status,
)
