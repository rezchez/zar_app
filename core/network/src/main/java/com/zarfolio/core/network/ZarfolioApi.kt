package com.zarfolio.core.network

import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class NativeLoginRequest(
    val email: String,
    val password: String,
    val mfaId: String? = null,
    val otpId: String? = null,
    val otpCode: String? = null,
    val totpCode: String? = null,
    val authMethod: String? = null,
)

@Serializable
data class NativeLoginResponse(
    val success: Boolean = false,
    val token: String? = null,
    val user: NativeUser? = null,
    val mfaRequired: Boolean = false,
    val mfaId: String? = null,
    val otpId: String? = null,
    val authenticatorAvailable: Boolean = false,
    val emailOtpAvailable: Boolean = false,
    val message: String? = null,
)

@Serializable
data class NativeUser(
    val id: String,
    val email: String? = null,
    val name: String? = null,
    val role: String? = null,
)

@Serializable
data class EtiquetteEnvelope(
    val items: List<EtiquetteDto> = emptyList(),
    val item: EtiquetteDto? = null,
    val message: String? = null,
    val totalCount: Int? = null,
)

@Serializable
data class EtiquetteDto(
    val id: String,
    val barcode: String = "",
    val title: String = "",
    val itemCode: String? = null,
    val totalWeight: Double = 0.0,
    val netWeight: Double = 0.0,
    val totalPrice: Long = 0,
    val metal: String = "gold",
    val purity: Int = 750,
    val etiquetteType: String? = null,
    val wage: Double = 0.0,
    val wageType: String = "percentage",
    val profitPercentage: Double = 0.0,
    val stonePrice: Long = 0,
    val otherCosts: Long = 0,
    val storageLocation: String? = null,
    val description: String? = null,
    val quantity: Int = 1,
    val status: String = "available",
    val image: String? = null,
    val imageUrl: String? = null,
    val images: List<String> = emptyList(),
    val imageUrls: List<String> = emptyList(),
)

@Serializable
data class MarketQuote(
    val id: String = "",
    val category: String = "",
    val title: String = "",
    val symbol: String = "",
    val unit: String = "",
    val price: Double = 0.0,
    val fetchedAt: String = "",
)

@Serializable
data class MarketQuotesEnvelope(val quotes: List<MarketQuote> = emptyList())

@Serializable
data class EtiquetteUpdateRequest(
    val title: String,
    val barcode: String,
    val itemCode: String,
    val etiquetteType: String,
    val totalWeight: Double,
    val purity: Int,
    val wage: Double,
    val wageType: String,
    val profitPercentage: Double,
    val stonePrice: Long,
    val otherCosts: Long,
    val totalPrice: Long,
    val storageLocation: String,
    val quantity: Int,
    val status: String,
    val description: String,
)

@Serializable
data class PrimaryEtiquetteImageRequest(val name: String)

interface ZarfolioApi {
    @POST("api/auth/login")
    suspend fun login(
        @Header("X-Zarfolio-Client") client: String = "android",
        @Body request: NativeLoginRequest,
    ): Response<NativeLoginResponse>

    @GET("api/etiquettes")
    suspend fun searchEtiquettes(
        @Query("search") query: String? = null,
        @Query("status") status: String? = null,
    ): Response<EtiquetteEnvelope>

    @GET("api/price-api/quotes")
    suspend fun marketQuotes(): Response<MarketQuotesEnvelope>

    @retrofit2.http.PATCH("api/etiquettes/{id}")
    suspend fun updateEtiquette(@Path("id") id: String, @Body updates: EtiquetteUpdateRequest): Response<EtiquetteEnvelope>

    @Multipart
    @POST("api/etiquettes/{id}/image")
    suspend fun uploadEtiquetteImage(
        @Path("id") id: String,
        @Part image: MultipartBody.Part,
        @Part("replace") replace: okhttp3.RequestBody? = null,
    ): Response<EtiquetteEnvelope>

    @retrofit2.http.DELETE("api/etiquettes/{id}/image")
    suspend fun deleteEtiquetteImage(@Path("id") id: String, @Query("name") imageName: String): Response<EtiquetteEnvelope>

    @retrofit2.http.PATCH("api/etiquettes/{id}/image")
    suspend fun setPrimaryEtiquetteImage(@Path("id") id: String, @Body request: PrimaryEtiquetteImageRequest): Response<EtiquetteEnvelope>

    @GET("api/etiquettes/{id}/image")
    @retrofit2.http.Streaming
    suspend fun getEtiquetteImage(@Path("id") id: String, @Query("name") imageName: String): Response<ResponseBody>
}
