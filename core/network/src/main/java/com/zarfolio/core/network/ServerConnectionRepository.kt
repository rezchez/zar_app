package com.zarfolio.core.network

import com.zarfolio.core.common.ApiResult
import com.zarfolio.core.model.ServerConfiguration
import kotlinx.coroutines.flow.Flow

interface ServerConnectionRepository {
    fun observeServers(): Flow<List<ServerConfiguration>>
    suspend fun save(configuration: ServerConfiguration)
    suspend fun select(serverId: String)
    suspend fun active(): ServerConfiguration?
    suspend fun test(configuration: ServerConfiguration): ApiResult<Long>
}

object ServerUrlValidator {
    fun normalize(raw: String): String? {
        val text = raw.trim().let { if ("://" in it) it else "https://$it" }
        return runCatching {
            val uri = java.net.URI(text).normalize()
            val scheme = uri.scheme?.lowercase() ?: return null
            val host = uri.host ?: return null
            if (scheme !in setOf("https", "http") || uri.userInfo != null || uri.fragment != null) return null
            if (scheme == "http" && !isPrivateOrLoopback(host)) return null
            val path = uri.rawPath.orEmpty().trimEnd('/').let { if (it.isEmpty()) "" else it }
            "$scheme://${uri.rawAuthority}$path"
        }.getOrNull()
    }

    private fun isPrivateOrLoopback(host: String): Boolean {
        val h = host.lowercase()
        if (h == "localhost" || h.endsWith(".localhost") || h.endsWith(".local") || h == "::1") return true
        val ip = runCatching { java.net.InetAddress.getByName(h) }.getOrNull() ?: return false
        return ip.isLoopbackAddress || ip.isSiteLocalAddress || ip.isLinkLocalAddress
    }
}
