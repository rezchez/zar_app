package com.zarfolio.core.security

interface TokenStore {
    fun read(serverId: String): String?
    fun write(serverId: String, token: String)
    fun remove(serverId: String)
    fun readUserName(serverId: String): String?
    fun writeUserName(serverId: String, userName: String)
}
