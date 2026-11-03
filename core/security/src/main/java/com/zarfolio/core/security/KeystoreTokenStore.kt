package com.zarfolio.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeystoreTokenStore @Inject constructor(@ApplicationContext context: Context) : TokenStore {
    private val prefs = context.getSharedPreferences("zarfolio_secure_tokens", Context.MODE_PRIVATE)

    override fun read(serverId: String): String? = readValue(serverId, "token")

    override fun write(serverId: String, token: String) = writeValue(serverId, "token", token)

    override fun readUserName(serverId: String): String? = readValue(serverId, "user")

    override fun writeUserName(serverId: String, userName: String) = writeValue(serverId, "user", userName)

    private fun readValue(serverId: String, kind: String): String? = synchronized(this) {
        val packed = prefs.getString(prefKey(serverId, kind), null) ?: return null
        runCatching {
            val chunks = packed.split(':', limit = 2)
            require(chunks.size == 2)
            val iv = Base64.decode(chunks[0], Base64.NO_WRAP)
            val encrypted = Base64.decode(chunks[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, keyFor(serverId), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun writeValue(serverId: String, kind: String, value: String) = synchronized(this) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keyFor(serverId))
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val encrypted = Base64.encodeToString(cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        prefs.edit().putString(prefKey(serverId, kind), "$iv:$encrypted").apply()
    }

    override fun remove(serverId: String) { prefs.edit().remove(prefKey(serverId, "token")).remove(prefKey(serverId, "user")).apply() }

    private fun prefKey(serverId: String, kind: String) = "${kind}_${MessageDigest.getInstance("SHA-256").digest(serverId.toByteArray()).joinToString("") { "%02x".format(it) }}"

    private fun keyFor(serverId: String): SecretKey {
        val alias = "zarfolio_token_${MessageDigest.getInstance("SHA-256").digest(serverId.toByteArray()).take(12).joinToString("") { "%02x".format(it) }}"
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build())
        return generator.generateKey()
    }
}
