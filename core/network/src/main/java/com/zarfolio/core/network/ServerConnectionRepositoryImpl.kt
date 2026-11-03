package com.zarfolio.core.network

import android.content.Context
import com.zarfolio.core.common.ApiResult
import com.zarfolio.core.model.ServerConfiguration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLSocketFactory

@Singleton
class ServerConnectionRepositoryImpl @Inject constructor(@ApplicationContext context: Context) : ServerConnectionRepository {
    private val prefs = context.getSharedPreferences("zarfolio_servers", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val state = MutableStateFlow(readSaved())

    override fun observeServers(): Flow<List<ServerConfiguration>> = state.asStateFlow()
    override suspend fun save(configuration: ServerConfiguration) {
        require(ServerUrlValidator.normalize(configuration.baseUrl) != null) { "نشانی سرور معتبر نیست" }
        val normalized = configuration.copy(baseUrl = ServerUrlValidator.normalize(configuration.baseUrl)!!)
        val next = (state.value.filterNot { it.id == normalized.id } + normalized).map { it.copy(isActive = it.id == (state.value.firstOrNull { s -> s.isActive }?.id ?: normalized.id)) }
        persist(next)
    }
    override suspend fun select(serverId: String) {
        require(state.value.any { it.id == serverId })
        persist(state.value.map { it.copy(isActive = it.id == serverId) })
    }
    override suspend fun active(): ServerConfiguration? = state.value.firstOrNull { it.isActive }

    /** TCP/TLS reachability check only; no guessed HTTP route or application health endpoint is requested. */
    override suspend fun test(configuration: ServerConfiguration): ApiResult<Long> = withContext(Dispatchers.IO) {
        val normalized = ServerUrlValidator.normalize(configuration.baseUrl)
            ?: return@withContext ApiResult.Failure(IllegalArgumentException("نشانی سرور معتبر نیست"), "نشانی سرور معتبر نیست")
        val uri = java.net.URI(normalized)
        val port = if (uri.port >= 0) uri.port else if (uri.scheme == "https") 443 else 80
        val started = System.nanoTime()
        runCatching {
            if (uri.scheme == "https") {
                (SSLSocketFactory.getDefault().createSocket() as javax.net.ssl.SSLSocket).use { socket ->
                    socket.connect(InetSocketAddress(uri.host, port), 6000)
                    socket.soTimeout = 6000
                    socket.startHandshake()
                }
            } else Socket().use { it.connect(InetSocketAddress(uri.host, port), 6000) }
            (System.nanoTime() - started) / 1_000_000
        }.fold(
            onSuccess = { ApiResult.Success(it) },
            onFailure = { ApiResult.Failure(it, "سرور در دسترس نیست؛ نشانی و شبکه را بررسی کنید") },
        )
    }

    private fun readSaved(): List<ServerConfiguration> = runCatching {
        val value = prefs.getString(KEY, null) ?: return emptyList()
        json.decodeFromString(ListSerializer(ServerConfiguration.serializer()), value)
    }.getOrDefault(emptyList())
    private fun persist(value: List<ServerConfiguration>) {
        prefs.edit().putString(KEY, json.encodeToString(ListSerializer(ServerConfiguration.serializer()), value)).apply()
        state.value = value
    }
    private companion object { const val KEY = "saved_servers_v1" }
}
