package com.zarfolio.core.security

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthSessionManager @Inject constructor(private val tokenStore: TokenStore) {
    fun hasSession(serverId: String): Boolean = !tokenStore.read(serverId).isNullOrBlank()
    fun save(serverId: String, pocketBaseToken: String) = tokenStore.write(serverId, pocketBaseToken)
    fun logout(serverId: String) = tokenStore.remove(serverId)
    fun onUnauthorized(serverId: String) = tokenStore.remove(serverId)
}
