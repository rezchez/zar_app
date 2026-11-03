plugins { id("com.android.library"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android { namespace = "com.zarfolio.core.designsystem"; compileSdk = 36; defaultConfig { minSdk = 26 }; buildFeatures { compose = true } }
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.10.00")
    api(composeBom); api("androidx.compose.material3:material3"); api("androidx.compose.ui:ui"); api("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation"); implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.core:core-ktx:1.17.0")
}
