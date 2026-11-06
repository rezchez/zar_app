package com.zarfolio.feature.products

import com.zarfolio.core.common.ApiResult
import com.zarfolio.core.model.Product
import com.zarfolio.core.model.ProductImage
import com.zarfolio.core.model.ProductPrice
import com.zarfolio.core.model.ProductSearchQuery
import com.zarfolio.core.network.ApiClient
import com.zarfolio.core.network.EtiquetteDto
import com.zarfolio.core.network.EtiquetteUpdateRequest
import com.zarfolio.core.network.ServerConnectionRepository
import com.zarfolio.core.network.ZarfolioApi
import com.zarfolio.core.network.decodeOrientedPhoto
import com.zarfolio.core.network.encodePhotoPreview
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
    suspend fun all(): ApiResult<List<Product>>
    suspend fun replacePhoto(productId: String, photo: File, replaceName: String?): ApiResult<Unit>
    suspend fun loadPhoto(productId: String, imageName: String): ApiResult<ByteArray>
    suspend fun deletePhoto(productId: String, imageName: String): ApiResult<Unit>
    suspend fun updateProduct(product: Product): ApiResult<Product>
    suspend fun liveGoldPrice(): ApiResult<Long>
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

    override suspend fun all(): ApiResult<List<Product>> = request {
        val server = servers.active() ?: throw IOException("ابتدا نشانی سرور را تنظیم کنید")
        apiClient.retrofitFor(server).create(ZarfolioApi::class.java)
            .searchEtiquettes().let { response ->
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

    override suspend fun loadPhoto(productId: String, imageName: String): ApiResult<ByteArray> = try {
        val response = activeApi().getEtiquetteImage(productId, imageName)
        if (!response.isSuccessful) throw IOException("دریافت تصویر ناموفق بود (${response.code()})")
        val bytes = response.body()?.use { it.bytes() }?.takeIf { it.isNotEmpty() }
            ?: throw IOException("پاسخ تصویر خالی است")
        val bitmap = decodeOrientedPhoto(bytes) ?: throw IOException("فایل تصویر معتبر نیست")
        try { ApiResult.Success(encodePhotoPreview(bitmap)) } finally { bitmap.recycle() }
    } catch (error: Exception) {
        ApiResult.Failure(error, error.message ?: "بارگیری تصویر ناموفق بود")
    }

    override suspend fun deletePhoto(productId: String, imageName: String): ApiResult<Unit> = requestUnit {
        val response = activeApi().deleteEtiquetteImage(productId, imageName)
        if (!response.isSuccessful) throw IOException("حذف عکس ناموفق بود (${response.code()})")
        if (response.body()?.item?.id != productId) throw IOException("سرور اتیکت مقصد حذف تصویر را تأیید نکرد")
    }

    override suspend fun updateProduct(product: Product): ApiResult<Product> = requestValue {
        val response = activeApi().updateEtiquette(
            product.id,
            EtiquetteUpdateRequest(
                title = product.displayName,
                barcode = product.barcode.orEmpty(),
                itemCode = product.itemCode.orEmpty(),
                etiquetteType = product.category?.takeIf { it in setOf("plain", "gemstone", "jewelry", "base") } ?: "plain",
                totalWeight = product.totalWeight ?: 0.0,
                purity = product.purity ?: 750,
                wage = product.wage ?: 0.0,
                wageType = product.wageType ?: "percentage",
                profitPercentage = product.profitPercentage ?: 0.0,
                stonePrice = product.stonePrice ?: 0L,
                otherCosts = product.otherCosts ?: 0L,
                totalPrice = product.price?.amountRial ?: 0L,
                storageLocation = product.storageLocation.orEmpty(),
                quantity = product.quantity ?: 1,
                status = product.status ?: "available",
                description = product.description.orEmpty(),
            ),
        )
        if (!response.isSuccessful) throw IOException(if (response.code() == 401) "نشست ورود منقضی شده است" else "ویرایش اتیکت ناموفق بود (${response.code()})")
        response.body()?.item?.toProduct() ?: throw IOException("سرور پاسخ ویرایش را تأیید نکرد")
    }

    override suspend fun liveGoldPrice(): ApiResult<Long> = requestValue {
        val response = activeApi().marketQuotes()
        if (!response.isSuccessful) throw IOException("دریافت قیمت زنده ناموفق بود (${response.code()})")
        val gold = response.body()?.quotes.orEmpty().firstOrNull {
            it.category.equals("gold", true) && (it.symbol.contains("750") || it.title.contains("18") || it.title.contains("گرم"))
        } ?: response.body()?.quotes.orEmpty().firstOrNull { it.category.equals("gold", true) }
        (gold?.price ?: throw IOException("قیمت زندهٔ طلا در پاسخ سرور موجود نیست")).toLong().coerceAtLeast(0L)
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

    private suspend fun <T> requestValue(block: suspend () -> T): ApiResult<T> = try {
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
    barcode = barcode.takeIf(String::isNotBlank),
    itemCode = itemCode,
    category = etiquetteType,
    reference = listOfNotNull(barcode.takeIf(String::isNotBlank), itemCode?.takeIf(String::isNotBlank)).joinToString(" · ").ifBlank { null },
    price = ProductPrice(totalPrice.coerceAtLeast(0L)),
    images = imageUrls.mapIndexed { index, url -> ProductImage(url, images.getOrNull(index)) }
        .ifEmpty { listOfNotNull(imageUrl?.let { ProductImage(it, image) }) },
    totalWeight = totalWeight,
    netWeight = netWeight,
    metal = metal,
    purity = purity,
    wage = wage,
    wageType = wageType,
    profitPercentage = profitPercentage,
    stonePrice = stonePrice,
    otherCosts = otherCosts,
    storageLocation = storageLocation,
    description = description,
    quantity = quantity,
    status = status,
)
