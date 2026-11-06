package com.zarfolio.app.navigation

object AppDestinations {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val PRICE_INQUIRY = "price-inquiry"
    const val PRODUCTS = "products"
    const val CAMERA = "camera"
    const val RECENT = "recent"
    const val WITHOUT_PHOTO = "without-photo"
    const val ALL_ETIQUETTES = "all-etiquettes"
    const val CONNECTION = "connection"
    const val LOGIN = "login"
    val bottomTabs = listOf(HOME, PRODUCTS, RECENT, SETTINGS, PROFILE)
    val all = setOf(HOME, SETTINGS, PROFILE, PRICE_INQUIRY, PRODUCTS, CAMERA, RECENT, WITHOUT_PHOTO, ALL_ETIQUETTES, CONNECTION, LOGIN)
}
