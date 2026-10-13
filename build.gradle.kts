plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.example.lostbtc"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.lostbtc"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"
    }
}
dependencies {
    implementation("org.bitcoinj:bitcoinj-core:0.16.3")
}
