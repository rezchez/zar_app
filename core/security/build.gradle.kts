plugins { id("com.android.library"); id("org.jetbrains.kotlin.android"); id("com.google.dagger.hilt.android"); kotlin("kapt") }
android { namespace = "com.zarfolio.core.security"; compileSdk = 36; defaultConfig { minSdk = 26 } }
dependencies {
    implementation("com.google.dagger:hilt-android:2.58")
    kapt("com.google.dagger:hilt-compiler:2.58")
}
kapt { correctErrorTypes = true }
