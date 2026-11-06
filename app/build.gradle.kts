plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("com.google.dagger.hilt.android"); id("org.jetbrains.kotlin.plugin.compose"); kotlin("kapt") }
android {
    namespace = "com.zarfolio.app"
    compileSdk = 36
    defaultConfig { applicationId = "com.zarfolio.app"; minSdk = 26; targetSdk = 36; versionCode = 7; versionName = "1.1.14b" }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildTypes { release { isMinifyEnabled = false; signingConfig = signingConfigs.getByName("debug") } }
}
dependencies {
    implementation(project(":core:common")); implementation(project(":core:model")); implementation(project(":core:network")); implementation(project(":core:security")); implementation(project(":core:designsystem"))
    implementation(project(":feature:authentication")); implementation(project(":feature:dashboard")); implementation(project(":feature:products")); implementation(project(":feature:camera"))
    implementation("com.google.dagger:hilt-android:2.58"); kapt("com.google.dagger:hilt-compiler:2.58")
    implementation("androidx.activity:activity-compose:1.12.0"); implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4"); implementation("androidx.navigation:navigation-compose:2.9.8"); implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    val composeBom = platform("androidx.compose:compose-bom:2025.10.00")
    implementation(composeBom); implementation("androidx.compose.foundation:foundation"); implementation("androidx.compose.material3:material3"); implementation("androidx.compose.ui:ui"); implementation("androidx.compose.material:material-icons-extended")
    testImplementation("junit:junit:4.13.2"); testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    androidTestImplementation(composeBom); androidTestImplementation("androidx.compose.ui:ui-test-junit4"); androidTestImplementation("androidx.test.ext:junit:1.2.1"); androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling"); debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
}
kapt { correctErrorTypes = true }
