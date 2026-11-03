package com.zarfolio.app.navigation

object AppDestinations {
    const val HOME = "home"
    const val PRODUCTS = "products"
    const val CAMERA = "camera"
    const val RECENT = "recent"
    const val CONNECTION = "connection"
    const val LOGIN = "login"
    val bottomTabs = listOf(PRODUCTS, CAMERA, RECENT, CONNECTION)
    val all = setOf(HOME, PRODUCTS, CAMERA, RECENT, CONNECTION, LOGIN)
}
