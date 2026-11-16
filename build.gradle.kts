plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.aleksmathai"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.aleksmathai"
        minSdk = 26
        targetSdk = 35
        versionCode = 41
        versionName = "4.1-free"
    }
}
dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
}
