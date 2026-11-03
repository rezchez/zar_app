package com.zarfolio.core.model

import kotlinx.serialization.Serializable

/** A tenant/server address supplied by the user; it is not a PocketBase collection or schema. */
@Serializable
data class ServerConfiguration(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val isActive: Boolean = false,
)
