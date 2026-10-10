plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.roloam.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.roloam.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 70000
        versionName = "0.7.0"
        buildConfigField("boolean", "BETA_CHANNEL", "false")
    }

    signingConfigs {
        create("beta") {
            storeFile = file("roloam-beta.keystore")
            storePassword = "roloam-beta-only"
            keyAlias = "roloam-beta"
            keyPassword = "roloam-beta-only"
        }
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("boolean", "BETA_CHANNEL", "false")
        }
        create("beta") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"
            signingConfig = signingConfigs.getByName("beta")
            isDebuggable = false
            isMinifyEnabled = false
            buildConfigField("boolean", "BETA_CHANNEL", "true")
            matchingFallbacks += listOf("debug")
        }
        getByName("release") {
            buildConfigField("boolean", "BETA_CHANNEL", "false")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.02.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.moshi:moshi:1.15.2")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.2")
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    testImplementation("junit:junit:4.13.2")
}
