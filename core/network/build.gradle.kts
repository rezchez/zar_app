plugins { id("com.android.library"); id("org.jetbrains.kotlin.android"); id("com.google.dagger.hilt.android"); id("org.jetbrains.kotlin.plugin.serialization"); kotlin("kapt") }
android { namespace = "com.zarfolio.core.network"; compileSdk = 36; defaultConfig { minSdk = 26 } }
dependencies {
    implementation(project(":core:common")); api(project(":core:model")); implementation(project(":core:security"))
    implementation("com.google.dagger:hilt-android:2.58"); kapt("com.google.dagger:hilt-compiler:2.58")
    api("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:5.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:3.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    testImplementation("junit:junit:4.13.2"); testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
kapt { correctErrorTypes = true }
