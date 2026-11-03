plugins { id("com.android.library"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.serialization") }
android { namespace = "com.zarfolio.core.model"; compileSdk = 36; defaultConfig { minSdk = 26 } }
dependencies { implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0") }
