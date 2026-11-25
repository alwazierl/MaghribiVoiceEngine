plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.maghribivoice"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.maghribivoice"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // غيّر العنوان إلى خادم HTTPS المنشور، مع / في النهاية.
        buildConfigField("String", "API_BASE_URL", "\"https://YOUR-SERVER.example/\"")
        // رمز تجريبي فقط. لا تعتبره سراً آمناً داخل APK.
        buildConfigField("String", "API_TOKEN", "\"CHANGE_ME\"")
    }

    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
