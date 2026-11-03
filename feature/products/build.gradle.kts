plugins { id("com.android.library"); id("org.jetbrains.kotlin.android"); id("com.google.dagger.hilt.android"); id("org.jetbrains.kotlin.plugin.compose"); kotlin("kapt") }
android { namespace = "com.zarfolio.feature.products"; compileSdk = 36; defaultConfig { minSdk = 26 }; buildFeatures { compose = true } }
dependencies {
    implementation(project(":core:designsystem")); implementation(project(":core:model")); implementation(project(":core:common")); implementation(project(":core:network"))
    implementation("com.google.dagger:hilt-android:2.58"); kapt("com.google.dagger:hilt-compiler:2.58"); implementation("androidx.hilt:hilt-navigation-compose:1.2.0"); implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.core:core-ktx:1.17.0")
    val composeBom = platform("androidx.compose:compose-bom:2025.10.00")
    implementation(composeBom); implementation("androidx.compose.foundation:foundation"); implementation("androidx.compose.material3:material3"); implementation("androidx.compose.ui:ui"); implementation("androidx.compose.material:material-icons-extended")
    testImplementation("junit:junit:4.13.2")
}

kapt { correctErrorTypes = true }
