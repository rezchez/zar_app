package com.zarfolio.core.network

import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
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
    val status: String = "available",
    val image: String? = null,
    val imageUrl: String? = null,
    val images: List<String> = emptyList(),
    val imageUrls: List<String> = emptyList(),
)

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

    @Multipart
    @POST("api/etiquettes/{id}/image")
    suspend fun uploadEtiquetteImage(
        @Path("id") id: String,
        @Part image: MultipartBody.Part,
        @Part("replace") replace: okhttp3.RequestBody? = null,
    ): Response<EtiquetteEnvelope>

    @retrofit2.http.DELETE("api/etiquettes/{id}/image")
    suspend fun deleteEtiquetteImage(@Path("id") id: String, @Query("name") imageName: String): Response<EtiquetteEnvelope>
}
