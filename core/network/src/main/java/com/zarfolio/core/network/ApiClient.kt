package com.zarfolio.core.network

import com.zarfolio.core.model.ServerConfiguration
import com.zarfolio.core.security.TokenStore
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Builds tenant-scoped clients without embedding API paths or PocketBase collection names. */
@Singleton
class ApiClient @Inject constructor(private val tokenStore: TokenStore) {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    fun retrofitFor(server: ServerConfiguration): Retrofit {
        val normalized = ServerUrlValidator.normalize(server.baseUrl)
            ?: throw IllegalArgumentException("نشانی سرور معتبر نیست")
        val baseUrl = normalized.trimEnd('/') + "/"
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder().apply {
                    tokenStore.read(server.id)?.takeIf(String::isNotBlank)?.let { header("Authorization", "Bearer $it") }
                }.build()
                val response = chain.proceed(request)
                if (response.code == 401) tokenStore.remove(server.id)
                response
            }.build()
        return Retrofit.Builder().baseUrl(baseUrl).client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build()
    }
}
