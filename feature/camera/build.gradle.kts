plugins { id("com.android.library"); id("org.jetbrains.kotlin.android"); id("com.google.dagger.hilt.android"); id("org.jetbrains.kotlin.plugin.compose"); kotlin("kapt") }
android { namespace = "com.zarfolio.feature.camera"; compileSdk = 36; defaultConfig { minSdk = 26 }; buildFeatures { compose = true } }
dependencies {
    implementation(project(":core:designsystem")); implementation(project(":core:network")); implementation(project(":core:model")); implementation(project(":core:common")); implementation("androidx.core:core-ktx:1.17.0"); implementation("androidx.activity:activity-compose:1.12.0"); implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4"); implementation("androidx.hilt:hilt-navigation-compose:1.2.0"); implementation("com.squareup.okhttp3:okhttp:5.3.0")
    val composeBom = platform("androidx.compose:compose-bom:2025.10.00")
    implementation(composeBom); implementation("androidx.compose.foundation:foundation"); implementation("androidx.compose.material3:material3"); implementation("androidx.compose.material:material-icons-extended"); implementation("androidx.compose.ui:ui")
    implementation("com.google.dagger:hilt-android:2.58"); kapt("com.google.dagger:hilt-compiler:2.58")
    // CameraX is intentionally not initialized until the product-photo workflow and permissions are specified.
}
kapt { correctErrorTypes = true }
