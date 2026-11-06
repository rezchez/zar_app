package com.zarfolio.core.model

/** Domain concepts only. Mapping to PocketBase fields awaits the server's real API contract. */
data class Product(
    val id: String,
    val displayName: String,
    val barcode: String? = null,
    val itemCode: String? = null,
    val category: String? = null,
    val reference: String? = null,
    val price: ProductPrice? = null,
    val images: List<ProductImage> = emptyList(),
    val totalWeight: Double? = null,
    val netWeight: Double? = null,
    val metal: String? = null,
    val purity: Int? = null,
    val wage: Double? = null,
    val wageType: String? = null,
    val profitPercentage: Double? = null,
    val stonePrice: Long? = null,
    val otherCosts: Long? = null,
    val storageLocation: String? = null,
    val description: String? = null,
    val quantity: Int? = null,
    val status: String? = null,
)
data class ProductImage(val uri: String, val description: String? = null)
data class ProductPrice(val amountRial: Long, val currencyCode: String = "IRR")
data class ProductSearchQuery(val text: String, val tag: String? = null)
