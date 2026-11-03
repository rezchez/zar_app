package com.zarfolio.core.model

/** Domain concepts only. Mapping to PocketBase fields awaits the server's real API contract. */
data class Product(
    val id: String,
    val displayName: String,
    val reference: String? = null,
    val price: ProductPrice? = null,
    val images: List<ProductImage> = emptyList(),
    val totalWeight: Double? = null,
    val netWeight: Double? = null,
    val metal: String? = null,
    val status: String? = null,
)
data class ProductImage(val uri: String, val description: String? = null)
data class ProductPrice(val amountRial: Long, val currencyCode: String = "IRR")
data class ProductSearchQuery(val text: String, val tag: String? = null)
